package com.example.nightbrate // Paket tanımı

import android.content.SharedPreferences // Kalıcı ayar deposu

object ThemeUtils { // Tema ve rol yardımcıları
    const val PREF_NAME = "auth" // SharedPreferences dosya adı
    const val KEY_THEME = "theme" // Tema tercihi anahtarı
    const val KEY_ROLE = "role" // Kullanıcı rolü anahtarı

    fun isDietitianRole(role: String?): Boolean = // Rol diyetisyen mi?
        role.equals("dietitian", ignoreCase = true) || // İngilizce rol adı
            role.equals("diyetisyen", ignoreCase = true) // Türkçe rol adı

    fun applyOnAppStart(prefs: SharedPreferences) { // Uygulama açılışında tema uygula
        persistLightOnly(prefs) // Yalnızca açık tema kaydet
    }

    fun applyLightTheme(prefs: SharedPreferences) { // Açık temayı zorla uygula
        persistLightOnly(prefs) // Tercihi kalıcı olarak light yap
    }

    private fun persistLightOnly(prefs: SharedPreferences) { // Tema tercihini light olarak yaz
        prefs.edit().putString(KEY_THEME, "light").apply() // Asenkron kayıt
    }
}
