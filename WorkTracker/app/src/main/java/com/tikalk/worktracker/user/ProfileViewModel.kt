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
import com.tikalk.worktracker.model.ProfilePage
import com.tikalk.worktracker.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

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
    private var _error = MutableStateFlow<ProfileError?>(null)
    override val error: Flow<ProfileError?> = _error
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
        _error.emit(ProfileError.General(page.errorMessage ?: ""))
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
        _error.emit(null)

        // Store values at the time of the submission attempt.
        val emailValue = userEmail.value
        val loginValue = credentialsLogin.value
        val passwordValue = credentialsPassword.value
        val confirmPasswordValue = credentialsPasswordConfirmation.value

        val validator = LoginValidator()

        // Check for a valid email address.
        when (validator.validateEmail(emailValue)) {
            LoginValidator.ERROR_REQUIRED -> {
                _error.emit(ProfileError.Email(resources.getString(R.string.error_field_required)))
                return false
            }

            LoginValidator.ERROR_LENGTH,
            LoginValidator.ERROR_INVALID -> {
                _error.emit(ProfileError.Email(resources.getString(R.string.error_invalid_email)))
                return false
            }
        }

        // Check for a valid login name.
        when (validator.validateUsername(loginValue)) {
            LoginValidator.ERROR_REQUIRED -> {
                _error.emit(ProfileError.Login(resources.getString(R.string.error_field_required)))
                return false
            }

            LoginValidator.ERROR_LENGTH,
            LoginValidator.ERROR_INVALID -> {
                _error.emit(ProfileError.Login(resources.getString(R.string.error_invalid_login)))
                return false
            }
        }

        // Check for a valid password, if the user entered one.
        when (validator.validatePassword(passwordValue)) {
            LoginValidator.ERROR_REQUIRED -> {
                _error.emit(ProfileError.Password(resources.getString(R.string.error_field_required)))
                return false
            }

            LoginValidator.ERROR_LENGTH,
            LoginValidator.ERROR_INVALID -> {
                _error.emit(ProfileError.Password(resources.getString(R.string.error_invalid_password)))
                return false
            }
        }

        when (validator.validatePassword(passwordValue, confirmPasswordValue)) {
            LoginValidator.ERROR_REQUIRED -> {
                _error.emit(ProfileError.PasswordConfirmation(resources.getString(R.string.error_field_required)))
                return false
            }

            LoginValidator.ERROR_LENGTH,
            LoginValidator.ERROR_INVALID -> {
                _error.emit(ProfileError.PasswordConfirmation(resources.getString(R.string.error_invalid_password)))
                return false
            }

            LoginValidator.ERROR_CONFIRM -> {
                _error.emit(ProfileError.PasswordConfirmation(resources.getString(R.string.error_match_password)))
                return false
            }
        }

        return true
    }

    suspend fun editProfile(
        nameValue: String,
        emailValue: String,
        loginValue: String,
        passwordValue: String,
        passwordConfirm: String
    ) {
        notifyLoading(true)
        val user = User(username = loginValue, email = emailValue, displayName = nameValue)
        val userCredentials = UserCredentials(loginValue, passwordValue)
        val page = ProfilePage(
            user = user,
            userCredentials = userCredentials,
            nameInputEditable = false,
            emailInputEditable = false,
            loginInputEditable = false,
            passwordConfirm = passwordConfirm,
            errorMessage = null
        )
        services.dataSource.editProfile(page)
            .flowOn(Dispatchers.IO)
            .collect { pageWithError ->
                val errorMessage = pageWithError.errorMessage
                if (errorMessage.isNullOrEmpty()) {
                    _error.emit(null)
                    notifyProfileSuccess(pageWithError.user)
                } else {
                    _error.emit(ProfileError.General(errorMessage))
                    notifyProfileFailure(user, errorMessage)
                }
                notifyLoading(false)
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

    fun profilePage(refresh: Boolean): Flow<ProfilePage> {
        return services.dataSource.profilePage(refresh)
    }
}