package com.jarvis.assistant

import android.app.*
import android.content.*
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.speech.*
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.text.SimpleDateFormat
import java.util.*

class JarvisService : Service(), TextToSpeech.OnInitListener {
    companion object {
        const val STATUS = "com.jarvis.assistant.STATUS"
        const val TALK = "com.jarvis.assistant.TALK"
        const val PREFS = "jarvis_voice_v2"
    }
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var active = false
    private var speaking = false
    private var ttsReady = false
    private var useMalayalamTts = false
    private var waitingUntil = 0L
    private var generation = 0
    private var utterance = 0
    private var errors = 0
    private var forceEnglish = false
    private val channel = "jarvis_voice"
    private val restart = Runnable { listen() }
    private val recognitionTimeout = Runnable {
        invalidateRecognizer()
        schedule(500)
    }
    private val speechTimeout = Runnable {
        tts?.stop()
        finishSpeaking(utterance)
    }
    private val initTimeout = Runnable {
        if (active && !ttsReady) {
            status("Voice output unavailable. Listening is still active.")
            schedule()
        }
    }
    private fun waiting() = SystemClock.elapsedRealtime() < waitingUntil
    private fun status(message: String) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("status", message).apply()
        sendBroadcast(Intent(STATUS).setPackage(packageName).putExtra("status", message))
    }
    private fun notification(message: String, target: Intent? = null): Notification {
        val intent = target ?: Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(this, if (target == null) 0 else 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, channel)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(if (target == null) "Jarvis" else "Jarvis — tap to open")
            .setContentText(message).setContentIntent(pending).setOngoing(true).build()
    }
    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(channel, "Jarvis Voice", NotificationManager.IMPORTANCE_LOW))
        startForeground(101, notification("Starting microphone…"))
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status("Speech recognition unavailable. Enable a speech recognition service in Android settings.")
            stopSelf()
            return
        }
        active = true
        status("Starting voice output…")
        tts = TextToSpeech(this, this)
        handler.postDelayed(initTimeout, 4000)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == TALK && active) {
            waitingUntil = SystemClock.elapsedRealtime() + 20000
            say("പറയൂ, എന്താണ് വേണ്ടത്?", "Tell me your command.")
        }
        return START_NOT_STICKY
    }
    override fun onInit(result: Int) {
        handler.removeCallbacks(initTimeout)
        if (!active) return
        if (result == TextToSpeech.SUCCESS) {
            val language = tts?.setLanguage(Locale("ml", "IN")) ?: TextToSpeech.ERROR
            useMalayalamTts = language >= TextToSpeech.LANG_AVAILABLE
            val fallback = if (!useMalayalamTts) tts?.setLanguage(Locale.US) else language
            ttsReady = fallback != null && fallback >= TextToSpeech.LANG_AVAILABLE
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}
                override fun onDone(id: String?) { completed(id) }
                @Deprecated("Framework compatibility callback")
                override fun onError(id: String?) { completed(id) }
                override fun onError(id: String?, code: Int) { completed(id) }
                private fun completed(id: String?) {
                    handler.post { id?.toIntOrNull()?.let { finishSpeaking(it) } }
                }
            })
        }
        if (waiting()) schedule() else say("ജാർവിസ് തയ്യാറാണ്. ഹേയ് ജാർവിസ് എന്ന് വിളിക്കൂ.", "Jarvis is ready. Say Hey Jarvis.")
    }
    private fun invalidateRecognizer() {
        generation++ // Cancelled sessions must never execute a stale result or restart listening.
        handler.removeCallbacks(recognitionTimeout)
        val previous = recognizer
        recognizer = null
        try { previous?.cancel(); previous?.destroy() } catch (_: Exception) {}
    }
    private fun schedule(delay: Long = 450) {
        handler.removeCallbacks(restart)
        if (active && !speaking) handler.postDelayed(restart, delay)
    }
    private fun listen() {
        if (!active || speaking) return
        invalidateRecognizer()
        val session = generation
        val commandMode = waiting()
        val chosenLanguage = getSharedPreferences(PREFS, MODE_PRIVATE).getString("language", "ml-IN") ?: "ml-IN"
        val language = if (!commandMode || forceEnglish) "en-IN" else chosenLanguage
        status(if (commandMode) "Listening for command ($language)…" else "Listening — say Hey Jarvis (English)…")
        try {
            val engine = SpeechRecognizer.createSpeechRecognizer(this)
            recognizer = engine
            engine.setRecognitionListener(object : RecognitionListener {
                private fun valid() = active && !speaking && session == generation
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(type: Int, params: Bundle?) {}
                override fun onPartialResults(results: Bundle?) {
                    if (!valid()) return
                    val heard = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!heard.isNullOrBlank()) status("Hearing: $heard")
                    // Only final results execute commands: partial wake results can contain an unfinished command.
                }
                override fun onResults(results: Bundle?) {
                    if (!valid()) return
                    handler.removeCallbacks(recognitionTimeout)
                    errors = 0
                    val alternatives = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    val text = alternatives.firstOrNull { VoiceCommands.afterWake(it) != null }
                        ?: alternatives.firstOrNull { commandMode && VoiceCommands.parse(it).kind != "unknown" }
                        ?: alternatives.firstOrNull().orEmpty()
                    invalidateRecognizer()
                    if (text.isNotBlank()) {
                        status("Heard: $text")
                        process(text, commandMode)
                    }
                    schedule()
                }
                override fun onError(error: Int) {
                    if (!valid()) return
                    invalidateRecognizer()
                    errors++
                    when (error) {
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                            status("Microphone permission missing. Grant microphone access, then press START.")
                            stopSelf()
                            return
                        }
                        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> {
                            if (language != "en-IN") {
                                forceEnglish = true
                                status("Malayalam speech unavailable. Trying English; install Malayalam speech support for Malayalam commands.")
                            } else {
                                status("English speech support unavailable. Enable/download English (India) in your speech service, then press START.")
                                stopSelf()
                                return
                            }
                        }
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                            status("Speech network error. Check internet and speech service.")
                        SpeechRecognizer.ERROR_AUDIO -> status("Microphone unavailable. Close other recording apps.")
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> status("Speech service busy. Retrying…")
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> Unit
                        else -> status("Speech error $error. Retrying…")
                    }
                    schedule(if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) 600 else (errors * 1000L).coerceAtMost(8000))
                }
            })
            engine.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            })
            handler.postDelayed(recognitionTimeout, 15000)
        } catch (error: Exception) {
            invalidateRecognizer()
            status("Cannot start microphone: ${error.javaClass.simpleName}. Retrying…")
            schedule(2500)
        }
    }
    private fun say(ml: String, en: String) {
        handler.removeCallbacks(restart)
        handler.removeCallbacks(speechTimeout)
        invalidateRecognizer()
        utterance++
        val message = if (useMalayalamTts) ml else en
        status(message)
        if (!ttsReady) { speaking = false; schedule(); return }
        speaking = true
        if (tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, utterance.toString()) == TextToSpeech.ERROR) {
            finishSpeaking(utterance)
        } else handler.postDelayed(speechTimeout, 20000) // Recovery only; normal resumption uses onDone.
    }
    private fun finishSpeaking(id: Int) {
        if (!active || id != utterance) return
        handler.removeCallbacks(speechTimeout)
        speaking = false
        if (waiting()) waitingUntil = SystemClock.elapsedRealtime() + 15000
        schedule()
    }
    private fun process(text: String, wasWaiting: Boolean) {
        val tail = VoiceCommands.afterWake(text)
        if (tail != null) {
            if (tail.isBlank()) {
                waitingUntil = SystemClock.elapsedRealtime() + 20000
                say("പറയൂ, എന്താണ് വേണ്ടത്?", "Tell me your command.")
            } else { waitingUntil = 0; execute(tail) }
        } else if (wasWaiting && waiting()) { waitingUntil = 0; execute(text) }
    }
    private fun execute(text: String) {
        val command = VoiceCommands.parse(text)
        when (command.kind) {
            "time" -> {
                val time = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
                say("സമയം $time", "The time is $time")
            }
            "date" -> {
                val date = SimpleDateFormat("EEEE, dd MMMM yyyy", if (useMalayalamTts) Locale("ml", "IN") else Locale.US).format(Date())
                say("ഇന്ന് $date", "Today is $date")
            }
            "app" -> {
                val target = packageManager.getLaunchIntentForPackage(command.argument)
                if (target != null) launch(target) else say("ആപ്പ് കണ്ടെത്താനായില്ല.", "That app is not installed or available.")
            }
            "navigate" -> launch(Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${Uri.encode(command.argument)}")).setPackage("com.google.android.apps.maps"))
            "camera" -> launch(Intent("android.media.action.STILL_IMAGE_CAMERA"))
            "settings" -> launch(Intent(Settings.ACTION_SETTINGS))
            "dial" -> {
                val number = command.argument.replace(Regex("[\\s()\\-]"), "")
                if (number.matches(Regex("\\+?[0-9]{3,15}"))) launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
                else say("നമ്പർ പറയൂ. പേരുപയോഗിച്ച് വിളിക്കൽ ഇപ്പോൾ ലഭ്യമല്ല.", "Say a phone number. Calling contacts by name is not available yet.")
            }
            "cancel" -> say("ശരി.", "Cancelled.")
            "stop" -> { status("Jarvis stopped."); stopSelf() }
            else -> say("കമാൻഡ് മനസ്സിലായില്ല. വീണ്ടും ജാർവിസ് എന്ന് വിളിച്ച് പറയൂ.", "Command not understood. Say Hey Jarvis and try open YouTube or what is the time.")
        }
    }
    private fun launch(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            if (MainActivity.visible) {
                startActivity(intent)
                status("Opening requested app…")
            } else {
                getSystemService(NotificationManager::class.java).notify(102, notification("Tap to open your requested app", intent))
                say("ആപ്പ് തുറക്കാൻ ജാർവിസ് നോട്ടിഫിക്കേഷൻ ടാപ്പ് ചെയ്യൂ.", "Tap the Jarvis notification to open the requested app.")
            }
        } catch (_: Exception) { say("ആപ്പ് തുറക്കാനായില്ല.", "Unable to open the requested app.") }
    }
    override fun onDestroy() {
        active = false
        handler.removeCallbacksAndMessages(null)
        invalidateRecognizer()
        tts?.stop()
        tts?.shutdown()
        getSystemService(NotificationManager::class.java).cancel(102)
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
