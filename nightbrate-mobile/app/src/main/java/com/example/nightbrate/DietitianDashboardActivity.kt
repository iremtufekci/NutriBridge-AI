package com.example.nightbrate

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class DietitianDashboardActivity : AppCompatActivity() {

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dietitian_dashboard)
        DietitianBottomBarHelper.bind(this, 0)

        findViewById<TextView>(R.id.btnDashViewAllClients).setOnClickListener {
            DietitianTabNav.go(this, 1)
        }
        findViewById<TextView>(R.id.btnDashAiReview).setOnClickListener {
            DietitianTabNav.go(this, 3)
        }

        findViewById<View>(R.id.cardDashTodayTasks).setOnClickListener {
            startActivity(Intent(this, DietitianDailyTasksActivity::class.java))
        }
        findViewById<TextView>(R.id.btnDashAllTasks).setOnClickListener {
            startActivity(Intent(this, DietitianDailyTasksActivity::class.java))
        }

        loadDashboard()
    }

    private fun loadDashboard() {
        val progress = findViewById<ProgressBar>(R.id.dashProgress)
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val profileReq = async { RetrofitClient.instance.getCurrentUserProfile() }
                val clientsReq = async { RetrofitClient.instance.getClientsWithLastMeal() }
                val tasksReq = async { RetrofitClient.instance.getTodayDailyTasks() }
                val critReq = async { RetrofitClient.instance.getDietitianCriticalAlerts() }
                val pr = profileReq.await()
                val cr = clientsReq.await()
                val tr = tasksReq.await()
                val critRes = critReq.await()

                val taskBundle = if (tr.isSuccessful) tr.body() else null
                val critCount =
                    if (critRes.isSuccessful) critRes.body()?.size ?: 0 else 0

                val p = pr.body()
                val displayName = when {
                    !p?.displayName.isNullOrBlank() -> p!!.displayName!!.trim()
                    else -> listOf(p?.firstName, p?.lastName)
                        .mapNotNull { it?.trim() }
                        .filter { it.isNotEmpty() }
                        .joinToString(" ")
                        .ifBlank { "Diyetisyen" }
                }
                findViewById<TextView>(R.id.tvDashGreeting).text = "Merhaba, $displayName 👋"

                val clients = if (cr.isSuccessful) cr.body().orEmpty() else emptyList()
                val criticalRows = clients.take(3).map { c ->
                    val name = listOf(c.firstName, c.lastName).mapNotNull { it?.trim() }.filter { it.isNotEmpty() }
                        .joinToString(" ").ifBlank { "Danışan" }
                    val initial = (c.firstName?.trim()?.firstOrNull() ?: 'D').uppercaseChar().toString()
                    val lastSeen = if (c.lastMeal?.timestamp != null) {
                        "Yeni kayıt var"
                    } else {
                        "Henüz öğün yok"
                    }
                    CriticalRow(name, initial, lastSeen, "Son öğün kontrolü gerekiyor")
                }

                val nCrit = criticalRows.size
                val subtitle = "Bugün $nCrit danışanınızın kritik durumu var. Güncel aktivitelere göz atın."
                val sp = SpannableString(subtitle)
                val key = "$nCrit danışanınızın"
                val startN = subtitle.indexOf(key)
                if (startN >= 0) {
                    sp.setSpan(StyleSpan(Typeface.BOLD), startN, startN + key.length, 0)
                }
                findViewById<TextView>(R.id.tvDashSubtitle).text = sp

                findViewById<TextView>(R.id.tvStatTotalClients).text = clients.size.toString()
                findViewById<TextView>(R.id.tvStatActivePrograms).text = clients.size.toString()
                findViewById<TextView>(R.id.tvStatTodayTasks).text =
                    (taskBundle?.totalCount ?: 0).toString()
                findViewById<TextView>(R.id.tvStatCriticalAlerts).text = critCount.toString()

                bindTodayTasksPreview(taskBundle?.tasks.orEmpty())

                findViewById<TextView>(R.id.tvCriticalBadge).text = "$nCrit Danışan"
                bindCriticalSection(criticalRows)

                val photos = clients
                    .filter { !it.lastMeal?.photoUrl.isNullOrBlank() }
                    .take(3)
                    .map { c ->
                        val label = if (c.lastMeal?.timestamp != null) "Son kayıt" else "Kayıt yok"
                        c.lastMeal!!.photoUrl!!.trim() to label
                    }
                bindPhotoSection(photos)
            } catch (_: Exception) {
                findViewById<TextView>(R.id.tvDashSubtitle).text =
                    "Veriler yüklenemedi. Bağlantınızı kontrol edip tekrar deneyin."
                bindTodayTasksPreview(emptyList())
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    private data class CriticalRow(
        val name: String,
        val initial: String,
        val lastSeen: String,
        val reason: String
    )

    private fun bindCriticalSection(rows: List<CriticalRow>) {
        val container = findViewById<LinearLayout>(R.id.containerCriticalClients)
        container.removeAllViews()
        val strong = ContextCompat.getColor(this, R.color.admin_strong)
        val muted = ContextCompat.getColor(this, R.color.admin_muted)
        val roseText = android.graphics.Color.parseColor("#F87171")
        if (rows.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "Henüz bağlı danışan yok."
                setTextColor(muted)
                textSize = 14f
                setPadding(0, dp(8), 0, dp(8))
            })
            return
        }
        for (row in rows) {
            val outer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(14), dp(14), dp(14))
                setBackgroundResource(R.drawable.diet_critical_row_bg)
            }
            val stripe = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(4), ViewGroup.LayoutParams.MATCH_PARENT).apply {
                    marginEnd = dp(12)
                }
                setBackgroundColor(roseText)
            }
            val avatar = TextView(this).apply {
                text = row.initial
                textSize = 14f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(roseText)
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(36), dp(36))
                setBackgroundResource(R.drawable.diet_critical_avatar_bg)
            }
            val textCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }
            textCol.addView(TextView(this).apply {
                text = row.name
                setTextColor(strong)
                textSize = 17f
                setTypeface(typeface, Typeface.BOLD)
            })
            textCol.addView(TextView(this).apply {
                text = row.lastSeen
                setTextColor(muted)
                textSize = 13f
                setPadding(0, dp(4), 0, 0)
            })
            val chip = TextView(this).apply {
                text = row.reason
                setTextColor(roseText)
                textSize = 11f
                setTypeface(typeface, Typeface.BOLD)
                setPadding(dp(10), dp(6), dp(10), dp(6))
                setBackgroundResource(R.drawable.diet_badge_rose)
            }
            textCol.addView(
                chip,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8) }
            )
            val chevron = TextView(this).apply {
                text = "›"
                setTextColor(muted)
                textSize = 22f
                includeFontPadding = false
            }
            val inner = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.TOP
                addView(avatar)
                addView(
                    textCol,
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginStart = dp(10)
                    }
                )
                addView(chevron)
            }
            outer.addView(stripe)
            outer.addView(
                inner,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            )
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = dp(10)
            container.addView(outer, lp)
        }
    }

    private fun bindPhotoSection(photos: List<Pair<String, String>>) {
        val container = findViewById<LinearLayout>(R.id.containerMealPhotos)
        container.removeAllViews()
        val muted = ContextCompat.getColor(this, R.color.admin_muted)
        if (photos.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "Henüz fotoğraflı öğün kaydı yok."
                setTextColor(muted)
                textSize = 14f
            })
            return
        }
        var i = 0
        while (i < photos.size) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(8) }
            }
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(6)
            }
            row.addView(makePhotoCell(photos[i].first, photos[i].second), lp)
            if (i + 1 < photos.size) {
                row.addView(makePhotoCell(photos[i + 1].first, photos[i + 1].second), lp)
                i += 2
            } else {
                i += 1
            }
            container.addView(row)
        }
    }

    private fun makePhotoCell(url: String, caption: String): LinearLayout {
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val iv = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(160)
            )
        }
        iv.load(url) {
            crossfade(true)
            placeholder(R.drawable.diet_stat_icon_bg)
            error(R.drawable.diet_stat_icon_bg)
        }
        col.addView(iv)
        col.addView(TextView(this).apply {
            text = caption
            setTextColor(ContextCompat.getColor(this@DietitianDashboardActivity, R.color.admin_muted))
            textSize = 13f
            setPadding(0, dp(6), 0, 0)
        })
        return col
    }

    private fun bindTodayTasksPreview(tasks: List<DietitianDailyTaskItemDto>) {
        val container = findViewById<LinearLayout>(R.id.containerTodayTasks)
        container.removeAllViews()
        val strong = ContextCompat.getColor(this, R.color.admin_strong)
        val muted = ContextCompat.getColor(this, R.color.admin_muted)
        val preview = tasks.filter { !it.isCompleted }.take(4)
        if (preview.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "Bekleyen günlük görev yok."
                setTextColor(muted)
                textSize = 14f
                setPadding(0, dp(6), 0, dp(6))
            })
            return
        }
        for (t in preview) {
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
                        val resp = RetrofitClient.instance.setDailyTaskComplete(
                            tid,
                            SetDietitianTaskCompleteBody(isCompleted = newVal)
                        )
                        if (!resp.isSuccessful) throw IllegalStateException()
                        loadDashboard()
                    } catch (_: Exception) {
                        check.isChecked = !newVal
                    } finally {
                        check.isEnabled = true
                    }
                }
            }
            val textCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
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
