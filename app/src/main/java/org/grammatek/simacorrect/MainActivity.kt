package org.grammatek.simacorrect

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import kotlinx.coroutines.*
import org.grammatek.apis.DevelopersApi
import org.grammatek.models.Annotations
import android.provider.Settings

class MainActivity : AppCompatActivity() {
    private lateinit var beforeText: TextView
    private lateinit var afterText: TextView
    private lateinit var etText: EditText
    private lateinit var btnSend: Button

    private lateinit var btnSettings: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etText = findViewById(R.id.textInputEditText)
        afterText = findViewById(R.id.text_after)
        beforeText = findViewById(R.id.text_before)
        btnSettings = findViewById(R.id.button2)
        btnSend = findViewById(R.id.button)

        btnSend.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                highlightCorrection()
            }
        }
        btnSettings.setOnClickListener {
            val intent = Intent(Settings.ACTION_SETTINGS)
            intent.setClassName("com.android.settings", "com.android.settings.LanguageSettings")
            startActivity(intent)
        }
    }

    private suspend fun highlightCorrection() {
        coroutineScope {
            val api = DevelopersApi()
            val response = api.correctApiPost(etText.text.toString())
            val corrected = response.result?.get(0)?.get(0)?.corrected

            val wordToSpan = SpannableString(etText.text)
            val annotations: List<Annotations> = response.result?.get(0)?.get(0)?.annotations!!

            for (annotation in annotations) {
                wordToSpan.setSpan(
                    ForegroundColorSpan(Color.RED),
                    annotation.startChar!!,
                    annotation.endChar!! + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            beforeText.text = wordToSpan
            afterText.text = corrected
        }
    }
}