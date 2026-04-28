package com.example.nightbrate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ClientAiKitchenSharesActivity : AppCompatActivity() {

    private var skip = 0
    private val pageSize = 50
    private var hasMore = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_ai_kitchen_shares)
        ClientBottomBarHelper.bind(this, 5)

        val etFrom = findViewById<EditText>(R.id.etShareFrom)
        val etTo = findViewById<EditText>(R.id.etShareTo)

        findViewById<MaterialButton>(R.id.btnShareLast7).setOnClickListener { applyRange(etFrom, etTo, 7) }
        findViewById<MaterialButton>(R.id.btnShareLast30).setOnClickListener { applyRange(etFrom, etTo, 30) }
        findViewById<MaterialButton>(R.id.btnShareClear).setOnClickListener {
            etFrom.setText("")
            etTo.setText("")
        }
        findViewById<MaterialButton>(R.id.btnShareLoad).setOnClickListener { load(etFrom, etTo, reset = true) }
        findViewById<MaterialButton>(R.id.btnShareMore).setOnClickListener { load(etFrom, etTo, reset = false) }

        load(etFrom, etTo, reset = true)
    }

    private fun applyRange(etFrom: EditText, etTo: EditText, days: Int) {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val to = fmt.format(cal.time)
        cal.add(Calendar.DAY_OF_MONTH, -days)
        val from = fmt.format(cal.time)
        etFrom.setText(from)
        etTo.setText(to)
    }

    private fun load(etFrom: EditText, etTo: EditText, reset: Boolean) {
        if (reset) {
            skip = 0
            findViewById<LinearLayout>(R.id.llAiSharesList).removeAllViews()
        }
        val progress = findViewById<ProgressBar>(R.id.progressAiShares)
        val err = findViewById<TextView>(R.id.tvAiSharesError)
        val more = findViewById<MaterialButton>(R.id.btnShareMore)
        err.visibility = View.GONE
        progress.visibility = View.VISIBLE
        more.visibility = View.GONE

        val from = etFrom.text?.toString()?.trim().orEmpty().ifBlank { null }
        val to = etTo.text?.toString()?.trim().orEmpty().ifBlank { null }
        val nextSkip = if (reset) 0 else skip

        lifecycleScope.launch {
            val resp: Response<List<KitchenChefShareLogItem>> = withContext(Dispatchers.IO) {
                RetrofitClient.instance.getMyKitchenShares(
                    from = from,
                    to = to,
                    source = null,
                    skip = nextSkip,
                    take = pageSize
                )
            }
            progress.visibility = View.GONE
            val list = if (resp.isSuccessful) resp.body() ?: emptyList() else emptyList()
            if (!resp.isSuccessful) {
                err.text = readErrorMessage(resp)
                err.visibility = View.VISIBLE
            } else {
                val container = findViewById<LinearLayout>(R.id.llAiSharesList)
                val inflater = LayoutInflater.from(this@ClientAiKitchenSharesActivity)
                for (log in list) {
                    val card = inflater.inflate(R.layout.item_ai_kitchen_share_log, container, false)
                    val meta = card.findViewById<TextView>(R.id.tvShareItemMeta)
                    val title = card.findViewById<TextView>(R.id.tvShareItemTitle)
                    val body = card.findViewById<TextView>(R.id.tvShareItemQuery)
                    val r = log.selectedRecipes.firstOrNull()
                    val srcLabel = if (log.source == "gemini") "Gemini" else "Mock"
                    meta.text = buildString {
                        append(log.createdAtUtc.replace("T", " ").take(19))
                        append("  ·  ")
                        append(srcLabel)
                        append("  ·  Hedef: ")
                        append(log.targetCalories)
                        append(" kcal")
                    }
                    title.text = r?.title ?: "Tarif"
                    body.text = buildString {
                        append("Tercih: ${log.preference}\n")
                        append("Sorgu malzemeler: ${log.ingredients}\n\n")
                        r?.let { x ->
                            x.description?.takeIf { it.isNotBlank() }?.let { append("$it\n\n") }
                            append("~${x.estimatedCalories} kcal")
                            x.prepTimeMinutes?.takeIf { it > 0 }?.let { append("  ·  ~$it dk") }
                            append("\n\n")
                            if (x.ingredients.isNotEmpty()) {
                                append("Malzeme listesi:\n")
                                x.ingredients.forEach { append("• $it\n") }
                            }
                            if (x.steps.isNotEmpty()) {
                                append("\nAdımlar:\n")
                                x.steps.forEachIndexed { i, s -> append("${i + 1}. $s\n") }
                            }
                        }
                    }
                    container.addView(card)
                }
                hasMore = list.size == pageSize
                skip = nextSkip + list.size
                if (hasMore) more.visibility = View.VISIBLE
            }
        }
    }

    private fun readErrorMessage(response: Response<*>): String {
        val raw = response.errorBody()?.string().orEmpty()
        return try {
            JSONObject(raw).optString("message").ifBlank { "İstek başarısız (${response.code()})." }
        } catch (_: Exception) {
            raw.ifBlank { "İstek başarısız (${response.code()})." }
        }
    }
}
