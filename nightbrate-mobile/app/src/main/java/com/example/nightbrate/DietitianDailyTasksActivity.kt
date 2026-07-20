package com.example.nightbrate // Uygulama paketi

import android.graphics.Typeface // Kalın yazı tipi
import android.os.Bundle // Aktivite durum paketi
import android.view.View // Görünüm temel sınıfı
import android.view.ViewGroup // Görünüm grubu düzeni
import android.widget.CheckBox // Tamamlama kutusu
import android.widget.LinearLayout // Dikey/yatay düzen
import android.widget.ProgressBar // Yükleme göstergesi
import android.widget.TextView // Metin görünümü
import androidx.appcompat.app.AppCompatActivity // Temel aktivite
import androidx.core.content.ContextCompat // Kaynak renk erişimi
import androidx.lifecycle.lifecycleScope // Yaşam döngüsü coroutine kapsamı
import kotlinx.coroutines.launch // Asenkron başlatma

class DietitianDailyTasksActivity : AppCompatActivity() { // Günlük görevler ekranı

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt() // dp'yi piksele çevir

    override fun onCreate(savedInstanceState: Bundle?) { // Aktivite oluşturulduğunda
        super.onCreate(savedInstanceState) // Üst sınıf başlatması
        setContentView(R.layout.activity_dietitian_daily_tasks) // Düzeni yükle
        DietitianBottomBarHelper.bind(this, 0) // Alt menüyü bağla

        findViewById<TextView>(R.id.btnDailyTasksBack).setOnClickListener { finish() } // Geri dön

        loadAll() // Görevleri yükle
    }

    private fun loadAll() { // Tüm günlük görevleri API'den al
        val progress = findViewById<ProgressBar>(R.id.progressDailyTasks) // İlerleme çubuğu
        progress.visibility = View.VISIBLE // Yükleniyor göster
        lifecycleScope.launch { // Coroutine ile ağ isteği
            try {
                val r = RetrofitClient.instance.getTodayDailyTasks() // Bugünkü görevleri getir
                val body = if (r.isSuccessful) r.body() else null // Başarılıysa gövdeyi al
                bindSummary(body) // Özet metnini güncelle
                bindTaskList(body?.tasks.orEmpty()) // Görev listesini çiz
            } catch (_: Exception) { // Ağ veya parse hatası
                findViewById<TextView>(R.id.tvDailyTasksSummary).text = "Görevler yüklenemedi." // Hata mesajı
            } finally {
                progress.visibility = View.GONE // Yüklemeyi gizle
            }
        }
    }

    private fun bindSummary(bundle: DietitianTodayTasksBundleDto?) { // Üst özet satırını doldur
        val tv = findViewById<TextView>(R.id.tvDailyTasksSummary) // Özet metin alanı
        if (bundle == null) { // Veri yoksa
            tv.text = "" // Boş bırak
            return // Çık
        }
        tv.text =
            "Tarih: ${bundle.taskDate ?: "—"} · Bekleyen ${bundle.pendingCount} · Tamamlanan ${bundle.completedCount} · Toplam ${bundle.totalCount}" // İstatistik satırı
    }

    private fun bindTaskList(tasks: List<DietitianDailyTaskItemDto>) { // Görev satırlarını oluştur
        val container = findViewById<LinearLayout>(R.id.containerAllTasks) // Liste konteyneri
        container.removeAllViews() // Önceki satırları temizle
        val strong = ContextCompat.getColor(this, R.color.admin_strong) // Koyu metin rengi
        val muted = ContextCompat.getColor(this, R.color.admin_muted) // Soluk metin rengi
        if (tasks.isEmpty()) { // Görev yoksa
            container.addView(TextView(this).apply {
                text = "Bugün için görev yok." // Boş durum mesajı
                setTextColor(muted) // Soluk renk
                textSize = 15f // Yazı boyutu
            })
            return // Liste çizimini bitir
        }
        for (t in tasks) { // Her görev için satır
            val id = t.id ?: continue // Kimlik yoksa atla
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL // Yatay düzen
                gravity = android.view.Gravity.CENTER_VERTICAL // Dikey ortala
                setPadding(dp(14), dp(14), dp(14), dp(14)) // İç boşluk
                setBackgroundResource(R.drawable.diet_task_row_bg) // Satır arka planı
            }
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) // Tam genişlik
            lp.bottomMargin = dp(10) // Alt boşluk
            val check = CheckBox(this).apply {
                tag = id // Görev kimliğini sakla
                isChecked = t.isCompleted // Tamamlanma durumu
            }
            check.setOnClickListener { // Kutuya tıklanınca
                val tid = check.tag as? String ?: return@setOnClickListener // Kimlik al
                val newVal = check.isChecked // Yeni durum
                lifecycleScope.launch { // API güncellemesi
                    check.isEnabled = false // Çift tıklamayı engelle
                    try {
                        val resp =
                            RetrofitClient.instance.setDailyTaskComplete(
                                tid,
                                SetDietitianTaskCompleteBody(isCompleted = newVal) // Tamamlama isteği
                            )
                        if (!resp.isSuccessful) throw IllegalStateException() // Başarısızsa hata
                        loadAll() // Listeyi yenile
                    } catch (_: Exception) { // Hata durumunda
                        check.isChecked = !newVal // Eski duruma geri al
                    } finally {
                        check.isEnabled = true // Kutuyu tekrar etkinleştir
                    }
                }
            }
            val textCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL // Başlık ve alt başlık sütunu
            }
            textCol.addView(TextView(this).apply {
                text = t.title ?: "" // Görev başlığı
                setTextColor(strong) // Koyu renk
                textSize = 16f // Başlık boyutu
                setTypeface(typeface, Typeface.BOLD) // Kalın yazı
            })
            textCol.addView(TextView(this).apply {
                text = t.subtitle ?: "" // Alt açıklama
                setTextColor(muted) // Soluk renk
                textSize = 14f // Alt metin boyutu
                setPadding(0, dp(4), 0, 0) // Üst boşluk
            })
            val due = TextView(this).apply {
                text = t.dueLabel ?: "Bugün" // Son tarih etiketi
                setTextColor(android.graphics.Color.parseColor("#F59E0B")) // Turuncu vurgu
                textSize = 13f // Etiket boyutu
                setTypeface(typeface, Typeface.BOLD) // Kalın yazı
            }
            row.addView(check) // Onay kutusu
            row.addView(
                textCol,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(8) } // Esnek metin alanı
            )
            row.addView(due) // Son tarih
            container.addView(row, lp) // Satırı listeye ekle
        }
    }
}
