package com.example.nightbrate // Paket tanımı

import android.graphics.Typeface // Kalın yazı tipi
import android.os.Bundle // Activity durum paketi
import android.view.View // Görünüm temel sınıfı
import android.widget.LinearLayout // Dikey düzen
import android.widget.ProgressBar // Yükleniyor göstergesi
import android.widget.TextView // Metin görünümü
import androidx.appcompat.app.AppCompatActivity // Temel Activity
import androidx.core.content.ContextCompat // Tema renkleri
import androidx.lifecycle.lifecycleScope // Yaşam döngüsü coroutine
import kotlinx.coroutines.launch // Coroutine başlat
import java.text.SimpleDateFormat // Tarih biçimlendirme
import java.util.Calendar // Takvim
import java.util.Locale // Yerel ayar

class ClientDietProgramHistoryActivity : AppCompatActivity() { // Geçmiş diyet programları ekranı

    private var allPast: List<ClientDietProgramDayResponse> = emptyList() // Tüm geçmiş programlar
    private var monthFilter: String = "all" // Ay filtresi ("all" veya yyyy-MM)
    private lateinit var filterRow: LinearLayout // Ay filtre chip satırı
    private lateinit var list: LinearLayout // Program listesi konteyneri
    private lateinit var empty: TextView // Boş durum metni
    private lateinit var countLabel: TextView // Kayıt sayısı etiketi

