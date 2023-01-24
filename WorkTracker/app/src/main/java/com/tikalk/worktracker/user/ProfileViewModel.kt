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

package com.tikalk.worktracker.user

import android.content.res.Resources
import com.tikalk.compose.TextFieldViewState
import com.tikalk.compose.UnitCallback
import com.tikalk.worktracker.R
import com.tikalk.worktracker.app.TrackerServices
import com.tikalk.worktracker.app.TrackerViewModel
import com.tikalk.worktracker.auth.LoginValidator
import com.tikalk.worktracker.model.ProfilePage
import com.tikalk.worktracker.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    services: TrackerServices
) : TrackerViewModel(services),
    ProfileViewState {

    private val _user = MutableStateFlow(User.EMPTY)
    val user: StateFlow<User> = _user

    private val profileUpdateData = MutableStateFlow<ProfileData?>(null)
    val profileUpdate: StateFlow<ProfileData?> = profileUpdateData

    /**
     * Data for profile callbacks.
     */
    data class ProfileData(val user: User, val reason: String? = null)

    /**
     * Profile update was successful.
     * @param user the updated user.
     */
    suspend fun onProfileSuccess(user: User) {
        notifyProfileSuccess(user)
    }

    /**
     * Profile update failed.
     * @param user the current user.
     * @param reason the failure reason.
     */
    suspend fun onProfileFailure(user: User, reason: String) {
        notifyProfileFailure(user, reason)
    }

    private suspend fun notifyProfileSuccess(user: User) {
        profileUpdateData.emit(ProfileData(user))
    }

    private suspend fun notifyProfileFailure(user: User, reason: String) {
        profileUpdateData.emit(ProfileData(user, reason))
    }

    private val _userDisplayName = MutableStateFlow(TextFieldViewState())
    override val userDisplayName: StateFlow<TextFieldViewState> = _userDisplayName
    private val _userEmail = MutableStateFlow(TextFieldViewState())
    override val userEmail: StateFlow<TextFieldViewState> = _userEmail
    private val _credentialsLogin = MutableStateFlow(TextFieldViewState())
    override val credentialsLogin: StateFlow<TextFieldViewState> = _credentialsLogin
    private val _credentialsPassword = MutableStateFlow(TextFieldViewState())
    override val credentialsPassword: StateFlow<TextFieldViewState> = _credentialsPassword
    private val _credentialsPasswordConfirmation = MutableStateFlow(TextFieldViewState())
    override val credentialsPasswordConfirmation: StateFlow<TextFieldViewState> =
        _credentialsPasswordConfirmation
    private var _errorMessage = MutableStateFlow("")
    override val errorMessage: StateFlow<String> = _errorMessage
    override val onConfirmClick: UnitCallback = ::onDialogConfirmClick

    init {
        val user = services.preferences.user
        val userCredentials = services.preferences.userCredentials

        var email = user.email
        if (email.isNullOrEmpty()) email = userCredentials.login

        _userDisplayName.value.value = user.displayName ?: ""
        _userEmail.value.value = email
        _credentialsLogin.value.value = userCredentials.login
        _credentialsPassword.value.value = userCredentials.password
    }

    private fun onDialogConfirmClick() {
        TODO("Not yet implemented")
    }

    suspend fun setPage(page: ProfilePage) {
        var email = page.user.email
        if (email.isNullOrEmpty()) email = page.userCredentials.login

        _userDisplayName.emit(
            _userDisplayName.value.copy(
                value = page.user.displayName ?: "",
                isReadOnly = !page.nameInputEditable
            )
        )
        _userEmail.emit(
            _userEmail.value.copy(
                value = email,
                isReadOnly = !page.emailInputEditable
            )
        )
        _credentialsLogin.emit(
            _credentialsLogin.value.copy(
                value = page.userCredentials.login,
                isReadOnly = !page.loginInputEditable
            )
        )
        _credentialsPassword.emit(
            _credentialsPassword.value.copy(
                value = page.userCredentials.password
            )
        )
        _credentialsPasswordConfirmation.emit(
            _credentialsPasswordConfirmation.value.copy(
                value = page.passwordConfirm ?: ""
            )
        )
        _errorMessage.emit(page.errorMessage ?: "")

        _user.emit(page.user)
    }

    /**
     * Attempts to save any changes.
     */
    suspend fun validateForm(resources: Resources): Boolean {
        val viewState: ProfileViewState = this

        val userEmailState = viewState.userEmail
        val credentialsLoginState = viewState.credentialsLogin
        val credentialsPasswordState = viewState.credentialsPassword
        val credentialsPasswordConfirmationState = viewState.credentialsPasswordConfirmation

        val userEmail = userEmailState.value
        val credentialsLogin = credentialsLoginState.value
        val credentialsPassword = credentialsPasswordState.value
        val credentialsPasswordConfirmation = credentialsPasswordConfirmationState.value

        // Reset errors.
        _userDisplayName.emit(_userDisplayName.value.copy(isError = false))
        _userEmail.emit(_userEmail.value.copy(isError = false))
        _credentialsLogin.emit(_credentialsLogin.value.copy(isError = false))
        _credentialsPassword.emit(_credentialsPassword.value.copy(isError = false))
        _credentialsPasswordConfirmation.emit(_credentialsPasswordConfirmation.value.copy(isError = false))
        _errorMessage.emit("")

        // Store values at the time of the submission attempt.
        val emailValue = userEmail.value
        val loginValue = credentialsLogin.value
        val passwordValue = credentialsPassword.value
        val confirmPasswordValue = credentialsPasswordConfirmation.value

        val validator = LoginValidator()

        // Check for a valid email address.
        when (validator.validateEmail(emailValue)) {
            LoginValidator.ERROR_REQUIRED -> {
                notifyError(_userEmail, resources.getString(R.string.error_field_required))
                return false
            }
            LoginValidator.ERROR_LENGTH,
            LoginValidator.ERROR_INVALID -> {
                notifyError(_userEmail, resources.getString(R.string.error_invalid_email))
                return false
            }
        }

        // Check for a valid login name.
        when (validator.validateUsername(loginValue)) {
            LoginValidator.ERROR_REQUIRED -> {
                notifyError(_credentialsLogin, resources.getString(R.string.error_field_required))
                return false
            }
            LoginValidator.ERROR_LENGTH,
            LoginValidator.ERROR_INVALID -> {
                notifyError(_credentialsLogin, resources.getString(R.string.error_invalid_login))
                return false
            }
        }

        // Check for a valid password, if the user entered one.
        when (validator.validatePassword(passwordValue)) {
            LoginValidator.ERROR_REQUIRED -> {
                notifyError(
                    _credentialsPassword,
                    resources.getString(R.string.error_field_required)
                )
                return false
            }
            LoginValidator.ERROR_LENGTH,
            LoginValidator.ERROR_INVALID -> {
                notifyError(
                    _credentialsPassword,
                    resources.getString(R.string.error_invalid_password)
                )
                return false
            }
        }

        when (validator.validatePassword(passwordValue, confirmPasswordValue)) {
            LoginValidator.ERROR_REQUIRED -> {
                notifyError(
                    _credentialsPasswordConfirmation,
                    resources.getString(R.string.error_field_required)
                )
                return false
            }
            LoginValidator.ERROR_LENGTH,
            LoginValidator.ERROR_INVALID -> {
                notifyError(
                    _credentialsPasswordConfirmation,
                    resources.getString(R.string.error_invalid_password)
                )
                return false
            }
            LoginValidator.ERROR_CONFIRM -> {
                notifyError(
                    _credentialsPasswordConfirmation,
                    resources.getString(R.string.error_match_password)
                )
                return false
            }
        }

        return true
    }

    private suspend fun notifyError(state: MutableStateFlow<TextFieldViewState>, message: String) {
        state.emit(state.value.copy(isError = true))
        _errorMessage.emit(message)
    }
}