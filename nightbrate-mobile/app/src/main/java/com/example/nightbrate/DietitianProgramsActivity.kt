package com.example.nightbrate // Uygulama paketi

import android.app.DatePickerDialog // Tarih seçici diyalog
import android.graphics.Color // Renk sabitleri
import android.graphics.Typeface // Kalın yazı tipi
import android.os.Bundle // Aktivite durum paketi
import android.text.Editable // Düzenlenebilir metin
import android.text.TextWatcher // Metin değişim dinleyicisi
import android.view.LayoutInflater // Liste satır şablonu
import android.view.View // Görünüm temel sınıfı
import android.view.ViewGroup // Liste öğe düzeni
import android.widget.BaseAdapter // Danışan liste adaptörü
import android.widget.EditText // Program ve kalori alanları
import android.widget.LinearLayout // Dikey/yatay düzen
import android.widget.ListView // Danışan seçim listesi
import android.widget.ProgressBar // Yükleme göstergesi
import android.widget.TextView // Metin görünümü
import android.widget.Toast // Kısa bildirim
import androidx.appcompat.app.AppCompatActivity // Temel aktivite
import androidx.core.content.ContextCompat // Kaynak renk erişimi
import androidx.lifecycle.lifecycleScope // Yaşam döngüsü coroutine kapsamı
import com.google.android.material.button.MaterialButton // Kaydet düğmesi
import kotlinx.coroutines.launch // Asenkron başlatma
import org.json.JSONObject // Hata JSON ayrıştırma
import retrofit2.Response // HTTP yanıtı
import java.text.SimpleDateFormat // Tarih biçimlendirme
import java.util.Calendar // Takvim hesabı
import java.util.Locale // Yerel ayar

class DietitianProgramsActivity : AppCompatActivity() { // Diyet programı düzenleme ekranı

    private var selectedYmd: String = todayYmd() // Seçili program tarihi
    private lateinit var dateOptions: List<Pair<String, String>> // Sonraki 60 gün seçenekleri
    private val dateButtons = mutableListOf<TextView>() // Gün chip görünümleri

    private var selectedClientId: String? = null // Seçili danışan kimliği
    private var clientRows: List<Pair<String, String>> = emptyList() // Tüm danışan satırları
    private var filteredRows: List<Pair<String, String>> = emptyList() // Filtrelenmiş danışanlar
    private val assignedChipViews = mutableListOf<Pair<String, TextView>>() // Atanmış tarih chip'leri

    private lateinit var clientAdapter: ClientPickAdapter // Danışan liste adaptörü
    private lateinit var tvCalendarValue: TextView // Seçili tarih etiketi
    private lateinit var cardSelectedBanner: View // Seçili danışan banner'ı
    private lateinit var tvSelectedName: TextView // Seçili danışan adı
    private lateinit var cardAssigned: View // Atanmış tarihler kartı
    private lateinit var tvAssignedStatus: TextView // Atanmış tarih durumu
    private lateinit var listProgress: ProgressBar // Liste yükleme çubuğu
    private lateinit var rowProgramLoading: View // Program yükleme satırı
    private lateinit var btnSave: MaterialButton // Kaydet düğmesi

    private val emerald = Color.parseColor("#22C55E") // Aktif chip rengi
    private val emeraldMutedBg = Color.parseColor("#F0FDF4") // Pasif chip arka planı
    private val slateBorder = Color.parseColor("#E2E8F0") // Kenarlık rengi
    private val slateText = Color.parseColor("#0F172A") // Koyu metin rengi
    private val mutedText by lazy { ContextCompat.getColor(this, R.color.admin_muted) } // Soluk metin rengi
    private val amberActive = Color.parseColor("#D97706") // Atanmış aktif chip rengi

    private fun todayYmd(): String = calendarToYmd(Calendar.getInstance()) // Bugünün YMD değeri

    private fun isPastProgramDate(ymd: String): Boolean = ymd < todayYmd() // Geçmiş tarih mi

    private fun calendarToYmd(c: Calendar): String { // Takvimi YMD dizgesine çevir
        val y = c.get(Calendar.YEAR) // Yıl
        val m = (c.get(Calendar.MONTH) + 1).toString().padStart(2, '0') // Ay
        val d = c.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0') // Gün
        return "$y-$m-$d" // ISO tarih
    }

