package com.example.nightbrate // Uygulama paketi

import android.graphics.Typeface // Kalın yazı tipi
import android.os.Bundle // Aktivite durum paketi
import android.text.SpannableString // Biçimlendirilmiş metin
import android.text.style.StyleSpan // Kalın stil aralığı
import android.view.View // Görünüm temel sınıfı
import android.view.ViewGroup // Kart düzeni
import android.widget.LinearLayout // Dikey liste düzeni
import android.widget.ProgressBar // Yükleme göstergesi
import android.widget.ScrollView // Diyalog kaydırma
import android.widget.TextView // Metin görünümü
import android.widget.Toast // Kısa bildirim
import androidx.appcompat.app.AlertDialog // Onay diyaloğu
import androidx.appcompat.app.AppCompatActivity // Temel aktivite
import androidx.core.content.ContextCompat // Kaynak renk erişimi
import androidx.lifecycle.lifecycleScope // Yaşam döngüsü coroutine kapsamı
import com.google.android.material.button.MaterialButton // Material düğme
import com.google.android.material.card.MaterialCardView // Uyarı kartı
import kotlinx.coroutines.launch // Asenkron başlatma
import org.json.JSONObject // Hata JSON ayrıştırma
import java.text.SimpleDateFormat // Tarih biçimlendirme
import java.util.Locale // Yerel ayar
import java.util.TimeZone // Saat dilimi

class DietitianCriticalAlertsActivity : AppCompatActivity() { // Kritik uyarılar ekranı

    private lateinit var progress: ProgressBar // Yükleme çubuğu
    private lateinit var tvError: TextView // Hata metni
    private lateinit var cardBanner: MaterialCardView // Üst bilgi banner'ı
    private lateinit var tvBanner: TextView // Banner metni
    private lateinit var cardempty: MaterialCardView // Boş durum kartı
    private lateinit var llList: LinearLayout // Uyarı listesi

    private var alerts: MutableList<DietitianCriticalAlertDto> = mutableListOf() // Bellekteki uyarılar
    private var busyId: String? = null // Onay işlemi devam eden kimlik

