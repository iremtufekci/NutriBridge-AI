package com.example.nightbrate

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Lütfen alanları doldurun!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Backend'e istek atıyoruz (Coroutine ile arka planda)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val request = LoginRequest(email, password)
                    val response = RetrofitClient.instance.login(request)

                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            val userResponse = response.body()
                            Toast.makeText(this@MainActivity, "Giriş Başarılı! Hoş geldin ${userResponse?.username}", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@MainActivity, "Hata: Bilgiler yanlış!", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        // Burada hata alırsan IP adresini (192.168.37.74) kontrol etmeliyiz
                        Toast.makeText(this@MainActivity, "Bağlantı hatası: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}