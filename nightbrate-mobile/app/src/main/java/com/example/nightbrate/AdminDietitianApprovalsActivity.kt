package com.example.nightbrate // Paket tanımı

import android.content.Intent // Harici bağlantı açma
import android.net.Uri // URL/URI işleme
import android.os.Bundle // Activity durum paketi
import android.view.View // Görünüm temel sınıfı
import android.widget.LinearLayout // Dikey liste konteyneri
import android.widget.ProgressBar // Yükleme göstergesi
import android.widget.TextView // Metin görünümü
import android.widget.Toast // Kısa bildirim
import androidx.appcompat.app.AppCompatActivity // Temel Activity
import androidx.core.content.ContextCompat // Kaynak renk çözümleme
import androidx.lifecycle.lifecycleScope // Yaşam döngüsü coroutine kapsamı
import com.google.android.material.button.MaterialButton // Material düğme
import com.google.android.material.dialog.MaterialAlertDialogBuilder // Material diyalog
import kotlinx.coroutines.launch // Coroutine başlatma
import org.json.JSONObject // JSON ayrıştırma
import retrofit2.Response // HTTP yanıt sarmalayıcısı
import java.text.SimpleDateFormat // Tarih biçimlendirme
import java.util.Date // Tarih nesnesi
import java.util.Locale // Yerel ayar
import java.util.TimeZone // Saat dilimi

class AdminDietitianApprovalsActivity : AppCompatActivity() { // Diyetisyen onay ekranı

    private lateinit var list: LinearLayout // Onay listesi konteyneri
    private lateinit var progress: ProgressBar // Yükleme çubuğu
    private lateinit var empty: TextView // Boş/hata metni
    private lateinit var badge: TextView // Bekleyen sayı rozeti

    override fun onCreate(savedInstanceState: Bundle?) { // Activity oluşturulduğunda
        super.onCreate(savedInstanceState) // Üst sınıf başlatma
        setContentView(R.layout.activity_admin_approvals) // Onay düzeni
        AdminBottomBarHelper.bind(this, 2) // Alt sekme (onaylar)
        list = findViewById(R.id.approvalList) // Liste referansı
        progress = findViewById(R.id.approvalsProgress) // İlerleme referansı
        empty = findViewById(R.id.tvApprovalsEmpty) // Boş metin referansı
        badge = findViewById(R.id.tvApprovalsBadge) // Rozet referansı
        load() // Bekleyenleri yükle
    }

    private fun load() { // API'den bekleyen diyetisyenleri çek
        progress.visibility = View.VISIBLE // Yükleniyor göster
        empty.visibility = View.GONE // Boş metni gizle
        lifecycleScope.launch { // Arka planda istek
            try {
                val r = RetrofitClient.instance.getPendingDietitians() // Bekleyen listesi
                progress.visibility = View.GONE // Yükleme bitti
                list.removeAllViews() // Eski satırları temizle
                if (!r.isSuccessful) { // HTTP hatası
                    empty.text = readErrorMessage(r) // Hata mesajı
                    empty.visibility = View.VISIBLE // Metni göster
                    badge.text = "—" // Rozeti sıfırla
                    return@launch // İşlemi durdur
                }
                val body = r.body() ?: emptyList() // Yanıt gövdesi
                badge.text = "${body.size} Onay bekliyor" // Bekleyen sayısı
                if (body.isEmpty()) { // Kayıt yok
                    empty.text = "Onay bekleyen diyetisyen bulunmuyor." // Boş mesaj
                    empty.visibility = View.VISIBLE // Göster
                    return@launch
                }
                for (item in body) { // Her kayıt için satır
                    addRow(item) // Satır ekle
                }
            } catch (e: Exception) { // Ağ veya beklenmeyen hata
                progress.visibility = View.GONE // Yüklemeyi kapat
                empty.text = e.message ?: "Bağlantı hatası" // Hata metni
                empty.visibility = View.VISIBLE // Panelde göster
                badge.text = "—" // Rozeti sıfırla
            }
        }
    }

