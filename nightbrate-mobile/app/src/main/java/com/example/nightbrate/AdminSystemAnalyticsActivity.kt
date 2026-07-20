package com.example.nightbrate // Paket tanımı

import android.graphics.Color // Renk işlemleri
import android.os.Bundle // Activity durum paketi
import android.view.View // Görünüm temel sınıfı
import android.widget.LinearLayout // Dikey liste konteyneri
import android.widget.ProgressBar // Yükleme göstergesi
import android.widget.TextView // Metin görünümü
import android.widget.Toast // Kısa bildirim
import androidx.appcompat.app.AppCompatActivity // Temel Activity
import androidx.core.content.ContextCompat // Kaynak renk çözümleme
import androidx.lifecycle.Lifecycle // Yaşam döngüsü durumu
import androidx.lifecycle.lifecycleScope // Coroutine kapsamı
import androidx.lifecycle.repeatOnLifecycle // Yaşam döngüsüne bağlı tekrar
import com.github.mikephil.charting.charts.BarChart // Çubuk grafik
import com.github.mikephil.charting.charts.LineChart // Çizgi grafik
import com.github.mikephil.charting.components.Legend // Grafik lejantı
import com.github.mikephil.charting.components.XAxis // X ekseni
import com.github.mikephil.charting.data.BarData // Çubuk verisi
import com.github.mikephil.charting.data.BarDataSet // Çubuk veri seti
import com.github.mikephil.charting.data.BarEntry // Çubuk girişi
import com.github.mikephil.charting.data.Entry // Çizgi noktası
import com.github.mikephil.charting.data.LineData // Çizgi verisi
import com.github.mikephil.charting.data.LineDataSet // Çizgi veri seti
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter // İndeks eksen etiketleri
import kotlinx.coroutines.Dispatchers // Coroutine iş parçacığı havuzları
import kotlinx.coroutines.delay // Bekleme süresi
import kotlinx.coroutines.isActive // Coroutine aktif mi
import kotlinx.coroutines.launch // Coroutine başlatma
import kotlinx.coroutines.withContext // Bağlam değiştirme
import java.text.NumberFormat // Sayı biçimlendirme
import org.json.JSONObject // JSON ayrıştırma
import retrofit2.Response // HTTP yanıt sarmalayıcısı
import java.text.SimpleDateFormat // Tarih biçimlendirme
import java.util.Date // Tarih nesnesi
import java.util.Locale // Yerel ayar
import java.util.TimeZone // Saat dilimi

class AdminSystemAnalyticsActivity : AppCompatActivity() { // Sistem analitiği ekranı

    private lateinit var swipe: androidx.swiperefreshlayout.widget.SwipeRefreshLayout // Çekerek yenile
    private lateinit var progress: ProgressBar // İlk yükleme çubuğu
    private lateinit var errorPanel: View // Hata paneli
    private lateinit var content: View // İçerik alanı
    private lateinit var tvError: TextView // Hata metni

    private var hasLoadedOnce = false // En az bir kez yüklendi mi

    override fun onCreate(savedInstanceState: Bundle?) { // Activity oluşturulduğunda
        super.onCreate(savedInstanceState) // Üst sınıf başlatma
        setContentView(R.layout.activity_admin_system_analytics) // Analitik düzeni
        AdminBottomBarHelper.bind(this, 3) // Alt sekme (analitik)

        swipe = findViewById(R.id.sysAnSwipe) // Yenileme konteyneri
        progress = findViewById(R.id.sysAnProgress) // İlerleme çubuğu
        errorPanel = findViewById(R.id.sysAnErrorPanel) // Hata paneli
        content = findViewById(R.id.sysAnContent) // Ana içerik
        tvError = findViewById(R.id.tvSysAnError) // Hata metni

        swipe.setOnRefreshListener { // Çekerek yenile dinleyicisi
            lifecycleScope.launch { loadInternal(silent = true) } // Sessiz yenileme
        }
        findViewById<View>(R.id.btnSysAnRetry).setOnClickListener { // Tekrar dene düğmesi
            lifecycleScope.launch { loadInternal(silent = false) } // Tam yenileme
        }

        lifecycleScope.launch { // Periyodik veri yükleme
            repeatOnLifecycle(Lifecycle.State.STARTED) { // STARTED durumunda çalış
                loadInternal(silent = false) // İlk yükleme
                while (isActive) { // Coroutine aktifken döngü
                    delay(20_000) // 20 saniye bekle
                    loadInternal(silent = true) // Arka plan yenileme
                }
            }
        }
    }

