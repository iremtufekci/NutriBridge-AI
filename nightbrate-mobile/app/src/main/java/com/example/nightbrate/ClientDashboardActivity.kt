package com.example.nightbrate

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ClientDashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_dashboard)

        val username = intent.getStringExtra("USERNAME") ?: "Ayşe"
        findViewById<TextView>(R.id.tvWelcome).text = "Günaydın, $username ☀️"
        ClientBottomBarHelper.bind(this, 0)
    }
}
