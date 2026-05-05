package com.example.nightbrate

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ClientDietProgramHistoryActivity : AppCompatActivity() {

    private fun startOfCalDay(c: Calendar): Calendar {
        return (c.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun isYmdBeforeToday(ymd: String): Boolean {
        val parts = ymd.trim().split("-")
        if (parts.size != 3) return false
        return try {
            val y = parts[0].toInt()
            val m = parts[1].toInt() - 1
            val d = parts[2].toInt()
            val dayCal = Calendar.getInstance()
            dayCal.set(y, m, d, 12, 0, 0)
            dayCal.set(Calendar.MILLISECOND, 0)
            val t0 = startOfCalDay(Calendar.getInstance())
            startOfCalDay(dayCal).before(t0)
        } catch (_: Exception) {
            false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_diet_history)
        ClientBottomBarHelper.bind(this, 2)
        loadPrograms()
    }

    private fun loadPrograms() {
        val progress = findViewById<ProgressBar>(R.id.progressClientDietHistory)
        val empty = findViewById<TextView>(R.id.tvClientDietHistoryEmpty)
        val list = findViewById<LinearLayout>(R.id.clientDietHistoryList)
        progress.visibility = View.VISIBLE
        empty.visibility = View.GONE
        list.removeAllViews()
        lifecycleScope.launch {
            var rows: List<ClientDietProgramDayResponse> = emptyList()
            try {
                val r = RetrofitClient.instance.getMyDietPrograms()
                rows = if (r.isSuccessful) r.body().orEmpty() else emptyList()
            } catch (_: Exception) {
                rows = emptyList()
            } finally {
                progress.visibility = View.GONE
            }
            val past = rows
                .filter { (it.programDate ?: "").isNotBlank() && isYmdBeforeToday(it.programDate!!) }
                .sortedByDescending { it.programDate }
            if (past.isEmpty()) {
                empty.visibility = View.VISIBLE
            } else {
                empty.visibility = View.GONE
                val fmtTitle = SimpleDateFormat("d MMMM yyyy, EEEE", Locale("tr", "TR"))
                val pad = (12 * resources.displayMetrics.density).toInt()
                for (p in past) {
                    val ymd = p.programDate ?: continue
                    val cal = try {
                        val parts = ymd.split("-")
                        val c = Calendar.getInstance()
                        c.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 12, 0, 0)
                        c
                    } catch (_: Exception) {
                        null
                    }
                    val head = if (cal != null) {
                        val cap = fmtTitle.format(cal.time)
                        cap.replaceFirstChar { it.uppercase() }
                    } else ymd
                    val card = LinearLayout(this@ClientDietProgramHistoryActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setBackgroundResource(R.drawable.custom_input_bg)
                        setPadding(pad, pad, pad, pad)
                        val m = (8 * resources.displayMetrics.density).toInt()
                        val lp = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        lp.bottomMargin = m
                        layoutParams = lp
                    }
                    val t1 = TextView(this@ClientDietProgramHistoryActivity).apply {
                        text = head
                        setTextColor(ContextCompat.getColor(this@ClientDietProgramHistoryActivity, R.color.white))
                        textSize = 16f
                        setTypeface(null, Typeface.BOLD)
                    }
                    val t2 = TextView(this@ClientDietProgramHistoryActivity).apply {
                        text = ymd
                        setTextColor(ContextCompat.getColor(this@ClientDietProgramHistoryActivity, R.color.text_gray))
                        textSize = 12f
                    }
                    card.addView(t1)
                    card.addView(t2)
                    val name = p.dietitianName?.trim().orEmpty()
                    if (name.isNotEmpty()) {
                        val t3 = TextView(this@ClientDietProgramHistoryActivity).apply {
                            text = name
                            setTextColor(ContextCompat.getColor(this@ClientDietProgramHistoryActivity, R.color.text_gray))
                            textSize = 13f
                        }
                        card.addView(t3)
                    }
                    val totalK = p.totalCalories ?: 0
                    val tTot = TextView(this@ClientDietProgramHistoryActivity).apply {
                        text = "Günlük toplam: $totalK kkal"
                        setTextColor(ContextCompat.getColor(this@ClientDietProgramHistoryActivity, R.color.primary_green))
                        textSize = 14f
                        setTypeface(null, Typeface.BOLD)
                    }
                    card.addView(tTot)
                    val bck = p.breakfastCalories ?: 0
                    val lck = p.lunchCalories ?: 0
                    val dck = p.dinnerCalories ?: 0
                    val sck = p.snackCalories ?: 0
                    val perSum = bck + lck + dck + sck
                    fun kFor(i: Int) = if (perSum > 0) {
                        when (i) {
                            0 -> maxOf(0, bck)
                            1 -> maxOf(0, lck)
                            2 -> maxOf(0, dck)
                            else -> maxOf(0, sck)
                        }
                    } else {
                        if (totalK > 0) totalK / 4 else 0
                    }
                    val mealLines = listOf(
                        Triple("Kahvaltı", p.breakfast, 0),
                        Triple("Öğle", p.lunch, 1),
                        Triple("Akşam", p.dinner, 2),
                        Triple("Ara öğün", p.snack, 3)
                    )
                    for ((label, value, idx) in mealLines) {
                        val mk = kFor(idx)
                        if (value.isNullOrBlank() && mk == 0) continue
                        val line = if (value.isNullOrBlank()) "Sadece $mk kkal" else "$label ($mk kkal): $value"
                        val row = TextView(this@ClientDietProgramHistoryActivity).apply {
                            text = line
                            setTextColor(ContextCompat.getColor(this@ClientDietProgramHistoryActivity, R.color.text_gray))
                            textSize = 13f
                        }
                        card.addView(row)
                    }
                    p.updatedAt?.let { u ->
                        if (u.isNotBlank()) {
                            val tU = TextView(this@ClientDietProgramHistoryActivity).apply {
                                text = "Son güncelleme: $u"
                                setTextColor(ContextCompat.getColor(this@ClientDietProgramHistoryActivity, R.color.text_gray))
                                textSize = 11f
                            }
                            card.addView(tU)
                        }
                    }
                    list.addView(card)
                }
            }
        }
    }
}
