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
import androidx.preference.Preference
import com.tikalk.preference.TikalPreferenceFragment
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.model.BasicCredentials
import com.tikalk.worktracker.auth.model.UserCredentials
import com.tikalk.worktracker.model.User
import com.tikalk.worktracker.net.TimeTrackerServiceFactory
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TimeSettingsFragment : TikalPreferenceFragment() {

    @Inject
    lateinit var preferences: TimeTrackerPrefs

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val context = requireContext()

        val clearUser = findPreference<Preference>("clear_user")
        clearUser?.setOnPreferenceClickListener { preference ->
            onClearUserClicked(preference)
            true
        }

        val clearAppData = findPreference<Preference>("clear_data")
        clearAppData?.setOnPreferenceClickListener { preference ->
            onClearDataClicked(preference)
            true
        }

        val version = findPreference<Preference>("about.version")
        if (version != null) {
            try {
                version.summary =
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (e: PackageManager.NameNotFoundException) {
                // Never should happen with our own package!
            }

            validateIntent(version)
        }
        validateIntent("about.issue")
    }

    private fun onClearUserClicked(preference: Preference) {
        preference.isEnabled = false
        deleteUser()
        preference.isEnabled = true
    }

    private fun deleteUser() {
        preferences.user = User.EMPTY
        preferences.userCredentials = UserCredentials.EMPTY
        preferences.basicCredentials = BasicCredentials.EMPTY
        TimeTrackerServiceFactory.clearCookies()

        val context = this.context ?: return
        Toast.makeText(context, context.getString(R.string.pref_logout_toast), Toast.LENGTH_LONG)
            .show()
    }

    private fun onClearDataClicked(preference: Preference) {
        preference.isEnabled = false
        deleteAppData()
        preference.isEnabled = true
    }

    /**
     * Clear the application data.
     */
    private fun deleteAppData() {
        val context = this.context ?: return
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        am.clearApplicationUserData()

        Toast.makeText(
            context,
            context.getString(R.string.clear_user_data_toast),
            Toast.LENGTH_LONG
        ).show()
    }
}