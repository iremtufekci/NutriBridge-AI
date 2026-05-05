package com.example.nightbrate

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class DietitianClientsActivity : AppCompatActivity() {

    private var currentTab = "all"
    private var sortNameAscending = true

    private lateinit var container: LinearLayout
    private lateinit var progress: ProgressBar
    private lateinit var tvErr: TextView
    private lateinit var etSearch: EditText
    private lateinit var btnSort: MaterialButton

    private lateinit var btnAll: MaterialButton
    private lateinit var btnActive: MaterialButton
    private lateinit var btnCritical: MaterialButton
    private lateinit var btnPassive: MaterialButton

    private var loadedClients: List<DietitianClientCardDto> = emptyList()
    private var counts: DietitianTabCountsDto = DietitianTabCountsDto()

    private val emerald by lazy { ContextCompat.getColor(this, R.color.nav_item_active) }
    private val muted by lazy { ContextCompat.getColor(this, R.color.admin_muted) }
    private val strong by lazy { ContextCompat.getColor(this, R.color.admin_strong) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dietitian_clients)
        DietitianBottomBarHelper.bind(this, 1)

        container = findViewById(R.id.containerClientCards)
        progress = findViewById(R.id.progressClients)
        tvErr = findViewById(R.id.tvClientsError)
        etSearch = findViewById(R.id.etClientSearch)
        btnSort = findViewById(R.id.btnSortName)

        btnAll = findViewById(R.id.btnTabAll)
        btnActive = findViewById(R.id.btnTabActive)
        btnCritical = findViewById(R.id.btnTabCritical)
        btnPassive = findViewById(R.id.btnTabPassive)

        btnAll.setOnClickListener { switchTab("all") }
        btnActive.setOnClickListener { switchTab("active") }
        btnCritical.setOnClickListener { switchTab("critical") }
        btnPassive.setOnClickListener { switchTab("passive") }

        btnSort.setOnClickListener {
            sortNameAscending = !sortNameAscending
            syncSortLabel()
            loadFromApi()
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                renderCards()
            }
        })

        syncSortLabel()
        styleTabs()
        loadFromApi()
    }

    private fun switchTab(tab: String) {
        if (currentTab == tab) return
        currentTab = tab
        styleTabs()
        loadFromApi()
    }

    private fun syncSortLabel() {
        btnSort.text = if (sortNameAscending) "A-Z" else "Z-A"
    }

    private fun styleTabs() {
        listOf(
            Triple(btnAll, "all", counts.all),
            Triple(btnActive, "active", counts.active),
            Triple(btnCritical, "critical", counts.critical),
            Triple(btnPassive, "passive", counts.passive)
        ).forEach { (btn, key, n) ->
            val label = when (key) {
                "all" -> "Tümü"
                "active" -> "Aktif"
                "critical" -> "Kritik"
                else -> "Pasif"
            }
            btn.text = "$label ($n)"
            val sel = currentTab == key
            btn.setBackgroundColor(if (sel) emerald else Color.TRANSPARENT)
            btn.setTextColor(if (sel) Color.WHITE else strong)
        }
    }

    private fun loadFromApi() {
        tvErr.visibility = View.GONE
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            val sort = if (sortNameAscending) "nameAsc" else "nameDesc"
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    RetrofitClient.instance.getMyClients(sort = sort, tab = currentTab)
                }
            }
            progress.visibility = View.GONE
            result.onSuccess { resp ->
                if (resp.isSuccessful && resp.body() != null) {
                    val body = resp.body()!!
                    counts = body.tabCounts
                    loadedClients = body.clients
                    styleTabs()
                    renderCards()
                } else {
                    tvErr.text = readError(resp)
                    tvErr.visibility = View.VISIBLE
                }
            }.onFailure {
                tvErr.text = it.message ?: "Liste alınamadı."
                tvErr.visibility = View.VISIBLE
            }
        }
    }

    private fun filteredList(): List<DietitianClientCardDto> {
        val q = etSearch.text?.toString()?.trim()?.lowercase().orEmpty()
        if (q.isEmpty()) return loadedClients
        return loadedClients.filter { c ->
            val name = (c.displayName ?: "${c.firstName.orEmpty()} ${c.lastName.orEmpty()}").trim().lowercase()
            name.contains(q)
        }
    }

    private fun dp(x: Int): Int = (x * resources.displayMetrics.density).toInt()

    private fun renderCards() {
        container.removeAllViews()
        val list = filteredList()
        if (list.isEmpty()) {
            val tv = TextView(this).apply {
                text = "Kayıt yok."
                setTextColor(muted)
                setPadding(dp(8), dp(24), dp(8), dp(8))
            }
            container.addView(tv)
            return
        }
        val pad = dp(12)
        for (c in list) {
            val id = c.id ?: continue
            val card = MaterialCardView(this).apply {
                radius = dp(16).toFloat()
                cardElevation = dp(2).toFloat()
                setPadding(pad, pad, pad, pad)
                val critical = c.isCritical || c.segment.equals("critical", true)
                strokeWidth = if (critical) dp(2) else 0
                strokeColor = if (critical) Color.parseColor("#FB7185") else Color.TRANSPARENT
            }
            val root = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }
            val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val initial = (c.displayName?.trim()?.take(1)?.uppercase() ?: "?")
            val avatar = TextView(this).apply {
                text = initial
                setTextColor(Color.WHITE)
                textSize = 16f
                gravity = Gravity.CENTER
                val w = dp(48)
                layoutParams = LinearLayout.LayoutParams(w, w)
                val bg = if (c.isCritical || c.segment.equals("critical", true)) Color.parseColor("#F43F5E") else Color.parseColor("#22C55E")
                setBackgroundColor(bg)
            }
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }
            val name = TextView(this).apply {
                text = c.displayName ?: "${c.firstName.orEmpty()} ${c.lastName.orEmpty()}".trim()
                setTextColor(strong)
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
            }
            val start = TextView(this).apply {
                text = "Başlangıç: ${formatShortDate(c.startedAtUtc)}"
                setTextColor(muted)
                textSize = 12f
            }
            val last = TextView(this).apply {
                text = "Son aktivite: ${relativeActivity(c.lastActivityUtc)}"
                setTextColor(muted)
                textSize = 12f
            }
            col.addView(name)
            col.addView(start)
            col.addView(last)
            head.addView(avatar)
            head.addView(col, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(12)
            })

            val rowPct = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(10), 0, 0)
            }
            val pct = (c.compliancePercent).coerceIn(0, 100)
            rowPct.addView(TextView(this).apply {
                text = "Uyum oranı"
                setTextColor(muted)
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            rowPct.addView(TextView(this).apply {
                text = "%$pct"
                setTextColor(strong)
                textSize = 12f
            })
            val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100
                progress = pct
            }
            val barLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(8))
            barLp.topMargin = dp(4)

            val btnView = MaterialButton(this).apply {
                text = "Görüntüle"
                setBackgroundColor(emerald)
                setTextColor(Color.WHITE)
                setOnClickListener {
                    startActivity(
                        Intent(this@DietitianClientsActivity, DietitianClientDetailActivity::class.java)
                            .putExtra(DietitianClientDetailActivity.EXTRA_CLIENT_ID, id)
                    )
                }
            }
            val btnLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            btnLp.topMargin = dp(12)

            root.addView(head)
            root.addView(rowPct)
            root.addView(bar, barLp)
            root.addView(btnView, btnLp)
            card.addView(root)
            val cardLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            cardLp.bottomMargin = dp(12)
            container.addView(card, cardLp)
        }
    }

    private fun formatShortDate(iso: String?): String {
        if (iso.isNullOrBlank()) return "—"
        return try {
            val fmtIn = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val fmtOut = SimpleDateFormat("d MMM yyyy", Locale("tr", "TR"))
            val cleaned = iso.replace("Z", "").take(23)
            fmtOut.format(fmtIn.parse(cleaned)!!)
        } catch (_: Exception) {
            try {
                iso.take(10)
            } catch (_: Exception) {
                "—"
            }
        }
    }

    private fun relativeActivity(iso: String?): String {
        if (iso.isNullOrBlank()) return "henüz yok"
        return try {
            val t = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.parse(iso)?.time
                ?: java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.parse(iso)?.time
                ?: return "—"
            val diff = System.currentTimeMillis() - t
            val mins = diff / 60000
            val h = diff / 3600000
            val d = diff / 86400000
            when {
                mins < 2 -> "az önce"
                mins < 60 -> "$mins dk önce"
                h < 24 -> "$h saat önce"
                else -> "$d gün önce"
            }
        } catch (_: Exception) {
            "—"
        }
    }

    private fun readError(resp: Response<*>): String {
        val raw = resp.errorBody()?.string().orEmpty()
        return try {
            JSONObject(raw).optString("message").ifBlank { "Hata ${resp.code()}" }
        } catch (_: Exception) {
            raw.ifBlank { "Hata ${resp.code()}" }
        }
    }
}
