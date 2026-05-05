package com.example.nightbrate

import android.graphics.drawable.GradientDrawable
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Response

class DietitianClientDetailActivity : AppCompatActivity() {

    private lateinit var progress: ProgressBar
    private lateinit var tvErr: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvComplianceHint: TextView
    private lateinit var containerWeekly: LinearLayout
    private lateinit var containerRecipes: LinearLayout
    private lateinit var containerPdfs: LinearLayout

    private val emerald by lazy { ContextCompat.getColor(this, R.color.nav_item_active) }
    private val muted by lazy { ContextCompat.getColor(this, R.color.admin_muted) }
    private val strong by lazy { ContextCompat.getColor(this, R.color.admin_strong) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dietitian_client_detail)

        progress = findViewById(R.id.progressDetail)
        tvErr = findViewById(R.id.tvDetailError)
        tvTitle = findViewById(R.id.tvDetailTitle)
        tvComplianceHint = findViewById(R.id.tvComplianceHint)
        containerWeekly = findViewById(R.id.containerWeeklyProgram)
        containerRecipes = findViewById(R.id.containerRecipes)
        containerPdfs = findViewById(R.id.containerPdfs)

        findViewById<View>(R.id.btnDetailBack).setOnClickListener { finish() }

        val clientId = intent.getStringExtra(EXTRA_CLIENT_ID)?.trim().orEmpty()
        if (clientId.isEmpty()) {
            tvErr.text = "Danışan seçilmedi."
            tvErr.visibility = View.VISIBLE
            return
        }
        loadOverview(clientId)
    }

    private fun loadOverview(clientId: String) {
        tvErr.visibility = View.GONE
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { RetrofitClient.instance.getClientOverview(clientId) }
            }
            progress.visibility = View.GONE
            result.onSuccess { resp ->
                if (resp.isSuccessful && resp.body() != null) {
                    bind(resp.body()!!)
                } else {
                    tvErr.text = readError(resp)
                    tvErr.visibility = View.VISIBLE
                }
            }.onFailure {
                tvErr.text = it.message ?: "Yüklenemedi."
                tvErr.visibility = View.VISIBLE
            }
        }
    }

    private fun bind(data: DietitianClientOverviewDto) {
        val c = data.client
        val title = when {
            c == null -> "Danışan"
            "${c.firstName.orEmpty()} ${c.lastName.orEmpty()}".trim().isNotBlank() ->
                "${c.firstName.orEmpty()} ${c.lastName.orEmpty()}".trim()
            else -> c.email ?: "Danışan"
        }
        tvTitle.text = title
        tvComplianceHint.text = "Hedef: ${c?.targetCalories ?: "—"} kkal"

        bindWeeklyProgram(data.weeklyProgramDays)

        containerRecipes.removeAllViews()
        val logs = data.kitchenRecipeLogs
        if (logs.isEmpty()) {
            containerRecipes.addView(textMuted("Henüz yapay zeka tarif kaydı yok."))
        } else {
            logs.forEach { log ->
                val card = MaterialCardView(this).apply {
                    radius = dp(12).toFloat()
                    cardElevation = dp(2).toFloat()
                    setPadding(dp(12), dp(12), dp(12), dp(12))
                }
                val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                col.addView(
                    TextView(this).apply {
                        text = log.createdAtUtc.take(19).replace("T", " ")
                        textSize = 11f
                        setTextColor(muted)
                    }
                )
                log.selectedRecipes.forEach { r ->
                    col.addView(
                        TextView(this).apply {
                            text = "${r.title}  (~${r.estimatedCalories} kkal)"
                            textSize = 14f
                            setTextColor(strong)
                            setPadding(0, dp(6), 0, 0)
                        }
                    )
                }
                if (log.selectedRecipes.isEmpty()) {
                    col.addView(TextView(this).apply { text = "(Tarif listesi boş)"; setTextColor(muted); textSize = 13f })
                }
                card.addView(
                    col,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                )
                val lpR = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lpR.bottomMargin = dp(10)
                containerRecipes.addView(card, lpR)
            }
        }

        containerPdfs.removeAllViews()
        val pdfs = data.pdfAnalyses
        if (pdfs.isEmpty()) {
            containerPdfs.addView(textMuted("Henüz PDF yüklemesi yok."))
        } else {
            pdfs.forEach { p ->
                val row = MaterialCardView(this).apply {
                    radius = dp(12).toFloat()
                    cardElevation = dp(2).toFloat()
                    setPadding(dp(12), dp(12), dp(12), dp(12))
                }
                val inner = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                row.addView(
                    inner,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                )
                inner.addView(
                    TextView(this).apply {
                        text = p.originalFileName ?: "PDF"
                        textSize = 14f
                        setTextColor(strong)
                    }
                )
                inner.addView(
                    TextView(this).apply {
                        val s = p.summary ?: ""
                        text = if (s.length > 120) s.take(120) + "…" else s
                        textSize = 12f
                        setTextColor(muted)
                    }
                )
                inner.addView(
                    TextView(this).apply {
                        text = "PDF’yi aç"
                        setTextColor(emerald)
                        textSize = 14f
                        setPadding(0, dp(8), 0, 0)
                        setOnClickListener {
                            val url = pdfPublicUrl(p.pdfUrl)
                            if (url.isNotBlank()) {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        }
                    }
                )
                val lpP = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lpP.bottomMargin = dp(10)
                containerPdfs.addView(row, lpP)
            }
        }
    }

    private fun bindWeeklyProgram(days: List<DietitianProgramDayOverviewDto>) {
        containerWeekly.removeAllViews()
        if (days.isEmpty()) {
            containerWeekly.addView(textMuted("Haftalık program verisi yok."))
            return
        }
        days.forEach { day ->
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                val bg = GradientDrawable()
                bg.cornerRadius = dp(12).toFloat()
                bg.setColor(ContextCompat.getColor(this@DietitianClientDetailActivity, R.color.admin_card_bg))
                bg.setStroke(dp(1), Color.parseColor("#E2E8F0"))
                background = bg
                setPadding(dp(12), dp(12), dp(12), dp(12))
            }
            val lpCol = LinearLayout.LayoutParams(dp(220), LinearLayout.LayoutParams.WRAP_CONTENT)
            lpCol.marginEnd = dp(10)

            col.addView(
                TextView(this).apply {
                    text = day.weekdayLabel ?: "—"
                    textSize = 15f
                    setTextColor(strong)
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
            )
            col.addView(
                TextView(this).apply {
                    text = day.programDate.orEmpty()
                    textSize = 11f
                    setTextColor(muted)
                    setPadding(0, 0, 0, dp(8))
                }
            )

            val meals = day.meals
            if (meals.isEmpty()) {
                col.addView(
                    TextView(this).apply {
                        text = "Bu gün için program kaydı yok."
                        textSize = 13f
                        setTextColor(muted)
                    }
                )
            } else {
                meals.forEachIndexed { idx, m ->
                    col.addView(mealCard(m))
                    if (idx < meals.lastIndex) {
                        val gap = View(this)
                        gap.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dp(8)
                        )
                        col.addView(gap)
                    }
                }
            }

            containerWeekly.addView(col, lpCol)
        }
    }

    private fun mealCard(m: DietitianProgramMealOverviewDto): LinearLayout {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
            val gd = GradientDrawable()
            gd.cornerRadius = dp(10).toFloat()
            gd.setColor(if (m.completed) Color.parseColor("#ECFDF5") else Color.parseColor("#FFFFFF"))
            gd.setStroke(
                dp(1),
                if (m.completed) Color.parseColor("#6EE7B7") else Color.parseColor("#CBD5E1")
            )
            background = gd
        }
        val rowTop = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        rowTop.addView(
            TextView(this).apply {
                text = m.label ?: ""
                textSize = 11f
                setTextColor(muted)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
        )
        rowTop.addView(
            TextView(this).apply {
                text = if (m.completed) "✓" else "✗"
                textSize = 14f
                setTextColor(
                    if (m.completed) Color.parseColor("#16A34A") else Color.parseColor("#94A3B8")
                )
            }
        )
        wrap.addView(rowTop)
        wrap.addView(
            TextView(this).apply {
                val d = m.description?.trim().orEmpty()
                text = if (d.isNotEmpty()) d else "—"
                textSize = 13f
                setTextColor(strong)
                setPadding(0, dp(6), 0, 0)
            }
        )
        wrap.addView(
            TextView(this).apply {
                val cal = m.calories
                text = if (cal > 0) "$cal kkal" else "—"
                textSize = 11f
                setTextColor(muted)
                setPadding(0, dp(4), 0, 0)
            }
        )
        return wrap
    }

    private fun textMuted(msg: String) = TextView(this).apply {
        text = msg
        setTextColor(muted)
        textSize = 13f
    }

    private fun dp(x: Int): Int = (x * resources.displayMetrics.density).toInt()

    private fun pdfPublicUrl(rel: String?): String {
        val r = rel?.trim().orEmpty()
        if (r.isEmpty()) return ""
        if (r.startsWith("http://", true) || r.startsWith("https://", true)) return r
        val base = RetrofitClient.API_BASE_URL.trimEnd('/')
        val p = if (r.startsWith("/")) r else "/$r"
        return base + p
    }

    private fun readError(resp: Response<*>): String {
        val raw = resp.errorBody()?.string().orEmpty()
        return try {
            JSONObject(raw).optString("message").ifBlank { "Hata ${resp.code()}" }
        } catch (_: Exception) {
            raw.ifBlank { "Hata ${resp.code()}" }
        }
    }

    companion object {
        const val EXTRA_CLIENT_ID = "extra_client_id"
    }
}
