package com.example.nightbrate

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AdminPlaceholderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_placeholder)
        val index = intent.getIntExtra(EXTRA_INDEX, 1)
        findViewById<TextView>(R.id.aPlaceholderTitle).text =
            intent.getStringExtra(EXTRA_TITLE).orEmpty()
        findViewById<TextView>(R.id.aPlaceholderMessage).text =
            intent.getStringExtra(EXTRA_MESSAGE).orEmpty()
        AdminBottomBarHelper.bind(this, index)
    }

    companion object {
        const val EXTRA_INDEX = "extra_a_index"
        const val EXTRA_TITLE = "extra_a_title"
        const val EXTRA_MESSAGE = "extra_a_message"
    }
}
