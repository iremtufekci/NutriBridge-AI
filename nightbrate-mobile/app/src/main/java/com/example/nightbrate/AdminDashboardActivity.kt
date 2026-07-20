package com.example.nightbrate // Paket tanımı

import android.graphics.Color // Renk işlemleri
import android.os.Bundle // Activity durum paketi
import androidx.core.content.ContextCompat // Kaynak renk çözümleme
import android.view.LayoutInflater // Satır şablonu şişirme
import android.view.View // Görünüm temel sınıfı
import android.widget.LinearLayout // Dikey liste konteyneri
import android.widget.ProgressBar // Yükleme göstergesi
import android.widget.TextView // Metin görünümü
import android.widget.Toast // Kısa bildirim
import androidx.appcompat.app.AppCompatActivity // Temel Activity
import androidx.lifecycle.lifecycleScope // Yaşam döngüsü coroutine kapsamı
import com.example.nightbrate.ActivityWindowHelper.applyStandardContentWindow // Standart pencere düzeni
import com.github.mikephil.charting.components.XAxis // Grafik X ekseni
import com.github.mikephil.charting.data.Entry // Grafik veri noktası
import com.github.mikephil.charting.data.LineData // Çizgi grafik verisi
import com.github.mikephil.charting.data.LineDataSet // Çizgi veri seti
import com.github.mikephil.charting.data.PieData // Pasta grafik verisi
import com.github.mikephil.charting.data.PieDataSet // Pasta veri seti
import com.github.mikephil.charting.data.PieEntry // Pasta dilim girişi
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter // İndeks eksen etiketleri
import com.github.mikephil.charting.formatter.ValueFormatter // Değer biçimlendirici
import kotlinx.coroutines.async // Paralel async görev
import kotlinx.coroutines.coroutineScope // Alt coroutine kapsamı
import kotlinx.coroutines.launch // Coroutine başlatma
import java.text.ParseException // Tarih ayrıştırma hatası
import java.text.SimpleDateFormat // Tarih biçimlendirme
import java.util.Date // Tarih nesnesi
import java.util.Locale // Yerel ayar
import java.util.TimeZone // Saat dilimi
import java.util.Calendar // Takvim hesabı
import kotlin.math.abs // Mutlak değer

class AdminDashboardActivity : AppCompatActivity() { // Admin ana panel ekranı

    private fun parseActivityCreatedAtToMillis(value: String?): Long? { // ISO tarihi milisaniyeye çevir
        if (value.isNullOrBlank()) return null // Boşsa geçersiz
        val tries = listOf( // Denenecek tarih formatları
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" to TimeZone.getTimeZone("UTC"),
            "yyyy-MM-dd'T'HH:mm:ss'Z'" to TimeZone.getTimeZone("UTC"),
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX" to null,
            "yyyy-MM-dd'T'HH:mm:ssXXX" to null,
            "yyyy-MM-dd'T'HH:mm:ss.SSS" to null,
            "yyyy-MM-dd'T'HH:mm:ss" to null
        )
        for ((pat, zone) in tries) { // Her formatı sırayla dene
            try {
                val sdf = SimpleDateFormat(pat, Locale.US) // Biçimlendirici oluştur
                if (zone != null) sdf.timeZone = zone // UTC varsa uygula
                val d = sdf.parse(value) ?: continue // Ayrıştır, başarısızsa devam
                return d.time // Milisaniye döndür
            } catch (_: ParseException) {
                continue // Sonraki formatı dene
            }
        }
        return null // Hiçbiri tutmadı
    }

    private fun formatTimeAgoTr(createdAt: String?): String { // Türkçe göreli zaman metni
        val then = parseActivityCreatedAtToMillis(createdAt) ?: return "—" // Tarih yoksa tire
        val diff = System.currentTimeMillis() - then // Geçen süre ms
        if (diff < 0) return "Az önce" // Gelecek tarih koruması
        val s = diff / 1000 // Saniye
        if (s < 60) return "Az önce" // Bir dakikadan az
        val m = s / 60 // Dakika
        if (m < 60) return "$m dk önce" // Saatten az
        val h = m / 60 // Saat
        if (h < 24) return "$h saat önce" // Günden az
        val days = h / 24 // Gün
        if (days < 7) return "$days gün önce" // Haftadan az
        return SimpleDateFormat("d MMM yyyy", Locale("tr", "TR")).format(Date(then)) // Uzun tarih
    }

