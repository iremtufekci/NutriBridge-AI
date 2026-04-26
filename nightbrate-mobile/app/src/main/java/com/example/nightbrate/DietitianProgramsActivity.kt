package com.example.nightbrate

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
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

        dateOptions = buildNext60Days()
        val dayRow = findViewById<LinearLayout>(R.id.dietDayRow)
        val padH = (8 * resources.displayMetrics.density).toInt()
        val padV = (6 * resources.displayMetrics.density).toInt()
        dateOptions.forEach { (ymd, label) ->
            val tv = TextView(this).apply {
                text = "$label\n$ymd"
                setPadding(padH, padV, padH, padV)
                setTextColor(Color.WHITE)
                textSize = 9f
            }
            tv.setOnClickListener { selectYmd(ymd) }
            dayRow.addView(
                tv,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = padH / 2
                }
            )
            dateButtons.add(tv)
        }
        selectYmd(selectedYmd)

        val etSearch = findViewById<EditText>(R.id.etDietClientSearch)
        val lv = findViewById<ListView>(R.id.lvDietClients)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applyFilter(s?.toString().orEmpty())
            }
        })

        lv.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val row = filteredRows.getOrNull(position) ?: return@OnItemClickListener
            selectedClientId = row.first
            updateSelectedClientLabel(row.second)
            loadAssignedDatesForCurrentClient()
        }

        findViewById<Button>(R.id.btnDietSaveProgram).setOnClickListener { saveProgram() }
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

    private fun kcalFromEdit(id: Int): Int {
        return findViewById<EditText>(id).text.toString().toIntOrNull()?.coerceAtLeast(0) ?: 0
    }

    private fun updateTotalKcalLabel() {
        val sum = kcalFromEdit(R.id.etDietBreakfastKcal) +
            kcalFromEdit(R.id.etDietLunchKcal) +
            kcalFromEdit(R.id.etDietDinnerKcal) +
            kcalFromEdit(R.id.etDietSnackKcal)
        findViewById<TextView>(R.id.tvDietTotalKcal).text = "$sum kcal"
    }

    private fun selectYmd(ymd: String) {
        selectedYmd = ymd
        updateDateChipHighlights()
        if (selectedClientId != null) loadProgram()
    }

    private fun updateDateChipHighlights() {
        val ymd = selectedYmd
        dateOptions.forEachIndexed { i, pair ->
            val active = pair.first == ymd
            dateButtons.getOrNull(i)?.setBackgroundColor(
                if (active) Color.parseColor("#22C55E") else Color.parseColor("#334155")
            )
        }
        assignedChipViews.forEach { (d, tv) ->
            tv.setBackgroundColor(
                if (d == ymd) Color.parseColor("#D97706") else Color.parseColor("#52525B")
            )
        }
    }

    private fun rebuildAssignedDateChips(dates: List<String>) {
        val row = findViewById<LinearLayout>(R.id.dietAssignedRow)
        row.removeAllViews()
        assignedChipViews.clear()
        val padH = (8 * resources.displayMetrics.density).toInt()
        val padV = (6 * resources.displayMetrics.density).toInt()
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
                setTextColor(Color.WHITE)
                textSize = 9f
            }
            tv.setOnClickListener { selectYmd(ymdStr) }
            assignedChipViews.add(ymdStr to tv)
            row.addView(
                tv,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = padH / 2
                }
            )
        }
    }

    private fun loadAssignedDatesForCurrentClient() {
        val cid = selectedClientId ?: return
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.getDietProgramDates(cid)
                val list = if (r.isSuccessful) r.body().orEmpty() else emptyList()
                rebuildAssignedDateChips(list)
                updateDateChipHighlights()
            } catch (_: Exception) {
                rebuildAssignedDateChips(emptyList())
                updateDateChipHighlights()
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
        val lv = findViewById<ListView>(R.id.lvDietClients)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, filteredRows.map { it.second })
        lv.adapter = adapter
    }

    private fun loadClients() {
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.getClientsWithLastMeal()
                if (!r.isSuccessful) {
                    Toast.makeText(this@DietitianProgramsActivity, "Danisan listesi alinamadi", Toast.LENGTH_LONG).show()
                    return@launch
                }
                val list = r.body().orEmpty()
                clientRows = list.mapNotNull { c ->
                    val id = c.id ?: return@mapNotNull null
                    val label = listOf(c.firstName, c.lastName).mapNotNull { it?.trim() }.filter { it.isNotEmpty() }
                        .joinToString(" ").ifEmpty { "Danisan" }
                    id to label
                }
                applyFilter(findViewById<EditText>(R.id.etDietClientSearch).text.toString())
                updateSelectedClientLabelFromId()
                selectedClientId?.let { loadAssignedDatesForCurrentClient() }
            } catch (e: Exception) {
                Toast.makeText(this@DietitianProgramsActivity, e.message ?: "Hata", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadProgram() {
        val cid = selectedClientId ?: return
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

    private fun updateSelectedClientLabel(name: String) {
        val tv = findViewById<TextView>(R.id.tvDietSelectedClient)
        tv.text = "Secili danisan:\n$name"
    }

    private fun clearSelectedClientLabel() {
        val tv = findViewById<TextView>(R.id.tvDietSelectedClient)
        tv.text = "Secili danisan: —\nArayip listeden secin"
    }

    private fun updateSelectedClientLabelFromId() {
        val id = selectedClientId ?: return
        val name = clientRows.find { it.first == id }?.second
        if (name != null) updateSelectedClientLabel(name)
    }

    private fun saveProgram() {
        val cid = selectedClientId
        if (cid.isNullOrEmpty()) {
            Toast.makeText(this, "Once danisan secin", Toast.LENGTH_SHORT).show()
            return
        }
        if (!"""^\d{4}-\d{2}-\d{2}$""".toRegex().matches(selectedYmd.trim())) {
            Toast.makeText(this, "Once yukaridan bir program tarihi secin", Toast.LENGTH_LONG).show()
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
                    Toast.makeText(this@DietitianProgramsActivity, "Program kaydedildi. Ayni danisan ve tarih icin tekrar duzenleyebilirsiniz.", Toast.LENGTH_LONG).show()
                    loadAssignedDatesForCurrentClient()
                } else {
                    Toast.makeText(this@DietitianProgramsActivity, "Kayit basarisiz: ${r.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DietitianProgramsActivity, e.message ?: "Baglanti hatasi", Toast.LENGTH_LONG).show()
            }
        }
    }
}
