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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.model.BasicCredentials
import com.tikalk.worktracker.auth.model.UserCredentials
import com.tikalk.worktracker.net.InternetActivity
import com.tikalk.worktracker.net.TimeTrackerServiceFactory
import com.tikalk.worktracker.preference.TimeTrackerPrefs
import com.tikalk.worktracker.time.formatSystemDate
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.android.synthetic.main.progress.*
import okhttp3.Response
import timber.log.Timber

/**
 * A login screen that offers login via email/password.
 */
class LoginActivity : InternetActivity() {

    companion object {
        private const val REQUEST_AUTHENTICATE = 1

        const val EXTRA_EMAIL = "email"
        const val EXTRA_PASSWORD = "password"
        const val EXTRA_SUBMIT = "submit"
    }

    private val context: Context = this

    private lateinit var prefs: TimeTrackerPrefs
    private lateinit var loginFragment: LoginFragment

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private var authTask: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = TimeTrackerPrefs(this)

        // Set up the login form.
        setContentView(R.layout.activity_login)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        loginFragment = supportFragmentManager.findFragmentById(R.id.login_form) as LoginFragment

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        handleIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        authTask?.dispose()
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

        if (extras.containsKey(EXTRA_EMAIL)) {
            email.setText(extras.getString(EXTRA_EMAIL))
            password.text = null

            if (extras.containsKey(EXTRA_PASSWORD)) {
                password.setText(extras.getString(EXTRA_PASSWORD))
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
        if (!emailSignInButton.isEnabled) {
            return
        }

        // Reset errors.
        email.error = null
        password.error = null

        // Store values at the time of the login attempt.
        val emailValue = email.text.toString()
        val passwordValue = password.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid password, if the user entered one.
        if (passwordValue.isEmpty()) {
            password.error = getString(R.string.error_field_required)
            focusView = password
            cancel = true
        } else if (!isPasswordValid(passwordValue)) {
            password.error = getString(R.string.error_invalid_password)
            focusView = password
            cancel = true
        }

        // Check for a valid email address.
        if (emailValue.isEmpty()) {
            email.error = getString(R.string.error_field_required)
            focusView = email
            cancel = true
        } else if (!isEmailValid(emailValue)) {
            email.error = getString(R.string.error_invalid_email)
            focusView = email
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
            emailSignInButton.isEnabled = false

            prefs.userCredentials = UserCredentials(emailValue, passwordValue)

            val authToken = prefs.basicCredentials.authToken()
            val service = TimeTrackerServiceFactory.createPlain(this, authToken)

            val today = formatSystemDate()
            authTask = service.login(emailValue, passwordValue, today)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    showProgress(false)
                    emailSignInButton.isEnabled = true

                    if (isValidResponse(response)) {
                        val body = response.body()!!
                        val errorMessage = getResponseError(body)
                        if (errorMessage.isNullOrEmpty()) {
                            setResult(RESULT_OK)
                            finish()
                        } else {
                            email.error = errorMessage
                        }
                    } else {
                        password.requestFocus()
                        authenticate(emailValue, response.raw())
                    }
                }, { err ->
                    Timber.e(err, "Error signing in: ${err.message}")
                    showProgress(false)
                    emailSignInButton.isEnabled = true
                })
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.trim().length > 4
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private fun showProgress(show: Boolean) {
        val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        login_form.visibility = if (show) View.GONE else View.VISIBLE
        login_form.animate().setDuration(shortAnimTime).alpha(
            (if (show) 0 else 1).toFloat()).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                login_form.visibility = if (show) View.GONE else View.VISIBLE
            }
        })

        progress.visibility = if (show) View.VISIBLE else View.GONE
        progress.animate().setDuration(shortAnimTime).alpha(
            (if (show) 1 else 0).toFloat()).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                progress.visibility = if (show) View.VISIBLE else View.GONE
            }
        })
    }

    private fun authenticate(email: String, response: Response): Boolean {
        val challenges = response.challenges()
        for (challenge in challenges) {
            if (challenge.scheme() == BasicCredentials.SCHEME) {
                val indexAt = email.indexOf('@')
                val username = if (indexAt < 0) email else email.substring(0, indexAt)
                val intent = Intent(context, BasicRealmActivity::class.java)
                intent.putExtra(BasicRealmActivity.EXTRA_REALM, challenge.realm())
                intent.putExtra(BasicRealmActivity.EXTRA_USER, username)
                startActivityForResult(intent, REQUEST_AUTHENTICATE)
                return true
            }
        }
        return false
    }
}

