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

package com.tikalk.worktracker.report

import com.tikalk.worktracker.app.TrackerServices
import com.tikalk.worktracker.app.TrackerViewModel
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.time.TimeRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

@HiltViewModel
class ReportViewModel @Inject constructor(
    services: TrackerServices
) : TrackerViewModel(services) {

    private val _onEdit = MutableStateFlow<TimeRecord?>(null)
    val onEdit: Flow<TimeRecord?> = _onEdit

    fun maybeEditRecord(scope: CoroutineScope, record: TimeRecord) {
        scope.launch(Dispatchers.IO) {
            notifyLoading(true)

            val projectName = record.project.name
            val taskName = record.task.name
            val date = record.date
            val start = record.start
            val duration = record.duration

            // fetch the page from the server.
            var hasCandidate = false
            services.dataSource.timeListPage(record.date, true)
                .onCompletion {
                    notifyLoading(false)
                }
                .collect { page ->
                    if (hasCandidate) return@collect
                    val candidate = page.records.firstOrNull { pageRecord ->
                        (pageRecord.id != TikalEntity.ID_NONE) &&
                            (pageRecord.project.name == projectName) &&
                            (pageRecord.task.name == taskName) &&
                            (pageRecord.date == date) &&
                            (pageRecord.start == start) &&
                            (pageRecord.duration == duration)
                    }
                    if (candidate != null) {
                        hasCandidate = true
                        _onEdit.emit(candidate)
                    }
                }
        }
    }

    fun clearEvents() {
        _onEdit.value = null
    }

    override fun onCleared() {
        super.onCleared()
        clearEvents()
    }
}