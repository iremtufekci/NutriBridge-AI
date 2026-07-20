package com.example.nightbrate // Paket tanımı

import android.content.res.ColorStateList // Düğme renk durumu
import android.os.Bundle // Activity durum paketi
import android.os.Handler // Ana iş parçacığı gecikmesi
import android.os.Looper // Ana döngü
import android.text.Editable // Düzenlenebilir metin
import android.text.TextWatcher // Metin değişim dinleyicisi
import android.view.View // Görünüm temel sınıfı
import android.view.ViewGroup // Görünüm grubu
import android.widget.AdapterView // Spinner seçim dinleyicisi
import android.widget.ArrayAdapter // Spinner veri adaptörü
import android.widget.LinearLayout // Dikey liste konteyneri
import android.widget.ProgressBar // Yükleme göstergesi
import android.widget.Spinner // Açılır filtre seçici
import android.widget.TextView // Metin görünümü
import android.widget.Toast // Kısa bildirim
import androidx.appcompat.app.AlertDialog // Uyarı diyalogu
import androidx.appcompat.app.AppCompatActivity // Temel Activity
import androidx.core.content.ContextCompat // Kaynak renk çözümleme
import androidx.core.text.HtmlCompat // HTML metin ayrıştırma
import androidx.lifecycle.lifecycleScope // Coroutine kapsamı
import com.google.android.material.button.MaterialButton // Material düğme
import com.google.android.material.dialog.MaterialAlertDialogBuilder // Material diyalog
import com.google.android.material.textfield.TextInputEditText // Metin giriş alanı
import kotlinx.coroutines.CancellationException // İptal edilen coroutine
import kotlinx.coroutines.Job // Coroutine işi
import kotlinx.coroutines.launch // Coroutine başlatma
import org.json.JSONObject // JSON ayrıştırma
import retrofit2.Response // HTTP yanıt sarmalayıcısı
import java.text.SimpleDateFormat // Tarih biçimlendirme
import java.util.Date // Tarih nesnesi
import java.util.Locale // Yerel ayar
import java.util.TimeZone // Saat dilimi

class AdminUserManagementActivity : AppCompatActivity() { // Kullanıcı yönetimi ekranı

    private lateinit var list: LinearLayout // Kullanıcı listesi konteyneri
    private lateinit var progress: ProgressBar // Yükleme çubuğu
    private lateinit var err: TextView // Hata metni
    private lateinit var tvEmpty: TextView // Boş liste metni
    private lateinit var tvCount: TextView // Bulunan kullanıcı sayısı
    private lateinit var etSearch: TextInputEditText // Arama kutusu
    private lateinit var btnFilters: MaterialButton // Filtre paneli düğmesi
    private lateinit var panelFilters: View // Filtre paneli
    private lateinit var spinnerRole: Spinner // Rol filtresi
    private lateinit var spinnerStatus: Spinner // Durum filtresi

    private val debounceHandler = Handler(Looper.getMainLooper()) // Arama gecikme işleyicisi
    private var debounceRunnable: Runnable? = null // Ertelenmiş arama görevi

    private var suppressSpinner = true // Spinner ilk seçimde tetiklenmesin
    private var filtersVisible = false // Filtre paneli açık mı

    private val roleValues by lazy { resources.getStringArray(R.array.um_role_filter_values) } // Rol API değerleri
    private val statusValues by lazy { resources.getStringArray(R.array.um_status_filter_values) } // Durum API değerleri
    private var usersLoadJob: Job? = null // Devam eden kullanıcı yükleme işi

