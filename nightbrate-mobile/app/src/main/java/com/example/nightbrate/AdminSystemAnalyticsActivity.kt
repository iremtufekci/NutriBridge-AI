package com.example.nightbrate

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import org.json.JSONObject
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AdminSystemAnalyticsActivity : AppCompatActivity() {

    private lateinit var swipe: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private lateinit var progress: ProgressBar
    private lateinit var errorPanel: View
    private lateinit var content: View
    private lateinit var tvError: TextView

    private var hasLoadedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_system_analytics)
        AdminBottomBarHelper.bind(this, 3)

        swipe = findViewById(R.id.sysAnSwipe)
        progress = findViewById(R.id.sysAnProgress)
        errorPanel = findViewById(R.id.sysAnErrorPanel)
        content = findViewById(R.id.sysAnContent)
        tvError = findViewById(R.id.tvSysAnError)

        swipe.setOnRefreshListener {
            lifecycleScope.launch { loadInternal(silent = true) }
        }
        findViewById<View>(R.id.btnSysAnRetry).setOnClickListener {
            lifecycleScope.launch { loadInternal(silent = false) }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                loadInternal(silent = false)
                while (isActive) {
                    delay(20_000)
                    loadInternal(silent = true)
                }
            }
        }
    }

    private suspend fun loadInternal(silent: Boolean) {
        if (!silent && !hasLoadedOnce) {
            progress.visibility = View.VISIBLE
            errorPanel.visibility = View.GONE
        }
        if (silent) swipe.isRefreshing = true
        try {
            val r = RetrofitClient.instance.getSystemAnalytics()
            withContext(Dispatchers.Main) {
                progress.visibility = View.GONE
                swipe.isRefreshing = false
                if (!r.isSuccessful) {
                    val msg = readErrorMessage(r)
                    if (!hasLoadedOnce) {
                        tvError.text = msg
                        errorPanel.visibility = View.VISIBLE
                        content.visibility = View.GONE
                    } else {
                        Toast.makeText(this@AdminSystemAnalyticsActivity, msg, Toast.LENGTH_LONG).show()
                    }
                    return@withContext
                }
                val d = r.body()
                if (d == null) {
                    if (!hasLoadedOnce) {
                        tvError.text = "Boş yanıt"
                        errorPanel.visibility = View.VISIBLE
                        content.visibility = View.GONE
                    }
                    return@withContext
                }
                hasLoadedOnce = true
                errorPanel.visibility = View.GONE
                content.visibility = View.VISIBLE
                bindUi(d)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                progress.visibility = View.GONE
                swipe.isRefreshing = false
                val msg = e.message ?: "Hata"
                if (!hasLoadedOnce) {
                    tvError.text = msg
                    errorPanel.visibility = View.VISIBLE
                    content.visibility = View.GONE
                } else {
                    Toast.makeText(this@AdminSystemAnalyticsActivity, msg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun bindUi(d: SystemAnalyticsResponse) {
        val hours = d.dataWindowHours ?: 24
        findViewById<TextView>(R.id.tvSysAnSubtitle).text =
            "Teknik metrikler, API ve güvenlik (son $hours saat, periyodik yenileme)"

        val note = findViewById<TextView>(R.id.tvSysAnDataNote)
        if (d.dataNote.isNullOrBlank()) {
            note.visibility = View.GONE
        } else {
            note.text = d.dataNote
            note.visibility = View.VISIBLE
        }

        val gen = findViewById<TextView>(R.id.tvSysAnGenerated)
        val genMs = parseIsoToMillis(d.generatedAtUtc)
        if (genMs == null) {
            gen.visibility = View.GONE
        } else {
            gen.text = "Son üretim: ${
                SimpleDateFormat("d.MM.yyyy HH:mm", Locale("tr", "TR")).format(Date(genMs))
            }"
            gen.visibility = View.VISIBLE
        }

        val k = d.kpis
        val tr = Locale("tr", "TR")
        findViewById<TextView>(R.id.kpiApiValue).text =
            NumberFormat.getIntegerInstance(tr).format(k.apiRequestsPerHour)
        findViewById<TextView>(R.id.kpiApiSub).text = deltaLine(k.apiRequestsPerHourDeltaPercent)

        findViewById<TextView>(R.id.kpiQueryValue).text = "${k.avgQueryTimeMs} ms"
        findViewById<TextView>(R.id.kpiQuerySub).text = deltaLine(k.avgQueryTimeDeltaPercent)

        findViewById<TextView>(R.id.kpiSecValue).text =
            String.format(tr, "%.2f", k.securityScore) + " / 100"
        findViewById<TextView>(R.id.kpiSecSub).text =
            "Açık: ${k.securityOpenIssues}"

        findViewById<TextView>(R.id.kpiCacheValue).text =
            String.format(Locale.US, "%.1f", k.cacheHitRatioPercent) + "%"
        findViewById<TextView>(R.id.kpiCacheSub).text =
            k.cacheStatusLabel?.ifBlank { null } ?: "—"

        bindDbChart(findViewById(R.id.chartDbHourly), d.databaseHourly)
        bindCacheChart(findViewById(R.id.chartCacheHourly), d.cacheHourly)
        bindNetChart(findViewById(R.id.chartNetHourly), d.networkHourly)

        val boxEp = findViewById<LinearLayout>(R.id.boxEndpoints)
        boxEp.removeAllViews()
        val eps = d.endpointPerformance.take(12)
        if (eps.isEmpty()) {
            boxEp.addView(placeholderText("Uç nokta verisi yok"))
        } else {
            for (row in eps) {
                boxEp.addView(endpointRowView(row))
            }
        }

        val res = d.systemResources
        val tvCpu = findViewById<TextView>(R.id.tvResCpuRam)
        val tvDisk = findViewById<TextView>(R.id.tvResDisk)
        val tvNet = findViewById<TextView>(R.id.tvResNet)
        val tvNetNote = findViewById<TextView>(R.id.tvResNetNote)
        if (res == null) {
            tvCpu.text = "Kaynak verisi yok"
            tvDisk.text = ""
            tvNet.text = ""
            tvNetNote.visibility = View.GONE
        } else {
            tvCpu.text = "CPU %${"%.0f".format(res.cpuPercent)}  ·  RAM %${"%.0f".format(res.memoryPercent)}  ${res.memoryRefLabel ?: ""}".trim()
            tvDisk.text = "Disk I/O %${"%.0f".format(res.diskIoPercent)}  ${res.diskRefLabel ?: ""}".trim()
            tvNet.text =
                "Ağ ~${"%.2f".format(res.networkMbps)} MB/s  ↑${"%.1f".format(res.networkUp)}  ↓${"%.1f".format(res.networkDown)}"
            val nn = res.networkNote?.trim().orEmpty()
            if (nn.isEmpty()) {
                tvNetNote.visibility = View.GONE
            } else {
                tvNetNote.text = nn
                tvNetNote.visibility = View.VISIBLE
            }
        }

        val boxErr = findViewById<LinearLayout>(R.id.boxErrors)
        boxErr.removeAllViews()
        val errs = d.errorLogs.orEmpty().take(20)
        if (errs.isEmpty()) {
            boxErr.addView(placeholderText("Hata kaydı yok"))
        } else {
            for (e in errs) {
                boxErr.addView(errorRowView(e))
            }
        }

        val boxSec = findViewById<LinearLayout>(R.id.boxSecurity)
        boxSec.removeAllViews()
        val secs = d.securityEvents.orEmpty().take(20)
        if (secs.isEmpty()) {
            boxSec.addView(placeholderText("Güvenlik olayı yok"))
        } else {
            for (s in secs) {
                boxSec.addView(securityRowView(s))
            }
        }
    }

    private fun deltaLine(pct: Double): String {
        val arrow = if (pct >= 0) "↗" else "↘"
        return "$arrow ${String.format(Locale.US, "%.1f", kotlin.math.abs(pct))}% (önceki pencereye göre)"
    }

    private fun placeholderText(s: String): TextView {
        return TextView(this).apply {
            text = s
            setTextColor(ContextCompat.getColor(this@AdminSystemAnalyticsActivity, R.color.admin_muted))
            textSize = 14f
            setPadding(0, 8, 0, 8)
        }
    }

    private fun endpointRowView(row: EndpointPerformanceRow): TextView {
        val line =
            "${row.endpoint}\nçağrı=${row.calls}  ort=${row.avgTimeMs} ms  hata=${row.errors}  2xx=${
                String.format(Locale.US, "%.1f", row.successRatePercent)
            }%"
        return TextView(this).apply {
            text = line
            setTextColor(ContextCompat.getColor(this@AdminSystemAnalyticsActivity, R.color.admin_strong))
            textSize = 12f
            setPadding(0, 10, 0, 10)
        }
    }

    private fun errorRowView(e: ErrorLogEntry): TextView {
        val line = "${e.time}  HTTP ${e.statusCode}  ${e.endpoint}\n${e.message}  (×${e.count})"
        return TextView(this).apply {
            text = line
            setTextColor(ContextCompat.getColor(this@AdminSystemAnalyticsActivity, R.color.admin_strong))
            textSize = 12f
            setPadding(0, 10, 0, 10)
        }
    }

    private fun securityRowView(s: SecurityEventEntry): TextView {
        val line = "${s.time}  [${s.severity}] ${s.name}\n${s.obfuscatedSource}  ${s.countLabel}"
        return TextView(this).apply {
            text = line
            textSize = 12f
            setPadding(12, 12, 12, 12)
            setTextColor(ContextCompat.getColor(this@AdminSystemAnalyticsActivity, R.color.admin_strong))
            setBackgroundColor(toneBg(s.tone))
        }
    }

    private fun toneBg(tone: String?): Int {
        return when (tone?.lowercase(Locale.US)) {
            "high" -> Color.argb(0x44, 0xF8, 0x71, 0x71)
            "medium" -> Color.argb(0x44, 0xF5, 0x9E, 0x0B)
            else -> Color.argb(0x33, 0x2E, 0xCC, 0x71)
        }
    }

    private fun chartMutedGrid(): Pair<Int, Int> {
        val muted = ContextCompat.getColor(this, R.color.admin_muted)
        val grid = ContextCompat.getColor(this, R.color.admin_row_stroke)
        return muted to grid
    }

    private fun bindDbChart(chart: BarChart, rows: List<HourlyDbRow>?) {
        val list = rows.orEmpty()
        val (muted, grid) = chartMutedGrid()
        chart.description.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.axisRight.isEnabled = false
        chart.axisLeft.textColor = muted
        chart.axisLeft.axisMinimum = 0f
        chart.axisLeft.setDrawGridLines(true)
        chart.axisLeft.gridColor = grid
        chart.axisLeft.gridLineWidth = 0.5f
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.textColor = muted
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.granularity = 1f
        chart.legend.isEnabled = true
        chart.legend.textColor = muted
        chart.legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        chart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        chart.setExtraOffsets(0f, 2f, 0f, 8f)

        if (list.isEmpty()) {
            chart.clear()
            chart.setNoDataText("Veri yok")
            chart.setNoDataTextColor(muted)
            return
        }
        val entries = list.mapIndexed { i, r ->
            BarEntry(
                i.toFloat(),
                floatArrayOf(r.reads.toFloat(), r.writes.toFloat(), r.slowQueries.toFloat())
            )
        }
        val set = BarDataSet(entries, "")
        set.setColors(
            mutableListOf(
                Color.parseColor("#2ECC71"),
                Color.parseColor("#3498DB"),
                Color.parseColor("#E74C3C")
            )
        )
        set.stackLabels = arrayOf("Okuma", "Yazma", "Yavaş")
        val data = BarData(set)
        data.barWidth = 0.7f
        chart.data = data
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(list.map { it.label })
        chart.xAxis.labelRotationAngle = -35f
        chart.invalidate()
    }

    private fun bindCacheChart(chart: BarChart, rows: List<HourlyCacheRow>?) {
        val list = rows.orEmpty()
        val (muted, grid) = chartMutedGrid()
        chart.description.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.axisRight.isEnabled = false
        chart.axisLeft.textColor = muted
        chart.axisLeft.axisMinimum = 0f
        chart.axisLeft.setDrawGridLines(true)
        chart.axisLeft.gridColor = grid
        chart.axisLeft.gridLineWidth = 0.5f
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.textColor = muted
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.granularity = 1f
        chart.legend.isEnabled = true
        chart.legend.textColor = muted
        chart.legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        chart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        chart.setExtraOffsets(0f, 2f, 0f, 8f)

        if (list.isEmpty()) {
            chart.clear()
            chart.setNoDataText("Veri yok")
            chart.setNoDataTextColor(muted)
            return
        }
        val entries = list.mapIndexed { i, r ->
            BarEntry(i.toFloat(), floatArrayOf(r.hits.toFloat(), r.misses.toFloat()))
        }
        val set = BarDataSet(entries, "")
        set.setColors(
            mutableListOf(
                Color.parseColor("#1ABC9C"),
                Color.parseColor("#F39C12")
            )
        )
        set.stackLabels = arrayOf("Hit", "Miss")
        val data = BarData(set)
        data.barWidth = 0.7f
        chart.data = data
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(list.map { it.label })
        chart.xAxis.labelRotationAngle = -35f
        chart.invalidate()
    }

    private fun bindNetChart(chart: LineChart, rows: List<HourlyNetRow>?) {
        val list = rows.orEmpty()
        val (muted, grid) = chartMutedGrid()
        chart.description.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.axisRight.isEnabled = false
        chart.axisLeft.textColor = muted
        chart.axisLeft.setDrawGridLines(true)
        chart.axisLeft.gridColor = grid
        chart.axisLeft.gridLineWidth = 0.5f
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.textColor = muted
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.granularity = 1f
        chart.legend.isEnabled = true
        chart.legend.textColor = muted
        chart.setExtraOffsets(0f, 2f, 0f, 8f)

        if (list.isEmpty()) {
            chart.clear()
            chart.setNoDataText("Veri yok")
            chart.setNoDataTextColor(muted)
            return
        }
        val inc = list.mapIndexed { i, r -> Entry(i.toFloat(), r.incomingMbps.toFloat()) }
        val out = list.mapIndexed { i, r -> Entry(i.toFloat(), r.outgoingMbps.toFloat()) }
        val ds1 = LineDataSet(inc, "Gelen Mb/s")
        ds1.color = Color.parseColor("#8B5CF6")
        ds1.lineWidth = 2f
        ds1.setDrawCircles(true)
        ds1.mode = LineDataSet.Mode.CUBIC_BEZIER
        val ds2 = LineDataSet(out, "Giden Mb/s")
        ds2.color = Color.parseColor("#F59E0B")
        ds2.lineWidth = 2f
        ds2.setDrawCircles(true)
        ds2.mode = LineDataSet.Mode.CUBIC_BEZIER
        chart.data = LineData(ds1, ds2)
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(list.map { it.label })
        chart.xAxis.labelRotationAngle = -35f
        chart.invalidate()
    }

    private fun readErrorMessage(response: Response<*>): String {
        val raw = response.errorBody()?.string().orEmpty()
        return try {
            JSONObject(raw).optString("message").ifBlank { "HTTP ${response.code()}" }
        } catch (_: Exception) {
            if (raw.isNotBlank()) raw else "HTTP ${response.code()}"
        }
    }

    private fun parseIsoToMillis(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
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
