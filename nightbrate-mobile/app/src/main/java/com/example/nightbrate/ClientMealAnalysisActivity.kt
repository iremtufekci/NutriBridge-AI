package com.example.nightbrate // Paket tanımı

import android.Manifest // İzin sabitleri
import android.content.pm.PackageManager // İzin kontrolü
import android.content.res.ColorStateList // Chip arka plan rengi
import android.net.Uri // Dosya URI'si
import android.os.Bundle // Activity durum paketi
import android.provider.OpenableColumns // Dosya adı sütunu
import android.view.View // Görünüm temel sınıfı
import android.widget.ImageView // Önizleme görseli
import android.widget.ProgressBar // Yükleniyor göstergesi
import android.widget.TextView // Metin görünümü
import android.widget.Toast // Kısa bildirim
import androidx.activity.result.contract.ActivityResultContracts // Activity result API
import androidx.appcompat.app.AppCompatActivity // Temel Activity
import androidx.core.content.ContextCompat // İzin/renk yardımcıları
import androidx.core.content.FileProvider // Kamera dosya URI'si
import androidx.lifecycle.lifecycleScope // Yaşam döngüsü coroutine
import coil.load // Görsel yükleme
import com.google.android.material.card.MaterialCardView // Material kart
import com.google.android.material.chip.Chip // Yiyecek chip'i
import com.google.android.material.chip.ChipGroup // Chip grubu
import kotlinx.coroutines.Dispatchers // IO dispatcher
import kotlinx.coroutines.launch // Coroutine başlat
import kotlinx.coroutines.withContext // Thread değiştir
import okhttp3.MediaType.Companion.toMediaTypeOrNull // MIME tipi
import okhttp3.MultipartBody // Multipart yükleme
import okhttp3.RequestBody.Companion.toRequestBody // İstek gövdesi
import org.json.JSONObject // Hata JSON
import retrofit2.Response // HTTP yanıt
import java.io.File // Geçici kamera dosyası
import java.io.IOException // Ağ hatası
import java.net.SocketTimeoutException // Zaman aşımı

class ClientMealAnalysisActivity : AppCompatActivity() { // Öğün fotoğraf analizi ekranı

    private val maxBytes = 5 * 1024 * 1024 // Maksimum dosya boyutu (5 MB)

    private lateinit var cardMealPreview: MaterialCardView // Önizleme kartı
    private lateinit var imgPreview: ImageView // Önizleme görseli
    private lateinit var cardMealLoading: MaterialCardView // Yükleniyor kartı
    private lateinit var progress: ProgressBar // İlerleme çubuğu
    private lateinit var tvError: TextView // Hata metni
    private lateinit var cardResult: MaterialCardView // Sonuç kartı
    private lateinit var tvKcalValue: TextView // Tahmini kalori
    private lateinit var chipGroupFoods: ChipGroup // Tespit edilen yiyecekler
    private lateinit var tvMealLogId: TextView // Kayıt ID
    private lateinit var tvAnalysisSource: TextView // Analiz kaynağı

    private var cameraOutputFile: File? = null // Kamera çıktı dosyası

