package com.example.nightbrate // Uygulama paketi

import android.content.Intent // Aktivite geçişi
import android.graphics.Typeface // Kalın yazı tipi
import android.os.Bundle // Aktivite durum paketi
import android.text.SpannableString // Biçimlendirilmiş metin
import android.text.style.StyleSpan // Kalın stil aralığı
import android.view.View // Görünüm temel sınıfı
import android.view.ViewGroup // Satır düzeni
import android.widget.CheckBox // Görev tamamlama kutusu
import android.widget.LinearLayout // Dikey liste düzeni
import android.widget.ProgressBar // Yükleme göstergesi
import android.widget.TextView // Metin görünümü
import androidx.appcompat.app.AppCompatActivity // Temel aktivite
import androidx.core.content.ContextCompat // Kaynak renk erişimi
import androidx.lifecycle.lifecycleScope // Yaşam döngüsü coroutine kapsamı
import com.example.nightbrate.ActivityWindowHelper.applyStandardContentWindow // Standart pencere düzeni
import kotlinx.coroutines.async // Paralel istek
import kotlinx.coroutines.awaitAll // Tüm istekleri bekle
import kotlinx.coroutines.coroutineScope // Coroutine kapsamı
import kotlinx.coroutines.launch // Asenkron başlatma
import java.text.SimpleDateFormat // Tarih biçimlendirme
import java.util.Locale // Yerel ayar
import java.util.TimeZone // Saat dilimi

class DietitianDashboardActivity : AppCompatActivity() { // Diyetisyen ana panel ekranı

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt() // dp'yi piksele çevir

    private fun alertTypeLabel(t: String) = when (t) { // Uyarı tipi etiketi
        "MissedMeals" -> "Öğün tamamlama" // Kaçırılan öğün
        "HighCalories" -> "Yüksek kalori" // Kalori aşımı
        else -> t // Bilinmeyen tip
    }

    private fun formatAlertDate(iso: String): String { // ISO uyarı tarihini Türkçe biçimle
        if (iso.isBlank()) return "" // Boşsa çık
        val outFmt = SimpleDateFormat("d MMMM yyyy", Locale("tr", "TR")) // Çıkış biçimi
        val patterns = listOf( // Olası giriş desenleri
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd"
        )
        for (p in patterns) { // Her deseni dene
            try {
                val inFmt = SimpleDateFormat(p, Locale.US).apply {
                    if (p.contains("'Z'")) timeZone = TimeZone.getTimeZone("UTC") // UTC saat dilimi
                }
                val d = inFmt.parse(iso.trim()) ?: continue // Ayrıştır
                return outFmt.format(d) // Biçimlendirilmiş tarih
            } catch (_: Exception) {
                /* try next */ // Sonraki deseni dene
            }
        }
        return iso // Ham değeri döndür
    }

    override fun onCreate(savedInstanceState: Bundle?) { // Aktivite oluşturulduğunda
        super.onCreate(savedInstanceState) // Üst sınıf başlatması
        applyStandardContentWindow() // Standart pencere düzeni uygula
        setContentView(R.layout.activity_dietitian_dashboard) // Panel düzenini yükle
        DietitianBottomBarHelper.bind(this, 0) // Alt menüyü bağla

        val goCriticalAlerts = { DietitianTabNav.go(this, 4) } // Kritik uyarılar sekmesi
        val goAiReview = { DietitianTabNav.go(this, 3) } // AI inceleme sekmesi

        findViewById<TextView>(R.id.btnDashViewAllClients).setOnClickListener { goCriticalAlerts() } // Tüm uyarılar
        findViewById<TextView>(R.id.btnDashAiReview).setOnClickListener { goAiReview() } // AI inceleme
        findViewById<View>(R.id.cardDashCriticalAlerts).setOnClickListener { goCriticalAlerts() } // Uyarı kartı
        findViewById<View>(R.id.cardDashAiReview).setOnClickListener { goAiReview() } // AI kartı

        findViewById<View>(R.id.cardDashTodayTasks).setOnClickListener { // Günlük görevler kartı
            startActivity(Intent(this, DietitianDailyTasksActivity::class.java)) // Görev ekranına git
        }
        findViewById<TextView>(R.id.btnDashAllTasks).setOnClickListener { // Tüm görevler bağlantısı
            startActivity(Intent(this, DietitianDailyTasksActivity::class.java)) // Görev ekranına git
        }

        loadDashboard() // Panel verilerini yükle
    }

