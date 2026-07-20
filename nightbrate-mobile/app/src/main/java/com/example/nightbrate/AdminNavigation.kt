package com.example.nightbrate // Paket tanımı

import android.content.Intent // Activity geçişi
import androidx.appcompat.app.AppCompatActivity // Temel Activity
import androidx.core.content.ContextCompat // Renk kaynağı çözümleme

object AdminBottomBarHelper { // Admin alt navigasyon çubuğu yardımcısı
    private val tabIds = listOf( // Tıklanabilir sekme konteynerleri
        R.id.atab0, R.id.atab1, R.id.atab2, R.id.atab3, R.id.atab4
    )
    private val iconIds = listOf( // Sekme ikon görünümleri
        R.id.aicon0, R.id.aicon1, R.id.aicon2, R.id.aicon3, R.id.aicon4
    )
    private val labelIds = listOf( // Sekme etiket görünümleri
        R.id.alabel0, R.id.alabel1, R.id.alabel2, R.id.alabel3, R.id.alabel4
    )

    fun styleTabs(activity: AppCompatActivity, selectedIndex: Int) { // Sekme renklerini güncelle
        val active = ContextCompat.getColor(activity, R.color.nav_item_active) // Seçili sekme rengi
        val inactive = ContextCompat.getColor(activity, R.color.nav_item_inactive) // Pasif sekme rengi
        iconIds.forEachIndexed { i, id -> // İkon renkleri
            activity.findViewById<android.widget.TextView>(id)
                .setTextColor(if (i == selectedIndex) active else inactive)
        }
        labelIds.forEachIndexed { i, id -> // Etiket renkleri
            activity.findViewById<android.widget.TextView>(id)
                .setTextColor(if (i == selectedIndex) active else inactive)
        }
    }

    fun bind(activity: AppCompatActivity, selectedIndex: Int) { // Sekmeleri stil + tıklama ile bağla
        styleTabs(activity, selectedIndex) // Görsel stili uygula
        tabIds.forEachIndexed { index, viewId -> // Her sekmeye dinleyici
            activity.findViewById<android.widget.LinearLayout>(viewId)
                .setOnClickListener { AdminTabNav.go(activity, index) } // Sekme navigasyonu
        }
    }
}

object AdminTabNav { // Admin sekme yönlendirme mantığı
    fun go(activity: AppCompatActivity, index: Int) { // Sekme indeksine göre Activity aç
        if (index == 0) { // Ana panel
            if (activity is AdminDashboardActivity) return // Zaten paneldeyse çık
            activity.startActivity( // Dashboard'a git
                Intent(activity, AdminDashboardActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            if (activity is AdminDietitianApprovalsActivity) activity.finish() // Diğer admin ekranlarını kapat
            if (activity is AdminSettingsActivity) activity.finish()
            if (activity is AdminSystemAnalyticsActivity) activity.finish()
            if (activity is AdminPlaceholderActivity) activity.finish()
            if (activity is AdminUserManagementActivity) activity.finish()
            return
        }
        if (index == 1) { // Kullanıcı yönetimi
            if (activity is AdminUserManagementActivity) return // Zaten kullanıcı yönetimindeyse çık
            activity.startActivity(Intent(activity, AdminUserManagementActivity::class.java)) // Kullanıcı listesi
            if (activity is AdminPlaceholderActivity) activity.finish()
            if (activity is AdminDietitianApprovalsActivity) activity.finish()
            if (activity is AdminSystemAnalyticsActivity) activity.finish()
            if (activity is AdminSettingsActivity) activity.finish()
            if (activity !is AdminDashboardActivity) activity.finish()
            return
        }
        if (index == 2) { // Diyetisyen onayları
            if (activity is AdminDietitianApprovalsActivity) return // Zaten onay ekranındaysa çık
            activity.startActivity(Intent(activity, AdminDietitianApprovalsActivity::class.java)) // Onay listesi
            if (activity is AdminPlaceholderActivity) activity.finish()
            if (activity is AdminSystemAnalyticsActivity) activity.finish()
            if (activity is AdminSettingsActivity) activity.finish()
            if (activity is AdminUserManagementActivity) activity.finish()
            if (activity !is AdminDashboardActivity) activity.finish()
            return
        }
        if (index == 3) { // Sistem analitiği
            if (activity is AdminSystemAnalyticsActivity) return // Zaten analitikteyse çık
            activity.startActivity(Intent(activity, AdminSystemAnalyticsActivity::class.java)) // Analitik ekranı
            if (activity is AdminPlaceholderActivity) activity.finish()
            if (activity is AdminSettingsActivity) activity.finish()
            if (activity is AdminDietitianApprovalsActivity) activity.finish()
            if (activity is AdminUserManagementActivity) activity.finish()
            if (activity !is AdminDashboardActivity) activity.finish()
            return
        }
        if (index == 4) { // Ayarlar
            if (activity is AdminSettingsActivity) return // Zaten ayarlardaysa çık
            activity.startActivity(Intent(activity, AdminSettingsActivity::class.java)) // Ayarlar ekranı
            if (activity is AdminPlaceholderActivity) activity.finish()
            if (activity is AdminDietitianApprovalsActivity) activity.finish()
            if (activity is AdminSystemAnalyticsActivity) activity.finish()
            if (activity is AdminUserManagementActivity) activity.finish()
            if (activity !is AdminDashboardActivity) activity.finish()
            return
        }
        if (activity is AdminPlaceholderActivity) { // Placeholder üzerinden sekme kontrolü
            val t = activity.intent.getIntExtra(AdminPlaceholderActivity.EXTRA_INDEX, -1) // Mevcut placeholder indeksi
            if (t == index) return // Aynı sekmeye tekrar tıklanırsa çık
        }
    }
}
