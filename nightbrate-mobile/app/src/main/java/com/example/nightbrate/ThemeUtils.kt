package com.example.nightbrate

import android.content.SharedPreferences

object ThemeUtils {
    const val PREF_NAME = "auth"
    const val KEY_THEME = "theme"
    const val KEY_ROLE = "role"

    fun isDietitianRole(role: String?): Boolean =
        role.equals("dietitian", ignoreCase = true) ||
            role.equals("diyetisyen", ignoreCase = true)

    fun applyOnAppStart(prefs: SharedPreferences) {
        persistLightOnly(prefs)
    }

    fun applyLightTheme(prefs: SharedPreferences) {
        persistLightOnly(prefs)
    }

    private fun persistLightOnly(prefs: SharedPreferences) {
        prefs.edit().putString(KEY_THEME, "light").apply()
    }
}
