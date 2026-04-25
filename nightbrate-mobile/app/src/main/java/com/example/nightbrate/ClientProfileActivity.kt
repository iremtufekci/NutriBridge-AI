package com.example.nightbrate

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ClientProfileActivity : AppCompatActivity() {
    private var currentTheme: String = "light"
    private var pendingDietCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_profile)
        ClientBottomBarHelper.bind(this, 4)
        val btnTheme = findViewById<TextView>(R.id.btnTheme)
        val btnPrivacy = findViewById<TextView>(R.id.btnPrivacy)
        val btnAbout = findViewById<TextView>(R.id.btnAbout)
        val tvInfo = findViewById<TextView>(R.id.tvInfo)

        btnPrivacy.setOnClickListener {
            showInfo(
                tvInfo,
                "Gizlilik: Verileriniz sadece saglik takibi icin, guvenli sekilde saklanir (web ile ayni yaklasim)."
            )
        }
        btnAbout.setOnClickListener {
            showInfo(
                tvInfo,
                "NutriBridge, danisan ve diyetisyen surecini kolaylastirmak icin gelistirilmistir."
            )
        }
        btnTheme.setOnClickListener { toggleTheme() }
        setupDietitianConnect()
        loadProfile(btnTheme)
    }

    private fun setupDietitianConnect() {
        val card = findViewById<CardView>(R.id.cardConnectDietitian)
        val et = findViewById<EditText>(R.id.etDietCode)
        val btnVerify = findViewById<TextView>(R.id.btnVerifyDietCode)
        val btnConfirm = findViewById<TextView>(R.id.btnConfirmDietCode)
        val tvPreview = findViewById<TextView>(R.id.tvDietCodePreview)

        fun resetPreview() {
            tvPreview.visibility = View.GONE
            btnConfirm.visibility = View.GONE
            tvPreview.text = ""
            pendingDietCode = null
        }

        btnVerify.setOnClickListener {
            val code = et.text.toString().trim().uppercase(Locale.ROOT)
            if (code.length != 6) {
                Toast.makeText(this, "6 hane girin (harf ve rakam)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            resetPreview()
            lifecycleScope.launch {
                try {
                    val r = RetrofitClient.instance.previewDietitianByCode(ConnectToDietitianRequest(code))
                    if (r.isSuccessful) {
                        val b = r.body()
                        val name = b?.displayName ?: "Dr. ${b?.firstName} ${b?.lastName}"
                        tvPreview.text = "Bulundu: $name"
                        tvPreview.visibility = View.VISIBLE
                        btnConfirm.visibility = View.VISIBLE
                        pendingDietCode = code
                    } else {
                        Toast.makeText(
                            this@ClientProfileActivity,
                            "Kod gecerli degil (onayli diyetisyen gerekir)",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this@ClientProfileActivity,
                        e.message ?: "Baglanti hatasi",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        btnConfirm.setOnClickListener {
            val code = pendingDietCode
            if (code.isNullOrBlank()) return@setOnClickListener
            val display = tvPreview.text.toString().removePrefix("Bulundu: ").trim()
            AlertDialog.Builder(this@ClientProfileActivity)
                .setTitle("Eşleştirme onayı")
                .setMessage("Veritabanına kayıt edilecektir.\n\n$display\n\nOnaylıyor musunuz?")
                .setNegativeButton("Hayır", null)
                .setPositiveButton("Evet") { _, _ ->
                    lifecycleScope.launch {
                        try {
                            val r = RetrofitClient.instance.connectToDietitian(ConnectToDietitianRequest(code))
                            if (r.isSuccessful) {
                                val msg = r.body()?.message ?: "Eslesti"
                                Toast.makeText(this@ClientProfileActivity, msg, Toast.LENGTH_LONG).show()
                                et.setText("")
                                resetPreview()
                                loadProfile(findViewById(R.id.btnTheme))
                            } else {
                                Toast.makeText(
                                    this@ClientProfileActivity,
                                    "Eslestirme basarisiz (${r.code()})",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@ClientProfileActivity,
                                e.message ?: "Hata",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                .show()
        }

    }

    private fun showInfo(tv: TextView, text: String) {
        tv.text = text
        tv.visibility = View.VISIBLE
    }

    private fun loadProfile(btnTheme: TextView) {
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.getClientProfile()
                if (r.isSuccessful) {
                    val p = r.body() ?: return@launch
                    val cardConnect = findViewById<CardView>(R.id.cardConnectDietitian)
                    val dName = p.dietitianName?.trim().orEmpty()
                    cardConnect.visibility =
                        if (dName.isEmpty() || dName == "Atanmadi") View.VISIBLE else View.GONE
                    val first = p.firstName?.trim().orEmpty()
                    val last = p.lastName?.trim().orEmpty()
                    val name = listOf(first, last).filter { it.isNotEmpty() }.joinToString(" ")
                        .ifEmpty { "Danisan" }
                    findViewById<TextView>(R.id.tvDisplayName).text = name
                    findViewById<TextView>(R.id.tvDietitian).text =
                        "Diyetisyen: ${p.dietitianName ?: "Atanmadi"}"
                    findViewById<TextView>(R.id.tvHeight).text =
                        if (p.height > 0) p.height.toInt().toString() else "-"
                    findViewById<TextView>(R.id.tvWeight).text =
                        if (p.weight > 0) p.weight.toString() else "-"
                    findViewById<TextView>(R.id.tvGoal).text = p.goalText ?: "—"
                    val pStart = p.programStartDate?.let { formatProgramDate(it) } ?: "—"
                    findViewById<TextView>(R.id.tvProgramStart).text = "Program baslangici: $pStart"
                    val a = (first.take(1) + last.take(1)).uppercase(Locale.ROOT)
                    findViewById<TextView>(R.id.avatarText).text = if (a.isNotBlank()) a else "D"
                    currentTheme = if (p.themePreference == "dark") "dark" else "light"
                    applyMode(currentTheme)
                    btnTheme.text = if (currentTheme == "dark") "Tema: Koyu (degistir)" else "Tema: Acik (degistir)"
                } else {
                    Toast.makeText(
                        this@ClientProfileActivity,
                        "Profil yuklenemedi",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ClientProfileActivity,
                    "Baglanti hatasi: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun formatProgramDate(iso: String): String {
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
            } catch (_: Exception) { }
        }
        return iso
    }

    private fun applyMode(mode: String) {
        if (mode == "dark")
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        else
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    private fun toggleTheme() {
        val next = if (currentTheme == "dark") "light" else "dark"
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.updateClientTheme(
                    UpdateThemeRequest(themePreference = next)
                )
                if (r.isSuccessful) {
                    currentTheme = next
                    applyMode(next)
                    findViewById<TextView>(R.id.btnTheme).text =
                        if (next == "dark") "Tema: Koyu (degistir)" else "Tema: Acik (degistir)"
                } else {
                    Toast.makeText(this@ClientProfileActivity, "Tema kaydedilemedi", Toast.LENGTH_LONG)
                        .show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ClientProfileActivity,
                    e.message ?: "Hata",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
