package com.example.nightbrate

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

object AdminBottomBarHelper {
    private val tabIds = listOf(
        R.id.atab0, R.id.atab1, R.id.atab2, R.id.atab3, R.id.atab4
    )
    private val iconIds = listOf(
        R.id.aicon0, R.id.aicon1, R.id.aicon2, R.id.aicon3, R.id.aicon4
    )
    private val labelIds = listOf(
        R.id.alabel0, R.id.alabel1, R.id.alabel2, R.id.alabel3, R.id.alabel4
    )

    fun styleTabs(activity: AppCompatActivity, selectedIndex: Int) {
        val active = ContextCompat.getColor(activity, R.color.nav_item_active)
        val inactive = ContextCompat.getColor(activity, R.color.nav_item_inactive)
        iconIds.forEachIndexed { i, id ->
            activity.findViewById<android.widget.TextView>(id)
                .setTextColor(if (i == selectedIndex) active else inactive)
        }
        labelIds.forEachIndexed { i, id ->
            activity.findViewById<android.widget.TextView>(id)
                .setTextColor(if (i == selectedIndex) active else inactive)
        }
    }

    fun bind(activity: AppCompatActivity, selectedIndex: Int) {
        styleTabs(activity, selectedIndex)
        tabIds.forEachIndexed { index, viewId ->
            activity.findViewById<android.widget.LinearLayout>(viewId)
                .setOnClickListener { AdminTabNav.go(activity, index) }
        }
    }
}

object AdminTabNav {
    fun go(activity: AppCompatActivity, index: Int) {
        if (index == 0) {
            if (activity is AdminDashboardActivity) return
            activity.startActivity(
                Intent(activity, AdminDashboardActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            return
        }
        if (activity is AdminPlaceholderActivity) {
            val t = activity.intent.getIntExtra(AdminPlaceholderActivity.EXTRA_INDEX, -1)
            if (t == index) return
        }
        val (title, body) = when (index) {
            1 -> "Kullanıcı yönetimi" to "Web’deki kullanıcı listesi ve işlemler burada sunulacak."
            2 -> "Diyetisyen onayları" to "Bekleyen onaylar ve inceleme (web AdminApprovals) buraya bağlanacak."
            3 -> "Sistem analitiği" to "Grafikler ve raporlar eklenecek."
            4 -> "Ayarlar" to "Sistem ve hesap ayarları."
            else -> return
        }
        activity.startActivity(
            Intent(activity, AdminPlaceholderActivity::class.java)
                .putExtra(AdminPlaceholderActivity.EXTRA_INDEX, index)
                .putExtra(AdminPlaceholderActivity.EXTRA_TITLE, title)
                .putExtra(AdminPlaceholderActivity.EXTRA_MESSAGE, body)
        )
        if (activity is AdminPlaceholderActivity) activity.finish()
    }
}
