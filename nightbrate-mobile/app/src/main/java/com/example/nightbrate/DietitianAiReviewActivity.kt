package com.example.nightbrate // Uygulama paketi

import android.content.Context // Bağlam referansı
import android.graphics.Typeface // Kalın yazı tipi
import android.os.Bundle // Aktivite durum paketi
import android.text.Editable // Düzenlenebilir metin
import android.text.TextWatcher // Metin değişim dinleyicisi
import android.view.View // Görünüm temel sınıfı
import android.view.ViewGroup // Liste öğe düzeni
import android.widget.BaseAdapter // Liste adaptörü
import android.widget.EditText // Arama kutusu
import android.widget.LinearLayout // Dikey liste düzeni
import android.widget.ListView // Danışan listesi
import android.widget.ProgressBar // Yükleme göstergesi
import android.widget.TextView // Metin görünümü
import android.widget.Toast // Kısa bildirim
import androidx.appcompat.app.AppCompatActivity // Temel aktivite
import androidx.core.content.ContextCompat // Kaynak renk erişimi
import androidx.lifecycle.lifecycleScope // Yaşam döngüsü coroutine kapsamı
import com.google.android.material.card.MaterialCardView // Seçili danışan kartı
import kotlinx.coroutines.launch // Asenkron başlatma
import java.text.SimpleDateFormat // Tarih biçimlendirme
import java.util.Locale // Yerel ayar
import java.util.TimeZone // Saat dilimi

class DietitianAiReviewActivity : AppCompatActivity() { // AI tarif inceleme ekranı

    private lateinit var etSearch: EditText // Danışan arama alanı
    private lateinit var listClients: ListView // Danışan listesi
    private lateinit var progressList: ProgressBar // Liste yükleme çubuğu
    private lateinit var cardSelected: MaterialCardView // Seçili danışan kartı
    private lateinit var tvSelectedName: TextView // Seçili danışan adı
    private lateinit var progressKitchen: ProgressBar // Tarif kayıtları yükleme
    private lateinit var llLogs: LinearLayout // Tarif kayıtları listesi
    private lateinit var tvLogsEmpty: TextView // Boş liste mesajı
    private lateinit var layoutKitchenSection: LinearLayout // Mutfak bölümü konteyneri

    private var allClients: List<ClientWithLastMealItem> = emptyList() // Tüm danışanlar
    private var filteredClients: List<ClientWithLastMealItem> = emptyList() // Filtrelenmiş danışanlar
    private var selectedClientId: String? = null // Seçili danışan kimliği
    private lateinit var adapter: ClientListAdapter // Liste adaptörü

