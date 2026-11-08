package com.jarvis.assistant

import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/** Talks to YOUR proxy (see /proxy), which holds the Gemini key. The key is never in the APK. */
object Gemini {
    fun ask(
        url: String, token: String, history: List<Pair<String, String>>,
        driving: Boolean, done: (String) -> Unit
    ) {
        thread {
            val reply = try {
                val body = JSONObject().put("driving", driving).put(
                    "messages",
                    JSONArray(history.map { JSONObject().put("role", it.first).put("text", it.second) })
                )
                val c = URL(url).openConnection() as HttpURLConnection
                c.requestMethod = "POST"
                c.connectTimeout = 8000
                c.readTimeout = 20000
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                if (token.isNotBlank()) c.setRequestProperty("Authorization", "Bearer $token")
                c.outputStream.use { it.write(body.toString().toByteArray()) }
                val text = c.inputStream.bufferedReader().use { it.readText() }
                JSONObject(text).getString("reply")
            } catch (e: Exception) {
                "I couldn't reach my brain."
            }
            Handler(Looper.getMainLooper()).post { done(reply) }
        }
    }
}
