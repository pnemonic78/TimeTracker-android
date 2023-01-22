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

import com.tikalk.compose.TextFieldViewState
import com.tikalk.compose.UnitCallback
import com.tikalk.worktracker.app.TrackerServices
import com.tikalk.worktracker.app.TrackerViewModel
import com.tikalk.worktracker.auth.model.UserCredentials
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

    private val _userCredentials = MutableStateFlow(UserCredentials.EMPTY)
    val userCredentials: StateFlow<UserCredentials> = _userCredentials

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
                isReadOnly = page.nameInputEditable
            )
        )
        _userEmail.emit(
            _userEmail.value.copy(
                value = email,
                isReadOnly = page.emailInputEditable
            )
        )
        _credentialsLogin.emit(
            _credentialsLogin.value.copy(
                value = page.userCredentials.login,
                isReadOnly = page.loginInputEditable
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
        _userCredentials.emit(page.userCredentials)
    }
}