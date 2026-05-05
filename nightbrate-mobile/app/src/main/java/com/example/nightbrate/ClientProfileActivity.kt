package com.example.nightbrate

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.nightbrate.ActivityWindowHelper.applyStandardContentWindow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ClientProfileActivity : AppCompatActivity() {
    private var pendingDietCode: String? = null
    private var lastProfile: ClientProfileResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyStandardContentWindow()
        setContentView(R.layout.activity_client_profile)
        ClientBottomBarHelper.bind(this, 7)
        val btnPrivacy = findViewById<View>(R.id.btnPrivacy)
        val btnAbout = findViewById<View>(R.id.btnAbout)
        val btnEdit = findViewById<View>(R.id.btnEditProfile)

        btnPrivacy.setOnClickListener {
            showLongTextDialog(
                "Gizlilik politikası",
                getString(R.string.client_privacy_policy_text)
            )
        }
        btnAbout.setOnClickListener {
            showLongTextDialog("Hakkında", getString(R.string.client_about_text))
        }
        btnEdit.setOnClickListener { showEditProfileDialog() }
        setupDietitianConnect()
        loadProfile()
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
            Toast.makeText(this, "Önce profil yüklensin", Toast.LENGTH_SHORT).show()
            return
        }
        val view = layoutInflater.inflate(R.layout.dialog_client_edit_profile, null, false)
        val etF = view.findViewById<EditText>(R.id.dlgEtFirst)
        val etL = view.findViewById<EditText>(R.id.dlgEtLast)
        val etH = view.findViewById<EditText>(R.id.dlgEtHeight)
        val etW = view.findViewById<EditText>(R.id.dlgEtWeight)
        val etC = view.findViewById<EditText>(R.id.dlgEtCal)
        val tvGoalHint = view.findViewById<TextView>(R.id.tvDlgGoalHint)
        val btnP1600 = view.findViewById<TextView>(R.id.btnDlgPreset1600)
        val btnP2000 = view.findViewById<TextView>(R.id.btnDlgPreset2000)
        val btnP2500 = view.findViewById<TextView>(R.id.btnDlgPreset2500)
        etF.setText(p.firstName.orEmpty().trim())
        etL.setText(p.lastName.orEmpty().trim())
        if (p.height > 0) etH.setText(String.format(Locale.ROOT, "%.1f", p.height).trimEnd('0').trimEnd('.'))
        if (p.weight > 0) etW.setText(String.format(Locale.ROOT, "%.1f", p.weight).trimEnd('0').trimEnd('.'))
        etC.setText(p.targetCalories.toString())

        fun refreshGoalHint() {
            val cal = etC.text.toString().toIntOrNull() ?: p.targetCalories
            tvGoalHint.text =
                "Öneri: Hedef, girilen kaloriye göre gösterilir: ${resolveGoalLabelFromCalories(cal)}"
        }
        etC.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                refreshGoalHint()
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        btnP1600.setOnClickListener {
            etC.setText("1600")
            refreshGoalHint()
        }
        btnP2000.setOnClickListener {
            etC.setText("2000")
            refreshGoalHint()
        }
        btnP2500.setOnClickListener {
            etC.setText("2500")
            refreshGoalHint()
        }
        val dlg = AlertDialog.Builder(this)
            .setTitle("Kişisel bilgileri düzenle")
            .setView(view)
            .setNegativeButton("Iptal", null)
            .setPositiveButton("Kaydet", null)
            .create()
        dlg.setOnShowListener {
            dlg.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            refreshGoalHint()
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
                    Toast.makeText(this, "Değişiklik yok", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                AlertDialog.Builder(this@ClientProfileActivity)
                    .setMessage("Bilgileri bu şekilde kaydetmek istediğinize emin misiniz?")
                    .setNegativeButton("Hayır", null)
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
                                        "Profil güncellendi",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    loadProfile()
                                } else {
                                    val body = r.errorBody()?.string().orEmpty()
                                    val msg = when (r.code()) {
                                        401 -> "Oturum süresi doldu; tekrar giriş yapın."
                                        404 -> "Endpoint yok: API güncel mi? (POST api/Client/profile)"
                                        405 -> "İstek yöntemi: API'yi yeniden başlatın."
                                        else -> body.ifBlank { "Kayıt başarısız (HTTP ${r.code()})" }
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
                            "Kod geçerli değil (onaylı diyetisyen gerekir)",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this@ClientProfileActivity,
                        e.message ?: "Bağlantı hatası",
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
                                val msg = r.body()?.message ?: "Eşleşti"
                                Toast.makeText(this@ClientProfileActivity, msg, Toast.LENGTH_LONG).show()
                                et.setText("")
                                resetPreview()
                                loadProfile()
                            } else {
                                Toast.makeText(
                                    this@ClientProfileActivity,
                                    "Eşleştirme başarısız (${r.code()})",
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

    private fun loadProfile() {
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.getClientProfile()
                if (r.isSuccessful) {
                    val p = r.body() ?: return@launch
                    lastProfile = p
                    val cardConnect = findViewById<View>(R.id.cardConnectDietitian)
                    val dName = p.dietitianName?.trim().orEmpty()
                    cardConnect.visibility =
                        if (dName.isEmpty() || dName == "Atanmadi" || dName == "Atanmadı") View.VISIBLE else View.GONE
                    val first = p.firstName?.trim().orEmpty()
                    val last = p.lastName?.trim().orEmpty()
                    val name = listOf(first, last).filter { it.isNotEmpty() }.joinToString(" ")
                        .ifEmpty { "Danışan" }
                    findViewById<TextView>(R.id.tvDisplayName).text = name
                    findViewById<TextView>(R.id.tvDietitian).text =
                        "Diyetisyen: ${p.dietitianName ?: "Atanmadı"}"
                    findViewById<TextView>(R.id.tvHeight).text =
                        if (p.height > 0) "${p.height.toInt()} cm" else "-"
                    findViewById<TextView>(R.id.tvWeight).text =
                        if (p.weight > 0) {
                            val s = String.format(Locale.ROOT, "%.1f", p.weight).trimEnd('0').trimEnd('.')
                            "$s kg"
                        } else "-"
                    findViewById<TextView>(R.id.tvGoal).text = p.goalText ?: "—"
                    val pStart = p.programStartDate?.let { formatProgramDate(it) } ?: "—"
                    val prefix = "Program başlangıcı: "
                    val progText = prefix + pStart
                    val ss = SpannableString(progText)
                    val startBold = prefix.length
                    if (startBold < progText.length) {
                        ss.setSpan(
                            StyleSpan(Typeface.BOLD),
                            startBold,
                            progText.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                    findViewById<TextView>(R.id.tvProgramStart).text = ss
                    val a = (first.take(1) + last.take(1)).uppercase(Locale.ROOT)
                    findViewById<TextView>(R.id.avatarText).text = if (a.isNotBlank()) a else "D"
                } else {
                    Toast.makeText(
                        this@ClientProfileActivity,
                        "Profil yüklenemedi",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ClientProfileActivity,
                    "Bağlantı hatası: ${e.message}",
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

    private fun resolveGoalLabelFromCalories(targetCalories: Int): String =
        when {
            targetCalories <= 1800 -> "Kilo Ver"
            targetCalories >= 2300 -> "Kilo Al"
            else -> "Formu Koru"
        }
}
