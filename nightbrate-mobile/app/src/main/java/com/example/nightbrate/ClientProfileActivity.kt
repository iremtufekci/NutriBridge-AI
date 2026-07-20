package com.example.nightbrate // Paket tanımı

import android.content.Context // Sistem servisleri
import android.graphics.Typeface // Kalın yazı tipi
import android.os.Bundle // Activity durum paketi
import android.text.SpannableString // Biçimlendirilmiş metin
import android.text.Spanned // Span bayrakları
import android.text.style.StyleSpan // Kalın span
import android.view.View // Görünüm temel sınıfı
import android.view.WindowManager // Klavye pencere modu
import android.view.inputmethod.InputMethodManager // Klavye yönetimi
import android.widget.EditText // Metin girişi
import android.widget.ScrollView // Kaydırılabilir diyalog içeriği
import android.widget.TextView // Metin görünümü
import android.widget.Toast // Kısa bildirim
import androidx.appcompat.app.AlertDialog // Onay/düzenleme diyaloğu
import androidx.appcompat.app.AppCompatActivity // Temel Activity
import androidx.lifecycle.lifecycleScope // Yaşam döngüsü coroutine
import com.example.nightbrate.ActivityWindowHelper.applyStandardContentWindow // Standart pencere
import kotlinx.coroutines.launch // Coroutine başlat
import java.text.SimpleDateFormat // Tarih biçimlendirme
import java.util.Locale // Yerel ayar
import java.util.TimeZone // Saat dilimi

class ClientProfileActivity : AppCompatActivity() { // Danışan profil ekranı
    private var pendingDietCode: String? = null // Onay bekleyen diyetisyen kodu
    private var lastProfile: ClientProfileResponse? = null // Son yüklenen profil

    override fun onCreate(savedInstanceState: Bundle?) { // Activity oluşturulduğunda
        super.onCreate(savedInstanceState) // Üst sınıf onCreate
        applyStandardContentWindow() // Standart içerik penceresi
        setContentView(R.layout.activity_client_profile) // Profil layout'u
        ClientBottomBarHelper.bind(this, 7) // Alt çubuk: Profil sekmesi
        val btnPrivacy = findViewById<View>(R.id.btnPrivacy) // Gizlilik butonu
        val btnAbout = findViewById<View>(R.id.btnAbout) // Hakkında butonu
        val btnEdit = findViewById<View>(R.id.btnEditProfile) // Düzenle butonu

        btnPrivacy.setOnClickListener { // Gizlilik politikası tıklama
            showLongTextDialog(
                "Gizlilik politikası",
                getString(R.string.client_privacy_policy_text)
            )
        }
        btnAbout.setOnClickListener { // Hakkında tıklama
            showLongTextDialog("Hakkında", getString(R.string.client_about_text))
        }
        btnEdit.setOnClickListener { showEditProfileDialog() } // Profil düzenleme diyaloğu
        setupDietitianConnect() // Diyetisyen eşleştirme UI
        loadProfile() // Profili API'den yükle
    }

    private fun showLongTextDialog(title: String, text: String) { // Uzun metin diyaloğu göster
        val scroll = ScrollView(this) // Kaydırılabilir konteyner
        val tv = TextView(this).apply {
            setPadding(48, 32, 48, 32) // İç boşluk
            textSize = 14f // Yazı boyutu
            setTextIsSelectable(true) // Metin seçilebilir
            this.text = text // İçerik
        }
        scroll.addView(tv) // ScrollView'a ekle
        val pad = (resources.displayMetrics.density * 8).toInt() // Dış padding
        scroll.setPadding(pad, 0, pad, 0)
        AlertDialog.Builder(this)
            .setTitle(title) // Diyalog başlığı
            .setView(scroll) // İçerik görünümü
            .setPositiveButton("Kapat", null) // Kapat butonu
            .show()
    }