    override fun onCreate(savedInstanceState: Bundle?) { // Aktivite oluşturulduğunda
        super.onCreate(savedInstanceState) // Üst sınıf başlatması
        setContentView(R.layout.activity_dietitian_critical_alerts) // Düzeni yükle
        DietitianBottomBarHelper.bind(this, 4) // Alt menüyü bağla

        progress = findViewById(R.id.progressCriticalAlerts) // İlerleme çubuğu
        tvError = findViewById(R.id.tvCriticalAlertsError) // Hata alanı
        cardBanner = findViewById(R.id.cardCriticalAlertsBanner) // Banner kartı
        tvBanner = findViewById(R.id.tvCriticalAlertsBannerText) // Banner metni
        cardempty = findViewById(R.id.cardCriticalAlertsEmpty) // Boş durum kartı
        llList = findViewById(R.id.llCriticalAlertsList) // Liste konteyneri

        loadAlerts() // Uyarıları yükle
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt() // dp'yi piksele çevir

    private fun alertTypeLabel(t: String) = when (t) { // Uyarı tipi etiketi
        "MissedMeals" -> "Öğün tamamlama" // Kaçırılan öğün
        "HighCalories" -> "Yüksek kalori" // Kalori aşımı
        else -> t // Bilinmeyen tip
    }

    private fun loadAlerts() { // Kritik uyarıları API'den al
        progress.visibility = View.VISIBLE // Yükleniyor göster
        tvError.visibility = View.GONE // Hatayı gizle
        cardBanner.visibility = View.GONE // Banner'ı gizle
        cardempty.visibility = View.GONE // Boş kartı gizle
        llList.removeAllViews() // Listeyi temizle
        lifecycleScope.launch { // Coroutine ile istek
            try {
                val r = RetrofitClient.instance.getDietitianCriticalAlerts() // Uyarıları getir
                if (r.isSuccessful) { // Başarılı yanıt
                    alerts = r.body().orEmpty().toMutableList() // Listeyi sakla
                    bindUi() // Arayüzü güncelle
                } else { // HTTP hatası
                    val raw = r.errorBody()?.string().orEmpty() // Ham hata gövdesi
                    val msg = try {
                        JSONObject(raw).optString("message") // JSON mesajı
                    } catch (_: Exception) {
                        raw // Ham metin
                    }
                    tvError.text = msg.ifBlank { "Uyarılar alınamadı (${r.code()})" } // Hata metni
                    tvError.visibility = View.VISIBLE // Hatayı göster
                }
            } catch (e: Exception) { // Bağlantı hatası
                tvError.text = e.message ?: "Bağlantı hatası" // Hata mesajı
                tvError.visibility = View.VISIBLE // Hatayı göster
            } finally {
                progress.visibility = View.GONE // Yüklemeyi gizle
            }
        }
    }

    private fun bindUi() { // Uyarı listesini ekrana bağla
        llList.removeAllViews() // Önceki kartları temizle
        if (alerts.isEmpty()) { // Uyarı yoksa
            cardempty.visibility = View.VISIBLE // Boş kartı göster
            return // Çizimi bitir
        }
        cardBanner.visibility = View.VISIBLE // Banner'ı göster
        val highCount = alerts.count { it.severity.equals("High", ignoreCase = true) } // Yüksek öncelik sayısı
        val line1 = "Kritik durumda ${alerts.size} kayıt" // İlk satır
        val rest = when { // Devam metni
            highCount > 0 -> " ($highCount yüksek öncelik). Bu danışanlar kısa sürede değerlendirme gerektirebilir; kaydı onaylayarak arşivleyebilirsiniz." // Yüksek öncelik açıklaması
            else -> " Orta öncelikli uyarıları inceleyip onaylayabilirsiniz." // Orta öncelik açıklaması
        }
        val full = line1 + rest // Tam banner metni
        val sp = SpannableString(full) // Biçimlendirilmiş metin
        sp.setSpan(StyleSpan(Typeface.BOLD), 0, line1.length, 0) // İlk kısmı kalın yap
        tvBanner.text = sp // Banner'a yaz

        for (a in alerts) { // Her uyarı için kart
            llList.addView(buildAlertCard(a)) // Kartı listeye ekle
        }
    }

    private fun buildAlertCard(a: DietitianCriticalAlertDto): View { // Tek uyarı kartı oluştur
        val isHigh = a.severity.equals("High", ignoreCase = true) // Yüksek öncelik mi
        val stroke = if (isHigh) android.graphics.Color.parseColor("#FDA4AF") else android.graphics.Color.parseColor("#FCD34D") // Kenarlık rengi
        val leftStripe = if (isHigh) android.graphics.Color.parseColor("#F43F5E") else android.graphics.Color.parseColor("#D97706") // Sol şerit rengi
        val card = MaterialCardView(this).apply {
            radius = dp(16).toFloat() // Köşe yuvarlaklığı
            cardElevation = dp(2).toFloat() // Gölge
            strokeWidth = dp(1) // Kenarlık kalınlığı
            this.strokeColor = stroke // Kenarlık rengi
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT // Tam genişlik kart
            ).apply { bottomMargin = dp(14) } // Alt boşluk
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL // Yatay düzen
            setPadding(dp(16), dp(16), dp(16), dp(16)) // İç boşluk
        }
        val stripe = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(4), ViewGroup.LayoutParams.MATCH_PARENT).apply {
                marginEnd = dp(12) // Sağ boşluk
            }
            setBackgroundColor(leftStripe) // Sol şerit rengi
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL // Dikey içerik
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) // Esnek alan
        }
        val head = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL // Başlık satırı
        }
        val avatarFg = if (isHigh) "#E11D48" else "#B45309" // Avatar metin rengi
        val initial = (a.clientName.ifBlank { "?" }).trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?" // Baş harf
        head.addView(TextView(this).apply {
            text = initial // Avatar harfi
            textSize = 14f // Normal yazı
            setTypeface(typeface, Typeface.BOLD) // Kalın yazı
            setTextColor(android.graphics.Color.parseColor(avatarFg)) // Avatar rengi
            gravity = android.view.Gravity.CENTER // Ortala
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)) // Kare boyut
            setBackgroundResource(R.drawable.diet_critical_avatar_bg) // Avatar arka planı
        })
        val names = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL // Ad ve tarih sütunu
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(10) // Sol boşluk
            }
        }
        names.addView(TextView(this).apply {
            text = a.clientName.ifBlank { "Danışan" } // Danışan adı
            textSize = 17f // Büyük yazı
            setTypeface(typeface, Typeface.BOLD) // Kalın yazı
            setTextColor(ContextCompat.getColor(this@DietitianCriticalAlertsActivity, R.color.admin_strong)) // Koyu renk
        })
        names.addView(TextView(this).apply {
            textSize = 11f // Küçük yazı
            setTextColor(ContextCompat.getColor(this@DietitianCriticalAlertsActivity, R.color.admin_muted)) // Soluk renk
            text = "Tarih: ${formatAlertDate(a.date)}" // Uyarı tarihi
        })
        head.addView(names) // Ad sütununu ekle
        val chip = TextView(this).apply {
            text = if (isHigh) "Yüksek öncelik" else "Orta öncelik" // Öncelik etiketi
            textSize = 11f // Küçük yazı
            setTypeface(typeface, Typeface.BOLD) // Kalın yazı
            setPadding(dp(10), dp(6), dp(10), dp(6)) // İç boşluk
            setTextColor(android.graphics.Color.parseColor(if (isHigh) "#BE123C" else "#92400E")) // Metin rengi
            setBackgroundColor(android.graphics.Color.parseColor(if (isHigh) "#FEE2E2" else "#FEF3C7")) // Arka plan
        }
        head.addView(chip) // Öncelik etiketini ekle
        content.addView(head) // Başlığı içeriğe ekle

        val typeRowBg = ContextCompat.getColor(this, R.color.admin_row_surface) // Tip satırı arka planı
        val typeRow = TextView(this).apply {
            text = "⚠ ${alertTypeLabel(a.alertType)}" // Uyarı tipi
            textSize = 13f // Normal yazı
            setTypeface(typeface, Typeface.BOLD) // Kalın yazı
            setTextColor(ContextCompat.getColor(this@DietitianCriticalAlertsActivity, R.color.admin_strong)) // Koyu renk
            setPadding(dp(10), dp(8), dp(10), dp(8)) // İç boşluk
            setBackgroundColor(typeRowBg) // Arka plan rengi
        }
        content.addView(typeRow.apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT // Tam genişlik satır
            ).apply { topMargin = dp(10) } // Üst boşluk
        })

        content.addView(TextView(this).apply {
            text = a.message // Uyarı mesajı
            textSize = 13f // Normal yazı
            setTextColor(ContextCompat.getColor(this@DietitianCriticalAlertsActivity, R.color.admin_muted)) // Soluk renk
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT // Tam genişlik metin
            ).apply { topMargin = dp(8) } // Üst boşluk
        })

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL // Düğme satırı
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT // Tam genişlik satır
            ).apply { topMargin = dp(14) } // Üst boşluk
        }
        val btnAck = MaterialButton(this).apply {
            text = "İncelendi (diyetisyen onayı)" // Onay düğmesi
            textSize = 13f // Normal yazı
            isEnabled = true // Etkin
            setOnClickListener { acknowledge(a) } // Onay işlemi
        }
        val btnProfile = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Profili gör" // Profil düğmesi
            textSize = 13f // Normal yazı
            setOnClickListener { showClientBrief(a.clientId) } // Özet diyaloğu
        }
        btnRow.addView(btnAck, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = dp(8) // Sağ boşluk
        })
        btnRow.addView(btnProfile, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)) // Profil düğmesi
        content.addView(btnRow) // Düğme satırını ekle

        row.addView(stripe) // Sol şeridi ekle
        row.addView(content) // İçeriği ekle
        card.addView(row) // Satırı karta ekle
        return card // Kartı döndür
    }

    private fun formatAlertDate(iso: String): String { // ISO tarihi Türkçe biçimle
        val patterns = listOf( // Olası tarih desenleri
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (p in patterns) { // Her deseni dene
            try {
                val sdf = SimpleDateFormat(p, Locale.ROOT) // Giriş biçimleyici
                sdf.timeZone = TimeZone.getTimeZone("UTC") // UTC saat dilimi
                val d = sdf.parse(iso) ?: continue // Ayrıştır
                val out = SimpleDateFormat("d MMMM yyyy", Locale("tr", "TR")) // Çıkış biçimi
                out.timeZone = TimeZone.getDefault() // Yerel saat dilimi
                return out.format(d) // Biçimlendirilmiş tarih
            } catch (_: Exception) { // Desen uymazsa devam
            }
        }
        return iso // Ham değeri döndür
    }

    private fun acknowledge(a: DietitianCriticalAlertDto) { // Uyarıyı onayla ve arşivle
        if (busyId != null) return // Başka işlem devam ediyorsa çık
        busyId = a.id // Meşgul kimliği işaretle
        lifecycleScope.launch { // Coroutine ile istek
            try {
                val r = RetrofitClient.instance.acknowledgeCriticalAlert(
                    AckCriticalAlertRequest(
                        clientId = a.clientId, // Danışan kimliği
                        alertType = a.alertType, // Uyarı tipi
                        referenceDate = a.referenceDate // Referans tarihi
                    )
                )
                if (r.isSuccessful) { // Onay başarılı
                    alerts.removeAll { it.id == a.id } // Listeden kaldır
                    bindUi() // Arayüzü yenile
                } else { // Onay başarısız
                    Toast.makeText(this@DietitianCriticalAlertsActivity, "Onay kaydedilemedi", Toast.LENGTH_LONG) // Bildirim
                        .show()
                }
            } catch (e: Exception) { // Hata durumu
                Toast.makeText(this@DietitianCriticalAlertsActivity, e.message ?: "Hata", Toast.LENGTH_LONG).show() // Bildirim
            } finally {
                busyId = null // Meşgul durumunu temizle
            }
        }
    }

    private fun showClientBrief(clientId: String) { // Danışan özet diyaloğu göster
        val scroll = ScrollView(this) // Kaydırılabilir alan
        val tv = TextView(this).apply {
            setPadding(dp(16), dp(16), dp(16), dp(16)) // İç boşluk
            textSize = 14f // Normal yazı
            setTextColor(ContextCompat.getColor(this@DietitianCriticalAlertsActivity, R.color.admin_strong)) // Koyu renk
            text = "Yükleniyor…" // Başlangıç metni
        }
        scroll.addView(tv) // Metni kaydırmaya ekle
        val dlg = AlertDialog.Builder(this)
            .setTitle("Danışan özeti") // Diyalog başlığı
            .setView(scroll) // İçerik görünümü
            .setNegativeButton("Kapat", null) // Kapat düğmesi
            .create() // Diyalogu oluştur
        dlg.show() // Diyalogu göster
        lifecycleScope.launch { // Profil verisini yükle
            try {
                val r = RetrofitClient.instance.getDietitianClientBrief(clientId) // Kısa profil getir
                if (r.isSuccessful) { // Başarılı yanıt
                    val b = r.body() // Profil gövdesi
                    if (b != null) { // Veri varsa
                        tv.text = buildString {
                            appendLine("Ad: ${b.firstName.orEmpty()} ${b.lastName.orEmpty()}".trim()) // Ad soyad
                            appendLine("E-posta: ${b.email?.trim()?.takeIf { it.isNotEmpty() } ?: "—"}") // E-posta
                            appendLine(
                                "Telefon: ${
                                    b.phone?.trim()?.takeIf { it.isNotEmpty() } ?: "—" // Telefon
                                }"
                            )
                            appendLine("Hedef kalori: ${b.targetCalories} kkal") // Hedef kalori
                            appendLine("Kilo: ${b.weight} kg") // Kilo
                            appendLine("Boy: ${b.height} cm") // Boy
                        }.trim() // Fazla boşluğu temizle
                    } else { // Gövde boş
                        tv.text = "Profil yüklenemedi veya erişim yok." // Hata mesajı
                        tv.setTextColor(ContextCompat.getColor(this@DietitianCriticalAlertsActivity, R.color.um_chip_red_text)) // Kırmızı metin
                    }
                } else { // HTTP hatası
                    tv.text = "Profil yüklenemedi veya erişim yok." // Hata mesajı
                    tv.setTextColor(ContextCompat.getColor(this@DietitianCriticalAlertsActivity, R.color.um_chip_red_text)) // Kırmızı metin
                }
            } catch (_: Exception) { // İstisna
                tv.text = "Profil yüklenemedi veya erişim yok." // Hata mesajı
                tv.setTextColor(ContextCompat.getColor(this@DietitianCriticalAlertsActivity, R.color.um_chip_red_text)) // Kırmızı metin
            }
        }
    }
}
