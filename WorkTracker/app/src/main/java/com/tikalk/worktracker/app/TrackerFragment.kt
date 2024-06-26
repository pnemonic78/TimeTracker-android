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

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.annotation.WorkerThread
import androidx.lifecycle.lifecycleScope
import com.tikalk.app.TikalFragment
import com.tikalk.app.runOnUiThread
import com.tikalk.model.TikalResult
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
abstract class TrackerFragment : TikalFragment,
    TrackerFragmentDelegate.TrackerFragmentDelegateCallback,
    Runnable {

    constructor() : super()

    constructor(args: Bundle) : super(args)

    protected abstract val viewModel: TrackerViewModel
    protected val delegate = TrackerFragmentDelegate(fragment = this, callback = this)
    protected val firstRun: Boolean get() = delegate.firstRun

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        delegate.onCreate(savedInstanceState)
    }

    protected open fun onLoginSuccess(login: String) {
        Timber.i("login success")
        run()
    }

    protected open fun onLoginFailure(login: String, reason: String) {
        Timber.e("login failure: $reason")
    }

    override fun authenticateMain(submit: Boolean) {
        runOnUiThread { authenticate(submit) }
    }

    protected fun handleError(error: Throwable) {
        delegate.handleError(error)
    }

    protected fun handleError(result: TikalResult.Error<*>) {
        delegate.handleError(result.exception)
    }

    @WorkerThread
    protected open fun handleErrorMain(error: Throwable) {
        delegate.handleErrorMain(error)
    }

    override fun showError(@StringRes messageId: Int) {
        delegate.showError(messageId)
    }

    override fun showErrorMain(messageId: Int) {
        runOnUiThread { showError(messageId) }
    }

    override fun showProgress(show: Boolean) {
        (activity as? TrackerActivity)?.showProgress(show)
    }

    override fun showProgressMain(show: Boolean) {
        runOnUiThread { showProgress(show) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                showProgress(show = isLoading)
            }
        }
        lifecycleScope.launch {
            viewModel.onError.collect { error ->
                if (error != null) handleError(error)
            }
        }
        lifecycleScope.launch {
            delegate.login.collect { (login, reason) ->
                if (login.isEmpty()) return@collect
                if (reason == null) {
                    onLoginSuccess(login)
                } else {
                    onLoginFailure(login, reason)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        run()
    }

    override fun onStop() {
        super.onStop()
        delegate.onStop()
    }
}