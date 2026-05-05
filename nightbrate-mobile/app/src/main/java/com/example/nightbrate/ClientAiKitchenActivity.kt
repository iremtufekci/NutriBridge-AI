package com.example.nightbrate

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Response

class ClientAiKitchenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_ai_kitchen)
        ClientBottomBarHelper.bind(this, 5)

        val etIngredients = findViewById<EditText>(R.id.etAiKitchenIngredients)
        val etKcal = findViewById<EditText>(R.id.etAiKitchenKcal)
        val rg = findViewById<RadioGroup>(R.id.rgAiKitchenPreference)
        val btn = findViewById<MaterialButton>(R.id.btnAiKitchenGenerate)
        val progress = findViewById<ProgressBar>(R.id.progressAiKitchen)
        val tvError = findViewById<TextView>(R.id.tvAiKitchenError)
        val tvSource = findViewById<TextView>(R.id.tvAiKitchenSource)
        val llResults = findViewById<LinearLayout>(R.id.llAiKitchenResults)

        btn.setOnClickListener {
            tvError.visibility = View.GONE
            llResults.removeAllViews()
            tvSource.visibility = View.GONE

            val ing = etIngredients.text?.toString()?.trim().orEmpty()
            val kcalStr = etKcal.text?.toString()?.trim().orEmpty()
            val kcal = kcalStr.toIntOrNull()
            val rid = rg.checkedRadioButtonId
            val checked = if (rid != View.NO_ID) findViewById<View>(rid).tag?.toString() else null

            if (ing.isEmpty()) {
                tvError.text = "Malzemeler zorunlu."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (checked.isNullOrBlank()) {
                tvError.text = "Bir tercih seçin."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (kcal == null || kcal < 200 || kcal > 5000) {
                tvError.text = "Hedef kalori 200–5000 arası olmalı."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            progress.visibility = View.VISIBLE
            btn.isEnabled = false

            lifecycleScope.launch {
                val resp = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.generateKitchenRecipes(
                        KitchenChefGenerateRequest(
                            ingredients = ing,
                            preference = checked,
                            targetCalories = kcal
                        )
                    )
                }
                progress.visibility = View.GONE
                btn.isEnabled = true

                if (resp.isSuccessful) {
                    val data = resp.body()
                    if (data != null) {
                        when (data.source?.lowercase()) {
                            "gemini" -> {
                                tvSource.text = "Kaynak: yapay zeka hizmeti"
                                tvSource.setTextColor(0xFF1D4ED8.toInt())
                                tvSource.visibility = View.VISIBLE
                            }
                            "mock" -> {
                                tvSource.text = "Kaynak: yerel önizleme (sunucuda yapay zeka anahtarı yoksa)"
                                tvSource.setTextColor(0xFFB45309.toInt())
                                tvSource.visibility = View.VISIBLE
                            }
                        }
                        for (r in data.recipes) {
                            val card = layoutInflater.inflate(R.layout.item_ai_kitchen_recipe, llResults, false)
                            card.findViewById<TextView>(R.id.tvRecipeTitle).text = r.title
                            card.findViewById<TextView>(R.id.tvRecipeMeta).text =
                                buildString {
                                    append("${r.estimatedCalories} kkal")
                                    r.prepTimeMinutes?.let { if (it > 0) append("  ·  ~${it} dk") }
                                }
                            r.description?.takeIf { it.isNotBlank() }?.let {
                                card.findViewById<TextView>(R.id.tvRecipeDesc).apply {
                                    text = it
                                    visibility = View.VISIBLE
                                }
                            }
                            val ingText = r.ingredients.joinToString("\n") { "• $it" }
                            card.findViewById<TextView>(R.id.tvRecipeIngredients).text = ingText
                            val stepsText = r.steps.mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n")
                            card.findViewById<TextView>(R.id.tvRecipeSteps).text = stepsText
                            llResults.addView(card)
                        }
                    }
                } else {
                    tvError.text = readErrorMessage(resp)
                    tvError.visibility = View.VISIBLE
                }
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
