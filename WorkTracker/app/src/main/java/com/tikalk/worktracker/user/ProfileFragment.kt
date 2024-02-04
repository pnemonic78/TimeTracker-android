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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.tikalk.app.isDestination
import com.tikalk.compose.TikalTheme
import com.tikalk.core.databinding.FragmentComposeBinding
import com.tikalk.widget.PaddedBox
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.LoginFragment
import com.tikalk.worktracker.lang.isFalse
import com.tikalk.worktracker.lang.isTrue
import com.tikalk.worktracker.net.InternetFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * User's profile screen.
 */
class ProfileFragment : InternetFragment() {

    private var _binding: FragmentComposeBinding? = null
    private val binding get() = _binding!!

    override val viewModel by activityViewModels<ProfileViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentComposeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewState: ProfileViewState = viewModel

        binding.composeView.setContent {
            TikalTheme {
                PaddedBox {
                    ProfileForm(viewState = viewState)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.onDialogConfirmClick.collect {
                if (it) {
                    viewModel.clearEvents()
                    attemptSave()
                }
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
                services.dataSource.profilePage(firstRun)
                    .flowOn(Dispatchers.IO)
                    .collect { page ->
                        viewModel.processPage(page)
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error loading page: ${e.message}")
                handleError(e)
            }
        }
    }

    /**
     * Attempts to save any changes.
     */
    @MainThread
    private suspend fun attemptSave() {
        if (!viewModel.validateForm(resources)) return

        val viewState: ProfileViewState = viewModel

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
                val response = services.service.editProfile(
                    name = nameValue,
                    email = emailValue,
                    login = loginValue,
                    password1 = passwordValue,
                    password2 = confirmPasswordValue
                )
                lifecycleScope.launch(Dispatchers.Main) main@{
                    if (isValidResponse(response)) {
                        val html = response.body() ?: return@main
                        viewModel.processEdit(
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
                showProgressMain(false)
                handleErrorMain(e)
            }
        }
    }

    override fun authenticate(submit: Boolean) {
        val navController = findNavController()
        Timber.i("authenticate submit=$submit currentDestination=${navController.currentDestination?.label}")
        if (!navController.isDestination(R.id.loginFragment)) {
            Bundle().apply {
                putBoolean(LoginFragment.EXTRA_SUBMIT, submit)
                navController.navigate(R.id.action_profile_to_login, this)
            }
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        if (view?.isVisible.isTrue) {
            menuInflater.inflate(R.menu.profile, menu)
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (view?.isVisible.isFalse) {
            return false
        }
        when (menuItem.itemId) {
            R.id.menu_submit -> {
                lifecycleScope.launch { attemptSave() }
                return true
            }
        }
        return super.onMenuItemSelected(menuItem)
    }
}