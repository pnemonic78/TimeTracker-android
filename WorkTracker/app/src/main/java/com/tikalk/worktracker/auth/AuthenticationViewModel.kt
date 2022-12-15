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

import com.tikalk.worktracker.app.TrackerViewModel
import com.tikalk.worktracker.data.TimeTrackerRepository
import com.tikalk.worktracker.db.TrackerDatabase
import com.tikalk.worktracker.net.TimeTrackerService
import com.tikalk.worktracker.preference.TimeTrackerPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class AuthenticationViewModel @Inject constructor(
    preferences: TimeTrackerPrefs,
    db: TrackerDatabase,
    service: TimeTrackerService,
    dataSource: TimeTrackerRepository
) : TrackerViewModel(preferences, db, service, dataSource) {

    private val _login = MutableStateFlow(LoginData(""))
    val login: Flow<LoginData> = _login

    /**
     * Data for login callbacks.
     */
    data class LoginData(val login: String, val reason: String? = null)

    /**
     * Login was successful.
     * @param login the user's login that was used.
     */
    suspend fun onLoginSuccess(login: String) {
        notifyLoginSuccess(login)
    }

    /**
     * Login failed.
     * @param login the user's login that was used.
     * @param reason the failure reason.
     */
    suspend fun onLoginFailure(login: String, reason: String) {
        notifyLoginFailure(login, reason)
    }

    private suspend fun notifyLoginSuccess(login: String) {
        _login.emit(LoginData(login))
    }

    private suspend fun notifyLoginFailure(login: String, reason: String) {
        _login.emit(LoginData(login, reason))
    }
}