package com.example.nightbrate // Paket tanımı

import android.os.Bundle // Activity durum paketi
import android.view.View // Görünüm temel sınıfı
import android.widget.EditText // Metin girişi
import android.widget.LinearLayout // Dikey düzen
import android.widget.ProgressBar // Yükleniyor göstergesi
import android.widget.RadioButton // Tarif seçim radio
import android.widget.RadioGroup // Tercih radio grubu
import android.widget.TextView // Metin görünümü
import androidx.appcompat.app.AppCompatActivity // Temel Activity
import androidx.lifecycle.lifecycleScope // Yaşam döngüsü coroutine
import com.google.android.material.button.MaterialButton // Material buton
import kotlinx.coroutines.Dispatchers // IO dispatcher
import kotlinx.coroutines.launch // Coroutine başlat
import kotlinx.coroutines.withContext // Thread değiştir
import org.json.JSONObject // Hata JSON
import retrofit2.Response // HTTP yanıt

class ClientAiKitchenActivity : AppCompatActivity() { // Yapay zeka mutfak / tarif ekranı

    private var lastRecipes: List<KitchenChefRecipeItem> = emptyList() // Son üretilen tarifler
    private var lastSource: String? = null // Analiz kaynağı (gemini/mock)
    private var lastIngredients = "" // Son malzeme girişi
    private var lastPreference = "" // Son tercih (vegan vb.)
    private var lastKcal = 0 // Son hedef kalori
    private var selectedRecipeIndex: Int? = null // Paylaşım için seçili tarif indeksi
    private var canShareToday = true // Bugün paylaşım hakkı var mı

    override fun onCreate(savedInstanceState: Bundle?) { // Activity oluşturulduğunda
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_ai_kitchen) // AI mutfak layout'u
        ClientBottomBarHelper.bind(this, 5) // Alt çubuk: AI mutfak sekmesi

        val etIngredients = findViewById<EditText>(R.id.etAiKitchenIngredients) // Malzeme girişi
        val etKcal = findViewById<EditText>(R.id.etAiKitchenKcal) // Hedef kalori girişi
        val rg = findViewById<RadioGroup>(R.id.rgAiKitchenPreference) // Tercih radio grubu
        val btn = findViewById<MaterialButton>(R.id.btnAiKitchenGenerate) // Tarif üret butonu
        val btnShare = findViewById<MaterialButton>(R.id.btnAiKitchenShare) // Paylaş butonu
        val progress = findViewById<ProgressBar>(R.id.progressAiKitchen) // Yükleniyor
        val tvError = findViewById<TextView>(R.id.tvAiKitchenError) // Hata metni
        val tvSource = findViewById<TextView>(R.id.tvAiKitchenSource) // Kaynak etiketi
        val tvShareHint = findViewById<TextView>(R.id.tvAiKitchenShareHint) // Paylaşım ipucu
        val tvSaveInfo = findViewById<TextView>(R.id.tvAiKitchenSaveInfo) // Kayıt bilgisi
        val tvDailyLimit = findViewById<TextView>(R.id.tvAiKitchenDailyLimit) // Günlük limit uyarısı
        val llResults = findViewById<LinearLayout>(R.id.llAiKitchenResults) // Tarif listesi

        loadShareStatus(tvDailyLimit, btnShare) // Paylaşım durumunu API'den yükle

