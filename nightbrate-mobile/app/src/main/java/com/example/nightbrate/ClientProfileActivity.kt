package com.example.nightbrate

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ClientProfileActivity : AppCompatActivity() {
    private var currentTheme: String = "light"
    private var pendingDietCode: String? = null
    private var lastProfile: ClientProfileResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_profile)
        ClientBottomBarHelper.bind(this, 5)
        val btnTheme = findViewById<TextView>(R.id.btnTheme)
        val btnPrivacy = findViewById<TextView>(R.id.btnPrivacy)
        val btnAbout = findViewById<TextView>(R.id.btnAbout)
        val btnEdit = findViewById<TextView>(R.id.btnEditProfile)

        btnPrivacy.setOnClickListener {
            showLongTextDialog(
                "Gizlilik politikasi",
                getString(R.string.client_privacy_policy_text)
            )
        }
        btnAbout.setOnClickListener {
            showLongTextDialog("Hakkinda", getString(R.string.client_about_text))
        }
        btnEdit.setOnClickListener { showEditProfileDialog() }
        btnTheme.setOnClickListener { toggleTheme() }
        setupDietitianConnect()
        loadProfile(btnTheme)
    }

    private fun showLongTextDialog(title: String, text: String) {
        val scroll = ScrollView(this)
        val tv = TextView(this).apply {
            setPadding(48, 32, 48, 32)
            textSize = 14f
            setTextIsSelectable(true)
            this.text = text
        }
        scroll.addView(tv)
        val pad = (resources.displayMetrics.density * 8).toInt()
        scroll.setPadding(pad, 0, pad, 0)
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(scroll)
            .setPositiveButton("Kapat", null)
            .show()
    }

    private fun showEditProfileDialog() {
        val p = lastProfile
        if (p == null) {
            Toast.makeText(this, "Once profil yuklensin", Toast.LENGTH_SHORT).show()
            return
        }
        val view = layoutInflater.inflate(R.layout.dialog_client_edit_profile, null, false)
        val etF = view.findViewById<EditText>(R.id.dlgEtFirst)
        val etL = view.findViewById<EditText>(R.id.dlgEtLast)
        val etH = view.findViewById<EditText>(R.id.dlgEtHeight)
        val etW = view.findViewById<EditText>(R.id.dlgEtWeight)
        val etC = view.findViewById<EditText>(R.id.dlgEtCal)
        etF.setText(p.firstName.orEmpty().trim())
        etL.setText(p.lastName.orEmpty().trim())
        if (p.height > 0) etH.setText(String.format(Locale.ROOT, "%.1f", p.height).trimEnd('0').trimEnd('.'))
        if (p.weight > 0) etW.setText(String.format(Locale.ROOT, "%.1f", p.weight).trimEnd('0').trimEnd('.'))
        etC.setText(p.targetCalories.toString())
        val dlg = AlertDialog.Builder(this)
            .setTitle("Kisisel bilgileri duzenle")
            .setView(view)
            .setNegativeButton("Iptal", null)
            .setPositiveButton("Kaydet", null)
            .create()
        dlg.setOnShowListener {
            dlg.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            etF.post {
                etF.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showSoftInput(etF, InputMethodManager.SHOW_IMPLICIT)
            }
            dlg.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                val fn = etF.text.toString().trim()
                val ln = etL.text.toString().trim()
                if (fn.isEmpty() || ln.isEmpty()) {
                    Toast.makeText(this, "Ad ve soyad gerekli", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                val h = etH.text.toString().replace(',', '.').toDoubleOrNull()
                val w = etW.text.toString().replace(',', '.').toDoubleOrNull()
                val cal = etC.text.toString().toIntOrNull()
                if (h == null || h < 50.0 || h > 250.0) {
                    Toast.makeText(this, "Boy 50-250 cm", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                if (w == null || w < 20.0 || w > 400.0) {
                    Toast.makeText(this, "Kilo 20-400 kg", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                if (cal == null || cal < 800 || cal > 6000) {
                    Toast.makeText(this, "Hedef kalori 800-6000", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                val approxEq: (Double, Double) -> Boolean = { a, b -> kotlin.math.abs(a - b) < 0.01 }
                val changed = fn != p.firstName?.trim() ||
                    ln != p.lastName?.trim() ||
                    !approxEq(h, p.height) ||
                    !approxEq(w, p.weight) ||
                    cal != p.targetCalories
                if (!changed) {
                    Toast.makeText(this, "Degisiklik yok", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                AlertDialog.Builder(this@ClientProfileActivity)
                    .setMessage("Bilgileri bu sekilde kaydetmek istediginize emin misiniz?")
                    .setNegativeButton("Hayir", null)
                    .setPositiveButton("Evet") { _, _ ->
                        dlg.dismiss()
                        lifecycleScope.launch {
                            try {
                                val r = RetrofitClient.instance.updateClientProfile(
                                    UpdateClientProfileRequest(
                                        firstName = fn,
                                        lastName = ln,
                                        weight = w,
                                        height = h,
                                        targetCalories = cal
                                    )
                                )
                                if (r.isSuccessful) {
                                    Toast.makeText(
                                        this@ClientProfileActivity,
                                        "Profil guncellendi",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    loadProfile(findViewById(R.id.btnTheme))
                                } else {
                                    val body = r.errorBody()?.string().orEmpty()
                                    val msg = when (r.code()) {
                                        401 -> "Oturum suresi doldu; tekrar giris yapin."
                                        404 -> "Endpoint yok: API guncel mi? (POST api/Client/profile)"
                                        405 -> "Istek yontemi: API'yi yeniden baslatin."
                                        else -> body.ifBlank { "Kayit basarisiz (HTTP ${r.code()})" }
                                    }
                                    Toast.makeText(this@ClientProfileActivity, msg, Toast.LENGTH_LONG).show()
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
        dlg.show()
    }

    private fun setupDietitianConnect() {
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

    private fun loadProfile(btnTheme: TextView) {
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.getClientProfile()
                if (r.isSuccessful) {
                    val p = r.body() ?: return@launch
                    lastProfile = p
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
                    currentTheme = ThemeUtils.fromProfile(p.themePreference)
                    ThemeUtils.applyAndPersist(getSharedPreferences(ThemeUtils.PREF_NAME, MODE_PRIVATE), currentTheme)
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

    private fun toggleTheme() {
        val next = if (currentTheme == "dark") "light" else "dark"
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.updateAuthTheme(
                    UpdateThemeRequest(themePreference = next)
                )
                if (r.isSuccessful) {
                    currentTheme = next
                    ThemeUtils.applyAndPersist(
                        getSharedPreferences(ThemeUtils.PREF_NAME, MODE_PRIVATE),
                        next
                    )
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
