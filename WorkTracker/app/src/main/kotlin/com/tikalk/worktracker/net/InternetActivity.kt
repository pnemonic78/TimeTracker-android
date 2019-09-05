/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2017, Tikal Knowledge, Ltd.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tikalk.worktracker.net

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import com.tikalk.app.TikalActivity
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import retrofit2.Response

/**
 * Activity that is Internet-aware.
 */
abstract class InternetActivity : TikalActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

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
            when (intent.action) {
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

    protected fun isValidResponse(response: Response<String>): Boolean {
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
                if (networkUrl.pathSegments()[networkUrl.pathSize() - 1] == TimeTrackerService.PHP_TIME) {
                    return true
                }
                return false
            }
            return true
        }
        return false
    }

    protected fun getResponseError(html: String?): String? {
        if (html == null) return null
        val doc: Document = Jsoup.parse(html)
        return findError(doc)
    }

    /**
     * Find the first error table element.
     */
    protected fun findError(doc: Document): String? {
        val body = doc.body()
        val tables = body.select("table")
        if (tables.isEmpty()) {
            return body.text()
        }

        for (table in tables) {
            val errorNode = table.selectFirst("td[class='error']")
            if (errorNode != null) {
                return errorNode.text()
            }
        }

        return null
    }
}