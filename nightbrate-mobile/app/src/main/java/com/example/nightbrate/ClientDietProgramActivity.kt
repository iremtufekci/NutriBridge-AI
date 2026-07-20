package com.example.nightbrate // Paket tanımı

import android.graphics.Typeface // Kalın yazı tipi
import android.os.Bundle // Activity durum paketi
import android.text.SpannableString // Biçimlendirilmiş metin
import android.text.style.StyleSpan // Kalın span
import android.view.LayoutInflater // Layout şişirme
import android.view.View // Görünüm temel sınıfı
import android.view.ViewGroup // Görünüm grubu layout parametreleri
import android.widget.LinearLayout // Dikey düzen
import android.widget.ProgressBar // Yükleniyor göstergesi
import android.widget.TextView // Metin görünümü
import android.widget.Toast // Kısa bildirim
import androidx.appcompat.app.AlertDialog // Onay diyaloğu
import androidx.appcompat.app.AppCompatActivity // Temel Activity
import androidx.core.content.ContextCompat // Tema renkleri
import androidx.lifecycle.lifecycleScope // Yaşam döngüsü coroutine
import com.google.android.material.button.MaterialButton // Material buton
import com.google.android.material.card.MaterialCardView // Material kart
import kotlinx.coroutines.launch // Coroutine başlat
import org.json.JSONObject // Hata JSON ayrıştırma
import retrofit2.Response // HTTP yanıt sarmalayıcı
import java.text.NumberFormat // Sayı biçimlendirme
import java.text.SimpleDateFormat // Tarih biçimlendirme
import java.util.Calendar // Takvim
import java.util.Date // Tarih nesnesi
import java.util.Locale // Yerel ayar
import java.util.TimeZone // Saat dilimi
import kotlin.math.max // Maksimum değer
import kotlin.math.roundToInt // Yuvarlama

class ClientDietProgramActivity : AppCompatActivity() { // Danışan günlük diyet programı ekranı

    private val shortDays = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz") // Kısa gün adları
    private val mealDefs = listOf( // Öğün tanımları
        MealDef("breakfast", "Kahvaltı", "08:00", "🥚"),
        MealDef("lunch", "Öğle", "12:30", "🥬"),
        MealDef("dinner", "Akşam", "19:00", "🍽"),
        MealDef("snack", "Ara Öğün", "16:00", "🍪")
    )

    private data class MealDef(val key: String, val title: String, val time: String, val emoji: String) // Öğün meta verisi

    private var upcomingDays: List<Calendar> = emptyList() // Önümüzdeki günler listesi
    private val dayStripButtons = mutableListOf<Pair<Calendar, TextView>>() // Gün şeridi butonları
    private lateinit var selected: Calendar // Seçili gün

    private var dayProgram: ClientDietProgramDayResponse? = null // Seçili günün program verisi
    private var detailLoading = false // Detay yükleniyor bayrağı
    private var loadSeq = 0 // Eşzamanlı istek sıra numarası
    private var markingMealKey: String? = null // Tamamlanan öğün işaretleniyor

    private val nfTr: NumberFormat by lazy { NumberFormat.getIntegerInstance(Locale("tr", "TR")) } // Türkçe sayı formatı

    override fun onCreate(savedInstanceState: Bundle?) { // Activity oluşturulduğunda
        super.onCreate(savedInstanceState) // Üst sınıf onCreate
        setContentView(R.layout.activity_client_diet_program) // Diyet program layout'u
        ClientBottomBarHelper.bind(this, 1) // Alt çubuk: Günlük sekmesi

        selected = startOfCalDay(Calendar.getInstance()) // Bugünü seç
        buildUpcomingFromToday(14) // 14 gün listele
        buildDayStrip() // Gün şeridini kur
        bindIntro() // Giriş metnini biçimlendir
        findViewById<ProgressBar>(R.id.progressClientDiet).visibility = View.GONE // Eski progress gizle
        loadDay(calendarToYmd(selected)) // Seçili günü yükle
    }

    override fun onResume() { // Ekran tekrar görünür olduğunda
        super.onResume() // Üst sınıf onResume
        loadDay(calendarToYmd(selected)) // Programı yenile
    }

