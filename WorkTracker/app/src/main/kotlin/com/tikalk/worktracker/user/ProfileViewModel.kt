/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2020, Tikal Knowledge, Ltd.
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

import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.tikalk.worktracker.app.TrackerViewModel
import com.tikalk.worktracker.model.User

class ProfileViewModel: TrackerViewModel() {

    private val profileUpdateData = MutableLiveData<ProfileData>()
    val profileUpdate: LiveData<ProfileData> = profileUpdateData

    /**
     * Data for profile callbacks.
     */
    data class ProfileData(val user: User, val reason: String? = null)

    /**
     * Profile update was successful.
     * @param user the updated user.
     */
    fun onProfileSuccess(user: User) {
        notifyProfileSuccess(user)
    }

    /**
     * Profile update failed.
     * @param user the current user.
     * @param reason the failure reason.
     */
    fun onProfileFailure(user: User, reason: String) {
        notifyProfileFailure(user, reason)
    }

    private fun notifyProfileSuccess(user: User) {
        profileUpdateData.postValue(ProfileData(user))
    }

    private fun notifyProfileFailure(user: User, reason: String) {
        profileUpdateData.postValue(ProfileData(user, reason))
    }

    companion object {
        fun get(fragment: Fragment) = get(fragment.requireActivity())

        fun get(owner: ViewModelStoreOwner) = ViewModelProvider(owner).get(ProfileViewModel::class.java)
    }
}