    private fun registrationMomPercent(monthly: List<MonthlyRegistrationItem>): String? { // Aylık kayıt değişim yüzdesi
        if (monthly.size < 2) return null // Karşılaştırma için en az 2 ay gerek
        val last = monthly[monthly.size - 1].count // Son ay sayısı
        val prev = monthly[monthly.size - 2].count // Önceki ay sayısı
        if (prev == 0L) return if (last > 0) "+100%" else null // Sıfırdan artış
        val p = (last - prev) * 100.0 / prev // Yüzde değişim
        return "${if (p >= 0) "+" else "-"}${String.format(Locale.US, "%.1f", abs(p))}%" // İşaretli yüzde metni
    }

    override fun onCreate(savedInstanceState: Bundle?) { // Activity oluşturulduğunda
        super.onCreate(savedInstanceState) // Üst sınıf başlatma
        applyStandardContentWindow() // Standart içerik penceresi
        setContentView(R.layout.activity_admin_dashboard) // Panel düzeni
        AdminBottomBarHelper.bind(this, 0) // Alt sekme çubuğu (panel)
        loadDashboard() // Verileri yükle
    }

    private fun loadDashboard() { // API'den panel verilerini çek
        val progress = findViewById<ProgressBar>(R.id.adminDashProgress) // Yükleme çubuğu
        val err = findViewById<TextView>(R.id.tvAdminDashError) // Hata metni
        progress.visibility = View.VISIBLE // Yükleniyor göster
        err.visibility = View.GONE // Hatayı gizle
        lifecycleScope.launch { // Arka planda istek
            try {
                coroutineScope { // Paralel istekler
                    val statsD = async { RetrofitClient.instance.getAdminDashboardStats() } // İstatistikler
                    val actD = async { RetrofitClient.instance.getRecentActivities(15) } // Son aktiviteler
                    val r = statsD.await() // İstatistik yanıtını bekle
                    progress.visibility = View.GONE // Yükleme bitti
                    if (!r.isSuccessful) { // HTTP hatası
                        err.text = "Veri alınamadı (${r.code()})" // Hata mesajı
                        err.visibility = View.VISIBLE // Hatayı göster
                        return@coroutineScope // İşlemi durdur
                    }
                    val s = r.body() ?: return@coroutineScope // Gövde yoksa çık
                    findViewById<TextView>(R.id.tvStatActiveUsers).text = s.activeUsers.toString() // Aktif kullanıcı
                    findViewById<TextView>(R.id.tvStatActiveDietitians).text = s.activeDietitians.toString() // Aktif diyetisyen
                    findViewById<TextView>(R.id.tvStatPending).text = s.pendingDietitians.toString() // Bekleyen onay
                    findViewById<TextView>(R.id.tvStatTotalUsers).text = s.totalUsers.toString() // Toplam kullanıcı
                    val mom = registrationMomPercent(s.monthlyRegistrations) // Aylık trend yüzdesi
                    val trendTv = findViewById<TextView>(R.id.tvCard4Trend) // Trend etiketi
                    if (mom != null) { // Trend hesaplandıysa
                        trendTv.text = "↑ $mom · son aya göre yeni kayıt" // Artış metni
                    } else {
                        trendTv.text = "" // Trend yoksa boş
                    }
                    setupMonthlyChart(s.monthlyRegistrations) // Aylık çizgi grafik
                    setupPieChart(s.roleDistribution) // Rol pasta grafik
                    val ar = actD.await() // Aktivite yanıtını bekle
                    val list = if (ar.isSuccessful) ar.body().orEmpty() else emptyList() // Liste veya boş
                    bindRecentActivities(list) // Aktivite satırlarını bağla
                }
            } catch (e: Exception) { // Ağ veya beklenmeyen hata
                progress.visibility = View.GONE // Yüklemeyi kapat
                err.text = e.message ?: "Bağlantı hatası" // Hata metni
                err.visibility = View.VISIBLE // Panelde göster
                Toast.makeText(this@AdminDashboardActivity, err.text, Toast.LENGTH_LONG).show() // Toast bildirimi
            }
        }
    }

