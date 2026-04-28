package com.example.nightbrate

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

class ClientDashboardActivity : AppCompatActivity() {

    private var profile: ClientProfileResponse? = null
    private var dayProgram: ClientDietProgramDayResponse? = null
    private var programLoad = false

    private lateinit var selected: Calendar
    private val upcomingDays: MutableList<Calendar> = mutableListOf()
    private val dayStripButtons: MutableList<Pair<Calendar, TextView>> = mutableListOf()

    private val shortDays = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
    private val nfTr: NumberFormat by lazy { NumberFormat.getIntegerInstance(Locale("tr", "TR")) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_dashboard)
        ClientBottomBarHelper.bind(this, 0)

        selected = startOfDay(Calendar.getInstance())
        buildUpcomingDays()
        buildDayStrip()

        findViewById<TextView>(R.id.btnMealDetail).setOnClickListener {
            startActivity(Intent(this, ClientDietProgramActivity::class.java))
        }
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardDietitianHome).setOnClickListener {
            startActivity(Intent(this, ClientDietProgramActivity::class.java))
        }
        findViewById<TextView>(R.id.btnAiChefTry).setOnClickListener {
            ClientTabNav.go(this, 4)
        }
        findViewById<ImageButton>(R.id.btnClientBell).setOnClickListener {
            Toast.makeText(this, "Bildirimler yakında.", Toast.LENGTH_SHORT).show()
        }
        findViewById<TextView>(R.id.btnRefreshProgram).setOnClickListener {
            loadProgramForSelectedDate()
        }

        loadProfile()
    }

    private fun startOfDay(c: Calendar): Calendar {
        return (c.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun calendarToYmd(c: Calendar): String {
        val y = c.get(Calendar.YEAR)
        val m = (c.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val d = c.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        return "$y-$m-$d"
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    private fun buildUpcomingDays() {
        upcomingDays.clear()
        val t = startOfDay(Calendar.getInstance())
        repeat(14) { i ->
            upcomingDays.add((t.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, i) })
        }
    }

    private fun buildDayStrip() {
        val row = findViewById<LinearLayout>(R.id.llDayStrip)
        row.removeAllViews()
        dayStripButtons.clear()
        val padV = (8 * resources.displayMetrics.density).toInt()
        val padH = (10 * resources.displayMetrics.density).toInt()
        val todayRef = startOfDay(Calendar.getInstance())
        val tomorrowRef = (todayRef.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }

        for (d in upcomingDays) {
            val idx = d.get(Calendar.DAY_OF_WEEK).let { if (it == Calendar.SUNDAY) 6 else it - 2 }
            val short = shortDays.getOrElse(idx) { "" }
            val n = d.get(Calendar.DAY_OF_MONTH)
            val today = isSameDay(d, todayRef)
            val tomorrow = isSameDay(d, tomorrowRef)
            val dayLabel = when {
                today -> "Bugün"
                tomorrow -> "Yarın"
                else -> short
            }
            val tv = TextView(this).apply {
                text = "$dayLabel\n$n"
                gravity = android.view.Gravity.CENTER
                textSize = 12f
                setPadding(padH, padV, padH, padV)
                minWidth = (48 * resources.displayMetrics.density).toInt()
            }
            tv.setOnClickListener {
                selected = startOfDay(d)
                refreshDayStripStyles()
                loadProgramForSelectedDate()
            }
            dayStripButtons.add(d to tv)
            row.addView(
                tv,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = (6 * resources.displayMetrics.density).toInt()
                }
            )
        }
        refreshDayStripStyles()
    }

    private fun refreshDayStripStyles() {
        val todayRef = startOfDay(Calendar.getInstance())
        val cWhite = Color.WHITE
        val cText = ContextCompat.getColor(this, R.color.text_primary)
        val cMuted = ContextCompat.getColor(this, R.color.text_muted)
        dayStripButtons.forEach { (cal, button) ->
            val act = isSameDay(cal, selected)
            val tDay = isSameDay(cal, todayRef)
            when {
                act -> {
                    button.setBackgroundResource(R.drawable.client_day_active_bg)
                    button.setTextColor(cWhite)
                }
                tDay -> {
                    button.setBackgroundResource(R.drawable.client_day_today_bg)
                    button.setTextColor(cText)
                }
                else -> {
                    button.setBackgroundResource(R.drawable.client_day_normal_bg)
                    button.setTextColor(cMuted)
                }
            }
        }
    }

    private fun loadProfile() {
        findViewById<TextView>(R.id.tvClientProfileLoading).visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.getClientProfile()
                profile = if (r.isSuccessful) r.body() else null
            } catch (_: Exception) {
                profile = null
            } finally {
                findViewById<TextView>(R.id.tvClientProfileLoading).visibility = View.GONE
                bindProfileUi()
                loadProgramForSelectedDate()
            }
        }
    }

    private fun loadProgramForSelectedDate() {
        val ymd = calendarToYmd(selected)
        lifecycleScope.launch {
            programLoad = true
            bindProgramLoadingUi()
            try {
                val r = RetrofitClient.instance.getMyDietProgramForDate(ymd)
                dayProgram = if (r.isSuccessful) r.body() else null
            } catch (_: Exception) {
                dayProgram = null
            } finally {
                programLoad = false
                bindProgramUi()
            }
        }
    }

    private fun bindProfileUi() {
        val p = profile
        val fromProfile = listOf(p?.firstName, p?.lastName)
            .mapNotNull { it?.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
        val greetingName = fromProfile.ifBlank {
            intent.getStringExtra("USERNAME")?.trim()?.ifBlank { null } ?: "Danışan"
        }
        findViewById<TextView>(R.id.tvClientGreeting).text = "Günaydın, $greetingName 🌞"

        val hasLive = hasLiveDietitian()
        val isToday = isSameDay(selected, Calendar.getInstance())
        val sub = if (hasLive) {
            "${displayDietitianName()} — ${if (isToday) "Bugün" else "Seçili gün"} için öğünler ve günlük plan aşağıda."
        } else {
            "Diyetisyeninizle eşleştiğinizde planınız burada görünür."
        }
        findViewById<TextView>(R.id.tvClientSubtitle).text = sub

        val targetKcal = p?.targetCalories ?: 0
        findViewById<TextView>(R.id.tvChipGoal).text =
            p?.goalText?.trim()?.ifEmpty { null } ?: "Hedef yok"
        val chipTarget = findViewById<TextView>(R.id.tvChipTarget)
        if (targetKcal > 0) {
            chipTarget.visibility = View.VISIBLE
            chipTarget.text = "$targetKcal kcal / gün"
        } else {
            chipTarget.visibility = View.GONE
        }

        findViewById<TextView>(R.id.tvRingExplain).text = if (targetKcal > 0) {
            "Halka, profil hedefinize göre seçili günün diyetisyen planı toplamının payını gösterir."
        } else {
            "Profilinizde kalori hedefi tanımlayın; karşılaştırma buna göre hesaplanır."
        }
    }

    private fun bindProgramLoadingUi() {
        findViewById<View>(R.id.rowDailyTotalLoading).visibility = View.VISIBLE
        findViewById<TextView>(R.id.tvDailyTotalKcal).visibility = View.INVISIBLE
        findViewById<TextView>(R.id.tvRingCenterKcal).text = "…"
    }

    private fun bindProgramUi() {
        val p = profile
        val targetKcal = p?.targetCalories ?: 0
        val planTotal = computePlanTotal(dayProgram)
        val ratio = if (targetKcal > 0) (planTotal.toFloat() / targetKcal).coerceAtMost(1f) else 0f
        val ring = findViewById<CircularProgressIndicator>(R.id.clientHomeRing)
        ring.max = 10_000
        ring.setProgressCompat((ratio * 10_000f).roundToInt().coerceIn(0, 10_000), true)

        val isToday = isSameDay(selected, Calendar.getInstance())
        val ringCenter = findViewById<TextView>(R.id.tvRingCenterKcal)
        if (programLoad) {
            ringCenter.text = "…"
        } else {
            ringCenter.text = when {
                dayProgram != null && planTotal > 0 -> nfTr.format(planTotal)
                else -> "—"
            }
        }
        findViewById<TextView>(R.id.tvRingSubline).text =
            "${if (isToday) "Bugün" else "Seçili gün"} / " +
                if (targetKcal > 0) "$targetKcal kcal hedef" else "hedef tanımlı değil"

        findViewById<View>(R.id.rowDailyTotalLoading).visibility =
            if (programLoad) View.VISIBLE else View.GONE
        val tvDaily = findViewById<TextView>(R.id.tvDailyTotalKcal)
        tvDaily.visibility = if (programLoad) View.INVISIBLE else View.VISIBLE
        if (!programLoad) {
            tvDaily.text = if (dayProgram != null && planTotal > 0) {
                "${nfTr.format(planTotal)} kcal"
            } else {
                "—"
            }
        }

        val fmtLong = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("tr", "TR"))
        findViewById<TextView>(R.id.tvDailyDateLong).text = fmtLong.format(selected.time)

        val ymd = calendarToYmd(selected)
        val banner = findViewById<View>(R.id.bannerNoProgram)
        if (!programLoad && dayProgram == null) {
            banner.visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvNoProgramText).text =
                "$ymd için henüz program kaydı yok. Diyetisyeniniz atadığında bu liste dolar."
        } else {
            banner.visibility = View.GONE
        }

        bindMealRows()
        bindDietitianCard()
        bindProfileUi()
    }

    private fun computePlanTotal(day: ClientDietProgramDayResponse?): Int {
        if (day == null) return 0
        val s = (day.breakfastCalories ?: 0) + (day.lunchCalories ?: 0) +
            (day.dinnerCalories ?: 0) + (day.snackCalories ?: 0)
        if (s > 0) return s
        return day.totalCalories ?: 0
    }

    private fun hasPerMealKcal(p: ClientDietProgramDayResponse): Boolean =
        (p.breakfastCalories ?: 0) + (p.lunchCalories ?: 0) +
            (p.dinnerCalories ?: 0) + (p.snackCalories ?: 0) > 0

    private fun kcalForSlot(p: ClientDietProgramDayResponse, slot: Char): Int {
        val a = p.breakfastCalories ?: 0
        val b = p.lunchCalories ?: 0
        val c = p.dinnerCalories ?: 0
        val d = p.snackCalories ?: 0
        if (a + b + c + d > 0) {
            return when (slot) {
                'b' -> maxOf(0, a)
                'l' -> maxOf(0, b)
                'd' -> maxOf(0, c)
                else -> maxOf(0, d)
            }
        }
        val tot = p.totalCalories ?: 0
        if (tot > 0) return (tot / 4.0).roundToInt()
        return 0
    }

    private fun bindMealRows() {
        val container = findViewById<LinearLayout>(R.id.llClientMealRows)
        container.removeAllViews()
        val dp = dayProgram
        val inflater = LayoutInflater.from(this)
        val cEmerald = Color.parseColor("#16A34A")
        val cAmber = Color.parseColor("#D97706")
        val cSlate = ContextCompat.getColor(this, R.color.text_muted)
        val cBorderEmerald = Color.parseColor("#22C55E")
        val cBorderAmber = Color.parseColor("#FCD34D")
        val cBorderMuted = Color.parseColor("#CBD5E1")

        data class Row(
            val key: Char,
            val name: String,
            val emoji: String,
            val text: String,
            val kcal: Int,
            val status: String,
            val statusColor: Int,
            val stripe: Int
        )

        val rows: List<Row> = if (dp == null) {
            listOf(
                Row('b', "Kahvaltı", "🥣", "", 0, "Plan yok", cSlate, cBorderMuted),
                Row('l', "Öğle", "🍽", "", 0, "Plan yok", cSlate, cBorderMuted),
                Row('d', "Akşam", "🥗", "", 0, "Plan yok", cSlate, cBorderMuted),
                Row('s', "Ara Öğün", "🍎", "", 0, "Plan yok", cSlate, cBorderMuted)
            )
        } else {
            val t = mapOf(
                'b' to (dp.breakfast ?: "").trim(),
                'l' to (dp.lunch ?: "").trim(),
                'd' to (dp.dinner ?: "").trim(),
                's' to (dp.snack ?: "").trim()
            )
            fun row(key: Char, name: String, emoji: String): Row {
                val te = t[key].orEmpty()
                val has = te.isNotEmpty()
                val k = kcalForSlot(dp, key)
                return Row(
                    key, name, emoji, te, k,
                    status = if (has) "Planda" else "Bekleniyor",
                    statusColor = if (has) cEmerald else cAmber,
                    stripe = if (has) cBorderEmerald else cBorderAmber
                )
            }
            listOf(
                row('b', "Kahvaltı", "🥣"),
                row('l', "Öğle", "🍽"),
                row('d', "Akşam", "🥗"),
                row('s', "Ara Öğün", "🍎")
            )
        }

        val hasDetailKcal = dp != null && hasPerMealKcal(dp)
        for (mr in rows) {
            val v = inflater.inflate(R.layout.item_client_home_meal, container, false)
            v.findViewById<View>(R.id.mealLeftStripe).setBackgroundColor(mr.stripe)
            v.findViewById<TextView>(R.id.tvMealEmoji).text = mr.emoji
            val title = v.findViewById<TextView>(R.id.tvMealTitle)
            val span = SpannableString("${mr.name}  ${mr.status}")
            span.setSpan(
                ForegroundColorSpan(mr.statusColor),
                mr.name.length + 2,
                span.length,
                0
            )
            title.text = span
            val kcalLine = v.findViewById<TextView>(R.id.tvMealKcalLine)
            kcalLine.text = when {
                mr.kcal > 0 && dp != null && hasDetailKcal -> "${mr.kcal} kcal (diyetisyen)"
                mr.kcal > 0 -> "~${mr.kcal} kcal (toplam/4, eski kayıt)"
                else -> "0 kcal"
            }
            val tvText = v.findViewById<TextView>(R.id.tvMealText)
            if (mr.text.isNotEmpty()) {
                tvText.visibility = View.VISIBLE
                tvText.text = mr.text
            } else {
                tvText.visibility = View.GONE
            }
            container.addView(v)
        }
    }

    private fun displayDietitianName(): String {
        val fromProgram = dayProgram?.dietitianName?.trim()
        if (!fromProgram.isNullOrEmpty()) return fromProgram
        val d = profile?.dietitianName?.trim()
        if (!d.isNullOrEmpty() && !d.equals("Atanmadi", ignoreCase = true)) return d
        return "Diyetisyen atanmadı"
    }

    private fun hasLiveDietitian(): Boolean = displayDietitianName() != "Diyetisyen atanmadı"

    private fun initialsFromDietitianName(raw: String): String {
        if (raw.isBlank() || raw.contains("atanmad", ignoreCase = true)) return "?"
        var s = raw.replace(Regex("^Dr\\.\\s*", RegexOption.IGNORE_CASE), "").trim()
        if (s.isEmpty()) return "?"
        val parts = s.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (parts.size >= 2) {
            return (parts[0].first().uppercaseChar().toString() + parts.last().first().uppercaseChar().toString())
        }
        return s.take(2).uppercase(Locale("tr", "TR"))
    }

    private fun bindDietitianCard() {
        val name = displayDietitianName()
        findViewById<TextView>(R.id.tvDietitianInitials).text = initialsFromDietitianName(name)
        findViewById<TextView>(R.id.tvDietitianNameHome).text = name
        findViewById<TextView>(R.id.tvDietitianHintHome).text = if (hasLiveDietitian()) {
            "Öğün listenin altında gün seçimi ve günlük toplam kcal. Tam takvim: Diyet Programım."
        } else {
            "Takip kodu ile diyetisyeninize bağlanın; profil sayfasından eşleştirme yapabilirsiniz."
        }
    }
}
