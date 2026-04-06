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
        
        val tvGoToRegisterClient = findViewById<TextView>(R.id.tvGoToRegisterClient)
        val tvGoToRegisterDietitian = findViewById<TextView>(R.id.tvGoToRegisterDietitian)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Lütfen tüm alanları doldurun!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performLogin(email, password)
        }

        tvGoToRegisterClient.setOnClickListener {
            val intent = Intent(this, RegisterClientActivity::class.java)
            startActivity(intent)
        }

        tvGoToRegisterDietitian.setOnClickListener {
            val intent = Intent(this, RegisterDietitianActivity::class.java)
            startActivity(intent)
        }
    }

    private fun performLogin(email: String, password: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.login(LoginRequest(email, password))

                if (response.isSuccessful) {
                    val userResponse = response.body()
                    val role = userResponse?.role?.lowercase() ?: ""
                    val username = userResponse?.username ?: "Kullanıcı"

                    Toast.makeText(this@MainActivity, "Hoş geldin $username!", Toast.LENGTH_SHORT).show()

                    val intent = when (role) {
                        "admin" -> Intent(this@MainActivity, AdminDashboardActivity::class.java)
                        "dietitian", "diyetisyen" -> Intent(this@MainActivity, DietitianDashboardActivity::class.java)
                        else -> Intent(this@MainActivity, ClientDashboardActivity::class.java)
                    }
                    
                    intent.putExtra("USERNAME", username)
                    startActivity(intent)
                    finish()
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
