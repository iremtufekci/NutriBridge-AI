package com.example.nightbrate

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.Calendar
import kotlin.math.abs

class AdminDashboardActivity : AppCompatActivity() {

    private fun parseActivityCreatedAtToMillis(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        val tries = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" to TimeZone.getTimeZone("UTC"),
            "yyyy-MM-dd'T'HH:mm:ss'Z'" to TimeZone.getTimeZone("UTC"),
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX" to null,
            "yyyy-MM-dd'T'HH:mm:ssXXX" to null,
            "yyyy-MM-dd'T'HH:mm:ss.SSS" to null,
            "yyyy-MM-dd'T'HH:mm:ss" to null
        )
        for ((pat, zone) in tries) {
            try {
                val sdf = SimpleDateFormat(pat, Locale.US)
                if (zone != null) sdf.timeZone = zone
                val d = sdf.parse(value) ?: continue
                return d.time
            } catch (_: ParseException) {
                continue
            }
        }
        return null
    }

    private fun formatTimeAgoTr(createdAt: String?): String {
        val then = parseActivityCreatedAtToMillis(createdAt) ?: return "—"
        val diff = System.currentTimeMillis() - then
        if (diff < 0) return "Az önce"
        val s = diff / 1000
        if (s < 60) return "Az önce"
        val m = s / 60
        if (m < 60) return "$m dk önce"
        val h = m / 60
        if (h < 24) return "$h saat önce"
        val days = h / 24
        if (days < 7) return "$days gün önce"
        return SimpleDateFormat("d MMM yyyy", Locale("tr", "TR")).format(Date(then))
    }

    private fun registrationMomPercent(monthly: List<MonthlyRegistrationItem>): String? {
        if (monthly.size < 2) return null
        val last = monthly[monthly.size - 1].count
        val prev = monthly[monthly.size - 2].count
        if (prev == 0L) return if (last > 0) "+100%" else null
        val p = (last - prev) * 100.0 / prev
        return "${if (p >= 0) "+" else "-"}${String.format(Locale.US, "%.1f", abs(p))}%"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)
        AdminBottomBarHelper.bind(this, 0)
        loadDashboard()
    }

    private fun loadDashboard() {
        val progress = findViewById<ProgressBar>(R.id.adminDashProgress)
        val err = findViewById<TextView>(R.id.tvAdminDashError)
        progress.visibility = View.VISIBLE
        err.visibility = View.GONE
        lifecycleScope.launch {
            try {
                coroutineScope {
                    val statsD = async { RetrofitClient.instance.getAdminDashboardStats() }
                    val actD = async { RetrofitClient.instance.getRecentActivities(15) }
                    val r = statsD.await()
                    progress.visibility = View.GONE
                    if (!r.isSuccessful) {
                        err.text = "Veri alınamadı (${r.code()})"
                        err.visibility = View.VISIBLE
                        return@coroutineScope
                    }
                    val s = r.body() ?: return@coroutineScope
                    findViewById<TextView>(R.id.tvStatActiveUsers).text = s.activeUsers.toString()
                    findViewById<TextView>(R.id.tvStatActiveDietitians).text = s.activeDietitians.toString()
                    findViewById<TextView>(R.id.tvStatPending).text = s.pendingDietitians.toString()
                    findViewById<TextView>(R.id.tvStatTotalUsers).text = s.totalUsers.toString()
                    val mom = registrationMomPercent(s.monthlyRegistrations)
                    val trendTv = findViewById<TextView>(R.id.tvCard4Trend)
                    if (mom != null) {
                        trendTv.text = "↑ $mom · son aya göre yeni kayıt"
                    } else {
                        trendTv.text = ""
                    }
                    setupMonthlyChart(s.monthlyRegistrations)
                    setupPieChart(s.roleDistribution)
                    val ar = actD.await()
                    val list = if (ar.isSuccessful) ar.body().orEmpty() else emptyList()
                    bindRecentActivities(list)
                }
            } catch (e: Exception) {
                progress.visibility = View.GONE
                err.text = e.message ?: "Bağlantı hatası"
                err.visibility = View.VISIBLE
                Toast.makeText(this@AdminDashboardActivity, err.text, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun bindRecentActivities(activities: List<ActivityItemDto>) {
        val container = findViewById<LinearLayout>(R.id.llRecentActivities)
        val empty = findViewById<TextView>(R.id.tvRecentActivitiesEmpty)
        container.removeAllViews()
        if (activities.isEmpty()) {
            empty.visibility = View.VISIBLE
            return
        }
        empty.visibility = View.GONE
        val inflater = LayoutInflater.from(this)
        for (item in activities) {
            val row = inflater.inflate(R.layout.item_activity_row, container, false)
            val initial = (item.initial?.take(1) ?: "?").uppercase()
            row.findViewById<TextView>(R.id.tvActivityInitial).text = initial
            row.findViewById<TextView>(R.id.tvActivityName).text = item.actorDisplayName ?: "—"
            row.findViewById<TextView>(R.id.tvActivityDescription).text = item.description.orEmpty()
            row.findViewById<TextView>(R.id.tvActivityTimeAgo).text = formatTimeAgoTr(item.createdAt)
            container.addView(row)
        }
    }

    private fun monthLabelTr(year: Int, month: Int): String {
        val cal = Calendar.getInstance(Locale("tr", "TR"))
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return SimpleDateFormat("MMM yy", Locale("tr", "TR")).format(cal.time)
    }

    private fun roleTr(role: String?): String = when (role) {
        "Admin" -> "Yönetici"
        "Client" -> "Danışan"
        "Dietitian" -> "Diyetisyen"
        else -> role.orEmpty()
    }

    private fun setupMonthlyChart(monthly: List<MonthlyRegistrationItem>) {
        val chart = findViewById<com.github.mikephil.charting.charts.LineChart>(R.id.chartMonthly)
        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setDrawGridBackground(false)
        chart.legend.isEnabled = false
        chart.setDrawBorders(false)
        chart.axisRight.isEnabled = false
        chart.axisLeft.textColor = Color.LTGRAY
        chart.axisLeft.axisMinimum = 0f
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.textColor = Color.LTGRAY
        chart.xAxis.setDrawGridLines(true)
        chart.xAxis.gridColor = Color.parseColor("#64748B")
        chart.xAxis.gridLineWidth = 0.5f
        chart.axisLeft.setDrawGridLines(true)
        chart.axisLeft.gridColor = Color.parseColor("#94A3B8")
        chart.axisLeft.gridLineWidth = 0.5f

        if (monthly.isEmpty()) {
            chart.clear()
            chart.setNoDataText("Kayıt yok")
            chart.setNoDataTextColor(Color.GRAY)
            return
        }
        val entries = monthly.mapIndexed { i, m -> Entry(i.toFloat(), m.count.toFloat()) }
        val set = LineDataSet(entries, "Yeni kayıt")
        set.color = Color.parseColor("#22C55E")
        set.setDrawCircles(true)
        set.setDrawFilled(true)
        set.fillColor = Color.parseColor("#3322C55E")
        set.lineWidth = 2f
        set.mode = LineDataSet.Mode.CUBIC_BEZIER
        chart.data = LineData(set)
        val labels = monthly.map { monthLabelTr(it.year, it.month) }
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        chart.xAxis.granularity = 1f
        chart.xAxis.labelRotationAngle = -25f
        chart.invalidate()
    }

    private fun setupPieChart(roles: List<RoleCountItem>) {
        val chart = findViewById<com.github.mikephil.charting.charts.PieChart>(R.id.chartRoles)
        chart.description.isEnabled = false
        chart.setUsePercentValues(false)
        chart.setDrawEntryLabels(false)
        chart.legend.isEnabled = false
        chart.setEntryLabelColor(Color.WHITE)
        chart.setHoleColor(Color.parseColor("#152238"))
        chart.setTransparentCircleAlpha(0)
        chart.holeRadius = 52f
        chart.transparentCircleRadius = 56f

        val list = roles.filter { it.count > 0 }
        if (list.isEmpty()) {
            chart.setNoDataText("Rol verisi yok")
            chart.setNoDataTextColor(Color.GRAY)
            return
        }
        val colors = listOf(
            Color.parseColor("#2ECC71"),
            Color.parseColor("#1ABC9C"),
            Color.parseColor("#F39C12")
        )
        val pieEntries = list.map { PieEntry(it.count.toFloat(), roleTr(it.role)) }
        val set = PieDataSet(pieEntries, "")
        set.colors = colors.take(list.size)
        set.valueTextSize = 12f
        set.valueTextColor = Color.WHITE
        val data = PieData(set)
        data.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String = value.toInt().toString()
        })
        chart.data = data
        chart.invalidate()
    }
}
