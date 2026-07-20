package com.example.nightbrate // Paket tanımı

import android.content.Intent // Activity geçişi
import androidx.appcompat.app.AppCompatActivity // Temel Activity
import androidx.core.content.ContextCompat // Renk kaynağı çözümleme

object DietitianBottomBarHelper { // Diyetisyen alt navigasyon çubuğu yardımcısı
    private val tabIds = listOf( // Tıklanabilir sekme konteynerleri
        R.id.dtab0, R.id.dtab1, R.id.dtab2, R.id.dtab3, R.id.dtab4, R.id.dtab5
    )
    private val iconIds = listOf( // Sekme ikon görünümleri
        R.id.dicon0, R.id.dicon1, R.id.dicon2, R.id.dicon3, R.id.dicon4, R.id.dicon5
    )
    private val labelIds = listOf( // Sekme etiket görünümleri
        R.id.dlabel0, R.id.dlabel1, R.id.dlabel2, R.id.dlabel3, R.id.dlabel4, R.id.dlabel5
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
                .setOnClickListener { DietitianTabNav.go(activity, index) } // Sekme navigasyonu
        }

    }
}

object DietitianTabNav { // Diyetisyen sekme yönlendirme mantığı
    fun go(activity: AppCompatActivity, index: Int) { // Sekme indeksine göre Activity aç
        if (index == 0) { // Ana panel
            if (activity is DietitianDashboardActivity) return // Zaten paneldeyse çık
            activity.startActivity( // Dashboard'a git
                Intent(activity, DietitianDashboardActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            return
        }
        if (index == 1) { // Danışanlar
            if (activity is DietitianClientsActivity) return // Zaten danışan listesindeyse çık
            activity.startActivity( // Danışanlar ekranı
                Intent(activity, DietitianClientsActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            if (activity is DietitianPlaceholderActivity) activity.finish() // Placeholder kapat
            if (activity is DietitianProfileActivity) activity.finish() // Profil kapat
            if (activity !is DietitianDashboardActivity) activity.finish() // Diğer ekranları kapat
            return
        }
        if (index == 2) { // Programlar
            if (activity is DietitianProgramsActivity) return // Zaten programlardaysa çık
            activity.startActivity( // Programlar ekranı
                Intent(activity, DietitianProgramsActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            if (activity is DietitianPlaceholderActivity) activity.finish()
            if (activity is DietitianProfileActivity) activity.finish()
            if (activity !is DietitianDashboardActivity) activity.finish()
            return
        }
        if (index == 5) { // Profil/hesap
            if (activity is DietitianProfileActivity) return // Zaten profildeyse çık
            activity.startActivity( // Profil ekranı
                Intent(activity, DietitianProfileActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            if (activity is DietitianPlaceholderActivity) activity.finish()
            if (activity !is DietitianDashboardActivity) activity.finish()
            return
        }
        if (index == 3) { // Yapay zeka inceleme
            if (activity is DietitianAiReviewActivity) return // Zaten incelemedeyse çık
            activity.startActivity( // AI inceleme ekranı
                Intent(activity, DietitianAiReviewActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            if (activity is DietitianPlaceholderActivity) activity.finish()
            if (activity is DietitianProfileActivity) activity.finish()
            if (activity !is DietitianDashboardActivity) activity.finish()
            return
        }
        if (index == 4) { // Kritik uyarılar
            if (activity is DietitianCriticalAlertsActivity) return // Zaten uyarılardaysa çık
            activity.startActivity( // Kritik uyarılar ekranı
                Intent(activity, DietitianCriticalAlertsActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            if (activity is DietitianPlaceholderActivity) activity.finish()
            if (activity is DietitianProfileActivity) activity.finish()
            if (activity !is DietitianDashboardActivity) activity.finish()
            return
        }
    }
}
