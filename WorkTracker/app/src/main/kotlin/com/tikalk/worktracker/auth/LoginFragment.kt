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
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.tikalk.view.showAnimated
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.model.BasicCredentials
import com.tikalk.worktracker.auth.model.UserCredentials
import com.tikalk.worktracker.net.InternetFragment
import com.tikalk.worktracker.net.TimeTrackerServiceFactory
import com.tikalk.worktracker.preference.TimeTrackerPrefs
import com.tikalk.worktracker.time.formatSystemDate
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_login.*
import okhttp3.Response
import timber.log.Timber

/**
 * A login screen that offers login via email/password.
 */
class LoginFragment : InternetFragment() {

    private lateinit var prefs: TimeTrackerPrefs

    private var passwordImeActionId = 109

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private var authTask: Disposable? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        prefs = TimeTrackerPrefs(context)
    }

    override fun onDestroy() {
        super.onDestroy()
        authTask?.dispose()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        email.setText(prefs.userCredentials.login)

        passwordImeActionId = resources.getInteger(R.integer.password_imeActionId)
        password.setOnEditorActionListener(TextView.OnEditorActionListener { textView, id, keyEvent ->
            if (id == passwordImeActionId || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })
        password.setText(prefs.userCredentials.password)

        emailSignIn.setOnClickListener { attemptLogin() }
    }

    override fun handleIntent(intent: Intent) {
        super.handleIntent(intent)
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
    fun attemptLogin() {
        if (!emailSignIn.isEnabled) {
            return
        }

        val context: Context = this.context ?: return

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
            emailSignIn.isEnabled = false

            prefs.userCredentials = UserCredentials(emailValue, passwordValue)

            val authToken = prefs.basicCredentials.authToken()
            val service = TimeTrackerServiceFactory.createPlain(context, authToken)

            val today = formatSystemDate()
            authTask = service.login(emailValue, passwordValue, today)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    showProgress(false)
                    emailSignIn.isEnabled = true

                    if (isValidResponse(response)) {
                        val body = response.body()!!
                        val errorMessage = getResponseError(body)
                        if (errorMessage.isNullOrEmpty()) {
                            //FIXME call OnLoginListener.onSuccess()
                            activity?.setResult(AppCompatActivity.RESULT_OK)
                            activity?.finish()
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
                    emailSignIn.isEnabled = true
                })
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.trim().length > 4
    }

    private fun showProgress(show: Boolean) {
        loginForm.showAnimated(show.not())
        (activity as LoginActivity).showProgress(show)
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

    companion object {
        const val REQUEST_AUTHENTICATE = 0xAECA

        const val EXTRA_EMAIL = "email"
        const val EXTRA_PASSWORD = "password"
        const val EXTRA_SUBMIT = "submit"
    }
}