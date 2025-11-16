package com.edu.wszib.findyourpet

import android.content.Context

class PrefsManager(context: Context) {
    private val prefs = context.getSharedPreferences("findyourpet_prefs", Context.MODE_PRIVATE)

    fun isConsentGiven(): Boolean = prefs.getBoolean("rodo_consent", false)

    fun setConsentGiven(consent: Boolean) {
        prefs.edit().putBoolean("rodo_consent", consent).apply()
    }

    fun setConsentGiven() {
        setConsentGiven(true)
    }
}