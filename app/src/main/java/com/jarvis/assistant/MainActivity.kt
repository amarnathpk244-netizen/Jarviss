package com.jarvis.assistant

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    companion object { var visible = false; private set }
    private lateinit var statusText: TextView
    private var pendingTalk = false
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            statusText.text = intent?.getStringExtra("status").orEmpty()
        }
    }
    private fun prefs() = getSharedPreferences(JarvisService.PREFS, MODE_PRIVATE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.rgb(12, 17, 28))
        }
        fun label(value: String, size: Float = 18f) = TextView(this).apply {
            text = value; textSize = size; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
            setPadding(0, 12, 0, 12)
        }
        layout.addView(label("J A R V I S", 34f))
        layout.addView(label("Say Hey Jarvis → wait for reply → say your command"))
        val language = Spinner(this)
        language.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Malayalam commands", "English commands"))
        language.setSelection(if (prefs().getString("language", "ml-IN") == "en-IN") 1 else 0)
        language.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                prefs().edit().putString("language", if (position == 1) "en-IN" else "ml-IN").apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        layout.addView(language)
        fun button(title: String, action: () -> Unit) {
            layout.addView(Button(this).apply { text = title; setOnClickListener { action() } })
        }
        button("START JARVIS") { begin(false) }
        button("TALK NOW") { begin(true) }
        button("STOP JARVIS") {
            stopService(Intent(this, JarvisService::class.java))
            prefs().edit().putString("status", "Jarvis stopped.").apply()
            statusText.text = "Jarvis stopped."
        }
        statusText = label("Press START to begin.", 16f)
        layout.addView(statusText)
        layout.addView(label("Try: YouTube തുറക്കൂ / Open YouTube\nസമയം എത്രയായി / What is the time?\nKeep Jarvis visible for direct app opening.", 15f))
        setContentView(ScrollView(this).apply { addView(layout) })
    }
    private fun begin(talk: Boolean) {
        pendingTalk = talk
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            val required = mutableListOf(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33) required.add(Manifest.permission.POST_NOTIFICATIONS)
            requestPermissions(required.toTypedArray(), 100)
        } else {
            startJarvis()
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
        }
    }
    private fun startJarvis() {
        val intent = Intent(this, JarvisService::class.java)
        if (pendingTalk) intent.action = JarvisService.TALK
        try { startForegroundService(intent) }
        catch (_: Exception) { statusText.text = "Unable to start microphone service. Check microphone permission and try again." }
    }
    override fun onResume() {
        super.onResume()
        visible = true
        val filter = IntentFilter(JarvisService.STATUS)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
        else registerReceiver(receiver, filter)
        statusText.text = prefs().getString("status", "Press START to begin.")
    }
    override fun onPause() {
        visible = false
        unregisterReceiver(receiver)
        super.onPause()
    }
    override fun onRequestPermissionsResult(code: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, permissions, results)
        if (code == 100) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startJarvis()
            else statusText.text = "Microphone permission is required. Allow it in Android app settings."
        }
    }
}
