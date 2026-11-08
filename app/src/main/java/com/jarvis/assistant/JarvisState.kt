package com.jarvis.assistant

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Tiny shared state so the UI can show what the service is doing. */
object JarvisState {
    var status by mutableStateOf("Off")
    var running by mutableStateOf(false)
    var last by mutableStateOf("")
}
