package com.example.nightbrate

import android.app.Application
import com.example.nightbrate.ThemeUtils.PREF_NAME

class NightstrateApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        ThemeUtils.applyOnAppStart(prefs)
    }

    companion object {
        lateinit var instance: NightstrateApp
            private set
    }
}
