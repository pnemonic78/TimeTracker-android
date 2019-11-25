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

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.tikalk.app.topLevel
import com.tikalk.worktracker.R
import com.tikalk.worktracker.app.TrackerFragment
import com.tikalk.worktracker.auth.model.BasicCredentials
import com.tikalk.worktracker.auth.model.UserCredentials
import com.tikalk.worktracker.net.InternetFragment
import com.tikalk.worktracker.net.TimeTrackerServiceProvider
import com.tikalk.worktracker.time.formatSystemDate
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_login.*
import okhttp3.Response
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A login screen that offers login via email/password.
 */
class LoginFragment : InternetFragment,
    BasicRealmFragment.OnBasicRealmListener {

    constructor() : super()

    constructor(args: Bundle) : super(args)

    private val listeners: MutableList<OnLoginListener> = CopyOnWriteArrayList<OnLoginListener>()

    fun addListener(listener: OnLoginListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: OnLoginListener) {
        listeners.remove(listener)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val caller = this.caller
        if (caller != null) {
            if (caller is OnLoginListener) {
                addListener(caller)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setTitle(R.string.activity_login)
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        emailInput.setText(preferences.userCredentials.login)

        val passwordImeActionId = resources.getInteger(R.integer.password_imeActionId)
        passwordInput.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == passwordImeActionId || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })
        passwordInput.setText(preferences.userCredentials.password)

        actionSignIn.setOnClickListener { attemptLogin() }
    }

    @MainThread
    fun run() {
        Timber.v("run")
        val args = this.arguments ?: return

        if (args.containsKey(EXTRA_EMAIL)) {
            emailInput.setText(args.getString(EXTRA_EMAIL))
            passwordInput.text = null

            if (args.containsKey(EXTRA_PASSWORD)) {
                passwordInput.setText(args.getString(EXTRA_PASSWORD))
            }
        }
        if (args.containsKey(EXTRA_SUBMIT) && args.getBoolean(EXTRA_SUBMIT)) {
            attemptLogin()
        }
    }

    override fun onStart() {
        super.onStart()
        run()
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    fun attemptLogin() {
        if (!actionSignIn.isEnabled) {
            return
        }

        val context: Context = requireContext()

        // Reset errors.
        emailInput.error = null
        passwordInput.error = null

        // Store values at the time of the login attempt.
        val emailValue = emailInput.text.toString()
        val passwordValue = passwordInput.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid password, if the user entered one.
        if (passwordValue.isEmpty()) {
            passwordInput.error = getString(R.string.error_field_required)
            focusView = passwordInput
            cancel = true
        } else if (!isPasswordValid(passwordValue)) {
            passwordInput.error = getString(R.string.error_invalid_password)
            focusView = passwordInput
            cancel = true
        }

        // Check for a valid email address.
        if (emailValue.isEmpty()) {
            emailInput.error = getString(R.string.error_field_required)
            focusView = emailInput
            cancel = true
        } else if (!isEmailValid(emailValue)) {
            emailInput.error = getString(R.string.error_invalid_email)
            focusView = emailInput
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
            actionSignIn.isEnabled = false

            preferences.userCredentials = UserCredentials(emailValue, passwordValue)

            val service = TimeTrackerServiceProvider.providePlain(context, preferences)

            val today = formatSystemDate()
            service.login(emailValue, passwordValue, today)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    showProgress(false)
                    actionSignIn.isEnabled = true

                    if (isValidResponse(response)) {
                        val body = response.body()!!
                        val errorMessage = getResponseError(body)
                        if (errorMessage.isNullOrEmpty()) {
                            notifyLoginSuccess(emailValue)
                        } else {
                            emailInput.error = errorMessage
                            notifyLoginFailure(emailValue, errorMessage)
                        }
                    } else {
                        passwordInput.requestFocus()
                        authenticate(emailValue, response.raw())
                    }
                }, { err ->
                    Timber.e(err, "Error signing in: ${err.message}")
                    showProgress(false)
                    actionSignIn.isEnabled = true
                })
                .addTo(disposables)
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.trim().length > 4
    }

    private fun authenticate(email: String, response: Response): Boolean {
        val challenges = response.challenges()
        for (challenge in challenges) {
            if (challenge.scheme() == BasicCredentials.SCHEME) {
                authenticateBasicRealm(email, challenge.realm())
                return true
            }
        }
        return false
    }

    private fun authenticateBasicRealm(username: String, realm: String) {
        val indexAt = username.indexOf('@')
        val userClean = if (indexAt < 0) username else username.substring(0, indexAt)

        val args = Bundle().apply {
            putString(BasicRealmFragment.EXTRA_REALM, realm)
            putString(BasicRealmFragment.EXTRA_USER, userClean)
        }
        requireFragmentManager().putFragment(args, BasicRealmFragment.EXTRA_CALLER, this)
        findNavController().navigate(R.id.action_basicRealmLogin, args)
    }

    override fun onBasicRealmSuccess(fragment: BasicRealmFragment, realm: String, username: String) {
        Timber.i("basic realm success for \"$realm\"")
        if (fragment.isVisible) {
            findNavController().popBackStack()
        }
        attemptLogin()
    }

    override fun onBasicRealmFailure(fragment: BasicRealmFragment, realm: String, username: String, reason: String) {
        Timber.e("basic realm failure for \"$realm\": $reason")
    }

    private fun notifyLoginSuccess(email: String) {
        val fragment: LoginFragment = this
        for (listener in listeners) {
            listener.onLoginSuccess(fragment, email)
        }
    }

    private fun notifyLoginFailure(email: String, reason: String) {
        val fragment: LoginFragment = this
        for (listener in listeners) {
            listener.onLoginFailure(fragment, email, reason)
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        notifyLoginFailure("", "onCancel")
    }

    /**
     * Listener for login callbacks.
     */
    interface OnLoginListener {
        /**
         * Login was successful.
         * @param fragment the login fragment.
         * @param email the user's email that was used.
         */
        fun onLoginSuccess(fragment: LoginFragment, email: String)

        /**
         * Login failed.
         * @param fragment the login fragment.
         * @param email the user's email that was used.
         * @param reason the failure reason.
         */
        fun onLoginFailure(fragment: LoginFragment, email: String, reason: String)
    }

    companion object {
        const val EXTRA_CALLER = TrackerFragment.EXTRA_CALLER
        const val EXTRA_EMAIL = "email"
        const val EXTRA_PASSWORD = "password"
        const val EXTRA_SUBMIT = "submit"

        const val REQUEST_LOGIN = 0x109E

        @Synchronized
        fun show(fragment: Fragment, submit: Boolean = false, listener: OnLoginListener, tag: String = "login_email") {
            val topLevel = fragment.topLevel()
            val fragmentManager = topLevel.requireFragmentManager()
            if (!fragmentManager.isDestroyed) {
                val fragmentExisting = fragmentManager.findFragmentByTag(tag) as LoginFragment?
                if (fragmentExisting == null) {
                    val fragmentArguments = fragment.arguments
                    if (fragmentManager.isStateSaved) {
                        val intent = Intent(fragment.requireContext(), LoginActivity::class.java)
                        if (fragmentArguments != null) {
                            intent.putExtras(fragmentArguments)
                        }
                        intent.putExtra(EXTRA_SUBMIT, submit)
                        fragment.startActivityForResult(intent, REQUEST_LOGIN)
                    } else {
                        val args = Bundle().apply {
                            if (fragmentArguments != null) {
                                putAll(fragmentArguments)
                            }
                            putBoolean(EXTRA_SUBMIT, submit)
                        }
                        val fragmentLogin: LoginFragment = fragmentExisting ?: LoginFragment(args)
                        fragmentLogin.addListener(listener)
                        fragmentLogin.show(fragmentManager, tag)
                    }
                }
            }
        }
    }
}