package com.example.nightbrate

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Response
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.roundToInt

class ClientDietProgramActivity : AppCompatActivity() {

    private val shortDays = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
    private val mealDefs = listOf(
        MealDef("breakfast", "Kahvaltı", "08:00", "🥚"),
        MealDef("lunch", "Öğle", "12:30", "🥬"),
        MealDef("dinner", "Akşam", "19:00", "🍽"),
        MealDef("snack", "Ara Öğün", "16:00", "🍪")
    )

    private data class MealDef(val key: String, val title: String, val time: String, val emoji: String)

    private var upcomingDays: List<Calendar> = emptyList()
    private val dayStripButtons = mutableListOf<Pair<Calendar, TextView>>()
    private lateinit var selected: Calendar

    private var dayProgram: ClientDietProgramDayResponse? = null
    private var detailLoading = false
    private var loadSeq = 0
    private var markingMealKey: String? = null

    private val nfTr: NumberFormat by lazy { NumberFormat.getIntegerInstance(Locale("tr", "TR")) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_diet_program)
        ClientBottomBarHelper.bind(this, 1)

        selected = startOfCalDay(Calendar.getInstance())
        buildUpcomingFromToday(14)
        buildDayStrip()
        bindIntro(null)
        findViewById<ProgressBar>(R.id.progressClientDiet).visibility = View.GONE
        loadDay(calendarToYmd(selected))
    }

    override fun onResume() {
        super.onResume()
        loadDay(calendarToYmd(selected))
    }

    private fun startOfCalDay(c: Calendar): Calendar {
        return (c.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun calendarToYmd(cal: Calendar): String {
        val y = cal.get(Calendar.YEAR)
        val m = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val d = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        return "$y-$m-$d"
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    private fun isBeforeCalendarDay(a: Calendar, b: Calendar): Boolean {
        val at = a.timeInMillis / 86_400_000L
        val bt = b.timeInMillis / 86_400_000L
        return at < bt
    }

    private fun buildUpcomingFromToday(count: Int) {
        val t = startOfCalDay(Calendar.getInstance())
        upcomingDays = (0 until count).map { o ->
            (t.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, o) }
        }
    }

    private fun clampSelectedNotBeforeToday() {
        val t0 = startOfCalDay(Calendar.getInstance())
        if (isBeforeCalendarDay(selected, t0)) {
            selected = t0
        }
    }

    private fun buildDayStrip() {
        clampSelectedNotBeforeToday()
        val row = findViewById<LinearLayout>(R.id.clientDietDayStrip)
        row.removeAllViews()
        dayStripButtons.clear()
        val padV = (10 * resources.displayMetrics.density).toInt()
        val padH = (12 * resources.displayMetrics.density).toInt()
        val todayRef = startOfCalDay(Calendar.getInstance())
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
                minWidth = (52 * resources.displayMetrics.density).toInt()
            }
            tv.setOnClickListener {
                selected = startOfCalDay(d)
                refreshDayStripStyles()
                loadDay(calendarToYmd(selected))
            }
            dayStripButtons.add(d to tv)
            row.addView(
                tv,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = (8 * resources.displayMetrics.density).toInt()
                }
            )
        }
        refreshDayStripStyles()
    }

    private fun refreshDayStripStyles() {
        val todayRef = startOfCalDay(Calendar.getInstance())
        val cWhite = ContextCompat.getColor(this, R.color.white)
        val cText = ContextCompat.getColor(this, R.color.text_primary)
        val cMuted = ContextCompat.getColor(this, R.color.text_muted)
        dayStripButtons.forEach { (cal, button) ->
            val act = isSameDay(cal, selected)
            val today = isSameDay(cal, todayRef)
            when {
                act -> {
                    button.setBackgroundResource(R.drawable.client_day_active_bg)
                    button.setTextColor(cWhite)
                }
                today -> {
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

    private fun bindIntro(dietitianName: String?) {
        val tv = findViewById<TextView>(R.id.tvClientDietIntro)
        val base = "Tarih sekmesinde bugün ve ileri 14 gün gösteriliyor; güne tıklayın."
        val extra = dietitianName?.trim()?.takeIf { it.isNotEmpty() }?.let { " $it" } ?: ""
        val sp = SpannableString(base + extra)
        val i0 = base.indexOf("bugün")
        if (i0 >= 0) {
            val i1 = i0 + "bugün ve ileri 14 gün".length
            sp.setSpan(StyleSpan(Typeface.BOLD), i0, i1.coerceAtMost(sp.length), 0)
        }
        tv.text = sp
    }

    private fun loadDay(ymd: String) {
        val seq = ++loadSeq
        detailLoading = true
        updateLoadingUi()
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.getMyDietProgramForDate(ymd)
                if (seq != loadSeq) return@launch
                dayProgram = if (r.isSuccessful) r.body() else null
            } catch (_: Exception) {
                if (seq != loadSeq) return@launch
                dayProgram = null
            } finally {
                if (seq == loadSeq) {
                    detailLoading = false
                    render()
                }
            }
        }
    }

    private fun updateLoadingUi() {
        findViewById<View>(R.id.rowClientDietLoading).visibility =
            if (detailLoading) View.VISIBLE else View.GONE
        if (detailLoading) {
            findViewById<View>(R.id.tvClientDietEmpty).visibility = View.GONE
            findViewById<View>(R.id.clientDietMealsContainer).visibility = View.GONE
            findViewById<View>(R.id.tvClientDietTotal).visibility = View.GONE
        }
    }

    private fun render() {
        updateLoadingUi()
        bindIntro(dayProgram?.dietitianName)

        val empty = findViewById<TextView>(R.id.tvClientDietEmpty)
        val container = findViewById<LinearLayout>(R.id.clientDietMealsContainer)
        val total = findViewById<TextView>(R.id.tvClientDietTotal)
        val ymd = calendarToYmd(selected)

        if (detailLoading) return

        val p = dayProgram
        if (p == null) {
            empty.visibility = View.VISIBLE
            empty.text =
                "$ymd tarihi için plan yok. Diyetisyeniniz bu tarihe program atadığında burada göreceksiniz."
            container.visibility = View.GONE
            total.visibility = View.GONE
            return
        }

        empty.visibility = View.GONE
        container.visibility = View.VISIBLE
        total.visibility = View.VISIBLE

        container.removeAllViews()
        val inflater = LayoutInflater.from(this)
        for (m in mealDefs) {
            val v = inflater.inflate(R.layout.item_client_diet_program_meal, container, false) as MaterialCardView
            bindMealCard(v, p, m)
            container.addView(v)
        }

        val dayTotal = dayTotalKcal(p)
        val num = nfTr.format(dayTotal)
        val sp = SpannableString("Günlük toplam hedef: $num kkal")
        val kStart = sp.indexOf(num)
        if (kStart >= 0) {
            sp.setSpan(StyleSpan(Typeface.BOLD), kStart, kStart + num.length, 0)
        }
        total.text = sp
        p.updatedAt?.let { raw ->
            val ms = parseIsoToMillis(raw)
            if (ms != null) {
                total.append(
                    "\nSon güncelleme: ${
                        SimpleDateFormat("d.MM.yyyy HH:mm", Locale("tr", "TR")).format(Date(ms))
                    }"
                )
            }
        }
    }

    private fun dayTotalKcal(p: ClientDietProgramDayResponse): Int {
        val s = (p.breakfastCalories ?: 0) + (p.lunchCalories ?: 0) +
            (p.dinnerCalories ?: 0) + (p.snackCalories ?: 0)
        if (s > 0) return s
        return p.totalCalories ?: 0
    }

    private fun kcalForMeal(p: ClientDietProgramDayResponse, key: String): Int {
        val a = p.breakfastCalories ?: 0
        val b = p.lunchCalories ?: 0
        val c = p.dinnerCalories ?: 0
        val d = p.snackCalories ?: 0
        if (a + b + c + d > 0) {
            return when (key) {
                "breakfast" -> max(0, a)
                "lunch" -> max(0, b)
                "dinner" -> max(0, c)
                else -> max(0, d)
            }
        }
        val tot = p.totalCalories ?: 0
        if (tot > 0) return (tot / 4.0).roundToInt()
        return 0
    }

    private fun mealCompleted(p: ClientDietProgramDayResponse, key: String): Boolean = when (key) {
        "breakfast" -> p.breakfastCompleted == true
        "lunch" -> p.lunchCompleted == true
        "dinner" -> p.dinnerCompleted == true
        else -> p.snackCompleted == true
    }

    private fun mealText(p: ClientDietProgramDayResponse, key: String): String = when (key) {
        "breakfast" -> p.breakfast.orEmpty()
        "lunch" -> p.lunch.orEmpty()
        "dinner" -> p.dinner.orEmpty()
        else -> p.snack.orEmpty()
    }.trim()

    private fun bindMealCard(card: MaterialCardView, p: ClientDietProgramDayResponse, m: MealDef) {
        val kcal = kcalForMeal(p, m.key)
        val text = mealText(p, m.key)
        val done = mealCompleted(p, m.key)
        val isMarking = markingMealKey == m.key

        if (done) {
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.client_meal_done_bg))
            card.strokeWidth = (1 * resources.displayMetrics.density).toInt()
            card.strokeColor = ContextCompat.getColor(this, R.color.client_meal_done_stroke)
        } else {
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_surface))
            card.strokeWidth = (1 * resources.displayMetrics.density).toInt()
            card.strokeColor = ContextCompat.getColor(this, R.color.border_subtle)
        }

        card.findViewById<TextView>(R.id.tvMealEmoji).text = m.emoji
        card.findViewById<TextView>(R.id.tvMealTitle).text = m.title
        card.findViewById<TextView>(R.id.tvMealTime).text = m.time
        card.findViewById<TextView>(R.id.tvMealKcal).text = "${nfTr.format(kcal)} kkal"
        val desc = card.findViewById<TextView>(R.id.tvMealDesc)
        desc.text = text.ifEmpty { "—" }

        val btnComplete = card.findViewById<MaterialButton>(R.id.btnMealComplete)
        val tvDone = card.findViewById<TextView>(R.id.tvMealCompleted)
        val rowSaving = card.findViewById<View>(R.id.rowMealSaving)

        if (done) {
            btnComplete.visibility = View.GONE
            rowSaving.visibility = View.GONE
            tvDone.visibility = View.VISIBLE
        } else if (isMarking) {
            btnComplete.visibility = View.GONE
            tvDone.visibility = View.GONE
            rowSaving.visibility = View.VISIBLE
        } else {
            btnComplete.visibility = View.VISIBLE
            tvDone.visibility = View.GONE
            rowSaving.visibility = View.GONE
            btnComplete.setOnClickListener {
                AlertDialog.Builder(this)
                    .setMessage("Bu öğünü tamamlandı olarak kaydetmek istediğinize emin misiniz?")
                    .setNegativeButton("Vazgeç", null)
                    .setPositiveButton("Evet") { _, _ -> markMealComplete(m.key) }
                    .show()
            }
        }

        card.findViewById<MaterialButton>(R.id.btnMealNotHome).setOnClickListener {
            Toast.makeText(this, "Alternatif öneriler yakında eklenecek.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun markMealComplete(key: String) {
        if (markingMealKey != null) return
        markingMealKey = key
        render()
        val ymd = calendarToYmd(selected)
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.markMealCompleted(
                    SetMealCompletedRequest(programDate = ymd, meal = key)
                )
                if (!r.isSuccessful) {
                    Toast.makeText(this@ClientDietProgramActivity, readErrorMessage(r), Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@ClientDietProgramActivity, "Kaydedildi", Toast.LENGTH_SHORT).show()
                    loadDay(ymd)
                }
            } catch (e: Exception) {
                Toast.makeText(this@ClientDietProgramActivity, e.message ?: "Hata", Toast.LENGTH_LONG).show()
            } finally {
                markingMealKey = null
                render()
            }
        }
    }

    private fun readErrorMessage(response: Response<*>): String {
        val raw = response.errorBody()?.string().orEmpty()
        return try {
            JSONObject(raw).optString("message").ifBlank { "HTTP ${response.code()}" }
        } catch (_: Exception) {
            if (raw.isNotBlank()) raw else "HTTP ${response.code()}"
        }
    }

    private fun parseIsoToMillis(raw: String): Long? {
        val tries = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US)
        )
        for (fmt in tries) {
            try {
                val d = fmt.parse(raw) ?: continue
                return d.time
            } catch (_: Exception) { }
        }
        return null
    }
}