    private fun addRow(d: PendingDietitianItem) { // Tek onay satırı oluştur
        val v = layoutInflater.inflate(R.layout.item_approval_row, list, false) // Satır şablonu
        val initial = (d.firstName?.take(1) ?: "D").uppercase() // Avatar harfi
        v.findViewById<TextView>(R.id.apAvatar).text = initial // Harfi yaz
        v.findViewById<TextView>(R.id.apName).text =
            "Dr. ${d.firstName.orEmpty().trim()} ${d.lastName.orEmpty().trim()}".trim() // Tam ad
        v.findViewById<TextView>(R.id.apEmail).text = d.email.orEmpty().ifBlank { "—" } // E-posta
        v.findViewById<TextView>(R.id.apDiploma).text =
            "Diploma: ${d.diplomaNo ?: "—"}" // Diploma numarası
        v.findViewById<TextView>(R.id.apClinic).text =
            "Klinik: ${d.clinicName ?: "—"}" // Klinik adı
        v.findViewById<TextView>(R.id.apDate).text =
            "Kayıt: ${formatDateTr(d.createdAt)}" // Kayıt tarihi

        val id = d.id // Diyetisyen kimliği
        v.findViewById<MaterialButton>(R.id.apInspect).setOnClickListener { // İncele düğmesi
            if (id.isNullOrBlank()) return@setOnClickListener // Kimlik yoksa çık
            showDetailDialog(id) // Detay diyalogu aç
        }
        v.findViewById<MaterialButton>(R.id.apApprove).setOnClickListener { // Onayla düğmesi
            if (id.isNullOrBlank()) return@setOnClickListener // Kimlik yoksa çık
            approveById(id) // Onay API çağrısı
        }
        list.addView(v) // Satırı listeye ekle
    }

