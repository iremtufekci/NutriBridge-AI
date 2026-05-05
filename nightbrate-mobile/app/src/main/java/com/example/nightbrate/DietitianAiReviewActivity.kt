package com.example.nightbrate

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class DietitianAiReviewActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var listClients: ListView
    private lateinit var progressList: ProgressBar
    private lateinit var cardSelected: MaterialCardView
    private lateinit var tvSelectedName: TextView
    private lateinit var progressKitchen: ProgressBar
    private lateinit var llLogs: LinearLayout
    private lateinit var tvLogsEmpty: TextView
    private lateinit var layoutKitchenSection: LinearLayout

    private var allClients: List<ClientWithLastMealItem> = emptyList()
    private var filteredClients: List<ClientWithLastMealItem> = emptyList()
    private var selectedClientId: String? = null
    private lateinit var adapter: ClientListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dietitian_ai_review)
        DietitianBottomBarHelper.bind(this, 3)

        etSearch = findViewById(R.id.etAiReviewSearch)
        listClients = findViewById(R.id.listAiReviewClients)
        progressList = findViewById(R.id.progressAiReviewClients)
        cardSelected = findViewById(R.id.cardAiReviewSelected)
        tvSelectedName = findViewById(R.id.tvAiReviewSelectedName)
        progressKitchen = findViewById(R.id.progressAiReviewKitchen)
        llLogs = findViewById(R.id.llAiReviewKitchenLogs)
        tvLogsEmpty = findViewById(R.id.tvAiReviewLogsEmpty)
        layoutKitchenSection = findViewById(R.id.layoutAiReviewKitchenSection)

        adapter = ClientListAdapter(this, emptyList(), null) { id ->
            selectedClientId = id
            adapter.setSelected(id)
            adapter.notifyDataSetChanged()
            updateSelectedBanner()
            layoutKitchenSection.visibility = View.VISIBLE
            loadKitchenLogs(id)
        }
        listClients.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterClients(s?.toString().orEmpty())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        loadClients()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun filterClients(q: String) {
        val t = q.trim().lowercase(Locale.getDefault())
        filteredClients = if (t.isEmpty()) allClients else allClients.filter { c ->
            val n =
                "${c.firstName.orEmpty()} ${c.lastName.orEmpty()}".trim().lowercase(Locale.getDefault())
            n.contains(t)
        }
        adapter.updateItems(filteredClients)
        adapter.notifyDataSetChanged()
    }

    private fun loadClients() {
        progressList.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.getClientsWithLastMeal()
                val list = if (r.isSuccessful) r.body().orEmpty() else emptyList()
                allClients = list
                filterClients(etSearch.text?.toString().orEmpty())
            } catch (e: Exception) {
                Toast.makeText(this@DietitianAiReviewActivity, e.message ?: "Liste alınamadı", Toast.LENGTH_LONG)
                    .show()
                allClients = emptyList()
                filterClients("")
            } finally {
                progressList.visibility = View.GONE
            }
        }
    }

    private fun updateSelectedBanner() {
        val id = selectedClientId
        if (id.isNullOrBlank()) {
            cardSelected.visibility = View.GONE
            return
        }
        val c = allClients.find { it.id == id }
        val name =
            listOf(c?.firstName, c?.lastName).mapNotNull { it?.trim() }.filter { it.isNotEmpty() }
                .joinToString(" ").ifBlank { "Danışan" }
        tvSelectedName.text = name
        cardSelected.visibility = View.VISIBLE
    }

    private fun loadKitchenLogs(clientId: String) {
        llLogs.removeAllViews()
        tvLogsEmpty.visibility = View.GONE
        progressKitchen.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.getClientKitchenRecipeLogs(clientId, 30)
                val logs = if (r.isSuccessful) r.body().orEmpty() else emptyList()
                progressKitchen.visibility = View.GONE
                if (logs.isEmpty()) {
                    tvLogsEmpty.visibility = View.VISIBLE
                    return@launch
                }
                for (log in logs) {
                    llLogs.addView(buildLogCard(log))
                }
            } catch (_: Exception) {
                progressKitchen.visibility = View.GONE
                tvLogsEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun buildLogCard(log: KitchenChefShareLogItem): View {
        val pad = dp(12)
        val card = MaterialCardView(this).apply {
            radius = dp(12).toFloat()
            cardElevation = 0f
            strokeWidth = dp(1)
            strokeColor = ContextCompat.getColor(this@DietitianAiReviewActivity, R.color.admin_row_stroke)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        val meta = TextView(this).apply {
            textSize = 11f
            setTextColor(ContextCompat.getColor(this@DietitianAiReviewActivity, R.color.admin_muted))
            val whenStr = formatLogWhen(log.createdAtUtc)
            val src = log.source?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""
            text = "Hedef ${log.targetCalories} kkal · $whenStr$src"
        }
        col.addView(meta)
        col.addView(TextView(this).apply {
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@DietitianAiReviewActivity, R.color.admin_strong))
            text = "Tercih: ${log.preference}"
            setPadding(0, dp(6), 0, 0)
        })
        col.addView(TextView(this).apply {
            textSize = 11f
            setTextColor(ContextCompat.getColor(this@DietitianAiReviewActivity, R.color.admin_muted))
            text = "Malzemeler (sorgu): ${log.ingredients}"
        })
        for (r in log.selectedRecipes) {
            col.addView(buildRecipeBlock(r))
        }
        card.addView(col)
        return card
    }

    private fun buildRecipeBlock(r: KitchenChefRecipeItem): View {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, 0)
        }
        wrap.addView(TextView(this).apply {
            text = r.title
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@DietitianAiReviewActivity, R.color.admin_strong))
        })
        if (!r.description.isNullOrBlank()) {
            wrap.addView(TextView(this).apply {
                text = r.description
                textSize = 13f
                setTextColor(ContextCompat.getColor(this@DietitianAiReviewActivity, R.color.admin_muted))
            })
        }
        wrap.addView(TextView(this).apply {
            val prep = r.prepTimeMinutes?.let { " · $it dk" } ?: ""
            text = "~${r.estimatedCalories} kkal$prep"
            textSize = 11f
        })
        for (ing in r.ingredients) {
            wrap.addView(TextView(this).apply {
                text = "• $ing"
                textSize = 11f
            })
        }
        var step = 1
        for (s in r.steps) {
            wrap.addView(TextView(this).apply {
                text = "$step. $s"
                textSize = 11f
            })
            step++
        }
        return wrap
    }

    private fun formatLogWhen(iso: String): String {
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (p in patterns) {
            try {
                val sdf = SimpleDateFormat(p, Locale.ROOT)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val d = sdf.parse(iso) ?: continue
                val out = SimpleDateFormat("d MMM yyyy, HH:mm", Locale("tr", "TR"))
                out.timeZone = TimeZone.getDefault()
                return out.format(d)
            } catch (_: Exception) {
            }
        }
        return iso
    }

    private class ClientListAdapter(
        private val context: Context,
        private var items: List<ClientWithLastMealItem>,
        private var selectedId: String?,
        private val onSelect: (String) -> Unit
    ) : BaseAdapter() {
        fun updateItems(newItems: List<ClientWithLastMealItem>) {
            items = newItems
        }

        fun setSelected(id: String?) {
            selectedId = id
        }

        override fun getCount() = items.size
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val item = items[position]
            val tv = (convertView as? TextView) ?: TextView(context).apply {
                setPadding(36, 28, 36, 28)
                textSize = 14f
            }
            val label =
                listOf(item.firstName, item.lastName).mapNotNull { it?.trim() }.filter { it.isNotEmpty() }
                    .joinToString(" ").ifBlank { "İsimsiz" }
            tv.text = label
            val cid = item.id
            val sel = cid != null && cid == selectedId
            tv.setBackgroundColor(
                if (sel)
                    ContextCompat.getColor(context, R.color.ai_review_row_selected)
                else
                    android.graphics.Color.TRANSPARENT
            )
            tv.setOnClickListener {
                if (cid != null) onSelect(cid)
            }
            return tv
        }
    }
}
