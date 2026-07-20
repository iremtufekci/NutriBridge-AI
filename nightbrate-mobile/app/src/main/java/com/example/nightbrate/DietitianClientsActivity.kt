package com.example.nightbrate // Uygulama paketi

import android.content.Intent // Aktivite geçişi
import android.graphics.Color // Renk sabitleri
import android.graphics.Typeface // Kalın yazı tipi
import android.os.Bundle // Aktivite durum paketi
import android.text.Editable // Düzenlenebilir metin
import android.text.TextWatcher // Metin değişim dinleyicisi
import android.view.Gravity // Hizalama
import android.view.View // Görünüm temel sınıfı
import android.widget.EditText // Arama kutusu
import android.widget.LinearLayout // Dikey liste düzeni
import android.widget.ProgressBar // Yükleme göstergesi
import android.widget.TextView // Metin görünümü
import androidx.appcompat.app.AppCompatActivity // Temel aktivite
import androidx.core.content.ContextCompat // Kaynak renk erişimi
import androidx.lifecycle.lifecycleScope // Yaşam döngüsü coroutine kapsamı
import com.google.android.material.button.MaterialButton // Sekme ve düğmeler
import com.google.android.material.card.MaterialCardView // Danışan kartı
import kotlinx.coroutines.Dispatchers // IO iş parçacığı
import kotlinx.coroutines.launch // Asenkron başlatma
import kotlinx.coroutines.withContext // Bağlam değiştirme
import org.json.JSONObject // Hata JSON ayrıştırma
import retrofit2.Response // HTTP yanıtı
import java.text.SimpleDateFormat // Tarih biçimlendirme
import java.util.Locale // Yerel ayar
import java.util.TimeZone // Saat dilimi

class DietitianClientsActivity : AppCompatActivity() { // Danışan listesi ekranı

    private var currentTab = "all" // Aktif sekme anahtarı
    private var sortNameAscending = true // Ada göre artan sıralama

    private lateinit var container: LinearLayout // Kart listesi konteyneri
    private lateinit var progress: ProgressBar // Yükleme çubuğu
    private lateinit var tvErr: TextView // Hata metni
    private lateinit var etSearch: EditText // Arama alanı
    private lateinit var btnSort: MaterialButton // Sıralama düğmesi

    private lateinit var btnAll: MaterialButton // Tümü sekmesi
    private lateinit var btnActive: MaterialButton // Aktif sekmesi
    private lateinit var btnCritical: MaterialButton // Kritik sekmesi
    private lateinit var btnPassive: MaterialButton // Pasif sekmesi

    private var loadedClients: List<DietitianClientCardDto> = emptyList() // Yüklenen danışanlar
    private var counts: DietitianTabCountsDto = DietitianTabCountsDto() // Sekme sayıları

    private val emerald by lazy { ContextCompat.getColor(this, R.color.nav_item_active) } // Aktif sekme rengi
    private val muted by lazy { ContextCompat.getColor(this, R.color.admin_muted) } // Soluk metin rengi
    private val strong by lazy { ContextCompat.getColor(this, R.color.admin_strong) } // Koyu metin rengi

