package com.example.nightbrate

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class DietitianDailyTasksActivity : AppCompatActivity() {

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dietitian_daily_tasks)
        DietitianBottomBarHelper.bind(this, 0)

        findViewById<TextView>(R.id.btnDailyTasksBack).setOnClickListener { finish() }

        loadAll()
    }

    private fun loadAll() {
        val progress = findViewById<ProgressBar>(R.id.progressDailyTasks)
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val r = RetrofitClient.instance.getTodayDailyTasks()
                val body = if (r.isSuccessful) r.body() else null
                bindSummary(body)
                bindTaskList(body?.tasks.orEmpty())
            } catch (_: Exception) {
                findViewById<TextView>(R.id.tvDailyTasksSummary).text = "Görevler yüklenemedi."
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    private fun bindSummary(bundle: DietitianTodayTasksBundleDto?) {
        val tv = findViewById<TextView>(R.id.tvDailyTasksSummary)
        if (bundle == null) {
            tv.text = ""
            return
        }
        tv.text =
            "Tarih: ${bundle.taskDate ?: "—"} · Bekleyen ${bundle.pendingCount} · Tamamlanan ${bundle.completedCount} · Toplam ${bundle.totalCount}"
    }

    private fun bindTaskList(tasks: List<DietitianDailyTaskItemDto>) {
        val container = findViewById<LinearLayout>(R.id.containerAllTasks)
        container.removeAllViews()
        val strong = ContextCompat.getColor(this, R.color.admin_strong)
        val muted = ContextCompat.getColor(this, R.color.admin_muted)
        if (tasks.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "Bugün için görev yok."
                setTextColor(muted)
                textSize = 15f
            })
            return
        }
        for (t in tasks) {
            val id = t.id ?: continue
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(14), dp(14), dp(14))
                setBackgroundResource(R.drawable.diet_task_row_bg)
            }
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = dp(10)
            val check = CheckBox(this).apply {
                tag = id
                isChecked = t.isCompleted
            }
            check.setOnClickListener {
                val tid = check.tag as? String ?: return@setOnClickListener
                val newVal = check.isChecked
                lifecycleScope.launch {
                    check.isEnabled = false
                    try {
                        val resp =
                            RetrofitClient.instance.setDailyTaskComplete(
                                tid,
                                SetDietitianTaskCompleteBody(isCompleted = newVal)
                            )
                        if (!resp.isSuccessful) throw IllegalStateException()
                        loadAll()
                    } catch (_: Exception) {
                        check.isChecked = !newVal
                    } finally {
                        check.isEnabled = true
                    }
                }
            }
            val textCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }
            textCol.addView(TextView(this).apply {
                text = t.title ?: ""
                setTextColor(strong)
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
            })
            textCol.addView(TextView(this).apply {
                text = t.subtitle ?: ""
                setTextColor(muted)
                textSize = 14f
                setPadding(0, dp(4), 0, 0)
            })
            val due = TextView(this).apply {
                text = t.dueLabel ?: "Bugün"
                setTextColor(android.graphics.Color.parseColor("#F59E0B"))
                textSize = 13f
                setTypeface(typeface, Typeface.BOLD)
            }
            row.addView(check)
            row.addView(
                textCol,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(8) }
            )
            row.addView(due)
            container.addView(row, lp)
        }
    }
}
