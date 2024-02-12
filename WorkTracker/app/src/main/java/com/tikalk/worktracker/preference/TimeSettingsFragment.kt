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

package com.tikalk.worktracker.preference

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import com.tikalk.preference.TikalPreferenceFragment
import com.tikalk.worktracker.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class TimeSettingsFragment : TikalPreferenceFragment() {

    private val viewModel by viewModels<SettingsViewModel>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val context = requireContext()

        findPreference<Preference>("logout")?.apply {
            setOnPreferenceClickListener { preference ->
                onLogoutClicked(preference)
                true
            }
        }

        findPreference<Preference>("clear_data")?.apply {
            setOnPreferenceClickListener { preference ->
                onClearDataClicked(preference)
                true
            }
        }

        findPreference<Preference>("about.version")?.apply {
            try {
                summary = context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (e: PackageManager.NameNotFoundException) {
                // Never should happen with our own package!
            }

            validateIntent(this)
        }

        validateIntent("about.issue")
    }

    private fun onLogoutClicked(preference: Preference) {
        val context: Context = preference.context
        preference.isEnabled = false
        logout()
        preference.isEnabled = true
        Toast.makeText(context, context.getString(R.string.pref_logout_toast), Toast.LENGTH_LONG)
            .show()
    }

    private fun logout() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                viewModel.logout()
            } catch (e: Exception) {
                Timber.e(e, "Error signing out: ${e.message}")
            }
        }
    }

    private fun onClearDataClicked(preference: Preference) {
        preference.isEnabled = false
        deleteAppData(preference.context)
        preference.isEnabled = true
    }

    /**
     * Clear the application data.
     */
    private fun deleteAppData(context: Context) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        am.clearApplicationUserData()

        Toast.makeText(
            context,
            context.getString(R.string.clear_user_data_toast),
            Toast.LENGTH_LONG
        ).show()
    }
}