    override fun onCreate(savedInstanceState: Bundle?) { // Aktivite oluşturulduğunda
        super.onCreate(savedInstanceState) // Üst sınıf başlatması
        setContentView(R.layout.activity_dietitian_clients) // Düzeni yükle
        DietitianBottomBarHelper.bind(this, 1) // Alt menüyü bağla

        container = findViewById(R.id.containerClientCards) // Kart konteyneri
        progress = findViewById(R.id.progressClients) // İlerleme çubuğu
        tvErr = findViewById(R.id.tvClientsError) // Hata alanı
        etSearch = findViewById(R.id.etClientSearch) // Arama kutusu
        btnSort = findViewById(R.id.btnSortName) // Sıralama düğmesi

        btnAll = findViewById(R.id.btnTabAll) // Tümü sekmesi
        btnActive = findViewById(R.id.btnTabActive) // Aktif sekmesi
        btnCritical = findViewById(R.id.btnTabCritical) // Kritik sekmesi
        btnPassive = findViewById(R.id.btnTabPassive) // Pasif sekmesi

        btnAll.setOnClickListener { switchTab("all") } // Tümü sekmesine geç
        btnActive.setOnClickListener { switchTab("active") } // Aktif sekmesine geç
        btnCritical.setOnClickListener { switchTab("critical") } // Kritik sekmesine geç
        btnPassive.setOnClickListener { switchTab("passive") } // Pasif sekmesine geç

        btnSort.setOnClickListener { // Sıralama yönünü değiştir
            sortNameAscending = !sortNameAscending // Yönü ters çevir
            syncSortLabel() // Etiketi güncelle
            loadFromApi() // Listeyi yeniden yükle
        }

        etSearch.addTextChangedListener(object : TextWatcher { // Arama metni değişince
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {} // Öncesi
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {} // Değişim anı
            override fun afterTextChanged(s: Editable?) { // Değişim sonrası
                renderCards() // Kartları yeniden çiz
            }
        })

        syncSortLabel() // Sıralama etiketini ayarla
        styleTabs() // Sekmeleri biçimlendir
        loadFromApi() // İlk veriyi yükle
    }

    private fun switchTab(tab: String) { // Sekme değiştir
        if (currentTab == tab) return // Aynı sekmedeyse çık
        currentTab = tab // Aktif sekmeyi güncelle
        styleTabs() // Sekme görünümünü yenile
        loadFromApi() // Yeni sekme verisini yükle
    }

    private fun syncSortLabel() { // Sıralama düğmesi metnini güncelle
        btnSort.text = if (sortNameAscending) "A-Z" else "Z-A" // Yön etiketi
    }

    private fun styleTabs() { // Sekme düğmelerinin görünümünü ayarla
        listOf(
            Triple(btnAll, "all", counts.all), // Tümü ve sayısı
            Triple(btnActive, "active", counts.active), // Aktif ve sayısı
            Triple(btnCritical, "critical", counts.critical), // Kritik ve sayısı
            Triple(btnPassive, "passive", counts.passive) // Pasif ve sayısı
        ).forEach { (btn, key, n) -> // Her sekme için
            val label = when (key) { // Sekme etiketi
                "all" -> "Tümü" // Tüm danışanlar
                "active" -> "Aktif" // Aktif danışanlar
                "critical" -> "Kritik" // Kritik danışanlar
                else -> "Pasif" // Pasif danışanlar
            }
            btn.text = "$label ($n)" // Etiket ve sayı
            val sel = currentTab == key // Seçili mi
            btn.setBackgroundColor(if (sel) emerald else Color.TRANSPARENT) // Arka plan rengi
            btn.setTextColor(if (sel) Color.WHITE else strong) // Metin rengi
        }
    }

    private fun loadFromApi() { // Danışan listesini API'den al
        tvErr.visibility = View.GONE // Hatayı gizle
        progress.visibility = View.VISIBLE // Yükleniyor göster
        lifecycleScope.launch { // Coroutine ile istek
            val sort = if (sortNameAscending) "nameAsc" else "nameDesc" // Sıralama parametresi
            val result = withContext(Dispatchers.IO) { // IO iş parçacığında
                runCatching {
                    RetrofitClient.instance.getMyClients(sort = sort, tab = currentTab) // Liste çağrısı
                }
            }
            progress.visibility = View.GONE // Yüklemeyi gizle
            result.onSuccess { resp -> // Başarılı sonuç
                if (resp.isSuccessful && resp.body() != null) { // Geçerli gövde
                    val body = resp.body()!! // Yanıt gövdesi
                    counts = body.tabCounts // Sekme sayılarını sakla
                    loadedClients = body.clients // Danışanları sakla
                    styleTabs() // Sekmeleri yenile
                    renderCards() // Kartları çiz
                } else { // HTTP hatası
                    tvErr.text = readError(resp) // Hata mesajını oku
                    tvErr.visibility = View.VISIBLE // Hatayı göster
                }
            }.onFailure { // İstisna
                tvErr.text = it.message ?: "Liste alınamadı." // Hata metni
                tvErr.visibility = View.VISIBLE // Hatayı göster
            }
        }
    }

