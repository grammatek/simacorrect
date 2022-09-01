package org.grammatek.simacorrect

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        title = "About"

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        populateInformation()
    }

    private fun populateInformation() {
        val info: ArrayList<String?> = object : ArrayList<String?>() {
            init {
                add(getString(R.string.info_app_version))
                add(getString(R.string.info_url))
                add(getString(R.string.info_copyright))
                add(getString(R.string.info_privacy_notice))
                add(getString(R.string.info_android_version))
                add(getString(R.string.info_phone_model))
            }
        }
        val data: ArrayList<String?> = object : ArrayList<String?>() {
            init {
                try {
                    val pInfo: PackageInfo = applicationContext.packageManager
                        .getPackageInfo(applicationContext.packageName, 0)
                    val version: String = pInfo.versionName
                    add(version)
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                    add(getString(R.string.info_version_error))
                }
                add(getString(R.string.info_repo_url))
                add(getString(R.string.info_about))
                add(getString(R.string.info_privacy_notice_url))
                add(Build.VERSION.RELEASE)
                add(Build.MODEL)
            }
        }
        val dataArray = arrayOfNulls<String>(data.size)
        data.toArray<String>(dataArray)
        val infoArray = arrayOfNulls<String>(info.size)
        info.toArray<String>(infoArray)
        val infoView: ListView = findViewById(R.id.infoListView)
        infoView.adapter = SettingsArrayAdapter(this, infoArray, dataArray)
    }

    private class SettingsArrayAdapter(
        context: Context,
        values: Array<String?>,
        data: Array<String?>
    ) :
        ArrayAdapter<String?>(context, R.layout.activity_about, values) {
        private val ctx: Context = context
        private val values: Array<String?> = values
        private val data: Array<String?> = data
        override fun getViewTypeCount(): Int {
            return 2
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var cView = convertView
            val inflater = ctx
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            if (cView == null) {
                cView = inflater.inflate(R.layout.info_item, parent, false)
            }
            val infoType = cView!!.findViewById<TextView>(R.id.feedback)
            val infoDetail = cView.findViewById<TextView>(R.id.info_detail)
            infoType.text = values[position]
            infoDetail.text = data[position]
            return cView
        }
    }

    companion object {
        private val TAG = AboutActivity::class.java.simpleName
    }
}