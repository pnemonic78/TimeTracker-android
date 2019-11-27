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
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.navigation.fragment.findNavController
import com.tikalk.worktracker.R
import com.tikalk.worktracker.app.TrackerFragment
import com.tikalk.worktracker.auth.model.BasicCredentials
import com.tikalk.worktracker.auth.model.UserCredentials
import com.tikalk.worktracker.model.User
import com.tikalk.worktracker.net.InternetFragment
import com.tikalk.worktracker.net.TimeTrackerServiceProvider
import com.tikalk.worktracker.time.formatSystemDate
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_profile.*
import okhttp3.Response
import timber.log.Timber

/**
 * User's profile screen.
 */
class ProfileFragment : InternetFragment {

    constructor() : super()

    constructor(args: Bundle) : super(args)

    var listener: OnProfileListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val caller = this.caller
        if (caller != null) {
            if (caller is OnProfileListener) {
                this.listener = caller
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setTitle(R.string.profile)
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = preferences.user
        val credentials = preferences.userCredentials
        var emailValue = user.email
        if (emailValue.isNullOrBlank()) {
            emailValue = credentials.login
        }

        nameInput.setText(user.displayName)
        emailInput.setText(emailValue)
        loginInput.setText(credentials.login)
        passwordInput.setText(credentials.password)
        confirmPasswordInput.setText("")

        actionSave.setOnClickListener { attemptSave() }
    }

    @MainThread
    fun run() {
        Timber.v("run")
    }

    override fun onStart() {
        super.onStart()
        run()
    }

    /**
     * Attempts to save any changes.
     */
    fun attemptSave() {
        if (!actionSave.isEnabled) {
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
            actionSave.isEnabled = false

            preferences.userCredentials = UserCredentials(emailValue, passwordValue)

            val service = TimeTrackerServiceProvider.providePlain(context, preferences)

            val today = formatSystemDate()
            service.login(emailValue, passwordValue, today)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    showProgress(false)
                    actionSave.isEnabled = true

                    if (isValidResponse(response)) {
                        val body = response.body()!!
                        val errorMessage = getResponseError(body)
                        if (errorMessage.isNullOrEmpty()) {
                            notifyProfileSuccess(user)
                        } else {
                            emailInput.error = errorMessage
                            notifyProfileFailure(user, errorMessage)
                        }
                    } else {
                        passwordInput.requestFocus()
                        authenticate(emailValue, response.raw())
                    }
                }, { err ->
                    Timber.e(err, "Error signing in: ${err.message}")
                    showProgress(false)
                    actionSave.isEnabled = true
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

    private fun notifyProfileSuccess(user: User) {
        val fragment: ProfileFragment = this
        listener?.onProfileSuccess(fragment, user)
    }

    private fun notifyProfileFailure(user: User, reason: String) {
        val fragment: ProfileFragment = this
        listener?.onProfileFailure(fragment, user, reason)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        notifyProfileFailure(user, "onCancel")
    }

    /**
     * Listener for profile callbacks.
     */
    interface OnProfileListener {
        /**
         * Profile update was successful.
         * @param fragment the login fragment.
         * @param email the user's email that was used.
         */
        fun onProfileSuccess(fragment: ProfileFragment, user: User)

        /**
         * Profile update failed.
         * @param fragment the login fragment.
         * @param email the user's email that was used.
         * @param reason the failure reason.
         */
        fun onProfileFailure(fragment: ProfileFragment, user: User, reason: String)
    }

    companion object {
        const val EXTRA_CALLER = TrackerFragment.EXTRA_CALLER
    }
}