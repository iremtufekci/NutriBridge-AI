package com.example.nightbrate // Paket tanımı

import android.content.Intent // Activity geçişi
import android.graphics.Color // Renk sabitleri
import android.os.Bundle // Activity durum paketi
import android.text.SpannableString // Renkli metin parçası
import android.text.style.ForegroundColorSpan // Metin rengi span'ı
import android.view.LayoutInflater // Satır şablonu şişirme
import android.view.View // Görünüm temel sınıfı
import android.widget.LinearLayout // Dikey/yatay düzen
import android.widget.TextView // Metin görünümü
import android.widget.Toast // Kısa bildirim (kullanılmıyor ama import)
import androidx.appcompat.app.AppCompatActivity // Temel Activity
import androidx.core.content.ContextCompat // Tema renkleri
import androidx.lifecycle.lifecycleScope // Yaşam döngüsü coroutine kapsamı
import com.example.nightbrate.ActivityWindowHelper.applyStandardContentWindow // Standart pencere düzeni
import com.google.android.material.progressindicator.CircularProgressIndicator // Dairesel ilerleme halkası
import kotlinx.coroutines.launch // Coroutine başlatma
import java.text.NumberFormat // Sayı biçimlendirme
import java.text.SimpleDateFormat // Tarih biçimlendirme
import java.util.Calendar // Takvim işlemleri
import java.util.Locale // Yerel ayar (tr-TR)
import kotlin.math.roundToInt // Yuvarlama

class ClientDashboardActivity : AppCompatActivity() { // Danışan ana sayfa ekranı

    private var profile: ClientProfileResponse? = null // Yüklenen profil verisi
    private var dayProgram: ClientDietProgramDayResponse? = null // Seçili günün programı
    private var programLoad = false // Program yükleniyor bayrağı

    private lateinit var selected: Calendar // Seçili takvim günü
    private val upcomingDays: MutableList<Calendar> = mutableListOf() // Önümüzdeki 14 gün
    private val dayStripButtons: MutableList<Pair<Calendar, TextView>> = mutableListOf() // Gün şeridi butonları

    private val shortDays = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz") // Kısa gün adları
    private val nfTr: NumberFormat by lazy { NumberFormat.getIntegerInstance(Locale("tr", "TR")) } // Türkçe sayı formatı

    override fun onCreate(savedInstanceState: Bundle?) { // Activity oluşturulduğunda
        super.onCreate(savedInstanceState) // Üst sınıf onCreate
        applyStandardContentWindow() // Standart içerik penceresi uygula
        setContentView(R.layout.activity_client_dashboard) // Ana sayfa layout'u
        ClientBottomBarHelper.bind(this, 0) // Alt çubuk: Ana sayfa sekmesi

        selected = startOfDay(Calendar.getInstance()) // Bugünü seçili gün yap
        buildUpcomingDays() // 14 günlük listeyi oluştur
        buildDayStrip() // Gün şeridi UI'sını kur

        findViewById<TextView>(R.id.btnMealDetail).setOnClickListener { // Öğün detay butonu
            startActivity(Intent(this, ClientDietProgramActivity::class.java)) // Diyet programına git
        }
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardDietitianHome).setOnClickListener { // Diyetisyen kartı
            startActivity(Intent(this, ClientDietProgramActivity::class.java)) // Diyet programına git
        }
        findViewById<TextView>(R.id.btnAiChefTry).setOnClickListener { // AI şef dene butonu
            ClientTabNav.go(this, 4) // PDF/AI sekmesine git
        }
        findViewById<View>(R.id.btnClientProfile).setOnClickListener { // Profil butonu
            startActivity(Intent(this, ClientProfileActivity::class.java)) // Profil ekranına git
        }
        findViewById<TextView>(R.id.btnRefreshProgram).setOnClickListener { // Program yenile butonu
            loadProgramForSelectedDate() // Seçili gün programını yeniden yükle
        }