    private fun startOfCalDay(c: Calendar): Calendar { // Gün başlangıcına sıfırla
        return (c.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0) // Saat sıfır
            set(Calendar.MINUTE, 0) // Dakika sıfır
            set(Calendar.SECOND, 0) // Saniye sıfır
            set(Calendar.MILLISECOND, 0) // Milisaniye sıfır
        }
    }

    private fun calendarToYmd(cal: Calendar): String { // Takvimi yyyy-MM-dd'ye çevir
        val y = cal.get(Calendar.YEAR) // Yıl
        val m = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0') // Ay
        val d = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0') // Gün
        return "$y-$m-$d" // ISO tarih
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean = // Aynı gün mü
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    private fun isBeforeCalendarDay(a: Calendar, b: Calendar): Boolean { // a, b'den önceki gün mü
        val at = a.timeInMillis / 86_400_000L // a gün indeksi
        val bt = b.timeInMillis / 86_400_000L // b gün indeksi
        return at < bt // Önce mi
    }

    private fun buildUpcomingFromToday(count: Int) { // Bugünden itibaren count gün oluştur
        val t = startOfCalDay(Calendar.getInstance()) // Bugün başlangıcı
        upcomingDays = (0 until count).map { o -> // 0..count-1 günler
            (t.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, o) } // o. gün
        }
    }

    private fun clampSelectedNotBeforeToday() { // Seçili gün bugünden önce olamaz
        val t0 = startOfCalDay(Calendar.getInstance()) // Bugün
        if (isBeforeCalendarDay(selected, t0)) { // Geçmiş gün seçiliyse
            selected = t0 // Bugüne çek
        }
    }

    private fun buildDayStrip() { // Yatay gün seçim şeridi
        clampSelectedNotBeforeToday() // Geçmiş gün engelle
        val row = findViewById<LinearLayout>(R.id.clientDietDayStrip) // Şerit konteyneri
        row.removeAllViews() // Eski görünümleri temizle
        dayStripButtons.clear() // Buton listesini sıfırla
        val padV = (10 * resources.displayMetrics.density).toInt() // Dikey padding
        val padH = (12 * resources.displayMetrics.density).toInt() // Yatay padding
        val todayRef = startOfCalDay(Calendar.getInstance()) // Bugün referansı
        val tomorrowRef = (todayRef.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) } // Yarın

        for (d in upcomingDays) { // Her gün için buton
            val idx = d.get(Calendar.DAY_OF_WEEK).let { if (it == Calendar.SUNDAY) 6 else it - 2 } // Pazartesi=0
            val short = shortDays.getOrElse(idx) { "" } // Kısa gün adı
            val n = d.get(Calendar.DAY_OF_MONTH) // Gün numarası
            val today = isSameDay(d, todayRef) // Bugün mü
            val tomorrow = isSameDay(d, tomorrowRef) // Yarın mı
            val dayLabel = when { // Etiket
                today -> "Bugün"
                tomorrow -> "Yarın"
                else -> short
            }
            val tv = TextView(this).apply { // Gün butonu
                text = "$dayLabel\n$n" // Etiket + numara
                gravity = android.view.Gravity.CENTER // Ortala
                textSize = 12f // Yazı boyutu
                setPadding(padH, padV, padH, padV) // Padding
                minWidth = (52 * resources.displayMetrics.density).toInt() // Min genişlik
            }
            tv.setOnClickListener { // Gün tıklama
                selected = startOfCalDay(d) // Günü seç
                refreshDayStripStyles() // Stilleri güncelle
                loadDay(calendarToYmd(selected)) // Programı yükle
            }
            dayStripButtons.add(d to tv) // Listeye ekle
            row.addView(
                tv,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = (8 * resources.displayMetrics.density).toInt() // Aralık
                }
            )
        }
        refreshDayStripStyles() // İlk stil
    }

    private fun refreshDayStripStyles() { // Gün şeridi buton stilleri
        val todayRef = startOfCalDay(Calendar.getInstance()) // Bugün
        val cWhite = ContextCompat.getColor(this, R.color.white) // Beyaz
        val cText = ContextCompat.getColor(this, R.color.text_primary) // Ana metin
        val cMuted = ContextCompat.getColor(this, R.color.text_muted) // Soluk metin
        dayStripButtons.forEach { (cal, button) -> // Her buton
            val act = isSameDay(cal, selected) // Seçili mi
            val today = isSameDay(cal, todayRef) // Bugün mü
            when {
                act -> { // Seçili gün
                    button.setBackgroundResource(R.drawable.client_day_active_bg)
                    button.setTextColor(cWhite)
                }
                today -> { // Bugün
                    button.setBackgroundResource(R.drawable.client_day_today_bg)
                    button.setTextColor(cText)
                }
                else -> { // Diğer
                    button.setBackgroundResource(R.drawable.client_day_normal_bg)
                    button.setTextColor(cMuted)
                }
            }
        }
    }

    private fun bindIntro() { // Giriş açıklama metnini kalınlaştır
        val tv = findViewById<TextView>(R.id.tvClientDietIntro) // Intro TextView
        val base = "Tarih sekmesinde bugün ve ileri 14 gün gösteriliyor; güne tıklayın." // Temel metin
        val sp = SpannableString(base) // Span'lı metin
        val i0 = base.indexOf("bugün") // Kalın başlangıç
        if (i0 >= 0) {
            val i1 = i0 + "bugün ve ileri 14 gün".length // Kalın bitiş
            sp.setSpan(StyleSpan(Typeface.BOLD), i0, i1.coerceAtMost(sp.length), 0) // Kalın span
        }
        tv.text = sp // Metni ata
    }

    private fun loadDay(ymd: String) { // Belirli tarih için program yükle
        val seq = ++loadSeq // İstek sıra numarası artır
        detailLoading = true // Yükleniyor
        updateLoadingUi() // UI güncelle
        lifecycleScope.launch { // Coroutine
            try {
                val r = RetrofitClient.instance.getMyDietProgramForDate(ymd) // API çağrısı
                if (seq != loadSeq) return@launch // Eski istek, yoksay
                dayProgram = if (r.isSuccessful) r.body() else null // Yanıtı kaydet
            } catch (_: Exception) {
                if (seq != loadSeq) return@launch // Eski istek
                dayProgram = null // Hata
            } finally {
                if (seq == loadSeq) { // Güncel istek
                    detailLoading = false // Yükleme bitti
                    render() // Ekranı çiz
                }
            }
        }
    }

    private fun updateLoadingUi() { // Yükleniyor durumunda görünürlük
        findViewById<View>(R.id.rowClientDietLoading).visibility =
            if (detailLoading) View.VISIBLE else View.GONE // Yükleniyor satırı
        if (detailLoading) { // Yüklenirken içerik gizle
            findViewById<View>(R.id.tvClientDietEmpty).visibility = View.GONE
            findViewById<View>(R.id.clientDietMealsContainer).visibility = View.GONE
            findViewById<View>(R.id.tvClientDietTotal).visibility = View.GONE
        }
    }

    private fun render() { // Program ekranını çiz
        updateLoadingUi() // Yükleniyor UI
        bindIntro() // Intro metni

        val empty = findViewById<TextView>(R.id.tvClientDietEmpty) // Boş durum metni
        val container = findViewById<LinearLayout>(R.id.clientDietMealsContainer) // Öğün konteyneri
        val total = findViewById<TextView>(R.id.tvClientDietTotal) // Günlük toplam
        val ymd = calendarToYmd(selected) // Seçili tarih

        if (detailLoading) return // Hâlâ yükleniyorsa çık

        val p = dayProgram // Program verisi
        if (p == null) { // Program yok
            empty.visibility = View.VISIBLE // Boş mesaj göster
            empty.text =
                "$ymd tarihi için plan yok. Diyetisyeniniz bu tarihe program atadığında burada göreceksiniz."
            container.visibility = View.GONE // Öğünler gizli
            total.visibility = View.GONE // Toplam gizli
            return
        }

        empty.visibility = View.GONE // Boş mesaj gizle
        container.visibility = View.VISIBLE // Öğünler göster
        total.visibility = View.VISIBLE // Toplam göster

        container.removeAllViews() // Eski kartları temizle
        val inflater = LayoutInflater.from(this) // Layout şişirici
        for (m in mealDefs) { // Her öğün için kart
            val v = inflater.inflate(R.layout.item_client_diet_program_meal, container, false) as MaterialCardView
            bindMealCard(v, p, m) // Kartı doldur
            container.addView(v) // Konteynere ekle
        }

        val dayTotal = dayTotalKcal(p) // Günlük toplam kalori
        val num = nfTr.format(dayTotal) // Biçimlendirilmiş sayı
        val sp = SpannableString("Günlük toplam hedef: $num kkal") // Toplam metni
        val kStart = sp.indexOf(num) // Kalori sayısı konumu
        if (kStart >= 0) {
            sp.setSpan(StyleSpan(Typeface.BOLD), kStart, kStart + num.length, 0) // Kalori kalın
        }
        total.text = sp // Toplam metnini yaz
        p.updatedAt?.let { raw -> // Son güncelleme varsa
            val ms = parseIsoToMillis(raw) // ISO → milisaniye
            if (ms != null) {
                total.append( // Alt satır ekle
                    "\nSon güncelleme: ${
                        SimpleDateFormat("d.MM.yyyy HH:mm", Locale("tr", "TR")).format(Date(ms))
                    }"
                )
            }
        }
    }

    private fun dayTotalKcal(p: ClientDietProgramDayResponse): Int { // Günlük toplam kalori hesapla
        val s = (p.breakfastCalories ?: 0) + (p.lunchCalories ?: 0) +
            (p.dinnerCalories ?: 0) + (p.snackCalories ?: 0) // Öğün toplamı
        if (s > 0) return s // Ayrıntılı toplam
        return p.totalCalories ?: 0 // Genel toplam
    }

    private fun kcalForMeal(p: ClientDietProgramDayResponse, key: String): Int { // Öğün anahtarına göre kalori
        val a = p.breakfastCalories ?: 0 // Kahvaltı
        val b = p.lunchCalories ?: 0 // Öğle
        val c = p.dinnerCalories ?: 0 // Akşam
        val d = p.snackCalories ?: 0 // Ara öğün
        if (a + b + c + d > 0) { // Ayrıntılı kalori var
            return when (key) { // Anahtara göre
                "breakfast" -> max(0, a)
                "lunch" -> max(0, b)
                "dinner" -> max(0, c)
                else -> max(0, d)
            }
        }
        val tot = p.totalCalories ?: 0 // Genel toplam
        if (tot > 0) return (tot / 4.0).roundToInt() // Eski kayıt: /4
        return 0
    }

    private fun mealCompleted(p: ClientDietProgramDayResponse, key: String): Boolean = when (key) { // Öğün tamamlandı mı
        "breakfast" -> p.breakfastCompleted == true
        "lunch" -> p.lunchCompleted == true
        "dinner" -> p.dinnerCompleted == true
        else -> p.snackCompleted == true
    }

    private fun mealText(p: ClientDietProgramDayResponse, key: String): String = when (key) { // Öğün plan metni
        "breakfast" -> p.breakfast.orEmpty()
        "lunch" -> p.lunch.orEmpty()
        "dinner" -> p.dinner.orEmpty()
        else -> p.snack.orEmpty()
    }.trim()

    private fun bindMealCard(card: MaterialCardView, p: ClientDietProgramDayResponse, m: MealDef) { // Öğün kartını doldur
        val kcal = kcalForMeal(p, m.key) // Öğün kalorisi
        val text = mealText(p, m.key) // Plan metni
        val done = mealCompleted(p, m.key) // Tamamlandı mı
        val isMarking = markingMealKey == m.key // Kaydediliyor mu

        if (done) { // Tamamlanmış öğün stili
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.client_meal_done_bg))
            card.strokeWidth = (1 * resources.displayMetrics.density).toInt()
            card.strokeColor = ContextCompat.getColor(this, R.color.client_meal_done_stroke)
        } else { // Bekleyen öğün stili
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_surface))
            card.strokeWidth = (1 * resources.displayMetrics.density).toInt()
            card.strokeColor = ContextCompat.getColor(this, R.color.border_subtle)
        }

        card.findViewById<TextView>(R.id.tvMealEmoji).text = m.emoji // Emoji
        card.findViewById<TextView>(R.id.tvMealTitle).text = m.title // Başlık
        card.findViewById<TextView>(R.id.tvMealTime).text = m.time // Saat
        card.findViewById<TextView>(R.id.tvMealKcal).text = "${nfTr.format(kcal)} kkal" // Kalori
        val desc = card.findViewById<TextView>(R.id.tvMealDesc) // Açıklama
        desc.text = text.ifEmpty { "—" } // Boşsa tire

        val btnComplete = card.findViewById<MaterialButton>(R.id.btnMealComplete) // Tamamla butonu
        val tvDone = card.findViewById<TextView>(R.id.tvMealCompleted) // Tamamlandı etiketi
        val rowSaving = card.findViewById<View>(R.id.rowMealSaving) // Kaydediliyor satırı

        if (done) { // Tamamlanmış durum
            btnComplete.visibility = View.GONE // Buton gizle
            rowSaving.visibility = View.GONE // Kayıt satırı gizle
            tvDone.visibility = View.VISIBLE // Tamamlandı göster
        } else if (isMarking) { // Kaydediliyor
            btnComplete.visibility = View.GONE
            tvDone.visibility = View.GONE
            rowSaving.visibility = View.VISIBLE // Yükleniyor göster
        } else { // Tamamlanmamış
            btnComplete.visibility = View.VISIBLE // Buton göster
            tvDone.visibility = View.GONE
            rowSaving.visibility = View.GONE
            btnComplete.setOnClickListener { // Tamamla tıklama
                AlertDialog.Builder(this)
                    .setMessage("Bu öğünü tamamlandı olarak kaydetmek istediğinize emin misiniz?")
                    .setNegativeButton("Vazgeç", null)
                    .setPositiveButton("Evet") { _, _ -> markMealComplete(m.key) } // Onayla
                    .show()
            }
        }

        card.findViewById<MaterialButton>(R.id.btnMealNotHome).setOnClickListener { // Evde değilim butonu
            Toast.makeText(this, "Alternatif öneriler yakında eklenecek.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun markMealComplete(key: String) { // Öğünü tamamlandı olarak işaretle
        if (markingMealKey != null) return // Zaten işlem devam ediyor
        markingMealKey = key // İşaretlenen öğün
        render() // UI güncelle (kaydediliyor)
        val ymd = calendarToYmd(selected) // Seçili tarih
        lifecycleScope.launch { // API çağrısı
            try {
                val r = RetrofitClient.instance.markMealCompleted( // Tamamla API
                    SetMealCompletedRequest(programDate = ymd, meal = key)
                )
                if (!r.isSuccessful) { // Hata
                    Toast.makeText(this@ClientDietProgramActivity, readErrorMessage(r), Toast.LENGTH_LONG).show()
                } else { // Başarılı
                    Toast.makeText(this@ClientDietProgramActivity, "Kaydedildi", Toast.LENGTH_SHORT).show()
                    loadDay(ymd) // Programı yenile
                }
            } catch (e: Exception) {
                Toast.makeText(this@ClientDietProgramActivity, e.message ?: "Hata", Toast.LENGTH_LONG).show()
            } finally {
                markingMealKey = null // İşlem bitti
                render() // UI yenile
            }
        }
    }

    private fun readErrorMessage(response: Response<*>): String { // HTTP hata mesajını oku
        val raw = response.errorBody()?.string().orEmpty() // Ham hata gövdesi
        return try {
            JSONObject(raw).optString("message").ifBlank { "HTTP ${response.code()}" } // JSON message
        } catch (_: Exception) {
            if (raw.isNotBlank()) raw else "HTTP ${response.code()}" // Ham metin veya kod
        }
    }

    private fun parseIsoToMillis(raw: String): Long? { // ISO tarih string'ini milisaniyeye çevir
        val tries = listOf( // Denenecek formatlar
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US)
        )
        for (fmt in tries) { // Her formatı dene
            try {
                val d = fmt.parse(raw) ?: continue // Parse et
                return d.time // Milisaniye döndür
            } catch (_: Exception) { }
        }
        return null // Parse edilemedi
    }
}