    private fun showEditProfileDialog() { // Profil düzenleme diyaloğu
        val p = lastProfile // Mevcut profil
        if (p == null) { // Profil henüz yüklenmedi
            Toast.makeText(this, "Önce profil yüklensin", Toast.LENGTH_SHORT).show()
            return
        }
        val view = layoutInflater.inflate(R.layout.dialog_client_edit_profile, null, false) // Diyalog layout
        val etF = view.findViewById<EditText>(R.id.dlgEtFirst) // Ad alanı
        val etL = view.findViewById<EditText>(R.id.dlgEtLast) // Soyad alanı
        val etH = view.findViewById<EditText>(R.id.dlgEtHeight) // Boy alanı
        val etW = view.findViewById<EditText>(R.id.dlgEtWeight) // Kilo alanı
        val etC = view.findViewById<EditText>(R.id.dlgEtCal) // Hedef kalori alanı
        val tvGoalHint = view.findViewById<TextView>(R.id.tvDlgGoalHint) // Hedef ipucu
        val btnP1600 = view.findViewById<TextView>(R.id.btnDlgPreset1600) // 1600 preset
        val btnP2000 = view.findViewById<TextView>(R.id.btnDlgPreset2000) // 2000 preset
        val btnP2500 = view.findViewById<TextView>(R.id.btnDlgPreset2500) // 2500 preset
        etF.setText(p.firstName.orEmpty().trim()) // Mevcut ad
        etL.setText(p.lastName.orEmpty().trim()) // Mevcut soyad
        if (p.height > 0) etH.setText(String.format(Locale.ROOT, "%.1f", p.height).trimEnd('0').trimEnd('.')) // Boy
        if (p.weight > 0) etW.setText(String.format(Locale.ROOT, "%.1f", p.weight).trimEnd('0').trimEnd('.')) // Kilo
        etC.setText(p.targetCalories.toString()) // Hedef kalori

        fun refreshGoalHint() { // Kalori hedefine göre ipucu güncelle
            val cal = etC.text.toString().toIntOrNull() ?: p.targetCalories // Girilen veya mevcut kalori
            tvGoalHint.text =
                "Öneri: Hedef, girilen kaloriye göre gösterilir: ${resolveGoalLabelFromCalories(cal)}"
        }
        etC.addTextChangedListener(object : android.text.TextWatcher { // Kalori değişim dinleyicisi
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                refreshGoalHint() // İpucu yenile
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        btnP1600.setOnClickListener { // 1600 kkal preset
            etC.setText("1600")
            refreshGoalHint()
        }
        btnP2000.setOnClickListener { // 2000 kkal preset
            etC.setText("2000")
            refreshGoalHint()
        }
        btnP2500.setOnClickListener { // 2500 kkal preset
            etC.setText("2500")
            refreshGoalHint()
        }
        val dlg = AlertDialog.Builder(this)
            .setTitle("Kişisel bilgileri düzenle")
            .setView(view)
            .setNegativeButton("Iptal", null)
            .setPositiveButton("Kaydet", null) // Özel tıklama aşağıda
            .create()
        dlg.setOnShowListener { // Diyalog gösterildiğinde
            dlg.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE) // Klavye ayarı
            refreshGoalHint() // İlk ipucu
            etF.post { // Odak ve klavye
                etF.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showSoftInput(etF, InputMethodManager.SHOW_IMPLICIT)
            }
            dlg.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener { // Kaydet tıklama
                val fn = etF.text.toString().trim() // Ad
                val ln = etL.text.toString().trim() // Soyad
                if (fn.isEmpty() || ln.isEmpty()) { // Zorunlu alan kontrolü
                    Toast.makeText(this, "Ad ve soyad gerekli", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                val h = etH.text.toString().replace(',', '.').toDoubleOrNull() // Boy parse
                val w = etW.text.toString().replace(',', '.').toDoubleOrNull() // Kilo parse
                val cal = etC.text.toString().toIntOrNull() // Kalori parse
                if (h == null || h < 50.0 || h > 250.0) { // Boy aralığı
                    Toast.makeText(this, "Boy 50-250 cm", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                if (w == null || w < 20.0 || w > 400.0) { // Kilo aralığı
                    Toast.makeText(this, "Kilo 20-400 kg", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                if (cal == null || cal < 800 || cal > 6000) { // Kalori aralığı
                    Toast.makeText(this, "Hedef kalori 800-6000", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                val approxEq: (Double, Double) -> Boolean = { a, b -> kotlin.math.abs(a - b) < 0.01 } // Yaklaşık eşitlik
                val changed = fn != p.firstName?.trim() || // Değişiklik var mı
                    ln != p.lastName?.trim() ||
                    !approxEq(h, p.height) ||
                    !approxEq(w, p.weight) ||
                    cal != p.targetCalories
                if (!changed) { // Değişiklik yok
                    Toast.makeText(this, "Değişiklik yok", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                AlertDialog.Builder(this@ClientProfileActivity) // Onay diyaloğu
                    .setMessage("Bilgileri bu şekilde kaydetmek istediğinize emin misiniz?")
                    .setNegativeButton("Hayır", null)
                    .setPositiveButton("Evet") { _, _ -> // Onaylandı
                        dlg.dismiss() // Düzenleme diyaloğunu kapat
                        lifecycleScope.launch { // Profil güncelleme API
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
                                if (r.isSuccessful) { // Başarılı
                                    Toast.makeText(
                                        this@ClientProfileActivity,
                                        "Profil güncellendi",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    loadProfile() // Profili yenile
                                } else { // HTTP hatası
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
        dlg.show() // Diyalogu göster
    }

    private fun setupDietitianConnect() { // Diyetisyen kodu ile eşleştirme UI
        val et = findViewById<EditText>(R.id.etDietCode) // Kod girişi
        val btnVerify = findViewById<TextView>(R.id.btnVerifyDietCode) // Doğrula butonu
        val btnConfirm = findViewById<TextView>(R.id.btnConfirmDietCode) // Onayla butonu
        val tvPreview = findViewById<TextView>(R.id.tvDietCodePreview) // Önizleme metni

        fun resetPreview() { // Önizleme alanını sıfırla
            tvPreview.visibility = View.GONE
            btnConfirm.visibility = View.GONE
            tvPreview.text = ""
            pendingDietCode = null
        }

        btnVerify.setOnClickListener { // Kod doğrulama tıklama
            val code = et.text.toString().trim().uppercase(Locale.ROOT) // 6 haneli kod
            if (code.length != 6) { // Uzunluk kontrolü
                Toast.makeText(this, "6 hane girin (harf ve rakam)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            resetPreview() // Önceki önizlemeyi temizle
            lifecycleScope.launch { // Önizleme API
                try {
                    val r = RetrofitClient.instance.previewDietitianByCode(ConnectToDietitianRequest(code))
                    if (r.isSuccessful) { // Diyetisyen bulundu
                        val b = r.body()
                        val name = b?.displayName ?: "Dr. ${b?.firstName} ${b?.lastName}"
                        tvPreview.text = "Bulundu: $name"
                        tvPreview.visibility = View.VISIBLE
                        btnConfirm.visibility = View.VISIBLE
                        pendingDietCode = code // Onay bekleyen kod
                    } else { // Geçersiz kod
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

        btnConfirm.setOnClickListener { // Eşleştirmeyi onayla
            val code = pendingDietCode // Bekleyen kod
            if (code.isNullOrBlank()) return@setOnClickListener // Kod yok
            val display = tvPreview.text.toString().removePrefix("Bulundu: ").trim() // Diyetisyen adı
            AlertDialog.Builder(this@ClientProfileActivity)
                .setTitle("Eşleştirme onayı")
                .setMessage("Veritabanına kayıt edilecektir.\n\n$display\n\nOnaylıyor musunuz?")
                .setNegativeButton("Hayır", null)
                .setPositiveButton("Evet") { _, _ -> // Onaylandı
                    lifecycleScope.launch { // Bağlan API
                        try {
                            val r = RetrofitClient.instance.connectToDietitian(ConnectToDietitianRequest(code))
                            if (r.isSuccessful) { // Eşleşme başarılı
                                val msg = r.body()?.message ?: "Eşleşti"
                                Toast.makeText(this@ClientProfileActivity, msg, Toast.LENGTH_LONG).show()
                                et.setText("") // Kod alanını temizle
                                resetPreview()
                                loadProfile() // Profili yenile
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

    private fun loadProfile() { // API'den profil yükle ve UI'ya bağla
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.getClientProfile() // Profil API
                if (r.isSuccessful) {
                    val p = r.body() ?: return@launch // Gövde yok
                    lastProfile = p // Profili sakla
                    val cardConnect = findViewById<View>(R.id.cardConnectDietitian) // Eşleştirme kartı
                    val dName = p.dietitianName?.trim().orEmpty() // Diyetisyen adı
                    cardConnect.visibility =
                        if (dName.isEmpty() || dName == "Atanmadi" || dName == "Atanmadı") View.VISIBLE else View.GONE // Atanmamışsa göster
                    val first = p.firstName?.trim().orEmpty() // Ad
                    val last = p.lastName?.trim().orEmpty() // Soyad
                    val name = listOf(first, last).filter { it.isNotEmpty() }.joinToString(" ")
                        .ifEmpty { "Danışan" } // Görünen ad
                    findViewById<TextView>(R.id.tvDisplayName).text = name // Ad soyad
                    findViewById<TextView>(R.id.tvDietitian).text =
                        "Diyetisyen: ${p.dietitianName ?: "Atanmadı"}" // Diyetisyen satırı
                    findViewById<TextView>(R.id.tvHeight).text =
                        if (p.height > 0) "${p.height.toInt()} cm" else "-" // Boy
                    findViewById<TextView>(R.id.tvWeight).text =
                        if (p.weight > 0) {
                            val s = String.format(Locale.ROOT, "%.1f", p.weight).trimEnd('0').trimEnd('.')
                            "$s kg"
                        } else "-" // Kilo
                    findViewById<TextView>(R.id.tvGoal).text = p.goalText ?: "—" // Hedef metni
                    val pStart = p.programStartDate?.let { formatProgramDate(it) } ?: "—" // Program başlangıcı
                    val prefix = "Program başlangıcı: "
                    val progText = prefix + pStart
                    val ss = SpannableString(progText) // Kalın tarih span'ı
                    val startBold = prefix.length
                    if (startBold < progText.length) {
                        ss.setSpan(
                            StyleSpan(Typeface.BOLD),
                            startBold,
                            progText.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                    findViewById<TextView>(R.id.tvProgramStart).text = ss // Program tarihi
                    val a = (first.take(1) + last.take(1)).uppercase(Locale.ROOT) // Avatar baş harfleri
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

    private fun formatProgramDate(iso: String): String { // ISO tarihi Türkçe formata çevir
        val patterns = listOf( // Denenecek ISO formatları
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (p in patterns) {
            try {
                val sdf = SimpleDateFormat(p, Locale.ROOT)
                sdf.timeZone = TimeZone.getTimeZone("UTC") // UTC parse
                val d = sdf.parse(iso) ?: continue
                val out = SimpleDateFormat("d MMMM yyyy", Locale("tr", "TR")) // Türkçe çıktı
                out.timeZone = TimeZone.getDefault()
                return out.format(d)
            } catch (_: Exception) { }
        }
        return iso // Parse edilemezse ham string
    }

    private fun resolveGoalLabelFromCalories(targetCalories: Int): String = // Kaloriye göre hedef etiketi
        when {
            targetCalories <= 1800 -> "Kilo Ver"
            targetCalories >= 2300 -> "Kilo Al"
            else -> "Formu Koru"
        }
}
