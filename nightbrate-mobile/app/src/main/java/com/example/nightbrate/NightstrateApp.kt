package com.example.nightbrate

import android.app.Application

class NightstrateApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: NightstrateApp
            private set
    }
}