    private val pickGallery = // Galeriden görsel seç
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) uploadUri(uri) // Seçildiyse yükle
        }

    private val takePicture = // Kamera ile fotoğraf çek
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val f = cameraOutputFile // Geçici dosya
            if (success && f != null && f.exists()) { // Başarılı çekim
                val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", f)
                uploadUri(uri) // Dosyayı yükle
            }
        }

    private val requestCameraPermission = // Kamera izni iste
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchCameraCapture() else // İzin verildiyse kamera aç
                Toast.makeText(this, "Kamera izni gerekli.", Toast.LENGTH_LONG).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) { // Activity oluşturulduğunda
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_meal_analysis) // Öğün analizi layout'u
        ClientBottomBarHelper.bind(this, 3) // Alt çubuk: Öğün analizi sekmesi

        cardMealPreview = findViewById(R.id.cardMealPreview) // Önizleme kartı bağla
        imgPreview = findViewById(R.id.imgMealPreview)
        cardMealLoading = findViewById(R.id.cardMealLoading)
        progress = findViewById(R.id.progressMealAnalysis)
        tvError = findViewById(R.id.tvMealAnalysisError)
        cardResult = findViewById(R.id.cardMealResult)
        tvKcalValue = findViewById(R.id.tvMealKcalValue)
        chipGroupFoods = findViewById(R.id.chipGroupMealFoods)
        tvMealLogId = findViewById(R.id.tvMealLogId)
        tvAnalysisSource = findViewById(R.id.tvAnalysisSource)

        cardMealPreview.visibility = View.GONE // Başlangıçta önizleme gizli

        findViewById<View>(R.id.btnGallery).setOnClickListener { // Galeri butonu
            pickGallery.launch("image/*") // Görsel seçici aç
        }
        findViewById<View>(R.id.btnCamera).setOnClickListener { openCamera() } // Kamera butonu
    }

    private fun openCamera() { // Kamera aç (izin kontrolü ile)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission.launch(Manifest.permission.CAMERA) // İzin iste
            return
        }
        launchCameraCapture() // İzin var, kamera başlat
    }

    private fun launchCameraCapture() { // Kamera yakalama başlat
        val f = File(cacheDir, "meal_capture_${System.currentTimeMillis()}.jpg") // Geçici dosya
        cameraOutputFile = f // Referansı sakla
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", f) // Content URI
        takePicture.launch(uri) // Kamera intent
    }

    private fun uploadUri(uri: Uri) { // Seçilen URI'yi sunucuya yükle ve analiz et
        tvError.visibility = View.GONE // Önceki hata gizle
        cardResult.visibility = View.GONE // Önceki sonuç gizle
        chipGroupFoods.removeAllViews() // Eski chip'leri temizle

        lifecycleScope.launch { // Yükleme coroutine
            try {
                val tripleResult = withContext(Dispatchers.IO) { // IO thread'de dosya oku
                    runCatching {
                        val cr = contentResolver // Content resolver
                        val mime = cr.getType(uri) ?: "image/jpeg" // MIME tipi
                        require(mime in setOf("image/jpeg", "image/png", "image/jpg")) { "Sadece JPG veya PNG seçin." }
                        val bytes = cr.openInputStream(uri)?.use { it.readBytes() } // Dosya baytları
                            ?: error("Dosya okunamadı.")
                        require(bytes.size <= maxBytes) { "Dosya en fazla 5 MB olabilir." } // Boyut kontrolü
                        val fileName = queryDisplayName(uri).ifBlank { "meal.jpg" } // Dosya adı
                        Triple(bytes, mime, fileName) // Yükleme verisi
                    }
                }

                val (bytes, mime, fileName) = tripleResult.getOrElse { // Okuma hatası
                    Toast.makeText(this@ClientMealAnalysisActivity, it.message ?: "Hata", Toast.LENGTH_LONG).show()
                    return@launch
                }

                imgPreview.load(uri) // Önizlemeyi göster
                cardMealPreview.visibility = View.VISIBLE

                cardMealLoading.visibility = View.VISIBLE // Analiz yükleniyor
                val response = withContext(Dispatchers.IO) { // API çağrısı
                    val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("photo", fileName, body) // Multipart parça
                    RetrofitClient.instance.uploadMealPhoto(part) // Yükleme API
                }

                if (response.isSuccessful) { // Başarılı yanıt
                    val data = response.body()
                    if (data != null) {
                        showResult(data) // Sonuçları göster
                    }
                } else { // HTTP hatası
                    tvError.text = readErrorMessage(response)
                    tvError.visibility = View.VISIBLE
                }
            } catch (e: SocketTimeoutException) { // Zaman aşımı
                val msg = "Sunucu yanıt vermedi (zaman aşımı). Backend çalışıyor mu ve emülatörde 10.0.2.2 adresini kontrol edin."
                Toast.makeText(this@ClientMealAnalysisActivity, msg, Toast.LENGTH_LONG).show()
                tvError.text = msg
                tvError.visibility = View.VISIBLE
            } catch (e: IOException) { // Ağ hatası
                val msg = e.message?.takeIf { it.isNotBlank() } ?: "Ağ bağlantısı hatası."
                Toast.makeText(this@ClientMealAnalysisActivity, msg, Toast.LENGTH_LONG).show()
                tvError.text = msg
                tvError.visibility = View.VISIBLE
            } catch (e: Exception) { // Genel hata
                val msg = e.message ?: "Yükleme başarısız."
                Toast.makeText(this@ClientMealAnalysisActivity, msg, Toast.LENGTH_LONG).show()
                tvError.text = msg
                tvError.visibility = View.VISIBLE
            } finally {
                cardMealLoading.visibility = View.GONE // Yükleniyor gizle
            }
        }
    }

    private fun showResult(data: MealPhotoAnalysisResponse) { // Analiz sonuçlarını UI'ya bağla
        cardResult.visibility = View.VISIBLE // Sonuç kartını göster
        tvKcalValue.text = data.estimatedCalories.toString() // Tahmini kalori
        chipGroupFoods.removeAllViews() // Eski chip'leri temizle

        val chipBg = ContextCompat.getColor(this, R.color.white) // Chip arka plan
        val chipText = ContextCompat.getColor(this, R.color.text_primary) // Chip metin rengi
        val cornerDp = 8f * resources.displayMetrics.density // Köşe yarıçapı

        for (food in data.detectedFoods) { // Her tespit edilen yiyecek için chip
            val chip = Chip(this).apply {
                text = food // Yiyecek adı
                isClickable = false
                isCheckable = false
                isFocusable = false
                chipBackgroundColor = ColorStateList.valueOf(chipBg)
                setTextColor(chipText)
                chipStrokeWidth = 0f
                chipCornerRadius = cornerDp
                textSize = 14f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                elevation = 1f * resources.displayMetrics.density
            }
            chipGroupFoods.addView(chip) // Gruba ekle
        }

        val id = data.mealLogId // Kayıt ID
        if (!id.isNullOrBlank()) {
            tvMealLogId.visibility = View.VISIBLE
            tvMealLogId.text = "Kayıt ID: $id"
        } else {
            tvMealLogId.visibility = View.GONE
        }

        when { // Analiz kaynağı etiketi
            isRealAiSource(data.analysisSource) -> {
                tvAnalysisSource.visibility = View.VISIBLE
                tvAnalysisSource.text = "Analiz kaynağı: yapay zeka hizmeti (Groq)"
                tvAnalysisSource.setTextColor(0xFF1D4ED8.toInt()) // Mavi
            }
            isMockNetworkSource(data.analysisSource) -> {
                tvAnalysisSource.visibility = View.VISIBLE
                tvAnalysisSource.text =
                    "Analiz kaynağı: yerel simülasyon (Groq servisine bağlanılamadı — ağ/DNS hatası)"
                tvAnalysisSource.setTextColor(0xFFB45309.toInt()) // Turuncu
            }
            data.analysisSource?.lowercase() == "mock" -> {
                tvAnalysisSource.visibility = View.VISIBLE
                tvAnalysisSource.text =
                    "Analiz kaynağı: yerel simülasyon (Groq anahtarı tanımlı değil)"
                tvAnalysisSource.setTextColor(0xFFB45309.toInt()) // Turuncu
            }
            else -> {
                tvAnalysisSource.visibility = View.GONE
            }
        }

        val path = data.photoUrl // Sunucu fotoğraf yolu
        if (!path.isNullOrBlank()) {
            val url = if (path.startsWith("http")) path else "${RetrofitClient.API_BASE_URL.trimEnd('/')}/${path.trimStart('/')}" // Tam URL
            imgPreview.load(url) // Sunucudan görsel yükle
            cardMealPreview.visibility = View.VISIBLE
        }
    }

    private fun queryDisplayName(uri: Uri): String { // URI'den dosya adını sorgula
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0) return c.getString(i).orEmpty()
            }
        }
        return "" // Bulunamadı
    }

    private fun readErrorMessage(response: Response<*>): String { // HTTP hata mesajını oku
        val raw = response.errorBody()?.string().orEmpty()
        return try {
            JSONObject(raw).optString("message").ifBlank { "İstek başarısız (${response.code()})." }
        } catch (_: Exception) {
            raw.ifBlank { "İstek başarısız (${response.code()})." }
        }
    }
}
