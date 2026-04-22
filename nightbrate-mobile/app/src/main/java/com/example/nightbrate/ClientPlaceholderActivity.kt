package com.example.nightbrate

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ClientPlaceholderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_placeholder)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val message = intent.getStringExtra(EXTRA_MESSAGE).orEmpty()
        val index = intent.getIntExtra(EXTRA_INDEX, 1)
        findViewById<TextView>(R.id.placeholderTitle).text = title
        findViewById<TextView>(R.id.placeholderMessage).text = message
        ClientBottomBarHelper.bind(this, index)
    }

    companion object {
        const val EXTRA_INDEX = "extra_index"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
    }
}