    private fun loadDashboard() { // Tüm panel verilerini paralel yükle
        val progress = findViewById<ProgressBar>(R.id.dashProgress) // Yükleme çubuğu
        progress.visibility = View.VISIBLE // Yükleniyor göster
        lifecycleScope.launch { // Coroutine ile istekler
            try {
                val profileReq = async { RetrofitClient.instance.getCurrentUserProfile() } // Profil isteği
                val clientsReq = async { RetrofitClient.instance.getClientsWithLastMeal() } // Danışan isteği
                val tasksReq = async { RetrofitClient.instance.getTodayDailyTasks() } // Görev isteği
                val critReq = async { RetrofitClient.instance.getDietitianCriticalAlerts() } // Uyarı isteği
                val pr = profileReq.await() // Profil yanıtı
                val cr = clientsReq.await() // Danışan yanıtı
                val tr = tasksReq.await() // Görev yanıtı
                val critRes = critReq.await() // Uyarı yanıtı
                val clients = if (cr.isSuccessful) cr.body().orEmpty() else emptyList() // Danışan listesi
                val aiPreviewsReq = async { loadRecentKitchenShares(clients) } // AI önizleme isteği

                val taskBundle = if (tr.isSuccessful) tr.body() else null // Görev paketi
                val alerts = if (critRes.isSuccessful) critRes.body().orEmpty() else emptyList() // Uyarı listesi
                val critCount = alerts.size // Kritik uyarı sayısı

                val p = pr.body() // Profil gövdesi
                val displayName = when { // Görünen adı belirle
                    !p?.displayName.isNullOrBlank() -> p!!.displayName!!.trim() // Görünen ad
                    else -> listOf(p?.firstName, p?.lastName)
                        .mapNotNull { it?.trim() } // Ad parçaları
                        .filter { it.isNotEmpty() } // Boş olmayanlar
                        .joinToString(" ") // Birleştir
                        .ifBlank { "Diyetisyen" } // Varsayılan
                }
                findViewById<TextView>(R.id.tvDashGreeting).text = "Merhaba, $displayName 👋" // Karşılama metni

                val subtitle = if (critCount > 0) { // Uyarı varsa
                    "$critCount kritik uyarınız var. Öğün uyumu ve kalori eşiklerini inceleyin." // Uyarılı alt başlık
                } else { // Uyarı yoksa
                    "Şu an kritik uyarı yok. Güncel aktivitelere göz atın." // Sakin alt başlık
                }
                val sp = SpannableString(subtitle) // Biçimlendirilmiş alt başlık
                if (critCount > 0) { // Uyarı varsa kalın vurgu
                    val key = "$critCount kritik uyarınız" // Vurgulanacak kısım
                    val startN = subtitle.indexOf(key) // Başlangıç konumu
                    if (startN >= 0) {
                        sp.setSpan(StyleSpan(Typeface.BOLD), startN, startN + key.length, 0) // Kalın yap
                    }
                }
                findViewById<TextView>(R.id.tvDashSubtitle).text = sp // Alt başlığı yaz

                findViewById<TextView>(R.id.tvStatTotalClients).text = clients.size.toString() // Toplam danışan
                findViewById<TextView>(R.id.tvStatActivePrograms).text = clients.size.toString() // Aktif program sayısı
                findViewById<TextView>(R.id.tvStatTodayTasks).text =
                    (taskBundle?.totalCount ?: 0).toString() // Bugünkü görev sayısı
                findViewById<TextView>(R.id.tvStatCriticalAlerts).text = critCount.toString() // Kritik uyarı sayısı

                bindTodayTasksPreview(taskBundle?.tasks.orEmpty()) // Görev önizlemesini çiz

                findViewById<TextView>(R.id.tvCriticalBadge).text = "$critCount uyarı" // Uyarı rozeti
                bindCriticalSection(alerts.take(3)) { goToCriticalAlerts() } // İlk 3 uyarıyı göster

                val (aiPreviews, aiTotal) = aiPreviewsReq.await() // AI önizleme sonucu
                findViewById<TextView>(R.id.tvAiReviewBadge).text = "$aiTotal kayıt" // AI kayıt rozeti
                bindAiReviewSection(aiPreviews) // AI önizlemesini çiz
            } catch (_: Exception) { // Genel hata
                findViewById<TextView>(R.id.tvDashSubtitle).text =
                    "Veriler yüklenemedi. Bağlantınızı kontrol edip tekrar deneyin." // Hata alt başlığı
                bindTodayTasksPreview(emptyList()) // Boş görev önizlemesi
                bindAiReviewSection(emptyList()) // Boş AI önizlemesi
            } finally {
                progress.visibility = View.GONE // Yüklemeyi gizle
            }
        }
    }

