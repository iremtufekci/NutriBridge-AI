package com.example.nightbrate

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DietitianPlaceholderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dietitian_placeholder)
        val index = intent.getIntExtra(EXTRA_INDEX, 1)
        findViewById<TextView>(R.id.dPlaceholderTitle).text =
            intent.getStringExtra(EXTRA_TITLE).orEmpty()
        findViewById<TextView>(R.id.dPlaceholderMessage).text =
            intent.getStringExtra(EXTRA_MESSAGE).orEmpty()
        DietitianBottomBarHelper.bind(this, index)
    }

    companion object {
        const val EXTRA_INDEX = "extra_d_index"
        const val EXTRA_TITLE = "extra_d_title"
        const val EXTRA_MESSAGE = "extra_d_message"
    }
}
