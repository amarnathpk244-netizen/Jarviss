package com.jarvis.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { JarvisApp() }
    }
}

private val Blue = Color(0xFF2EC4FF)

@Composable
fun Core(animate: Boolean, active: Boolean) {
    val s = if (animate) {
        rememberInfiniteTransition(label = "core")
            .animateFloat(0.9f, 1.1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "s").value
    } else 1f
    Canvas(Modifier.size(180.dp)) {
        val r = size.minDimension / 2 * 0.8f * s
        drawCircle(if (active) Blue.copy(alpha = 0.25f) else Color(0xFF173045), radius = r)
        drawCircle(Blue, radius = r, style = Stroke(width = 4f))
        drawCircle(Blue, radius = r * 0.45f, style = Stroke(width = 2f))
    }
}

@Composable
fun JarvisApp() {
    val ctx = LocalContext.current
    val prefs = remember { Prefs.get(ctx) }
    var key by remember { mutableStateOf(prefs.accessKey) }
    var url by remember { mutableStateOf(prefs.proxyUrl) }
    var token by remember { mutableStateOf(prefs.token) }
    var driving by remember { mutableStateOf(prefs.driving) }
    var ml by remember { mutableStateOf(prefs.lang == "ml-IN") }
    val perms = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    fun save() {
        prefs.accessKey = key.trim(); prefs.proxyUrl = url.trim(); prefs.token = token.trim()
        prefs.driving = driving; prefs.lang = if (ml) "ml-IN" else "en-IN"
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Blue, background = Color(0xFF050B14), surface = Color(0xFF0C1826))) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(20.dp).statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("J.A.R.V.I.S.", color = Blue, style = MaterialTheme.typography.titleLarge)
                Core(animate = JarvisState.running && !driving, active = JarvisState.running)
                Text(JarvisState.status)
                if (JarvisState.last.isNotBlank()) Text("You: ${JarvisState.last}", color = Color(0xFF7F97AD))
                Button(onClick = {
                    save()
                    if (JarvisState.running) {
                        ctx.startService(Intent(ctx, JarvisService::class.java).setAction(JarvisService.ACTION_STOP))
                    } else if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        perms.launch(arrayOf(
                            Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS,
                            Manifest.permission.CALL_PHONE, Manifest.permission.READ_CONTACTS
                        ))
                    } else {
                        ContextCompat.startForegroundService(ctx, Intent(ctx, JarvisService::class.java))
                    }
                }) { Text(if (JarvisState.running) "Disable microphone" else "Enable Jarvis") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Driving mode (short answers)"); Spacer(Modifier.width(8.dp))
                    Switch(driving, { driving = it; save() })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Listen in Malayalam"); Spacer(Modifier.width(8.dp))
                    Switch(ml, { ml = it; save() })
                }
                OutlinedTextField(key, { key = it }, label = { Text("Picovoice AccessKey") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(url, { url = it }, label = { Text("AI proxy URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(token, { token = it }, label = { Text("Proxy token") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