    override fun onCreate(savedInstanceState: Bundle?) { // Aktivite oluşturulduğunda
        super.onCreate(savedInstanceState) // Üst sınıf başlatması
        setContentView(R.layout.activity_dietitian_ai_review) // Düzeni yükle
        DietitianBottomBarHelper.bind(this, 3) // Alt menüyü bağla

        etSearch = findViewById(R.id.etAiReviewSearch) // Arama alanı
        listClients = findViewById(R.id.listAiReviewClients) // Danışan listesi
        progressList = findViewById(R.id.progressAiReviewClients) // Liste yükleme
        cardSelected = findViewById(R.id.cardAiReviewSelected) // Seçim kartı
        tvSelectedName = findViewById(R.id.tvAiReviewSelectedName) // Seçili ad
        progressKitchen = findViewById(R.id.progressAiReviewKitchen) // Kayıt yükleme
        llLogs = findViewById(R.id.llAiReviewKitchenLogs) // Kayıt listesi
        tvLogsEmpty = findViewById(R.id.tvAiReviewLogsEmpty) // Boş mesaj
        layoutKitchenSection = findViewById(R.id.layoutAiReviewKitchenSection) // Mutfak bölümü

        adapter = ClientListAdapter(this, emptyList(), null) { id -> // Danışan seçildiğinde
            selectedClientId = id // Kimliği sakla
            adapter.setSelected(id) // Adaptörde seçimi güncelle
            adapter.notifyDataSetChanged() // Listeyi yenile
            updateSelectedBanner() // Üst banner'ı güncelle
            layoutKitchenSection.visibility = View.VISIBLE // Mutfak bölümünü göster
            loadKitchenLogs(id) // Tarif kayıtlarını yükle
        }
        listClients.adapter = adapter // Adaptörü bağla

        etSearch.addTextChangedListener(object : TextWatcher { // Arama metni değişince
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {} // Öncesi
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { // Değişim anı
                filterClients(s?.toString().orEmpty()) // Listeyi filtrele
            }

            override fun afterTextChanged(s: Editable?) {} // Sonrası
        })

        loadClients() // Danışanları yükle
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt() // dp'yi piksele çevir

    private fun filterClients(q: String) { // Danışan listesini aramaya göre süz
        val t = q.trim().lowercase(Locale.getDefault()) // Küçük harf sorgu
        filteredClients = if (t.isEmpty()) allClients else allClients.filter { c -> // Boşsa tümü
            val n =
                "${c.firstName.orEmpty()} ${c.lastName.orEmpty()}".trim().lowercase(Locale.getDefault()) // Tam ad
            n.contains(t) // Eşleşme kontrolü
        }
        adapter.updateItems(filteredClients) // Adaptör verisini güncelle
        adapter.notifyDataSetChanged() // Listeyi yenile
    }

    private fun loadClients() { // Danışan listesini API'den al
        progressList.visibility = View.VISIBLE // Yükleniyor göster
        lifecycleScope.launch { // Coroutine ile istek
            try {
                val r = RetrofitClient.instance.getClientsWithLastMeal() // Danışanları getir
                val list = if (r.isSuccessful) r.body().orEmpty() else emptyList() // Başarılıysa liste
                allClients = list // Tüm listeyi sakla
                filterClients(etSearch.text?.toString().orEmpty()) // Mevcut aramayı uygula
            } catch (e: Exception) { // Hata durumu
                Toast.makeText(this@DietitianAiReviewActivity, e.message ?: "Liste alınamadı", Toast.LENGTH_LONG) // Bildirim
                    .show()
                allClients = emptyList() // Listeyi boşalt
                filterClients("") // Boş filtreyi uygula
            } finally {
                progressList.visibility = View.GONE // Yüklemeyi gizle
            }
        }
    }

    private fun updateSelectedBanner() { // Seçili danışan üst şeridini güncelle
        val id = selectedClientId // Aktif kimlik
        if (id.isNullOrBlank()) { // Seçim yoksa
            cardSelected.visibility = View.GONE // Kartı gizle
            return // Çık
        }
        val c = allClients.find { it.id == id } // Danışanı bul
        val name =
            listOf(c?.firstName, c?.lastName).mapNotNull { it?.trim() }.filter { it.isNotEmpty() } // Ad soyad birleştir
                .joinToString(" ").ifBlank { "Danışan" } // Varsayılan ad
        tvSelectedName.text = name // Adı yaz
        cardSelected.visibility = View.VISIBLE // Kartı göster
    }

    private fun loadKitchenLogs(clientId: String) { // Seçili danışanın tarif kayıtlarını yükle
        llLogs.removeAllViews() // Eski kartları temizle
        tvLogsEmpty.visibility = View.GONE // Boş mesajı gizle
        progressKitchen.visibility = View.VISIBLE // Yükleniyor göster
        lifecycleScope.launch { // Coroutine ile istek
            try {
                val r = RetrofitClient.instance.getClientKitchenRecipeLogs(clientId, 30) // Son 30 kayıt
                val logs = if (r.isSuccessful) r.body().orEmpty() else emptyList() // Başarılıysa liste
                progressKitchen.visibility = View.GONE // Yüklemeyi gizle
                if (logs.isEmpty()) { // Kayıt yoksa
                    tvLogsEmpty.visibility = View.VISIBLE // Boş mesajı göster
                    return@launch // Çık
                }
                for (log in logs) { // Her kayıt için kart
                    llLogs.addView(buildLogCard(log)) // Kartı listeye ekle
                }
            } catch (_: Exception) { // Hata durumu
                progressKitchen.visibility = View.GONE // Yüklemeyi gizle
                tvLogsEmpty.visibility = View.VISIBLE // Boş mesajı göster
            }
        }
    }

    private fun buildLogCard(log: KitchenChefShareLogItem): View { // Tek tarif kaydı kartı
        val pad = dp(12) // İç boşluk değeri
        val card = MaterialCardView(this).apply {
            radius = dp(12).toFloat() // Köşe yuvarlaklığı
            cardElevation = 0f // Gölge yok
            strokeWidth = dp(1) // İnce kenarlık
            strokeColor = ContextCompat.getColor(this@DietitianAiReviewActivity, R.color.admin_row_stroke) // Kenar rengi
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT // Tam genişlik kart
            ).apply {
                bottomMargin = dp(12) // Alt boşluk
            }
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL // Dikey düzen
            setPadding(pad, pad, pad, pad) // İç boşluk
        }
        val meta = TextView(this).apply {
            textSize = 11f // Küçük yazı
            setTextColor(ContextCompat.getColor(this@DietitianAiReviewActivity, R.color.admin_muted)) // Soluk renk
            val whenStr = formatLogWhen(log.createdAtUtc) // Biçimlendirilmiş zaman
            val src = log.source?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: "" // Kaynak etiketi
            text = "Hedef ${log.targetCalories} kkal · $whenStr$src" // Üst bilgi satırı
        }
        col.addView(meta) // Meta satırını ekle
        col.addView(TextView(this).apply {
            textSize = 13f // Normal yazı
            setTextColor(ContextCompat.getColor(this@DietitianAiReviewActivity, R.color.admin_strong)) // Koyu renk
            text = "Tercih: ${log.preference}" // Beslenme tercihi
            setPadding(0, dp(6), 0, 0) // Üst boşluk
        })
        col.addView(TextView(this).apply {
            textSize = 11f // Küçük yazı
            setTextColor(ContextCompat.getColor(this@DietitianAiReviewActivity, R.color.admin_muted)) // Soluk renk
            text = "Malzemeler (sorgu): ${log.ingredients}" // Sorgu malzemeleri
        })
        for (r in log.selectedRecipes) { // Seçilen tarifler
            col.addView(buildRecipeBlock(r)) // Tarif bloğunu ekle
        }
        card.addView(col) // Sütunu karta ekle
        return card // Kartı döndür
    }

