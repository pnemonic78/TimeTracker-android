/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2021, Tikal Knowledge, Ltd.
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

package com.tikalk.worktracker.app

import android.app.AlertDialog
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.tikalk.app.runOnUiThread
import com.tikalk.worktracker.BuildConfig
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.AuthenticationViewModel
import com.tikalk.worktracker.net.InternetFragmentDelegate
import kotlinx.coroutines.flow.Flow
import retrofit2.Response
import timber.log.Timber

class TrackerFragmentDelegate(
    private val fragment: Fragment,
    private val callback: TrackerFragmentDelegateCallback
) {

    val authenticationViewModel by fragment.activityViewModels<AuthenticationViewModel>()
    internal val login: Flow<AuthenticationViewModel.LoginData> get() = authenticationViewModel.login

    var firstRun = true
        private set
    val internet = InternetFragmentDelegate(callback)

    fun onCreate(savedInstanceState: Bundle?) {
        firstRun = (savedInstanceState == null)
    }

    fun authenticateMain(submit: Boolean = true) {
        Timber.i("authenticateMain submit=$submit")
        fragment.runOnUiThread { callback.authenticate(submit) }
    }

    /**
     * Handle an error.
     * @param error the error.
     */
    fun handleError(error: Throwable) {
        internet.handleError(error)
    }

    /**
     * Handle an error, on the main thread.
     * @param error the error.
     */
    fun handleErrorMain(error: Throwable) {
        fragment.runOnUiThread { handleError(error) }
    }

    fun showError(@StringRes messageId: Int) {
        AlertDialog.Builder(fragment.requireContext())
            .setTitle(R.string.error_title)
            .setMessage(messageId)
            .setIcon(R.drawable.ic_report_problem)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    fun isValidResponse(response: Response<String>): Boolean {
        return internet.isValidResponse(response)
    }

    fun getResponseError(html: String?): String? {
        return internet.getResponseError(html)
    }

    suspend fun onLoginSuccess(login: String) {
        authenticationViewModel.onLoginSuccess(login)
    }

    suspend fun onLoginFailure(login: String, reason: String) {
        authenticationViewModel.onLoginFailure(login, reason)
    }

    interface TrackerFragmentDelegateCallback : InternetFragmentDelegate.InternetFragmentCallback

    companion object {
        const val EXTRA_ACTION = BuildConfig.APPLICATION_ID + ".ACTION"
        const val EXTRA_DATE = BuildConfig.APPLICATION_ID + ".DATE"

        const val ACTION_DATE = BuildConfig.APPLICATION_ID + ".action.DATE"
        const val ACTION_STOP = BuildConfig.APPLICATION_ID + ".action.STOP"
        const val ACTION_TODAY = BuildConfig.APPLICATION_ID + ".action.TODAY"
    }
}