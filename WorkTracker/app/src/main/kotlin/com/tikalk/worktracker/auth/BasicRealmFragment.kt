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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.model.BasicCredentials
import com.tikalk.worktracker.net.InternetFragment
import com.tikalk.worktracker.preference.TimeTrackerPrefs
import kotlinx.android.synthetic.main.fragment_basic_realm.*

/**
 * An authentication screen for Basic Realm via email/password.
 */
class BasicRealmFragment : InternetFragment() {

    private lateinit var prefs: TimeTrackerPrefs
    private var realmName = "(realm)"

    override fun onAttach(context: Context) {
        super.onAttach(context)
        prefs = TimeTrackerPrefs(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_basic_realm, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val credentials = prefs.basicCredentials
        realmName = credentials.realm
        val username = credentials.username
        val password = credentials.password

        realmTitle.text = getString(R.string.authentication_basic_realm, realmName)

        usernameInput.setText(username)

        val passwordImeActionId = resources.getInteger(R.integer.password_imeActionId)
        passwordInput.setOnEditorActionListener(TextView.OnEditorActionListener { textView, id, keyEvent ->
            if (id == passwordImeActionId || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })
        passwordInput.setText(password)

        actionAuthenticate.setOnClickListener { attemptLogin() }
    }

    override fun handleIntent(intent: Intent, savedInstanceState: Bundle?) {
        super.handleIntent(intent, savedInstanceState)
        val extras = intent.extras ?: return

        if (extras.containsKey(EXTRA_REALM)) {
            realmName = extras.getString(EXTRA_REALM) ?: "?"
            realmTitle.text = getString(R.string.authentication_basic_realm, realmName)
        }
        if (extras.containsKey(EXTRA_USER)) {
            val username = extras.getString(EXTRA_USER)
            usernameInput.setText(username)

            val credentials = prefs.basicCredentials

            when {
                extras.containsKey(EXTRA_PASSWORD) -> passwordInput.setText(extras.getString(EXTRA_PASSWORD))
                username == credentials.username -> passwordInput.setText(credentials.password)
                else -> passwordInput.text = null
            }
        }
        if (extras.containsKey(EXTRA_SUBMIT) && extras.getBoolean(EXTRA_SUBMIT)) {
            attemptLogin()
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    fun attemptLogin() {
        if (!actionAuthenticate.isEnabled) {
            return
        }

        // Reset errors.
        usernameInput.error = null
        passwordInput.error = null

        // Store values at the time of the login attempt.
        val username = usernameInput.text.toString()
        val password = passwordInput.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid name.
        if (username.isEmpty()) {
            usernameInput.error = getString(R.string.error_field_required)
            focusView = usernameInput
            cancel = true
        } else if (!isUsernameValid(username)) {
            usernameInput.error = getString(R.string.error_invalid_email)
            focusView = usernameInput
            cancel = true
        }

        // Check for a valid password, if the user entered one.
        if (password.isEmpty()) {
            passwordInput.error = getString(R.string.error_field_required)
            focusView = passwordInput
            cancel = true
        } else if (!isPasswordValid(password)) {
            passwordInput.error = getString(R.string.error_invalid_password)
            focusView = passwordInput
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView!!.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            actionAuthenticate.isEnabled = false

            prefs.basicCredentials = BasicCredentials(realmName, username, password)
            //FIXME call OnLoginListener.onSuccess()
            activity?.setResult(AppCompatActivity.RESULT_OK)
            activity?.finish()
        }
    }

    private fun isUsernameValid(username: String): Boolean {
        return username.length > 1
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.trim().length > 4
    }

    companion object {
        const val EXTRA_REALM = "realm"
        const val EXTRA_USER = "user"
        const val EXTRA_PASSWORD = "password"
        const val EXTRA_SUBMIT = "submit"
    }
}