    private fun bindRecentActivities(activities: List<ActivityItemDto>) { // Son aktivite listesini doldur
        val container = findViewById<LinearLayout>(R.id.llRecentActivities) // Liste konteyneri
        val empty = findViewById<TextView>(R.id.tvRecentActivitiesEmpty) // Boş durum metni
        container.removeAllViews() // Önceki satırları temizle
        if (activities.isEmpty()) { // Kayıt yoksa
            empty.visibility = View.VISIBLE // Boş mesajı göster
            return
        }
        empty.visibility = View.GONE // Boş mesajını gizle
        val inflater = LayoutInflater.from(this) // Satır şişirici
        for (item in activities) { // Her aktivite için satır
            val row = inflater.inflate(R.layout.item_activity_row, container, false) // Satır şablonu
            val initial = (item.initial?.take(1) ?: "?").uppercase() // Avatar harfi
            row.findViewById<TextView>(R.id.tvActivityInitial).text = initial // Harfi yaz
            row.findViewById<TextView>(R.id.tvActivityName).text = item.actorDisplayName ?: "—" // Aktör adı
            row.findViewById<TextView>(R.id.tvActivityDescription).text =
                ActivityDescriptionNormalize.toDisplay(item.description) // Açıklama metni
            row.findViewById<TextView>(R.id.tvActivityTimeAgo).text = formatTimeAgoTr(item.createdAt) // Göreli zaman
            container.addView(row) // Satırı listeye ekle
        }
    }

    private fun monthLabelTr(year: Int, month: Int): String { // Ay etiketi (Türkçe kısa)
        val cal = Calendar.getInstance(Locale("tr", "TR")) // Türkçe takvim
        cal.set(Calendar.YEAR, year) // Yılı ayarla
        cal.set(Calendar.MONTH, month - 1) // Ayı ayarla (0 tabanlı)
        cal.set(Calendar.DAY_OF_MONTH, 1) // Ayın ilk günü
        return SimpleDateFormat("MMM yy", Locale("tr", "TR")).format(cal.time) // Örn. Oca 25
    }

    private fun roleTr(role: String?): String = when (role) { // Rol adını Türkçeleştir
        "Admin" -> "Yönetici" // Admin rolü
        "Client" -> "Danışan" // Danışan rolü
        "Dietitian" -> "Diyetisyen" // Diyetisyen rolü
        else -> role.orEmpty() // Bilinmeyen rol olduğu gibi
    }

