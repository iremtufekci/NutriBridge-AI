package com.example.nightbrate // Uygulama paketi

import android.net.Uri // Dosya URI'si
import android.os.Bundle // Aktivite durum paketi
import android.provider.OpenableColumns // Dosya adı sütunu
import android.widget.Button // Düğme bileşeni
import android.widget.EditText // Metin girişi
import android.widget.TextView // Metin görünümü
import android.widget.Toast // Kısa bildirim
import androidx.activity.result.contract.ActivityResultContracts // Dosya seçici sözleşmesi
import androidx.appcompat.app.AppCompatActivity // Temel aktivite
import androidx.lifecycle.lifecycleScope // Yaşam döngüsü coroutine kapsamı
import kotlinx.coroutines.Dispatchers // IO iş parçacığı
import kotlinx.coroutines.launch // Asenkron başlatma
import kotlinx.coroutines.withContext // Bağlam değiştirme
import okhttp3.MediaType.Companion.toMediaTypeOrNull // MIME tipi
import okhttp3.MultipartBody // Çok parçalı istek gövdesi
import okhttp3.RequestBody.Companion.asRequestBody // Dosyayı gövdeye çevir
import okhttp3.RequestBody.Companion.toRequestBody // Metni gövdeye çevir
import java.io.File // Geçici dosya

class RegisterDietitianActivity : AppCompatActivity() { // Diyetisyen kayıt ekranı

    private var diplomaUri: Uri? = null // Seçilen diploma dosyası

    private val pickDiploma = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> // Belge seçici
        if (uri != null) { // Dosya seçildiyse
            diplomaUri = uri // URI'yi sakla
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION // Kalıcı okuma izni
                )
            } catch (_: SecurityException) { // İzin verilemezse
                // Bazı sağlayıcılarda kalıcı izin gerekmez
            }
            findViewById<TextView>(R.id.tvDiplomaFile).text = resolveDisplayName(uri) // Dosya adını göster
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) { // Aktivite oluşturulduğunda
        super.onCreate(savedInstanceState) // Üst sınıf başlatması
        setContentView(R.layout.activity_register_dietitian) // Kayıt düzenini yükle

        val etFirstName = findViewById<EditText>(R.id.etFirstName) // Ad alanı
        val etLastName = findViewById<EditText>(R.id.etLastName) // Soyad alanı
        val etEmail = findViewById<EditText>(R.id.etEmail) // E-posta alanı
        val etPassword = findViewById<EditText>(R.id.etPassword) // Şifre alanı
        val etDiploma = findViewById<EditText>(R.id.etDiploma) // Diploma no alanı
        val etClinic = findViewById<EditText>(R.id.etClinic) // Klinik adı alanı
        val btnPickDiploma = findViewById<Button>(R.id.btnPickDiploma) // Dosya seç düğmesi
        val btnRegister = findViewById<Button>(R.id.btnRegister) // Kayıt düğmesi
        val tvBackToLogin = findViewById<TextView>(R.id.tvBackToLogin) // Girişe dön bağlantısı

        btnPickDiploma.setOnClickListener { // Diploma seçiciyi aç
            pickDiploma.launch(arrayOf("application/pdf", "image/*")) // PDF veya resim
        }

        tvBackToLogin.setOnClickListener { finish() } // Önceki ekrana dön

        btnRegister.setOnClickListener { // Kayıt gönder
            val firstName = etFirstName.text.toString().trim() // Ad değeri
            val lastName = etLastName.text.toString().trim() // Soyad değeri
            val email = etEmail.text.toString().trim() // E-posta değeri
            val password = etPassword.text.toString().trim() // Şifre değeri
            val diploma = etDiploma.text.toString().trim() // Diploma no
            val clinic = etClinic.text.toString().trim() // Klinik adı
            val uri = diplomaUri // Seçili dosya

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() ||
                password.isEmpty() || diploma.isEmpty() || clinic.isEmpty() // Zorunlu alan kontrolü
            ) {
                Toast.makeText(this, "Lütfen tüm alanları doldurun!", Toast.LENGTH_SHORT).show() // Uyarı göster
                return@setOnClickListener // İşlemi durdur
            }
            if (uri == null) { // Dosya seçilmemişse
                Toast.makeText(this, "Diploma veya sertifika belgesi seçin.", Toast.LENGTH_SHORT).show() // Uyarı
                return@setOnClickListener // İşlemi durdur
            }

            lifecycleScope.launch { // Ağ isteğini başlat
                try {
                    val response = withContext(Dispatchers.IO) { // IO iş parçacığında
                        val displayName = resolveDisplayName(uri) // Görünen dosya adı
                        val mime = contentResolver.getType(uri) ?: "application/pdf" // MIME tipi
                        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } // Dosya baytları
                            ?: throw IllegalArgumentException("Dosya okunamadı.") // Okuma hatası
                        if (bytes.size > 10 * 1024 * 1024) throw IllegalArgumentException("Dosya en fazla 10 MB olabilir.") // Boyut sınırı

                        val tmp = File.createTempFile("diploma_", ".upload", cacheDir) // Geçici dosya
                        tmp.writeBytes(bytes) // Baytları yaz
                        val filePart = MultipartBody.Part.createFormData(
                            "diploma",
                            displayName,
                            tmp.asRequestBody(mime.toMediaTypeOrNull()) // Dosya parçası
                        )
                        val text = "text/plain".toMediaTypeOrNull() // Metin alanları tipi
                        RetrofitClient.instance.registerDietitian(
                            firstName = firstName.toRequestBody(text), // Ad alanı
                            lastName = lastName.toRequestBody(text), // Soyad alanı
                            email = email.toRequestBody(text), // E-posta alanı
                            password = password.toRequestBody(text), // Şifre alanı
                            diplomaNo = diploma.toRequestBody(text), // Diploma no
                            clinicName = clinic.toRequestBody(text), // Klinik adı
                            diploma = filePart // Dosya eki
                        ).also { tmp.delete() } // Geçici dosyayı sil
                    }
                    if (response.isSuccessful) { // Kayıt başarılı
                        Toast.makeText(
                            this@RegisterDietitianActivity,
                            "Başvurunuz alındı. Yönetici onayından sonra giriş yapabilirsiniz.",
                            Toast.LENGTH_LONG // Bilgi mesajı
                        ).show()
                        finish() // Ekranı kapat
                    } else { // Sunucu hatası
                        Toast.makeText(this@RegisterDietitianActivity, "Hata: ${response.code()}", Toast.LENGTH_SHORT).show() // Kod göster
                    }
                } catch (e: Exception) { // İstisna durumu
                    Toast.makeText(
                        this@RegisterDietitianActivity,
                        e.message ?: "Bağlantı sorunu!",
                        Toast.LENGTH_LONG // Hata mesajı
                    ).show()
                }
            }
        }
    }

    private fun resolveDisplayName(uri: Uri): String { // URI'den dosya adını çöz
        contentResolver.query(uri, null, null, null, null)?.use { c -> // İçerik sorgusu
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME) // Ad sütunu indeksi
            if (c.moveToFirst() && idx >= 0) return c.getString(idx) ?: "diploma.pdf" // Görünen ad
        }
        return "diploma.pdf" // Varsayılan ad
    }
}