    private fun filteredList(): List<DietitianClientCardDto> { // Aramaya göre süzülmüş liste
        val q = etSearch.text?.toString()?.trim()?.lowercase().orEmpty() // Arama sorgusu
        if (q.isEmpty()) return loadedClients // Boşsa tüm liste
        return loadedClients.filter { c -> // Eşleşenleri filtrele
            val name = (c.displayName ?: "${c.firstName.orEmpty()} ${c.lastName.orEmpty()}").trim().lowercase() // Tam ad
            name.contains(q) // İçeriyor mu
        }
    }

    private fun dp(x: Int): Int = (x * resources.displayMetrics.density).toInt() // dp'yi piksele çevir

    private fun renderCards() { // Danışan kartlarını oluştur
        container.removeAllViews() // Önceki kartları temizle
        val list = filteredList() // Filtrelenmiş liste
        if (list.isEmpty()) { // Kayıt yoksa
            val tv = TextView(this).apply {
                text = "Kayıt yok." // Boş durum mesajı
                setTextColor(muted) // Soluk renk
                setPadding(dp(8), dp(24), dp(8), dp(8)) // İç boşluk
            }
            container.addView(tv) // Mesajı ekle
            return // Çizimi bitir
        }
        val pad = dp(12) // Kart iç boşluğu
        for (c in list) { // Her danışan için kart
            val id = c.id ?: continue // Kimlik yoksa atla
            val card = MaterialCardView(this).apply {
                radius = dp(16).toFloat() // Köşe yuvarlaklığı
                cardElevation = dp(2).toFloat() // Gölge
                setPadding(pad, pad, pad, pad) // İç boşluk
                val critical = c.isCritical || c.segment.equals("critical", true) // Kritik mi
                strokeWidth = if (critical) dp(2) else 0 // Kritikse kenarlık
                strokeColor = if (critical) Color.parseColor("#FB7185") else Color.TRANSPARENT // Kenar rengi
            }
            val root = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL // Dikey düzen
            }
            val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL } // Üst satır
            val initial = (c.displayName?.trim()?.take(1)?.uppercase() ?: "?") // Baş harf
            val avatar = TextView(this).apply {
                text = initial // Avatar harfi
                setTextColor(Color.WHITE) // Beyaz metin
                textSize = 16f // Normal yazı
                gravity = Gravity.CENTER // Ortala
                val w = dp(48) // Avatar genişliği
                layoutParams = LinearLayout.LayoutParams(w, w) // Kare boyut
                val bg = if (c.isCritical || c.segment.equals("critical", true)) Color.parseColor("#F43F5E") else Color.parseColor("#22C55E") // Avatar rengi
                setBackgroundColor(bg) // Arka plan
            }
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL // Ad ve bilgi sütunu
            }
            val name = TextView(this).apply {
                text = c.displayName ?: "${c.firstName.orEmpty()} ${c.lastName.orEmpty()}".trim() // Danışan adı
                setTextColor(strong) // Koyu renk
                textSize = 16f // Normal yazı
                setTypeface(null, Typeface.BOLD) // Kalın yazı
            }
            val start = TextView(this).apply {
                text = "Başlangıç: ${formatShortDate(c.startedAtUtc)}" // Başlangıç tarihi
                setTextColor(muted) // Soluk renk
                textSize = 12f // Küçük yazı
            }
            val last = TextView(this).apply {
                text = "Son aktivite: ${relativeActivity(c.lastActivityUtc)}" // Son aktivite
                setTextColor(muted) // Soluk renk
                textSize = 12f // Küçük yazı
            }
            col.addView(name) // Adı ekle
            col.addView(start) // Başlangıcı ekle
            col.addView(last) // Son aktiviteyi ekle
            head.addView(avatar) // Avatarı ekle
            head.addView(col, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(12) // Sol boşluk
            })

            val rowPct = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL // Uyum oranı satırı
                setPadding(0, dp(10), 0, 0) // Üst boşluk
            }
            val pct = (c.compliancePercent).coerceIn(0, 100) // Yüzde değeri
            rowPct.addView(TextView(this).apply {
                text = "Uyum oranı" // Etiket
                setTextColor(muted) // Soluk renk
                textSize = 12f // Küçük yazı
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) // Esnek alan
            })
            rowPct.addView(TextView(this).apply {
                text = "%$pct" // Yüzde metni
                setTextColor(strong) // Koyu renk
                textSize = 12f // Küçük yazı
            })
            val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100 // Maksimum değer
                progress = pct // Mevcut uyum
            }
            val barLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(8)) // Tam genişlik çubuk
            barLp.topMargin = dp(4) // Üst boşluk

            val btnView = MaterialButton(this).apply {
                text = "Görüntüle" // Detay düğmesi
                setBackgroundColor(emerald) // Vurgu rengi
                setTextColor(Color.WHITE) // Beyaz metin
                setOnClickListener { // Detay ekranına git
                    startActivity(
                        Intent(this@DietitianClientsActivity, DietitianClientDetailActivity::class.java)
                            .putExtra(DietitianClientDetailActivity.EXTRA_CLIENT_ID, id) // Kimlik gönder
                    )
                }
            }
            val btnLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT) // Tam genişlik düğme
            btnLp.topMargin = dp(12) // Üst boşluk

            root.addView(head) // Üst satırı ekle
            root.addView(rowPct) // Uyum etiketini ekle
            root.addView(bar, barLp) // İlerleme çubuğunu ekle
            root.addView(btnView, btnLp) // Görüntüle düğmesini ekle
            card.addView(root) // Kök düzeni karta ekle
            val cardLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT) // Tam genişlik kart
            cardLp.bottomMargin = dp(12) // Alt boşluk
            container.addView(card, cardLp) // Kartı listeye ekle
        }
    }

    private fun formatShortDate(iso: String?): String { // ISO tarihi kısa Türkçe biçimle
        if (iso.isNullOrBlank()) return "—" // Boşsa tire
        return try {
            val fmtIn = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC") // UTC saat dilimi
            }
            val fmtOut = SimpleDateFormat("d MMM yyyy", Locale("tr", "TR")) // Çıkış biçimi
            val cleaned = iso.replace("Z", "").take(23) // Zaman damgasını temizle
            fmtOut.format(fmtIn.parse(cleaned)!!) // Biçimlendirilmiş tarih
        } catch (_: Exception) { // Ayrıştırma hatası
            try {
                iso.take(10) // Sadece tarih kısmı
            } catch (_: Exception) {
                "—" // Varsayılan
            }
        }
    }

    private fun relativeActivity(iso: String?): String { // Son aktiviteyi göreli süre olarak göster
        if (iso.isNullOrBlank()) return "henüz yok" // Aktivite yok
        return try {
            val t = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC") // UTC saat dilimi
            }.parse(iso)?.time // Milisaniye zamanı
                ?: java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC") // Yedek desen
                }.parse(iso)?.time // Milisaniye zamanı
                ?: return "—" // Ayrıştırılamazsa tire
            val diff = System.currentTimeMillis() - t // Geçen süre
            val mins = diff / 60000 // Dakika
            val h = diff / 3600000 // Saat
            val d = diff / 86400000 // Gün
            when { // Göreli metin seç
                mins < 2 -> "az önce" // Çok yakın
                mins < 60 -> "$mins dk önce" // Dakika önce
                h < 24 -> "$h saat önce" // Saat önce
                else -> "$d gün önce" // Gün önce
            }
        } catch (_: Exception) { // Hata durumu
            "—" // Varsayılan
        }
    }

    private fun readError(resp: Response<*>): String { // API hata mesajını çöz
        val raw = resp.errorBody()?.string().orEmpty() // Ham hata gövdesi
        return try {
            JSONObject(raw).optString("message").ifBlank { "Hata ${resp.code()}" } // JSON mesajı
        } catch (_: Exception) { // JSON değilse
            raw.ifBlank { "Hata ${resp.code()}" } // Ham veya varsayılan
        }
    }
}