    private fun buildNext60Days(): List<Pair<String, String>> { // Sonraki 60 gün listesi oluştur
        val out = ArrayList<Pair<String, String>>() // Çıktı listesi
        val cal = Calendar.getInstance() // Bugünün takvimi
        cal.set(Calendar.HOUR_OF_DAY, 0) // Saati sıfırla
        cal.set(Calendar.MINUTE, 0) // Dakikayı sıfırla
        cal.set(Calendar.SECOND, 0) // Saniyeyi sıfırla
        cal.set(Calendar.MILLISECOND, 0) // Milisaniyeyi sıfırla
        val fmt = SimpleDateFormat("EEE d MMM", Locale("tr", "TR")) // Gün etiketi biçimi
        for (i in 0 until 60) { // 60 gün döngüsü
            val c = cal.clone() as Calendar // Gün kopyası
            c.add(Calendar.DAY_OF_MONTH, i) // İleriye git
            out.add(calendarToYmd(c) to fmt.format(c.time)) // YMD ve etiket ekle
        }
        return out // Gün listesini döndür
    }

    override fun onCreate(savedInstanceState: Bundle?) { // Aktivite oluşturulduğunda
        super.onCreate(savedInstanceState) // Üst sınıf başlatması
        setContentView(R.layout.activity_dietitian_programs) // Düzeni yükle
        DietitianBottomBarHelper.bind(this, 2) // Alt menüyü bağla

        tvCalendarValue = findViewById(R.id.tvDietCalendarValue) // Tarih etiketi
        cardSelectedBanner = findViewById(R.id.cardDietSelectedBanner) // Seçim banner'ı
        tvSelectedName = findViewById(R.id.tvDietSelectedClientName) // Seçili ad
        cardAssigned = findViewById(R.id.cardAssignedDates) // Atanmış tarihler kartı
        tvAssignedStatus = findViewById(R.id.tvAssignedDatesStatus) // Durum metni
        listProgress = findViewById(R.id.dietListProgress) // Liste yükleme
        rowProgramLoading = findViewById(R.id.rowProgramLoading) // Program yükleme
        btnSave = findViewById(R.id.btnDietSaveProgram) // Kaydet düğmesi

        dateOptions = buildNext60Days() // 60 günlük seçenekleri hazırla
        val dayRow = findViewById<LinearLayout>(R.id.dietDayRow) // Gün chip satırı
        val padH = (10 * resources.displayMetrics.density).toInt() // Yatay boşluk
        val padV = (8 * resources.displayMetrics.density).toInt() // Dikey boşluk
        dateOptions.forEach { (ymd, label) -> // Her gün için chip
            val tv = TextView(this).apply {
                text = "$label\n$ymd" // Etiket ve tarih
                setPadding(padH, padV, padH, padV) // İç boşluk
                textSize = 12f // Küçük yazı
                setOnClickListener { selectYmd(ymd) } // Tıklanınca tarih seç
            }
            dayRow.addView(
                tv,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = (6 * resources.displayMetrics.density).toInt() // Sağ boşluk
                }
            )
            dateButtons.add(tv) // Chip listesine ekle
        }
        selectYmd(selectedYmd) // Varsayılan tarihi seç
        refreshCalendarLabel() // Tarih etiketini güncelle

        findViewById<MaterialButton>(R.id.btnDietPickDate).setOnClickListener { openDatePicker() } // Takvim aç

        val etSearch = findViewById<EditText>(R.id.etDietClientSearch) // Danışan arama
        val lv = findViewById<ListView>(R.id.lvDietClients) // Danışan listesi
        clientAdapter = ClientPickAdapter() // Adaptör oluştur
        lv.adapter = clientAdapter // Adaptörü bağla
        lv.setOnItemClickListener { _, _, position, _ -> // Satır seçildiğinde
            val row = filteredRows.getOrNull(position) ?: return@setOnItemClickListener // Geçersiz konum
            selectedClientId = row.first // Kimliği sakla
            clientAdapter.notifyDataSetChanged() // Listeyi yenile
            updateSelectedUi(row.second) // Seçim arayüzünü güncelle
            loadAssignedDatesForCurrentClient() // Atanmış tarihleri yükle
        }

