package com.example.nightbrate

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

object ClientBottomBarHelper {

    fun styleTabs(activity: AppCompatActivity, selectedIndex: Int) {
        val active = ContextCompat.getColor(activity, R.color.nav_item_active)
        val inactive = ContextCompat.getColor(activity, R.color.nav_item_inactive)
        val icons = listOf(
            R.id.iconHome, R.id.iconJournal, R.id.iconPast, R.id.iconFood, R.id.iconChef, R.id.iconProfile
        )
        val labels = listOf(
            R.id.labelHome, R.id.labelJournal, R.id.labelPast, R.id.labelFood, R.id.labelChef, R.id.labelProfile
        )
        icons.forEachIndexed { i, id ->
            activity.findViewById<android.widget.TextView>(id)
                .setTextColor(if (i == selectedIndex) active else inactive)
        }
        labels.forEachIndexed { i, id ->
            activity.findViewById<android.widget.TextView>(id)
                .setTextColor(if (i == selectedIndex) active else inactive)
        }
    }

    fun bind(activity: AppCompatActivity, selectedIndex: Int) {
        styleTabs(activity, selectedIndex)
        listOf(
            R.id.tabHome, R.id.tabJournal, R.id.tabPast, R.id.tabFood, R.id.tabChef, R.id.tabProfile
        ).forEachIndexed { index, viewId ->
            activity.findViewById<android.widget.LinearLayout>(viewId)
                .setOnClickListener { ClientTabNav.go(activity, index) }
        }
    }
}

object ClientTabNav {
    private const val TAB_HOME = 0
    private const val TAB_JOURNAL = 1
    private const val TAB_PAST = 2
    private const val TAB_FOOD = 3
    private const val TAB_CHEF = 4
    private const val TAB_PROFILE = 5

    fun go(activity: AppCompatActivity, index: Int) {
        if (index == TAB_HOME) {
            if (activity is ClientDashboardActivity) return
            activity.startActivity(
                Intent(activity, ClientDashboardActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            if (activity is ClientDietProgramHistoryActivity) activity.finish()
            return
        }
        if (index == TAB_PROFILE) {
            if (activity is ClientProfileActivity) return
            activity.startActivity(Intent(activity, ClientProfileActivity::class.java))
            if (activity !is ClientDashboardActivity) activity.finish()
            return
        }
        if (index == TAB_JOURNAL) {
            if (activity is ClientDietProgramActivity) return
            activity.startActivity(
                Intent(activity, ClientDietProgramActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            if (activity is ClientDietProgramHistoryActivity) activity.finish()
            if (activity is ClientPlaceholderActivity) activity.finish()
            if (activity is ClientProfileActivity) activity.finish()
            if (activity !is ClientDashboardActivity) activity.finish()
            return
        }
        if (index == TAB_PAST) {
            if (activity is ClientDietProgramHistoryActivity) return
            activity.startActivity(
                Intent(activity, ClientDietProgramHistoryActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            if (activity is ClientDietProgramActivity) activity.finish()
            if (activity is ClientPlaceholderActivity) activity.finish()
            if (activity is ClientProfileActivity) activity.finish()
            if (activity !is ClientDashboardActivity) activity.finish()
            return
        }
        if (index in setOf(TAB_FOOD, TAB_CHEF)) {
            if (activity is ClientPlaceholderActivity) {
                val t = activity.intent.getIntExtra(ClientPlaceholderActivity.EXTRA_INDEX, -1)
                if (t == index) return
            }
            val (title, body) = when (index) {
                TAB_FOOD -> "Yemek Analizi" to
                    "Fotoğrafla yemek analizi web ile aynı API'lerden burada kullanılacak."
                TAB_CHEF -> "AI Mutfak Şefi" to
                    "Web'deki AI Mutfak akışı buraya eklenecek."
                else -> return
            }
            activity.startActivity(
                Intent(activity, ClientPlaceholderActivity::class.java)
                    .putExtra(ClientPlaceholderActivity.EXTRA_INDEX, index)
                    .putExtra(ClientPlaceholderActivity.EXTRA_TITLE, title)
                    .putExtra(ClientPlaceholderActivity.EXTRA_MESSAGE, body)
            )
            if (activity is ClientDietProgramActivity) activity.finish()
            if (activity is ClientDietProgramHistoryActivity) activity.finish()
            if (activity is ClientProfileActivity) activity.finish()
            if (activity is ClientPlaceholderActivity) activity.finish()
        }
    }
}
