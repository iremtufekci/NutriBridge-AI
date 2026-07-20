package com.example.nightbrate // Paket tanımı

import android.content.Intent // Harici bağlantı açma
import android.net.Uri // Dosya URI'si
import android.os.Bundle // Activity durum paketi
import android.provider.OpenableColumns // Dosya adı sütunu
import android.view.View // Görünüm temel sınıfı
import android.view.ViewGroup // Layout parametreleri
import android.widget.LinearLayout // Dikey düzen
import android.widget.ProgressBar // Yükleniyor göstergesi
import android.widget.TextView // Metin görünümü
import android.widget.Toast // Kısa bildirim
import androidx.activity.result.contract.ActivityResultContracts // Dosya seçici
import androidx.appcompat.app.AppCompatActivity // Temel Activity
import androidx.core.content.ContextCompat // Tema renkleri
import androidx.core.widget.NestedScrollView // Kaydırılabilir alan
import androidx.lifecycle.lifecycleScope // Yaşam döngüsü coroutine
import com.google.android.material.button.MaterialButton // Material buton
import com.google.android.material.card.MaterialCardView // Material kart
import kotlinx.coroutines.Dispatchers // IO dispatcher
import kotlinx.coroutines.launch // Coroutine başlat
import kotlinx.coroutines.withContext // Thread değiştir
import okhttp3.MediaType.Companion.toMediaTypeOrNull // MIME tipi
import okhttp3.MultipartBody // Multipart yükleme
import okhttp3.RequestBody.Companion.toRequestBody // İstek gövdesi
import retrofit2.Response // HTTP yanıt
import java.text.SimpleDateFormat // Tarih biçimlendirme
import java.util.Locale // Yerel ayar
import java.util.TimeZone // Saat dilimi

/**
 * Danışan PDF analizi: dosya seç → backend'e yükle → sonuç + geçmiş listesi.
 */
class ClientPdfAnalysisActivity : AppCompatActivity() { // Danışan PDF analizi ekranı

    private val maxBytes = 10 * 1024 * 1024 // Maksimum PDF boyutu (10 MB)
    private var selectedHistoryId: String? = null // Seçili geçmiş kayıt ID'si

    private val pickPdf = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> // PDF seçici
        if (uri == null) return@registerForActivityResult // İptal edildi
        try {
            contentResolver.takePersistableUriPermission( // Kalıcı okuma izni al
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Bazı sağlayıcılarda kalıcı izin gerekmez
        }
        uploadUri(uri) // Seçilen PDF'i yükle
    }

    override fun onCreate(savedInstanceState: Bundle?) { // Activity oluşturulduğunda
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_pdf_analysis) // PDF analiz layout'u
        ClientBottomBarHelper.bind(this, 4) // Alt çubuk: PDF sekmesi

        findViewById<MaterialButton>(R.id.btnPickPdf).setOnClickListener { // PDF seç butonu
            pickPdf.launch(arrayOf("application/pdf", "application/x-pdf")) // Dosya seçici aç
        }

