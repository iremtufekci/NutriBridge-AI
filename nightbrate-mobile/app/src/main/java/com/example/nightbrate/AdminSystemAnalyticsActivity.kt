package com.example.nightbrate

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Locale

class AdminSystemAnalyticsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_system_analytics)
        AdminBottomBarHelper.bind(this, 3)
        load()
    }

    private fun load() {
        val progress = findViewById<ProgressBar>(R.id.sysAnProgress)
        val err = findViewById<TextView>(R.id.tvSysAnError)
        val content = findViewById<TextView>(R.id.tvSysAnContent)
        progress.visibility = View.VISIBLE
        err.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.getSystemAnalytics()
                progress.visibility = View.GONE
                if (!r.isSuccessful) {
                    err.text = "Veri alınamadı (${r.code()})"
                    err.visibility = View.VISIBLE
                    return@launch
                }
                val d = r.body() ?: return@launch
                content.text = formatAnalytics(d)
            } catch (e: Exception) {
                progress.visibility = View.GONE
                err.text = e.message ?: "Hata"
                err.visibility = View.VISIBLE
                Toast.makeText(this@AdminSystemAnalyticsActivity, err.text, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun formatAnalytics(d: SystemAnalyticsResponse): String = buildString {
        val k = d.kpis
        appendLine("— KPI (son ~1 saat) —")
        appendLine("İstek/saat: ${k.apiRequestsPerHour}  (Δ% ${"%.1f".format(k.apiRequestsPerHourDeltaPercent)})")
        appendLine("Ort. ms: ${k.avgQueryTimeMs}  (Δ% ${"%.0f".format(k.avgQueryTimeDeltaPercent)})")
        appendLine("Güvenlik skoru: ${"%.2f".format(k.securityScore)}%  açık: ${k.securityOpenIssues}")
        appendLine("2xx oranı: ${"%.1f".format(k.cacheHitRatioPercent)}%  ${k.cacheStatusLabel ?: ""}")
        appendLine()
        appendLine("— Uç noktalar (özet) —")
        d.endpointPerformance.take(12).forEach { row ->
            appendLine(
                "${row.endpoint}  çağrı=${row.calls}  ms=${row.avgTimeMs}  hata=${row.errors}  2xx%=${
                    String.format(
                        Locale.US,
                        "%.1f",
                        row.successRatePercent
                    )
                }"
            )
        }
        appendLine()
        d.systemResources?.let { s ->
            appendLine("— Kaynak —")
            appendLine("CPU %${"%.0f".format(s.cpuPercent)}  RAM %${"%.0f".format(s.memoryPercent)}  ${s.memoryRefLabel ?: ""}")
            appendLine("Disk %${"%.0f".format(s.diskIoPercent)}  ${s.diskRefLabel ?: ""}")
            appendLine("Ağ(tahm): ${"%.2f".format(s.networkMbps)} MB/s  ↑${"%.1f".format(s.networkUp)} ↓${"%.1f".format(s.networkDown)}")
        }
        d.dataNote?.let { appendLine(); appendLine(it) }
    }
}
