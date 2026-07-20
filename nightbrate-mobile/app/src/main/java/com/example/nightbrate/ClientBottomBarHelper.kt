package com.example.nightbrate // Paket tanımı

import android.content.Intent // Activity geçişi
import androidx.appcompat.app.AppCompatActivity // Temel Activity
import androidx.core.content.ContextCompat // Renk kaynağı çözümleme

object ClientBottomBarHelper { // Danışan alt navigasyon çubuğu yardımcısı

    fun styleTabs(activity: AppCompatActivity, selectedIndex: Int) { // Sekme renklerini güncelle
        val active = ContextCompat.getColor(activity, R.color.nav_item_active) // Seçili sekme rengi
        val inactive = ContextCompat.getColor(activity, R.color.nav_item_inactive) // Pasif sekme rengi
        val icons = listOf( // İkon görünüm kimlikleri
            R.id.iconHome, R.id.iconJournal, R.id.iconPast, R.id.iconFood,
            R.id.iconPdf, R.id.iconChef, R.id.iconShares, R.id.iconProfile
        )
        val labels = listOf( // Etiket görünüm kimlikleri
            R.id.labelHome, R.id.labelJournal, R.id.labelPast, R.id.labelFood,
            R.id.labelPdf, R.id.labelChef, R.id.labelShares, R.id.labelProfile
        )
        icons.forEachIndexed { i, id -> // Her ikon için renk uygula
            activity.findViewById<android.widget.TextView>(id)
                .setTextColor(if (i == selectedIndex) active else inactive) // Seçili/pasif renk
        }
        labels.forEachIndexed { i, id -> // Her etiket için renk uygula
            activity.findViewById<android.widget.TextView>(id)
                .setTextColor(if (i == selectedIndex) active else inactive) // Seçili/pasif renk
        }
    }

    fun bind(activity: AppCompatActivity, selectedIndex: Int) { // Sekmeleri stil + tıklama ile bağla
        styleTabs(activity, selectedIndex) // Önce görsel stili uygula
        listOf( // Tıklanabilir sekme konteynerleri
            R.id.tabHome, R.id.tabJournal, R.id.tabPast, R.id.tabFood,
            R.id.tabPdf, R.id.tabChef, R.id.tabShares, R.id.tabProfile
        ).forEachIndexed { index, viewId -> // Her sekmeye dinleyici ekle
            activity.findViewById<android.widget.LinearLayout>(viewId)
                .setOnClickListener { ClientTabNav.go(activity, index) } // Sekme navigasyonu
        }
    }
}

object ClientTabNav { // Danışan sekme yönlendirme mantığı
    private const val TAB_HOME = 0 // Ana sayfa sekmesi
    private const val TAB_JOURNAL = 1 // Günlük program sekmesi
    private const val TAB_PAST = 2 // Geçmiş programlar sekmesi
    private const val TAB_FOOD = 3 // Öğün analizi sekmesi
    private const val TAB_PDF = 4 // PDF analizi sekmesi
    private const val TAB_CHEF = 5 // Yapay zeka mutfak sekmesi
    private const val TAB_SHARES = 6 // Paylaşımlar sekmesi
    private const val TAB_PROFILE = 7 // Profil sekmesi

