/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2022, Tikal Knowledge, Ltd.
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

import androidx.annotation.MainThread
import androidx.lifecycle.viewModelScope
import com.tikalk.model.TikalResult
import com.tikalk.worktracker.app.TrackerServices
import com.tikalk.worktracker.app.TrackerViewModel
import com.tikalk.worktracker.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class UsersViewModel @Inject constructor(
    services: TrackerServices
) : TrackerViewModel(services),
    UsersViewState {

    private val _users = MutableStateFlow<TikalResult<List<User>>>(TikalResult.Loading())
    override val users: Flow<TikalResult<List<User>>> = _users

    @MainThread
    suspend fun fetchUsers(firstRun: Boolean) {
        try {
            _users.emit(TikalResult.Loading())
            notifyLoading(true)
            services.dataSource.usersPage(firstRun)
                .flowOn(Dispatchers.IO)
                .collect { page ->
                    _users.emit(TikalResult.Success(page.users))
                    notifyLoading(false)
                }
        } catch (e: Exception) {
            Timber.e(e, "Error loading page: ${e.message}")
            _users.emit(TikalResult.Error(e))
            notifyError(e)
        }
    }

    private val _userSelectedPosition = MutableStateFlow(0)
    override val userSelectedPosition: Flow<Int> = _userSelectedPosition

    override fun onScrollIndex(index: String, position: Int) {
        viewModelScope.launch {
            _userSelectedPosition.emit(position)
        }
    }
}