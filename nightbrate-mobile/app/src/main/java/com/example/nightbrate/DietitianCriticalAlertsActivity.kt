package com.example.nightbrate

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
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
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class DietitianCriticalAlertsActivity : AppCompatActivity() {

    private lateinit var progress: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var cardBanner: MaterialCardView
    private lateinit var tvBanner: TextView
    private lateinit var cardempty: MaterialCardView
    private lateinit var llList: LinearLayout

    private var alerts: MutableList<DietitianCriticalAlertDto> = mutableListOf()
    private var busyId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dietitian_critical_alerts)
        DietitianBottomBarHelper.bind(this, 4)

        progress = findViewById(R.id.progressCriticalAlerts)
        tvError = findViewById(R.id.tvCriticalAlertsError)
        cardBanner = findViewById(R.id.cardCriticalAlertsBanner)
        tvBanner = findViewById(R.id.tvCriticalAlertsBannerText)
        cardempty = findViewById(R.id.cardCriticalAlertsEmpty)
        llList = findViewById(R.id.llCriticalAlertsList)

        loadAlerts()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun alertTypeLabel(t: String) = when (t) {
        "MissedMeals" -> "Öğün tamamlama"
        "HighCalories" -> "Yüksek kalori"
        else -> t
    }

    private fun loadAlerts() {
        progress.visibility = View.VISIBLE
        tvError.visibility = View.GONE
        cardBanner.visibility = View.GONE
        cardempty.visibility = View.GONE
        llList.removeAllViews()
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.getDietitianCriticalAlerts()
                if (r.isSuccessful) {
                    alerts = r.body().orEmpty().toMutableList()
                    bindUi()
                } else {
                    val raw = r.errorBody()?.string().orEmpty()
                    val msg = try {
                        JSONObject(raw).optString("message")
                    } catch (_: Exception) {
                        raw
                    }
                    tvError.text = msg.ifBlank { "Uyarılar alınamadı (${r.code()})" }
                    tvError.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                tvError.text = e.message ?: "Bağlantı hatası"
                tvError.visibility = View.VISIBLE
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    private fun bindUi() {
        llList.removeAllViews()
        if (alerts.isEmpty()) {
            cardempty.visibility = View.VISIBLE
            return
        }
        cardBanner.visibility = View.VISIBLE
        val highCount = alerts.count { it.severity.equals("High", ignoreCase = true) }
        val line1 = "Kritik durumda ${alerts.size} kayıt"
        val rest = when {
            highCount > 0 -> " ($highCount yüksek öncelik). Bu danışanlar kısa sürede değerlendirme gerektirebilir; kaydı onaylayarak arşivleyebilirsiniz."
            else -> " Orta öncelikli uyarıları inceleyip onaylayabilirsiniz."
        }
        val full = line1 + rest
        val sp = SpannableString(full)
        sp.setSpan(StyleSpan(Typeface.BOLD), 0, line1.length, 0)
        tvBanner.text = sp

        for (a in alerts) {
            llList.addView(buildAlertCard(a))
        }
    }

    private fun buildAlertCard(a: DietitianCriticalAlertDto): View {
        val isHigh = a.severity.equals("High", ignoreCase = true)
        val stroke = if (isHigh) android.graphics.Color.parseColor("#FDA4AF") else android.graphics.Color.parseColor("#FCD34D")
        val leftStripe = if (isHigh) android.graphics.Color.parseColor("#F43F5E") else android.graphics.Color.parseColor("#D97706")
        val card = MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = dp(2).toFloat()
            strokeWidth = dp(1)
            this.strokeColor = stroke
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(14) }
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        val stripe = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(4), ViewGroup.LayoutParams.MATCH_PARENT).apply {
                marginEnd = dp(12)
            }
            setBackgroundColor(leftStripe)
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val head = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val avatarFg = if (isHigh) "#E11D48" else "#B45309"
        val initial = (a.clientName.ifBlank { "?" }).trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        head.addView(TextView(this).apply {
            text = initial
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor(avatarFg))
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
            setBackgroundResource(R.drawable.diet_critical_avatar_bg)
        })
        val names = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(10)
            }
        }
        names.addView(TextView(this).apply {
            text = a.clientName.ifBlank { "Danışan" }
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@DietitianCriticalAlertsActivity, R.color.admin_strong))
        })
        names.addView(TextView(this).apply {
            textSize = 11f
            setTextColor(ContextCompat.getColor(this@DietitianCriticalAlertsActivity, R.color.admin_muted))
            text = "Tarih: ${formatAlertDate(a.date)}"
        })
        head.addView(names)
        val chip = TextView(this).apply {
            text = if (isHigh) "Yüksek öncelik" else "Orta öncelik"
            textSize = 11f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setTextColor(android.graphics.Color.parseColor(if (isHigh) "#BE123C" else "#92400E"))
            setBackgroundColor(android.graphics.Color.parseColor(if (isHigh) "#FEE2E2" else "#FEF3C7"))
        }
        head.addView(chip)
        content.addView(head)

        val typeRowBg = ContextCompat.getColor(this, R.color.admin_row_surface)
        val typeRow = TextView(this).apply {
            text = "⚠ ${alertTypeLabel(a.alertType)}"
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@DietitianCriticalAlertsActivity, R.color.admin_strong))
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setBackgroundColor(typeRowBg)
        }
        content.addView(typeRow.apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
        })

        content.addView(TextView(this).apply {
            text = a.message
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@DietitianCriticalAlertsActivity, R.color.admin_muted))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        })

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(14) }
        }
        val btnAck = MaterialButton(this).apply {
            text = "İncelendi (diyetisyen onayı)"
            textSize = 13f
            isEnabled = true
            setOnClickListener { acknowledge(a) }
        }
        val btnProfile = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Profili gör"
            textSize = 13f
            setOnClickListener { showClientBrief(a.clientId) }
        }
        btnRow.addView(btnAck, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = dp(8)
        })
        btnRow.addView(btnProfile, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        content.addView(btnRow)

        row.addView(stripe)
        row.addView(content)
        card.addView(row)
        return card
    }

    private fun formatAlertDate(iso: String): String {
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (p in patterns) {
            try {
                val sdf = SimpleDateFormat(p, Locale.ROOT)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val d = sdf.parse(iso) ?: continue
                val out = SimpleDateFormat("d MMMM yyyy", Locale("tr", "TR"))
                out.timeZone = TimeZone.getDefault()
                return out.format(d)
            } catch (_: Exception) {
            }
        }
        return iso
    }

    private fun acknowledge(a: DietitianCriticalAlertDto) {
        if (busyId != null) return
        busyId = a.id
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.acknowledgeCriticalAlert(
                    AckCriticalAlertRequest(
                        clientId = a.clientId,
                        alertType = a.alertType,
                        referenceDate = a.referenceDate
                    )
                )
                if (r.isSuccessful) {
                    alerts.removeAll { it.id == a.id }
                    bindUi()
                } else {
                    Toast.makeText(this@DietitianCriticalAlertsActivity, "Onay kaydedilemedi", Toast.LENGTH_LONG)
                        .show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DietitianCriticalAlertsActivity, e.message ?: "Hata", Toast.LENGTH_LONG).show()
            } finally {
                busyId = null
            }
        }
    }

    private fun showClientBrief(clientId: String) {
        val scroll = ScrollView(this)
        val tv = TextView(this).apply {
            setPadding(dp(16), dp(16), dp(16), dp(16))
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@DietitianCriticalAlertsActivity, R.color.admin_strong))
            text = "Yükleniyor…"
        }
        scroll.addView(tv)
        val dlg = AlertDialog.Builder(this)
            .setTitle("Danışan özeti")
            .setView(scroll)
            .setNegativeButton("Kapat", null)
            .create()
        dlg.show()
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.getDietitianClientBrief(clientId)
                if (r.isSuccessful) {
                    val b = r.body()
                    if (b != null) {
                        tv.text = buildString {
                            appendLine("Ad: ${b.firstName.orEmpty()} ${b.lastName.orEmpty()}".trim())
                            appendLine("E-posta: ${b.email?.trim()?.takeIf { it.isNotEmpty() } ?: "—"}")
                            appendLine(
                                "Telefon: ${
                                    b.phone?.trim()?.takeIf { it.isNotEmpty() } ?: "—"
                                }"
                            )
                            appendLine("Hedef kalori: ${b.targetCalories} kkal")
                            appendLine("Kilo: ${b.weight} kg")
                            appendLine("Boy: ${b.height} cm")
                        }.trim()
                    } else {
                        tv.text = "Profil yüklenemedi veya erişim yok."
                        tv.setTextColor(ContextCompat.getColor(this@DietitianCriticalAlertsActivity, R.color.um_chip_red_text))
                    }
                } else {
                    tv.text = "Profil yüklenemedi veya erişim yok."
                    tv.setTextColor(ContextCompat.getColor(this@DietitianCriticalAlertsActivity, R.color.um_chip_red_text))
                }
            } catch (_: Exception) {
                tv.text = "Profil yüklenemedi veya erişim yok."
                tv.setTextColor(ContextCompat.getColor(this@DietitianCriticalAlertsActivity, R.color.um_chip_red_text))
            }
        }
    }
}