    fun go(activity: AppCompatActivity, index: Int) { // Sekme indeksine göre Activity aç
        if (index == TAB_HOME) { // Ana sayfa
            if (activity is ClientDashboardActivity) return // Zaten ana sayfadaysa çık
            activity.startActivity( // Dashboard'a git
                Intent(activity, ClientDashboardActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP) // Tek örnek
            )
            if (activity is ClientDietProgramHistoryActivity) activity.finish() // Geçmiş ekranını kapat
            if (activity is ClientAiKitchenActivity || activity is ClientAiKitchenSharesActivity) activity.finish() // Mutfak ekranlarını kapat
            if (activity is ClientMealAnalysisActivity) activity.finish() // Öğün analizini kapat
            if (activity is ClientPdfAnalysisActivity) activity.finish() // PDF analizini kapat
            return
        }
        if (index == TAB_CHEF) { // Yapay zeka mutfak
            if (activity is ClientAiKitchenActivity) return // Zaten mutfaktaysa çık
            if (activity is ClientAiKitchenSharesActivity) { // Paylaşımlardan mutfağa dönüş
                activity.startActivity(
                    Intent(activity, ClientAiKitchenActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                )
                activity.finish() // Paylaşım ekranını kapat
                return
            }
            activity.startActivity( // Mutfak ekranını aç
                Intent(activity, ClientAiKitchenActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            if (activity is ClientDietProgramActivity) activity.finish() // Diğer ekranları kapat
            if (activity is ClientDietProgramHistoryActivity) activity.finish()
            if (activity is ClientProfileActivity) activity.finish()
            if (activity is ClientPlaceholderActivity) activity.finish()
            if (activity is ClientMealAnalysisActivity) activity.finish()
            if (activity is ClientPdfAnalysisActivity) activity.finish()
            return
        }
        if (index == TAB_SHARES) { // Paylaşımlar
            if (activity is ClientAiKitchenSharesActivity) return // Zaten paylaşımlardaysa çık
            activity.startActivity( // Paylaşım listesini aç
                Intent(activity, ClientAiKitchenSharesActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            if (activity is ClientDietProgramActivity) activity.finish() // Diğer ekranları kapat
            if (activity is ClientDietProgramHistoryActivity) activity.finish()
            if (activity is ClientProfileActivity) activity.finish()
            if (activity is ClientPlaceholderActivity) activity.finish()
            if (activity is ClientMealAnalysisActivity) activity.finish()
            if (activity is ClientAiKitchenActivity) activity.finish()
            if (activity is ClientPdfAnalysisActivity) activity.finish()
            return
        }
        if (index == TAB_PROFILE) { // Profil
            if (activity is ClientProfileActivity) return // Zaten profildeyse çık
            activity.startActivity(Intent(activity, ClientProfileActivity::class.java)) // Profil ekranı
            if (activity is ClientAiKitchenActivity || activity is ClientAiKitchenSharesActivity) activity.finish() // Mutfak ekranlarını kapat
            if (activity !is ClientDashboardActivity) activity.finish() // Dashboard dışındakileri kapat
            return
        }
        if (index == TAB_JOURNAL) { // Günlük program
            if (activity is ClientDietProgramActivity) return // Zaten günlükteyse çık
            activity.startActivity( // Günlük program ekranı
                Intent(activity, ClientDietProgramActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            if (activity is ClientDietProgramHistoryActivity) activity.finish() // Diğer ekranları kapat
            if (activity is ClientPlaceholderActivity) activity.finish()
            if (activity is ClientAiKitchenActivity || activity is ClientAiKitchenSharesActivity) activity.finish()
            if (activity is ClientProfileActivity) activity.finish()
            if (activity is ClientMealAnalysisActivity) activity.finish()
            if (activity is ClientPdfAnalysisActivity) activity.finish()
            if (activity !is ClientDashboardActivity) activity.finish()
            return
        }
        if (index == TAB_PAST) { // Geçmiş programlar
            if (activity is ClientDietProgramHistoryActivity) return // Zaten geçmişteyse çık
            activity.startActivity( // Geçmiş program ekranı
                Intent(activity, ClientDietProgramHistoryActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            if (activity is ClientDietProgramActivity) activity.finish() // Diğer ekranları kapat
            if (activity is ClientPlaceholderActivity) activity.finish()
            if (activity is ClientProfileActivity) activity.finish()
            if (activity is ClientAiKitchenActivity || activity is ClientAiKitchenSharesActivity) activity.finish()
            if (activity is ClientMealAnalysisActivity) activity.finish()
            if (activity is ClientPdfAnalysisActivity) activity.finish()
            if (activity !is ClientDashboardActivity) activity.finish()
            return
        }
        if (index == TAB_FOOD) { // Öğün fotoğraf analizi
            if (activity is ClientMealAnalysisActivity) return // Zaten analiz ekranındaysa çık
            activity.startActivity( // Öğün analiz ekranı
                Intent(activity, ClientMealAnalysisActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            if (activity is ClientDietProgramActivity) activity.finish() // Diğer ekranları kapat
            if (activity is ClientDietProgramHistoryActivity) activity.finish()
            if (activity is ClientProfileActivity) activity.finish()
            if (activity is ClientPlaceholderActivity) activity.finish()
            if (activity is ClientAiKitchenActivity || activity is ClientAiKitchenSharesActivity) activity.finish()
            if (activity is ClientPdfAnalysisActivity) activity.finish()
            return
        }
        if (index == TAB_PDF) { // PDF analizi
            if (activity is ClientPdfAnalysisActivity) return // Zaten PDF ekranındaysa çık
            activity.startActivity( // PDF analiz ekranı
                Intent(activity, ClientPdfAnalysisActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            if (activity is ClientDietProgramActivity) activity.finish() // Diğer ekranları kapat
            if (activity is ClientDietProgramHistoryActivity) activity.finish()
            if (activity is ClientProfileActivity) activity.finish()
            if (activity is ClientPlaceholderActivity) activity.finish()
            if (activity is ClientAiKitchenActivity || activity is ClientAiKitchenSharesActivity) activity.finish()
            if (activity is ClientMealAnalysisActivity) activity.finish()
            return
        }
    }
}
