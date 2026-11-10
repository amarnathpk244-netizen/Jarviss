package com.jarvis.assistant

import android.app.*
import android.content.*
import android.os.*
import android.speech.*
import android.speech.tts.TextToSpeech
import android.net.Uri
import android.provider.Settings
import java.util.Locale

class JarvisService : Service(),
    RecognitionListener,
    TextToSpeech.OnInitListener {

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    private val handler = Handler(Looper.getMainLooper())

    private var active = false
    private var waitingForCommand = false
    private var speaking = false
    private var speechReady = false

    private val channelId = "jarvis_voice"

    override fun onCreate() {
        super.onCreate()

        createChannel()

        val notification = Notification.Builder(
            this, channelId
        )
            .setContentTitle("Jarvis Active")
            .setContentText("Listening for Hey Jarvis")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

        startForeground(101, notification)

        tts = TextToSpeech(this, this)

        if (
            SpeechRecognizer.isRecognitionAvailable(this)
        ) {
            recognizer =
                SpeechRecognizer.createSpeechRecognizer(this)

            recognizer?.setRecognitionListener(this)

            active = true
        } else {
            stopSelf()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            speechReady = true
            listen()
        }
    }

    private fun listen() {
        if (!active || speaking) return

        val intent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )

            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "en-IN"
            )

            putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                true
            )

            putExtra(
                RecognizerIntent.EXTRA_PREFER_OFFLINE,
                true
            )
        }

        try {
            recognizer?.startListening(intent)
        } catch (_: Exception) {
            restart()
        }
    }

    private fun restart() {
        if (!active) return

        handler.postDelayed({
            if (active && !speaking) {
                listen()
            }
        }, 900)
    }

    private fun say(message: String) {
        speaking = true

        try {
            recognizer?.cancel()
        } catch (_: Exception) {}

        tts?.speak(
            message,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "jarvis_reply"
        )

        val delay = (message.length * 75L)
            .coerceIn(1200L, 6500L)

        handler.postDelayed({
            speaking = false
            restart()
        }, delay)
    }

    override fun onResults(results: Bundle?) {
        val text = results
            ?.getStringArrayList(
                SpeechRecognizer.RESULTS_RECOGNITION
            )
            ?.firstOrNull()
            ?.lowercase(Locale.ROOT)
            ?: ""

        process(text)
        restart()
    }

    override fun onPartialResults(results: Bundle?) {
        val text = results
            ?.getStringArrayList(
                SpeechRecognizer.RESULTS_RECOGNITION
            )
            ?.firstOrNull()
            ?.lowercase(Locale.ROOT)
            ?: ""

        if (
            !waitingForCommand &&
            (
                text.contains("hey jarvis") ||
                text.contains("hi jarvis")
            )
        ) {
            waitingForCommand = true
            say("Yes, what can I do for you?")
        }
    }

    private fun process(text: String) {
        if (speaking || text.isBlank()) return

        val wake = text.contains("hey jarvis") ||
            text.contains("hi jarvis") ||
            text.startsWith("jarvis")

        if (wake && !waitingForCommand) {
            waitingForCommand = true

            val command = text
                .replace(
                    Regex("^(hey|hi)?\\s*jarvis\\s*"),
                    ""
                )
                .trim()

            if (command.isNotEmpty()) {
                execute(command)
            } else {
                say("Yes, what can I do for you?")
            }

            return
        }

        if (waitingForCommand) {
            waitingForCommand = false
            execute(text)
        }
    }

    private fun execute(command: String) {
        when {
            command.contains("time") -> {
                val time =
                    java.text.SimpleDateFormat(
                        "hh:mm a",
                        Locale.US
                    ).format(java.util.Date())

                say("The time is $time")
            }

            command.contains("date") -> {
                val date =
                    java.text.SimpleDateFormat(
                        "EEEE, MMMM dd",
                        Locale.US
                    ).format(java.util.Date())

                say("Today is $date")
            }

            command.contains("navigate") ||
            command.contains("direction") -> {
                val place = command
                    .replace("navigate to", "")
                    .replace("navigate", "")
                    .replace("directions to", "")
                    .replace("direction", "")
                    .trim()

                if (place.isNotEmpty()) {
                    openNavigation(place)
                } else {
                    say("Where should I navigate?")
                }
            }

            command.contains("open youtube") -> {
                openApp(
                    "com.google.android.youtube"
                )
            }

            command.contains("open whatsapp") -> {
                openApp(
                    "com.whatsapp"
                )
            }

            command.contains("open maps") -> {
                openApp(
                    "com.google.android.apps.maps"
                )
            }

            command.contains("open camera") -> {
                launch(
                    Intent(
                        "android.media.action.IMAGE_CAPTURE"
                    )
                )
            }

            command.contains("open settings") -> {
                launch(
                    Intent(Settings.ACTION_SETTINGS)
                )
            }

            command.startsWith("call ") -> {
                val number = command
                    .removePrefix("call ")
                    .trim()

                val intent = Intent(
                    Intent.ACTION_DIAL,
                    Uri.parse(
                        "tel:${Uri.encode(number)}"
                    )
                )

                launch(intent)
            }

            command.contains("stop listening") ||
            command.contains("shutdown") -> {
                say("Shutting down Jarvis")
                handler.postDelayed({
                    stopSelf()
                }, 1800)
            }

            else -> {
                say(
                    "Sorry, I cannot perform that command yet."
                )
            }
        }
    }

    private fun openNavigation(place: String) {
        val uri = Uri.parse(
            "google.navigation:q=${Uri.encode(place)}"
        )

        val intent = Intent(
            Intent.ACTION_VIEW,
            uri
        ).apply {
            setPackage(
                "com.google.android.apps.maps"
            )
        }

        launch(intent)
    }

    private fun openApp(packageName: String) {
        val intent = packageManager
            .getLaunchIntentForPackage(packageName)

        if (intent != null) {
            launch(intent)
        } else {
            say("Application is not available")
        }
    }

    private fun launch(intent: Intent) {
        try {
            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )

            startActivity(intent)
        } catch (_: Exception) {
            say("Unable to open the requested application")
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}

    override fun onError(error: Int) {
        restart()
    }

    override fun onEvent(
        eventType: Int,
        params: Bundle?
    ) {}

    override fun onDestroy() {
        active = false

        handler.removeCallbacksAndMessages(null)

        recognizer?.cancel()
        recognizer?.destroy()

        tts?.stop()
        tts?.shutdown()

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
