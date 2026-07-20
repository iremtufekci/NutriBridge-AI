package com.example.nightbrate // Uygulama paketi

import android.graphics.drawable.GradientDrawable // Yuvarlatılmış arka plan
import android.content.Intent // Harici bağlantı açma
import android.graphics.Color // Renk sabitleri
import android.net.Uri // PDF URL'si
import android.os.Bundle // Aktivite durum paketi
import android.view.View // Görünüm temel sınıfı
import android.widget.FrameLayout // Kart iç düzeni
import android.widget.LinearLayout // Dikey liste düzeni
import android.widget.ProgressBar // Yükleme göstergesi
import android.widget.TextView // Metin görünümü
import androidx.appcompat.app.AppCompatActivity // Temel aktivite
import androidx.core.content.ContextCompat // Kaynak renk erişimi
import androidx.lifecycle.lifecycleScope // Yaşam döngüsü coroutine kapsamı
import com.google.android.material.card.MaterialCardView // Material kart
import kotlinx.coroutines.Dispatchers // IO iş parçacığı
import kotlinx.coroutines.launch // Asenkron başlatma
import kotlinx.coroutines.withContext // Bağlam değiştirme
import org.json.JSONObject // Hata JSON ayrıştırma
import retrofit2.Response // HTTP yanıtı

class DietitianClientDetailActivity : AppCompatActivity() { // Danışan detay ekranı

    private lateinit var progress: ProgressBar // Yükleme çubuğu
    private lateinit var tvErr: TextView // Hata metni
    private lateinit var tvTitle: TextView // Danışan başlığı
    private lateinit var tvComplianceHint: TextView // Hedef kalori ipucu
    private lateinit var containerWeekly: LinearLayout // Haftalık program satırı
    private lateinit var containerRecipes: LinearLayout // Tarif kayıtları listesi
    private lateinit var containerPdfs: LinearLayout // PDF analizleri listesi

    private val emerald by lazy { ContextCompat.getColor(this, R.color.nav_item_active) } // Vurgu rengi
    private val muted by lazy { ContextCompat.getColor(this, R.color.admin_muted) } // Soluk metin rengi
    private val strong by lazy { ContextCompat.getColor(this, R.color.admin_strong) } // Koyu metin rengi

    override fun onCreate(savedInstanceState: Bundle?) { // Aktivite oluşturulduğunda
        super.onCreate(savedInstanceState) // Üst sınıf başlatması
        setContentView(R.layout.activity_dietitian_client_detail) // Detay düzenini yükle

        progress = findViewById(R.id.progressDetail) // İlerleme çubuğu
        tvErr = findViewById(R.id.tvDetailError) // Hata alanı
        tvTitle = findViewById(R.id.tvDetailTitle) // Başlık alanı
        tvComplianceHint = findViewById(R.id.tvComplianceHint) // Hedef kalori alanı
        containerWeekly = findViewById(R.id.containerWeeklyProgram) // Haftalık program konteyneri
        containerRecipes = findViewById(R.id.containerRecipes) // Tarif konteyneri
        containerPdfs = findViewById(R.id.containerPdfs) // PDF konteyneri

        findViewById<View>(R.id.btnDetailBack).setOnClickListener { finish() } // Geri dön

        val clientId = intent.getStringExtra(EXTRA_CLIENT_ID)?.trim().orEmpty() // Gelen danışan kimliği
        if (clientId.isEmpty()) { // Kimlik yoksa
            tvErr.text = "Danışan seçilmedi." // Hata mesajı
            tvErr.visibility = View.VISIBLE // Hatayı göster
            return // Yüklemeyi durdur
        }
        loadOverview(clientId) // Özet verisini getir
    }