        loadProfile() // Profil ve ardından programı yükle
    }

    private fun startOfDay(c: Calendar): Calendar { // Gün başlangıcına sıfırla (00:00)
        return (c.clone() as Calendar).apply { // Takvimi klonla
            set(Calendar.HOUR_OF_DAY, 0) // Saat sıfır
            set(Calendar.MINUTE, 0) // Dakika sıfır
            set(Calendar.SECOND, 0) // Saniye sıfır
            set(Calendar.MILLISECOND, 0) // Milisaniye sıfır
        }
    }

    private fun timeGreeting(): Pair<String, String> { // Saate göre selamlama metni ve emoji
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) // Mevcut saat
        return when (h) { // Saat aralığına göre
            in 5..11 -> "Günaydın" to "🌞" // Sabah
            in 12..17 -> "İyi günler" to "☀️" // Öğleden sonra
            in 18..21 -> "İyi akşamlar" to "🌆" // Akşam
            else -> "İyi geceler" to "🌙" // Gece
        }
    }

    private fun calendarToYmd(c: Calendar): String { // Takvimi yyyy-MM-dd string'e çevir
        val y = c.get(Calendar.YEAR) // Yıl
        val m = (c.get(Calendar.MONTH) + 1).toString().padStart(2, '0') // Ay (1-12)
        val d = c.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0') // Gün
        return "$y-$m-$d" // ISO tarih string'i
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean = // İki takvim aynı gün mü
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && // Yıl eşit
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR) // Yılın günü eşit

    private fun buildUpcomingDays() { // Bugünden itibaren 14 gün listele
        upcomingDays.clear() // Önceki listeyi temizle
        val t = startOfDay(Calendar.getInstance()) // Bugünün başlangıcı
        repeat(14) { i -> // 14 gün tekrarla
            upcomingDays.add((t.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, i) }) // i. günü ekle
        }
    }

    private fun buildDayStrip() { // Yatay gün seçim şeridini oluştur
        val row = findViewById<LinearLayout>(R.id.llDayStrip) // Şerit konteyneri
        row.removeAllViews() // Eski görünümleri kaldır
        dayStripButtons.clear() // Buton listesini temizle
        val padV = (8 * resources.displayMetrics.density).toInt() // Dikey padding (dp→px)
        val padH = (10 * resources.displayMetrics.density).toInt() // Yatay padding
        val todayRef = startOfDay(Calendar.getInstance()) // Bugün referansı
        val tomorrowRef = (todayRef.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) } // Yarın referansı

        for (d in upcomingDays) { // Her gün için buton oluştur
            val idx = d.get(Calendar.DAY_OF_WEEK).let { if (it == Calendar.SUNDAY) 6 else it - 2 } // Pazartesi=0 indeks
            val short = shortDays.getOrElse(idx) { "" } // Kısa gün adı
            val n = d.get(Calendar.DAY_OF_MONTH) // Ayın gün numarası
            val today = isSameDay(d, todayRef) // Bugün mü
            val tomorrow = isSameDay(d, tomorrowRef) // Yarın mı
            val dayLabel = when { // Görünen etiket
                today -> "Bugün" // Bugün etiketi
                tomorrow -> "Yarın" // Yarın etiketi
                else -> short // Kısa gün adı
            }
            val tv = TextView(this).apply { // Gün butonu TextView
                text = "$dayLabel\n$n" // Etiket + gün numarası
                gravity = android.view.Gravity.CENTER // Ortala
                textSize = 12f // Yazı boyutu
                setPadding(padH, padV, padH, padV) // İç boşluk
                minWidth = (48 * resources.displayMetrics.density).toInt() // Minimum genişlik
            }
            tv.setOnClickListener { // Gün tıklama
                selected = startOfDay(d) // Seçili günü güncelle
                refreshDayStripStyles() // Şerit stillerini yenile
                loadProgramForSelectedDate() // O günün programını yükle
            }
            dayStripButtons.add(d to tv) // Buton listesine ekle
            row.addView( // Şeride ekle
                tv,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = (6 * resources.displayMetrics.density).toInt() // Butonlar arası boşluk
                }
            )
        }
        refreshDayStripStyles() // İlk stil uygulaması
    }

    private fun refreshDayStripStyles() { // Gün şeridi buton renklerini güncelle
        val todayRef = startOfDay(Calendar.getInstance()) // Bugün referansı
        val cWhite = Color.WHITE // Beyaz metin rengi
        val cText = ContextCompat.getColor(this, R.color.text_primary) // Ana metin rengi
        val cMuted = ContextCompat.getColor(this, R.color.text_muted) // Soluk metin rengi
        dayStripButtons.forEach { (cal, button) -> // Her gün butonu
            val act = isSameDay(cal, selected) // Seçili gün mü
            val tDay = isSameDay(cal, todayRef) // Bugün mü
            when { // Stil durumu
                act -> { // Seçili gün
                    button.setBackgroundResource(R.drawable.client_day_active_bg) // Aktif arka plan
                    button.setTextColor(cWhite) // Beyaz metin
                }
                tDay -> { // Bugün (seçili değil)
                    button.setBackgroundResource(R.drawable.client_day_today_bg) // Bugün arka planı
                    button.setTextColor(cText) // Ana metin rengi
                }
                else -> { // Diğer günler
                    button.setBackgroundResource(R.drawable.client_day_normal_bg) // Normal arka plan
                    button.setTextColor(cMuted) // Soluk metin
                }
            }
        }
    }

    private fun loadProfile() { // API'den danışan profilini yükle
        findViewById<TextView>(R.id.tvClientProfileLoading).visibility = View.VISIBLE // Yükleniyor göster
        lifecycleScope.launch { // Arka planda coroutine
            try {
                val r = RetrofitClient.instance.getClientProfile() // Profil API çağrısı
                profile = if (r.isSuccessful) r.body() else null // Başarılıysa gövdeyi al
            } catch (_: Exception) {
                profile = null // Hata durumunda null
            } finally {
                findViewById<TextView>(R.id.tvClientProfileLoading).visibility = View.GONE // Yükleniyor gizle
                bindProfileUi() // Profil UI'sını bağla
                loadProgramForSelectedDate() // Seçili gün programını yükle
            }
        }
    }

    private fun loadProgramForSelectedDate() { // Seçili tarih için diyet programını yükle
        val ymd = calendarToYmd(selected) // yyyy-MM-dd formatı
        lifecycleScope.launch { // Coroutine ile API
            programLoad = true // Yükleniyor bayrağı
            bindProgramLoadingUi() // Yükleniyor UI'sı
            try {
                val r = RetrofitClient.instance.getMyDietProgramForDate(ymd) // Günlük program API
                dayProgram = if (r.isSuccessful) r.body() else null // Yanıtı kaydet
            } catch (_: Exception) {
                dayProgram = null // Hata: program yok
            } finally {
                programLoad = false // Yükleme bitti
                bindProgramUi() // Program UI'sını güncelle
            }
        }
    }

    private fun bindProfileUi() { // Profil bilgilerini ekrana yaz
        val p = profile // Profil kısayolu
        val fromProfile = listOf(p?.firstName, p?.lastName) // Ad soyad listesi
            .mapNotNull { it?.trim() } // Boş olmayanları al
            .filter { it.isNotEmpty() } // Boş string filtrele
            .joinToString(" ") // Birleştir
        val greetingName = fromProfile.ifBlank { // Selamlama adı
            intent.getStringExtra("USERNAME")?.trim()?.ifBlank { null } ?: "Danışan" // Intent veya varsayılan
        }
        val (greetText, greetEmoji) = timeGreeting() // Saat selamlaması
        findViewById<TextView>(R.id.tvClientGreeting).text = "$greetText, $greetingName $greetEmoji" // Selamlama metni
        findViewById<TextView>(R.id.tvClientSubtitle).visibility = View.GONE // Alt başlık gizli

        val targetKcal = p?.targetCalories ?: 0 // Hedef kalori
        findViewById<TextView>(R.id.tvChipGoal).text =
            p?.goalText?.trim()?.ifEmpty { null } ?: "Hedef yok" // Hedef chip metni
        val chipTarget = findViewById<TextView>(R.id.tvChipTarget) // Hedef kalori chip'i
        if (targetKcal > 0) { // Hedef tanımlıysa
            chipTarget.visibility = View.VISIBLE // Chip'i göster
            chipTarget.text = "$targetKcal kkal / gün" // Kalori hedefi metni
        } else {
            chipTarget.visibility = View.GONE // Hedef yoksa gizle
        }

        findViewById<TextView>(R.id.tvRingExplain).text = if (targetKcal > 0) { // Halka açıklaması
            "Halka, profil hedefinize göre seçili günün diyetisyen planı toplamının payını gösterir."
        } else {
            "Profilinizde kalori hedefi tanımlayın; karşılaştırma buna göre hesaplanır."
        }
    }

    private fun bindProgramLoadingUi() { // Program yüklenirken UI durumu
        findViewById<View>(R.id.rowDailyTotalLoading).visibility = View.VISIBLE // Yükleniyor satırı
        findViewById<TextView>(R.id.tvDailyTotalKcal).visibility = View.INVISIBLE // Toplam gizli
        findViewById<TextView>(R.id.tvRingCenterKcal).text = "…" // Halka ortası bekleme
    }

    private fun bindProgramUi() { // Program verilerini ana sayfa UI'sına bağla
        val p = profile // Profil referansı
        val targetKcal = p?.targetCalories ?: 0 // Hedef kalori
        val planTotal = computePlanTotal(dayProgram) // Plan toplam kalori
        val ratio = if (targetKcal > 0) (planTotal.toFloat() / targetKcal).coerceAtMost(1f) else 0f // Hedef oranı (max 1)
        val ring = findViewById<CircularProgressIndicator>(R.id.clientHomeRing) // İlerleme halkası
        ring.max = 10_000 // Maksimum değer (hassasiyet için)
        ring.setProgressCompat((ratio * 10_000f).roundToInt().coerceIn(0, 10_000), true) // Oranı halkaya yaz

        val isToday = isSameDay(selected, Calendar.getInstance()) // Seçili gün bugün mü
        val ringCenter = findViewById<TextView>(R.id.tvRingCenterKcal) // Halka ortası metin
        if (programLoad) { // Hâlâ yükleniyorsa
            ringCenter.text = "…" // Bekleme göstergesi
        } else {
            ringCenter.text = when { // Kalori değeri
                dayProgram != null && planTotal > 0 -> nfTr.format(planTotal) // Biçimlendirilmiş toplam
                else -> "—" // Veri yok
            }
        }
        findViewById<TextView>(R.id.tvRingSubline).text =
            "${if (isToday) "Bugün" else "Seçili gün"} / " + // Gün etiketi
                if (targetKcal > 0) "$targetKcal kkal hedef" else "hedef tanımlı değil" // Hedef alt satır

        findViewById<View>(R.id.rowDailyTotalLoading).visibility =
            if (programLoad) View.VISIBLE else View.GONE // Yükleniyor satırı görünürlüğü
        val tvDaily = findViewById<TextView>(R.id.tvDailyTotalKcal) // Günlük toplam metni
        tvDaily.visibility = if (programLoad) View.INVISIBLE else View.VISIBLE // Yüklenirken gizle
        if (!programLoad) { // Yükleme bittiyse
            tvDaily.text = if (dayProgram != null && planTotal > 0) { // Toplam kalori metni
                "${nfTr.format(planTotal)} kkal"
            } else {
                "—"
            }
        }

        val fmtLong = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("tr", "TR")) // Uzun tarih formatı
        findViewById<TextView>(R.id.tvDailyDateLong).text = fmtLong.format(selected.time) // Seçili gün tarihi

        val ymd = calendarToYmd(selected) // Seçili tarih string
        val banner = findViewById<View>(R.id.bannerNoProgram) // Program yok banner'ı
        if (!programLoad && dayProgram == null) { // Program kaydı yok
            banner.visibility = View.VISIBLE // Banner göster
            findViewById<TextView>(R.id.tvNoProgramText).text =
                "$ymd için henüz program kaydı yok. Diyetisyeniniz atadığında bu liste dolar."
        } else {
            banner.visibility = View.GONE // Banner gizle
        }

        bindMealRows() // Öğün satırlarını oluştur
        bindDietitianCard() // Diyetisyen kartını güncelle
        bindProfileUi() // Profil chip'lerini yenile
    }

    private fun computePlanTotal(day: ClientDietProgramDayResponse?): Int { // Günlük plan toplam kalori
        if (day == null) return 0 // Program yok
        val s = (day.breakfastCalories ?: 0) + (day.lunchCalories ?: 0) + // Öğün kalorileri topla
            (day.dinnerCalories ?: 0) + (day.snackCalories ?: 0)
        if (s > 0) return s // Ayrıntılı toplam varsa döndür
        return day.totalCalories ?: 0 // Yoksa genel toplam
    }

    private fun hasPerMealKcal(p: ClientDietProgramDayResponse): Boolean = // Öğün bazlı kalori var mı
        (p.breakfastCalories ?: 0) + (p.lunchCalories ?: 0) + // Öğün kalorileri topla
            (p.dinnerCalories ?: 0) + (p.snackCalories ?: 0) > 0 // Sıfırdan büyük mü

    private fun kcalForSlot(p: ClientDietProgramDayResponse, slot: Char): Int { // Öğün slotu için kalori
        val a = p.breakfastCalories ?: 0 // Kahvaltı
        val b = p.lunchCalories ?: 0 // Öğle
        val c = p.dinnerCalories ?: 0 // Akşam
        val d = p.snackCalories ?: 0 // Ara öğün
        if (a + b + c + d > 0) { // Ayrıntılı kalori varsa
            return when (slot) { // Slota göre döndür
                'b' -> maxOf(0, a) // Kahvaltı
                'l' -> maxOf(0, b) // Öğle
                'd' -> maxOf(0, c) // Akşam
                else -> maxOf(0, d) // Ara öğün
            }
        }
        val tot = p.totalCalories ?: 0 // Genel toplam
        if (tot > 0) return (tot / 4.0).roundToInt() // Eski kayıt: toplam/4
        return 0 // Kalori yok
    }

    private fun bindMealRows() { // Ana sayfa öğün satırlarını dinamik oluştur
        val container = findViewById<LinearLayout>(R.id.llClientMealRows) // Öğün listesi konteyneri
        container.removeAllViews() // Eski satırları temizle
        val dp = dayProgram // Gün programı
        val inflater = LayoutInflater.from(this) // Layout şişirici
        val cEmerald = Color.parseColor("#16A34A") // Yeşil durum rengi
        val cAmber = Color.parseColor("#D97706") // Turuncu bekleme rengi
        val cSlate = ContextCompat.getColor(this, R.color.text_muted) // Soluk metin
        val cBorderEmerald = Color.parseColor("#22C55E") // Yeşil şerit
        val cBorderAmber = Color.parseColor("#FCD34D") // Sarı şerit
        val cBorderMuted = Color.parseColor("#CBD5E1") // Gri şerit

        data class Row( // Öğün satırı veri modeli
            val key: Char, // Slot anahtarı (b/l/d/s)
            val name: String, // Öğün adı
            val emoji: String, // Emoji ikon
            val text: String, // Plan metni
            val kcal: Int, // Kalori
            val status: String, // Durum etiketi
            val statusColor: Int, // Durum rengi
            val stripe: Int // Sol şerit rengi
        )

        val rows: List<Row> = if (dp == null) { // Program yoksa boş satırlar
            listOf(
                Row('b', "Kahvaltı", "🥣", "", 0, "Plan yok", cSlate, cBorderMuted),
                Row('l', "Öğle", "🍽", "", 0, "Plan yok", cSlate, cBorderMuted),
                Row('d', "Akşam", "🥗", "", 0, "Plan yok", cSlate, cBorderMuted),
                Row('s', "Ara Öğün", "🍎", "", 0, "Plan yok", cSlate, cBorderMuted)
            )
        } else { // Program varsa gerçek veriler
            val t = mapOf( // Öğün metinleri
                'b' to (dp.breakfast ?: "").trim(),
                'l' to (dp.lunch ?: "").trim(),
                'd' to (dp.dinner ?: "").trim(),
                's' to (dp.snack ?: "").trim()
            )
            fun row(key: Char, name: String, emoji: String): Row { // Tek öğün satırı oluştur
                val te = t[key].orEmpty() // Öğün metni
                val has = te.isNotEmpty() // Plan var mı
                val k = kcalForSlot(dp, key) // Slot kalorisi
                return Row(
                    key, name, emoji, te, k,
                    status = if (has) "Planda" else "Bekleniyor", // Durum metni
                    statusColor = if (has) cEmerald else cAmber, // Durum rengi
                    stripe = if (has) cBorderEmerald else cBorderAmber // Şerit rengi
                )
            }
            listOf(
                row('b', "Kahvaltı", "🥣"),
                row('l', "Öğle", "🍽"),
                row('d', "Akşam", "🥗"),
                row('s', "Ara Öğün", "🍎")
            )
        }

        val hasDetailKcal = dp != null && hasPerMealKcal(dp) // Öğün bazlı kalori mevcut mu
        for (mr in rows) { // Her öğün satırını şişir
            val v = inflater.inflate(R.layout.item_client_home_meal, container, false) // Satır layout'u
            v.findViewById<View>(R.id.mealLeftStripe).setBackgroundColor(mr.stripe) // Sol renk şeridi
            v.findViewById<TextView>(R.id.tvMealEmoji).text = mr.emoji // Emoji
            val title = v.findViewById<TextView>(R.id.tvMealTitle) // Başlık TextView
            val span = SpannableString("${mr.name}  ${mr.status}") // Renkli durum span'ı
            span.setSpan(
                ForegroundColorSpan(mr.statusColor), // Durum rengi
                mr.name.length + 2, // Span başlangıcı
                span.length, // Span sonu
                0
            )
            title.text = span // Başlığı ata
            val kcalLine = v.findViewById<TextView>(R.id.tvMealKcalLine) // Kalori satırı
            kcalLine.text = when { // Kalori açıklama metni
                mr.kcal > 0 && dp != null && hasDetailKcal -> "${mr.kcal} kkal (diyetisyen)" // Ayrıntılı
                mr.kcal > 0 -> "~${mr.kcal} kkal (toplam/4, eski kayıt)" // Tahmini
                else -> "0 kkal" // Sıfır
            }
            val tvText = v.findViewById<TextView>(R.id.tvMealText) // Öğün açıklama metni
            if (mr.text.isNotEmpty()) { // Metin varsa
                tvText.visibility = View.VISIBLE // Göster
                tvText.text = mr.text // Metni yaz
            } else {
                tvText.visibility = View.GONE // Gizle
            }
            container.addView(v) // Konteynere ekle
        }
    }

    private fun displayDietitianName(): String { // Ekranda gösterilecek diyetisyen adı
        val fromProgram = dayProgram?.dietitianName?.trim() // Programdan ad
        if (!fromProgram.isNullOrEmpty() && fromProgram != "Atanmadi" && fromProgram != "Atanmadı") return fromProgram // Geçerli ad
        val d = profile?.dietitianName?.trim() // Profilden ad
        if (!d.isNullOrEmpty() && d != "Atanmadi" && d != "Atanmadı") return d // Geçerli ad
        return "Diyetisyen atanmadı" // Varsayılan mesaj
    }

    private fun hasLiveDietitian(): Boolean = displayDietitianName() != "Diyetisyen atanmadı" // Atanmış diyetisyen var mı

    private fun initialsFromDietitianName(raw: String): String { // Diyetisyen adından baş harfler
        if (raw.isBlank() || raw.contains("atanmad", ignoreCase = true)) return "?" // Atanmamış
        var s = raw.replace(Regex("^Dr\\.\\s*", RegexOption.IGNORE_CASE), "").trim() // Dr. önekini kaldır
        if (s.isEmpty()) return "?" // Boş ad
        val parts = s.split(Regex("\\s+")).filter { it.isNotEmpty() } // Kelime parçaları
        if (parts.size >= 2) { // Ad soyad varsa
            return (parts[0].first().uppercaseChar().toString() + parts.last().first().uppercaseChar().toString()) // İlk harfler
        }
        return s.take(2).uppercase(Locale("tr", "TR")) // Tek kelime: ilk 2 harf
    }

    private fun bindDietitianCard() { // Diyetisyen kartı UI'sını güncelle
        val name = displayDietitianName() // Gösterilecek ad
        findViewById<TextView>(R.id.tvDietitianInitials).text = initialsFromDietitianName(name) // Avatar baş harfleri
        findViewById<TextView>(R.id.tvDietitianNameHome).text = name // Diyetisyen adı
        findViewById<TextView>(R.id.tvDietitianHintHome).text = if (hasLiveDietitian()) { // İpucu metni
            "Öğün listenin altında gün seçimi ve günlük toplam kkal. Tam takvim: Diyet Programım."
        } else {
            "Takip kodu ile diyetisyeninize bağlanın; profil sayfasından eşleştirme yapabilirsiniz."
        }
    }
}
