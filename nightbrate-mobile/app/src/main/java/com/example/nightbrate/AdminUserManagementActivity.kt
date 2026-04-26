package com.example.nightbrate

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class AdminUserManagementActivity : AppCompatActivity() {

    private lateinit var list: LinearLayout
    private lateinit var progress: ProgressBar
    private lateinit var err: TextView
    private lateinit var tvStats: TextView
    private lateinit var etSearch: EditText
    private lateinit var btnLoad: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_user_management)
        AdminBottomBarHelper.bind(this, 1)
        list = findViewById(R.id.umgList)
        progress = findViewById(R.id.umgProgress)
        err = findViewById(R.id.tvUmgError)
        tvStats = findViewById(R.id.tvUmgStats)
        etSearch = findViewById(R.id.etUmgSearch)
        btnLoad = findViewById(R.id.btnUmgLoad)
        btnLoad.setOnClickListener { loadAll() }
        loadAll()
    }

    private fun loadAll() {
        progress.visibility = View.VISIBLE
        err.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val statsR = RetrofitClient.instance.getUserManagementStats()
                if (statsR.isSuccessful) {
                    val s = statsR.body()
                    if (s != null) {
                        tvStats.text = buildString {
                            append("Top: ${s.totalUsers}  Admin: ${s.admins}  ")
                            append("Diyet: ${s.dietitians}  Danışan: ${s.clients}\n")
                            append("Aktif: ${s.active}  Bekleyen: ${s.pending}")
                        }
                    }
                }

                val q = etSearch.text?.toString()?.trim().orEmpty()
                val uR = RetrofitClient.instance.getUserManagementUsers(
                    if (q.isNotEmpty()) q else null,
                    null,
                    null
                )
                progress.visibility = View.GONE
                if (!uR.isSuccessful) {
                    err.text = "Liste alınamadı (kod: ${uR.code()})"
                    err.visibility = View.VISIBLE
                    return@launch
                }
                val body = uR.body() ?: emptyList()
                list.removeAllViews()
                if (body.isEmpty()) {
                    err.text = "Kullanıcı yok veya eşleşen sonuç yok."
                    err.visibility = View.VISIBLE
                }
                for (item in body) {
                    addCard(item)
                }
            } catch (e: Exception) {
                progress.visibility = View.GONE
                err.text = e.message ?: "Bağlantı hatası"
                err.visibility = View.VISIBLE
            }
        }
    }

    private fun addCard(u: AdminUserRowItem) {
        val v = layoutInflater.inflate(R.layout.item_admin_user_row, list, false)
        v.findViewById<TextView>(R.id.auName).text = u.displayName.orEmpty()
        v.findViewById<TextView>(R.id.auEmail).text = u.email.orEmpty()
        v.findViewById<TextView>(R.id.auStatus).text = buildString {
            append("Rol: ${u.role ?: "—"}  •  ${u.statusLabel ?: "—"}")
        }
        val id = u.id
        v.findViewById<Button>(R.id.auLog).setOnClickListener {
            if (id.isNullOrBlank()) return@setOnClickListener
            lifecycleScope.launch {
                try {
                    val r = RetrofitClient.instance.getUserActivityLogs(id, 40)
                    if (!r.isSuccessful) {
                        Toast.makeText(
                            this@AdminUserManagementActivity,
                            "Log alınamadı: ${r.code()}",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }
                    val items = r.body() ?: emptyList()
                    val sb = StringBuilder()
                    for (a in items) {
                        sb.append(a.description).append("\n")
                    }
                    if (sb.isEmpty()) sb.append("Kayıt yok.")
                    AlertDialog.Builder(this@AdminUserManagementActivity)
                        .setTitle("Aktivite")
                        .setMessage(sb.toString())
                        .setPositiveButton("Tamam", null)
                        .show()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@AdminUserManagementActivity,
                        e.message ?: "Hata",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        val blockBtn = v.findViewById<Button>(R.id.auBlock)
        if (u.isSuspended == true) {
            blockBtn.text = "Askıyı kaldır"
            blockBtn.setOnClickListener {
                if (id.isNullOrBlank()) return@setOnClickListener
                AlertDialog.Builder(this)
                    .setMessage("Askıyı kaldırmak istiyor musunuz?")
                    .setNegativeButton("İptal", null)
                    .setPositiveButton("Evet") { _, _ ->
                        lifecycleScope.launch {
                            try {
                                val r = RetrofitClient.instance.unsuspendUser(id)
                                if (r.isSuccessful) {
                                    Toast.makeText(
                                        this@AdminUserManagementActivity,
                                        "Askı kaldırıldı",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    loadAll()
                                } else {
                                    Toast.makeText(
                                        this@AdminUserManagementActivity,
                                        "Hata: ${r.code()}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(
                                    this@AdminUserManagementActivity,
                                    e.message ?: "Hata",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                    .show()
            }
        } else {
            blockBtn.text = "Askıya al"
            blockBtn.setOnClickListener {
                if (id.isNullOrBlank()) return@setOnClickListener
                val input = EditText(this)
                input.hint = "Giriş ekranında gösterilecek neden"
                AlertDialog.Builder(this)
                    .setTitle("Kullanıcıyı askıya al")
                    .setView(input)
                    .setNegativeButton("İptal", null)
                    .setPositiveButton("Askıya al") { _, _ ->
                        val msg = input.text?.toString()?.trim().orEmpty()
                        if (msg.isEmpty()) {
                            Toast.makeText(
                                this@AdminUserManagementActivity,
                                "Mesaj gerekli",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setPositiveButton
                        }
                        lifecycleScope.launch {
                            try {
                                val r = RetrofitClient.instance.suspendUser(
                                    id,
                                    SetUserSuspensionRequest(msg)
                                )
                                if (r.isSuccessful) {
                                    Toast.makeText(
                                        this@AdminUserManagementActivity,
                                        "Kullanıcı askıya alındı",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    loadAll()
                                } else {
                                    Toast.makeText(
                                        this@AdminUserManagementActivity,
                                        "Hata: ${r.code()}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(
                                    this@AdminUserManagementActivity,
                                    e.message ?: "Hata",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                    .show()
            }
        }
        list.addView(v)
    }
}
