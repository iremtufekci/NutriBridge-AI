package com.example.nightbrate // Paket tanımı

import android.os.Bundle // Activity durum verisi
import android.widget.TextView // Metin etiketi
import androidx.appcompat.app.AppCompatActivity // Temel Activity

class ClientPlaceholderActivity : AppCompatActivity() { // Danışan geçici/yapım aşaması ekranı
    override fun onCreate(savedInstanceState: Bundle?) { // Activity oluşturulurken
        super.onCreate(savedInstanceState) // Üst sınıf başlatması
        setContentView(R.layout.activity_client_placeholder) // Placeholder layout yükle
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty() // Başlık metni
        val message = intent.getStringExtra(EXTRA_MESSAGE).orEmpty() // Açıklama metni
        val index = intent.getIntExtra(EXTRA_INDEX, 1) // Seçili alt sekme indeksi
        findViewById<TextView>(R.id.placeholderTitle).text = title // Başlığı göster
        findViewById<TextView>(R.id.placeholderMessage).text = message // Mesajı göster
        ClientBottomBarHelper.bind(this, index) // Alt navigasyonu bağla
    }

    companion object { // Intent extra sabitleri
        const val EXTRA_INDEX = "extra_index" // Sekme indeksi anahtarı
        const val EXTRA_TITLE = "extra_title" // Başlık anahtarı
        const val EXTRA_MESSAGE = "extra_message" // Mesaj anahtarı
    }
}