    private fun startOfCalDay(c: Calendar): Calendar { // Gün başlangıcına sıfırla
        return (c.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun isYmdBeforeToday(ymd: String): Boolean { // yyyy-MM-dd bugünden önce mi
        val parts = ymd.trim().split("-") // Tarih parçaları
        if (parts.size != 3) return false // Geçersiz format
        return try {
            val y = parts[0].toInt() // Yıl
            val m = parts[1].toInt() - 1 // Ay (0 tabanlı)
            val d = parts[2].toInt() // Gün
            val dayCal = Calendar.getInstance() // Hedef gün takvimi
            dayCal.set(y, m, d, 12, 0, 0) // Öğlen saati (DST güvenli)
            dayCal.set(Calendar.MILLISECOND, 0)
            val t0 = startOfCalDay(Calendar.getInstance()) // Bugün başlangıcı
            startOfCalDay(dayCal).before(t0) // Bugünden önce mi
        } catch (_: Exception) {
            false // Parse hatası
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) { // Activity oluşturulduğunda
        super.onCreate(savedInstanceState) // Üst sınıf onCreate
        setContentView(R.layout.activity_client_diet_history) // Geçmiş layout'u
        ClientBottomBarHelper.bind(this, 2) // Alt çubuk: Geçmiş sekmesi
        list = findViewById(R.id.clientDietHistoryList) // Liste konteyneri
        empty = findViewById(R.id.tvClientDietHistoryEmpty) // Boş metin
        val scrollContent = list.parent as LinearLayout // Kaydırılabilir içerik
        filterRow = LinearLayout(this).apply { // Ay filtre satırı (dinamik)
            orientation = LinearLayout.HORIZONTAL // Yatay chip'ler
            visibility = View.GONE // Başlangıçta gizli
        }
        countLabel = TextView(this).apply { // Kayıt sayısı etiketi
            setTextColor(ContextCompat.getColor(this@ClientDietProgramHistoryActivity, R.color.text_gray))
            textSize = 12f
            setPadding(0, (8 * resources.displayMetrics.density).toInt(), 0, 0)
        }
        val filterIdx = scrollContent.indexOfChild(findViewById(R.id.progressClientDietHistory)) + 1 // Ekleme konumu
        scrollContent.addView(filterRow, filterIdx) // Filtre satırını ekle
        scrollContent.addView(countLabel, filterIdx + 1) // Sayı etiketini ekle
        loadPrograms() // Geçmiş programları API'den yükle
    }

    private fun monthKey(ymd: String): String? { // yyyy-MM-dd → yyyy-MM ay anahtarı
        val parts = ymd.split("-")
        return if (parts.size >= 2) "${parts[0]}-${parts[1]}" else null
    }

    private fun monthLabelTr(ym: String): String { // yyyy-MM → Türkçe ay etiketi
        return try {
            val p = ym.split("-")
            val c = Calendar.getInstance()
            c.set(p[0].toInt(), p[1].toInt() - 1, 1) // Ayın 1'i
            SimpleDateFormat("MMMM yyyy", Locale("tr", "TR")).format(c.time) // Örn. Haziran 2026
        } catch (_: Exception) {
            ym // Hata: ham anahtar
        }
    }

    private fun filteredPast(): List<ClientDietProgramDayResponse> { // Filtrelenmiş geçmiş listesi
        if (monthFilter == "all") return allPast // Tümü
        return allPast.filter { monthKey(it.programDate.orEmpty()) == monthFilter } // Seçili ay
    }

    private fun rebuildMonthChips() { // Ay filtre chip'lerini yeniden oluştur
        filterRow.removeAllViews() // Eski chip'leri temizle
        val months = allPast.mapNotNull { monthKey(it.programDate.orEmpty()) }.distinct().sortedDescending() // Benzersiz aylar
        if (months.isEmpty()) { // Ay yok
            filterRow.visibility = View.GONE // Filtre gizle
            return
        }
        filterRow.visibility = View.VISIBLE // Filtre göster
        val padH = (10 * resources.displayMetrics.density).toInt() // Yatay padding
        val padV = (6 * resources.displayMetrics.density).toInt() // Dikey padding
        fun addChip(label: String, value: String) { // Tek chip ekle
            val tv = TextView(this).apply {
                text = label // Chip metni
                setPadding(padH, padV, padH, padV)
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setOnClickListener { // Chip tıklama
                    monthFilter = value // Filtreyi güncelle
                    rebuildMonthChips() // Chip stillerini yenile
                    renderList() // Listeyi yeniden çiz
                }
            }
            val active = monthFilter == value // Seçili chip mi
            if (active) { // Aktif stil
                tv.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_green))
                tv.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else { // Pasif stil
                tv.setBackgroundResource(R.drawable.client_day_normal_bg)
                tv.setTextColor(ContextCompat.getColor(this, R.color.text_gray))
            }
            filterRow.addView(
                tv,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = (6 * resources.displayMetrics.density).toInt()
                }
            )
        }
        addChip("Tümü", "all") // Tümü chip'i
        for (m in months) addChip(monthLabelTr(m), m) // Her ay için chip
    }

    private fun mealDone(p: ClientDietProgramDayResponse, idx: Int): Boolean = when (idx) { // Öğün tamamlandı mı (indeks)
        0 -> p.breakfastCompleted == true // Kahvaltı
        1 -> p.lunchCompleted == true // Öğle
        2 -> p.dinnerCompleted == true // Akşam
        else -> p.snackCompleted == true // Ara öğün
    }

    private fun renderList() { // Geçmiş program listesini çiz
        list.removeAllViews() // Eski kartları temizle
        val past = filteredPast() // Filtrelenmiş kayıtlar
        countLabel.text = "${past.size} kayıt gösteriliyor" // Sayı etiketi
        countLabel.visibility = if (allPast.isEmpty()) View.GONE else View.VISIBLE // Boşsa gizle
        if (past.isEmpty()) { // Liste boş
            empty.visibility = View.VISIBLE // Boş mesaj
            empty.text = if (allPast.isEmpty()) {
                "Geçmişe ait program yok."
            } else {
                "Seçilen ay için kayıt yok."
            }
            return
        }
        empty.visibility = View.GONE // Boş mesaj gizle
        val fmtTitle = SimpleDateFormat("d MMMM yyyy, EEEE", Locale("tr", "TR")) // Kart başlık formatı
        val pad = (12 * resources.displayMetrics.density).toInt() // Kart padding
        for (p in past) { // Her geçmiş gün için kart
                    val ymd = p.programDate ?: continue // Tarih yoksa atla
                    val cal = try { // Tarihi takvime çevir
                        val parts = ymd.split("-")
                        val c = Calendar.getInstance()
                        c.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 12, 0, 0)
                        c
                    } catch (_: Exception) {
                        null
                    }
                    val head = if (cal != null) { // Başlık metni
                        val cap = fmtTitle.format(cal.time)
                        cap.replaceFirstChar { it.uppercase() } // İlk harf büyük
                    } else ymd // Parse edilemezse ham ymd
                    val card = LinearLayout(this@ClientDietProgramHistoryActivity).apply { // Kart konteyneri
                        orientation = LinearLayout.VERTICAL
                        setBackgroundResource(R.drawable.custom_input_bg)
                        setPadding(pad, pad, pad, pad)
                        val m = (8 * resources.displayMetrics.density).toInt()
                        val lp = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        lp.bottomMargin = m // Kartlar arası boşluk
                        layoutParams = lp
                    }
                    val t1 = TextView(this@ClientDietProgramHistoryActivity).apply { // Tarih başlığı
                        text = head
                        setTextColor(ContextCompat.getColor(this@ClientDietProgramHistoryActivity, R.color.white))
                        textSize = 16f
                        setTypeface(null, Typeface.BOLD)
                    }
                    val t2 = TextView(this@ClientDietProgramHistoryActivity).apply { // Ham ymd alt satır
                        text = ymd
                        setTextColor(ContextCompat.getColor(this@ClientDietProgramHistoryActivity, R.color.text_gray))
                        textSize = 12f
                    }
                    card.addView(t1) // Başlık ekle
                    card.addView(t2) // Alt tarih ekle
                    val totalK = p.totalCalories ?: 0 // Günlük toplam kalori
                    val tTot = TextView(this@ClientDietProgramHistoryActivity).apply { // Toplam satırı
                        text = "Günlük toplam: $totalK kkal"
                        setTextColor(ContextCompat.getColor(this@ClientDietProgramHistoryActivity, R.color.primary_green))
                        textSize = 14f
                        setTypeface(null, Typeface.BOLD)
                    }
                    card.addView(tTot)
                    val bck = p.breakfastCalories ?: 0 // Kahvaltı kalori
                    val lck = p.lunchCalories ?: 0 // Öğle kalori
                    val dck = p.dinnerCalories ?: 0 // Akşam kalori
                    val sck = p.snackCalories ?: 0 // Ara öğün kalori
                    val perSum = bck + lck + dck + sck // Öğün toplamı
                    fun kFor(i: Int) = if (perSum > 0) { // Öğün indeksine kalori
                        when (i) {
                            0 -> maxOf(0, bck)
                            1 -> maxOf(0, lck)
                            2 -> maxOf(0, dck)
                            else -> maxOf(0, sck)
                        }
                    } else {
                        if (totalK > 0) totalK / 4 else 0 // Eski kayıt: toplam/4
                    }
                    val mealLines = listOf( // Öğün satır tanımları
                        Triple("Kahvaltı", p.breakfast, 0),
                        Triple("Öğle", p.lunch, 1),
                        Triple("Akşam", p.dinner, 2),
                        Triple("Ara öğün", p.snack, 3)
                    )
                    for ((label, value, idx) in mealLines) { // Her öğün satırı
                        val mk = kFor(idx) // Öğün kalorisi
                        if (value.isNullOrBlank() && mk == 0) continue // Boş öğün atla
                        val doneSuffix = if (mealDone(p, idx)) "  ✓ Tamamlandı" else "" // Tamamlandı işareti
                        val line = if (value.isNullOrBlank()) { // Sadece kalori satırı
                            "Sadece $mk kkal$doneSuffix"
                        } else { // Metin + kalori satırı
                            "$label ($mk kkal)$doneSuffix: $value"
                        }
                        val row = TextView(this@ClientDietProgramHistoryActivity).apply {
                            text = line
                            setTextColor(
                                ContextCompat.getColor(
                                    this@ClientDietProgramHistoryActivity,
                                    if (mealDone(p, idx)) R.color.primary_green else R.color.text_gray // Tamamlandıysa yeşil
                                )
                            )
                            textSize = 13f
                        }
                        card.addView(row) // Öğün satırını karta ekle
                    }
                    p.updatedAt?.let { u -> // Son güncelleme varsa
                        if (u.isNotBlank()) {
                            val tU = TextView(this@ClientDietProgramHistoryActivity).apply {
                                text = "Son güncelleme: $u"
                                setTextColor(ContextCompat.getColor(this@ClientDietProgramHistoryActivity, R.color.text_gray))
                                textSize = 11f
                            }
                            card.addView(tU)
                        }
                    }
                    list.addView(card) // Kartı listeye ekle
                }
    }