    private fun loadOverview(clientId: String) { // Danışan özetini API'den al
        tvErr.visibility = View.GONE // Önceki hatayı gizle
        progress.visibility = View.VISIBLE // Yükleniyor göster
        lifecycleScope.launch { // Coroutine ile istek
            val result = withContext(Dispatchers.IO) { // IO iş parçacığında
                runCatching { RetrofitClient.instance.getClientOverview(clientId) } // Özet çağrısı
            }
            progress.visibility = View.GONE // Yüklemeyi gizle
            result.onSuccess { resp -> // Başarılı sonuç
                if (resp.isSuccessful && resp.body() != null) { // Geçerli gövde
                    bind(resp.body()!!) // Ekranı doldur
                } else { // HTTP hatası
                    tvErr.text = readError(resp) // Hata mesajını oku
                    tvErr.visibility = View.VISIBLE // Hatayı göster
                }
            }.onFailure { // İstisna
                tvErr.text = it.message ?: "Yüklenemedi." // Hata metni
                tvErr.visibility = View.VISIBLE // Hatayı göster
            }
        }
    }

    private fun bind(data: DietitianClientOverviewDto) { // Özet verisini arayüze bağla
        val c = data.client // Danışan bilgisi
        val title = when { // Başlık metnini belirle
            c == null -> "Danışan" // Varsayılan
            "${c.firstName.orEmpty()} ${c.lastName.orEmpty()}".trim().isNotBlank() ->
                "${c.firstName.orEmpty()} ${c.lastName.orEmpty()}".trim() // Ad soyad
            else -> c.email ?: "Danışan" // E-posta yedek
        }
        tvTitle.text = title // Başlığı yaz
        tvComplianceHint.text = "Hedef: ${c?.targetCalories ?: "—"} kkal" // Hedef kalori

        bindWeeklyProgram(data.weeklyProgramDays) // Haftalık programı çiz

        containerRecipes.removeAllViews() // Eski tarif kartlarını temizle
        val logs = data.kitchenRecipeLogs // Mutfak tarif kayıtları
        if (logs.isEmpty()) { // Kayıt yoksa
            containerRecipes.addView(textMuted("Henüz yapay zeka tarif kaydı yok.")) // Boş mesaj
        } else { // Kayıtlar varsa
            logs.forEach { log -> // Her kayıt için kart
                val card = MaterialCardView(this).apply {
                    radius = dp(12).toFloat() // Köşe yuvarlaklığı
                    cardElevation = dp(2).toFloat() // Gölge
                    setPadding(dp(12), dp(12), dp(12), dp(12)) // İç boşluk
                }
                val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL } // Dikey sütun
                col.addView(
                    TextView(this).apply {
                        text = log.createdAtUtc.take(19).replace("T", " ") // Oluşturma zamanı
                        textSize = 11f // Küçük yazı
                        setTextColor(muted) // Soluk renk
                    }
                )
                log.selectedRecipes.forEach { r -> // Seçilen tarifler
                    col.addView(
                        TextView(this).apply {
                            text = "${r.title}  (~${r.estimatedCalories} kkal)" // Tarif ve kalori
                            textSize = 14f // Normal yazı
                            setTextColor(strong) // Koyu renk
                            setPadding(0, dp(6), 0, 0) // Üst boşluk
                        }
                    )
                }
                if (log.selectedRecipes.isEmpty()) { // Tarif listesi boşsa
                    col.addView(TextView(this).apply { text = "(Tarif listesi boş)"; setTextColor(muted); textSize = 13f }) // Uyarı
                }
                card.addView(
                    col,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT // Kart iç düzeni
                    )
                )
                val lpR = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT) // Tam genişlik
                lpR.bottomMargin = dp(10) // Alt boşluk
                containerRecipes.addView(card, lpR) // Kartı ekle
            }
        }

        containerPdfs.removeAllViews() // Eski PDF satırlarını temizle
        val pdfs = data.pdfAnalyses // PDF analiz listesi
        if (pdfs.isEmpty()) { // PDF yoksa
            containerPdfs.addView(textMuted("Henüz PDF yüklemesi yok.")) // Boş mesaj
        } else { // PDF'ler varsa
            pdfs.forEach { p -> // Her PDF için satır
                val row = MaterialCardView(this).apply {
                    radius = dp(12).toFloat() // Köşe yuvarlaklığı
                    cardElevation = dp(2).toFloat() // Gölge
                    setPadding(dp(12), dp(12), dp(12), dp(12)) // İç boşluk
                }
                val inner = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL } // Dikey içerik
                row.addView(
                    inner,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT // Kart düzeni
                    )
                )
                inner.addView(
                    TextView(this).apply {
                        text = p.originalFileName ?: "PDF" // Dosya adı
                        textSize = 14f // Normal yazı
                        setTextColor(strong) // Koyu renk
                    }
                )
                inner.addView(
                    TextView(this).apply {
                        val s = p.summary ?: "" // Özet metni
                        text = if (s.length > 120) s.take(120) + "…" else s // Kısaltılmış özet
                        textSize = 12f // Küçük yazı
                        setTextColor(muted) // Soluk renk
                    }
                )
                inner.addView(
                    TextView(this).apply {
                        text = "PDF'yi aç" // Bağlantı metni
                        setTextColor(emerald) // Vurgu rengi
                        textSize = 14f // Normal yazı
                        setPadding(0, dp(8), 0, 0) // Üst boşluk
                        setOnClickListener { // Tıklanınca tarayıcıda aç
                            val url = pdfPublicUrl(p.pdfUrl) // Tam URL oluştur
                            if (url.isNotBlank()) { // Geçerli adres
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) // Görüntüle
                            }
                        }
                    }
                )
                val lpP = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT) // Tam genişlik
                lpP.bottomMargin = dp(10) // Alt boşluk
                containerPdfs.addView(row, lpP) // Satırı ekle
            }
        }
    }

    private fun bindWeeklyProgram(days: List<DietitianProgramDayOverviewDto>) { // Haftalık program sütunları
        containerWeekly.removeAllViews() // Önceki günleri temizle
        if (days.isEmpty()) { // Veri yoksa
            containerWeekly.addView(textMuted("Haftalık program verisi yok.")) // Boş mesaj
            return // Çizimi bitir
        }
        days.forEach { day -> // Her gün için sütun
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL // Dikey düzen
                val bg = GradientDrawable() // Arka plan şekli
                bg.cornerRadius = dp(12).toFloat() // Yuvarlatılmış köşe
                bg.setColor(ContextCompat.getColor(this@DietitianClientDetailActivity, R.color.admin_card_bg)) // Kart rengi
                bg.setStroke(dp(1), Color.parseColor("#E2E8F0")) // İnce kenarlık
                background = bg // Arka planı uygula
                setPadding(dp(12), dp(12), dp(12), dp(12)) // İç boşluk
            }
            val lpCol = LinearLayout.LayoutParams(dp(220), LinearLayout.LayoutParams.WRAP_CONTENT) // Sabit genişlik sütun
            lpCol.marginEnd = dp(10) // Sağ boşluk

            col.addView(
                TextView(this).apply {
                    text = day.weekdayLabel ?: "—" // Gün adı
                    textSize = 15f // Başlık boyutu
                    setTextColor(strong) // Koyu renk
                    setTypeface(null, android.graphics.Typeface.BOLD) // Kalın yazı
                }
            )
            col.addView(
                TextView(this).apply {
                    text = day.programDate.orEmpty() // Program tarihi
                    textSize = 11f // Küçük yazı
                    setTextColor(muted) // Soluk renk
                    setPadding(0, 0, 0, dp(8)) // Alt boşluk
                }
            )

            val meals = day.meals // Günün öğünleri
            if (meals.isEmpty()) { // Öğün yoksa
                col.addView(
                    TextView(this).apply {
                        text = "Bu gün için program kaydı yok." // Boş gün mesajı
                        textSize = 13f // Normal yazı
                        setTextColor(muted) // Soluk renk
                    }
                )
            } else { // Öğünler varsa
                meals.forEachIndexed { idx, m -> // Her öğün kartı
                    col.addView(mealCard(m)) // Öğün kartını ekle
                    if (idx < meals.lastIndex) { // Son öğün değilse
                        val gap = View(this) // Boşluk görünümü
                        gap.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dp(8) // Öğünler arası boşluk
                        )
                        col.addView(gap) // Boşluğu ekle
                    }
                }
            }

            containerWeekly.addView(col, lpCol) // Sütunu yatay listeye ekle
        }
    }

    private fun mealCard(m: DietitianProgramMealOverviewDto): LinearLayout { // Tek öğün kartı oluştur
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL // Dikey düzen
            setPadding(dp(10), dp(10), dp(10), dp(10)) // İç boşluk
            val gd = GradientDrawable() // Arka plan şekli
            gd.cornerRadius = dp(10).toFloat() // Yuvarlatılmış köşe
            gd.setColor(if (m.completed) Color.parseColor("#ECFDF5") else Color.parseColor("#FFFFFF")) // Tamamlanma rengi
            gd.setStroke(
                dp(1),
                if (m.completed) Color.parseColor("#6EE7B7") else Color.parseColor("#CBD5E1") // Kenarlık rengi
            )
            background = gd // Arka planı uygula
        }
        val rowTop = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL } // Üst satır
        rowTop.addView(
            TextView(this).apply {
                text = m.label ?: "" // Öğün etiketi
                textSize = 11f // Küçük yazı
                setTextColor(muted) // Soluk renk
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) // Esnek alan
            }
        )
        rowTop.addView(
            TextView(this).apply {
                text = if (m.completed) "✓" else "✗" // Tamamlanma işareti
                textSize = 14f // Normal yazı
                setTextColor(
                    if (m.completed) Color.parseColor("#16A34A") else Color.parseColor("#94A3B8") // İşaret rengi
                )
            }
        )
        wrap.addView(rowTop) // Üst satırı ekle
        wrap.addView(
            TextView(this).apply {
                val d = m.description?.trim().orEmpty() // Öğün açıklaması
                text = if (d.isNotEmpty()) d else "—" // Açıklama veya tire
                textSize = 13f // Normal yazı
                setTextColor(strong) // Koyu renk
                setPadding(0, dp(6), 0, 0) // Üst boşluk
            }
        )
        wrap.addView(
            TextView(this).apply {
                val cal = m.calories // Kalori değeri
                text = if (cal > 0) "$cal kkal" else "—" // Kalori veya tire
                textSize = 11f // Küçük yazı
                setTextColor(muted) // Soluk renk
                setPadding(0, dp(4), 0, 0) // Üst boşluk
            }
        )
        return wrap // Öğün kartını döndür
    }

    private fun textMuted(msg: String) = TextView(this).apply { // Soluk metin görünümü
        text = msg // Mesaj metni
        setTextColor(muted) // Soluk renk
        textSize = 13f // Normal yazı
    }

    private fun dp(x: Int): Int = (x * resources.displayMetrics.density).toInt() // dp'yi piksele çevir

    private fun pdfPublicUrl(rel: String?): String { // Göreli PDF yolunu tam URL yap
        val r = rel?.trim().orEmpty() // Temizlenmiş yol
        if (r.isEmpty()) return "" // Boşsa çık
        if (r.startsWith("http://", true) || r.startsWith("https://", true)) return r // Zaten tam URL
        val base = RetrofitClient.API_BASE_URL.trimEnd('/') // API kök adresi
        val p = if (r.startsWith("/")) r else "/$r" // Başında slash garantisi
        return base + p // Birleştirilmiş URL
    }

    private fun readError(resp: Response<*>): String { // API hata mesajını çöz
        val raw = resp.errorBody()?.string().orEmpty() // Ham hata gövdesi
        return try {
            JSONObject(raw).optString("message").ifBlank { "Hata ${resp.code()}" } // JSON mesajı
        } catch (_: Exception) { // JSON değilse
            raw.ifBlank { "Hata ${resp.code()}" } // Ham veya varsayılan
        }
    }

    companion object { // Sabitler
        const val EXTRA_CLIENT_ID = "extra_client_id" // Intent ekstra anahtarı
    }
}
