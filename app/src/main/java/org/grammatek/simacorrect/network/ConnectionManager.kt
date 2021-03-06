package org.grammatek.simacorrect.network

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import org.grammatek.apis.DevelopersApi
import org.grammatek.models.YfirlesturResponse
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Manages connectivity and API calls for the application
 */
class ConnectionManager {
    private var _threadPoolExecutor: ScheduledThreadPoolExecutor? = null
    private var _periodicFuture: ScheduledFuture<*>? = null
    private val _apiServer: String = "yfirlestur.is"

    /**
     * Registers network callback to monitor network changes
     *
     * @param [app] Provides access to application's connectivity service
     */
    fun registerConnectionManager(app: Application) {
        val manager = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        manager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val nc: NetworkCapabilities? = manager.getNetworkCapabilities(network)
                if(nc != null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    Log.d(TAG, "Internet available")
                    g_isNetworkConnected = true
                    startApiServiceHealthCheck()
                } else {
                    Log.d(TAG, "Internet unavailable")
                    g_isNetworkConnected = false
                    stopApiServiceHealthCheck()
                }
            }

            override fun onLost(network: Network) {
                g_isNetworkConnected = false
                stopApiServiceHealthCheck()
            }
        })
    }

    /**
     * Start timer that polls regularly API service availability.
     */
    private fun startApiServiceHealthCheck() {
        // if already started: do nothing
        if (_periodicFuture == null || _periodicFuture!!.isCancelled) {
            _periodicFuture = _threadPoolExecutor?.scheduleAtFixedRate(periodicTask, 0, 30, TimeUnit.SECONDS)
        }
    }

    /**
     * Stops timer for API service availability.
     */
    private fun stopApiServiceHealthCheck() {
        // if timer not running: do nothing
        // stop timer
        _periodicFuture?.cancel(false)
    }

    /**
     * Check if host is reachable.
     *
     * @param [host] The host to check for availability. Can either be a machine name, such as "google.com",
     *             or a textual representation of its IP address, such as "8.8.8.8".
     * @param [port] The port number.
     * @param [timeoutInMs] The timeout in milliseconds.
     * @return True if the host is reachable. False otherwise.
     */
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

    /**
     * Thread that periodically checks to see if the API host is available.
     */
    private var periodicTask = Runnable {
        try {
            Log.d(TAG, "Is Reachable: $g_isServiceReachable, Is Connected: $g_isNetworkConnected")

            if (isHostAvailable(_apiServer, 443, 2000)) {
                g_isServiceReachable = true
                Log.d(TAG, "$_apiServer Service available!")
            } else {
                g_isServiceReachable = false
                Log.d(TAG, "$_apiServer Service is NOT available !")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception: " + e.message)
        }
    }

    companion object {
        private val TAG = ConnectionManager::class.java.simpleName
        private val API: DevelopersApi = DevelopersApi()

        var g_isNetworkConnected: Boolean = false
            private set
        var g_isServiceReachable: Boolean = false
            private set

        /**
         * Returns the corrected spelling for [textToCorrect] if successful.
         * Returns null if there is no correction to be made OR no connection.
         *
         * @param [textToCorrect] The text to be spell checked (corrected).
         * @return YfirlesturResponse which contains the spell checking
         * information of [textToCorrect].
         */
        fun correctSentence(textToCorrect: String): YfirlesturResponse? {
            if(!g_isServiceReachable || !g_isNetworkConnected) {
                Log.d(TAG, "correctSentence: isServiceReachable: $g_isServiceReachable, isNetworkConnected: $g_isNetworkConnected")
                return null
            }
            try {
                return API.correctApiPost(textToCorrect)
            } catch (e: Exception) {
                Log.d(TAG, "Exception: $e")
            }
            return null
        }
    }

    init {
        _threadPoolExecutor = Executors.newScheduledThreadPool(5) as ScheduledThreadPoolExecutor
    }
}
