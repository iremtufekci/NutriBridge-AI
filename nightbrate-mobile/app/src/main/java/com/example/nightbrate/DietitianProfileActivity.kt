package com.example.nightbrate // Paket tanımı

import android.os.Bundle // Activity durum verisi
import android.widget.TextView // Metin etiketi
import android.widget.Toast // Kısa bildirim
import androidx.appcompat.app.AppCompatActivity // Temel Activity
import androidx.lifecycle.lifecycleScope // Activity ömrüne bağlı coroutine
import kotlinx.coroutines.launch // Arka planda API çağrısı
import java.util.Locale // Büyük/küçük harf dönüşümü

class DietitianProfileActivity : AppCompatActivity() { // Diyetisyen hesap/profil ekranı
    override fun onCreate(savedInstanceState: Bundle?) { // Activity oluşturulurken
        super.onCreate(savedInstanceState) // Üst sınıf başlatması
        setContentView(R.layout.activity_dietitian_account) // Profil layout yükle
        DietitianBottomBarHelper.bind(this, 5) // Profil sekmesini seçili göster
        loadProfile() // Profil verilerini API'den çek
    }

    private fun loadProfile() { // Diyetisyen profil bilgilerini yükle
        lifecycleScope.launch { // Coroutine başlat
            try { // Ağ hatalarını yakala
                val r = RetrofitClient.instance.getCurrentUserProfile() // Profil API çağrısı
                if (!r.isSuccessful) { // HTTP hata durumu
                    Toast.makeText(this@DietitianProfileActivity, "Profil yüklenemedi", Toast.LENGTH_LONG).show()
                    return@launch // Coroutine'den çık
                }
                val p = r.body() ?: return@launch // Boş gövde kontrolü
                val f = p.firstName?.trim().orEmpty() // Ad
                val l = p.lastName?.trim().orEmpty() // Soyad
                val name = listOf(f, l).filter { it.isNotEmpty() } // Boş olmayan ad parçaları
                    .joinToString(" ") // Tam isim birleştir
                    .ifEmpty { p.displayName?.trim().orEmpty() } // Yoksa displayName
                    .ifEmpty { "Diyetisyen" } // Son çare varsayılan
                findViewById<TextView>(R.id.daName).text = name // İsim alanı
                findViewById<TextView>(R.id.daEmail).text = "E-posta: ${p.email.orEmpty()}" // E-posta satırı
                findViewById<TextView>(R.id.daClinic).text = // Klinik satırı
                    "Klinik: ${p.clinicName?.trim().orEmpty().ifEmpty { "—" }}" // Boşsa tire
                findViewById<TextView>(R.id.daDiploma).text = // Diploma satırı
                    "Diploma no: ${p.diplomaNo?.trim().orEmpty().ifEmpty { "—" }}" // Boşsa tire
                val code = p.connectionCode?.trim().orEmpty() // Danışan bağlantı kodu
                val tvKod = findViewById<TextView>(R.id.daTakipKodu) // Kod metin alanı
                val tvKodBilgi = findViewById<TextView>(R.id.daTakipKoduBilgi) // Kod açıklama alanı
                if (code.isNotEmpty()) { // Kod atanmışsa
                    tvKod.text = code // Kodu göster
                    tvKodBilgi.text = // Bilgilendirme metni
                        "Danışanlar uygulamada bu 6 haneli kodu girerek size bağlanır. Kod veritabanındaki onaylı hesabınızdan yüklenir."
                } else { // Kod henüz yoksa
                    tvKod.text = "—" // Tire göster
                    tvKodBilgi.text = // Bekleme mesajı
                        "Kod henüz yok. Yönetici onayı sonrası otomatik atanır; onaylıysanız kısa süre sonra yenileyin."
                }
                val a = (f.take(1) + l.take(1)).uppercase(Locale.ROOT) // Ad soyad baş harfleri
                val avatar = if (a.isNotBlank()) a else (name.filter { it.isLetter() }).take(2).uppercase(Locale.ROOT) // Avatar harfleri
                findViewById<TextView>(R.id.daAvatar).text = if (avatar.isNotEmpty()) avatar else "D" // Avatar göster
            } catch (e: Exception) { // Bağlantı/parse hatası
                Toast.makeText( // Hata mesajı
                    this@DietitianProfileActivity,
                    e.message ?: "Bağlantı hatası",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
