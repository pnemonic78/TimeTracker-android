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

import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.tikalk.worktracker.app.TrackerViewModel
import com.tikalk.worktracker.model.Location
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.report.LocationItem

class TimeViewModel : TrackerViewModel() {

    val projectsData = MutableLiveData<List<Project>>()
    var projectEmpty: Project = Project.EMPTY.copy(true)
    var taskEmpty: ProjectTask = projectEmpty.tasksById[TikalEntity.ID_NONE]
        ?: ProjectTask.EMPTY.copy()
    var locationEmpty: LocationItem = LocationItem(Location.EMPTY, "")

    data class RecordEditData(
        val record: TimeRecord,
        val last: Boolean = true,
        val responseHtml: String = ""
    )

    private val editData = MutableLiveData<RecordEditData>()
    val edit: LiveData<RecordEditData> = editData

    /**
     * The record was submitted.
     * @param record the record.
     * @param last is this the last record in a series that was submitted?
     * @param responseHtml the response HTML.
     */
    fun onRecordEditSubmitted(record: TimeRecord, last: Boolean = true, responseHtml: String = "") {
        editData.postValue(RecordEditData(record, last, responseHtml))
    }

    data class RecordDeletedData(val record: TimeRecord, val responseHtml: String = "")

    private val deleteData = MutableLiveData<RecordDeletedData>()
    val delete: LiveData<RecordDeletedData> = deleteData

    /**
     * The record was deleted.
     * @param record the record.
     * @param responseHtml the response HTML.
     */
    fun onRecordEditDeleted(record: TimeRecord, responseHtml: String = "") {
        deleteData.postValue(RecordDeletedData(record, responseHtml))
    }

    private val favoriteData = MutableLiveData<TimeRecord>()
    val favorite: LiveData<TimeRecord> = favoriteData

    /**
     * The record was marked as favorite.
     * @param record the record.
     */
    fun onRecordEditFavorited(record: TimeRecord) {
        favoriteData.postValue(record)
    }

    data class RecordEditFailureData(val record: TimeRecord, val reason: String)

    private val editFailureData = MutableLiveData<RecordEditFailureData>()
    val editFailure: LiveData<RecordEditFailureData> = editFailureData

    /**
     * Editing record failed.
     * @param record the record.
     * @param reason the failure reason.
     */
    fun onRecordEditFailure(record: TimeRecord, reason: String) {
        editFailureData.postValue(RecordEditFailureData(record, reason))
    }

    companion object {
        fun get(fragment: Fragment) = get(fragment.requireActivity())

        fun get(owner: ViewModelStoreOwner) =
            ViewModelProvider(owner).get(TimeViewModel::class.java)
    }
}