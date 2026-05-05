package com.example.nightbrate

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response

class ClientPdfAnalysisActivity : AppCompatActivity() {

    private val maxBytes = 10 * 1024 * 1024

    private val pickPdf = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) uploadUri(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_pdf_analysis)
        ClientBottomBarHelper.bind(this, 4)

        findViewById<MaterialButton>(R.id.btnPickPdf).setOnClickListener {
            pickPdf.launch("application/pdf")
        }

        loadHistory()
    }

    private fun uploadUri(uri: Uri) {
        val tvErr = findViewById<TextView>(R.id.tvPdfError)
        val progress = findViewById<ProgressBar>(R.id.progressPdf)
        val card = findViewById<MaterialCardView>(R.id.cardPdfResult)
        tvErr.visibility = View.GONE
        card.visibility = View.GONE

        lifecycleScope.launch {
            progress.visibility = View.VISIBLE
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val cr = contentResolver
                    val displayName = cr.query(uri, null, null, null, null)?.use { c ->
                        val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (c.moveToFirst() && idx >= 0) c.getString(idx) else null
                    } ?: "belge.pdf"

                    val bytes = cr.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IllegalArgumentException("Dosya okunamadı.")
                    if (bytes.size > maxBytes) throw IllegalArgumentException("PDF en fazla 10 MB olabilir.")
                    if (bytes.isEmpty()) throw IllegalArgumentException("Dosya boş.")

                    val safeName = if (displayName.lowercase().endsWith(".pdf")) displayName else "$displayName.pdf"
                    val body = bytes.toRequestBody("application/pdf".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("pdf", safeName, body)
                    RetrofitClient.instance.uploadClientPdf(part)
                }
            }
            progress.visibility = View.GONE
            result.onSuccess { resp ->
                if (resp.isSuccessful && resp.body() != null) {
                    bindResult(resp.body()!!)
                    loadHistory()
                } else {
                    tvErr.text = readErrorMessage(resp)
                    tvErr.visibility = View.VISIBLE
                }
            }.onFailure { e ->
                tvErr.text = e.message ?: "Yükleme başarısız."
                tvErr.visibility = View.VISIBLE
            }
        }
    }

    private fun readErrorMessage(resp: Response<*>): String {
        val raw = resp.errorBody()?.string().orEmpty()
        return try {
            val msg = org.json.JSONObject(raw).optString("message")
            if (msg.isNotBlank()) msg else "Hata: ${resp.code()}"
        } catch (_: Exception) {
            if (raw.isNotBlank()) raw else "Hata: ${resp.code()}"
        }
    }

    private fun bindResult(d: ClientPdfAnalysisResponseDto) {
        findViewById<MaterialCardView>(R.id.cardPdfResult).visibility = View.VISIBLE
        findViewById<TextView>(R.id.tvPdfDocType).text = d.documentType ?: "Belge"
        findViewById<TextView>(R.id.tvPdfSource).text =
            if (d.analysisSource == "gemini") "Yapay zeka analizi" else "Örnek / yapılandırma"
        findViewById<TextView>(R.id.tvPdfSummary).text = d.summary.orEmpty()
        val url = pdfPublicUrl(d.pdfUrl)
        val link = findViewById<TextView>(R.id.tvPdfLink)
        link.text = "PDF: ${d.originalFileName ?: "İndir"}"
        link.setOnClickListener {
            if (url.isNotBlank()) {
                startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url)))
            } else {
                Toast.makeText(this, "Bağlantı yok", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun pdfPublicUrl(rel: String?): String {
        val r = rel?.trim().orEmpty()
        if (r.isEmpty()) return ""
        if (r.startsWith("http://", true) || r.startsWith("https://", true)) return r
        val base = RetrofitClient.API_BASE_URL.trimEnd('/')
        val p = if (r.startsWith("/")) r else "/$r"
        return base + p
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching { RetrofitClient.instance.getClientPdfAnalyses(30) }
            }
            val resp = r.getOrNull() ?: return@launch
            if (!resp.isSuccessful || resp.body() == null) return@launch
            bindHistory(resp.body()!!)
        }
    }

    private fun bindHistory(items: List<ClientPdfAnalysisResponseDto>) {
        val container = findViewById<LinearLayout>(R.id.containerPdfHistory)
        container.removeAllViews()
        val strong = ContextCompat.getColor(this, R.color.admin_strong)
        val muted = ContextCompat.getColor(this, R.color.admin_muted)
        if (items.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "Henüz kayıt yok."
                setTextColor(muted)
                textSize = 14f
            })
            return
        }
        val dp = resources.displayMetrics.density
        fun dpf(v: Int) = (v * dp).toInt()
        for (h in items) {
            val card = MaterialCardView(this).apply {
                radius = dpf(16).toFloat()
                strokeWidth = dpf(1)
                strokeColor = ContextCompat.getColor(this@ClientPdfAnalysisActivity, R.color.admin_row_stroke)
                setCardBackgroundColor(ContextCompat.getColor(this@ClientPdfAnalysisActivity, R.color.admin_card_bg))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dpf(10)
                }
            }
            val inner = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpf(14), dpf(14), dpf(14), dpf(14))
            }
            inner.addView(TextView(this).apply {
                text = h.originalFileName.orEmpty()
                setTextColor(strong)
                textSize = 15f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            inner.addView(TextView(this).apply {
                text = h.documentType.orEmpty()
                setTextColor(muted)
                textSize = 12f
                setPadding(0, dpf(4), 0, 0)
            })
            inner.addView(TextView(this).apply {
                text = h.summary.orEmpty()
                setTextColor(muted)
                textSize = 13f
                maxLines = 3
            })
            card.addView(inner)
            container.addView(card)
        }
    }
}
