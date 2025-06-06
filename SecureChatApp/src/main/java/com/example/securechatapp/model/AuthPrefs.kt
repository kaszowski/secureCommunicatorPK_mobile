package com.example.securechatapp.model

import android.content.Context
import android.content.SharedPreferences

class AuthPrefs(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    var authToken: String?
        get() = prefs.getString("token", null)
        set(value) = prefs.edit().putString("token", value).apply()

    var tokenExpiry: Long?
        get() = prefs.getLong("token_expiry", 0).takeIf { it > 0 }
        set(value) = value?.let { prefs.edit().putLong("token_expiry", it).apply() }!!

    fun clear() {
        prefs.edit().clear().apply()
    }
}