    private fun goToCriticalAlerts() = DietitianTabNav.go(this, 4) // Kritik uyarılar sekmesine git

    private data class AiSharePreviewRow( // AI paylaşım önizleme satırı
        val clientName: String, // Danışan adı
        val recipeTitle: String, // Tarif başlığı
        val targetCalories: Int, // Hedef kalori
        val estimatedCalories: Int, // Tahmini kalori
        val preference: String, // Beslenme tercihi
        val createdAtUtc: String // Oluşturma zamanı
    )

    private suspend fun loadRecentKitchenShares(
        clients: List<ClientWithLastMealItem>
    ): Pair<List<AiSharePreviewRow>, Int> = coroutineScope { // Son mutfak paylaşımlarını yükle
        if (clients.isEmpty()) return@coroutineScope emptyList<AiSharePreviewRow>() to 0 // Danışan yoksa boş
        val jobs = clients.mapNotNull { c -> // Her danışan için paralel iş
            val id = c.id ?: return@mapNotNull null // Kimlik yoksa atla
            val name = listOf(c.firstName, c.lastName)
                .mapNotNull { it?.trim() } // Ad parçaları
                .filter { it.isNotEmpty() } // Boş olmayanlar
                .joinToString(" ") // Birleştir
                .ifBlank { "Danışan" } // Varsayılan ad
            async {
                val r = RetrofitClient.instance.getClientKitchenRecipeLogs(id, take = 5) // Son 5 kayıt
                if (!r.isSuccessful) return@async emptyList<AiSharePreviewRow>() // Başarısızsa boş
                r.body().orEmpty().map { log ->
                    val recipe = log.selectedRecipes.firstOrNull() // İlk tarif
                    AiSharePreviewRow(
                        clientName = name, // Danışan adı
                        recipeTitle = recipe?.title?.ifBlank { "Paylaşılan tarif" } ?: "Paylaşılan tarif", // Tarif başlığı
                        targetCalories = log.targetCalories, // Hedef kalori
                        estimatedCalories = recipe?.estimatedCalories ?: log.targetCalories, // Tahmini kalori
                        preference = log.preference, // Tercih
                        createdAtUtc = log.createdAtUtc // Zaman damgası
                    )
                }
            }
        }
        val all = jobs.awaitAll()
            .flatten() // Tüm sonuçları birleştir
            .sortedByDescending { parseIsoMillis(it.createdAtUtc) } // Yeniden eskiye sırala
        all.take(3) to all.size // İlk 3 ve toplam sayı
    }