    override fun onCreate(savedInstanceState: Bundle?) { // Activity oluşturulduğunda
        super.onCreate(savedInstanceState) // Üst sınıf başlatma
        setContentView(R.layout.activity_admin_user_management) // Yönetim düzeni
        AdminBottomBarHelper.bind(this, 1) // Alt sekme (kullanıcılar)

        list = findViewById(R.id.umgList) // Liste referansı
        progress = findViewById(R.id.umgProgress) // İlerleme referansı
        err = findViewById(R.id.tvUmgError) // Hata referansı
        tvEmpty = findViewById(R.id.tvUmgEmpty) // Boş metin referansı
        tvCount = findViewById(R.id.tvUmgCount) // Sayı referansı
        etSearch = findViewById(R.id.etUmgSearch) // Arama referansı
        btnFilters = findViewById(R.id.btnUmgFilters) // Filtre düğmesi referansı
        panelFilters = findViewById(R.id.panelUmgFilters) // Panel referansı
        spinnerRole = findViewById(R.id.spinnerUmgRole) // Rol spinner referansı
        spinnerStatus = findViewById(R.id.spinnerUmgStatus) // Durum spinner referansı

        setupSpinners() // Filtre spinner'larını kur
        setupSearchDebounce() // Arama gecikmesini kur
        btnFilters.setOnClickListener { toggleFilters() } // Filtre paneli aç/kapa
        updateFilterButtonStyle() // Düğme stilini güncelle

        refreshAll() // İstatistik ve listeyi yükle
    }

