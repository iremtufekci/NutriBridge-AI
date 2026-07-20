package com.example.nightbrate // Paket tanımı

import android.os.Bundle // Activity durum verisi
import android.widget.TextView // Metin etiketi
import android.widget.Toast // Kısa bildirim
import androidx.appcompat.app.AppCompatActivity // Temel Activity
import androidx.lifecycle.lifecycleScope // Activity ömrüne bağlı coroutine
import kotlinx.coroutines.launch // Arka planda API çağrısı
import java.util.Locale // Büyük/küçük harf dönüşümü

class AdminSettingsActivity : AppCompatActivity() { // Admin hesap ayarları ekranı
    override fun onCreate(savedInstanceState: Bundle?) { // Activity oluşturulurken
        super.onCreate(savedInstanceState) // Üst sınıf başlatması
        setContentView(R.layout.activity_admin_settings) // Ayarlar layout yükle
        AdminBottomBarHelper.bind(this, 4) // Ayarlar sekmesini seçili göster
        loadAccountInfo() // Hesap bilgilerini API'den çek
    }

    private fun loadAccountInfo() { // Oturum açmış admin profilini yükle
        lifecycleScope.launch { // Coroutine başlat
            try { // Ağ hatalarını yakala
                val r = RetrofitClient.instance.getCurrentUserProfile() // Profil API çağrısı
                if (!r.isSuccessful) { // HTTP hata durumu
                    Toast.makeText( // Kullanıcıya bildir
                        this@AdminSettingsActivity,
                        "Hesap bilgisi yüklenemedi",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch // Coroutine'den çık
                }
                val p = r.body() ?: return@launch // Boş gövde kontrolü
                val name = p.displayName?.trim().orEmpty().ifEmpty { p.email.orEmpty() } // Görünen ad
                findViewById<TextView>(R.id.aaName).text = name // İsim alanını doldur
                findViewById<TextView>(R.id.aaEmail).text = p.email.orEmpty() // E-posta alanı
                val local = p.email?.substringBefore("@")?.take(2)?.uppercase(Locale.ROOT).orEmpty() // E-posta önekinden avatar
                val avatar = if (local.isNotEmpty()) local else (name.filter { it.isLetter() }).take(2) // Harflerden avatar
                    .uppercase(Locale.ROOT) // Büyük harfe çevir
                findViewById<TextView>(R.id.aaAvatar).text = if (avatar.isNotEmpty()) avatar else "A" // Avatar harfleri
            } catch (e: Exception) { // Bağlantı/parse hatası
                Toast.makeText( // Hata mesajı göster
                    this@AdminSettingsActivity,
                    e.message ?: "Bağlantı hatası",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
