package com.example.nightbrate

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        
        // Kayıt yönlendirme TextView'ları
        val tvGoToRegisterClient = findViewById<TextView>(R.id.tvGoToRegisterClient)
        val tvGoToRegisterDietitian = findViewById<TextView>(R.id.tvGoToRegisterDietitian)

        // GİRİŞ YAP BUTONU
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Lütfen tüm alanları doldurun!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performLogin(email, password)
        }

        // DANIŞAN KAYIT EKRANINA GİT
        tvGoToRegisterClient.setOnClickListener {
            val intent = Intent(this, RegisterClientActivity::class.java)
            startActivity(intent)
        }

        // DİYETİSYEN BAŞVURU EKRANINA GİT
        tvGoToRegisterDietitian.setOnClickListener {
            val intent = Intent(this, RegisterDietitianActivity::class.java)
            startActivity(intent)
        }
    }

    private fun performLogin(email: String, password: String) {
        lifecycleScope.launch {
            try {
                val request = LoginRequest(email, password)
                val response = RetrofitClient.instance.login(request)

                if (response.isSuccessful) {
                    val userResponse = response.body()
                    Toast.makeText(this@MainActivity, "Hoş geldin ${userResponse?.username}!", Toast.LENGTH_LONG).show()
                    // TODO: Ana sayfaya yönlendirme
                } else {
                    Toast.makeText(this@MainActivity, "Giriş Başarısız: Email veya şifre hatalı!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Bağlantı Hatası: Sunucuya ulaşılamıyor.", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }
}
