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

package com.tikalk.worktracker.auth

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.tikalk.compose.TikalTheme
import com.tikalk.core.databinding.FragmentComposeBinding
import com.tikalk.worktracker.R
import com.tikalk.worktracker.model.auth.UserCredentials
import com.tikalk.worktracker.net.InternetDialogFragment
import com.tikalk.worktracker.time.formatSystemDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * A login screen that offers login via login/password.
 */
class LoginFragment : InternetDialogFragment {

    constructor() : super()

    constructor(args: Bundle) : super(args)

    private var _binding: FragmentComposeBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<LoginViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            lifecycleScope.launch {
                val viewState: LoginViewState = viewModel
                val loginState = viewState.credentialsLogin.value
                val passwordState = viewState.credentialsPassword.value

                val userCredentials = viewModel.userCredentials
                val loginValue = userCredentials.login
                val passwordValue = userCredentials.password

                viewState.credentialsLogin.emit(loginState.copy(value = loginValue))
                viewState.credentialsPassword.emit(passwordState.copy(value = passwordValue))
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setTitle(R.string.activity_login)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentComposeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewState: LoginViewState = viewModel

        binding.composeView.setContent {
            TikalTheme {
                LoginForm(viewState = viewState)
            }
        }

        lifecycleScope.launch {
            viewModel.onDialogConfirmClick.collect {
                if (it) {
                    viewModel.clearEvents()
                    attemptLogin()
                }
            }
        }
        lifecycleScope.launch {
            viewModel.onDialogDismiss.collect {
                if (it) {
                    viewModel.clearEvents()
                    onDismiss()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @MainThread
    override fun run() {
        Timber.i("run")
        val args = this.arguments ?: return

        if (args.containsKey(EXTRA_LOGIN)) {
            lifecycleScope.launch {
                val viewState: LoginViewState = viewModel
                val loginState = viewState.credentialsLogin.value
                val passwordState = viewState.credentialsPassword.value

                val loginValue = args.getString(EXTRA_LOGIN) ?: ""
                val passwordValue = args.getString(EXTRA_PASSWORD) ?: ""

                viewState.credentialsLogin.emit(loginState.copy(value = loginValue))
                viewState.credentialsPassword.emit(passwordState.copy(value = passwordValue))
            }
        }
        if (args.containsKey(EXTRA_SUBMIT) && args.getBoolean(EXTRA_SUBMIT)) {
            viewModel.onConfirmClick()
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid login, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    @MainThread
    private suspend fun attemptLogin() {
        if (!viewModel.validateForm(resources)) return

        val viewState: LoginViewState = viewModel

        val loginState = viewState.credentialsLogin.value
        val passwordState = viewState.credentialsPassword.value

        // Store values at the time of the login attempt.
        val loginValue = loginState.value
        val passwordValue = passwordState.value

        showProgress(true)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val today = formatSystemDate()
                val response = viewModel.login(
                    name = loginValue,
                    password = passwordValue,
                    date = today
                )
                lifecycleScope.launch(Dispatchers.Main) {
                    showProgress(false)

                    if (isValidResponse(response)) {
                        val errorMessage = getResponseError(response)
                        if (errorMessage.isNullOrEmpty()) {
                            viewModel.userCredentials = UserCredentials(loginValue, passwordValue)
                            notifyLoginSuccess(loginValue)
                        } else {
                            showError(errorMessage)
                            notifyLoginFailure(loginValue, errorMessage)
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error signing in: ${e.message}")
                showProgressMain(false)
                handleErrorMain(e)
            }
        }
    }

    override fun authenticate(submit: Boolean) = Unit

    override fun authenticateMain(submit: Boolean) = Unit

    private suspend fun notifyLoginSuccess(login: String) {
        dismissAllowingStateLoss()
        delegate.onLoginSuccess(login)
    }

    private suspend fun notifyLoginFailure(login: String, reason: String) {
        delegate.onLoginFailure(login, reason)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        onDismiss()
    }

    private fun onDismiss() {
        lifecycleScope.launch { notifyLoginFailure("", REASON_CANCEL) }
    }

    private suspend fun showError(message: String) {
        viewModel.showError(message)
    }

    companion object {
        const val EXTRA_LOGIN = "login"
        const val EXTRA_PASSWORD = "password"
        const val EXTRA_SUBMIT = "submit"

        const val REASON_CANCEL = "onCancel"
    }
}