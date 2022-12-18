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

package com.tikalk.worktracker.user

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.tikalk.app.isNavDestination
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.LoginFragment
import com.tikalk.worktracker.auth.LoginValidator
import com.tikalk.worktracker.auth.model.UserCredentials
import com.tikalk.worktracker.auth.model.set
import com.tikalk.worktracker.data.remote.ProfilePageParser
import com.tikalk.worktracker.data.remote.ProfilePageSaver
import com.tikalk.worktracker.databinding.FragmentProfileBinding
import com.tikalk.worktracker.model.ProfilePage
import com.tikalk.worktracker.model.User
import com.tikalk.worktracker.model.set
import com.tikalk.worktracker.net.InternetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * User's profile screen.
 */
class ProfileFragment : InternetDialogFragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val profileViewModule by activityViewModels<ProfileViewModel>()
    private var userData = MutableStateFlow<User?>(null)
    private var userCredentialsData = MutableStateFlow<UserCredentials?>(null)
    private var nameInputEditable = false
    private var emailInputEditable = false
    private var loginInputEditable = false

    @Transient
    private var password2 = ""
    private var errorMessage: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showsDialog = true
        isCancelable = true

        userData.value = preferences.user
        userCredentialsData.value = preferences.userCredentials
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setTitle(R.string.profile_title)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.actionSave.setOnClickListener { attemptSave() }

        lifecycleScope.launch {
            userData.collect { user ->
                bindForm(user, userCredentialsData.value)
            }
        }
        lifecycleScope.launch {
            userCredentialsData.collect { userCredentials ->
                bindForm(userData.value, userCredentials)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @MainThread
    fun run() {
        Timber.i("run first=$firstRun")
        lifecycleScope.launch {
            try {
                dataSource.profilePage(firstRun)
                    .flowOn(Dispatchers.IO)
                    .collect { page ->
                        processPage(page)
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error loading page: ${e.message}")
                handleError(e)
            }
        }
    }

    private fun processPage(page: ProfilePage) {
        preferences.user = page.user
        userData.value = page.user
        userCredentialsData.value = page.userCredentials
        nameInputEditable = page.nameInputEditable
        emailInputEditable = page.emailInputEditable
        loginInputEditable = page.loginInputEditable
        password2 = page.passwordConfirm ?: ""
        errorMessage = page.errorMessage ?: ""
    }

    @MainThread
    private fun bindForm(
        user: User? = User.EMPTY,
        credentials: UserCredentials? = UserCredentials.EMPTY
    ) {
        var emailValue = user?.email
        if (emailValue.isNullOrBlank()) {
            emailValue = credentials?.login
        }

        binding.nameInput.setText(user?.displayName)
        binding.emailInput.setText(emailValue)
        binding.loginInput.setText(credentials?.login)
        binding.passwordInput.setText(credentials?.password)
        binding.confirmPasswordInput.setText(password2)
        setErrorLabel(errorMessage)

        binding.nameInput.isEnabled = nameInputEditable
        binding.emailInput.isEnabled = emailInputEditable
        binding.loginInput.isEnabled = loginInputEditable
    }

    /**
     * Attempts to save any changes.
     */
    private fun attemptSave() {
        if (!binding.actionSave.isEnabled) {
            return
        }

        // Reset errors.
        binding.nameInput.error = null
        binding.emailInput.error = null
        binding.loginInput.error = null
        binding.passwordInput.error = null
        binding.confirmPasswordInput.error = null
        binding.errorLabel.text = ""

        // Store values at the time of the submission attempt.
        val nameValue = binding.nameInput.text.toString()
        val emailValue = binding.emailInput.text.toString()
        val loginValue = binding.loginInput.text.toString()
        val passwordValue = binding.passwordInput.text.toString()
        val confirmPasswordValue = binding.confirmPasswordInput.text.toString()

        val validator = LoginValidator()
        var cancel = false
        var focusView: View? = null

        // Check for a valid name, if the user entered one.
        if (nameValue.isEmpty()) {
            binding.nameInput.error = getString(R.string.error_field_required)
            if (focusView == null) focusView = binding.nameInput
            cancel = true
        }

        // Check for a valid email address.
        when (validator.validateEmail(emailValue)) {
            LoginValidator.ERROR_REQUIRED -> {
                binding.emailInput.error = getString(R.string.error_field_required)
                if (focusView == null) focusView = binding.emailInput
                cancel = true
            }
            LoginValidator.ERROR_LENGTH,
            LoginValidator.ERROR_INVALID -> {
                binding.emailInput.error = getString(R.string.error_invalid_email)
                if (focusView == null) focusView = binding.emailInput
                cancel = true
            }
        }

        // Check for a valid login name.
        when (validator.validateUsername(loginValue)) {
            LoginValidator.ERROR_REQUIRED -> {
                binding.loginInput.error = getString(R.string.error_field_required)
                if (focusView == null) focusView = binding.loginInput
                cancel = true
            }
            LoginValidator.ERROR_LENGTH,
            LoginValidator.ERROR_INVALID -> {
                binding.loginInput.error = getString(R.string.error_invalid_login)
                if (focusView == null) focusView = binding.loginInput
                cancel = true
            }
        }

        // Check for a valid password, if the user entered one.
        when (validator.validatePassword(passwordValue)) {
            LoginValidator.ERROR_REQUIRED -> {
                binding.passwordInput.error = getString(R.string.error_field_required)
                if (focusView == null) focusView = binding.passwordInput
                cancel = true
            }
            LoginValidator.ERROR_LENGTH,
            LoginValidator.ERROR_INVALID -> {
                binding.passwordInput.error = getString(R.string.error_invalid_password)
                if (focusView == null) focusView = binding.passwordInput
                cancel = true
            }
        }

        when (validator.validatePassword(passwordValue, confirmPasswordValue)) {
            LoginValidator.ERROR_REQUIRED -> {
                binding.confirmPasswordInput.error = getString(R.string.error_field_required)
                if (focusView == null) focusView = binding.confirmPasswordInput
                cancel = true
            }
            LoginValidator.ERROR_LENGTH,
            LoginValidator.ERROR_INVALID -> {
                binding.confirmPasswordInput.error = getString(R.string.error_invalid_password)
                if (focusView == null) focusView = binding.confirmPasswordInput
                cancel = true
            }
            LoginValidator.ERROR_CONFIRM -> {
                binding.confirmPasswordInput.error = getString(R.string.error_match_password)
                if (focusView == null) focusView = binding.confirmPasswordInput
                cancel = true
            }
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView?.requestFocus()
        } else {
            showProgress(true)
            binding.actionSave.isEnabled = false

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val response = service.editProfile(
                        nameValue,
                        loginValue,
                        passwordValue,
                        confirmPasswordValue,
                        emailValue
                    )
                    lifecycleScope.launch(Dispatchers.Main) {
                        binding.actionSave.isEnabled = true
                        showProgress(false)

                        if (isValidResponse(response)) {
                            val html = response.body()!!
                            processEdit(
                                html,
                                loginValue,
                                emailValue,
                                nameValue,
                                passwordValue,
                                confirmPasswordValue
                            )
                        } else {
                            authenticate(true)
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error updating profile: ${e.message}")
                    handleErrorMain(e)
                    binding.actionSave.isEnabled = true
                }
            }
        }
    }

    override fun authenticate(submit: Boolean) {
        val navController = findNavController()
        Timber.i("authenticate submit=$submit currentDestination=${navController.currentDestination?.label}")
        if (!isNavDestination(R.id.loginFragment)) {
            Bundle().apply {
                putBoolean(LoginFragment.EXTRA_SUBMIT, submit)
                navController.navigate(R.id.action_profile_to_login, this)
            }
        }
    }

    private fun processPage(html: String): ProfilePage {
        val page = ProfilePageParser().parse(html)
        processPage(page)
        return page
    }

    private suspend fun notifyProfileSuccess(user: User) {
        dismissAllowingStateLoss()
        profileViewModule.onProfileSuccess(user)
    }

    private suspend fun notifyProfileFailure(user: User, reason: String) {
        profileViewModule.onProfileFailure(user, reason)
    }

    private suspend fun processEdit(
        html: String,
        loginValue: String,
        emailValue: String,
        nameValue: String,
        passwordValue: String,
        confirmPasswordValue: String
    ) {
        val user = userData.value ?: User(loginValue, emailValue, nameValue)
        val userCredentials =
            userCredentialsData.value ?: UserCredentials(loginValue, passwordValue)
        val page = processPage(html)
        val errorMessage = page.errorMessage ?: ""
        setErrorLabel(errorMessage)

        if (errorMessage.isEmpty()) {
            userCredentials.password = passwordValue
            if (page.user.isEmpty()) {
                page.user.set(user)
            }
            if (page.userCredentials.isEmpty()) {
                page.userCredentials.set(userCredentials)
            }
            ProfilePageSaver(preferences).save(page)
            password2 = confirmPasswordValue

            notifyProfileSuccess(user)
        } else {
            notifyProfileFailure(user, errorMessage)
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        userData.value?.let { user ->
            lifecycleScope.launch { notifyProfileFailure(user, REASON_CANCEL) }
        }
    }

    private fun setErrorLabel(text: CharSequence) {
        binding.errorLabel.text = text
        binding.errorLabel.visibility = if (text.isBlank()) View.GONE else View.VISIBLE
    }

    companion object {
        const val REASON_CANCEL = "onCancel"
    }
}