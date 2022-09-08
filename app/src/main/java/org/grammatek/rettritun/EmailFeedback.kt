package org.grammatek.rettritun

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class EmailFeedback : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sendEmail()
    }

    private fun sendEmail() {
        val recipientEmail = "info@grammatek.com"
        val subject = getString(R.string.email_subject)
        val msg = getString(R.string.email_message)
        val emailIntent = Intent(Intent.ACTION_SENDTO)
        emailIntent.data = Uri.parse("mailto:$recipientEmail")
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
        emailIntent.putExtra(Intent.EXTRA_TEXT, msg)
        val sendMsg = getString(R.string.send_message)
        try {
            startActivity(Intent.createChooser(emailIntent, sendMsg))
            finish()
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(
                this@EmailFeedback,
                "There is no email client installed.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}