    private suspend fun loadInternal(silent: Boolean) { // Analitik verisini API'den al
        if (!silent && !hasLoadedOnce) { // İlk tam yükleme
            progress.visibility = View.VISIBLE // İlerleme göster
            errorPanel.visibility = View.GONE // Hatayı gizle
        }
        if (silent) swipe.isRefreshing = true // Çekerek yenile animasyonu
        try {
            val r = RetrofitClient.instance.getSystemAnalytics() // Analitik API
            withContext(Dispatchers.Main) { // UI iş parçacığında güncelle
                progress.visibility = View.GONE // İlerlemeyi kapat
                swipe.isRefreshing = false // Yenile animasyonunu durdur
                if (!r.isSuccessful) { // HTTP hatası
                    val msg = readErrorMessage(r) // Hata mesajı
                    if (!hasLoadedOnce) { // İlk yükleme başarısız
                        tvError.text = msg // Panelde göster
                        errorPanel.visibility = View.VISIBLE // Hata paneli aç
                        content.visibility = View.GONE // İçeriği gizle
                    } else {
                        Toast.makeText(this@AdminSystemAnalyticsActivity, msg, Toast.LENGTH_LONG).show() // Toast
                    }
                    return@withContext
                }
                val d = r.body() // Yanıt gövdesi
                if (d == null) { // Boş yanıt
                    if (!hasLoadedOnce) { // İlk yükleme boş
                        tvError.text = "Boş yanıt" // Hata metni
                        errorPanel.visibility = View.VISIBLE // Panel göster
                        content.visibility = View.GONE // İçerik gizle
                    }
                    return@withContext
                }
                hasLoadedOnce = true // Başarılı yükleme işareti
                errorPanel.visibility = View.GONE // Hatayı gizle
                content.visibility = View.VISIBLE // İçeriği göster
                bindUi(d) // UI'ı veriye bağla
            }
        } catch (e: Exception) { // Ağ veya beklenmeyen hata
            withContext(Dispatchers.Main) { // UI'da hata göster
                progress.visibility = View.GONE // İlerlemeyi kapat
                swipe.isRefreshing = false // Yenilemeyi durdur
                val msg = e.message ?: "Hata" // Hata metni
                if (!hasLoadedOnce) { // İlk yükleme hatası
                    tvError.text = msg // Panel metni
                    errorPanel.visibility = View.VISIBLE // Hata paneli
                    content.visibility = View.GONE // İçerik gizle
                } else {
                    Toast.makeText(this@AdminSystemAnalyticsActivity, msg, Toast.LENGTH_LONG).show() // Toast
                }
            }
        }
    }

    private fun bindUi(d: SystemAnalyticsResponse) { // Analitik verisini ekrana yansıt
        val hours = d.dataWindowHours ?: 24 // Veri penceresi saati
        findViewById<TextView>(R.id.tvSysAnSubtitle).text =
            "Teknik metrikler, sunucu performansı ve güvenlik (son $hours saat, periyodik yenileme)" // Alt başlık

        val note = findViewById<TextView>(R.id.tvSysAnDataNote) // Veri notu alanı
        if (d.dataNote.isNullOrBlank()) { // Not yoksa
            note.visibility = View.GONE // Gizle
        } else {
            note.text = d.dataNote // Notu yaz
            note.visibility = View.VISIBLE // Göster
        }

        val gen = findViewById<TextView>(R.id.tvSysAnGenerated) // Üretim zamanı
        val genMs = parseIsoToMillis(d.generatedAtUtc) // UTC milisaniye
        if (genMs == null) { // Tarih yok
            gen.visibility = View.GONE // Gizle
        } else {
            gen.text = "Son üretim: ${
                SimpleDateFormat("d.MM.yyyy HH:mm", Locale("tr", "TR")).format(Date(genMs))
            }" // Türkçe tarih-saat
            gen.visibility = View.VISIBLE // Göster
        }

