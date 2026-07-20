package com.example.nightbrate // Paket tanımı

import android.app.Application // Uygulama yaşam döngüsü sınıfı
import androidx.appcompat.app.AppCompatDelegate // Tema/gece modu delegesi
import com.example.nightbrate.ThemeUtils.PREF_NAME // Auth prefs sabiti

class NightstrateApp : Application() { // Uygulama giriş noktası
    override fun onCreate() { // Uygulama ilk oluşturulduğunda
        Diagnostic.installUncaughtExceptionLogger() // Yakalanmamış hata günlüğü
        super.onCreate() // Üst sınıf başlatması
        Diagnostic.log("Application.onCreate (after super)") // Debug log
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) // Gece modunu kapat
        instance = this // Singleton örneğini ata
        val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE) // Auth ayarlarını oku
        ThemeUtils.applyOnAppStart(prefs) // Açılış temasını uygula
        Diagnostic.log("Application.onCreate done") // Başlatma tamamlandı logu
    }

    companion object { // Statik uygulama erişimi
        lateinit var instance: NightstrateApp // Global Application örneği
            private set // Dışarıdan yalnızca okunabilir
    }
}
