package com.example.nightbrate

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DietitianProgramsActivity : AppCompatActivity() {

    private var selectedYmd: String = todayYmd()
    private lateinit var dateOptions: List<Pair<String, String>>
    private val dateButtons = mutableListOf<TextView>()

    private var selectedClientId: String? = null
    private var clientRows: List<Pair<String, String>> = emptyList()
    private var filteredRows: List<Pair<String, String>> = emptyList()
    private val assignedChipViews = mutableListOf<Pair<String, TextView>>()

    private lateinit var clientAdapter: ClientPickAdapter
    private lateinit var tvCalendarValue: TextView
    private lateinit var cardSelectedBanner: View
    private lateinit var tvSelectedName: TextView
    private lateinit var cardAssigned: View
    private lateinit var tvAssignedStatus: TextView
    private lateinit var listProgress: ProgressBar
    private lateinit var rowProgramLoading: View
    private lateinit var btnSave: MaterialButton

    private val emerald = Color.parseColor("#22C55E")
    private val emeraldMutedBg = Color.parseColor("#F0FDF4")
    private val slateBorder = Color.parseColor("#E2E8F0")
    private val slateText = Color.parseColor("#0F172A")
    private val mutedText by lazy { ContextCompat.getColor(this, R.color.admin_muted) }
    private val amberActive = Color.parseColor("#D97706")

    private fun todayYmd(): String = calendarToYmd(Calendar.getInstance())

    private fun calendarToYmd(c: Calendar): String {
        val y = c.get(Calendar.YEAR)
        val m = (c.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val d = c.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        return "$y-$m-$d"
    }

    private fun buildNext60Days(): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val fmt = SimpleDateFormat("EEE d MMM", Locale("tr", "TR"))
        for (i in 0 until 60) {
            val c = cal.clone() as Calendar
            c.add(Calendar.DAY_OF_MONTH, i)
            out.add(calendarToYmd(c) to fmt.format(c.time))
        }
        return out
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dietitian_programs)
        DietitianBottomBarHelper.bind(this, 2)

        tvCalendarValue = findViewById(R.id.tvDietCalendarValue)
        cardSelectedBanner = findViewById(R.id.cardDietSelectedBanner)
        tvSelectedName = findViewById(R.id.tvDietSelectedClientName)
        cardAssigned = findViewById(R.id.cardAssignedDates)
        tvAssignedStatus = findViewById(R.id.tvAssignedDatesStatus)
        listProgress = findViewById(R.id.dietListProgress)
        rowProgramLoading = findViewById(R.id.rowProgramLoading)
        btnSave = findViewById(R.id.btnDietSaveProgram)

        dateOptions = buildNext60Days()
        val dayRow = findViewById<LinearLayout>(R.id.dietDayRow)
        val padH = (10 * resources.displayMetrics.density).toInt()
        val padV = (8 * resources.displayMetrics.density).toInt()
        dateOptions.forEach { (ymd, label) ->
            val tv = TextView(this).apply {
                text = "$label\n$ymd"
                setPadding(padH, padV, padH, padV)
                textSize = 12f
                setOnClickListener { selectYmd(ymd) }
            }
            dayRow.addView(
                tv,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = (6 * resources.displayMetrics.density).toInt()
                }
            )
            dateButtons.add(tv)
        }
        selectYmd(selectedYmd)
        refreshCalendarLabel()

        findViewById<MaterialButton>(R.id.btnDietPickDate).setOnClickListener { openDatePicker() }

        val etSearch = findViewById<EditText>(R.id.etDietClientSearch)
        val lv = findViewById<ListView>(R.id.lvDietClients)
        clientAdapter = ClientPickAdapter()
        lv.adapter = clientAdapter
        lv.setOnItemClickListener { _, _, position, _ ->
            val row = filteredRows.getOrNull(position) ?: return@setOnItemClickListener
            selectedClientId = row.first
            clientAdapter.notifyDataSetChanged()
            updateSelectedUi(row.second)
            loadAssignedDatesForCurrentClient()
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applyFilter(s?.toString().orEmpty())
            }
        })

        btnSave.setOnClickListener { saveProgram() }
        val kcalWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateTotalKcalLabel()
            }
        }
        listOf(R.id.etDietBreakfastKcal, R.id.etDietLunchKcal, R.id.etDietDinnerKcal, R.id.etDietSnackKcal).forEach {
            findViewById<EditText>(it).addTextChangedListener(kcalWatcher)
        }
        updateTotalKcalLabel()
        loadClients()
    }

    private inner class ClientPickAdapter : BaseAdapter() {
        override fun getCount(): Int = filteredRows.size
        override fun getItem(position: Int): String = filteredRows[position].second
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val v = convertView ?: LayoutInflater.from(this@DietitianProgramsActivity)
                .inflate(R.layout.item_diet_client_row, parent, false)
            val id = filteredRows[position].first
            val name = filteredRows[position].second
            v.findViewById<TextView>(R.id.tvDietClientName).text = name
            val selected = id == selectedClientId
            if (selected) {
                v.setBackgroundResource(R.drawable.diet_client_row_selected)
                v.findViewById<TextView>(R.id.tvDietClientName).setTextColor(emerald)
                v.findViewById<TextView>(R.id.tvDietClientName).setTypeface(null, Typeface.BOLD)
            } else {
                v.setBackgroundColor(Color.TRANSPARENT)
                v.findViewById<TextView>(R.id.tvDietClientName).setTextColor(slateText)
                v.findViewById<TextView>(R.id.tvDietClientName).setTypeface(null, Typeface.NORMAL)
            }
            return v
        }
    }

    private fun openDatePicker() {
        val parts = selectedYmd.split("-")
        val cal = Calendar.getInstance()
        if (parts.size == 3) {
            try {
                cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
            } catch (_: Exception) { }
        }
        DatePickerDialog(
            this,
            { _, y, m, d ->
                selectedYmd = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                updateDateChipHighlights()
                refreshCalendarLabel()
                if (selectedClientId != null) loadProgram()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun refreshCalendarLabel() {
        tvCalendarValue.text = formatYmdTr(selectedYmd)
    }

    private fun formatYmdTr(ymd: String): String {
        val p = ymd.split("-")
        if (p.size != 3) return ymd
        return try {
            val cal = Calendar.getInstance()
            cal.set(p[0].toInt(), p[1].toInt() - 1, p[2].toInt())
            val w = SimpleDateFormat("EEE", Locale("tr", "TR")).format(cal.time)
            val rest = SimpleDateFormat("d MMM yyyy", Locale("tr", "TR")).format(cal.time)
            "$w, $rest ($ymd)"
        } catch (_: Exception) {
            ymd
        }
    }

    private fun updateSelectedUi(displayName: String) {
        tvSelectedName.text = displayName
        cardSelectedBanner.visibility = View.VISIBLE
        cardAssigned.visibility = View.VISIBLE
    }

    private fun clearSelectionUi() {
        cardSelectedBanner.visibility = View.GONE
        cardAssigned.visibility = View.GONE
        tvAssignedStatus.text = ""
    }

    private fun kcalFromEdit(id: Int): Int {
        return findViewById<EditText>(id).text.toString().toIntOrNull()?.coerceAtLeast(0) ?: 0
    }

    private fun updateTotalKcalLabel() {
        val sum = kcalFromEdit(R.id.etDietBreakfastKcal) +
            kcalFromEdit(R.id.etDietLunchKcal) +
            kcalFromEdit(R.id.etDietDinnerKcal) +
            kcalFromEdit(R.id.etDietSnackKcal)
        findViewById<TextView>(R.id.tvDietTotalKcal).text = "$sum kkal"
    }

    private fun selectYmd(ymd: String) {
        selectedYmd = ymd
        updateDateChipHighlights()
        refreshCalendarLabel()
        if (selectedClientId != null) loadProgram()
    }

    private fun styleDayChip(tv: TextView, active: Boolean) {
        if (active) {
            tv.setBackgroundColor(emerald)
            tv.setTextColor(Color.WHITE)
        } else {
            tv.setBackgroundColor(emeraldMutedBg)
            tv.setTextColor(slateText)
        }
    }

    private fun styleAssignedChip(tv: TextView, active: Boolean) {
        if (active) {
            tv.setBackgroundColor(amberActive)
            tv.setTextColor(Color.WHITE)
        } else {
            tv.setBackgroundResource(R.drawable.diet_assigned_chip_bg)
            tv.setTextColor(slateText)
        }
    }

    private fun updateDateChipHighlights() {
        val ymd = selectedYmd
        dateOptions.forEachIndexed { i, pair ->
            val active = pair.first == ymd
            dateButtons.getOrNull(i)?.let { styleDayChip(it, active) }
        }
        assignedChipViews.forEach { (d, tv) ->
            styleAssignedChip(tv, d == ymd)
        }
    }

    private fun rebuildAssignedDateChips(dates: List<String>) {
        val row = findViewById<LinearLayout>(R.id.dietAssignedRow)
        row.removeAllViews()
        assignedChipViews.clear()
        val padH = (10 * resources.displayMetrics.density).toInt()
        val padV = (8 * resources.displayMetrics.density).toInt()
        val fmt = SimpleDateFormat("EEE d MMM", Locale("tr", "TR"))
        dates.forEach { ymdStr ->
            val cal = Calendar.getInstance()
            val parts = ymdStr.split("-")
            val label = if (parts.size == 3) {
                try {
                    cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                    fmt.format(cal.time)
                } catch (_: Exception) {
                    ymdStr
                }
            } else {
                ymdStr
            }
            val tv = TextView(this).apply {
                text = "$label\n$ymdStr"
                setPadding(padH, padV, padH, padV)
                textSize = 12f
                setOnClickListener { selectYmd(ymdStr) }
            }
            tv.setBackgroundResource(R.drawable.diet_assigned_chip_bg)
            tv.setTextColor(slateText)
            assignedChipViews.add(ymdStr to tv)
            row.addView(
                tv,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = (8 * resources.displayMetrics.density).toInt()
                }
            )
        }
    }

    private fun loadAssignedDatesForCurrentClient() {
        val cid = selectedClientId ?: return
        tvAssignedStatus.text = "Yükleniyor…"
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.getDietProgramDates(cid)
                val list = if (r.isSuccessful) r.body().orEmpty() else emptyList()
                rebuildAssignedDateChips(list)
                updateDateChipHighlights()
                tvAssignedStatus.text = when {
                    list.isEmpty() -> "Henüz kayıt yok; aşağıdan tarih seçip kaydedin."
                    else -> ""
                }
            } catch (_: Exception) {
                rebuildAssignedDateChips(emptyList())
                updateDateChipHighlights()
                tvAssignedStatus.text = "Tarihler alınamadı."
            }
            loadProgram()
        }
    }

    private fun applyFilter(q: String) {
        val t = q.trim()
        filteredRows = if (t.isEmpty()) {
            clientRows
        } else {
            clientRows.filter { it.second.contains(t, ignoreCase = true) }
        }
        clientAdapter.notifyDataSetChanged()
    }

    private fun loadClients() {
        listProgress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.getClientsWithLastMeal()
                if (!r.isSuccessful) {
                    Toast.makeText(this@DietitianProgramsActivity, "Danışan listesi alınamadı", Toast.LENGTH_LONG).show()
                    return@launch
                }
                val list = r.body().orEmpty()
                clientRows = list.mapNotNull { c ->
                    val id = c.id ?: return@mapNotNull null
                    val label = listOf(c.firstName, c.lastName).mapNotNull { it?.trim() }.filter { it.isNotEmpty() }
                        .joinToString(" ").ifEmpty { "Danışan" }
                    id to label
                }
                if (selectedClientId != null && clientRows.none { it.first == selectedClientId }) {
                    selectedClientId = null
                }
                applyFilter(findViewById<EditText>(R.id.etDietClientSearch).text.toString())
                selectedClientId?.let { id ->
                    val name = clientRows.find { it.first == id }?.second
                    if (name != null) updateSelectedUi(name) else clearSelectionUi()
                } ?: clearSelectionUi()
                selectedClientId?.let { loadAssignedDatesForCurrentClient() }
            } catch (e: Exception) {
                Toast.makeText(this@DietitianProgramsActivity, e.message ?: "Hata", Toast.LENGTH_LONG).show()
            } finally {
                listProgress.visibility = View.GONE
            }
        }
    }

    private fun loadProgram() {
        val cid = selectedClientId ?: return
        rowProgramLoading.visibility = View.VISIBLE
        btnSave.isEnabled = false
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.getDietProgram(cid, selectedYmd.trim())
                if (!r.isSuccessful) {
                    clearMealFields()
                    return@launch
                }
                val p = r.body()
                if (p == null) {
                    clearMealFields()
                    return@launch
                }
                findViewById<EditText>(R.id.etDietBreakfast).setText(p.breakfast.orEmpty())
                findViewById<EditText>(R.id.etDietLunch).setText(p.lunch.orEmpty())
                findViewById<EditText>(R.id.etDietDinner).setText(p.dinner.orEmpty())
                findViewById<EditText>(R.id.etDietSnack).setText(p.snack.orEmpty())
                findViewById<EditText>(R.id.etDietBreakfastKcal).setText((p.breakfastCalories ?: 0).toString())
                findViewById<EditText>(R.id.etDietLunchKcal).setText((p.lunchCalories ?: 0).toString())
                findViewById<EditText>(R.id.etDietDinnerKcal).setText((p.dinnerCalories ?: 0).toString())
                findViewById<EditText>(R.id.etDietSnackKcal).setText((p.snackCalories ?: 0).toString())
                updateTotalKcalLabel()
            } catch (_: Exception) {
                clearMealFields()
            } finally {
                rowProgramLoading.visibility = View.GONE
                btnSave.isEnabled = true
            }
        }
    }

    private fun clearMealFields() {
        findViewById<EditText>(R.id.etDietBreakfast).setText("")
        findViewById<EditText>(R.id.etDietLunch).setText("")
        findViewById<EditText>(R.id.etDietDinner).setText("")
        findViewById<EditText>(R.id.etDietSnack).setText("")
        listOf(
            R.id.etDietBreakfastKcal, R.id.etDietLunchKcal, R.id.etDietDinnerKcal, R.id.etDietSnackKcal
        ).forEach { findViewById<EditText>(it).setText("0") }
        updateTotalKcalLabel()
    }

    private fun saveProgram() {
        val cid = selectedClientId
        if (cid.isNullOrEmpty()) {
            Toast.makeText(this, "Önce danışan seçin.", Toast.LENGTH_SHORT).show()
            return
        }
        if (!"""^\d{4}-\d{2}-\d{2}$""".toRegex().matches(selectedYmd.trim())) {
            Toast.makeText(this, "Geçerli bir program tarihi seçin.", Toast.LENGTH_LONG).show()
            return
        }
        val b = findViewById<EditText>(R.id.etDietBreakfast).text.toString()
        val l = findViewById<EditText>(R.id.etDietLunch).text.toString()
        val d = findViewById<EditText>(R.id.etDietDinner).text.toString()
        val s = findViewById<EditText>(R.id.etDietSnack).text.toString()
        val bK = kcalFromEdit(R.id.etDietBreakfastKcal)
        val lK = kcalFromEdit(R.id.etDietLunchKcal)
        val dK = kcalFromEdit(R.id.etDietDinnerKcal)
        val sK = kcalFromEdit(R.id.etDietSnackKcal)
        val totalK = bK + lK + dK + sK
        btnSave.isEnabled = false
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.saveDietProgram(
                    SaveDietProgramRequest(
                        clientId = cid,
                        programDate = selectedYmd.trim(),
                        breakfast = b,
                        lunch = l,
                        dinner = d,
                        snack = s,
                        breakfastCalories = bK,
                        lunchCalories = lK,
                        dinnerCalories = dK,
                        snackCalories = sK,
                        totalCalories = totalK
                    )
                )
                if (r.isSuccessful) {
                    Toast.makeText(
                        this@DietitianProgramsActivity,
                        "Program kaydedildi. Aynı danışan ve tarih için tekrar düzenleyebilirsiniz.",
                        Toast.LENGTH_LONG
                    ).show()
                    loadAssignedDatesForCurrentClient()
                } else {
                    Toast.makeText(
                        this@DietitianProgramsActivity,
                        "Kayıt başarısız: ${readErrorMessage(r)}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DietitianProgramsActivity, e.message ?: "Bağlantı hatası", Toast.LENGTH_LONG).show()
            } finally {
                btnSave.isEnabled = true
            }
        }
    }

    private fun readErrorMessage(response: Response<*>): String {
        val raw = response.errorBody()?.string().orEmpty()
        return try {
            JSONObject(raw).optString("message").ifBlank { "HTTP ${response.code()}" }
        } catch (_: Exception) {
            if (raw.isNotBlank()) raw else "HTTP ${response.code()}"
        }
    }
}
