package com.example.nightbrate

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class RegisterDietitianActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_dietitian)

        val etFirstName = findViewById<EditText>(R.id.etFirstName)
        val etLastName = findViewById<EditText>(R.id.etLastName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etDiploma = findViewById<EditText>(R.id.etDiploma)
        val etClinic = findViewById<EditText>(R.id.etClinic)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvBackToLogin = findViewById<TextView>(R.id.tvBackToLogin)

        tvBackToLogin.setOnClickListener {
            finish()
        }

        btnRegister.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val diploma = etDiploma.text.toString().trim()
            val clinic = etClinic.text.toString().trim()

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || 
                password.isEmpty() || diploma.isEmpty() || clinic.isEmpty()) {
                Toast.makeText(this, "Lütfen tüm alanları doldurun!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = DietitianRegisterRequest(
                firstName = firstName,
                lastName = lastName,
                email = email,
                password = password,
                diplomaNo = diploma,
                clinicName = clinic
            )

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.instance.registerDietitian(request)
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@RegisterDietitianActivity,
                            "Başvurunuz alındı. Yönetici onayından sonra giriş yapabilirsiniz.",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    } else {
                        Toast.makeText(this@RegisterDietitianActivity, "Hata: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@RegisterDietitianActivity, "Bağlantı Sorunu!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