    private fun setupSearchDebounce() { // Arama için 350ms gecikme
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {} // Değişim öncesi
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {} // Değişim sırasında
            override fun afterTextChanged(s: Editable?) { // Metin değiştiğinde
                debounceRunnable?.let { debounceHandler.removeCallbacks(it) } // Önceki gecikmeyi iptal
                debounceRunnable = Runnable { refreshUsers() } // Yeni arama görevi
                debounceHandler.postDelayed(debounceRunnable!!, 350) // 350ms sonra çalıştır
            }
        })
    }

    private fun setupSpinners() { // Rol ve durum filtre spinner'ları
        ArrayAdapter.createFromResource(
            this,
            R.array.um_role_filter_labels,
            android.R.layout.simple_spinner_dropdown_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) // Açılır liste düzeni
            spinnerRole.adapter = adapter // Role bağla
        }
        ArrayAdapter.createFromResource(
            this,
            R.array.um_status_filter_labels,
            android.R.layout.simple_spinner_dropdown_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) // Açılır liste düzeni
            spinnerStatus.adapter = adapter // Duruma bağla
        }

        val sel = object : AdapterView.OnItemSelectedListener { // Seçim değişince listeyi yenile
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (suppressSpinner) return // İlk kurulumda atla
                refreshUsers() // Filtreyle yenile
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {} // Seçim yok
        }
        spinnerRole.onItemSelectedListener = sel // Rol dinleyicisi
        spinnerStatus.onItemSelectedListener = sel // Durum dinleyicisi
        suppressSpinner = false // Artık seçimler tetiklenebilir
    }

    private fun toggleFilters() { // Filtre panelini göster/gizle
        filtersVisible = !filtersVisible // Durumu ters çevir
        panelFilters.visibility = if (filtersVisible) View.VISIBLE else View.GONE // Görünürlük
        updateFilterButtonStyle() // Düğme rengini güncelle
    }

    private fun updateFilterButtonStyle() { // Filtre düğmesi aktif/pasif stili
        val green = ContextCompat.getColor(this, R.color.brand) // Aktif arka plan
        val white = ContextCompat.getColor(this, R.color.white) // Aktif metin
        val inactiveBg = ContextCompat.getColor(this, R.color.um_filter_inactive_bg) // Pasif arka plan
        val onInactive = ContextCompat.getColor(this, R.color.um_filter_inactive_text) // Pasif metin
        if (filtersVisible) { // Panel açık
            btnFilters.backgroundTintList = ColorStateList.valueOf(green) // Yeşil arka plan
            btnFilters.setTextColor(white) // Beyaz metin
        } else { // Panel kapalı
            btnFilters.backgroundTintList = ColorStateList.valueOf(inactiveBg) // Gri arka plan
            btnFilters.setTextColor(onInactive) // Soluk metin
        }
    }

    private fun refreshAll() { // İstatistik ve kullanıcı listesini yenile
        lifecycleScope.launch {
            refreshStats() // Özet kartları
            refreshUsers() // Kullanıcı kartları
        }
    }

    private suspend fun refreshStats() { // Üst istatistik kartlarını güncelle
        try {
            val statsR = RetrofitClient.instance.getUserManagementStats() // İstatistik API
            if (statsR.isSuccessful) { // Başarılı yanıt
                bindAllStats(statsR.body()) // Kartlara yaz
            }
        } catch (_: Exception) { } // Sessizce yoksay
    }

    private fun bindAllStats(s: UserManagementStatsResponse?) { // Tüm özet hücrelerini doldur
        val strong = ContextCompat.getColor(this, R.color.admin_strong) // Koyu metin rengi
        val amber = ContextCompat.getColor(this, R.color.um_stat_amber) // Amber vurgu
        val emerald = ContextCompat.getColor(this, R.color.um_stat_emerald) // Yeşil vurgu
        bindStatCell(R.id.cellStatTotal, "Toplam kullanıcı", s?.totalUsers?.toString(), strong) // Toplam
        bindStatCell(R.id.cellStatAdmin, "Admin", s?.admins?.toString(), amber) // Admin sayısı
        bindStatCell(R.id.cellStatDietitian, "Diyetisyen", s?.dietitians?.toString(), emerald) // Diyetisyen
        bindStatCell(R.id.cellStatClient, "Danışan", s?.clients?.toString(), emerald) // Danışan
        bindStatCell(R.id.cellStatActive, "Aktif", s?.active?.toString(), emerald) // Aktif
        bindStatCell(R.id.cellStatPending, "Bekleyen", s?.pending?.toString(), amber) // Bekleyen
    }

    private fun bindStatCell(cellId: Int, label: String, value: String?, valueColor: Int) { // Tek özet hücresi
        val root = findViewById<View>(cellId) // Hücre kök görünümü
        root.findViewById<TextView>(R.id.umStatLabel).text = label // Etiket metni
        val tv = root.findViewById<TextView>(R.id.umStatValue) // Değer görünümü
        tv.text = value ?: "—" // Değer veya tire
        tv.setTextColor(valueColor) // Renk uygula
    }

    private fun refreshUsers() { // Filtreli kullanıcı listesini yükle
        usersLoadJob?.cancel() // Önceki isteği iptal et
        usersLoadJob = lifecycleScope.launch { // Yeni yükleme işi
            progress.visibility = View.VISIBLE // Yükleniyor göster
            err.visibility = View.GONE // Hatayı gizle
            tvEmpty.visibility = View.GONE // Boş metni gizle
            try {
                val q = etSearch.text?.toString()?.trim().orEmpty() // Arama metni
                val rolePos = spinnerRole.selectedItemPosition.coerceAtLeast(0) // Rol seçimi
                val statusPos = spinnerStatus.selectedItemPosition.coerceAtLeast(0) // Durum seçimi
                val role = roleValues.getOrNull(rolePos)?.takeIf { it != "all" } // Rol filtresi (all hariç)
                val status = statusValues.getOrNull(statusPos)?.takeIf { it != "all" } // Durum filtresi

                val uR = RetrofitClient.instance.getUserManagementUsers(
                    q.ifEmpty { null }, // Boş arama null gönder
                    role,
                    status
                ) // Kullanıcı listesi API
                progress.visibility = View.GONE // Yükleme bitti
                if (!uR.isSuccessful) { // HTTP hatası
                    err.text = readErrorMessage(uR) // Hata mesajı
                    err.visibility = View.VISIBLE // Göster
                    list.removeAllViews() // Listeyi temizle
                    tvCount.text = "" // Sayıyı sıfırla
                    return@launch
                }
                val body = uR.body() ?: emptyList() // Yanıt gövdesi
                list.removeAllViews() // Eski kartları temizle
                tvCount.text = if (body.isEmpty()) {
                    "0 kullanıcı bulundu" // Boş sonuç
                } else {
                    "${body.size} kullanıcı bulundu" // Bulunan sayı
                }
                if (body.isEmpty()) { // Sonuç yok
                    tvEmpty.visibility = View.VISIBLE // Boş mesajı göster
                }
                for (item in body) { // Her kullanıcı için kart
                    addCard(item) // Kart ekle
                }
            } catch (e: CancellationException) { // İptal edilen istek
                throw e // Coroutine iptalini yay
            } catch (e: Exception) { // Ağ veya beklenmeyen hata
                progress.visibility = View.GONE // Yüklemeyi kapat
                err.text = e.message ?: "Bağlantı hatası" // Hata metni
                err.visibility = View.VISIBLE // Göster
                list.removeAllViews() // Listeyi temizle
                tvCount.text = "" // Sayıyı sıfırla
            }
        }
    }

    private fun readErrorMessage(response: Response<*>): String { // API hata gövdesini oku
        val raw = response.errorBody()?.string().orEmpty() // Ham hata metni
        return try {
            JSONObject(raw).optString("message").ifBlank { "HTTP ${response.code()}" } // JSON mesajı
        } catch (_: Exception) {
            if (raw.isNotBlank()) raw else "HTTP ${response.code()}" // Yedek metin
        }
    }

    private fun addCard(u: AdminUserRowItem) { // Tek kullanıcı kartı oluştur
        val v = layoutInflater.inflate(R.layout.item_admin_user_row, list, false) // Kart şablonu
        val initial = (u.initial?.take(1) ?: u.displayName?.take(1) ?: "?").uppercase() // Avatar harfi
        v.findViewById<TextView>(R.id.auAvatar).text = initial // Harfi yaz
        v.findViewById<TextView>(R.id.auName).text = u.displayName.orEmpty().ifBlank { "—" } // Görünen ad
        v.findViewById<TextView>(R.id.auEmail).text = buildString {
            append("✉ ") // E-posta simgesi
            append(u.email.orEmpty().ifBlank { "—" }) // E-posta adresi
        }
        val phoneTv = v.findViewById<TextView>(R.id.auPhone) // Telefon alanı
        val phone = u.phone?.trim().orEmpty() // Telefon metni
        if (phone.isNotEmpty() && phone != "—") { // Geçerli telefon var
            phoneTv.visibility = View.VISIBLE // Göster
            phoneTv.text = "☎ $phone" // Telefon satırı
        } else {
            phoneTv.visibility = View.GONE // Gizle
        }
        applyRoleChip(v.findViewById(R.id.auRole), u.roleKey, u.role) // Rol rozeti
        applyStatusChip(v.findViewById(R.id.auStatus), u.statusKey, u.statusLabel) // Durum rozeti

        v.findViewById<TextView>(R.id.auCreated).text = buildString {
            append("Kayıt: ") // Etiket
            append(formatDateTr(u.createdAt)) // Kayıt tarihi
        }
        v.findViewById<TextView>(R.id.auLastActivity).text = buildString {
            append("Son aktivite: ") // Etiket
            append(formatTimeAgoTr(u.lastActivityAt)) // Göreli zaman
        }

        val id = u.id // Kullanıcı kimliği
        val logBtn = v.findViewById<MaterialButton>(R.id.auLog) // Log düğmesi
        logBtn.setOnClickListener { // Aktivite logu aç
            if (id.isNullOrBlank()) return@setOnClickListener // Kimlik yoksa çık
            showActivityLogDialog(u.displayName.orEmpty(), id) // Log diyalogu
        }

        val blockBtn = v.findViewById<MaterialButton>(R.id.auBlock) // Askı düğmesi
        if (u.isSuspended == true) { // Kullanıcı askıda
            blockBtn.text = "Askıyı kaldır" // Düğme metni
            blockBtn.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.brand)
            ) // Yeşil arka plan
            blockBtn.setTextColor(ContextCompat.getColor(this, R.color.white)) // Beyaz metin
            blockBtn.strokeWidth = 0 // Kenarlık yok
            blockBtn.setOnClickListener { // Askıyı kaldır
                if (id.isNullOrBlank()) return@setOnClickListener // Kimlik yoksa çık
                MaterialAlertDialogBuilder(this)
                    .setMessage("Bu kullanıcının askısını kaldırmak istiyor musunuz?") // Onay mesajı
                    .setNegativeButton("İptal", null) // Vazgeç
                    .setPositiveButton("Evet") { _, _ -> // Onayla
                        lifecycleScope.launch {
                            try {
                                val r = RetrofitClient.instance.unsuspendUser(id) // Askı kaldır API
                                if (r.isSuccessful) { // Başarılı
                                    Toast.makeText(this@AdminUserManagementActivity, "Askı kaldırıldı", Toast.LENGTH_SHORT).show() // Toast
                                    refreshAll() // Listeyi yenile
                                } else { // HTTP hatası
                                    Toast.makeText(
                                        this@AdminUserManagementActivity,
                                        readErrorMessage(r),
                                        Toast.LENGTH_LONG
                                    ).show() // Hata toast
                                }
                            } catch (e: Exception) { // Ağ hatası
                                Toast.makeText(
                                    this@AdminUserManagementActivity,
                                    e.message ?: "Hata",
                                    Toast.LENGTH_LONG
                                ).show() // Hata toast
                            }
                        }
                    }
                    .show() // Diyalogu göster
            }
        } else { // Kullanıcı aktif
            blockBtn.text = "Askıya al" // Düğme metni
            blockBtn.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.um_suspend_btn)
            ) // Kırmızımsı arka plan
            blockBtn.setTextColor(ContextCompat.getColor(this, R.color.white)) // Beyaz metin
            blockBtn.setOnClickListener { // Askıya al
                if (id.isNullOrBlank()) return@setOnClickListener // Kimlik yoksa çık
                showSuspendDialog(u) // Askı diyalogu
            }
        }
        list.addView(v) // Kartı listeye ekle
    }

    private fun showSuspendDialog(u: AdminUserRowItem) { // Kullanıcı askıya alma diyalogu
        val id = u.id ?: return // Kimlik yoksa çık
        val dialogView = layoutInflater.inflate(R.layout.dialog_suspend_user, null) // Diyalog düzeni
        val explain = dialogView.findViewById<TextView>(R.id.tvSuspendExplain) // Açıklama metni
        val raw = getString(
            R.string.um_suspend_explain,
            u.email.orEmpty().ifBlank { u.displayName.orEmpty() }
        ) // HTML açıklama kaynağı
        explain.text = HtmlCompat.fromHtml(raw, HtmlCompat.FROM_HTML_MODE_LEGACY) // HTML'i göster
        val input = dialogView.findViewById<TextInputEditText>(R.id.etSuspendReason) // Gerekçe alanı
        val dlg = MaterialAlertDialogBuilder(this)
            .setTitle("Kullanıcıyı askıya al") // Başlık
            .setView(dialogView) // Özel görünüm
            .setNegativeButton("Vazgeç", null) // İptal
            .setPositiveButton("Askıya al", null) // Onay (manuel bağlanacak)
            .create() // Diyalogu oluştur
        dlg.setOnShowListener { // Gösterildiğinde pozitif düğmeyi bağla
            dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val msg = input.text?.toString()?.trim().orEmpty() // Gerekçe metni
                if (msg.isEmpty()) { // Boş gerekçe
                    Toast.makeText(this, "Mesaj gerekli", Toast.LENGTH_SHORT).show() // Uyarı
                    return@setOnClickListener
                }
                lifecycleScope.launch {
                    try {
                        val r = RetrofitClient.instance.suspendUser(id, SetUserSuspensionRequest(msg)) // Askı API
                        if (r.isSuccessful) { // Başarılı
                            dlg.dismiss() // Diyalogu kapat
                            Toast.makeText(this@AdminUserManagementActivity, "Kullanıcı askıya alındı", Toast.LENGTH_SHORT).show() // Toast
                            refreshAll() // Listeyi yenile
                        } else { // HTTP hatası
                            Toast.makeText(
                                this@AdminUserManagementActivity,
                                readErrorMessage(r),
                                Toast.LENGTH_LONG
                            ).show() // Hata toast
                        }
                    } catch (e: Exception) { // Ağ hatası
                        Toast.makeText(
                            this@AdminUserManagementActivity,
                            e.message ?: "Hata",
                            Toast.LENGTH_LONG
                        ).show() // Hata toast
                    }
                }
            }
        }
        dlg.show() // Diyalogu göster
    }

    private fun showActivityLogDialog(displayName: String, userId: String) { // Kullanıcı aktivite log diyalogu
        val dialogView = layoutInflater.inflate(R.layout.dialog_user_activity_logs, null) // Diyalog düzeni
        val title = dialogView.findViewById<TextView>(R.id.tvLogDialogTitle) // Başlık
        val container = dialogView.findViewById<LinearLayout>(R.id.llUserLogs) // Log listesi
        val prog = dialogView.findViewById<ProgressBar>(R.id.progressUserLogs) // Yükleme çubuğu
        title.text = getString(R.string.um_log_title, displayName.ifBlank { "—" }) // Kullanıcı adlı başlık
        container.removeAllViews() // Önceki satırları temizle

        val dlg = MaterialAlertDialogBuilder(this)
            .setView(dialogView) // Özel görünüm
            .create() // Diyalogu oluştur

        dialogView.findViewById<View>(R.id.btnLogDialogClose).setOnClickListener { dlg.dismiss() } // Kapat düğmesi
        dlg.show() // Diyalogu göster

        lifecycleScope.launch { // Log verisini çek
            prog.visibility = View.VISIBLE // Yükleniyor göster
            try {
                val r = RetrofitClient.instance.getUserActivityLogs(userId, 40) // Son 40 log
                prog.visibility = View.GONE // Yükleme bitti
                if (!r.isSuccessful) { // HTTP hatası
                    Toast.makeText(this@AdminUserManagementActivity, readErrorMessage(r), Toast.LENGTH_LONG).show() // Toast
                    dlg.dismiss() // Diyalogu kapat
                    return@launch
                }
                val items = r.body() ?: emptyList() // Log listesi
                if (items.isEmpty()) { // Kayıt yok
                    val tv = TextView(this@AdminUserManagementActivity).apply {
                        text = "Bu kullanıcı için kayıt yok." // Boş mesaj
                        setTextColor(ContextCompat.getColor(context, R.color.admin_muted)) // Soluk renk
                        textSize = 14f // Yazı boyutu
                    }
                    container.addView(tv) // Yer tutucu ekle
                    return@launch
                }
                for (a in items) { // Her log satırı
                    val row = layoutInflater.inflate(R.layout.item_user_log_entry, container, false) // Satır şablonu
                    row.findViewById<TextView>(R.id.uleInitial).text =
                        (a.initial?.take(1) ?: "?").uppercase() // Avatar harfi
                    val meta = "${a.actorDisplayName.orEmpty()} · ${formatDateTimeTr(a.createdAt)}" // Meta satırı
                    row.findViewById<TextView>(R.id.uleMeta).text = meta // Meta yaz
                    row.findViewById<TextView>(R.id.uleDesc).text = ActivityDescriptionNormalize.toDisplay(a.description) // Açıklama
                    container.addView(row) // Satırı ekle
                }
            } catch (e: Exception) { // Beklenmeyen hata
                prog.visibility = View.GONE // Yüklemeyi kapat
                Toast.makeText(this@AdminUserManagementActivity, e.message ?: "Hata", Toast.LENGTH_LONG).show() // Toast
                dlg.dismiss() // Diyalogu kapat
            }
        }
    }

    private fun applyRoleChip(tv: TextView, roleKey: String?, roleLabel: String?) { // Rol rozeti stili
        tv.text = roleLabel ?: "—" // Rol etiketi
        if (roleKey == "admin") { // Admin rolü
            tv.setBackgroundResource(R.drawable.um_chip_role_admin) // Amber arka plan
            tv.setTextColor(ContextCompat.getColor(this, R.color.um_chip_amber_text)) // Amber metin
        } else { // Diğer roller
            tv.setBackgroundResource(R.drawable.um_chip_role_default) // Varsayılan arka plan
            tv.setTextColor(ContextCompat.getColor(this, R.color.um_chip_emerald_text)) // Yeşil metin
        }
    }

    private fun applyStatusChip(tv: TextView, statusKey: String?, statusLabel: String?) { // Durum rozeti stili
        tv.text = statusLabel ?: "—" // Durum etiketi
        val bg = when (statusKey) { // Arka plan kaynağı
            "suspended" -> R.drawable.um_chip_status_suspended // Askıda
            "pending" -> R.drawable.um_chip_status_pending // Beklemede
            else -> R.drawable.um_chip_status_active // Aktif
        }
        val col = when (statusKey) { // Metin rengi
            "suspended" -> R.color.um_chip_red_text // Kırmızı
            "pending" -> R.color.um_chip_amber_text // Amber
            else -> R.color.um_chip_emerald_text // Yeşil
        }
        tv.setBackgroundResource(bg) // Arka plan uygula
        tv.setTextColor(ContextCompat.getColor(this, col)) // Renk uygula
    }

    private fun formatDateTr(iso: String?): String { // ISO tarihi kısa Türkçe göster
        if (iso.isNullOrBlank()) return "—" // Boşsa tire
        val ms = parseIsoToMillis(iso) ?: return "—" // Ayrıştırılamazsa tire
        return SimpleDateFormat("d.MM.yyyy", Locale("tr", "TR")).format(Date(ms)) // Gün.ay.yıl
    }

    private fun formatDateTimeTr(iso: String?): String { // ISO tarih-saat Türkçe göster
        if (iso.isNullOrBlank()) return "—" // Boşsa tire
        val ms = parseIsoToMillis(iso) ?: return "—" // Ayrıştırılamazsa tire
        return SimpleDateFormat("d.MM.yyyy HH:mm", Locale("tr", "TR")).format(Date(ms)) // Tarih ve saat
    }

    private fun formatTimeAgoTr(createdAt: String?): String { // Türkçe göreli zaman metni
        val then = parseIsoToMillis(createdAt) ?: return "—" // Tarih yoksa tire
        val diff = System.currentTimeMillis() - then // Geçen süre ms
        if (diff < 0) return "Az önce" // Gelecek tarih koruması
        val s = diff / 1000 // Saniye
        if (s < 60) return "Az önce" // Bir dakikadan az
        val m = s / 60 // Dakika
        if (m < 60) return "$m dk önce" // Saatten az
        val h = m / 60 // Saat
        if (h < 24) return "$h saat önce" // Günden az
        val days = h / 24 // Gün
        if (days < 7) return "$days gün önce" // Haftadan az
        return SimpleDateFormat("d MMM yyyy", Locale("tr", "TR")).format(Date(then)) // Uzun tarih
    }

    private fun parseIsoToMillis(raw: String?): Long? { // ISO string'i milisaniyeye çevir
        if (raw.isNullOrBlank()) return null // Boşsa geçersiz
        val tries = listOf( // Denenecek formatlar
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC") // UTC zaman dilimi
            },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        )
        for (fmt in tries) { // Her formatı dene
            try {
                val d = fmt.parse(raw) ?: continue // Ayrıştır
                return d.time // Milisaniye döndür
            } catch (_: Exception) { }
        }
        return null // Hiçbiri tutmadı
    }
}
