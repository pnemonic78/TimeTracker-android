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

package com.tikalk.preference

import android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import timber.log.Timber

abstract class TikalPreferenceFragment : PreferenceFragmentCompat() {

    protected fun validateIntent(key: String) {
        validateIntent(findPreference<Preference>(key))
    }

    protected fun validateIntent(preference: Preference?) {
        if (preference == null) {
            return
        }
        val intent = preference.intent ?: return
        val context = preference.context
        val pm = context.packageManager
        val info = pm.resolveActivity(intent, MATCH_DEFAULT_ONLY)
        if (info != null) {
            preference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Timber.e(e, "Error launching intent: %s", intent)
                }

                true
            }
        } else {
            preference.intent = null
        }
    }
}