package com.example.nightbrate

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ClientDietProgramActivity : AppCompatActivity() {

    private val shortDays = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
    private var programs: List<ClientDietProgramDayResponse> = emptyList()
    private var weekDays: List<Calendar> = emptyList()
    private var selected: Calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private var isPostingMeal = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_diet_program)
        ClientBottomBarHelper.bind(this, 1)
        loadPrograms()
    }

    private fun calendarToYmd(cal: Calendar): String {
        val y = cal.get(Calendar.YEAR)
        val m = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val d = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        return "$y-$m-$d"
    }

    private fun startOfCalDay(c: Calendar): Calendar {
        return (c.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun isCalDayBefore(a: Calendar, b: Calendar): Boolean {
        val ay = a.get(Calendar.YEAR) * 400 + a.get(Calendar.DAY_OF_YEAR)
        val by = b.get(Calendar.YEAR) * 400 + b.get(Calendar.DAY_OF_YEAR)
        return ay < by
    }

    /** Bugünden (dahil) ileri N gün; geçmiş günler yok. */
    private fun buildUpcomingFromToday(count: Int) {
        val t = startOfCalDay(Calendar.getInstance())
        weekDays = (0 until count).map { o ->
            (t.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, o) }
        }
    }

    private fun clampSelectedToNotBeforeToday() {
        val t0 = startOfCalDay(Calendar.getInstance())
        if (isCalDayBefore(selected, t0)) {
            selected = t0
        }
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    private fun loadPrograms() {
        val progress = findViewById<ProgressBar>(R.id.progressClientDiet)
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.getMyDietPrograms()
                if (!r.isSuccessful) {
                    Toast.makeText(this@ClientDietProgramActivity, "Programlar yüklenemedi", Toast.LENGTH_LONG).show()
                    programs = emptyList()
                } else {
                    programs = r.body().orEmpty()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ClientDietProgramActivity, e.message ?: "Hata", Toast.LENGTH_LONG).show()
                programs = emptyList()
            } finally {
                progress.visibility = View.GONE
                clampSelectedToNotBeforeToday()
                buildUpcomingFromToday(14)
                buildDayStrip()
                renderForSelected()
            }
        }
    }

    private fun buildDayStrip() {
        val row = findViewById<LinearLayout>(R.id.clientDietDayStrip)
        row.removeAllViews()
        val pad = (8 * resources.displayMetrics.density).toInt()
        val todayRef = startOfCalDay(Calendar.getInstance())
        val tomorrowRef = (todayRef.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
        weekDays.forEach { d ->
            val isSel = isSameDay(d, selected)
            val isToday = isSameDay(d, todayRef)
            val short = shortDays[when (d.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                else -> 6
            }]
            val dayLabel = when {
                isToday -> "Bugun"
                isSameDay(d, tomorrowRef) -> "Yarin"
                else -> short
            }
            val dayNum = d.get(Calendar.DAY_OF_MONTH).toString()
            val b = TextView(this).apply {
                text = "$dayLabel\n$dayNum"
                gravity = Gravity.CENTER
                setPadding(pad, pad, pad, pad)
                textSize = 12f
                setTextColor(Color.WHITE)
                if (isSel) {
                    setBackgroundColor(Color.parseColor("#22C55E"))
                } else if (isToday) {
                    setBackgroundColor(Color.parseColor("#1E3A2F"))
                } else {
                    setBackgroundColor(Color.parseColor("#1E293B"))
                }
            }
            b.setOnClickListener {
                selected = d.clone() as Calendar
                buildDayStrip()
                renderForSelected()
            }
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.marginEnd = pad / 2
            row.addView(b, lp)
        }
    }

    private fun renderForSelected() {
        val sub = findViewById<TextView>(R.id.tvClientDietSubtitle)
        val empty = findViewById<TextView>(R.id.tvClientDietEmpty)
        val container = findViewById<LinearLayout>(R.id.clientDietMealsContainer)
        val total = findViewById<TextView>(R.id.tvClientDietTotal)
        val ymd = calendarToYmd(selected)
        val df = SimpleDateFormat("d MMM yyyy", Locale("tr", "TR"))
        sub.text = "Tarih: $ymd — ${df.format(selected.time)}"

        val p = programs.find { (it.programDate ?: "").trim() == ymd }
        if (p == null) {
            empty.visibility = View.VISIBLE
            empty.text = "$ymd tarihi icin plan yok. Diyetisyeniniz girdiğinde burada listelenecek."
            container.visibility = View.GONE
            total.visibility = View.GONE
            return
        }
        empty.visibility = View.GONE
        container.visibility = View.VISIBLE
        total.visibility = View.VISIBLE

        p.dietitianName?.let {
            if (it.isNotBlank()) sub.text = "${sub.text}\n$it"
        }

        val bc = p.breakfastCalories ?: 0
        val lc = p.lunchCalories ?: 0
        val dcP = p.dinnerCalories ?: 0
        val sc = p.snackCalories ?: 0
        val hasPerMealKcal = bc + lc + dcP + sc > 0
        val totalK = p.totalCalories ?: 0
        fun kcalAt(index: Int): Int {
            if (hasPerMealKcal) {
                return when (index) {
                    0 -> maxOf(0, bc)
                    1 -> maxOf(0, lc)
                    2 -> maxOf(0, dcP)
                    else -> maxOf(0, sc)
                }
            }
            if (totalK > 0) return totalK / 4
            return 0
        }
        val mealKeys = listOf("breakfast", "lunch", "dinner", "snack")
        val meals = listOf(
            Triple("Kahvaltı", p.breakfast.orEmpty(), "08:00"),
            Triple("Öğle", p.lunch.orEmpty(), "12:30"),
            Triple("Akşam", p.dinner.orEmpty(), "19:00"),
            Triple("Ara öğün", p.snack.orEmpty(), "16:00")
        )
        container.removeAllViews()
        val pad = (12 * resources.displayMetrics.density).toInt()
        meals.forEachIndexed { index, (title, text, time) ->
            val mk = kcalAt(index)
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.custom_input_bg)
                setPadding(pad, pad, pad, pad)
                val lp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                val m = (8 * resources.displayMetrics.density).toInt()
                lp.bottomMargin = m
                layoutParams = lp
            }
            val t1 = TextView(this).apply {
                this.text = "$title  •  $time  •  $mk kcal"
                setTextColor(Color.WHITE)
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
            }
            val t2 = TextView(this).apply {
                this.text = text.ifBlank { "—" }
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 14f
            }
            val done = when (index) {
                0 -> p.breakfastCompleted == true
                1 -> p.lunchCompleted == true
                2 -> p.dinnerCompleted == true
                else -> p.snackCompleted == true
            }
            val ev = Button(this).apply {
                this.text = "Evde yok"
                setTextColor(Color.parseColor("#B45309"))
            }
            ev.setOnClickListener {
                Toast.makeText(this@ClientDietProgramActivity, "Yakında", Toast.LENGTH_SHORT).show()
            }
            card.addView(t1)
            card.addView(t2)
            if (done) {
                val tv = TextView(this).apply {
                    this.text = "Tamamlandı"
                    setTextColor(Color.parseColor("#2ECC71"))
                    textSize = 14f
                    setTypeface(null, Typeface.BOLD)
                }
                card.addView(tv)
            } else {
                val completeBtn = Button(this).apply {
                    this.text = "Tamamla"
                    setTextColor(Color.parseColor("#E2E8F0"))
                }
                completeBtn.setOnClickListener {
                    if (isPostingMeal) return@setOnClickListener
                    AlertDialog.Builder(this@ClientDietProgramActivity)
                        .setMessage("Bu öğünü tamamlandı olarak kaydetmek istediğinize emin misiniz?")
                        .setNegativeButton("Vazgeç", null)
                        .setPositiveButton("Evet") { _, _ ->
                            lifecycleScope.launch {
                                isPostingMeal = true
                                try {
                                    val r = RetrofitClient.instance.markMealCompleted(
                                        SetMealCompletedRequest(ymd, mealKeys[index])
                                    )
                                    if (!r.isSuccessful) {
                                        val err = r.errorBody()?.string() ?: "HTTP " + r.code()
                                        Toast.makeText(this@ClientDietProgramActivity, err, Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(
                                            this@ClientDietProgramActivity,
                                            "Kaydedildi",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        loadPrograms()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        this@ClientDietProgramActivity,
                                        e.message ?: "Hata",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } finally {
                                    isPostingMeal = false
                                }
                            }
                        }
                        .show()
                }
                card.addView(completeBtn)
            }
            card.addView(ev)
            container.addView(card)
        }

        total.text = "Günlük toplam: ${p.totalCalories ?: 0} kcal"
        p.updatedAt?.let {
            total.append("\nSon güncelleme: $it")
        }
    }
}