        val k = d.kpis // KPI özeti
        val tr = Locale("tr", "TR") // Türkçe yerel
        findViewById<TextView>(R.id.kpiApiValue).text =
            NumberFormat.getIntegerInstance(tr).format(k.apiRequestsPerHour) // Saatlik API isteği
        findViewById<TextView>(R.id.kpiApiSub).text = deltaLine(k.apiRequestsPerHourDeltaPercent) // API değişim

        findViewById<TextView>(R.id.kpiQueryValue).text = "${k.avgQueryTimeMs} ms" // Ortalama sorgu süresi
        findViewById<TextView>(R.id.kpiQuerySub).text = deltaLine(k.avgQueryTimeDeltaPercent) // Sorgu değişim

        findViewById<TextView>(R.id.kpiSecValue).text =
            String.format(tr, "%.2f", k.securityScore) + " / 100" // Güvenlik skoru
        findViewById<TextView>(R.id.kpiSecSub).text =
            "Açık: ${k.securityOpenIssues}" // Açık güvenlik sorunu

        findViewById<TextView>(R.id.kpiCacheValue).text =
            String.format(Locale.US, "%.1f", k.cacheHitRatioPercent) + "%" // Önbellek isabet oranı
        findViewById<TextView>(R.id.kpiCacheSub).text =
            k.cacheStatusLabel?.ifBlank { null } ?: "—" // Önbellek durum etiketi

        bindDbChart(findViewById(R.id.chartDbHourly), d.databaseHourly) // Veritabanı grafiği
        bindCacheChart(findViewById(R.id.chartCacheHourly), d.cacheHourly) // Önbellek grafiği
        bindNetChart(findViewById(R.id.chartNetHourly), d.networkHourly) // Ağ grafiği

        val boxEp = findViewById<LinearLayout>(R.id.boxEndpoints) // Uç nokta listesi
        boxEp.removeAllViews() // Önceki satırları temizle
        val eps = d.endpointPerformance.take(12) // En fazla 12 uç nokta
        if (eps.isEmpty()) { // Veri yok
            boxEp.addView(placeholderText("Uç nokta verisi yok")) // Yer tutucu
        } else {
            for (row in eps) { // Her uç nokta satırı
                boxEp.addView(endpointRowView(row)) // Satır ekle
            }
        }

        val res = d.systemResources // Sistem kaynakları
        val tvCpu = findViewById<TextView>(R.id.tvResCpuRam) // CPU/RAM metni
        val tvDisk = findViewById<TextView>(R.id.tvResDisk) // Disk metni
        val tvNet = findViewById<TextView>(R.id.tvResNet) // Ağ metni
        val tvNetNote = findViewById<TextView>(R.id.tvResNetNote) // Ağ notu
        if (res == null) { // Kaynak verisi yok
            tvCpu.text = "Kaynak verisi yok" // Bilgi metni
            tvDisk.text = "" // Boş
            tvNet.text = "" // Boş
            tvNetNote.visibility = View.GONE // Notu gizle
        } else {
            tvCpu.text = "İşlemci %${"%.0f".format(res.cpuPercent)}  ·  Bellek %${"%.0f".format(res.memoryPercent)}  ${res.memoryRefLabel ?: ""}".trim() // CPU ve bellek
            tvDisk.text = "Disk G/Ç %${"%.0f".format(res.diskIoPercent)}  ${res.diskRefLabel ?: ""}".trim() // Disk G/Ç
            tvNet.text =
                "Ağ ~${"%.2f".format(res.networkMbps)} MB/s  ↑${"%.1f".format(res.networkUp)}  ↓${"%.1f".format(res.networkDown)}" // Ağ trafiği
            val nn = res.networkNote?.trim().orEmpty() // Ağ notu metni
            if (nn.isEmpty()) { // Not yok
                tvNetNote.visibility = View.GONE // Gizle
            } else {
                tvNetNote.text = nn // Notu yaz
                tvNetNote.visibility = View.VISIBLE // Göster
            }
        }

        val boxErr = findViewById<LinearLayout>(R.id.boxErrors) // Hata kayıtları kutusu
        boxErr.removeAllViews() // Temizle
        val errs = d.errorLogs.orEmpty().take(20) // En fazla 20 hata
        if (errs.isEmpty()) { // Hata yok
            boxErr.addView(placeholderText("Hata kaydı yok")) // Yer tutucu
        } else {
            for (e in errs) { // Her hata satırı
                boxErr.addView(errorRowView(e)) // Satır ekle
            }
        }