        etSearch.addTextChangedListener(object : TextWatcher { // Arama metni değişince
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {} // Öncesi
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {} // Değişim anı
            override fun afterTextChanged(s: Editable?) { // Değişim sonrası
                applyFilter(s?.toString().orEmpty()) // Filtreyi uygula
            }
        })

        btnSave.setOnClickListener { saveProgram() } // Programı kaydet
        val kcalWatcher = object : TextWatcher { // Kalori alanları değişince
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {} // Öncesi
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {} // Değişim anı
            override fun afterTextChanged(s: Editable?) { // Değişim sonrası
                updateTotalKcalLabel() // Toplam kaloriyi güncelle
            }
        }
        listOf(R.id.etDietBreakfastKcal, R.id.etDietLunchKcal, R.id.etDietDinnerKcal, R.id.etDietSnackKcal).forEach {
            findViewById<EditText>(it).addTextChangedListener(kcalWatcher) // Her kalori alanına dinleyici
        }
        updateTotalKcalLabel() // İlk toplam kalori
        loadClients() // Danışanları yükle
    }

    private inner class ClientPickAdapter : BaseAdapter() { // Danışan seçim adaptörü
        override fun getCount(): Int = filteredRows.size // Öğe sayısı
        override fun getItem(position: Int): String = filteredRows[position].second // Görünen ad
        override fun getItemId(position: Int): Long = position.toLong() // Öğe kimliği

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View { // Satır görünümü
            val v = convertView ?: LayoutInflater.from(this@DietitianProgramsActivity)
                .inflate(R.layout.item_diet_client_row, parent, false) // Şablonu şişir
            val id = filteredRows[position].first // Danışan kimliği
            val name = filteredRows[position].second // Danışan adı
            v.findViewById<TextView>(R.id.tvDietClientName).text = name // Adı yaz
            val selected = id == selectedClientId // Seçili mi
            if (selected) { // Seçili satır stili
                v.setBackgroundResource(R.drawable.diet_client_row_selected) // Seçili arka plan
                v.findViewById<TextView>(R.id.tvDietClientName).setTextColor(emerald) // Vurgu rengi
                v.findViewById<TextView>(R.id.tvDietClientName).setTypeface(null, Typeface.BOLD) // Kalın yazı
            } else { // Normal satır stili
                v.setBackgroundColor(Color.TRANSPARENT) // Şeffaf arka plan
                v.findViewById<TextView>(R.id.tvDietClientName).setTextColor(slateText) // Koyu metin
                v.findViewById<TextView>(R.id.tvDietClientName).setTypeface(null, Typeface.NORMAL) // Normal yazı
            }
            return v // Satırı döndür
        }
    }

    private fun openDatePicker() { // Takvim diyalogunu aç
        val parts = selectedYmd.split("-") // Tarih parçaları
        val cal = Calendar.getInstance() // Başlangıç takvimi
        if (parts.size == 3) { // Geçerli YMD ise
            try {
                cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt()) // Takvimi ayarla
            } catch (_: Exception) { } // Geçersiz parça yoksay
        }
        val dialog = DatePickerDialog(
            this,
            { _, y, m, d -> // Tarih seçildiğinde
                selectedYmd = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d) // Yeni YMD
                updateDateChipHighlights() // Chip vurgularını güncelle
                refreshCalendarLabel() // Etiketi yenile
                applyProgramEditability() // Düzenlenebilirliği ayarla
                if (selectedClientId != null) loadProgram() // Programı yeniden yükle
            },
            cal.get(Calendar.YEAR), // Başlangıç yılı
            cal.get(Calendar.MONTH), // Başlangıç ayı
            cal.get(Calendar.DAY_OF_MONTH) // Başlangıç günü
        )
        dialog.datePicker.minDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0) // Bugünün başlangıcı
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis // Geçmiş tarih seçimini engelle
        dialog.show() // Diyalogu göster
    }

    private fun refreshCalendarLabel() { // Üst tarih etiketini güncelle
        tvCalendarValue.text = formatYmdTr(selectedYmd) // Türkçe tarih metni
    }

    private fun formatYmdTr(ymd: String): String { // YMD'yi Türkçe uzun biçimle
        val p = ymd.split("-") // Tarih parçaları
        if (p.size != 3) return ymd // Geçersizse ham değer
        return try {
            val cal = Calendar.getInstance() // Takvim nesnesi
            cal.set(p[0].toInt(), p[1].toInt() - 1, p[2].toInt()) // Tarihi ayarla
            val w = SimpleDateFormat("EEE", Locale("tr", "TR")).format(cal.time) // Gün adı
            val rest = SimpleDateFormat("d MMM yyyy", Locale("tr", "TR")).format(cal.time) // Tarih kısmı
            "$w, $rest ($ymd)" // Tam etiket
        } catch (_: Exception) {
            ymd // Hata durumunda ham değer
        }
    }

    private fun updateSelectedUi(displayName: String) { // Seçili danışan arayüzünü göster
        tvSelectedName.text = displayName // Adı yaz
        cardSelectedBanner.visibility = View.VISIBLE // Banner'ı göster
        cardAssigned.visibility = View.VISIBLE // Atanmış tarih kartını göster
    }

    private fun clearSelectionUi() { // Seçim arayüzünü gizle
        cardSelectedBanner.visibility = View.GONE // Banner'ı gizle
        cardAssigned.visibility = View.GONE // Atanmış kartı gizle
        tvAssignedStatus.text = "" // Durum metnini temizle
    }

    private fun kcalFromEdit(id: Int): Int { // EditText'ten kalori değeri oku
        return findViewById<EditText>(id).text.toString().toIntOrNull()?.coerceAtLeast(0) ?: 0 // Negatif olmaz
    }

    private fun updateTotalKcalLabel() { // Toplam kalori etiketini güncelle
        val sum = kcalFromEdit(R.id.etDietBreakfastKcal) +
            kcalFromEdit(R.id.etDietLunchKcal) +
            kcalFromEdit(R.id.etDietDinnerKcal) +
            kcalFromEdit(R.id.etDietSnackKcal) // Tüm öğünlerin toplamı
        findViewById<TextView>(R.id.tvDietTotalKcal).text = "$sum kkal" // Toplamı yaz
    }

    private fun selectYmd(ymd: String) { // Program tarihini seç
        selectedYmd = ymd // Tarihi sakla
        updateDateChipHighlights() // Chip vurgularını güncelle
        refreshCalendarLabel() // Etiketi yenile
        applyProgramEditability() // Düzenlenebilirliği ayarla
        if (selectedClientId != null) loadProgram() // Programı yükle
    }

    private fun styleDayChip(tv: TextView, active: Boolean) { // Gün chip stilini uygula
        if (active) { // Seçili chip
            tv.setBackgroundColor(emerald) // Yeşil arka plan
            tv.setTextColor(Color.WHITE) // Beyaz metin
        } else { // Pasif chip
            tv.setBackgroundColor(emeraldMutedBg) // Açık yeşil arka plan
            tv.setTextColor(slateText) // Koyu metin
        }
    }

    private fun styleAssignedChip(tv: TextView, active: Boolean) { // Atanmış tarih chip stili
        if (active) { // Seçili atanmış tarih
            tv.setBackgroundColor(amberActive) // Turuncu arka plan
            tv.setTextColor(Color.WHITE) // Beyaz metin
        } else { // Pasif atanmış tarih
            tv.setBackgroundResource(R.drawable.diet_assigned_chip_bg) // Varsayılan arka plan
            tv.setTextColor(slateText) // Koyu metin
        }
    }

    private fun updateDateChipHighlights() { // Tüm tarih chip vurgularını güncelle
        val ymd = selectedYmd // Aktif tarih
        dateOptions.forEachIndexed { i, pair -> // Gün chip'leri
            val active = pair.first == ymd // Eşleşme kontrolü
            dateButtons.getOrNull(i)?.let { styleDayChip(it, active) } // Stili uygula
        }
        assignedChipViews.forEach { (d, tv) -> // Atanmış chip'ler
            styleAssignedChip(tv, d == ymd) // Aktif olanı vurgula
        }
    }

    private fun rebuildAssignedDateChips(dates: List<String>) { // Atanmış tarih chip'lerini yeniden oluştur
        val row = findViewById<LinearLayout>(R.id.dietAssignedRow) // Chip satırı
        row.removeAllViews() // Eski chip'leri temizle
        assignedChipViews.clear() // Bellek listesini temizle
        val padH = (10 * resources.displayMetrics.density).toInt() // Yatay boşluk
        val padV = (8 * resources.displayMetrics.density).toInt() // Dikey boşluk
        val fmt = SimpleDateFormat("EEE d MMM", Locale("tr", "TR")) // Gün etiketi biçimi
        dates.forEach { ymdStr -> // Her atanmış tarih
            val cal = Calendar.getInstance() // Takvim nesnesi
            val parts = ymdStr.split("-") // Tarih parçaları
            val label = if (parts.size == 3) { // Geçerli YMD ise
                try {
                    cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt()) // Tarihi ayarla
                    fmt.format(cal.time) // Türkçe etiket
                } catch (_: Exception) {
                    ymdStr // Hata durumunda ham değer
                }
            } else {
                ymdStr // Geçersiz formatta ham değer
            }
            val tv = TextView(this).apply {
                text = "$label\n$ymdStr" // Etiket ve tarih
                setPadding(padH, padV, padH, padV) // İç boşluk
                textSize = 12f // Küçük yazı
                setOnClickListener { selectYmd(ymdStr) } // Tıklanınca tarih seç
            }
            tv.setBackgroundResource(R.drawable.diet_assigned_chip_bg) // Varsayılan arka plan
            tv.setTextColor(slateText) // Koyu metin
            assignedChipViews.add(ymdStr to tv) // Listeye ekle
            row.addView(
                tv,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = (8 * resources.displayMetrics.density).toInt() // Sağ boşluk
                }
            )
        }
    }

    private fun loadAssignedDatesForCurrentClient() { // Seçili danışanın atanmış tarihlerini yükle
        val cid = selectedClientId ?: return // Danışan yoksa çık
        tvAssignedStatus.text = "Yükleniyor…" // Durum metni
        lifecycleScope.launch { // Coroutine ile istek
            try {
                val r = RetrofitClient.instance.getDietProgramDates(cid) // Tarih listesi getir
                val list = if (r.isSuccessful) r.body().orEmpty() else emptyList() // Başarılıysa liste
                rebuildAssignedDateChips(list) // Chip'leri yeniden oluştur
                updateDateChipHighlights() // Vurguları güncelle
                tvAssignedStatus.text = when { // Durum mesajı
                    list.isEmpty() -> "Henüz kayıt yok; aşağıdan tarih seçip kaydedin." // Boş liste
                    else -> "" // Kayıt varsa boş
                }
            } catch (_: Exception) { // Hata durumu
                rebuildAssignedDateChips(emptyList()) // Chip'leri temizle
                updateDateChipHighlights() // Vurguları güncelle
                tvAssignedStatus.text = "Tarihler alınamadı." // Hata mesajı
            }
            loadProgram() // Seçili tarihin programını yükle
        }
    }

    private fun applyFilter(q: String) { // Danışan arama filtresini uygula
        val t = q.trim() // Temizlenmiş sorgu
        filteredRows = if (t.isEmpty()) { // Boş sorgu
            clientRows // Tüm danışanlar
        } else {
            clientRows.filter { it.second.contains(t, ignoreCase = true) } // Ada göre filtre
        }
        clientAdapter.notifyDataSetChanged() // Listeyi yenile
    }

    private fun loadClients() { // Danışan listesini API'den al
        listProgress.visibility = View.VISIBLE // Yükleniyor göster
        lifecycleScope.launch { // Coroutine ile istek
            try {
                val r = RetrofitClient.instance.getClientsWithLastMeal() // Danışanları getir
                if (!r.isSuccessful) { // Başarısız yanıt
                    Toast.makeText(this@DietitianProgramsActivity, "Danışan listesi alınamadı", Toast.LENGTH_LONG).show() // Bildirim
                    return@launch // Çık
                }
                val list = r.body().orEmpty() // Danışan listesi
                clientRows = list.mapNotNull { c -> // Kimlik ve ad çiftleri
                    val id = c.id ?: return@mapNotNull null // Kimlik yoksa atla
                    val label = listOf(c.firstName, c.lastName).mapNotNull { it?.trim() }.filter { it.isNotEmpty() }
                        .joinToString(" ").ifEmpty { "Danışan" } // Görünen ad
                    id to label // Çift oluştur
                }
                if (selectedClientId != null && clientRows.none { it.first == selectedClientId }) { // Seçim geçersizse
                    selectedClientId = null // Seçimi temizle
                }
                applyFilter(findViewById<EditText>(R.id.etDietClientSearch).text.toString()) // Mevcut filtreyi uygula
                selectedClientId?.let { id -> // Seçim varsa
                    val name = clientRows.find { it.first == id }?.second // Adı bul
                    if (name != null) updateSelectedUi(name) else clearSelectionUi() // Arayüzü güncelle
                } ?: clearSelectionUi() // Seçim yoksa gizle
                selectedClientId?.let { loadAssignedDatesForCurrentClient() } // Atanmış tarihleri yükle
            } catch (e: Exception) { // İstisna
                Toast.makeText(this@DietitianProgramsActivity, e.message ?: "Hata", Toast.LENGTH_LONG).show() // Bildirim
            } finally {
                listProgress.visibility = View.GONE // Yüklemeyi gizle
            }
        }
    }

    private fun loadProgram() { // Seçili tarihin programını yükle
        val cid = selectedClientId ?: return // Danışan yoksa çık
        rowProgramLoading.visibility = View.VISIBLE // Yükleniyor göster
        btnSave.isEnabled = false // Kaydet düğmesini kapat
        lifecycleScope.launch { // Coroutine ile istek
            try {
                val r = RetrofitClient.instance.getDietProgram(cid, selectedYmd.trim()) // Program getir
                if (!r.isSuccessful) { // Başarısız yanıt
                    clearMealFields() // Alanları temizle
                    return@launch // Çık
                }
                val p = r.body() // Program gövdesi
                if (p == null) { // Gövde boşsa
                    clearMealFields() // Alanları temizle
                    return@launch // Çık
                }
                findViewById<EditText>(R.id.etDietBreakfast).setText(p.breakfast.orEmpty()) // Kahvaltı metni
                findViewById<EditText>(R.id.etDietLunch).setText(p.lunch.orEmpty()) // Öğle metni
                findViewById<EditText>(R.id.etDietDinner).setText(p.dinner.orEmpty()) // Akşam metni
                findViewById<EditText>(R.id.etDietSnack).setText(p.snack.orEmpty()) // Atıştırma metni
                findViewById<EditText>(R.id.etDietBreakfastKcal).setText((p.breakfastCalories ?: 0).toString()) // Kahvaltı kalori
                findViewById<EditText>(R.id.etDietLunchKcal).setText((p.lunchCalories ?: 0).toString()) // Öğle kalori
                findViewById<EditText>(R.id.etDietDinnerKcal).setText((p.dinnerCalories ?: 0).toString()) // Akşam kalori
                findViewById<EditText>(R.id.etDietSnackKcal).setText((p.snackCalories ?: 0).toString()) // Atıştırma kalori
                updateTotalKcalLabel() // Toplam kaloriyi güncelle
            } catch (_: Exception) { // Hata durumu
                clearMealFields() // Alanları temizle
            } finally {
                rowProgramLoading.visibility = View.GONE // Yüklemeyi gizle
                applyProgramEditability() // Düzenlenebilirliği yeniden ayarla
            }
        }
    }

    private fun clearMealFields() { // Öğün alanlarını sıfırla
        findViewById<EditText>(R.id.etDietBreakfast).setText("") // Kahvaltı boş
        findViewById<EditText>(R.id.etDietLunch).setText("") // Öğle boş
        findViewById<EditText>(R.id.etDietDinner).setText("") // Akşam boş
        findViewById<EditText>(R.id.etDietSnack).setText("") // Atıştırma boş
        listOf(
            R.id.etDietBreakfastKcal, R.id.etDietLunchKcal, R.id.etDietDinnerKcal, R.id.etDietSnackKcal
        ).forEach { findViewById<EditText>(it).setText("0") } // Kalorileri sıfırla
        updateTotalKcalLabel() // Toplamı güncelle
    }

    private fun applyProgramEditability() { // Geçmiş tarihte düzenlemeyi kapat
        val readOnly = isPastProgramDate(selectedYmd.trim()) // Salt okunur mu
        val fields = listOf(
            R.id.etDietBreakfast, R.id.etDietLunch, R.id.etDietDinner, R.id.etDietSnack,
            R.id.etDietBreakfastKcal, R.id.etDietLunchKcal, R.id.etDietDinnerKcal, R.id.etDietSnackKcal
        ) // Tüm program alanları
        fields.forEach { id ->
            findViewById<EditText>(id).isEnabled = !readOnly // Alan etkinliği
        }
        btnSave.isEnabled = !readOnly && selectedClientId != null // Kaydet düğmesi
        btnSave.text = if (readOnly) "Geçmiş tarih — kayıt kapalı" else "Programı kaydet" // Düğme metni
    }

    private fun saveProgram() { // Programı API'ye kaydet
        val cid = selectedClientId // Danışan kimliği
        if (cid.isNullOrEmpty()) { // Danışan seçilmemişse
            Toast.makeText(this, "Önce danışan seçin.", Toast.LENGTH_SHORT).show() // Uyarı
            return // Çık
        }
        if (!"""^\d{4}-\d{2}-\d{2}$""".toRegex().matches(selectedYmd.trim())) { // Tarih formatı kontrolü
            Toast.makeText(this, "Geçerli bir program tarihi seçin.", Toast.LENGTH_LONG).show() // Uyarı
            return // Çık
        }
        if (isPastProgramDate(selectedYmd.trim())) { // Geçmiş tarih kontrolü
            Toast.makeText(this, "Geçmiş tarihlerdeki programlar güncellenemez.", Toast.LENGTH_LONG).show() // Uyarı
            return // Çık
        }
        val b = findViewById<EditText>(R.id.etDietBreakfast).text.toString() // Kahvaltı metni
        val l = findViewById<EditText>(R.id.etDietLunch).text.toString() // Öğle metni
        val d = findViewById<EditText>(R.id.etDietDinner).text.toString() // Akşam metni
        val s = findViewById<EditText>(R.id.etDietSnack).text.toString() // Atıştırma metni
        val bK = kcalFromEdit(R.id.etDietBreakfastKcal) // Kahvaltı kalori
        val lK = kcalFromEdit(R.id.etDietLunchKcal) // Öğle kalori
        val dK = kcalFromEdit(R.id.etDietDinnerKcal) // Akşam kalori
        val sK = kcalFromEdit(R.id.etDietSnackKcal) // Atıştırma kalori
        val totalK = bK + lK + dK + sK // Toplam kalori
        btnSave.isEnabled = false // Çift kaydı engelle
        lifecycleScope.launch { // Coroutine ile istek
            try {
                val r = RetrofitClient.instance.saveDietProgram(
                    SaveDietProgramRequest(
                        clientId = cid, // Danışan kimliği
                        programDate = selectedYmd.trim(), // Program tarihi
                        breakfast = b, // Kahvaltı
                        lunch = l, // Öğle
                        dinner = d, // Akşam
                        snack = s, // Atıştırma
                        breakfastCalories = bK, // Kahvaltı kalori
                        lunchCalories = lK, // Öğle kalori
                        dinnerCalories = dK, // Akşam kalori
                        snackCalories = sK, // Atıştırma kalori
                        totalCalories = totalK // Toplam kalori
                    )
                )
                if (r.isSuccessful) { // Kayıt başarılı
                    Toast.makeText(
                        this@DietitianProgramsActivity,
                        "Program kaydedildi. Aynı danışan ve tarih için tekrar düzenleyebilirsiniz.",
                        Toast.LENGTH_LONG // Bilgi mesajı
                    ).show()
                    loadAssignedDatesForCurrentClient() // Atanmış tarihleri yenile
                } else { // Kayıt başarısız
                    Toast.makeText(
                        this@DietitianProgramsActivity,
                        "Kayıt başarısız: ${readErrorMessage(r)}",
                        Toast.LENGTH_LONG // Hata mesajı
                    ).show()
                }
            } catch (e: Exception) { // İstisna
                Toast.makeText(this@DietitianProgramsActivity, e.message ?: "Bağlantı hatası", Toast.LENGTH_LONG).show() // Bildirim
            } finally {
                btnSave.isEnabled = true // Düğmeyi tekrar aç
            }
        }
    }

    private fun readErrorMessage(response: Response<*>): String { // API hata mesajını çöz
        val raw = response.errorBody()?.string().orEmpty() // Ham hata gövdesi
        return try {
            JSONObject(raw).optString("message").ifBlank { "HTTP ${response.code()}" } // JSON mesajı
        } catch (_: Exception) { // JSON değilse
            if (raw.isNotBlank()) raw else "HTTP ${response.code()}" // Ham veya varsayılan
        }
    }
}
