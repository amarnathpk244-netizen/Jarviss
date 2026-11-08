package com.jarvis.assistant

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Encrypted local settings. */
class Prefs private constructor(ctx: Context) {
    private val sp = EncryptedSharedPreferences.create(
        ctx.applicationContext, "jarvis_secure",
        MasterKey.Builder(ctx.applicationContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    var accessKey: String
        get() = sp.getString("pv", "") ?: ""
        set(v) = sp.edit().putString("pv", v).apply()
    var proxyUrl: String
        get() = sp.getString("url", BuildConfig.PROXY_URL) ?: BuildConfig.PROXY_URL
        set(v) = sp.edit().putString("url", v).apply()
    var token: String
        get() = sp.getString("tok", "") ?: ""
        set(v) = sp.edit().putString("tok", v).apply()
    var lang: String
        get() = sp.getString("lang", "en-IN") ?: "en-IN"
        set(v) = sp.edit().putString("lang", v).apply()
    var driving: Boolean
        get() = sp.getBoolean("drive", false)
        set(v) = sp.edit().putBoolean("drive", v).apply()

    companion object { fun get(ctx: Context) = Prefs(ctx) }
}
