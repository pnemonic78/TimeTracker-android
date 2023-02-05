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
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.model.UserCredentials
import com.tikalk.worktracker.databinding.FragmentLoginBinding
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

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<LoginViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            lifecycleScope.launch {
                val viewState: LoginViewState = viewModel
                val loginState = viewState.credentialsLogin.value
                val passwordState = viewState.credentialsPassword.value

                val loginValue = preferences.userCredentials.login
                val passwordValue = preferences.userCredentials.password

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
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewState: LoginViewState = viewModel

        binding.loginInput.doAfterTextChanged {
            val state = viewState.credentialsLogin.value
            lifecycleScope.launch {
                viewState.credentialsLogin.value = state.copy(value = it.toString())
            }
        }
        binding.passwordInput.doAfterTextChanged {
            val state = viewState.credentialsPassword.value
            lifecycleScope.launch {
                viewState.credentialsPassword.value = state.copy(value = it.toString())
            }
        }
        lifecycleScope.launch {
            viewState.credentialsLogin.collect { state ->
                binding.loginInput.setText(state.value)
                binding.loginInput.setSelection(state.value.length)
                binding.loginInput.error = if (state.isError) "!" else null
                binding.loginInput.isEnabled = state.isEnabled
                if (state.isError) binding.loginInput.requestFocus()
            }
        }
        lifecycleScope.launch {
            viewState.credentialsPassword.collect { state ->
                binding.passwordInput.setText(state.value)
                binding.passwordInput.setSelection(state.value.length)
                binding.passwordInput.error = if (state.isError) "!" else null
                binding.passwordInput.isEnabled = state.isEnabled
                if (state.isError) binding.passwordInput.requestFocus()
            }
        }
        lifecycleScope.launch {
            viewState.errorMessage.collect { errorMessage ->
                if (errorMessage.isEmpty()) {
                    binding.errorLabel.isVisible = false
                } else {
                    binding.errorLabel.isVisible = true
                    binding.errorLabel.text = errorMessage
                }
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
        binding.actionSignIn.setOnClickListener { viewState.onConfirmClick() }
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
    private suspend fun attemptLogin() {
        if (!binding.actionSignIn.isEnabled) {
            return
        }
        if (!viewModel.validateForm(resources)) return

        val viewState: LoginViewState = viewModel

        val loginState = viewState.credentialsLogin.value
        val passwordState = viewState.credentialsPassword.value

        // Store values at the time of the login attempt.
        val loginValue = loginState.value
        val passwordValue = passwordState.value

        binding.actionSignIn.isEnabled = false
        showProgress(true)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val today = formatSystemDate()
                val response = service.login(
                    name = loginValue,
                    password = passwordValue,
                    date = today
                )
                lifecycleScope.launch(Dispatchers.Main) {
                    binding.actionSignIn.isEnabled = true
                    showProgress(false)

                    if (isValidResponse(response)) {
                        val errorMessage = getResponseError(response)
                        if (errorMessage.isNullOrEmpty()) {
                            preferences.userCredentials = UserCredentials(loginValue, passwordValue)
                            notifyLoginSuccess(loginValue)
                        } else {
                            showError(errorMessage)
                            notifyLoginFailure(loginValue, errorMessage)
                        }
                    } else {
                        binding.passwordInput.requestFocus()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error signing in: ${e.message}")
                showProgressMain(false)
                handleErrorMain(e)
                binding.actionSignIn.isEnabled = true
            }
        }
    }

    override fun authenticate(submit: Boolean) = Unit

    private suspend fun notifyLoginSuccess(login: String) {
        dismissAllowingStateLoss()
        delegate.onLoginSuccess(login)
    }

    private suspend fun notifyLoginFailure(login: String, reason: String) {
        delegate.onLoginFailure(login, reason)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
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