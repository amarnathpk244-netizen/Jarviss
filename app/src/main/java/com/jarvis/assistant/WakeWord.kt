package com.jarvis.assistant

import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import android.content.Context

interface WakeWordEngine {
    fun start(onWake: () -> Unit)
    fun stop()
}

/**
 * On-device wake word via Picovoice Porcupine.
 * Needs (1) a free AccessKey and (2) a custom "Hey Jarvis" .ppn trained at console.picovoice.ai,
 * placed in app/src/main/assets/. Verify the asset-path handling against the Porcupine docs.
 */
class PorcupineWakeWord(
    private val ctx: Context,
    private val accessKey: String,
    private val keywordAsset: String
) : WakeWordEngine {
    private var mgr: PorcupineManager? = null

    override fun start(onWake: () -> Unit) {
        mgr = PorcupineManager.Builder()
            .setAccessKey(accessKey)
            .setKeywordPath(keywordAsset)
            .setSensitivity(0.7f)
            .build(ctx, PorcupineManagerCallback { onWake() })
        mgr?.start()
    }

    override fun stop() {
        try { mgr?.stop(); mgr?.delete() } catch (_: Exception) {}
        mgr = null
    }
}
