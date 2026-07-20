package com.example.nightbrate // Paket tanımı

import android.os.Bundle // Activity durum verisi
import android.widget.Button // Kayıt butonu
import android.widget.EditText // Metin giriş alanları
import android.widget.TextView // Geri dön linki
import android.widget.Toast // Kısa bildirim
import androidx.appcompat.app.AppCompatActivity // Temel Activity
import androidx.lifecycle.lifecycleScope // Activity ömrüne bağlı coroutine
import kotlinx.coroutines.launch // Arka planda API çağrısı

class RegisterClientActivity : AppCompatActivity() { // Danışan kayıt ekranı
    override fun onCreate(savedInstanceState: Bundle?) { // Activity oluşturulurken
        super.onCreate(savedInstanceState) // Üst sınıf başlatması
        setContentView(R.layout.activity_register_client) // Kayıt layout yükle

        val etUsername = findViewById<EditText>(R.id.etRegUsername) // Ad soyad alanı
        val etEmail = findViewById<EditText>(R.id.etRegEmail) // E-posta alanı
        val etPassword = findViewById<EditText>(R.id.etRegPassword) // Şifre alanı
        val btnRegister = findViewById<Button>(R.id.btnRegisterClient) // Kayıt ol butonu
        val tvBackToLogin = findViewById<TextView>(R.id.tvBackToLogin) // Girişe dön linki

        tvBackToLogin.setOnClickListener { // Geri dön tıklanınca
            finish() // Giriş sayfasına geri döner
        }

        btnRegister.setOnClickListener { // Kayıt butonuna basılınca
            val username = etUsername.text.toString().trim() // Kullanıcı adı/ad soyad
            val email = etEmail.text.toString().trim() // E-posta
            val password = etPassword.text.toString().trim() // Şifre

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) { // Alan validasyonu
                Toast.makeText(this, "Lütfen tüm alanları doldurun!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // İşlemi durdur
            }

            val nameParts = username.split(" ", limit = 2) // Ad ve soyadı ayır
            val firstName = nameParts.firstOrNull().orEmpty() // İlk kelime = ad
            val lastName = nameParts.getOrNull(1).orEmpty() // İkinci kelime = soyad

            val request = ClientRegisterRequest( // API istek gövdesi oluştur
                firstName = firstName,
                lastName = lastName,
                email = email,
                password = password,
                weight = 0.0, // Kayıtta varsayılan kilo
                height = 0.0, // Kayıtta varsayılan boy
                targetCalories = 2000, // Varsayılan hedef kalori
                dietitianId = null // Başlangıçta diyetisyen bağlantısı yok
            )

            lifecycleScope.launch { // Kayıt API çağrısı
                try { // Ağ hatalarını yakala
                    val response = RetrofitClient.instance.registerClient(request) // POST kayıt
                    if (response.isSuccessful) { // Başarılı yanıt
                        Toast.makeText(this@RegisterClientActivity, "Kayıt Başarılı!", Toast.LENGTH_SHORT).show()
                        finish() // Giriş ekranına dön
                    } else { // HTTP hata kodu
                        Toast.makeText(this@RegisterClientActivity, "Hata: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) { // Bağlantı hatası
                    Toast.makeText(this@RegisterClientActivity, "Bağlantı Sorunu!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
