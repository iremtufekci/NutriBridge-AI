package com.example.nightbrate

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

object ClientBottomBarHelper {

    fun styleTabs(activity: AppCompatActivity, selectedIndex: Int) {
        val active = ContextCompat.getColor(activity, R.color.nav_item_active)
        val inactive = ContextCompat.getColor(activity, R.color.nav_item_inactive)
        val icons = listOf(
            R.id.iconHome, R.id.iconJournal, R.id.iconPast, R.id.iconFood,
            R.id.iconPdf, R.id.iconChef, R.id.iconShares, R.id.iconProfile
        )
        val labels = listOf(
            R.id.labelHome, R.id.labelJournal, R.id.labelPast, R.id.labelFood,
            R.id.labelPdf, R.id.labelChef, R.id.labelShares, R.id.labelProfile
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
            R.id.tabHome, R.id.tabJournal, R.id.tabPast, R.id.tabFood,
            R.id.tabPdf, R.id.tabChef, R.id.tabShares, R.id.tabProfile
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
    private const val TAB_PDF = 4
    private const val TAB_CHEF = 5
    private const val TAB_SHARES = 6
    private const val TAB_PROFILE = 7

    fun go(activity: AppCompatActivity, index: Int) {
        if (index == TAB_HOME) {
            if (activity is ClientDashboardActivity) return
            activity.startActivity(
                Intent(activity, ClientDashboardActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            if (activity is ClientDietProgramHistoryActivity) activity.finish()
            if (activity is ClientAiKitchenActivity || activity is ClientAiKitchenSharesActivity) activity.finish()
            if (activity is ClientMealAnalysisActivity) activity.finish()
            if (activity is ClientPdfAnalysisActivity) activity.finish()
            return
        }
        if (index == TAB_CHEF) {
            if (activity is ClientAiKitchenActivity) return
            if (activity is ClientAiKitchenSharesActivity) {
                activity.startActivity(
                    Intent(activity, ClientAiKitchenActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                )
                activity.finish()
                return
            }
            activity.startActivity(
                Intent(activity, ClientAiKitchenActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            if (activity is ClientDietProgramActivity) activity.finish()
            if (activity is ClientDietProgramHistoryActivity) activity.finish()
            if (activity is ClientProfileActivity) activity.finish()
            if (activity is ClientPlaceholderActivity) activity.finish()
            if (activity is ClientMealAnalysisActivity) activity.finish()
            if (activity is ClientPdfAnalysisActivity) activity.finish()
            return
        }
        if (index == TAB_SHARES) {
            if (activity is ClientAiKitchenSharesActivity) return
            activity.startActivity(
                Intent(activity, ClientAiKitchenSharesActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            if (activity is ClientDietProgramActivity) activity.finish()
            if (activity is ClientDietProgramHistoryActivity) activity.finish()
            if (activity is ClientProfileActivity) activity.finish()
            if (activity is ClientPlaceholderActivity) activity.finish()
            if (activity is ClientMealAnalysisActivity) activity.finish()
            if (activity is ClientAiKitchenActivity) activity.finish()
            if (activity is ClientPdfAnalysisActivity) activity.finish()
            return
        }
        if (index == TAB_PROFILE) {
            if (activity is ClientProfileActivity) return
            activity.startActivity(Intent(activity, ClientProfileActivity::class.java))
            if (activity is ClientAiKitchenActivity || activity is ClientAiKitchenSharesActivity) activity.finish()
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
            if (activity is ClientAiKitchenActivity || activity is ClientAiKitchenSharesActivity) activity.finish()
            if (activity is ClientProfileActivity) activity.finish()
            if (activity is ClientMealAnalysisActivity) activity.finish()
            if (activity is ClientPdfAnalysisActivity) activity.finish()
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
            if (activity is ClientAiKitchenActivity || activity is ClientAiKitchenSharesActivity) activity.finish()
            if (activity is ClientMealAnalysisActivity) activity.finish()
            if (activity is ClientPdfAnalysisActivity) activity.finish()
            if (activity !is ClientDashboardActivity) activity.finish()
            return
        }
        if (index == TAB_FOOD) {
            if (activity is ClientMealAnalysisActivity) return
            activity.startActivity(
                Intent(activity, ClientMealAnalysisActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            if (activity is ClientDietProgramActivity) activity.finish()
            if (activity is ClientDietProgramHistoryActivity) activity.finish()
            if (activity is ClientProfileActivity) activity.finish()
            if (activity is ClientPlaceholderActivity) activity.finish()
            if (activity is ClientAiKitchenActivity || activity is ClientAiKitchenSharesActivity) activity.finish()
            if (activity is ClientPdfAnalysisActivity) activity.finish()
            return
        }
        if (index == TAB_PDF) {
            if (activity is ClientPdfAnalysisActivity) return
            activity.startActivity(
                Intent(activity, ClientPdfAnalysisActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            if (activity is ClientDietProgramActivity) activity.finish()
            if (activity is ClientDietProgramHistoryActivity) activity.finish()
            if (activity is ClientProfileActivity) activity.finish()
            if (activity is ClientPlaceholderActivity) activity.finish()
            if (activity is ClientAiKitchenActivity || activity is ClientAiKitchenSharesActivity) activity.finish()
            if (activity is ClientMealAnalysisActivity) activity.finish()
            return
        }
    }
}
