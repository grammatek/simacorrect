package org.grammatek.simacorrect

import android.app.Application
import android.content.Context
import org.grammatek.simacorrect.network.ConnectionManager

class App : Application() {
    private lateinit var _connectionChecker: ConnectionManager

    override fun onCreate() {
        super.onCreate()

        appContext = applicationContext
        _connectionChecker = ConnectionManager()
        _connectionChecker.registerConnectionManager(this)
    }

    companion object {
        lateinit var appContext: Context
            private set
    }
}
