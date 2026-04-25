package com.example.nightbrate

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

object DietitianBottomBarHelper {
    private val tabIds = listOf(
        R.id.dtab0, R.id.dtab1, R.id.dtab2, R.id.dtab3, R.id.dtab4, R.id.dtab5
    )
    private val iconIds = listOf(
        R.id.dicon0, R.id.dicon1, R.id.dicon2, R.id.dicon3, R.id.dicon4, R.id.dicon5
    )
    private val labelIds = listOf(
        R.id.dlabel0, R.id.dlabel1, R.id.dlabel2, R.id.dlabel3, R.id.dlabel4, R.id.dlabel5
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
                .setOnClickListener { DietitianTabNav.go(activity, index) }
        }
    }
}

object DietitianTabNav {
    fun go(activity: AppCompatActivity, index: Int) {
        if (index == 0) {
            if (activity is DietitianDashboardActivity) return
            activity.startActivity(
                Intent(activity, DietitianDashboardActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            return
        }
        if (index == 5) {
            if (activity is DietitianProfileActivity) return
            activity.startActivity(Intent(activity, DietitianProfileActivity::class.java))
            if (activity is DietitianPlaceholderActivity) activity.finish()
            if (activity !is DietitianDashboardActivity) activity.finish()
            return
        }
        if (activity is DietitianPlaceholderActivity) {
            val t = activity.intent.getIntExtra(DietitianPlaceholderActivity.EXTRA_INDEX, -1)
            if (t == index) return
        }
        val (title, body) = when (index) {
            1 -> "Danışanlarım" to "Web ile aynı danışan listesi ve detaylar bu ekrana bağlanacak."
            2 -> "Diyet Programları" to "Program oluşturma ve atama (web) burada açılacak."
            3 -> "AI Denetimi" to "Web’deki AI denetim ekranı buraya taşınacak."
            4 -> "Kritik Uyarılar" to "Uyarı listesi ve bildirimler eklenecek."
            else -> return
        }
        activity.startActivity(
            Intent(activity, DietitianPlaceholderActivity::class.java)
                .putExtra(DietitianPlaceholderActivity.EXTRA_INDEX, index)
                .putExtra(DietitianPlaceholderActivity.EXTRA_TITLE, title)
                .putExtra(DietitianPlaceholderActivity.EXTRA_MESSAGE, body)
        )
        if (activity is DietitianPlaceholderActivity) activity.finish()
        if (activity is DietitianProfileActivity) activity.finish()
    }
}
