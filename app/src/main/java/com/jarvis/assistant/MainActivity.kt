package com.jarvis.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import android.graphics.Color
import android.graphics.Typeface

class MainActivity : Activity() {

    private val permissions = mutableListOf(
        Manifest.permission.RECORD_AUDIO
    ).apply {
        if (Build.VERSION.SDK_INT >= 33) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.rgb(12, 17, 28))
        }

        val title = TextView(this).apply {
            text = "J A R V I S"
            textSize = 34f
            gravity = Gravity.CENTER
            setTextColor(Color.CYAN)
            setTypeface(null, Typeface.BOLD)
        }

        val subtitle = TextView(this).apply {
            text = "Personal Voice Assistant\n\nSay: Hey Jarvis"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        }

        val start = Button(this).apply {
            text = "START JARVIS"
            setOnClickListener {
                if (hasPermissions()) {
                    startJarvis()
                } else {
                    requestPermissions(
                        permissions.toTypedArray(), 100
                    )
                }
            }
        }

        val stop = Button(this).apply {
            text = "STOP JARVIS"
            setOnClickListener {
                stopService(
                    Intent(
                        this@MainActivity,
                        JarvisService::class.java
                    )
                )
            }
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(start)
        layout.addView(stop)

        setContentView(layout)

        if (!hasPermissions()) {
            requestPermissions(
                permissions.toTypedArray(), 100
            )
        }
    }

    private fun hasPermissions(): Boolean {
        return permissions.all {
            checkSelfPermission(it) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startJarvis() {
        val intent = Intent(
            this,
            JarvisService::class.java
        )

        startForegroundService(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (
            requestCode == 100 &&
            hasPermissions()
        ) {
            startJarvis()
        }
    }
}
