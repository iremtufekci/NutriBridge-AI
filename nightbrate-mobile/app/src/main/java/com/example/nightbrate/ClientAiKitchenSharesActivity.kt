package com.example.nightbrate // Uygulama paketi

import android.os.Bundle // Aktivite durum paketi
import android.view.LayoutInflater // Kart şablonu şişirici
import android.view.View // Görünüm temel sınıfı
import android.widget.EditText // Tarih girişi
import android.widget.LinearLayout // Liste konteyneri
import android.widget.ProgressBar // Yükleme göstergesi
import android.widget.TextView // Metin görünümü
import androidx.appcompat.app.AppCompatActivity // Temel aktivite
import androidx.lifecycle.lifecycleScope // Yaşam döngüsü coroutine kapsamı
import com.google.android.material.button.MaterialButton // Material düğme
import kotlinx.coroutines.Dispatchers // IO iş parçacığı
import kotlinx.coroutines.launch // Asenkron başlatma
import kotlinx.coroutines.withContext // Bağlam değiştirme
import org.json.JSONObject // Hata JSON ayrıştırma
import retrofit2.Response // HTTP yanıtı
import java.text.SimpleDateFormat // Tarih biçimlendirme
import java.util.Calendar // Takvim hesabı
import java.util.Locale // Yerel ayar

class ClientAiKitchenSharesActivity : AppCompatActivity() { // Danışan AI mutfak paylaşımları

    private var skip = 0 // Sayfalama atlama değeri
    private val pageSize = 50 // Sayfa başına kayıt
    private var hasMore = false // Daha fazla kayıt var mı

    override fun onCreate(savedInstanceState: Bundle?) { // Aktivite oluşturulduğunda
        super.onCreate(savedInstanceState) // Üst sınıf başlatması
        setContentView(R.layout.activity_client_ai_kitchen_shares) // Düzeni yükle
        ClientBottomBarHelper.bind(this, 6) // Alt menüyü bağla

        val etFrom = findViewById<EditText>(R.id.etShareFrom) // Başlangıç tarihi
        val etTo = findViewById<EditText>(R.id.etShareTo) // Bitiş tarihi

        findViewById<MaterialButton>(R.id.btnShareLast7).setOnClickListener { applyRange(etFrom, etTo, 7) } // Son 7 gün
        findViewById<MaterialButton>(R.id.btnShareLast30).setOnClickListener { applyRange(etFrom, etTo, 30) } // Son 30 gün
        findViewById<MaterialButton>(R.id.btnShareClear).setOnClickListener { // Tarihleri temizle
            etFrom.setText("") // Başlangıç boş
            etTo.setText("") // Bitiş boş
        }
        findViewById<MaterialButton>(R.id.btnShareLoad).setOnClickListener { load(etFrom, etTo, reset = true) } // Listeyi yükle
        findViewById<MaterialButton>(R.id.btnShareMore).setOnClickListener { load(etFrom, etTo, reset = false) } // Daha fazla

        load(etFrom, etTo, reset = true) // İlk yükleme
    }

