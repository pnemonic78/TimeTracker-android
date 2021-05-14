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
import androidx.lifecycle.LiveData
import com.tikalk.app.runOnUiThread
import com.tikalk.worktracker.BuildConfig
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.AuthenticationViewModel
import com.tikalk.worktracker.data.TimeTrackerRepository
import com.tikalk.worktracker.db.TrackerDatabase
import com.tikalk.worktracker.net.InternetHelper
import com.tikalk.worktracker.net.TimeTrackerService
import com.tikalk.worktracker.preference.TimeTrackerPrefs
import org.koin.android.ext.android.inject
import retrofit2.Response
import timber.log.Timber

class TrackerFragmentHelper(
    private val fragment: Fragment,
    private val callback: TrackerFragmentHelperCallback
) {

    lateinit var authenticationViewModel: AuthenticationViewModel
    lateinit var login: LiveData<AuthenticationViewModel.LoginData>
    lateinit var basicRealm: LiveData<AuthenticationViewModel.BasicRealmData>

    val preferences by fragment.inject<TimeTrackerPrefs>()
    val db by fragment.inject<TrackerDatabase>()
    val service by fragment.inject<TimeTrackerService>()
    val dataSource by fragment.inject<TimeTrackerRepository>()
    var firstRun = true
        private set
    val internet = InternetHelper(callback)

    fun onCreate(savedInstanceState: Bundle?) {
        authenticationViewModel = AuthenticationViewModel.get(fragment)
        login = authenticationViewModel.login
        basicRealm = authenticationViewModel.basicRealm
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

    fun onLoginSuccess(login: String) {
        authenticationViewModel.onLoginSuccess(login)
    }

    fun onLoginFailure(login: String, reason: String) {
        authenticationViewModel.onLoginFailure(login, reason)
    }

    fun onBasicRealmSuccess(realmName: String, username: String) {
        authenticationViewModel.onBasicRealmSuccess(realmName, username)
    }

    fun onBasicRealmFailure(realmName: String, username: String, reason: String) {
        authenticationViewModel.onBasicRealmFailure(realmName, username, reason)
    }

    interface TrackerFragmentHelperCallback : InternetHelper.InternetCallback {
    }

    companion object {
        const val EXTRA_ACTION = BuildConfig.APPLICATION_ID + ".ACTION"

        const val ACTION_STOP = BuildConfig.APPLICATION_ID + ".action.STOP"
    }
}