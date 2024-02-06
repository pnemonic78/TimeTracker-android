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

package com.tikalk.worktracker.time

import androidx.lifecycle.viewModelScope
import com.tikalk.worktracker.app.TrackerServices
import com.tikalk.worktracker.app.TrackerViewModel
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.time.FormPage
import com.tikalk.worktracker.model.time.PuncherPage
import com.tikalk.worktracker.model.time.ReportFormPage
import com.tikalk.worktracker.model.time.TimeEditPage
import com.tikalk.worktracker.model.time.TimeListPage
import com.tikalk.worktracker.model.time.TimeRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class TimeViewModel @Inject constructor(
    services: TrackerServices
) : TrackerViewModel(services) {

    private val _projectsFlow = MutableStateFlow<List<Project>>(emptyList())
    val projectsFlow: StateFlow<List<Project>> = _projectsFlow
    var projects: List<Project>
        get() = projectsFlow.value
        set(value) {
            viewModelScope.launch {
                _projectsFlow.emit(value.filter { !it.isEmpty() })
            }
        }
    var projectEmpty: Project = Project.EMPTY.copy(true)
    var taskEmpty: ProjectTask = ProjectTask.EMPTY.copy()

    data class RecordEditData(
        val record: TimeRecord,
        val isLast: Boolean = true,
        val page: TimeListPage? = null
    )

    private val _edited = MutableStateFlow<RecordEditData?>(null)
    val edited: Flow<RecordEditData?> = _edited

    /**
     * The record was submitted.
     * @param record the record.
     * @param last is this the last record in a series that was submitted?
     * @param page the response page.
     */
    suspend fun onRecordEditSubmitted(
        record: TimeRecord,
        last: Boolean = true,
        page: TimeListPage? = null
    ) {
        _edited.emit(RecordEditData(record, last, page))
    }

    data class RecordDeletedData(val record: TimeRecord, val page: TimeListPage? = null)

    private val _deleted = MutableStateFlow<RecordDeletedData?>(null)
    val deleted: Flow<RecordDeletedData?> = _deleted

    /**
     * The record was deleted.
     * @param record the record.
     * @param responseHtml the response HTML.
     */
    suspend fun onRecordEditDeleted(record: TimeRecord, page: TimeListPage? = null) {
        _deleted.emit(RecordDeletedData(record, page))
    }

    private val _favorite = MutableStateFlow<TimeRecord?>(null)
    val favorite: Flow<TimeRecord?> = _favorite

    /**
     * The record was marked as favorite.
     * @param record the record.
     */
    suspend fun onRecordEditFavorited(record: TimeRecord) {
        _favorite.emit(record)
    }

    data class RecordEditFailureData(val record: TimeRecord, val reason: String)

    private val _editFailure = MutableStateFlow<RecordEditFailureData?>(null)
    val editFailure: Flow<RecordEditFailureData?> = _editFailure

    /**
     * Editing record failed.
     * @param record the record.
     * @param reason the failure reason.
     */
    suspend fun onRecordEditFailure(record: TimeRecord, reason: String) {
        _editFailure.emit(RecordEditFailureData(record, reason))
    }

    fun reportFormPage(refresh: Boolean): Flow<ReportFormPage> {
        return services.dataSource.reportFormPage(refresh)
    }

    fun stopRecord() {
        services.preferences.stopRecord()
    }

    fun getStartedRecord(): TimeRecord? {
        return services.preferences.getStartedRecord()
    }

    fun puncherPage(refresh: Boolean): Flow<PuncherPage> {
        return services.dataSource.puncherPage(refresh)
    }

    fun editPage(recordId: Long, refresh: Boolean): Flow<TimeEditPage> {
        return services.dataSource.editPage(recordId, refresh)
    }

    fun editRecord(record: TimeRecord): Flow<FormPage<*>> {
        return services.dataSource.editRecord(record)
    }

    fun deleteRecord(record: TimeRecord): Flow<FormPage<*>> {
        return services.dataSource.deleteRecord(record)
    }

    fun setFavorite(record: TimeRecord) {
        services.preferences.setFavorite(record)
    }

    fun getFavoriteProject(): Long {
        return services.preferences.getFavoriteProject()
    }

    fun getFavoriteTask(): Long {
        return services.preferences.getFavoriteTask()
    }

    fun timeListPage(date: Calendar, refresh: Boolean): Flow<TimeListPage> {
        return services.dataSource.timeListPage(date, refresh)
    }

    suspend fun savePage(page: TimeListPage) {
        services.dataSource.savePage(page)
    }
}