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

package com.tikalk.worktracker.data.remote

import com.tikalk.worktracker.db.TrackerDatabase
import com.tikalk.worktracker.db.toTimeRecordEntity
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.time.FormPage
import com.tikalk.worktracker.model.time.TimeEditPage
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.net.TimeTrackerService
import com.tikalk.worktracker.time.formatDuration
import com.tikalk.worktracker.time.formatSystemDate
import com.tikalk.worktracker.time.formatSystemTime

class TimeEditPageSaver(
    private val service: TimeTrackerService,
    db: TrackerDatabase
) : FormPageSaver<TimeRecord, TimeEditPage>(db) {

    override suspend fun savePage(db: TrackerDatabase, page: TimeEditPage): FormPage<*> {
        val result = super.savePage(db, page)
        saveRecord(db, page.record)
        return result
    }

    private suspend fun saveRecord(db: TrackerDatabase, record: TimeRecord) {
        val recordDao = db.timeRecordDao()
        val entity = record.toTimeRecordEntity()
        if (record.id == TikalEntity.ID_NONE) {
            record.id = recordDao.insert(entity)
        } else {
            recordDao.update(entity)
        }
    }

    suspend fun saveRecord(record: TimeRecord): FormPage<*> {
        val dateValue = formatSystemDate(record.date)!!

        var startValue: String? = null
        var finishValue: String? = null
        var durationValue: String? = null
        if ((record.start != null) && (record.finish != null)) {
            startValue = formatSystemTime(record.start)
            finishValue = formatSystemTime(record.finish)
        } else {
            durationValue = formatDuration(record.duration)
        }

        val response = if (record.id == TikalEntity.ID_NONE) {
            service.addTime(
                projectId = record.project.id,
                taskId = record.task.id,
                date = dateValue,
                start = startValue,
                finish = finishValue,
                duration = durationValue,
                note = record.note
            )
        } else {
            service.editTime(
                id = record.id,
                projectId = record.project.id,
                taskId = record.task.id,
                date = dateValue,
                start = startValue,
                finish = finishValue,
                duration = durationValue,
                note = record.note
            )
        }

        return FormPageParser.parse(response)
    }
}