package com.example.nightbrate

import android.graphics.drawable.ColorDrawable
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat

object ActivityWindowHelper {
    /**
     * Hedef 35 / edge-to-edge: içerik sistem çubuklarının altında “kaybolup” siyahmuş gibi görünmesini önler.
     */
    fun AppCompatActivity.applyStandardContentWindow(@ColorRes backgroundColor: Int = R.color.client_scaffold) {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        @Suppress("DEPRECATION")
        window.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this, backgroundColor)))
    }
}
