package com.example.nightbrate

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class AdminDietitianApprovalsActivity : AppCompatActivity() {

    private lateinit var list: LinearLayout
    private lateinit var progress: ProgressBar
    private lateinit var empty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_approvals)
        AdminBottomBarHelper.bind(this, 2)
        list = findViewById(R.id.approvalList)
        progress = findViewById(R.id.approvalsProgress)
        empty = findViewById(R.id.tvApprovalsEmpty)
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
                    empty.text = "Liste alınamadı (kod: ${r.code()})"
                    empty.visibility = View.VISIBLE
                    return@launch
                }
                val body = r.body() ?: emptyList()
                if (body.isEmpty()) {
                    empty.text = "Onay bekleyen diyetisyen yok."
                    empty.visibility = View.VISIBLE
                    return@launch
                }
                for (item in body) {
                    addCard(item)
                }
            } catch (e: Exception) {
                progress.visibility = View.GONE
                empty.text = e.message ?: "Bağlantı hatası"
                empty.visibility = View.VISIBLE
            }
        }
    }

    private fun addCard(d: PendingDietitianItem) {
        val v = layoutInflater.inflate(R.layout.item_pending_dietitian, list, false)
        v.findViewById<TextView>(R.id.pdName).text =
            "Dr. ${d.firstName.orEmpty().trim()} ${d.lastName.orEmpty().trim()}".trim()
        v.findViewById<TextView>(R.id.pdEmail).text = d.email.orEmpty()
        v.findViewById<TextView>(R.id.pdDiploma).text =
            "Diploma no: ${d.diplomaNo ?: "—"}"
        v.findViewById<TextView>(R.id.pdClinic).text =
            "Klinik: ${d.clinicName ?: "—"}"
        v.findViewById<TextView>(R.id.pdStatus).text =
            if (d.isApproved == true) "Onaylı" else "Onaylanmadı"
        val id = d.id
        v.findViewById<Button>(R.id.pdApprove).setOnClickListener {
            if (id.isNullOrBlank()) return@setOnClickListener
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
                            "Onay başarısız: ${ar.code()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this@AdminDietitianApprovalsActivity,
                        e.message ?: "Hata",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        list.addView(v)
    }
}
