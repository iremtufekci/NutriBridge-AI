package com.example.nightbrate

import android.app.Application
import com.example.nightbrate.ThemeUtils.PREF_NAME

class NightstrateApp : Application() {
    override fun onCreate() {
        val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        val t = prefs.getString(ThemeUtils.KEY_THEME, "light")
        ThemeUtils.applyImmediate(ThemeUtils.fromProfile(t))
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: NightstrateApp
            private set
    }
}