    private fun showDetailDialog(dietitianId: String) { // Diyetisyen detay diyalogu
        val dialogView = layoutInflater.inflate(R.layout.dialog_dietitian_detail, null) // Diyalog düzeni
        val dlgProgress = dialogView.findViewById<ProgressBar>(R.id.detailProgress) // Diyalog yüklemesi
        val scroll = dialogView.findViewById<View>(R.id.detailScroll) // Kaydırılabilir içerik
        val ddAvatar = dialogView.findViewById<TextView>(R.id.ddAvatar) // Avatar harfi
        val ddName = dialogView.findViewById<TextView>(R.id.ddName) // Ad soyad
        val ddDiploma = dialogView.findViewById<TextView>(R.id.ddDiploma) // Diploma no
        val ddClinic = dialogView.findViewById<TextView>(R.id.ddClinic) // Klinik
        val ddCreated = dialogView.findViewById<TextView>(R.id.ddCreated) // Kayıt tarihi
        val ddStatus = dialogView.findViewById<TextView>(R.id.ddStatus) // Onay durumu
        val ddDownload = dialogView.findViewById<MaterialButton>(R.id.ddDownload) // Diploma indir
        val ddApprove = dialogView.findViewById<MaterialButton>(R.id.ddApprove) // Onayla

        scroll.visibility = View.GONE // İçeriği gizle
        dlgProgress.visibility = View.VISIBLE // Yüklemeyi göster

        val dlg = MaterialAlertDialogBuilder(this) // Diyalog oluşturucu
            .setView(dialogView) // Özel görünüm
            .setNegativeButton("Kapat", null) // Kapat düğmesi
            .create() // Diyalogu oluştur
        dlg.show() // Diyalogu göster

        lifecycleScope.launch { // Detay verisini çek
            try {
                val r = RetrofitClient.instance.getAdminDietitianDetail(dietitianId) // Detay API
                dlgProgress.visibility = View.GONE // Yükleme bitti
                if (!r.isSuccessful) { // HTTP hatası
                    Toast.makeText(this@AdminDietitianApprovalsActivity, readErrorMessage(r), Toast.LENGTH_LONG).show() // Toast
                    dlg.dismiss() // Diyalogu kapat
                    return@launch
                }
                val sel = r.body() // Yanıt gövdesi
                if (sel == null) { // Boş yanıt
                    dlg.dismiss() // Diyalogu kapat
                    return@launch
                }
                scroll.visibility = View.VISIBLE // İçeriği göster
                ddAvatar.text = (sel.firstName?.take(1) ?: "D").uppercase() // Avatar harfi
                ddName.text = "Dr. ${sel.firstName.orEmpty()} ${sel.lastName.orEmpty()}".trim() // Tam ad
                ddDiploma.text = sel.diplomaNo?.ifBlank { "—" } ?: "—" // Diploma
                ddClinic.text = sel.clinicName?.ifBlank { "—" } ?: "—" // Klinik
                ddCreated.text = formatDateTr(sel.createdAt) // Kayıt tarihi
                if (sel.isApproved == true) { // Zaten onaylı
                    ddStatus.text = "Onaylandı" // Durum metni
                    ddStatus.setBackgroundResource(R.drawable.um_chip_status_active) // Yeşil arka plan
                    ddStatus.setTextColor(ContextCompat.getColor(this@AdminDietitianApprovalsActivity, R.color.um_chip_emerald_text)) // Yeşil metin
                } else { // Beklemede
                    ddStatus.text = "Beklemede" // Durum metni
                    ddStatus.setBackgroundResource(R.drawable.um_chip_status_pending) // Amber arka plan
                    ddStatus.setTextColor(ContextCompat.getColor(this@AdminDietitianApprovalsActivity, R.color.um_chip_amber_text)) // Amber metin
                }

                val url = resolveDocumentUrl(sel.diplomaDocumentUrl?.trim().orEmpty()) // Diploma URL'si
                ddDownload.setOnClickListener { // İndir düğmesi
                    if (url.isEmpty()) { // Dosya yok
                        Toast.makeText(
                            this@AdminDietitianApprovalsActivity,
                            "Bu kayıt için yüklenmiş diploma dosyası bulunmuyor.",
                            Toast.LENGTH_LONG
                        ).show() // Uyarı toast
                    } else {
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) // Tarayıcıda aç
                        } catch (_: Exception) {
                            Toast.makeText(this@AdminDietitianApprovalsActivity, "Bağlantı açılamadı", Toast.LENGTH_SHORT).show() // Hata toast
                        }
                    }
                }

                ddApprove.setOnClickListener { // Diyalogdan onayla
                    val aid = sel.id ?: dietitianId // Kimlik yedekle
                    dlg.dismiss() // Diyalogu kapat
                    approveById(aid) // Onay API çağrısı
                }
            } catch (e: Exception) { // Beklenmeyen hata
                dlgProgress.visibility = View.GONE // Yüklemeyi kapat
                Toast.makeText(this@AdminDietitianApprovalsActivity, e.message ?: "Hata", Toast.LENGTH_LONG).show() // Toast
                dlg.dismiss() // Diyalogu kapat
            }
        }
    }

    private fun approveById(id: String) { // Diyetisyeni onayla
        lifecycleScope.launch { // Arka planda istek
            try {
                val ar = RetrofitClient.instance.approveDietitian(id) // Onay API
                if (ar.isSuccessful) { // Başarılı
                    Toast.makeText(
                        this@AdminDietitianApprovalsActivity,
                        "Onaylandı. Diyetisyen giriş yapabilir.",
                        Toast.LENGTH_LONG
                    ).show() // Başarı mesajı
                    load() // Listeyi yenile
                } else { // HTTP hatası
                    Toast.makeText(
                        this@AdminDietitianApprovalsActivity,
                        readErrorMessage(ar),
                        Toast.LENGTH_LONG
                    ).show() // Hata mesajı
                }
            } catch (e: Exception) { // Ağ hatası
                Toast.makeText(this@AdminDietitianApprovalsActivity, e.message ?: "Hata", Toast.LENGTH_LONG).show() // Toast
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

    private fun formatDateTr(iso: String?): String { // ISO tarihi Türkçe göster
        if (iso.isNullOrBlank()) return "—" // Boşsa tire
        val ms = parseIsoToMillis(iso) ?: return "—" // Ayrıştırılamazsa tire
        return SimpleDateFormat("d.MM.yyyy", Locale("tr", "TR")).format(Date(ms)) // Gün.ay.yıl
    }

    private fun parseIsoToMillis(raw: String?): Long? { // ISO string'i milisaniyeye çevir
        if (raw.isNullOrBlank()) return null // Boşsa geçersiz
        val tries = listOf( // Denenecek formatlar
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC") // UTC zaman dilimi
            },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US)
        )
        for (fmt in tries) { // Her formatı dene
            try {
                val d = fmt.parse(raw) ?: continue // Ayrıştır
                return d.time // Milisaniye döndür
            } catch (_: Exception) { }
        }
        return null // Hiçbiri tutmadı
    }

    private fun resolveDocumentUrl(path: String): String { // Göreli yolu tam URL'ye çevir
        if (path.isEmpty()) return "" // Boş yol
        if (path.startsWith("http://") || path.startsWith("https://")) return path // Zaten tam URL
        val base = RetrofitClient.API_BASE_URL.trimEnd('/') // API taban adresi
        val p = if (path.startsWith("/")) path else "/$path" // Başında / garantisi
        return "$base$p" // Birleştirilmiş URL
    }
}
