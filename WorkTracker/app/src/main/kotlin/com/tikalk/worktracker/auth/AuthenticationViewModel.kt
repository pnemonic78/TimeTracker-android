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

import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.tikalk.worktracker.app.TrackerViewModel

class AuthenticationViewModel : TrackerViewModel() {

    private val _login = MutableLiveData<LoginData>()
    val login: LiveData<LoginData> = _login

    private val _basicRealm = MutableLiveData<BasicRealmData>()
    val basicRealm: LiveData<BasicRealmData> = _basicRealm

    /**
     * Data for login callbacks.
     */
    data class LoginData(val login: String, val reason: String? = null)

    /**
     * Login was successful.
     * @param login the user's login that was used.
     */
    fun onLoginSuccess(login: String) {
        notifyLoginSuccess(login)
    }

    /**
     * Login failed.
     * @param login the user's login that was used.
     * @param reason the failure reason.
     */
    fun onLoginFailure(login: String, reason: String) {
        notifyLoginFailure(login, reason)
    }

    private fun notifyLoginSuccess(login: String) {
        _login.postValue(LoginData(login))
    }

    private fun notifyLoginFailure(login: String, reason: String) {
        _login.postValue(LoginData(login, reason))
    }

    /**
     * Data for basic realm login callbacks.
     */
    data class BasicRealmData(val realm: String, val username: String, val reason: String? = null)

    /**
     * Login was successful.
     * @param realm the realm name that was used.
     * @param username the user's name that was used.
     */
    fun onBasicRealmSuccess(realm: String, username: String) {
        notifyLoginSuccess(realm, username)
    }

    /**
     * Login failed.
     * @param realm the realm name that was used.
     * @param username the user's name that was used.
     * @param reason the failure reason.
     */
    fun onBasicRealmFailure(realm: String, username: String, reason: String) {
        notifyLoginFailure(realm, username, reason)
    }

    private fun notifyLoginSuccess(realmName: String, username: String) {
        _basicRealm.postValue(BasicRealmData(realmName, username))
    }

    private fun notifyLoginFailure(realmName: String, username: String, reason: String) {
        _basicRealm.postValue(BasicRealmData(realmName, username, reason))
    }

    companion object {
        fun get(fragment: Fragment) = get(fragment.requireActivity())

        fun get(owner: ViewModelStoreOwner) = ViewModelProvider(owner).get(AuthenticationViewModel::class.java)
    }
}