    private fun parseIsoMillis(iso: String): Long { // ISO zamanı milisaniyeye çevir
        val patterns = listOf( // Olası tarih desenleri
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (p in patterns) { // Her deseni dene
            try {
                val sdf = SimpleDateFormat(p, Locale.US) // Giriş biçimleyici
                if (p.contains("'Z'")) sdf.timeZone = TimeZone.getTimeZone("UTC") // UTC saat dilimi
                val d = sdf.parse(iso) ?: continue // Ayrıştır
                return d.time // Milisaniye döndür
            } catch (_: Exception) { // Desen uymazsa devam
            }
        }
        return 0L // Varsayılan sıfır
    }

    private fun formatShareWhen(iso: String): String { // Paylaşım zamanını kısa biçimle
        if (iso.isBlank()) return "" // Boşsa çık
        val patterns = listOf( // Olası tarih desenleri
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (p in patterns) { // Her deseni dene
            try {
                val sdf = SimpleDateFormat(p, Locale.US) // Giriş biçimleyici
                if (p.contains("'Z'")) sdf.timeZone = TimeZone.getTimeZone("UTC") // UTC saat dilimi
                val d = sdf.parse(iso) ?: continue // Ayrıştır
                val out = SimpleDateFormat("d MMM, HH:mm", Locale("tr", "TR")) // Çıkış biçimi
                out.timeZone = TimeZone.getDefault() // Yerel saat dilimi
                return out.format(d) // Biçimlendirilmiş zaman
            } catch (_: Exception) { // Desen uymazsa devam
            }
        }
        return iso // Ham değeri döndür
    }

    private fun bindCriticalSection(
        alerts: List<DietitianCriticalAlertDto>,
        onOpenAlerts: () -> Unit
    ) { // Kritik uyarı önizleme bölümünü çiz
        val container = findViewById<LinearLayout>(R.id.containerCriticalClients) // Liste konteyneri
        container.removeAllViews() // Önceki satırları temizle
        val strong = ContextCompat.getColor(this, R.color.admin_strong) // Koyu metin rengi
        val muted = ContextCompat.getColor(this, R.color.admin_muted) // Soluk metin rengi
        val roseText = android.graphics.Color.parseColor("#F87171") // Yüksek öncelik metin rengi
        val amberText = android.graphics.Color.parseColor("#D97706") // Orta öncelik metin rengi
        if (alerts.isEmpty()) { // Uyarı yoksa
            container.addView(TextView(this).apply {
                text = "Şu an listelenecek kritik uyarı yok." // Boş durum mesajı
                setTextColor(muted) // Soluk renk
                textSize = 14f // Normal yazı
                setPadding(0, dp(8), 0, dp(8)) // Dikey boşluk
            })
            return // Çizimi bitir
        }
        for (alert in alerts) { // Her uyarı için satır
            val isHigh = alert.severity.equals("High", ignoreCase = true) // Yüksek öncelik mi
            val accent = if (isHigh) roseText else amberText // Vurgu rengi
            val name = alert.clientName.ifBlank { "Danışan" } // Danışan adı
            val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?" // Baş harf
            val outer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL // Yatay düzen
                gravity = android.view.Gravity.CENTER_VERTICAL // Dikey ortala
                setPadding(dp(14), dp(14), dp(14), dp(14)) // İç boşluk
                setBackgroundResource(R.drawable.diet_critical_row_bg) // Satır arka planı
            }
            val stripe = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(4), ViewGroup.LayoutParams.MATCH_PARENT).apply {
                    marginEnd = dp(12) // Sağ boşluk
                }
                setBackgroundColor(accent) // Sol şerit rengi
            }
            val avatar = TextView(this).apply {
                text = initial // Avatar harfi
                textSize = 14f // Normal yazı
                setTypeface(typeface, Typeface.BOLD) // Kalın yazı
                setTextColor(accent) // Vurgu rengi
                gravity = android.view.Gravity.CENTER // Ortala
                layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)) // Kare boyut
                setBackgroundResource(R.drawable.diet_critical_avatar_bg) // Avatar arka planı
            }
            val textCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL // Dikey metin sütunu
            }
            textCol.addView(TextView(this).apply {
                text = name // Danışan adı
                setTextColor(strong) // Koyu renk
                textSize = 17f // Büyük yazı
                setTypeface(typeface, Typeface.BOLD) // Kalın yazı
            })
            val dateLabel = formatAlertDate(alert.date) // Biçimlendirilmiş tarih
            if (dateLabel.isNotEmpty()) { // Tarih varsa
                textCol.addView(TextView(this).apply {
                    text = dateLabel // Tarih metni
                    setTextColor(muted) // Soluk renk
                    textSize = 13f // Küçük yazı
                    setPadding(0, dp(4), 0, 0) // Üst boşluk
                })
            }
            val chip = TextView(this).apply {
                text = "⚠ ${alertTypeLabel(alert.alertType)}" // Uyarı tipi etiketi
                setTextColor(accent) // Vurgu rengi
                textSize = 11f // Küçük yazı
                setTypeface(typeface, Typeface.BOLD) // Kalın yazı
                setPadding(dp(10), dp(6), dp(10), dp(6)) // İç boşluk
                setBackgroundResource(R.drawable.diet_badge_rose) // Rozet arka planı
            }
            textCol.addView(
                chip,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT // Etiket düzeni
                ).apply { topMargin = dp(8) } // Üst boşluk
            )
            val msg = alert.message.trim() // Uyarı mesajı
            if (msg.isNotEmpty()) { // Mesaj varsa
                textCol.addView(TextView(this).apply {
                    text = msg // Mesaj metni
                    setTextColor(muted) // Soluk renk
                    textSize = 14f // Normal yazı
                    maxLines = 2 // En fazla iki satır
                    setPadding(0, dp(6), 0, 0) // Üst boşluk
                })
            }
            val chevron = TextView(this).apply {
                text = "›" // Sağ ok
                setTextColor(muted) // Soluk renk
                textSize = 22f // Büyük ok
                includeFontPadding = false // Font boşluğu yok
            }
            val inner = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL // İç yatay düzen
                gravity = android.view.Gravity.TOP // Üst hizala
                addView(avatar) // Avatar ekle
                addView(
                    textCol,
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginStart = dp(10) // Sol boşluk
                    }
                )
                addView(chevron) // Ok ekle
            }
            outer.addView(stripe) // Sol şerit
            outer.addView(
                inner,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) // Esnek içerik
            )
            outer.setOnClickListener { onOpenAlerts() } // Tıklanınca uyarılar sekmesi
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) // Tam genişlik
            lp.bottomMargin = dp(10) // Alt boşluk
            container.addView(outer, lp) // Satırı listeye ekle
        }
    }

    private fun bindAiReviewSection(rows: List<AiSharePreviewRow>) { // AI inceleme önizlemesini çiz
        val container = findViewById<LinearLayout>(R.id.containerAiReviewPreview) // Liste konteyneri
        container.removeAllViews() // Önceki satırları temizle
        val strong = ContextCompat.getColor(this, R.color.admin_strong) // Koyu metin rengi
        val muted = ContextCompat.getColor(this, R.color.admin_muted) // Soluk metin rengi
        val emerald = android.graphics.Color.parseColor("#047857") // Yeşil vurgu rengi
        if (rows.isEmpty()) { // Kayıt yoksa
            container.addView(TextView(this).apply {
                text = "Henüz paylaşılan yapay zeka tarif kaydı yok." // Boş durum mesajı
                setTextColor(muted) // Soluk renk
                textSize = 14f // Normal yazı
                setPadding(0, dp(8), 0, dp(8)) // Dikey boşluk
            })
            return // Çizimi bitir
        }
        for (row in rows) { // Her önizleme satırı
            val outer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL // Yatay düzen
                gravity = android.view.Gravity.CENTER_VERTICAL // Dikey ortala
                setPadding(dp(14), dp(14), dp(14), dp(14)) // İç boşluk
                setBackgroundResource(R.drawable.diet_task_row_bg) // Satır arka planı
                setOnClickListener { DietitianTabNav.go(this@DietitianDashboardActivity, 3) } // AI sekmesine git
            }
            val initial = row.clientName.firstOrNull()?.uppercaseChar()?.toString() ?: "?" // Baş harf
            val avatar = TextView(this).apply {
                text = initial // Avatar harfi
                textSize = 14f // Normal yazı
                setTypeface(typeface, Typeface.BOLD) // Kalın yazı
                setTextColor(emerald) // Yeşil vurgu
                gravity = android.view.Gravity.CENTER // Ortala
                layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)) // Kare boyut
                setBackgroundResource(R.drawable.diet_critical_avatar_bg) // Avatar arka planı
            }
            val textCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL } // Dikey metin sütunu
            textCol.addView(TextView(this).apply {
                text = row.clientName // Danışan adı
                setTextColor(strong) // Koyu renk
                textSize = 16f // Normal yazı
                setTypeface(typeface, Typeface.BOLD) // Kalın yazı
            })
            textCol.addView(TextView(this).apply {
                text = row.recipeTitle // Tarif başlığı
                setTextColor(muted) // Soluk renk
                textSize = 14f // Normal yazı
                setPadding(0, dp(4), 0, 0) // Üst boşluk
            })
            val kcalLine = buildString { // Kalori bilgi satırı
                append("~${row.estimatedCalories} kkal") // Tahmini kalori
                if (row.targetCalories > 0) append(" · hedef ${row.targetCalories}") // Hedef kalori
                row.preference.takeIf { it.isNotBlank() }?.let { append(" · $it") } // Tercih
            }
            textCol.addView(TextView(this).apply {
                text = kcalLine // Kalori satırı
                setTextColor(emerald) // Yeşil vurgu
                textSize = 13f // Küçük yazı
                setTypeface(typeface, Typeface.BOLD) // Kalın yazı
                setPadding(0, dp(4), 0, 0) // Üst boşluk
            })
            val whenStr = formatShareWhen(row.createdAtUtc) // Biçimlendirilmiş zaman
            if (whenStr.isNotEmpty()) { // Zaman varsa
                textCol.addView(TextView(this).apply {
                    text = whenStr // Zaman metni
                    setTextColor(muted) // Soluk renk
                    textSize = 12f // Küçük yazı
                    setPadding(0, dp(2), 0, 0) // Üst boşluk
                })
            }
            val chevron = TextView(this).apply {
                text = "›" // Sağ ok
                setTextColor(muted) // Soluk renk
                textSize = 22f // Büyük ok
                includeFontPadding = false // Font boşluğu yok
            }
            outer.addView(avatar) // Avatar ekle
            outer.addView(
                textCol,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(10) // Sol boşluk
                }
            )
            outer.addView(chevron) // Ok ekle
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) // Tam genişlik
            lp.bottomMargin = dp(10) // Alt boşluk
            container.addView(outer, lp) // Satırı listeye ekle
        }
    }

    private fun bindTodayTasksPreview(tasks: List<DietitianDailyTaskItemDto>) { // Bugünkü görev önizlemesini çiz
        val container = findViewById<LinearLayout>(R.id.containerTodayTasks) // Liste konteyneri
        container.removeAllViews() // Önceki satırları temizle
        val strong = ContextCompat.getColor(this, R.color.admin_strong) // Koyu metin rengi
        val muted = ContextCompat.getColor(this, R.color.admin_muted) // Soluk metin rengi
        val preview = tasks.filter { !it.isCompleted }.take(4) // Tamamlanmamış ilk 4 görev
        if (preview.isEmpty()) { // Bekleyen görev yoksa
            container.addView(TextView(this).apply {
                text = "Bekleyen günlük görev yok." // Boş durum mesajı
                setTextColor(muted) // Soluk renk
                textSize = 14f // Normal yazı
                setPadding(0, dp(6), 0, dp(6)) // Dikey boşluk
            })
            return // Çizimi bitir
        }
        for (t in preview) { // Her görev satırı
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
                        val resp = RetrofitClient.instance.setDailyTaskComplete(
                            tid,
                            SetDietitianTaskCompleteBody(isCompleted = newVal) // Tamamlama isteği
                        )
                        if (!resp.isSuccessful) throw IllegalStateException() // Başarısızsa hata
                        loadDashboard() // Paneli yenile
                    } catch (_: Exception) { // Hata durumunda
                        check.isChecked = !newVal // Eski duruma geri al
                    } finally {
                        check.isEnabled = true // Kutuyu tekrar etkinleştir
                    }
                }
            }
            val textCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL } // Dikey metin sütunu
            textCol.addView(TextView(this).apply {
                text = t.title ?: "" // Görev başlığı
                setTextColor(strong) // Koyu renk
                textSize = 16f // Normal yazı
                setTypeface(typeface, Typeface.BOLD) // Kalın yazı
            })
            textCol.addView(TextView(this).apply {
                text = t.subtitle ?: "" // Alt açıklama
                setTextColor(muted) // Soluk renk
                textSize = 14f // Normal yazı
                setPadding(0, dp(4), 0, 0) // Üst boşluk
            })
            val due = TextView(this).apply {
                text = t.dueLabel ?: "Bugün" // Son tarih etiketi
                setTextColor(android.graphics.Color.parseColor("#F59E0B")) // Turuncu vurgu
                textSize = 13f // Küçük yazı
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
