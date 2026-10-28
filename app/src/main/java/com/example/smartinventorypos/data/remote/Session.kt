package com.example.smartinventorypos.data.remote

import android.content.Context

object Session {
    private lateinit var prefs: android.content.SharedPreferences
    fun init(c: Context) { prefs = c.getSharedPreferences("session", Context.MODE_PRIVATE) }
    var token: String?
        get() = prefs.getString("token", null)
        set(v) { prefs.edit().putString("token", v).apply() }
    var role: String?
        get() = prefs.getString("role", null)
        set(v) { prefs.edit().putString("role", v).apply() }
}