        loadHistory() // Geçmiş analizleri yükle
    }

    private fun uploadUri(uri: Uri) { // PDF yükle ve analiz et
        val tvErr = findViewById<TextView>(R.id.tvPdfError) // Hata metni
        val tvStatus = findViewById<TextView>(R.id.tvPdfStatus) // Durum metni
        val tvSelected = findViewById<TextView>(R.id.tvPdfSelectedFile) // Seçilen dosya adı
        val progress = findViewById<ProgressBar>(R.id.progressPdf) // Yükleniyor
        val card = findViewById<MaterialCardView>(R.id.cardPdfResult) // Sonuç kartı
        val btn = findViewById<MaterialButton>(R.id.btnPickPdf) // Seç butonu

        tvErr.visibility = View.GONE // Hata gizle
        card.visibility = View.GONE // Sonuç gizle

        val displayName = resolveDisplayName(uri) // Dosya adını çöz
        tvSelected.text = "Seçilen: $displayName" // Seçilen dosya göster
        tvSelected.visibility = View.VISIBLE
        tvStatus.text = "PDF yükleniyor ve analiz ediliyor… Lütfen bekleyin." // Durum mesajı
        tvStatus.visibility = View.VISIBLE

        lifecycleScope.launch { // Yükleme coroutine
            progress.visibility = View.VISIBLE // Yükleniyor göster
            btn.isEnabled = false // Butonu devre dışı bırak
            val result = withContext(Dispatchers.IO) { // IO thread'de yükle
                runCatching {
                    val bytes = readPdfBytes(uri) // PDF baytlarını oku
                    if (bytes.size > maxBytes) throw IllegalArgumentException("PDF en fazla 10 MB olabilir.")
                    if (bytes.isEmpty()) throw IllegalArgumentException("Dosya boş veya okunamadı.")

                    val safeName = if (displayName.lowercase(Locale.ROOT).endsWith(".pdf")) { // Güvenli dosya adı
                        displayName
                    } else {
                        "$displayName.pdf"
                    }
                    val body = bytes.toRequestBody("application/pdf".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("pdf", safeName, body) // Multipart parça
                    RetrofitClient.instance.uploadClientPdf(part) // PDF yükleme API
                }
            }
            progress.visibility = View.GONE // Yükleniyor gizle
            btn.isEnabled = true // Butonu tekrar etkinleştir
            tvStatus.visibility = View.GONE // Durum gizle

            result.onSuccess { resp -> // Başarılı veya HTTP hatası
                if (resp.isSuccessful && resp.body() != null) {
                    val dto = resp.body()!! // Analiz sonucu
                    selectedHistoryId = dto.id // Seçili kayıt ID
                    bindResult(dto) // Sonucu göster
                    loadHistory() // Geçmişi yenile
                    Toast.makeText(this@ClientPdfAnalysisActivity, "Analiz tamamlandı.", Toast.LENGTH_SHORT).show()
                } else {
                    tvErr.text = readErrorMessage(resp) // Hata mesajı
                    tvErr.visibility = View.VISIBLE
                }
            }.onFailure { e -> // Okuma/yükleme hatası
                tvErr.text = e.message ?: "Yükleme başarısız."
                tvErr.visibility = View.VISIBLE
            }
        }
    }

    private fun resolveDisplayName(uri: Uri): String { // URI'den dosya adını çöz
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (c.moveToFirst() && idx >= 0) {
                return c.getString(idx)?.trim().orEmpty().ifBlank { "belge.pdf" }
            }
        }
        val last = uri.lastPathSegment?.trim().orEmpty() // Son path segmenti
        return last.ifBlank { "belge.pdf" }
    }

    private fun readPdfBytes(uri: Uri): ByteArray { // PDF dosyasını bayt dizisine oku
        contentResolver.openInputStream(uri)?.use { return it.readBytes() }
        throw IllegalArgumentException("PDF dosyası okunamadı. Dosyayı tekrar seçin.")
    }

    private fun readErrorMessage(resp: Response<*>): String { // HTTP hata mesajını oku
        val raw = resp.errorBody()?.string().orEmpty()
        return try {
            val msg = org.json.JSONObject(raw).optString("message")
            if (msg.isNotBlank()) msg else "Hata: ${resp.code()}"
        } catch (_: Exception) {
            if (raw.isNotBlank()) raw else "Hata: ${resp.code()}"
        }
    }

    private fun bindResult(d: ClientPdfAnalysisResponseDto) { // Analiz sonucunu UI'ya bağla
        findViewById<MaterialCardView>(R.id.cardPdfResult).visibility = View.VISIBLE // Sonuç kartı göster
        findViewById<TextView>(R.id.tvPdfDocType).text = d.documentType?.ifBlank { "Belge" } ?: "Belge" // Belge türü
        val sourceLabel = when { // Kaynak etiketi
            isRealAiSource(d.analysisSource) -> "Yapay zeka analizi (Groq)"
            isMockNetworkSource(d.analysisSource) -> "Örnek (Groq'a bağlanılamadı)"
            else -> "Örnek / yapılandırma"
        }
        val dateLabel = formatAnalysisDate(d.createdAtUtc) // Tarih etiketi
        findViewById<TextView>(R.id.tvPdfSource).text =
            if (dateLabel.isNotBlank()) "$sourceLabel · $dateLabel" else sourceLabel // Kaynak + tarih

        val summaryTv = findViewById<TextView>(R.id.tvPdfSummary) // Özet metni
        val summary = d.summary?.trim().orEmpty()
        if (summary.isNotEmpty()) {
            summaryTv.text = summary
            summaryTv.visibility = View.VISIBLE
        } else {
            summaryTv.visibility = View.GONE
        }

        val url = pdfPublicUrl(d.pdfUrl) // PDF genel URL
        val link = findViewById<TextView>(R.id.tvPdfLink) // PDF bağlantısı
        link.text = "PDF: ${d.originalFileName ?: "İndir"}"
        link.setOnClickListener { // PDF aç tıklama
            if (url.isNotBlank()) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) // Tarayıcıda aç
            } else {
                Toast.makeText(this, "Bağlantı yok", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<LinearLayout>(R.id.containerPdfKeyFindings).visibility = View.GONE // Bulgular gizli
        findViewById<LinearLayout>(R.id.containerPdfCautions).visibility = View.GONE // Uyarılar gizli

        val comments = d.suggestedForDietitian.map { it.trim() }.filter { it.isNotEmpty() } // Diyetisyen yorumları
        val fallback = when { // Yorum fallback zinciri
            comments.isNotEmpty() -> comments
            d.keyFindings.map { it.trim() }.filter { it.isNotEmpty() }.isNotEmpty() ->
                d.keyFindings.map { it.trim() }.filter { it.isNotEmpty() }
            summary.isNotEmpty() -> listOf(summary)
            else -> listOf("Bu belge için yorum üretilemedi. Backend bağlantısını veya dosya içeriğini kontrol edin.")
        }
        bindBulletSection( // Yorum bölümünü bağla
            findViewById(R.id.containerPdfSuggested),
            "PDF yorumu",
            fallback,
            strong = false,
            caution = false
        )

        findViewById<NestedScrollView>(R.id.pdfScroll)?.post { // Sonuç kartına kaydır
            findViewById<MaterialCardView>(R.id.cardPdfResult)?.let { card ->
                findViewById<NestedScrollView>(R.id.pdfScroll)?.smoothScrollTo(0, card.top)
            }
        }
    }

    private fun bindBulletSection( // Madde işaretli bölüm oluştur
        container: LinearLayout,
        title: String,
        items: List<String>,
        strong: Boolean,
        caution: Boolean
    ) {
        container.removeAllViews() // Eski satırları temizle
        val cleaned = items.map { it.trim() }.filter { it.isNotEmpty() } // Boş olmayan maddeler
        if (cleaned.isEmpty()) {
            container.visibility = View.GONE // Boş bölüm gizle
            return
        }
        container.visibility = View.VISIBLE
        val dp = resources.displayMetrics.density // Ekran yoğunluğu
        fun dpf(v: Int) = (v * dp).toInt() // dp → px
        val titleColor = if (caution) ContextCompat.getColor(this, R.color.um_chip_amber_text)
        else ContextCompat.getColor(this, R.color.admin_muted) // Başlık rengi
        val bodyColor = if (caution) ContextCompat.getColor(this, R.color.um_chip_amber_text)
        else ContextCompat.getColor(this, R.color.admin_strong) // Metin rengi
        if (caution) { // Uyarı stili arka plan
            container.setBackgroundResource(R.drawable.diet_badge_rose)
            container.setPadding(dpf(12), dpf(12), dpf(12), dpf(12))
        } else {
            container.background = null
            container.setPadding(0, 0, 0, 0)
        }
        container.addView(TextView(this).apply { // Bölüm başlığı
            text = title.uppercase(Locale("tr", "TR"))
            setTextColor(titleColor)
            textSize = 11f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        for (line in cleaned) { // Her madde için satır
            container.addView(TextView(this).apply {
                text = "• $line"
                setTextColor(bodyColor)
                textSize = 13f
                setPadding(0, dpf(6), 0, 0)
                if (strong) setTypeface(typeface, android.graphics.Typeface.NORMAL)
            })
        }
    }

    private fun formatAnalysisDate(iso: String?): String { // ISO tarihi Türkçe formata çevir
        val raw = iso?.trim().orEmpty()
        if (raw.isEmpty()) return ""
        val patterns = listOf( // Denenecek formatlar
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" to 24,
            "yyyy-MM-dd'T'HH:mm:ss'Z'" to 20,
            "yyyy-MM-dd'T'HH:mm:ss" to 19
        )
        val outFmt = SimpleDateFormat("d MMM yyyy, HH:mm", Locale("tr", "TR")) // Çıktı formatı
        for ((p, len) in patterns) {
            try {
                val inFmt = SimpleDateFormat(p, Locale.US).apply {
                    if (p.contains("'Z'")) timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = inFmt.parse(raw.take(len)) ?: continue
                return outFmt.format(date)
            } catch (_: Exception) {
            }
        }
        return ""
    }

    private fun pdfPublicUrl(rel: String?): String { // Göreli PDF yolunu tam URL'ye çevir
        val r = rel?.trim().orEmpty()
        if (r.isEmpty()) return ""
        if (r.startsWith("http://", true) || r.startsWith("https://", true)) return r // Zaten tam URL
        val base = RetrofitClient.API_BASE_URL.trimEnd('/') // API tabanı
        val p = if (r.startsWith("/")) r else "/$r" // Path normalize
        return base + p
    }

    private fun loadHistory() { // Geçmiş PDF analizlerini API'den yükle
        lifecycleScope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching { RetrofitClient.instance.getClientPdfAnalyses(30) } // Son 30 kayıt
            }
            val resp = r.getOrNull() ?: return@launch // İstek başarısız
            if (!resp.isSuccessful || resp.body() == null) return@launch // Yanıt geçersiz
            bindHistory(resp.body()!!) // Geçmiş listesini çiz
        }
    }

    private fun bindHistory(items: List<ClientPdfAnalysisResponseDto>) { // Geçmiş analiz kartlarını oluştur
        val container = findViewById<LinearLayout>(R.id.containerPdfHistory) // Geçmiş konteyneri
        container.removeAllViews() // Eski kartları temizle
        val strong = ContextCompat.getColor(this, R.color.admin_strong) // Koyu metin
        val muted = ContextCompat.getColor(this, R.color.admin_muted) // Soluk metin
        val accent = ContextCompat.getColor(this, R.color.primary_green) // Vurgu rengi
        if (items.isEmpty()) { // Kayıt yok
            container.addView(TextView(this).apply {
                text = "Henüz kayıt yok."
                setTextColor(muted)
                textSize = 14f
            })
            return
        }
        val dp = resources.displayMetrics.density
        fun dpf(v: Int) = (v * dp).toInt()
        for (h in items) { // Her geçmiş kayıt için kart
            val selected = h.id != null && h.id == selectedHistoryId // Seçili kayıt mı
            val card = MaterialCardView(this).apply { // Geçmiş kartı
                radius = dpf(16).toFloat()
                strokeWidth = dpf(if (selected) 2 else 1) // Seçiliyse kalın çerçeve
                strokeColor = if (selected) accent
                else ContextCompat.getColor(this@ClientPdfAnalysisActivity, R.color.admin_row_stroke)
                setCardBackgroundColor(
                    if (selected) ContextCompat.getColor(this@ClientPdfAnalysisActivity, R.color.client_meal_done_bg)
                    else ContextCompat.getColor(this@ClientPdfAnalysisActivity, R.color.admin_card_bg)
                )
                isClickable = true
                isFocusable = true
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dpf(10)
                }
                setOnClickListener { // Kart tıklama: detayı göster
                    selectedHistoryId = h.id
                    bindResult(h)
                    bindHistory(items) // Seçim vurgusunu yenile
                }
            }
            val inner = LinearLayout(this).apply { // Kart iç düzeni
                orientation = LinearLayout.VERTICAL
                setPadding(dpf(14), dpf(14), dpf(14), dpf(14))
            }
            inner.addView(TextView(this).apply { // Dosya adı
                text = h.originalFileName.orEmpty()
                setTextColor(strong)
                textSize = 15f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            inner.addView(TextView(this).apply { // Belge türü
                text = h.documentType.orEmpty()
                setTextColor(muted)
                textSize = 12f
                setPadding(0, dpf(4), 0, 0)
            })
            val date = formatAnalysisDate(h.createdAtUtc) // Tarih
            if (date.isNotBlank()) {
                inner.addView(TextView(this).apply {
                    text = date
                    setTextColor(muted)
                    textSize = 11f
                    setPadding(0, dpf(2), 0, 0)
                })
            }
            inner.addView(TextView(this).apply { // Özet satırı
                text = h.suggestedForDietitian.firstOrNull { it.isNotBlank() }
                    ?: h.summary.orEmpty()
                    .ifBlank { "Detay için dokunun" }
                setTextColor(muted)
                textSize = 13f
                maxLines = 2
                setPadding(0, dpf(6), 0, 0)
            })
            inner.addView(TextView(this).apply { // Detay ipucu
                text = if (selected) "Detay açık ↑" else "Tam analizi gör →"
                setTextColor(accent)
                textSize = 12f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, dpf(8), 0, 0)
            })
            card.addView(inner) // İçeriği karta ekle
            container.addView(card) // Kartı listeye ekle
        }
    }
}
