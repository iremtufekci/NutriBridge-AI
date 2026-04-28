package com.example.nightbrate

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AdminDietitianApprovalsActivity : AppCompatActivity() {

    private lateinit var list: LinearLayout
    private lateinit var progress: ProgressBar
    private lateinit var empty: TextView
    private lateinit var badge: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_approvals)
        AdminBottomBarHelper.bind(this, 2)
        list = findViewById(R.id.approvalList)
        progress = findViewById(R.id.approvalsProgress)
        empty = findViewById(R.id.tvApprovalsEmpty)
        badge = findViewById(R.id.tvApprovalsBadge)
        load()
    }

    private fun load() {
        progress.visibility = View.VISIBLE
        empty.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.getPendingDietitians()
                progress.visibility = View.GONE
                list.removeAllViews()
                if (!r.isSuccessful) {
                    empty.text = readErrorMessage(r)
                    empty.visibility = View.VISIBLE
                    badge.text = "—"
                    return@launch
                }
                val body = r.body() ?: emptyList()
                badge.text = "${body.size} Onay bekliyor"
                if (body.isEmpty()) {
                    empty.text = "Onay bekleyen diyetisyen bulunmuyor."
                    empty.visibility = View.VISIBLE
                    return@launch
                }
                for (item in body) {
                    addRow(item)
                }
            } catch (e: Exception) {
                progress.visibility = View.GONE
                empty.text = e.message ?: "Bağlantı hatası"
                empty.visibility = View.VISIBLE
                badge.text = "—"
            }
        }
    }

    private fun addRow(d: PendingDietitianItem) {
        val v = layoutInflater.inflate(R.layout.item_approval_row, list, false)
        val initial = (d.firstName?.take(1) ?: "D").uppercase()
        v.findViewById<TextView>(R.id.apAvatar).text = initial
        v.findViewById<TextView>(R.id.apName).text =
            "Dr. ${d.firstName.orEmpty().trim()} ${d.lastName.orEmpty().trim()}".trim()
        v.findViewById<TextView>(R.id.apEmail).text = d.email.orEmpty().ifBlank { "—" }
        v.findViewById<TextView>(R.id.apDiploma).text =
            "Diploma: ${d.diplomaNo ?: "—"}"
        v.findViewById<TextView>(R.id.apClinic).text =
            "Klinik: ${d.clinicName ?: "—"}"
        v.findViewById<TextView>(R.id.apDate).text =
            "Kayıt: ${formatDateTr(d.createdAt)}"

        val id = d.id
        v.findViewById<MaterialButton>(R.id.apInspect).setOnClickListener {
            if (id.isNullOrBlank()) return@setOnClickListener
            showDetailDialog(id)
        }
        v.findViewById<MaterialButton>(R.id.apApprove).setOnClickListener {
            if (id.isNullOrBlank()) return@setOnClickListener
            approveById(id)
        }
        list.addView(v)
    }

    private fun showDetailDialog(dietitianId: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_dietitian_detail, null)
        val dlgProgress = dialogView.findViewById<ProgressBar>(R.id.detailProgress)
        val scroll = dialogView.findViewById<View>(R.id.detailScroll)
        val ddAvatar = dialogView.findViewById<TextView>(R.id.ddAvatar)
        val ddName = dialogView.findViewById<TextView>(R.id.ddName)
        val ddDiploma = dialogView.findViewById<TextView>(R.id.ddDiploma)
        val ddClinic = dialogView.findViewById<TextView>(R.id.ddClinic)
        val ddCreated = dialogView.findViewById<TextView>(R.id.ddCreated)
        val ddStatus = dialogView.findViewById<TextView>(R.id.ddStatus)
        val ddDownload = dialogView.findViewById<MaterialButton>(R.id.ddDownload)
        val ddApprove = dialogView.findViewById<MaterialButton>(R.id.ddApprove)

        scroll.visibility = View.GONE
        dlgProgress.visibility = View.VISIBLE

        val dlg = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setNegativeButton("Kapat", null)
            .create()
        dlg.show()

        lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.getAdminDietitianDetail(dietitianId)
                dlgProgress.visibility = View.GONE
                if (!r.isSuccessful) {
                    Toast.makeText(this@AdminDietitianApprovalsActivity, readErrorMessage(r), Toast.LENGTH_LONG).show()
                    dlg.dismiss()
                    return@launch
                }
                val sel = r.body()
                if (sel == null) {
                    dlg.dismiss()
                    return@launch
                }
                scroll.visibility = View.VISIBLE
                ddAvatar.text = (sel.firstName?.take(1) ?: "D").uppercase()
                ddName.text = "Dr. ${sel.firstName.orEmpty()} ${sel.lastName.orEmpty()}".trim()
                ddDiploma.text = sel.diplomaNo?.ifBlank { "—" } ?: "—"
                ddClinic.text = sel.clinicName?.ifBlank { "—" } ?: "—"
                ddCreated.text = formatDateTr(sel.createdAt)
                if (sel.isApproved == true) {
                    ddStatus.text = "Onaylandı"
                    ddStatus.setBackgroundResource(R.drawable.um_chip_status_active)
                    ddStatus.setTextColor(ContextCompat.getColor(this@AdminDietitianApprovalsActivity, R.color.um_chip_emerald_text))
                } else {
                    ddStatus.text = "Beklemede"
                    ddStatus.setBackgroundResource(R.drawable.um_chip_status_pending)
                    ddStatus.setTextColor(ContextCompat.getColor(this@AdminDietitianApprovalsActivity, R.color.um_chip_amber_text))
                }

                val url = sel.diplomaDocumentUrl?.trim().orEmpty()
                ddDownload.setOnClickListener {
                    if (url.isEmpty()) {
                        Toast.makeText(
                            this@AdminDietitianApprovalsActivity,
                            "Bu kayıt için yüklenmiş diploma dosyası bulunmuyor.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } catch (_: Exception) {
                            Toast.makeText(this@AdminDietitianApprovalsActivity, "Bağlantı açılamadı", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                ddApprove.setOnClickListener {
                    val aid = sel.id ?: dietitianId
                    dlg.dismiss()
                    approveById(aid)
                }
            } catch (e: Exception) {
                dlgProgress.visibility = View.GONE
                Toast.makeText(this@AdminDietitianApprovalsActivity, e.message ?: "Hata", Toast.LENGTH_LONG).show()
                dlg.dismiss()
            }
        }
    }

    private fun approveById(id: String) {
        lifecycleScope.launch {
            try {
                val ar = RetrofitClient.instance.approveDietitian(id)
                if (ar.isSuccessful) {
                    Toast.makeText(
                        this@AdminDietitianApprovalsActivity,
                        "Onaylandı. Diyetisyen giriş yapabilir.",
                        Toast.LENGTH_LONG
                    ).show()
                    load()
                } else {
                    Toast.makeText(
                        this@AdminDietitianApprovalsActivity,
                        readErrorMessage(ar),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdminDietitianApprovalsActivity, e.message ?: "Hata", Toast.LENGTH_LONG).show()
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

    private fun formatDateTr(iso: String?): String {
        if (iso.isNullOrBlank()) return "—"
        val ms = parseIsoToMillis(iso) ?: return "—"
        return SimpleDateFormat("d.MM.yyyy", Locale("tr", "TR")).format(Date(ms))
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
