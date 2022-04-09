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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.annotation.MainThread
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.model.BasicCredentials
import com.tikalk.worktracker.databinding.FragmentBasicRealmBinding
import com.tikalk.worktracker.net.InternetDialogFragment
import timber.log.Timber

/**
 * An authentication screen for Basic Realm via email/password.
 */
class BasicRealmFragment : InternetDialogFragment() {

    private var _binding: FragmentBasicRealmBinding? = null
    private val binding get() = _binding!!

    var realmName = "(realm)"
        set(value) {
            field = value
            _binding?.realmTitle?.text = getString(R.string.authentication_basic_realm, value)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showsDialog = true
        isCancelable = false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setTitle(R.string.activity_basic_realm)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBasicRealmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val credentials = preferences.basicCredentials
        realmName = credentials.realm
        val username = credentials.username
        val password = credentials.password

        binding.usernameInput.setText(username)

        val passwordImeActionId = resources.getInteger(R.integer.password_imeActionId)
        binding.passwordInput.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == passwordImeActionId || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })
        binding.passwordInput.setText(password)

        binding.actionAuthenticate.setOnClickListener { attemptLogin() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @MainThread
    fun run() {
        Timber.i("run")
        val args = this.arguments ?: return

        if (args.containsKey(EXTRA_REALM)) {
            realmName = args.getString(EXTRA_REALM) ?: "?"
            binding.realmTitle.text = getString(R.string.authentication_basic_realm, realmName)
        }
        if (args.containsKey(EXTRA_USER)) {
            val username = args.getString(EXTRA_USER)
            binding.usernameInput.setText(username)

            val credentials = preferences.basicCredentials

            when {
                args.containsKey(EXTRA_PASSWORD) -> binding.passwordInput.setText(
                    args.getString(EXTRA_PASSWORD)
                )
                username == credentials.username -> binding.passwordInput.setText(credentials.password)
                else -> binding.passwordInput.text = null
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
    private fun attemptLogin() {
        if (!binding.actionAuthenticate.isEnabled) {
            return
        }

        // Reset errors.
        binding.usernameInput.error = null
        binding.passwordInput.error = null

        // Store values at the time of the login attempt.
        val username = binding.usernameInput.text.toString()
        val password = binding.passwordInput.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid name.
        if (username.isEmpty()) {
            binding.usernameInput.error = getString(R.string.error_field_required)
            if (focusView == null) focusView = binding.usernameInput
            cancel = true
        } else if (!isUsernameValid(username)) {
            binding.usernameInput.error = getString(R.string.error_invalid_email)
            if (focusView == null) focusView = binding.usernameInput
            cancel = true
        }

        // Check for a valid password, if the user entered one.
        if (password.isEmpty()) {
            binding.passwordInput.error = getString(R.string.error_field_required)
            if (focusView == null) focusView = binding.passwordInput
            cancel = true
        } else if (!isPasswordValid(password)) {
            binding.passwordInput.error = getString(R.string.error_invalid_password)
            if (focusView == null) focusView = binding.passwordInput
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView?.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            binding.actionAuthenticate.isEnabled = false

            preferences.basicCredentials = BasicCredentials(realmName, username, password)
            notifyLoginSuccess(realmName, username)
        }
    }

    private fun isUsernameValid(username: String): Boolean {
        return username.length > 1
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.trim().length > 4
    }

    private fun notifyLoginSuccess(realmName: String, username: String) {
        dismissAllowingStateLoss()
        delegate.onBasicRealmSuccess(realmName, username)
    }

    private fun notifyLoginFailure(realmName: String, username: String, reason: String) {
        delegate.onBasicRealmFailure(realmName, username, reason)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        notifyLoginFailure(realmName, "", REASON_CANCEL)
    }

    override fun authenticate(submit: Boolean) = Unit

    companion object {
        const val EXTRA_REALM = "realm"
        const val EXTRA_USER = "user"
        const val EXTRA_PASSWORD = "password"
        const val EXTRA_SUBMIT = "submit"

        const val REASON_CANCEL = "onCancel"
    }
}