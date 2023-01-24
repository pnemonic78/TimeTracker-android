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
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.tikalk.app.isNavDestination
import com.tikalk.compose.TextFieldViewState
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.LoginFragment
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * User's profile screen.
 */
class ProfileFragment : InternetDialogFragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val profileViewModule by viewModels<ProfileViewModel>()

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
        val viewState: ProfileViewState = profileViewModule

        binding.nameInput.doAfterTextChanged {
            val state = viewState.userDisplayName
            val userDisplayName = state.value
            userDisplayName.value = it.toString()
        }
        binding.emailInput.doAfterTextChanged {
            val state = viewState.userEmail
            val userEmail = state.value
            userEmail.value = it.toString()
        }
        binding.loginInput.doAfterTextChanged {
            val state = viewState.credentialsLogin
            val credentialsLogin = state.value
            credentialsLogin.value = it.toString()
        }
        binding.passwordInput.doAfterTextChanged {
            val state = viewState.credentialsPassword
            val credentialsPassword = state.value
            credentialsPassword.value = it.toString()
        }
        binding.confirmPasswordInput.doAfterTextChanged {
            val state = viewState.credentialsPasswordConfirmation
            val credentialsPasswordConfirmation = state.value
            credentialsPasswordConfirmation.value = it.toString()
        }

        binding.actionSave.setOnClickListener { lifecycleScope.launch { attemptSave() } }

        lifecycleScope.launch {
            profileViewModule.userDisplayName.collect {
                bindUserName(it)
            }
        }
        lifecycleScope.launch {
            profileViewModule.userEmail.collect {
                bindUserEmail(it)
            }
        }
        lifecycleScope.launch {
            profileViewModule.credentialsLogin.collect {
                bindCredentialsLogin(it)
            }
        }
        lifecycleScope.launch {
            profileViewModule.credentialsPassword.collect {
                bindCredentialsPassword(it)
            }
        }
        lifecycleScope.launch {
            profileViewModule.credentialsPasswordConfirmation.collect {
                bindCredentialsPasswordConfirmation(it)
            }
        }
        lifecycleScope.launch {
            profileViewModule.errorMessage.collect {
                setErrorLabel(it)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @MainThread
    override fun run() {
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

    private suspend fun processPage(page: ProfilePage) {
        preferences.user = page.user
        profileViewModule.setPage(page)
    }

    @MainThread
    private fun bindUserName(state: TextFieldViewState) {
        binding.nameInput.setText(state.value)
        binding.nameInput.setSelection(state.value.length)
        binding.nameInput.isEnabled = !state.isReadOnly
        binding.nameInput.error = if (state.isError) "" else null
    }

    @MainThread
    private fun bindUserEmail(state: TextFieldViewState) {
        binding.emailInput.setText(state.value)
        binding.emailInput.setSelection(state.value.length)
        binding.emailInput.isEnabled = !state.isReadOnly
        binding.emailInput.error = if (state.isError) "" else null
    }

    @MainThread
    private fun bindCredentialsLogin(state: TextFieldViewState) {
        binding.loginInput.setText(state.value)
        binding.loginInput.setSelection(state.value.length)
        binding.loginInput.isEnabled = !state.isReadOnly
        binding.loginInput.error = if (state.isError) "" else null
    }

    @MainThread
    private fun bindCredentialsPassword(state: TextFieldViewState) {
        binding.passwordInput.setText(state.value)
        binding.passwordInput.setSelection(state.value.length)
        binding.passwordInput.isEnabled = !state.isReadOnly
        binding.passwordInput.error = if (state.isError) "" else null
    }

    @MainThread
    private fun bindCredentialsPasswordConfirmation(state: TextFieldViewState) {
        binding.confirmPasswordInput.setText(state.value)
        binding.confirmPasswordInput.setSelection(state.value.length)
        binding.confirmPasswordInput.isEnabled = !state.isReadOnly
        binding.confirmPasswordInput.error = if (state.isError) "" else null
    }

    /**
     * Attempts to save any changes.
     */
    @MainThread
    private suspend fun attemptSave() {
        if (!binding.actionSave.isEnabled) {
            return
        }
        if (!profileViewModule.validateForm(resources)) return

        val viewState: ProfileViewState = profileViewModule

        val userDisplayNameState = viewState.userDisplayName
        val userEmailState = viewState.userEmail
        val credentialsLoginState = viewState.credentialsLogin
        val credentialsPasswordState = viewState.credentialsPassword
        val credentialsPasswordConfirmationState = viewState.credentialsPasswordConfirmation

        val userDisplayName = userDisplayNameState.value
        val userEmail = userEmailState.value
        val credentialsLogin = credentialsLoginState.value
        val credentialsPassword = credentialsPasswordState.value
        val credentialsPasswordConfirmation = credentialsPasswordConfirmationState.value

        // Store values at the time of the submission attempt.
        val nameValue = userDisplayName.value
        val emailValue = userEmail.value
        val loginValue = credentialsLogin.value
        val passwordValue = credentialsPassword.value
        val confirmPasswordValue = credentialsPasswordConfirmation.value

        showProgress(true)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = service.editProfile(
                    name = nameValue,
                    email = emailValue,
                    login = loginValue,
                    password1 = passwordValue,
                    password2 = confirmPasswordValue
                )
                lifecycleScope.launch(Dispatchers.Main) main@{
                    if (isValidResponse(response)) {
                        val html = response.body() ?: return@main
                        processEdit(
                            html,
                            loginValue,
                            emailValue,
                            nameValue,
                            passwordValue
                        )
                        showProgress(false)
                    } else {
                        showProgress(false)
                        authenticate(true)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating profile: ${e.message}")
                handleErrorMain(e)
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

    private suspend fun processPage(html: String): ProfilePage {
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
        passwordValue: String
    ) {
        val user = User(loginValue, emailValue, nameValue)
        val userCredentials = UserCredentials(loginValue, passwordValue)
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

            notifyProfileSuccess(user)
        } else {
            notifyProfileFailure(user, errorMessage)
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        lifecycleScope.launch {
            val user = profileViewModule.user.value
            notifyProfileFailure(user, REASON_CANCEL)
        }
    }

    private fun setErrorLabel(text: CharSequence) {
        binding.errorLabel.text = text
        binding.errorLabel.isVisible = text.isNotBlank()
    }

    companion object {
        const val REASON_CANCEL = "onCancel"
    }
}