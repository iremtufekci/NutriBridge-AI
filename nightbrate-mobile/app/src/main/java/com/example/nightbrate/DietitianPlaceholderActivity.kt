package com.example.nightbrate // Paket tanımı

import android.os.Bundle // Activity durum verisi
import android.widget.TextView // Metin etiketi
import androidx.appcompat.app.AppCompatActivity // Temel Activity

class DietitianPlaceholderActivity : AppCompatActivity() { // Diyetisyen geçici/yapım aşaması ekranı
    override fun onCreate(savedInstanceState: Bundle?) { // Activity oluşturulurken
        super.onCreate(savedInstanceState) // Üst sınıf başlatması
        setContentView(R.layout.activity_dietitian_placeholder) // Placeholder layout yükle
        val index = intent.getIntExtra(EXTRA_INDEX, 1) // Seçili alt sekme indeksi
        findViewById<TextView>(R.id.dPlaceholderTitle).text = // Başlık metnini ayarla
            intent.getStringExtra(EXTRA_TITLE).orEmpty() // Intent'ten başlık al
        findViewById<TextView>(R.id.dPlaceholderMessage).text = // Mesaj metnini ayarla
            intent.getStringExtra(EXTRA_MESSAGE).orEmpty() // Intent'ten mesaj al
        DietitianBottomBarHelper.bind(this, index) // Alt navigasyonu bağla
    }

    companion object { // Intent extra sabitleri
        const val EXTRA_INDEX = "extra_d_index" // Sekme indeksi anahtarı
        const val EXTRA_TITLE = "extra_d_title" // Başlık anahtarı
        const val EXTRA_MESSAGE = "extra_d_message" // Mesaj anahtarı
    }
}
