package com.example.nightbrate // Paket tanımı

import android.os.Process // İşlem kimliği
import android.util.Log // Android log API

/**
 * Logcat filtresi: **NightbrateDbg** (tam metin veya `tag:NightbrateDbg`).
 * Siyah ekranda bile hangi yaşam döngüsü satırına gelindiği buradan okunur.
 */
object Diagnostic { // Uygulama geneli debug günlüğü
    const val TAG = "NightbrateDbg" // Logcat etiketi

    private var uncaughtInstalled = false // Yakalanmamış handler kuruldu mu

    fun log(msg: String) { // Bilgi seviyesi log
        Log.i(TAG, "[pid=${Process.myPid()}] $msg") // PID ile birlikte yaz
    }

    fun logE(msg: String, t: Throwable? = null) { // Hata seviyesi log
        if (t != null) Log.e(TAG, "[pid=${Process.myPid()}] $msg", t) // Exception ile
        else Log.e(TAG, "[pid=${Process.myPid()}] $msg") // Yalnızca mesaj
    }

    /** Kırmızı FATAL çıkmadan sessiz düşüşleri yakalamak için (çok nadır). */
    fun installUncaughtExceptionLogger() { // Global exception handler kur
        if (uncaughtInstalled) return // Zaten kuruluysa çık
        uncaughtInstalled = true // Tekrar kurulumu engelle
        val previous = Thread.getDefaultUncaughtExceptionHandler() // Önceki handler
        Thread.setDefaultUncaughtExceptionHandler { thread, exception -> // Yeni handler
            logE("UNCAUGHT thread=${thread.name}", exception) // Hatayı logla
            previous?.uncaughtException(thread, exception) // Varsayılan davranışı sürdür
        }
    }
}
