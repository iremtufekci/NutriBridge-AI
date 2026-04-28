package com.example.nightbrate

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Response
import java.io.File

class ClientMealAnalysisActivity : AppCompatActivity() {

    private val maxBytes = 5 * 1024 * 1024

    private lateinit var cardMealPreview: MaterialCardView
    private lateinit var imgPreview: ImageView
    private lateinit var cardMealLoading: MaterialCardView
    private lateinit var progress: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var cardResult: MaterialCardView
    private lateinit var tvKcalValue: TextView
    private lateinit var chipGroupFoods: ChipGroup
    private lateinit var tvMealLogId: TextView
    private lateinit var tvAnalysisSource: TextView

    private var cameraOutputFile: File? = null

    private val pickGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) uploadUri(uri)
        }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val f = cameraOutputFile
            if (success && f != null && f.exists()) {
                val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", f)
                uploadUri(uri)
            }
        }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchCameraCapture() else
                Toast.makeText(this, "Kamera izni gerekli.", Toast.LENGTH_LONG).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_meal_analysis)
        ClientBottomBarHelper.bind(this, 3)

        cardMealPreview = findViewById(R.id.cardMealPreview)
        imgPreview = findViewById(R.id.imgMealPreview)
        cardMealLoading = findViewById(R.id.cardMealLoading)
        progress = findViewById(R.id.progressMealAnalysis)
        tvError = findViewById(R.id.tvMealAnalysisError)
        cardResult = findViewById(R.id.cardMealResult)
        tvKcalValue = findViewById(R.id.tvMealKcalValue)
        chipGroupFoods = findViewById(R.id.chipGroupMealFoods)
        tvMealLogId = findViewById(R.id.tvMealLogId)
        tvAnalysisSource = findViewById(R.id.tvAnalysisSource)

        cardMealPreview.visibility = View.GONE

        findViewById<View>(R.id.btnGallery).setOnClickListener {
            pickGallery.launch("image/*")
        }
        findViewById<View>(R.id.btnCamera).setOnClickListener { openCamera() }
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
            return
        }
        launchCameraCapture()
    }

    private fun launchCameraCapture() {
        val f = File(cacheDir, "meal_capture_${System.currentTimeMillis()}.jpg")
        cameraOutputFile = f
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", f)
        takePicture.launch(uri)
    }

    private fun uploadUri(uri: Uri) {
        tvError.visibility = View.GONE
        cardResult.visibility = View.GONE
        chipGroupFoods.removeAllViews()

        lifecycleScope.launch {
            val tripleResult = withContext(Dispatchers.IO) {
                runCatching {
                    val cr = contentResolver
                    val mime = cr.getType(uri) ?: "image/jpeg"
                    require(mime in setOf("image/jpeg", "image/png", "image/jpg")) { "Sadece JPG veya PNG seçin." }
                    val bytes = cr.openInputStream(uri)?.use { it.readBytes() }
                        ?: error("Dosya okunamadı.")
                    require(bytes.size <= maxBytes) { "Dosya en fazla 5 MB olabilir." }
                    val fileName = queryDisplayName(uri).ifBlank { "meal.jpg" }
                    Triple(bytes, mime, fileName)
                }
            }

            val (bytes, mime, fileName) = tripleResult.getOrElse {
                Toast.makeText(this@ClientMealAnalysisActivity, it.message ?: "Hata", Toast.LENGTH_LONG).show()
                return@launch
            }

            imgPreview.load(uri)
            cardMealPreview.visibility = View.VISIBLE

            cardMealLoading.visibility = View.VISIBLE
            val response = withContext(Dispatchers.IO) {
                val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("photo", fileName, body)
                RetrofitClient.instance.uploadMealPhoto(part)
            }
            cardMealLoading.visibility = View.GONE

            if (response.isSuccessful) {
                val data = response.body()
                if (data != null) {
                    showResult(data)
                }
            } else {
                tvError.text = readErrorMessage(response)
                tvError.visibility = View.VISIBLE
            }
        }
    }

    private fun showResult(data: MealPhotoAnalysisResponse) {
        cardResult.visibility = View.VISIBLE
        tvKcalValue.text = data.estimatedCalories.toString()
        chipGroupFoods.removeAllViews()

        val chipBg = ContextCompat.getColor(this, R.color.white)
        val chipText = ContextCompat.getColor(this, R.color.text_primary)
        val cornerDp = 8f * resources.displayMetrics.density

        for (food in data.detectedFoods) {
            val chip = Chip(this).apply {
                text = food
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
            chipGroupFoods.addView(chip)
        }

        val id = data.mealLogId
        if (!id.isNullOrBlank()) {
            tvMealLogId.visibility = View.VISIBLE
            tvMealLogId.text = "Kayıt ID: $id"
        } else {
            tvMealLogId.visibility = View.GONE
        }

        when (data.analysisSource?.lowercase()) {
            "gemini" -> {
                tvAnalysisSource.visibility = View.VISIBLE
                tvAnalysisSource.text = "Analiz kaynağı: Google Gemini"
                tvAnalysisSource.setTextColor(0xFF1D4ED8.toInt())
            }
            "mock" -> {
                tvAnalysisSource.visibility = View.VISIBLE
                tvAnalysisSource.text =
                    "Analiz kaynağı: Yerel simülasyon (API'de Gemini anahtarı tanımlı değil)"
                tvAnalysisSource.setTextColor(0xFFB45309.toInt())
            }
            else -> {
                tvAnalysisSource.visibility = View.GONE
            }
        }

        val path = data.photoUrl
        if (!path.isNullOrBlank()) {
            val url = if (path.startsWith("http")) path else "${RetrofitClient.API_BASE_URL.trimEnd('/')}/${path.trimStart('/')}"
            imgPreview.load(url)
            cardMealPreview.visibility = View.VISIBLE
        }
    }

    private fun queryDisplayName(uri: Uri): String {
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0) return c.getString(i).orEmpty()
            }
        }
        return ""
    }

    private fun readErrorMessage(response: Response<*>): String {
        val raw = response.errorBody()?.string().orEmpty()
        return try {
            JSONObject(raw).optString("message").ifBlank { "İstek başarısız (${response.code()})." }
        } catch (_: Exception) {
            raw.ifBlank { "İstek başarısız (${response.code()})." }
        }
    }
}
