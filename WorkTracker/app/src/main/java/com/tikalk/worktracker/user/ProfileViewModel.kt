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
import androidx.lifecycle.viewModelScope
import com.tikalk.compose.TextFieldViewState
import com.tikalk.compose.UnitCallback
import com.tikalk.worktracker.R
import com.tikalk.worktracker.app.TrackerServices
import com.tikalk.worktracker.app.TrackerViewModel
import com.tikalk.worktracker.auth.LoginValidator
import com.tikalk.worktracker.auth.model.UserCredentials
import com.tikalk.worktracker.data.remote.ProfilePageParser
import com.tikalk.worktracker.data.remote.ProfilePageSaver
import com.tikalk.worktracker.model.ProfilePage
import com.tikalk.worktracker.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    services: TrackerServices
) : TrackerViewModel(services),
    ProfileViewState {

    private val _profileUpdate = MutableStateFlow<ProfileData?>(null)
    val profileUpdate: StateFlow<ProfileData?> = _profileUpdate

    /**
     * Data for profile callbacks.
     */
    data class ProfileData(val user: User, val reason: String? = null)

    /**
     * Profile update was successful.
     * @param user the updated user.
     */
    private suspend fun notifyProfileSuccess(user: User) {
        _profileUpdate.emit(ProfileData(user))
    }

    /**
     * Profile update failed.
     * @param user the current user.
     * @param reason the failure reason.
     */
    private suspend fun notifyProfileFailure(user: User, reason: String) {
        _profileUpdate.emit(ProfileData(user, reason))
    }

    override val userDisplayName = MutableStateFlow(TextFieldViewState())
    override val userEmail = MutableStateFlow(TextFieldViewState())
    override val credentialsLogin = MutableStateFlow(TextFieldViewState())
    override val credentialsPassword = MutableStateFlow(TextFieldViewState())
    override val credentialsPasswordConfirmation = MutableStateFlow(TextFieldViewState())
    private var _errorMessage = MutableStateFlow("")
    override val errorMessage: StateFlow<String> = _errorMessage
    override val onConfirmClick: UnitCallback = ::onDialogConfirmClick
    override val onDismiss: UnitCallback = {}

    init {
        val user = services.preferences.user
        val userCredentials = services.preferences.userCredentials

        var email = user.email
        if (email.isNullOrEmpty()) email = userCredentials.login

        userDisplayName.value.value = user.displayName ?: ""
        userEmail.value.value = email
        credentialsLogin.value.value = userCredentials.login
        credentialsPassword.value.value = userCredentials.password
    }

    private val _onDialogConfirmClick = MutableStateFlow(false)
    val onDialogConfirmClick: StateFlow<Boolean> = _onDialogConfirmClick

    private fun onDialogConfirmClick() {
        viewModelScope.launch { _onDialogConfirmClick.emit(true) }
    }

    private suspend fun setPage(page: ProfilePage) {
        var email = page.user.email
        if (email.isNullOrEmpty()) email = page.userCredentials.login

        userDisplayName.emit(
            userDisplayName.value.copy(
                value = page.user.displayName ?: "",
                isReadOnly = !page.nameInputEditable
            )
        )
        userEmail.emit(
            userEmail.value.copy(
                value = email,
                isReadOnly = !page.emailInputEditable
            )
        )
        credentialsLogin.emit(
            credentialsLogin.value.copy(
                value = page.userCredentials.login,
                isReadOnly = !page.loginInputEditable
            )
        )
        credentialsPassword.emit(
            credentialsPassword.value.copy(
                value = page.userCredentials.password
            )
        )
        credentialsPasswordConfirmation.emit(
            credentialsPasswordConfirmation.value.copy(
                value = page.passwordConfirm ?: ""
            )
        )
        _errorMessage.emit(page.errorMessage ?: "")
    }

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
        userDisplayNameState.emit(userDisplayName.copy(isError = false))
        userEmailState.emit(userEmail.copy(isError = false))
        credentialsLoginState.emit(credentialsLogin.copy(isError = false))
        credentialsPasswordState.emit(credentialsPassword.copy(isError = false))
        credentialsPasswordConfirmationState.emit(credentialsPasswordConfirmation.copy(isError = false))
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
                notifyError(userEmailState, resources.getString(R.string.error_field_required))
                return false
            }
            LoginValidator.ERROR_LENGTH,
            LoginValidator.ERROR_INVALID -> {
                notifyError(userEmailState, resources.getString(R.string.error_invalid_email))
                return false
            }
        }

        // Check for a valid login name.
        when (validator.validateUsername(loginValue)) {
            LoginValidator.ERROR_REQUIRED -> {
                notifyError(
                    credentialsLoginState,
                    resources.getString(R.string.error_field_required)
                )
                return false
            }
            LoginValidator.ERROR_LENGTH,
            LoginValidator.ERROR_INVALID -> {
                notifyError(
                    credentialsLoginState,
                    resources.getString(R.string.error_invalid_login)
                )
                return false
            }
        }

        // Check for a valid password, if the user entered one.
        when (validator.validatePassword(passwordValue)) {
            LoginValidator.ERROR_REQUIRED -> {
                notifyError(
                    credentialsPasswordState,
                    resources.getString(R.string.error_field_required)
                )
                return false
            }
            LoginValidator.ERROR_LENGTH,
            LoginValidator.ERROR_INVALID -> {
                notifyError(
                    credentialsPasswordState,
                    resources.getString(R.string.error_invalid_password)
                )
                return false
            }
        }

        when (validator.validatePassword(passwordValue, confirmPasswordValue)) {
            LoginValidator.ERROR_REQUIRED -> {
                notifyError(
                    credentialsPasswordConfirmationState,
                    resources.getString(R.string.error_field_required)
                )
                return false
            }
            LoginValidator.ERROR_LENGTH,
            LoginValidator.ERROR_INVALID -> {
                notifyError(
                    credentialsPasswordConfirmationState,
                    resources.getString(R.string.error_invalid_password)
                )
                return false
            }
            LoginValidator.ERROR_CONFIRM -> {
                notifyError(
                    credentialsPasswordConfirmationState,
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

    suspend fun processEdit(
        html: String,
        loginValue: String,
        emailValue: String,
        nameValue: String,
        passwordValue: String
    ) {
        val user = User(username = loginValue, email = emailValue, displayName = nameValue)
        val pageWithError = parsePage(html)
        val errorMessage = pageWithError.errorMessage ?: ""
        _errorMessage.emit(errorMessage)

        if (errorMessage.isEmpty()) {
            val userCredentials = UserCredentials(loginValue, passwordValue)
            val page = ProfilePage(
                user = user,
                userCredentials = userCredentials,
                nameInputEditable = false,
                emailInputEditable = false,
                loginInputEditable = false,
                passwordConfirm = null,
                errorMessage = null
            )
            ProfilePageSaver(services.preferences).save(page)

            notifyProfileSuccess(user)
        } else {
            notifyProfileFailure(user, errorMessage)
        }
    }

    private fun parsePage(html: String): ProfilePage {
        return ProfilePageParser().parse(html)
    }

    internal suspend fun processPage(page: ProfilePage) {
        services.preferences.user = page.user
        setPage(page)
    }

    override fun onCleared() {
        super.onCleared()
        clearEvents()
    }

    fun clearEvents() {
        _onDialogConfirmClick.value = false
    }
}