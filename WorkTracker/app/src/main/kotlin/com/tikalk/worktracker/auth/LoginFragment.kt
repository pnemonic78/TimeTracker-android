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
import com.tikalk.worktracker.auth.model.BasicCredentials
import com.tikalk.worktracker.auth.model.UserCredentials
import com.tikalk.worktracker.databinding.FragmentLoginBinding
import com.tikalk.worktracker.net.InternetDialogFragment
import com.tikalk.worktracker.time.formatSystemDate
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.Response
import timber.log.Timber

/**
 * A login screen that offers login via login/password.
 */
class LoginFragment : InternetDialogFragment {

    constructor() : super()

    constructor(args: Bundle) : super(args)

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showsDialog = true
        isCancelable = false

        delegate.authenticationViewModel.basicRealm.observe(this, { (realm, _, reason) ->
            if (reason == null) {
                Timber.i("basic realm success for \"$realm\"")
                attemptLogin()
            } else {
                Timber.e("basic realm failure for \"$realm\": $reason")
            }
        })
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setTitle(R.string.activity_login)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loginInput.setText(preferences.userCredentials.login)

        val passwordImeActionId = resources.getInteger(R.integer.password_imeActionId)
        binding.passwordInput.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == passwordImeActionId || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })
        binding.passwordInput.setText(preferences.userCredentials.password)

        binding.actionSignIn.setOnClickListener { attemptLogin() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @MainThread
    fun run() {
        Timber.i("run")
        val args = this.arguments ?: return

        if (args.containsKey(EXTRA_LOGIN)) {
            binding.loginInput.setText(args.getString(EXTRA_LOGIN))
            binding.passwordInput.text = null

            if (args.containsKey(EXTRA_PASSWORD)) {
                binding.passwordInput.setText(args.getString(EXTRA_PASSWORD))
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
    private fun attemptLogin() {
        if (!binding.actionSignIn.isEnabled) {
            return
        }

        // Reset errors.
        binding.loginInput.error = null
        binding.passwordInput.error = null

        // Store values at the time of the login attempt.
        val loginValue = binding.loginInput.text.toString()
        val passwordValue = binding.passwordInput.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid login name.
        if (loginValue.isEmpty()) {
            binding.loginInput.error = getString(R.string.error_field_required)
            if (focusView == null) focusView = binding.loginInput
            cancel = true
        } else if (!isLoginValid(loginValue)) {
            binding.loginInput.error = getString(R.string.error_invalid_login)
            if (focusView == null) focusView = binding.loginInput
            cancel = true
        }

        // Check for a valid password, if the user entered one.
        if (passwordValue.isEmpty()) {
            binding.passwordInput.error = getString(R.string.error_field_required)
            if (focusView == null) focusView = binding.passwordInput
            cancel = true
        } else if (!isPasswordValid(passwordValue)) {
            binding.passwordInput.error = getString(R.string.error_invalid_password)
            if (focusView == null) focusView = binding.passwordInput
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView?.requestFocus()
        } else {
            binding.actionSignIn.isEnabled = false

            preferences.userCredentials = UserCredentials(loginValue, passwordValue)

            val today = formatSystemDate()
            delegate.service.login(loginValue, passwordValue, today)
                .subscribeOn(Schedulers.io())
                .doOnSubscribe { showProgressMain(true) }
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate { showProgress(false) }
                .subscribe({ response ->
                    binding.actionSignIn.isEnabled = true

                    if (isValidResponse(response)) {
                        val html = response.body()!!
                        val errorMessage = getResponseError(html)
                        if (errorMessage.isNullOrEmpty()) {
                            notifyLoginSuccess(loginValue)
                        } else {
                            binding.loginInput.error = errorMessage
                            notifyLoginFailure(loginValue, errorMessage)
                        }
                    } else {
                        binding.passwordInput.requestFocus()
                        authenticate(loginValue, response.raw())
                    }
                }, { err ->
                    Timber.e(err, "Error signing in: ${err.message}")
                    handleError(err)
                    binding.actionSignIn.isEnabled = true
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
            if (challenge.scheme == BasicCredentials.SCHEME) {
                authenticateBasicRealm(login, challenge.realm ?: "")
                return true
            }
        }
        return false
    }

    private fun authenticateBasicRealm(username: String, realm: String) {
        val navController = findNavController()
        Timber.i("authenticateBasicRealm realm=$realm currentDestination=${navController.currentDestination?.label}")
        val indexAt = username.indexOf('@')
        val userClean = if (indexAt < 0) username else username.substring(0, indexAt)

        if (!isNavDestination(R.id.basicRealmFragment)) {
            val args = Bundle().apply {
                putString(BasicRealmFragment.EXTRA_REALM, realm)
                putString(BasicRealmFragment.EXTRA_USER, userClean)
            }
            navController.navigate(R.id.action_basicRealmLogin, args)
        }
    }

    private fun notifyLoginSuccess(login: String) {
        dismissAllowingStateLoss()
        delegate.onLoginSuccess(login)
    }

    private fun notifyLoginFailure(login: String, reason: String) {
        delegate.onLoginFailure(login, reason)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        notifyLoginFailure("", "onCancel")
    }

    companion object {
        const val EXTRA_LOGIN = "login"
        const val EXTRA_PASSWORD = "password"
        const val EXTRA_SUBMIT = "submit"
    }
}