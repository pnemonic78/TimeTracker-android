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
import android.content.DialogInterface
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.annotation.MainThread
import androidx.navigation.fragment.findNavController
import com.tikalk.app.isNavDestination
import com.tikalk.worktracker.R
import com.tikalk.worktracker.app.TrackerFragment
import com.tikalk.worktracker.auth.model.BasicCredentials
import com.tikalk.worktracker.auth.model.UserCredentials
import com.tikalk.worktracker.net.InternetFragment
import com.tikalk.worktracker.time.formatSystemDate
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_login.*
import okhttp3.Response
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A login screen that offers login via login/password.
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
        } else {
            val activity = this.activity
            if (activity != null) {
                if (activity is OnLoginListener) {
                    addListener(activity)
                }
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

        loginInput.setText(preferences.userCredentials.login)

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
        Timber.i("run")
        val args = this.arguments ?: return

        if (args.containsKey(EXTRA_LOGIN)) {
            loginInput.setText(args.getString(EXTRA_LOGIN))
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
     * If there are form errors (invalid login, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    fun attemptLogin() {
        if (!actionSignIn.isEnabled) {
            return
        }

        // Reset errors.
        loginInput.error = null
        passwordInput.error = null

        // Store values at the time of the login attempt.
        val loginValue = loginInput.text.toString()
        val passwordValue = passwordInput.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid login name.
        if (loginValue.isEmpty()) {
            loginInput.error = getString(R.string.error_field_required)
            if (focusView == null) focusView = loginInput
            cancel = true
        } else if (!isLoginValid(loginValue)) {
            loginInput.error = getString(R.string.error_invalid_login)
            if (focusView == null) focusView = loginInput
            cancel = true
        }

        // Check for a valid password, if the user entered one.
        if (passwordValue.isEmpty()) {
            passwordInput.error = getString(R.string.error_field_required)
            if (focusView == null) focusView = passwordInput
            cancel = true
        } else if (!isPasswordValid(passwordValue)) {
            passwordInput.error = getString(R.string.error_invalid_password)
            if (focusView == null) focusView = passwordInput
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView?.requestFocus()
        } else {
            actionSignIn.isEnabled = false

            preferences.userCredentials = UserCredentials(loginValue, passwordValue)

            val today = formatSystemDate()
            service.login(loginValue, passwordValue, today)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { showProgress(true) }
                .doAfterTerminate { showProgress(false) }
                .subscribe({ response ->
                    actionSignIn.isEnabled = true

                    if (isValidResponse(response)) {
                        val html = response.body()!!
                        val errorMessage = getResponseError(html)
                        if (errorMessage.isNullOrEmpty()) {
                            notifyLoginSuccess(loginValue)
                        } else {
                            loginInput.error = errorMessage
                            notifyLoginFailure(loginValue, errorMessage)
                        }
                    } else {
                        passwordInput.requestFocus()
                        authenticate(loginValue, response.raw())
                    }
                }, { err ->
                    Timber.e(err, "Error signing in: ${err.message}")
                    handleError(err)
                    actionSignIn.isEnabled = true
                })
                .addTo(disposables)
        }
    }

    private fun isLoginValid(login: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(login).matches()
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.trim().length > 4
    }

    override fun authenticate(submit: Boolean) {
        authenticateBasicRealm("", "")
    }

    private fun authenticate(login: String, response: Response): Boolean {
        val challenges = response.challenges()
        for (challenge in challenges) {
            if (challenge.scheme() == BasicCredentials.SCHEME) {
                authenticateBasicRealm(login, challenge.realm())
                return true
            }
        }
        return false
    }

    private fun authenticateBasicRealm(username: String, realm: String) {
        Timber.i("authenticateBasicRealm realm=$realm currentDestination=${findNavController().currentDestination?.label}")
        val indexAt = username.indexOf('@')
        val userClean = if (indexAt < 0) username else username.substring(0, indexAt)

        if (!isNavDestination(R.id.basicRealmFragment)) {
            val args = Bundle().apply {
                putString(BasicRealmFragment.EXTRA_REALM, realm)
                putString(BasicRealmFragment.EXTRA_USER, userClean)
            }
            requireFragmentManager().putFragment(args, BasicRealmFragment.EXTRA_CALLER, this)
            findNavController().navigate(R.id.action_basicRealmLogin, args)
        }
    }

    override fun onBasicRealmSuccess(fragment: BasicRealmFragment, realm: String, username: String) {
        Timber.i("basic realm success for \"$realm\"")
        fragment.dismissAllowingStateLoss()
        attemptLogin()
    }

    override fun onBasicRealmFailure(fragment: BasicRealmFragment, realm: String, username: String, reason: String) {
        Timber.e("basic realm failure for \"$realm\": $reason")
    }

    private fun notifyLoginSuccess(login: String) {
        val fragment: LoginFragment = this
        for (listener in listeners) {
            listener.onLoginSuccess(fragment, login)
        }
    }

    private fun notifyLoginFailure(login: String, reason: String) {
        val fragment: LoginFragment = this
        for (listener in listeners) {
            listener.onLoginFailure(fragment, login, reason)
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
         * @param login the user's login that was used.
         */
        fun onLoginSuccess(fragment: LoginFragment, login: String)

        /**
         * Login failed.
         * @param fragment the login fragment.
         * @param login the user's login that was used.
         * @param reason the failure reason.
         */
        fun onLoginFailure(fragment: LoginFragment, login: String, reason: String)
    }

    companion object {
        const val EXTRA_CALLER = TrackerFragment.EXTRA_CALLER
        const val EXTRA_LOGIN = "login"
        const val EXTRA_PASSWORD = "password"
        const val EXTRA_SUBMIT = "submit"
    }
}