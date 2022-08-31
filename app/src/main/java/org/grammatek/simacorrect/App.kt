package org.grammatek.simacorrect

import android.app.Application
import android.content.Context
import android.util.Log
import org.grammatek.simacorrect.network.ConnectionManager

class App : Application() {
    private lateinit var _connectionChecker: ConnectionManager

    override fun onCreate() {
        Log.v(TAG, "onCreate()")
        super.onCreate()

        appContext = applicationContext
        _connectionChecker = ConnectionManager()
        _connectionChecker.registerConnectionManager(this)
    }

    companion object {
        private val TAG = App::class.java.simpleName
        lateinit var appContext: Context
            private set
    }
}
