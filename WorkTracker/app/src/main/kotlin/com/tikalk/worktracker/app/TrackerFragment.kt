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

package com.tikalk.worktracker.app

import android.app.AlertDialog
import android.os.Bundle
import androidx.annotation.StringRes
import com.tikalk.app.TikalFragment
import com.tikalk.app.runOnUiThread
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.AuthenticationViewModel
import com.tikalk.worktracker.data.TimeTrackerRepository
import com.tikalk.worktracker.db.TrackerDatabase
import com.tikalk.worktracker.net.InternetHelper
import com.tikalk.worktracker.net.TimeTrackerService
import com.tikalk.worktracker.preference.TimeTrackerPrefs
import org.koin.android.ext.android.inject
import timber.log.Timber

abstract class TrackerFragment : TikalFragment, InternetHelper.InternetCallback {

    constructor() : super()

    constructor(args: Bundle) : super(args)

    protected val preferences by inject<TimeTrackerPrefs>()
    protected val db by inject<TrackerDatabase>()
    protected val service by inject<TimeTrackerService>()
    protected val dataSource by inject<TimeTrackerRepository>()
    protected var firstRun = true
        private set
    protected lateinit var authenticationViewModel: AuthenticationViewModel
    protected val internet = InternetHelper(this)
    protected val helper = TrackerFragmentHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authenticationViewModel = AuthenticationViewModel.get(this)

        firstRun = (savedInstanceState == null)
    }

    protected fun authenticateMain(submit: Boolean = true) {
        Timber.i("authenticateMain submit=$submit")
        runOnUiThread { authenticate(submit) }
    }

    /**
     * Handle an error.
     * @param error the error.
     */
    protected fun handleError(error: Throwable) {
        internet.handleError(error)
    }

    /**
     * Handle an error, on the main thread.
     * @param error the error.
     */
    protected open fun handleErrorMain(error: Throwable) {
        runOnUiThread { handleError(error) }
    }

    override fun showError(@StringRes messageId: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.error_title)
            .setMessage(messageId)
            .setIcon(R.drawable.ic_report_problem)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}