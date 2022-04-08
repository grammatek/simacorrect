package org.grammatek.simacorrect.network

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import android.widget.Toast
import android.widget.Toast.makeText
import org.grammatek.apis.DevelopersApi
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class ConnectionManager {
    private var _sch: ScheduledThreadPoolExecutor? = null
    private var _periodicFuture: ScheduledFuture<*>? = null
    private val _apiServer: String = "yfirlestur.is"

    fun registerConnectionManager(app: Application) {
        val manager = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        manager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                try {
                    val nc: NetworkCapabilities? = manager.getNetworkCapabilities(network)
                    if(nc!!.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                        Log.d(TAG, "Internet available")
                        isNetworkConnected = true
                        startApiServiceHealthCheck()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Exception: ${e.message}")
                }
            }

            override fun onLost(network: Network) {
                isNetworkConnected = false
                stopApiServiceHealthCheck()

                try {
                    val nc: NetworkCapabilities? = manager.getNetworkCapabilities(network)
                    val nci = nc?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    Log.d(TAG, "LE NCI: $nci")
                    if(nci == null) {
                        val toast = makeText(app.applicationContext, "Simacorrect lost internet connection", Toast.LENGTH_SHORT)
                        toast.show()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Exception ${e.message}")
                }
            }
        })
    }

    // Start timer that polls regularly TTS service availability
    private fun startApiServiceHealthCheck() {
        // if already started: do nothing
        if (_periodicFuture == null || _periodicFuture!!.isCancelled) {
            _periodicFuture = _sch?.scheduleAtFixedRate(periodicTask, 0, 30, TimeUnit.SECONDS)
        }
    }

    // Stops timer for TTS service availability
    private fun stopApiServiceHealthCheck() {
        // if timer not running: do nothing
        // stop timer
        _periodicFuture?.cancel(false)
    }

    private fun isHostAvailable(host: String, port: Int, timeoutInMs: Int): Boolean {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutInMs)
                return true
            }
        } catch (e: IOException) {
            // Either we have a timeout, unreachable host or failed DNS lookup
            Log.d(TAG, "IO Exception: ${e.message}")
            return false
        }
    }

    private var periodicTask = Runnable {
        try {
            Log.d(TAG, "Is Reachable: $isServiceReachable, Is Connected: $isNetworkConnected")

            if (isHostAvailable(_apiServer, 443, 2000)) {
                isServiceReachable = true
                Log.d(TAG, "$_apiServer Service available!")
            } else {
                isServiceReachable = false
                Log.d(TAG, "$_apiServer Service is NOT available !")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception: " + e.message)
        }
    }

    companion object {
        private val TAG = ConnectionManager::class.java.simpleName

        private val api: DevelopersApi = DevelopersApi()
        var isNetworkConnected: Boolean = false
            private set
        var isServiceReachable: Boolean = false
            private set

        fun correctWord(text: String): String? {
            var correctedText = ""
            if(!isServiceReachable || !isNetworkConnected) {
                Log.d(TAG, "isServiceReachable: $isServiceReachable, isNetworkConnected: $isNetworkConnected")
                return null
            }
            try {
                val response = api.correctApiPost(text)
                correctedText = response.result?.get(0)?.get(0)?.corrected?.lowercase()
                    ?: throw NullPointerException("Received null value from response corrected text")
                val originalText = response.result?.get(0)?.get(0)?.original?.lowercase()
                    ?: throw NullPointerException("Received null value from response original text")
                Log.d(TAG, "corrected text: $correctedText, original text $originalText")
                if(correctedText == originalText) {
                    return null
                }
            } catch (e: Exception) {
                Log.d(TAG, "Exception: ${e.message}")
            }
            return correctedText
        }
    }

    init {
        _sch = Executors.newScheduledThreadPool(5) as ScheduledThreadPoolExecutor
    }
}
