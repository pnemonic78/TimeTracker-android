package com.tikalk.worktracker.net

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Response

/**
 * Activity that is Internet-aware.
 */
abstract class InternetActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(receiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    /**
     * Notification that the Internet is connected and available.
     */
    protected open fun onInternetConnected() {
    }

    /**
     * Notification that the Internet is disconnected and unavailable.
     */
    protected open fun onInternetDisconnected() {
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            when (action) {
                ConnectivityManager.CONNECTIVITY_ACTION -> {
                    val manager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
                    val netInfo = manager.activeNetworkInfo
                    if ((netInfo != null) && netInfo.isConnected) {
                        //FIXME ping the server to verify connection.
                        onInternetConnected()
                    } else {
                        onInternetDisconnected()
                    }
                }
            }
        }
    }

    protected fun validResponse(response: Response<String>, expected: String): Boolean {
        val body = response.body()
        if (response.isSuccessful && (body != null)) {
            val networkResponse = response.raw().networkResponse()
            val priorResponse = response.raw().priorResponse()
            if ((networkResponse != null) && (priorResponse != null) && priorResponse.isRedirect) {
                val networkUrl = networkResponse.request().url()
                val priorUrl = priorResponse.request().url()
                if (networkUrl == priorUrl) {
                    return true
                }
                if (networkUrl.pathSegments()[networkUrl.pathSize() - 1] == expected) {
                    return true
                }
                return false
            }
            return true
        }
        return false
    }
}