    private fun buildRecipeBlock(r: KitchenChefRecipeItem): View { // Tek tarif detay bloğu
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL // Dikey düzen
            setPadding(0, dp(10), 0, 0) // Üst boşluk
        }
        wrap.addView(TextView(this).apply {
            text = r.title // Tarif başlığı
            textSize = 14f // Normal yazı
            setTypeface(typeface, Typeface.BOLD) // Kalın yazı
            setTextColor(ContextCompat.getColor(this@DietitianAiReviewActivity, R.color.admin_strong)) // Koyu renk
        })
        if (!r.description.isNullOrBlank()) { // Açıklama varsa
            wrap.addView(TextView(this).apply {
                text = r.description // Tarif açıklaması
                textSize = 13f // Normal yazı
                setTextColor(ContextCompat.getColor(this@DietitianAiReviewActivity, R.color.admin_muted)) // Soluk renk
            })
        }
        wrap.addView(TextView(this).apply {
            val prep = r.prepTimeMinutes?.let { " · $it dk" } ?: "" // Hazırlık süresi
            text = "~${r.estimatedCalories} kkal$prep" // Kalori ve süre
            textSize = 11f // Küçük yazı
        })
        for (ing in r.ingredients) { // Malzeme listesi
            wrap.addView(TextView(this).apply {
                text = "• $ing" // Madde işaretli malzeme
                textSize = 11f // Küçük yazı
            })
        }
        var step = 1 // Adım sayacı
        for (s in r.steps) { // Hazırlık adımları
            wrap.addView(TextView(this).apply {
                text = "$step. $s" // Numaralı adım
                textSize = 11f // Küçük yazı
            })
            step++ // Sonraki adım
        }
        return wrap // Bloğu döndür
    }

    private fun formatLogWhen(iso: String): String { // ISO zamanı Türkçe biçimle
        val patterns = listOf( // Olası tarih desenleri
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (p in patterns) { // Her deseni dene
            try {
                val sdf = SimpleDateFormat(p, Locale.ROOT) // Giriş biçimleyici
                sdf.timeZone = TimeZone.getTimeZone("UTC") // UTC saat dilimi
                val d = sdf.parse(iso) ?: continue // Ayrıştır
                val out = SimpleDateFormat("d MMM yyyy, HH:mm", Locale("tr", "TR")) // Çıkış biçimi
                out.timeZone = TimeZone.getDefault() // Yerel saat dilimi
                return out.format(d) // Biçimlendirilmiş tarih
            } catch (_: Exception) { // Desen uymazsa devam
            }
        }
        return iso // Ham değeri döndür
    }

    private class ClientListAdapter( // Danışan liste adaptörü
        private val context: Context, // Bağlam
        private var items: List<ClientWithLastMealItem>, // Liste öğeleri
        private var selectedId: String?, // Seçili kimlik
        private val onSelect: (String) -> Unit // Seçim geri çağrısı
    ) : BaseAdapter() {
        fun updateItems(newItems: List<ClientWithLastMealItem>) { // Öğeleri güncelle
            items = newItems // Yeni listeyi ata
        }

        fun setSelected(id: String?) { // Seçili kimliği ayarla
            selectedId = id // Kimliği sakla
        }

        override fun getCount() = items.size // Öğe sayısı
        override fun getItem(position: Int) = items[position] // Konumdaki öğe
        override fun getItemId(position: Int) = position.toLong() // Öğe kimliği

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View { // Satır görünümü
            val item = items[position] // Liste öğesi
            val tv = (convertView as? TextView) ?: TextView(context).apply { // Yeniden kullan veya oluştur
                setPadding(36, 28, 36, 28) // İç boşluk
                textSize = 14f // Yazı boyutu
            }
            val label =
                listOf(item.firstName, item.lastName).mapNotNull { it?.trim() }.filter { it.isNotEmpty() } // Ad soyad
                    .joinToString(" ").ifBlank { "İsimsiz" } // Varsayılan etiket
            tv.text = label // Metni yaz
            val cid = item.id // Danışan kimliği
            val sel = cid != null && cid == selectedId // Seçili mi
            tv.setBackgroundColor(
                if (sel)
                    ContextCompat.getColor(context, R.color.ai_review_row_selected) // Seçili arka plan
                else
                    android.graphics.Color.TRANSPARENT // Şeffaf arka plan
            )
            tv.setOnClickListener { // Satıra tıklanınca
                if (cid != null) onSelect(cid) // Seçim geri çağrısı
            }
            return tv // Satır görünümünü döndür
        }
    }
}
