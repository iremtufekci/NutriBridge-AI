package com.example.nightbrate

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Locale

class DietitianProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dietitian_account)
        DietitianBottomBarHelper.bind(this, 5)
        loadProfile()
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.getCurrentUserProfile()
                if (!r.isSuccessful) {
                    Toast.makeText(this@DietitianProfileActivity, "Profil yuklenemedi", Toast.LENGTH_LONG).show()
                    return@launch
                }
                val p = r.body() ?: return@launch
                val f = p.firstName?.trim().orEmpty()
                val l = p.lastName?.trim().orEmpty()
                val name = listOf(f, l).filter { it.isNotEmpty() }
                    .joinToString(" ")
                    .ifEmpty { p.displayName?.trim().orEmpty() }
                    .ifEmpty { "Diyetisyen" }
                findViewById<TextView>(R.id.daName).text = name
                findViewById<TextView>(R.id.daEmail).text = "E-posta: ${p.email.orEmpty()}"
                findViewById<TextView>(R.id.daClinic).text =
                    "Klinik: ${p.clinicName?.trim().orEmpty().ifEmpty { "—" }}"
                findViewById<TextView>(R.id.daDiploma).text =
                    "Diploma no: ${p.diplomaNo?.trim().orEmpty().ifEmpty { "—" }}"
                val code = p.connectionCode?.trim().orEmpty()
                val tvKod = findViewById<TextView>(R.id.daTakipKodu)
                val tvKodBilgi = findViewById<TextView>(R.id.daTakipKoduBilgi)
                if (code.isNotEmpty()) {
                    tvKod.text = code
                    tvKodBilgi.text =
                        "Danışanlar uygulamada bu 6 haneli kodu girerek size bağlanır. Kod veritabanındaki onaylı hesabınızdan yüklenir."
                } else {
                    tvKod.text = "—"
                    tvKodBilgi.text =
                        "Kod henüz yok. Yönetici onayı sonrası otomatik atanır; onaylıysanız kısa süre sonra yenileyin."
                }
                val a = (f.take(1) + l.take(1)).uppercase(Locale.ROOT)
                val avatar = if (a.isNotBlank()) a else (name.filter { it.isLetter() }).take(2).uppercase(Locale.ROOT)
                findViewById<TextView>(R.id.daAvatar).text = if (avatar.isNotEmpty()) avatar else "D"
            } catch (e: Exception) {
                Toast.makeText(
                    this@DietitianProfileActivity,
                    e.message ?: "Baglanti hatasi",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
