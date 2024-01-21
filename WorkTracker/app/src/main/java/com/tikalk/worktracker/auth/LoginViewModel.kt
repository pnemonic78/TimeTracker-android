/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2020, Tikal Knowledge, Ltd.
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

import android.content.res.Resources
import androidx.lifecycle.viewModelScope
import com.tikalk.compose.TextFieldViewState
import com.tikalk.compose.UnitCallback
import com.tikalk.worktracker.R
import com.tikalk.worktracker.app.TrackerServices
import com.tikalk.worktracker.app.TrackerViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    services: TrackerServices
) : TrackerViewModel(services),
    LoginViewState {

    override val credentialsLogin = MutableStateFlow(TextFieldViewState())
    override val credentialsPassword = MutableStateFlow(TextFieldViewState())
    private val _error = MutableStateFlow<LoginError?>(null)
    override val error: StateFlow<LoginError?> = _error
    override val onConfirmClick: UnitCallback = ::onDialogConfirmClick
    override val onDismiss: UnitCallback = ::onDialogDismiss

    init {
        val userCredentials = services.preferences.userCredentials

        credentialsLogin.value.value = userCredentials.login
        credentialsPassword.value.value = userCredentials.password
    }

    private val _onDialogConfirmClick = MutableStateFlow(false)
    val onDialogConfirmClick: StateFlow<Boolean> = _onDialogConfirmClick

    private fun onDialogConfirmClick() {
        viewModelScope.launch { _onDialogConfirmClick.emit(true) }
    }

    private val _onDialogDismiss = MutableStateFlow(false)
    val onDialogDismiss: StateFlow<Boolean> = _onDialogDismiss

    private fun onDialogDismiss() {
        viewModelScope.launch { _onDialogConfirmClick.emit(true) }
    }

    suspend fun validateForm(resources: Resources): Boolean {
        val viewState: LoginViewState = this

        val credentialsLoginState = viewState.credentialsLogin
        val credentialsPasswordState = viewState.credentialsPassword

        val credentialsLogin = credentialsLoginState.value
        val credentialsPassword = credentialsPasswordState.value

        // Reset errors.
        _error.emit(null)

        // Store values at the time of the submission attempt.
        val loginValue = credentialsLogin.value
        val passwordValue = credentialsPassword.value

        val validator = LoginValidator()

        // Check for a valid login name.
        when (validator.validateUsername(loginValue)) {
            LoginValidator.ERROR_REQUIRED -> {
                _error.emit(LoginError.Name(resources.getString(R.string.error_field_required)))
                return false
            }
            LoginValidator.ERROR_LENGTH,
            LoginValidator.ERROR_INVALID -> {
                _error.emit(LoginError.Name(resources.getString(R.string.error_invalid_login)))
                return false
            }
        }

        // Check for a valid password, if the user entered one.
        when (validator.validatePassword(passwordValue)) {
            LoginValidator.ERROR_REQUIRED -> {
                _error.emit(LoginError.Password(resources.getString(R.string.error_field_required)))
                return false
            }
            LoginValidator.ERROR_LENGTH,
            LoginValidator.ERROR_INVALID -> {
                _error.emit(LoginError.Password(resources.getString(R.string.error_invalid_password)))
                return false
            }
        }

        return true
    }

    override fun onCleared() {
        super.onCleared()
        clearEvents()
    }

    fun clearEvents() {
        _onDialogConfirmClick.value = false
        _onDialogDismiss.value = false
    }

    suspend fun showError(message: String) {
        _error.emit(LoginError.General(message))
    }
}