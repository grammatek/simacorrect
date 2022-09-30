package org.grammatek.simacorrect

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView


class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "About"
        setContentView(R.layout.activity_about)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        populateAboutInformation()
    }

    private val emptyString = "" // for readability
    private fun populateAboutInformation() {
        val cardTitle = arrayListOf<String?>(
            getString(R.string.app_name),
            getString(R.string.info_app_version),
            getString(R.string.info_github),
            getString(R.string.info_about_device_title),
            getString(R.string.info_phone_model),
            getString(R.string.info_android_version),
            getString(R.string.info_other),
            emptyString,
            emptyString,
        )
        val cardText = arrayListOf<String?>(
            emptyString,
            getAppVersion(),
            getString(R.string.info_github),
            emptyString,
            Build.MODEL,
            Build.VERSION.RELEASE,
            emptyString,
            getString(R.string.info_copyright),
            getString(R.string.info_privacy_notice),
        )
        val cardUrl = arrayListOf<String?>(
            emptyString,
            emptyString,
            getString(R.string.info_repo_url),
            emptyString,
            emptyString,
            emptyString,
            emptyString,
            getString(R.string.info_copyright_url),
            getString(R.string.info_privacy_notice_url),
        )

        val titleArray = arrayOfNulls<String>(cardTitle.size)
        cardTitle.toArray<String>(titleArray)
        val textArray = arrayOfNulls<String>(cardText.size)
        cardText.toArray<String>(textArray)
        val urlArray = arrayOfNulls<String>(cardUrl.size)
        cardUrl.toArray<String>(urlArray)


        val infoView: ListView = findViewById(R.id.infoListView)
        infoView.adapter = SettingsArrayAdapter(this, textArray, titleArray, urlArray)
    }

    private fun getAppVersion(): String? {
        return try {
            val pInfo: PackageInfo = applicationContext.packageManager
                .getPackageInfo(applicationContext.packageName, 0)
            pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            getString(R.string.info_version_error)
        }
    }

    private class SettingsArrayAdapter(
        context: Context,
        private val text: Array<String?>,
        private val title: Array<String?>,
        private val url: Array<String?>
    ): ArrayAdapter<String?>(context, R.layout.activity_about, text) {
        private val ctx: Context = context

        override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup
        ): View {
            var cView = convertView
            val inflater = ctx
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            if (cView == null) {
                cView = when {
                    text[position].isNullOrEmpty() ->
                        getTitleView(inflater, parent, position)
                    this.url[position]!!.isNotEmpty() ->
                        getClickableCardView(inflater, parent, position)
                    else ->
                        getCardView(inflater, parent, position)
                }
            }

            return cView
        }

        private fun getTitleView(
            inflater: LayoutInflater,
            parent: ViewGroup,
            position: Int
        ): View {
            val cView1: View? = inflater.inflate(R.layout.about_list_title, parent, false)
            val title = cView1!!.findViewById<TextView>(R.id.title)
            title.text = this.title[position]
            return cView1
        }

        private fun getCardView(
            inflater: LayoutInflater,
            parent: ViewGroup,
            position: Int
        ): View {
            val cView1: View? = inflater.inflate(R.layout.about_list_item, parent, false)
            val infoDetail = cView1!!.findViewById<TextView>(R.id.info_title)
            val infoType = cView1.findViewById<TextView>(R.id.info_text)
            infoDetail.text = this.title[position]
            infoType.text = text[position]
            return cView1
        }

        private fun getClickableCardView(
            inflater: LayoutInflater,
            parent: ViewGroup,
            position: Int
        ): View {
            val cView1: View? = inflater.inflate(R.layout.about_list_item_clickable, parent, false)
            val title = cView1!!.findViewById<TextView>(R.id.info_title)
            val card = cView1.findViewById<CardView>(R.id.cardView)
            card.setOnClickListener {
                val uri: Uri = Uri.parse(this.url[position])
                val intent = Intent(Intent.ACTION_VIEW, uri)
                ctx.startActivity(intent)
            }
            title.text = text[position]
            return cView1
        }
    }
}