    private fun setupMonthlyChart(monthly: List<MonthlyRegistrationItem>) { // Aylık kayıt çizgi grafiği
        val chart = findViewById<com.github.mikephil.charting.charts.LineChart>(R.id.chartMonthly) // Grafik görünümü
        chart.description.isEnabled = false // Açıklama kapalı
        chart.setTouchEnabled(true) // Dokunma etkin
        chart.setDrawGridBackground(false) // Arka plan ızgarası yok
        chart.legend.isEnabled = false // Lejant kapalı
        chart.setDrawBorders(false) // Kenarlık yok
        chart.axisRight.isEnabled = false // Sağ eksen kapalı
        val muted = ContextCompat.getColor(this, R.color.admin_muted) // Soluk metin rengi
        val grid = ContextCompat.getColor(this, R.color.admin_row_stroke) // Izgara rengi
        val brand = ContextCompat.getColor(this, R.color.admin_brand) // Marka rengi
        chart.axisLeft.textColor = muted // Sol eksen metni
        chart.axisLeft.axisMinimum = 0f // Minimum sıfır
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM // X ekseni altta
        chart.xAxis.textColor = muted // X eksen metni
        chart.xAxis.setDrawGridLines(true) // Dikey ızgara
        chart.xAxis.gridColor = grid // Izgara rengi
        chart.xAxis.gridLineWidth = 0.5f // İnce çizgi
        chart.axisLeft.setDrawGridLines(true) // Yatay ızgara
        chart.axisLeft.gridColor = grid // Sol ızgara rengi
        chart.axisLeft.gridLineWidth = 0.5f // İnce çizgi

        if (monthly.isEmpty()) { // Veri yoksa
            chart.clear() // Grafiği temizle
            chart.setNoDataText("Kayıt yok") // Boş metin
            chart.setNoDataTextColor(muted) // Soluk renk
            return
        }
        val entries = monthly.mapIndexed { i, m -> Entry(i.toFloat(), m.count.toFloat()) } // Veri noktaları
        val set = LineDataSet(entries, "Yeni kayıt") // Çizgi seti
        set.color = brand // Çizgi rengi
        set.setDrawCircles(true) // Nokta işaretleri
        set.setDrawFilled(true) // Alt dolgu
        set.fillColor = Color.argb(0x33, Color.red(brand), Color.green(brand), Color.blue(brand)) // Yarı saydam dolgu
        set.lineWidth = 2f // Çizgi kalınlığı
        set.mode = LineDataSet.Mode.CUBIC_BEZIER // Yumuşak eğri
        chart.data = LineData(set) // Veriyi grafiğe bağla
        val labels = monthly.map { monthLabelTr(it.year, it.month) } // X eksen etiketleri
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels) // Etiket biçimlendirici
        chart.xAxis.granularity = 1f // Her ay bir adım
        chart.xAxis.labelRotationAngle = -25f // Etiketleri eğ
        chart.invalidate() // Grafiği yeniden çiz
    }

    private fun setupPieChart(roles: List<RoleCountItem>) { // Rol dağılımı pasta grafiği
        val chart = findViewById<com.github.mikephil.charting.charts.PieChart>(R.id.chartRoles) // Pasta grafik
        chart.description.isEnabled = false // Açıklama kapalı
        chart.setUsePercentValues(false) // Ham sayı göster
        chart.setDrawEntryLabels(false) // Dilim etiketi kapalı
        chart.legend.isEnabled = false // Lejant kapalı
        chart.setEntryLabelColor(Color.WHITE) // Etiket rengi
        chart.setHoleColor(ContextCompat.getColor(this, R.color.admin_card_bg)) // Orta delik rengi
        chart.setTransparentCircleAlpha(0) // Şeffaf halka yok
        chart.holeRadius = 52f // Delik yarıçapı
        chart.transparentCircleRadius = 56f // Dış halka yarıçapı

        val list = roles.filter { it.count > 0 } // Sıfır olmayan roller
        if (list.isEmpty()) { // Rol verisi yok
            chart.setNoDataText("Rol verisi yok") // Boş metin
            chart.setNoDataTextColor(ContextCompat.getColor(this, R.color.admin_muted)) // Soluk renk
            return
        }
        val colors = listOf( // Dilim renk paleti
            Color.parseColor("#2ECC71"),
            Color.parseColor("#1ABC9C"),
            Color.parseColor("#F39C12")
        )
        val pieEntries = list.map { PieEntry(it.count.toFloat(), roleTr(it.role)) } // Pasta girişleri
        val set = PieDataSet(pieEntries, "") // Veri seti
        set.colors = colors.take(list.size) // Rol sayısına göre renk
        set.valueTextSize = 12f // Değer yazı boyutu
        set.valueTextColor = Color.WHITE // Değer rengi
        val data = PieData(set) // Pasta verisi
        data.setValueFormatter(object : ValueFormatter() { // Tam sayı biçimlendirici
            override fun getFormattedValue(value: Float): String = value.toInt().toString() // Ondalıksız göster
        })
        chart.data = data // Veriyi grafiğe bağla
        chart.invalidate() // Grafiği yeniden çiz
    }
}