        val boxSec = findViewById<LinearLayout>(R.id.boxSecurity) // Güvenlik olayları kutusu
        boxSec.removeAllViews() // Temizle
        val secs = d.securityEvents.orEmpty().take(20) // En fazla 20 olay
        if (secs.isEmpty()) { // Olay yok
            boxSec.addView(placeholderText("Güvenlik olayı yok")) // Yer tutucu
        } else {
            for (s in secs) { // Her güvenlik satırı
                boxSec.addView(securityRowView(s)) // Satır ekle
            }
        }
    }

    private fun deltaLine(pct: Double): String { // Yüzde değişim satırı
        val arrow = if (pct >= 0) "↗" else "↘" // Artış veya azalış oku
        return "$arrow ${String.format(Locale.US, "%.1f", kotlin.math.abs(pct))}% (önceki pencereye göre)" // Biçimli metin
    }

    private fun placeholderText(s: String): TextView { // Boş liste yer tutucusu
        return TextView(this).apply {
            text = s // Yer tutucu metin
            setTextColor(ContextCompat.getColor(this@AdminSystemAnalyticsActivity, R.color.admin_muted)) // Soluk renk
            textSize = 14f // Yazı boyutu
            setPadding(0, 8, 0, 8) // Dikey boşluk
        }
    }

    private fun endpointRowView(row: EndpointPerformanceRow): TextView { // Uç nokta performans satırı
        val line =
            "${row.endpoint}\nçağrı=${row.calls}  ort=${row.avgTimeMs} ms  hata=${row.errors}  2xx=${
                String.format(Locale.US, "%.1f", row.successRatePercent)
            }%" // Özet metin
        return TextView(this).apply {
            text = line // Satır içeriği
            setTextColor(ContextCompat.getColor(this@AdminSystemAnalyticsActivity, R.color.admin_strong)) // Koyu metin
            textSize = 12f // Küçük yazı
            setPadding(0, 10, 0, 10) // Dikey boşluk
        }
    }

    private fun errorRowView(e: ErrorLogEntry): TextView { // Hata kaydı satırı
        val line = "${e.time}  HTTP ${e.statusCode}  ${e.endpoint}\n${e.message}  (×${e.count})" // Hata özeti
        return TextView(this).apply {
            text = line // Satır içeriği
            setTextColor(ContextCompat.getColor(this@AdminSystemAnalyticsActivity, R.color.admin_strong)) // Koyu metin
            textSize = 12f // Küçük yazı
            setPadding(0, 10, 0, 10) // Dikey boşluk
        }
    }

    private fun securityRowView(s: SecurityEventEntry): TextView { // Güvenlik olayı satırı
        val line = "${s.time}  [${s.severity}] ${s.name}\n${s.obfuscatedSource}  ${s.countLabel}" // Olay özeti
        return TextView(this).apply {
            text = line // Satır içeriği
            textSize = 12f // Küçük yazı
            setPadding(12, 12, 12, 12) // İç boşluk
            setTextColor(ContextCompat.getColor(this@AdminSystemAnalyticsActivity, R.color.admin_strong)) // Koyu metin
            setBackgroundColor(toneBg(s.tone)) // Önem derecesi rengi
        }
    }

    private fun toneBg(tone: String?): Int { // Önem derecesine göre arka plan
        return when (tone?.lowercase(Locale.US)) {
            "high" -> Color.argb(0x44, 0xF8, 0x71, 0x71) // Yüksek: kırmızımsı
            "medium" -> Color.argb(0x44, 0xF5, 0x9E, 0x0B) // Orta: turuncu
            else -> Color.argb(0x33, 0x2E, 0xCC, 0x71) // Düşük: yeşilimsi
        }
    }

    private fun chartMutedGrid(): Pair<Int, Int> { // Grafik soluk metin ve ızgara renkleri
        val muted = ContextCompat.getColor(this, R.color.admin_muted) // Soluk metin
        val grid = ContextCompat.getColor(this, R.color.admin_row_stroke) // Izgara çizgisi
        return muted to grid // Çift olarak döndür
    }

    private fun bindDbChart(chart: BarChart, rows: List<HourlyDbRow>?) { // Saatlik veritabanı grafiği
        val list = rows.orEmpty() // Null güvenli liste
        val (muted, grid) = chartMutedGrid() // Renk çifti
        chart.description.isEnabled = false // Açıklama kapalı
        chart.setDrawGridBackground(false) // Arka plan ızgarası yok
        chart.axisRight.isEnabled = false // Sağ eksen kapalı
        chart.axisLeft.textColor = muted // Sol eksen metni
        chart.axisLeft.axisMinimum = 0f // Minimum sıfır
        chart.axisLeft.setDrawGridLines(true) // Yatay ızgara
        chart.axisLeft.gridColor = grid // Izgara rengi
        chart.axisLeft.gridLineWidth = 0.5f // İnce çizgi
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM // X ekseni altta
        chart.xAxis.textColor = muted // X eksen metni
        chart.xAxis.setDrawGridLines(false) // Dikey ızgara yok
        chart.xAxis.granularity = 1f // Her saat bir adım
        chart.legend.isEnabled = true // Lejant açık
        chart.legend.textColor = muted // Lejant rengi
        chart.legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP // Üstte
        chart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT // Sağda
        chart.setExtraOffsets(0f, 2f, 0f, 8f) // Kenar boşlukları

        if (list.isEmpty()) { // Veri yok
            chart.clear() // Grafiği temizle
            chart.setNoDataText("Veri yok") // Boş metin
            chart.setNoDataTextColor(muted) // Soluk renk
            return
        }
        val entries = list.mapIndexed { i, r ->
            BarEntry(
                i.toFloat(),
                floatArrayOf(r.reads.toFloat(), r.writes.toFloat(), r.slowQueries.toFloat())
            ) // Okuma, yazma, yavaş sorgu
        }
        val set = BarDataSet(entries, "") // Yığılmış çubuk seti
        set.setColors(
            mutableListOf(
                Color.parseColor("#2ECC71"),
                Color.parseColor("#3498DB"),
                Color.parseColor("#E74C3C")
            )
        ) // Yeşil, mavi, kırmızı
        set.stackLabels = arrayOf("Okuma", "Yazma", "Yavaş") // Yığın etiketleri
        val data = BarData(set) // Çubuk verisi
        data.barWidth = 0.7f // Çubuk genişliği
        chart.data = data // Grafiğe bağla
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(list.map { it.label }) // Saat etiketleri
        chart.xAxis.labelRotationAngle = -35f // Etiketleri eğ
        chart.invalidate() // Yeniden çiz
    }

    private fun bindCacheChart(chart: BarChart, rows: List<HourlyCacheRow>?) { // Saatlik önbellek grafiği
        val list = rows.orEmpty() // Null güvenli liste
        val (muted, grid) = chartMutedGrid() // Renk çifti
        chart.description.isEnabled = false // Açıklama kapalı
        chart.setDrawGridBackground(false) // Arka plan ızgarası yok
        chart.axisRight.isEnabled = false // Sağ eksen kapalı
        chart.axisLeft.textColor = muted // Sol eksen metni
        chart.axisLeft.axisMinimum = 0f // Minimum sıfır
        chart.axisLeft.setDrawGridLines(true) // Yatay ızgara
        chart.axisLeft.gridColor = grid // Izgara rengi
        chart.axisLeft.gridLineWidth = 0.5f // İnce çizgi
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM // X ekseni altta
        chart.xAxis.textColor = muted // X eksen metni
        chart.xAxis.setDrawGridLines(false) // Dikey ızgara yok
        chart.xAxis.granularity = 1f // Her saat bir adım
        chart.legend.isEnabled = true // Lejant açık
        chart.legend.textColor = muted // Lejant rengi
        chart.legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP // Üstte
        chart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT // Sağda
        chart.setExtraOffsets(0f, 2f, 0f, 8f) // Kenar boşlukları

        if (list.isEmpty()) { // Veri yok
            chart.clear() // Grafiği temizle
            chart.setNoDataText("Veri yok") // Boş metin
            chart.setNoDataTextColor(muted) // Soluk renk
            return
        }
        val entries = list.mapIndexed { i, r ->
            BarEntry(i.toFloat(), floatArrayOf(r.hits.toFloat(), r.misses.toFloat()))
        } // Hit ve miss
        val set = BarDataSet(entries, "") // Yığılmış çubuk seti
        set.setColors(
            mutableListOf(
                Color.parseColor("#1ABC9C"),
                Color.parseColor("#F39C12")
            )
        ) // Turkuaz ve turuncu
        set.stackLabels = arrayOf("Hit", "Miss") // Yığın etiketleri
        val data = BarData(set) // Çubuk verisi
        data.barWidth = 0.7f // Çubuk genişliği
        chart.data = data // Grafiğe bağla
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(list.map { it.label }) // Saat etiketleri
        chart.xAxis.labelRotationAngle = -35f // Etiketleri eğ
        chart.invalidate() // Yeniden çiz
    }

    private fun bindNetChart(chart: LineChart, rows: List<HourlyNetRow>?) { // Saatlik ağ trafiği grafiği
        val list = rows.orEmpty() // Null güvenli liste
        val (muted, grid) = chartMutedGrid() // Renk çifti
        chart.description.isEnabled = false // Açıklama kapalı
        chart.setDrawGridBackground(false) // Arka plan ızgarası yok
        chart.axisRight.isEnabled = false // Sağ eksen kapalı
        chart.axisLeft.textColor = muted // Sol eksen metni
        chart.axisLeft.setDrawGridLines(true) // Yatay ızgara
        chart.axisLeft.gridColor = grid // Izgara rengi
        chart.axisLeft.gridLineWidth = 0.5f // İnce çizgi
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM // X ekseni altta
        chart.xAxis.textColor = muted // X eksen metni
        chart.xAxis.setDrawGridLines(false) // Dikey ızgara yok
        chart.xAxis.granularity = 1f // Her saat bir adım
        chart.legend.isEnabled = true // Lejant açık
        chart.legend.textColor = muted // Lejant rengi
        chart.setExtraOffsets(0f, 2f, 0f, 8f) // Kenar boşlukları

        if (list.isEmpty()) { // Veri yok
            chart.clear() // Grafiği temizle
            chart.setNoDataText("Veri yok") // Boş metin
            chart.setNoDataTextColor(muted) // Soluk renk
            return
        }
        val inc = list.mapIndexed { i, r -> Entry(i.toFloat(), r.incomingMbps.toFloat()) } // Gelen trafik
        val out = list.mapIndexed { i, r -> Entry(i.toFloat(), r.outgoingMbps.toFloat()) } // Giden trafik
        val ds1 = LineDataSet(inc, "Gelen Mb/s") // Gelen çizgi seti
        ds1.color = Color.parseColor("#8B5CF6") // Mor çizgi
        ds1.lineWidth = 2f // Çizgi kalınlığı
        ds1.setDrawCircles(true) // Nokta işaretleri
        ds1.mode = LineDataSet.Mode.CUBIC_BEZIER // Yumuşak eğri
        val ds2 = LineDataSet(out, "Giden Mb/s") // Giden çizgi seti
        ds2.color = Color.parseColor("#F59E0B") // Turuncu çizgi
        ds2.lineWidth = 2f // Çizgi kalınlığı
        ds2.setDrawCircles(true) // Nokta işaretleri
        ds2.mode = LineDataSet.Mode.CUBIC_BEZIER // Yumuşak eğri
        chart.data = LineData(ds1, ds2) // İki çizgiyi bağla
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(list.map { it.label }) // Saat etiketleri
        chart.xAxis.labelRotationAngle = -35f // Etiketleri eğ
        chart.invalidate() // Yeniden çiz
    }

    private fun readErrorMessage(response: Response<*>): String { // API hata gövdesini oku
        val raw = response.errorBody()?.string().orEmpty() // Ham hata metni
        return try {
            JSONObject(raw).optString("message").ifBlank { "HTTP ${response.code()}" } // JSON mesajı
        } catch (_: Exception) {
            if (raw.isNotBlank()) raw else "HTTP ${response.code()}" // Yedek metin
        }
    }

    private fun parseIsoToMillis(raw: String?): Long? { // ISO string'i milisaniyeye çevir
        if (raw.isNullOrBlank()) return null // Boşsa geçersiz
        val tries = listOf( // Denenecek formatlar
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC") // UTC zaman dilimi
            },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US)
        )
        for (fmt in tries) { // Her formatı dene
            try {
                val d = fmt.parse(raw) ?: continue // Ayrıştır
                return d.time // Milisaniye döndür
            } catch (_: Exception) { }
        }
        return null // Hiçbiri tutmadı
    }
}
