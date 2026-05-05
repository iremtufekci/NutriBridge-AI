package com.example.nightbrate

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Locale

class AdminSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_settings)
        AdminBottomBarHelper.bind(this, 4)
        loadAccountInfo()
    }

    private fun loadAccountInfo() {
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.getCurrentUserProfile()
                if (!r.isSuccessful) {
                    Toast.makeText(
                        this@AdminSettingsActivity,
                        "Hesap bilgisi yüklenemedi",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }
                val p = r.body() ?: return@launch
                val name = p.displayName?.trim().orEmpty().ifEmpty { p.email.orEmpty() }
                findViewById<TextView>(R.id.aaName).text = name
                findViewById<TextView>(R.id.aaEmail).text = p.email.orEmpty()
                val local = p.email?.substringBefore("@")?.take(2)?.uppercase(Locale.ROOT).orEmpty()
                val avatar = if (local.isNotEmpty()) local else (name.filter { it.isLetter() }).take(2)
                    .uppercase(Locale.ROOT)
                findViewById<TextView>(R.id.aaAvatar).text = if (avatar.isNotEmpty()) avatar else "A"
            } catch (e: Exception) {
                Toast.makeText(
                    this@AdminSettingsActivity,
                    e.message ?: "Bağlantı hatası",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
