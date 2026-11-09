package com.jarvis.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

/** Wake word -> "Yes, sir?" -> listen -> offline command or AI -> speak -> back to wake word. */
class JarvisService : Service() {
    companion object {
        const val ACTION_STOP = "com.jarvis.assistant.STOP"
        const val ACTION_LISTEN = "com.jarvis.assistant.LISTEN"
    }

    private val main = Handler(Looper.getMainLooper())
    private lateinit var talker: Talker
    private lateinit var listener: Listener
    private var wake: WakeWordEngine? = null
    private val history = mutableListOf<Pair<String, String>>()

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("jarvis", "Jarvis", NotificationManager.IMPORTANCE_LOW))
        ServiceCompat.startForeground(this, 1, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        talker = Talker(this)
        listener = Listener(this)
    }

    override fun onStartCommand(i: Intent?, flags: Int, startId: Int): Int {
        if (i?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        JarvisState.running = true
        if (Prefs.get(this).accessKey.isBlank()) {
            listen()
        } else {
            armWake()
        }
        return START_STICKY
    }

    private fun notification(): Notification {
        val stop = PendingIntent.getService(
            this, 0, Intent(this, JarvisService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, "jarvis")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Jarvis microphone is on")
            .setContentText("Say \"Hey Jarvis\"")
            .setOngoing(true)
            .addAction(0, "Turn off", stop)
            .build()
    }

    private fun armWake() {
        if (wake != null) return
        val key = Prefs.get(this).accessKey
        if (key.isBlank()) {
            JarvisState.status = "Ready - tap to speak"
            return
        }
        try {
            val w = PorcupineWakeWord(this, key, "Hey-Jarvis_en_android.ppn")
            w.start { main.post { onWake() } }
            wake = w
            JarvisState.status = "Listening for \"Hey Jarvis\""
        } catch (e: Exception) {
            JarvisState.status = "Wake word error: ${e.message}"
            stopSelf()
        }
    }

    private fun disarm() { wake?.stop(); wake = null }

    private fun onWake() {
        disarm()
        JarvisState.status = "Yes, sir?"
        talker.speak("Yes, sir?") { listen() }
    }

    private fun listen() {
        JarvisState.status = "Listening..."
        listener.listen(Prefs.get(this).lang) { handle(it) }
    }

    private fun handle(text: String?) {
        if (text.isNullOrBlank()) { armWake(); return }
        JarvisState.last = text
        if (Parsing.isStop(text)) { talker.speak("Stopped.") { armWake() }; return }
        CommandRouter.route(this, text)?.let { reply(it); return }
        val p = Prefs.get(this)
        history.add("user" to text)
        JarvisState.status = "Thinking..."
        Gemini.ask(p.proxyUrl, p.token, history.takeLast(10), p.driving) { r ->
            history.add("assistant" to r)
            reply(r)
        }
    }

    private fun reply(t: String) {
        JarvisState.status = "Speaking"
        talker.speak(t) { armWake() }
    }

    override fun onDestroy() {
        disarm(); listener.destroy(); talker.shutdown()
        JarvisState.running = false
        JarvisState.status = "Off"
        super.onDestroy()
    }
}