    private fun loadPrograms() { // API'den tüm programları yükle
        val progress = findViewById<ProgressBar>(R.id.progressClientDietHistory) // Yükleniyor
        progress.visibility = View.VISIBLE // Göster
        empty.visibility = View.GONE // Boş mesaj gizle
        list.removeAllViews() // Listeyi temizle
        filterRow.visibility = View.GONE // Filtre gizle
        countLabel.visibility = View.GONE // Sayı gizle
        lifecycleScope.launch { // Coroutine
            var rows: List<ClientDietProgramDayResponse> = emptyList()
            try {
                val r = RetrofitClient.instance.getMyDietPrograms() // Tüm programlar API
                rows = if (r.isSuccessful) r.body().orEmpty() else emptyList() // Yanıt listesi
            } catch (_: Exception) {
                rows = emptyList() // Hata: boş liste
            } finally {
                progress.visibility = View.GONE // Yükleniyor gizle
            }
            allPast = rows // Geçmiş kayıtları filtrele
                .filter { (it.programDate ?: "").isNotBlank() && isYmdBeforeToday(it.programDate!!) } // Bugünden önce
                .sortedByDescending { it.programDate } // Yeni → eski sırala
            monthFilter = "all" // Filtreyi sıfırla
            rebuildMonthChips() // Ay chip'lerini kur
            renderList() // Listeyi çiz
        }
    }
}
