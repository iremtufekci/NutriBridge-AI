package com.example.nightbrate

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeUtils {
    const val PREF_NAME = "auth"
    const val KEY_THEME = "theme"

    fun applyAndPersist(prefs: android.content.SharedPreferences, mode: String) {
        val t = if (mode.equals("dark", ignoreCase = true)) "dark" else "light"
        prefs.edit().putString(KEY_THEME, t).apply()
        applyImmediate(t)
    }

    fun applyImmediate(mode: String) {
        if (mode == "dark")
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        else
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    fun fromProfile(themePreference: String?): String =
        if (themePreference?.equals("dark", ignoreCase = true) == true) "dark" else "light"
}
