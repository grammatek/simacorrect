package org.grammatek.simacorrect

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableString
import android.text.util.Linkify
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.edit


class MainActivity : AppCompatActivity() {
    private lateinit var etText: EditText
    private lateinit var btnSettings: CardView
    private lateinit var btnFeedback: CardView
    private lateinit var btnAbout: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        etText = findViewById(R.id.textInputEditText)
        btnFeedback = findViewById(R.id.feedbackCard)
        btnSettings = findViewById(R.id.settingsCard)
        btnAbout = findViewById(R.id.aboutCard)
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

    override fun onResume() {
        super.onResume()
        val privacyAccepted = this.getPreferences(Context.MODE_PRIVATE).getBoolean("privacy_info_accepted", false)
        if (!privacyAccepted) {
            showPrivacyNoticeDialog()
        }
    }

    /**
     * Shows Réttritun privacy notice dialog
     */
    private fun showPrivacyNoticeDialog() {
        val privacyNotice = SpannableString(resources.getString(R.string.privacy_notice))
        Linkify.addLinks(privacyNotice, Linkify.ALL)

        val dialogBox: AlertDialog = AlertDialog.Builder(this, R.style.theme_dialog)
            .setTitle(R.string.privacy_title)
            .setMessage(privacyNotice)
            .setPositiveButton(R.string.ok) { _, _ ->
                this.getPreferences(Context.MODE_PRIVATE).edit {
                    putBoolean("privacy_info_accepted", true)
                    apply()
                }
            }
            .setCancelable(false)
            .create()

        dialogBox.show()
    }
}
