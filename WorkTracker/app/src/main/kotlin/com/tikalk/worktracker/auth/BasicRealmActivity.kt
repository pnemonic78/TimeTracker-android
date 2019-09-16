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

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.tikalk.view.showAnimated
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.model.BasicCredentials
import com.tikalk.worktracker.net.InternetActivity
import com.tikalk.worktracker.preference.TimeTrackerPrefs

/**
 * An authentication screen for Basic Realm via email/password.
 */
class BasicRealmActivity : InternetActivity() {

    companion object {
        const val EXTRA_REALM = "realm"
        const val EXTRA_USER = "user"
        const val EXTRA_PASSWORD = "password"
        const val EXTRA_SUBMIT = "submit"
    }

    // UI references.
    private lateinit var realmView: TextView
    private lateinit var usernameView: EditText
    private lateinit var passwordView: EditText
    private lateinit var progressView: View
    private lateinit var loginFormView: View
    private lateinit var authButton: Button

    private lateinit var prefs: TimeTrackerPrefs
    private var realmName = "(realm)"
    private var passwordImeActionId = 109

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = TimeTrackerPrefs(this)

        val credentials = prefs.basicCredentials
        realmName = credentials.realm
        val userName = credentials.username
        val password = credentials.password

        // Set up the login form.
        setContentView(R.layout.activity_basic_realm)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        realmView = findViewById(R.id.realm_title)
        realmView.text = getString(R.string.authentication_basic_realm, realmName)

        usernameView = findViewById(R.id.username)
        usernameView.setText(userName)

        passwordView = findViewById(R.id.password)
        passwordImeActionId = resources.getInteger(R.integer.password_imeActionId)
        passwordView.setOnEditorActionListener(TextView.OnEditorActionListener { textView, id, keyEvent ->
            if (id == passwordImeActionId || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })
        passwordView.setText(password)

        authButton = findViewById(R.id.realm_auth_button)
        authButton.setOnClickListener { attemptLogin() }

        loginFormView = findViewById(R.id.auth_form)
        progressView = findViewById(R.id.progress)

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        handleIntent(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.clear()
        menuInflater.inflate(R.menu.authenticate, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.menu_authenticate -> attemptLogin()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleIntent(intent: Intent) {
        val extras = intent.extras ?: return

        if (extras.containsKey(EXTRA_REALM)) {
            realmName = extras.getString(EXTRA_REALM) ?: "?"
            realmView.text = getString(R.string.authentication_basic_realm, realmName)
        }
        if (extras.containsKey(EXTRA_USER)) {
            val username = extras.getString(EXTRA_USER)
            usernameView.setText(username)

            val credentials = prefs.basicCredentials

            when {
                extras.containsKey(EXTRA_PASSWORD) -> passwordView.setText(extras.getString(EXTRA_PASSWORD))
                username == credentials.username -> passwordView.setText(credentials.password)
                else -> passwordView.text = null
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
    private fun attemptLogin() {
        if (!authButton.isEnabled) {
            return
        }

        // Reset errors.
        usernameView.error = null
        passwordView.error = null

        // Store values at the time of the login attempt.
        val username = usernameView.text.toString()
        val password = passwordView.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid name.
        if (username.isEmpty()) {
            usernameView.error = getString(R.string.error_field_required)
            focusView = usernameView
            cancel = true
        } else if (!isUsernameValid(username)) {
            usernameView.error = getString(R.string.error_invalid_email)
            focusView = usernameView
            cancel = true
        }

        // Check for a valid password, if the user entered one.
        if (password.isEmpty()) {
            passwordView.error = getString(R.string.error_field_required)
            focusView = passwordView
            cancel = true
        } else if (!isPasswordValid(password)) {
            passwordView.error = getString(R.string.error_invalid_password)
            focusView = passwordView
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView!!.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true)
            authButton.isEnabled = false

            prefs.basicCredentials = BasicCredentials(realmName, username, password)
            showProgress(false)
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun isUsernameValid(username: String): Boolean {
        return username.length > 1
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.trim().length > 4
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private fun showProgress(show: Boolean) {
        loginFormView.showAnimated(show.not())
        progressView.showAnimated(show)
    }
}