        btn.setOnClickListener { // Tarif üret tıklama
            tvError.visibility = View.GONE // Hata gizle
            tvSaveInfo.visibility = View.GONE // Kayıt bilgisi gizle
            clearResults(llResults, tvShareHint, btnShare) // Önceki sonuçları temizle

            val ing = etIngredients.text?.toString()?.trim().orEmpty() // Malzemeler
            val kcalStr = etKcal.text?.toString()?.trim().orEmpty() // Kalori string
            val kcal = kcalStr.toIntOrNull() // Kalori sayı
            val rid = rg.checkedRadioButtonId // Seçili radio ID
            val checked = if (rid != View.NO_ID) findViewById<View>(rid).tag?.toString() else null // Tercih değeri

            if (ing.isEmpty()) { // Malzeme zorunlu
                tvError.text = "Malzemeler zorunlu."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (checked.isNullOrBlank()) { // Tercih zorunlu
                tvError.text = "Bir tercih seçin."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (kcal == null || kcal < 200 || kcal > 5000) { // Kalori aralığı
                tvError.text = "Hedef kalori 200–5000 arası olmalı."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            progress.visibility = View.VISIBLE // Yükleniyor göster
            btn.isEnabled = false // Üret butonunu kilitle

            lifecycleScope.launch { // Tarif üretme API
                val resp = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.generateKitchenRecipes( // Tarif üret API
                        KitchenChefGenerateRequest(
                            ingredients = ing,
                            preference = checked,
                            targetCalories = kcal
                        )
                    )
                }
                progress.visibility = View.GONE // Yükleniyor gizle
                btn.isEnabled = true // Butonu aç

                if (resp.isSuccessful) { // Başarılı yanıt
                    val data = resp.body()
                    if (data != null && data.recipes.isNotEmpty()) { // Tarifler var
                        lastRecipes = data.recipes // Son tarifleri sakla
                        lastSource = data.source
                        lastIngredients = ing
                        lastPreference = checked
                        lastKcal = kcal
                        selectedRecipeIndex = null // Seçim sıfırla

                        when { // Kaynak etiketi
                            isRealAiSource(data.source) -> {
                                tvSource.text = "Kaynak: yapay zeka hizmeti (Groq)"
                                tvSource.setTextColor(0xFF1D4ED8.toInt()) // Mavi
                                tvSource.visibility = View.VISIBLE
                            }
                            isMockNetworkSource(data.source) -> {
                                tvSource.text =
                                    "Kaynak: yerel önizleme (Groq servisine bağlanılamadı — ağ/DNS hatası)"
                                tvSource.setTextColor(0xFFB45309.toInt()) // Turuncu
                                tvSource.visibility = View.VISIBLE
                            }
                            data.source?.lowercase() == "mock" -> {
                                tvSource.text =
                                    "Kaynak: yerel önizleme (sunucuda yapay zeka anahtarı tanımlı değilse)"
                                tvSource.setTextColor(0xFFB45309.toInt()) // Turuncu
                                tvSource.visibility = View.VISIBLE
                            }
                            else -> tvSource.visibility = View.GONE
                        }

                        tvShareHint.visibility = View.VISIBLE // Paylaşım ipucu göster
                        if (canShareToday) { // Paylaşım hakkı varsa
                            btnShare.visibility = View.VISIBLE
                            btnShare.isEnabled = true
                        } else {
                            btnShare.visibility = View.GONE
                        }

                        for ((i, r) in data.recipes.withIndex()) { // Her tarif için kart oluştur
                            val card = layoutInflater.inflate(R.layout.item_ai_kitchen_recipe, llResults, false)
                            card.findViewById<TextView>(R.id.tvRecipeTitle).text = r.title // Başlık
                            card.findViewById<TextView>(R.id.tvRecipeMeta).text =
                                buildString {
                                    append("${r.estimatedCalories} kkal") // Kalori
                                    r.prepTimeMinutes?.let { if (it > 0) append("  ·  ~${it} dk") } // Hazırlık süresi
                                }
                            r.description?.takeIf { it.isNotBlank() }?.let { // Açıklama varsa
                                card.findViewById<TextView>(R.id.tvRecipeDesc).apply {
                                    text = it
                                    visibility = View.VISIBLE
                                }
                            }
                            val ingText = r.ingredients.joinToString("\n") { "• $it" } // Malzeme listesi
                            card.findViewById<TextView>(R.id.tvRecipeIngredients).text = ingText
                            val stepsText = r.steps.mapIndexed { idx, s -> "${idx + 1}. $s" }.joinToString("\n") // Adımlar
                            card.findViewById<TextView>(R.id.tvRecipeSteps).text = stepsText

                            val rb = card.findViewById<RadioButton>(R.id.rbRecipeSelect) // Tarif seç radio
                            rb.isEnabled = canShareToday // Paylaşım yoksa devre dışı
                            rb.setOnClickListener { // Tarif seçimi
                                if (!canShareToday) return@setOnClickListener
                                selectedRecipeIndex = i // Seçili indeks
                                for (j in 0 until llResults.childCount) { // Tek seçim: diğerlerini kapat
                                    val other = llResults.getChildAt(j)
                                        .findViewById<RadioButton>(R.id.rbRecipeSelect)
                                    other.isChecked = j == i
                                }
                            }
                            llResults.addView(card) // Listeye ekle
                        }
                    }
                } else { // HTTP hatası
                    tvError.text = readErrorMessage(resp)
                    tvError.visibility = View.VISIBLE
                }
            }
        }

        btnShare.setOnClickListener { // Tarifi diyetisyene paylaş
            tvError.visibility = View.GONE
            tvSaveInfo.visibility = View.GONE

            if (!canShareToday) { // Günlük limit aşıldı
                tvError.text = "Bugün zaten bir tarif paylaştınız. Yarın tekrar deneyebilirsiniz."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (lastRecipes.isEmpty()) { // Önce tarif üretilmeli
                tvError.text = "Önce tarif üretin, sonra paylaşın."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            val idx = selectedRecipeIndex // Seçili tarif indeksi
            if (idx == null || idx !in lastRecipes.indices) { // Tarif seçilmedi
                tvError.text = "Paylaşmak için listelerden yalnızca bir tarif seçin (radio)."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            btnShare.isEnabled = false // Paylaş butonunu kilitle
            progress.visibility = View.VISIBLE // Yükleniyor

            lifecycleScope.launch { // Kaydetme API
                val resp = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.saveKitchenRecipes( // Tarif kaydet API
                        KitchenChefSaveRequest(
                            ingredients = lastIngredients,
                            preference = lastPreference,
                            targetCalories = lastKcal,
                            source = lastSource ?: "mock",
                            selectedRecipes = listOf(lastRecipes[idx]) // Seçili tek tarif
                        )
                    )
                }
                progress.visibility = View.GONE

                if (resp.isSuccessful) { // Kayıt başarılı
                    canShareToday = false // Bugünkü hak kullanıldı
                    btnShare.visibility = View.GONE
                    btnShare.isEnabled = false
                    updateDailyLimitUi(tvDailyLimit, btnShare) // Limit UI güncelle
                    tvSaveInfo.text =
                        "Seçtiğiniz tarif kaydedildi. Diyetisyeniniz “Yapay zeka denetimi” ekranından görebilir."
                    tvSaveInfo.visibility = View.VISIBLE
                } else { // Kayıt hatası
                    btnShare.isEnabled = canShareToday
                    tvError.text = readErrorMessage(resp)
                    tvError.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun loadShareStatus(tvDailyLimit: TextView, btnShare: MaterialButton) { // Günlük paylaşım durumunu yükle
        lifecycleScope.launch {
            val status = withContext(Dispatchers.IO) {
                runCatching { RetrofitClient.instance.getKitchenShareStatus() }.getOrNull() // Durum API
            }
            if (status?.isSuccessful == true) {
                val body = status.body()
                canShareToday = body?.canShareToday != false && body?.sharedToday != true // Paylaşım hakkı
            }
            updateDailyLimitUi(tvDailyLimit, btnShare) // UI güncelle
        }
    }

    private fun updateDailyLimitUi(tvDailyLimit: TextView, btnShare: MaterialButton) { // Günlük limit UI'sı
        if (canShareToday) { // Paylaşım hakkı var
            tvDailyLimit.visibility = View.GONE
            if (lastRecipes.isNotEmpty()) { // Tarif varsa paylaş butonu
                btnShare.visibility = View.VISIBLE
                btnShare.isEnabled = true
            }
        } else { // Limit dolmuş
            tvDailyLimit.text = "Bugün zaten bir tarif paylaştınız. Yarın tekrar deneyebilirsiniz."
            tvDailyLimit.visibility = View.VISIBLE
            btnShare.visibility = View.GONE
            btnShare.isEnabled = false
        }
    }

    private fun clearResults( // Sonuç alanını temizle
        llResults: LinearLayout,
        tvShareHint: TextView,
        btnShare: MaterialButton
    ) {
        llResults.removeAllViews() // Tarif kartlarını kaldır
        tvShareHint.visibility = View.GONE // İpucu gizle
        btnShare.visibility = View.GONE // Paylaş gizle
        lastRecipes = emptyList() // Durumu sıfırla
        lastSource = null
        selectedRecipeIndex = null
    }

    private fun readErrorMessage(response: Response<*>): String { // HTTP hata mesajını oku
        val raw = response.errorBody()?.string().orEmpty()
        val msg = try {
            JSONObject(raw).optString("message").ifBlank { "İstek başarısız (${response.code()})." }
        } catch (_: Exception) {
            raw.ifBlank { "İstek başarısız (${response.code()})." }
        }
        return friendlyKitchenError(msg) // Kullanıcı dostu mesaja çevir
    }

    private fun friendlyKitchenError(message: String): String { // AI mutfak hata mesajlarını Türkçeleştir
        val lower = message.lowercase()
        return when {
            lower.contains("high demand") || lower.contains("overloaded") ->
                "Yapay zeka hizmetinde geçici yoğunluk var. Birkaç dakika sonra tekrar deneyin."
            lower.contains("zaman aşımı") || lower.contains("timeout") ->
                "Tarif üretimi uzun sürdü. Lütfen tekrar deneyin."
            lower.contains("bugün zaten") ->
                "Bugün zaten bir tarif paylaştınız. Yarın tekrar deneyebilirsiniz."
            else -> message // Bilinmeyen: ham mesaj
        }
    }
}