    private fun applyRange(etFrom: EditText, etTo: EditText, days: Int) { // Hızlı tarih aralığı
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US) // ISO tarih biçimi
        val cal = Calendar.getInstance() // Bugünün takvimi
        val to = fmt.format(cal.time) // Bitiş bugün
        cal.add(Calendar.DAY_OF_MONTH, -days) // Geriye git
        val from = fmt.format(cal.time) // Başlangıç tarihi
        etFrom.setText(from) // Başlangıç alanına yaz
        etTo.setText(to) // Bitiş alanına yaz
    }

    private fun load(etFrom: EditText, etTo: EditText, reset: Boolean) { // Paylaşımları API'den al
        if (reset) { // Yeni arama ise
            skip = 0 // Sayfalamayı sıfırla
            findViewById<LinearLayout>(R.id.llAiSharesList).removeAllViews() // Listeyi temizle
        }
        val progress = findViewById<ProgressBar>(R.id.progressAiShares) // Yükleme çubuğu
        val err = findViewById<TextView>(R.id.tvAiSharesError) // Hata metni
        val more = findViewById<MaterialButton>(R.id.btnShareMore) // Daha fazla düğmesi
        err.visibility = View.GONE // Hatayı gizle
        progress.visibility = View.VISIBLE // Yükleniyor göster
        more.visibility = View.GONE // Daha fazla düğmesini gizle

        val from = etFrom.text?.toString()?.trim().orEmpty().ifBlank { null } // Filtre başlangıç
        val to = etTo.text?.toString()?.trim().orEmpty().ifBlank { null } // Filtre bitiş
        val nextSkip = if (reset) 0 else skip // Atlama değeri

        lifecycleScope.launch { // Ağ isteği
            val resp: Response<List<KitchenChefShareLogItem>> = withContext(Dispatchers.IO) { // IO'da çağrı
                RetrofitClient.instance.getMyKitchenShares(
                    from = from, // Başlangıç filtresi
                    to = to, // Bitiş filtresi
                    source = null, // Kaynak filtresi yok
                    skip = nextSkip, // Atlama
                    take = pageSize // Sayfa boyutu
                )
            }
            progress.visibility = View.GONE // Yüklemeyi gizle
            val list = if (resp.isSuccessful) resp.body() ?: emptyList() else emptyList() // Kayıt listesi
            if (!resp.isSuccessful) { // Hata yanıtı
                err.text = readErrorMessage(resp) // Hata mesajını oku
                err.visibility = View.VISIBLE // Hatayı göster
            } else { // Başarılı yanıt
                val container = findViewById<LinearLayout>(R.id.llAiSharesList) // Liste konteyneri
                val inflater = LayoutInflater.from(this@ClientAiKitchenSharesActivity) // Şablon şişirici
                for (log in list) { // Her paylaşım kaydı
                    val card = inflater.inflate(R.layout.item_ai_kitchen_share_log, container, false) // Kart şablonu
                    val meta = card.findViewById<TextView>(R.id.tvShareItemMeta) // Üst bilgi satırı
                    val title = card.findViewById<TextView>(R.id.tvShareItemTitle) // Tarif başlığı
                    val body = card.findViewById<TextView>(R.id.tvShareItemQuery) // Detay metni
                    val r = log.selectedRecipes.firstOrNull() // İlk seçilen tarif
                    val srcLabel = if (isRealAiSource(log.source)) "Yapay zeka" else "Yerel örnek" // Kaynak etiketi
                    meta.text = buildString {
                        append(log.createdAtUtc.replace("T", " ").take(19)) // Oluşturma zamanı
                        append("  ·  ") // Ayırıcı
                        append(srcLabel) // Kaynak
                        append("  ·  Hedef: ") // Hedef etiketi
                        append(log.targetCalories) // Hedef kalori
                        append(" kkal") // Birim
                    }
                    title.text = r?.title ?: "Tarif" // Başlık veya varsayılan
                    body.text = buildString {
                        append("Tercih: ${log.preference}\n") // Beslenme tercihi
                        append("Sorgu malzemeler: ${log.ingredients}\n\n") // Sorgu malzemeleri
                        r?.let { x -> // Tarif detayları
                            x.description?.takeIf { it.isNotBlank() }?.let { append("$it\n\n") } // Açıklama
                            append("~${x.estimatedCalories} kkal") // Tahmini kalori
                            x.prepTimeMinutes?.takeIf { it > 0 }?.let { append("  ·  ~$it dk") } // Hazırlık süresi
                            append("\n\n") // Boş satır
                            if (x.ingredients.isNotEmpty()) { // Malzeme listesi
                                append("Malzeme listesi:\n")
                                x.ingredients.forEach { append("• $it\n") } // Her malzeme
                            }
                            if (x.steps.isNotEmpty()) { // Adımlar
                                append("\nAdımlar:\n")
                                x.steps.forEachIndexed { i, s -> append("${i + 1}. $s\n") } // Numaralı adım
                            }
                        }
                    }
                    container.addView(card) // Kartı listeye ekle
                }
                hasMore = list.size == pageSize // Tam sayfa geldiyse devam var
                skip = nextSkip + list.size // Sonraki atlama
                if (hasMore) more.visibility = View.VISIBLE // Daha fazla düğmesini göster
            }
        }
    }

    private fun readErrorMessage(response: Response<*>): String { // API hata mesajını çöz
        val raw = response.errorBody()?.string().orEmpty() // Ham hata gövdesi
        return try {
            JSONObject(raw).optString("message").ifBlank { "İstek başarısız (${response.code()})." } // JSON mesajı
        } catch (_: Exception) { // JSON değilse
            raw.ifBlank { "İstek başarısız (${response.code()})." } // Ham veya varsayılan
        }
    }
}
