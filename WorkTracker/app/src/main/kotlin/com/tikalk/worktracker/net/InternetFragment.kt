/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2019, Tikal Knowledge, Ltd.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * • Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * • Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * • Neither the name of the copyright holder nor the names of its
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

import android.app.AlertDialog
import android.os.Bundle
import androidx.annotation.StringRes
import com.tikalk.app.runOnUiThread
import com.tikalk.html.textBr
import com.tikalk.worktracker.R
import com.tikalk.worktracker.app.TrackerFragment
import com.tikalk.worktracker.auth.AccessDeniedException
import com.tikalk.worktracker.auth.AuthenticationException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import retrofit2.Response
import java.net.UnknownHostException

/**
 * Fragment that is Internet-aware.
 */
abstract class InternetFragment : TrackerFragment {

    constructor() : super()

    constructor(args: Bundle) : super(args)

    protected fun isValidResponse(response: Response<String>): Boolean {
        val html = response.body()
        if (response.isSuccessful && (html != null)) {
            val networkResponse = response.raw().networkResponse()
            val priorResponse = response.raw().priorResponse()
            if ((networkResponse != null) && (priorResponse != null) && priorResponse.isRedirect) {
                val networkUrl = networkResponse.request().url()
                val priorUrl = priorResponse.request().url()
                if (networkUrl == priorUrl) {
                    return true
                }
                when (networkUrl.pathSegments()[networkUrl.pathSize() - 1]) {
                    TimeTrackerService.PHP_TIME,
                    TimeTrackerService.PHP_REPORT ->
                        return true
                    TimeTrackerService.PHP_ACCESS_DENIED ->
                        throw AccessDeniedException()
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

        val errorNode = body.selectFirst("td[class='error']")
        if (errorNode != null) {
            return errorNode.textBr()
        }

        return null
    }

    /**
     * Shows the progress UI and hides the login form.
     * @param show visible?
     */
    protected fun showProgress(show: Boolean) {
        (activity as InternetActivity?)?.showProgress(show)
    }

    /**
     * Shows the progress UI and hides the login form, on the main thread.
     * @param show visible?
     */
    protected fun showProgressMain(show: Boolean) {
        runOnUiThread { showProgress(show) }
    }

    /**
     * Handle an error.
     * @param error the error.
     */
    protected fun handleError(error: Throwable) {
        when (error) {
            is AccessDeniedException -> showAccessDeniedError()
            is AuthenticationException -> authenticate()
            is UnknownHostException -> showUnknownHostError()
        }
    }

    /**
     * Handle an error, on the main threadd.
     * @param error the error.
     */
    protected fun handleErrorMain(error: Throwable) {
        runOnUiThread {
            handleError(error)
        }
    }

    private fun showError(@StringRes messageId: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.error_title)
            .setMessage(messageId)
            .setIcon(R.drawable.ic_report_problem)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    protected fun showUnknownHostError() {
        showError(R.string.error_unknownHost)
    }

    protected fun showAccessDeniedError() {
        showError(R.string.error_accessDenied)
    }
}