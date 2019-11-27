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
import com.tikalk.html.selectByName
import com.tikalk.html.value
import com.tikalk.worktracker.R
import com.tikalk.worktracker.app.TrackerFragment
import com.tikalk.worktracker.auth.model.UserCredentials
import com.tikalk.worktracker.model.User
import com.tikalk.worktracker.net.InternetFragment
import com.tikalk.worktracker.net.TimeTrackerServiceProvider
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_profile.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.FormElement
import timber.log.Timber

/**
 * User's profile screen.
 */
class ProfileFragment : InternetFragment() {

    var listener: OnProfileListener? = null
    private var userCredentials = UserCredentials.EMPTY
    private var nameInputEditable = true
    private var emailInputEditable = true
    private var loginInputEditable = true
    @Transient
    private var password2 = ""
    private var errorMessage: String = ""

    override fun onAttach(context: Context) {
        super.onAttach(context)
        userCredentials = preferences.userCredentials
    }

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

        bindForm()
        actionSave.setOnClickListener { attemptSave() }
    }

    @MainThread
    fun run() {
        Timber.v("run")
        fetchPage()
    }

    override fun onStart() {
        super.onStart()
        run()
    }

    private fun bindForm() {
        val user = this.user
        val credentials = this.userCredentials

        var emailValue = user.email
        if (emailValue.isNullOrBlank()) {
            emailValue = credentials.login
        }

        nameInput.setText(user.displayName)
        emailInput.setText(emailValue)
        loginInput.setText(credentials.login)
        passwordInput.setText(credentials.password)
        confirmPasswordInput.setText(password2)
        errorLabel.text = errorMessage

        nameInput.isEnabled = nameInputEditable
        emailInput.isEnabled = emailInputEditable
        loginInput.isEnabled = loginInputEditable
    }

    /**
     * Attempts to save any changes.
     */
    private fun attemptSave() {
        if (!actionSave.isEnabled) {
            return
        }

        val context: Context = requireContext()

        // Reset errors.
        nameInput.error = null
        emailInput.error = null
        loginInput.error = null
        passwordInput.error = null
        confirmPasswordInput.error = null
        errorLabel.text = ""

        // Store values at the time of the submission attempt.
        val nameValue = nameInput.text.toString()
        val emailValue = emailInput.text.toString()
        val loginValue = loginInput.text.toString()
        val passwordValue = passwordInput.text.toString()
        val confirmPasswordValue = confirmPasswordInput.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid name, if the user entered one.
        if (nameValue.isEmpty()) {
            nameInput.error = getString(R.string.error_field_required)
            focusView = nameInput
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

        // Check for a valid login name.
        if (loginValue.isEmpty()) {
            loginInput.error = getString(R.string.error_field_required)
            focusView = loginInput
            cancel = true
        } else if (!isLoginValid(loginValue)) {
            loginInput.error = getString(R.string.error_invalid_login)
            focusView = loginInput
            cancel = true
        }

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

        if (confirmPasswordValue.isEmpty()) {
            confirmPasswordInput.error = getString(R.string.error_field_required)
            focusView = confirmPasswordInput
            cancel = true
        } else if (!isPasswordValid(confirmPasswordValue)) {
            confirmPasswordInput.error = getString(R.string.error_invalid_password)
            focusView = confirmPasswordInput
            cancel = true
        } else if (passwordValue != confirmPasswordValue) {
            confirmPasswordInput.error = getString(R.string.error_match_password)
            focusView = confirmPasswordInput
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

            val service = TimeTrackerServiceProvider.providePlain(context, preferences)

            service.editProfile(nameValue, loginValue, passwordValue, confirmPasswordValue, emailValue)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    showProgress(false)
                    actionSave.isEnabled = true

                    if (isValidResponse(response)) {
                        val html = response.body()!!
                        processPage(html, true)

                        if (errorMessage.isEmpty()) {
                            userCredentials.password = passwordValue
                            password2 = confirmPasswordValue
                            preferences.user = user
                            preferences.userCredentials = userCredentials

                            notifyProfileSuccess(user)
                        } else {
                            notifyProfileFailure(user, errorMessage)
                        }
                    } else {
                        authenticate(true)
                    }
                }, { err ->
                    Timber.e(err, "Error updating profile: ${err.message}")
                    showProgress(false)
                    actionSave.isEnabled = true
                })
                .addTo(disposables)
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isLoginValid(login: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(login).matches()
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.trim().length > 4
    }

    private fun authenticate(submit: Boolean = false) {
        Timber.v("authenticate submit=$submit")
        val args = Bundle()
        requireFragmentManager().putFragment(args, LoginFragment.EXTRA_CALLER, this)
        args.putBoolean(LoginFragment.EXTRA_SUBMIT, submit)
        findNavController().navigate(R.id.action_profile_to_login, args)
    }

    private fun fetchPage(progress: Boolean = true) {
        Timber.d("fetchPage")
        // Show a progress spinner, and kick off a background task to perform the user login attempt.
        if (progress) showProgress(true)

        // Fetch from remote server.
        val service = TimeTrackerServiceProvider.providePlain(context, preferences)

        service.fetchProfile()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                if (isValidResponse(response)) {
                    val html = response.body()!!
                    processPage(html, progress)
                } else {
                    authenticate(true)
                }
            }, { err ->
                Timber.e(err, "Error fetching page: ${err.message}")
                if (progress) showProgressMain(false)
            })
            .addTo(disposables)
    }

    private fun processPage(html: String, progress: Boolean = true) {
        populateForm(html)
        bindForm()
        if (progress) showProgress(false)
    }

    private fun populateForm(html: String) {
        val doc: Document = Jsoup.parse(html)
        populateForm(doc)
    }

    private fun populateForm(doc: Document) {
        errorMessage = findError(doc)?.trim() ?: ""

        val form = doc.selectFirst("form[name='profileForm']") as FormElement? ?: return
        populateForm(doc, form)
    }

    private fun populateForm(doc: Document, form: FormElement) {
        val nameInputElement = form.selectByName("name") ?: return
        val emailInputElement = form.selectByName("email") ?: return
        val loginInputElement = form.selectByName("login") ?: return
        val passwordInputElement = form.selectByName("password1") ?: return
        val confirmPasswordInputElement = form.selectByName("password2") ?: return

        nameInputEditable = !nameInputElement.hasAttr("readonly")
        emailInputEditable = !emailInputElement.hasAttr("readonly")
        loginInputEditable = !loginInputElement.hasAttr("readonly")

        user.displayName = nameInputElement.value()
        user.email = emailInputElement.value()
        user.username = loginInputElement.value()

        userCredentials.login = user.username
        val password1 = passwordInputElement.value()
        if (password1.isNotBlank()) {
            userCredentials.password = password1
        }

        password2 = confirmPasswordInputElement.text()
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
         * @param user the updated user.
         */
        fun onProfileSuccess(fragment: ProfileFragment, user: User)

        /**
         * Profile update failed.
         * @param fragment the login fragment.
         * @param user the current user.
         * @param reason the failure reason.
         */
        fun onProfileFailure(fragment: ProfileFragment, user: User, reason: String)
    }

    companion object {
        const val EXTRA_CALLER = TrackerFragment.EXTRA_CALLER
    }
}