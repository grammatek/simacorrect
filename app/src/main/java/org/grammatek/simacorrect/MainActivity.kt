package org.grammatek.simacorrect

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var etText: EditText
    private lateinit var btnSettings: TextView
    private lateinit var btnFeedback: TextView
    private lateinit var btnAbout: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etText = findViewById(R.id.textInputEditText)
        btnFeedback = findViewById(R.id.feedback)
        btnSettings = findViewById(R.id.settings)
        btnAbout = findViewById(R.id.about)

        btnFeedback.setOnClickListener {
            val intent = Intent(this, EmailFeedback::class.java)
            startActivity(intent)
        }
        btnSettings.setOnClickListener {
            val intent = Intent(Settings.ACTION_SETTINGS)
            intent.setClassName("com.android.settings", "com.android.settings.LanguageSettings")
            startActivity(intent)
        }
        btnAbout.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }
    }
}