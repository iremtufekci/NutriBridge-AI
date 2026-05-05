package com.example.nightbrate

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.nightbrate.ActivityWindowHelper.applyStandardContentWindow
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyStandardContentWindow()
        setContentView(R.layout.activity_main)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnLogin)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        val llRegisterClient = findViewById<LinearLayout>(R.id.llRegisterClient)
        val llRegisterDietitian = findViewById<LinearLayout>(R.id.llRegisterDietitian)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Lütfen tüm alanları doldurun!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performLogin(email, password)
        }

        tvForgotPassword.setOnClickListener {
            Toast.makeText(
                this,
                "Şifre sıfırlama yakında eklenecek.",
                Toast.LENGTH_SHORT
            ).show()
        }

        llRegisterClient.setOnClickListener {
            startActivity(Intent(this, RegisterClientActivity::class.java))
        }

        llRegisterDietitian.setOnClickListener {
            startActivity(Intent(this, RegisterDietitianActivity::class.java))
        }
    }

    private fun performLogin(email: String, password: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.login(LoginRequest(email, password))

                if (response.isSuccessful) {
                    val userResponse = response.body()
                    val role = userResponse?.role?.lowercase() ?: ""
                    val token = userResponse?.token.orEmpty()

                    val authPrefs = getSharedPreferences(ThemeUtils.PREF_NAME, MODE_PRIVATE)
                    authPrefs.edit()
                        .putString("token", token)
                        .putString("role", role)
                        .putString("email", email)
                        .apply()

                    ThemeUtils.applyLightTheme(authPrefs)

                    Toast.makeText(this@MainActivity, "Giriş başarılı!", Toast.LENGTH_SHORT).show()

                    val intent = when (role) {
                        "admin" -> Intent(this@MainActivity, AdminDashboardActivity::class.java)
                        "dietitian", "diyetisyen" -> Intent(this@MainActivity, DietitianDashboardActivity::class.java)
                        else -> Intent(this@MainActivity, ClientDashboardActivity::class.java)
                    }
                    
                    intent.putExtra("USERNAME", email.substringBefore("@"))
                    startActivity(intent)
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    val backendMessage = try {
                        JSONObject(errorBody ?: "{}").optString("message")
                    } catch (_: Exception) {
                        ""
                    }

                    val message = if (backendMessage.isNotBlank()) {
                        backendMessage
                    } else {
                        "Giriş başarısız: E-posta veya şifre hatalı!"
                    }

                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Bağlantı Hatası: Sunucuya ulaşılamıyor.", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }
}
