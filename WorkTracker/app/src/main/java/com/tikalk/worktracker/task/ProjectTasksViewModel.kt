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

package com.tikalk.worktracker.task

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tikalk.model.TikalResult
import com.tikalk.worktracker.app.TrackerViewModel
import com.tikalk.worktracker.data.TimeTrackerRepository
import com.tikalk.worktracker.db.TrackerDatabase
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.net.TimeTrackerService
import com.tikalk.worktracker.preference.TimeTrackerPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ProjectTasksViewModel @Inject constructor(
    preferences: TimeTrackerPrefs,
    db: TrackerDatabase,
    service: TimeTrackerService,
    dataSource: TimeTrackerRepository
) : TrackerViewModel(preferences, db, service, dataSource) {

    private val _tasksData = MutableLiveData<TikalResult<List<ProjectTask>>>(TikalResult.Loading())
    val tasksData: LiveData<TikalResult<List<ProjectTask>>> = _tasksData

    suspend fun fetchTasks(firstRun: Boolean) {
        _tasksData.postValue(TikalResult.Loading())
        try {
            dataSource.tasksPage(firstRun)
                .flowOn(Dispatchers.IO)
                .collect { page ->
                    _tasksData.postValue(TikalResult.Success(page.tasks))
                }
        } catch (e: Exception) {
            Timber.e(e, "Error loading page: ${e.message}")
            _tasksData.postValue(TikalResult.Error(e))
        }
    }

}