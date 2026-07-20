package com.example.nightbrate // Paket tanımı

import android.graphics.drawable.ColorDrawable // Düz renk arka plan çizimi
import android.view.View // Görünüm referansı
import android.view.WindowManager // Pencere bayrakları
import androidx.annotation.ColorRes // Renk kaynak kimliği
import androidx.appcompat.app.AppCompatActivity // Temel Activity
import androidx.core.content.ContextCompat // Kaynak renk çözümleme
import androidx.core.view.WindowCompat // Edge-to-edge uyumluluk

object ActivityWindowHelper { // Pencere/durum çubuğu yardımcıları
    /**
     * Hedef 35+ / önizleme emülatörleri: edge-to-edge ile pencere siyah kalabiliyor; arka planı açık renge sabitler.
     */
    fun AppCompatActivity.applyStandardContentWindow(@ColorRes backgroundColor: Int = R.color.client_scaffold) { // Standart pencere renkleri
        WindowCompat.setDecorFitsSystemWindows(window, true) // Sistem çubuklarına içerik sığdır
        @Suppress("DEPRECATION")
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS) // Yarı saydam durum çubuğunu kapat
        @Suppress("DEPRECATION")
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION) // Yarı saydam nav çubuğunu kapat
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS) // Sistem çubuklarını boyayabil
        val color = ContextCompat.getColor(this, backgroundColor) // Arka plan rengini çöz
        window.decorView.setBackgroundColor(color) // Dekor arka planı
        @Suppress("DEPRECATION")
        window.statusBarColor = color // Durum çubuğu rengi
        @Suppress("DEPRECATION")
        window.navigationBarColor = ContextCompat.getColor(this, R.color.nav_bar_bg) // Alt navigasyon rengi
        @Suppress("DEPRECATION")
        window.setBackgroundDrawable(ColorDrawable(color)) // Pencere arka plan drawable
    }

    /** [setContentView] sonrası çağrılmalı; `content` kökünün da çizildiğinden emin olur. */
    fun AppCompatActivity.applyContentRootBackground(@ColorRes backgroundColor: Int = R.color.client_scaffold) { // İçerik kökü arka planı
        val color = ContextCompat.getColor(this, backgroundColor) // Renk kaynağını al
        findViewById<View>(android.R.id.content).setBackgroundColor(color) // content kökünü boya
    }
}
