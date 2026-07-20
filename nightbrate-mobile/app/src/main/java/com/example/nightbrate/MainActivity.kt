package com.example.nightbrate // Paket

import android.content.Intent // Başka Activity'ye geçiş
import android.os.Bundle // Activity yaşam döngüsü verisi
import android.view.View // Genel görünüm referansı
import android.widget.EditText // E-posta/şifre kutuları
import android.widget.LinearLayout // Kayıt link satırları
import android.widget.TextView // Metin etiketleri
import android.widget.Toast // Kısa bildirim
import androidx.appcompat.app.AppCompatActivity // Temel Activity sınıfı
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen // Açılış splash ekranı
import androidx.lifecycle.lifecycleScope // Activity ömrüne bağlı coroutine
import com.example.nightbrate.ActivityWindowHelper.applyContentRootBackground // Arka plan rengi
import com.example.nightbrate.ActivityWindowHelper.applyStandardContentWindow // Durum çubuğu vb.
import kotlinx.coroutines.launch // Arka planda API çağrısı
import org.json.JSONObject // Hata gövdesinden message okuma

/** Uygulama giriş (launcher) ekranı — login ve kayıt yönlendirmesi */
class MainActivity : AppCompatActivity() {

    private var loginLayoutReady = false // Splash ekranını layout hazır olunca kapat

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen() // Android 12+ splash API
        splash.setKeepOnScreenCondition { !loginLayoutReady } // Layout yüklenene kadar splash göster
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_Nightbrate_NoActionBar) // Üst bar olmayan tema
        applyStandardContentWindow() // Pencere renkleri
        setContentView(R.layout.activity_main) // Login XML layout'unu yükle
        applyContentRootBackground() // İçerik arka planı
        loginLayoutReady = true // Splash artık kapanabilir
        window.decorView.post { // İlk karede siyah ekran riski için yeniden çiz
            applyStandardContentWindow()
            applyContentRootBackground()
            window.decorView.invalidate()
            findViewById<View>(android.R.id.content)?.invalidate()
        }
        Diagnostic.log("MainActivity.onCreate layout yüklendi") // Debug log

        val etEmail = findViewById<EditText>(R.id.etEmail) // E-posta alanı
        val etPassword = findViewById<EditText>(R.id.etPassword) // Şifre alanı
        val btnLogin = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnLogin) // Giriş butonu
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword) // Şifremi unuttum
        val llRegisterClient = findViewById<LinearLayout>(R.id.llRegisterClient) // Danışan kayıt tıklanabilir alan
        val llRegisterDietitian = findViewById<LinearLayout>(R.id.llRegisterDietitian) // Diyetisyen kayıt alanı

        btnLogin.setOnClickListener { // Giriş butonuna basılınca
            val email = etEmail.text.toString().trim() // Boşluksuz e-posta
            val password = etPassword.text.toString().trim() // Boşluksuz şifre
            if (email.isEmpty() || password.isEmpty()) { // Validasyon
                Toast.makeText(this, "Lütfen tüm alanları doldurun!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            performLogin(email, password) // API login
        }

        tvForgotPassword.setOnClickListener { // Henüz implemente değil
            Toast.makeText(this, "Şifre sıfırlama yakında eklenecek.", Toast.LENGTH_SHORT).show()
        }

        llRegisterClient.setOnClickListener { // Danışan kayıt ekranına git
            startActivity(Intent(this, RegisterClientActivity::class.java))
        }

        llRegisterDietitian.setOnClickListener { // Diyetisyen kayıt ekranına git
            startActivity(Intent(this, RegisterDietitianActivity::class.java))
        }
    }

    override fun onResume() { // Ekrana geri dönünce
        super.onResume()
        applyStandardContentWindow() // Tema tutarlılığı
        applyContentRootBackground()
        Diagnostic.log("MainActivity.onResume")
    }

    private fun performLogin(email: String, password: String) { // Backend'e giriş isteği
        lifecycleScope.launch { // IO ana thread'i bloklamaz
            try {
                val response = RetrofitClient.instance.login(LoginRequest(email, password)) // POST api/Auth/login
                if (response.isSuccessful) { // 2xx cevap
                    val userResponse = response.body() // JSON → LoginResponse
                    val role = userResponse?.role?.lowercase() ?: "" // admin / dietitian / client
                    val token = userResponse?.token.orEmpty() // JWT

                    val authPrefs = getSharedPreferences(ThemeUtils.PREF_NAME, MODE_PRIVATE) // Kalıcı depolama
                    authPrefs.edit()
                        .putString("token", token) // AuthInterceptor bunu okur
                        .putString("role", role) // Rol bilgisi
                        .putString("email", email) // E-posta hatırlama
                        .apply()

                    ThemeUtils.applyLightTheme(authPrefs) // Varsayılan açık tema
                    Toast.makeText(this@MainActivity, "Giriş başarılı!", Toast.LENGTH_SHORT).show()

                    val intent = when (role) { // Role göre ana panel
                        "admin" -> Intent(this@MainActivity, AdminDashboardActivity::class.java)
                        "dietitian", "diyetisyen" -> Intent(this@MainActivity, DietitianDashboardActivity::class.java)
                        else -> Intent(this@MainActivity, ClientDashboardActivity::class.java) // Varsayılan danışan
                    }
                    intent.putExtra("USERNAME", email.substringBefore("@")) // Karşılama adı için
                    startActivity(intent) // Panel Activity'sini aç
                    finish() // Login ekranını kapat (geri tuşu login'e dönmesin)
                } else { // 4xx/5xx
                    val errorBody = response.errorBody()?.string() // Ham hata JSON
                    val backendMessage = try {
                        JSONObject(errorBody ?: "{}").optString("message") // message alanı
                    } catch (_: Exception) {
                        ""
                    }
                    val message = backendMessage.ifBlank { "Giriş başarısız: E-posta veya şifre hatalı!" }
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) { // Ağ/timeout hatası
                Toast.makeText(this@MainActivity, "Bağlantı Hatası: Sunucuya ulaşılamıyor.", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }
}
