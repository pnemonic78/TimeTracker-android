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

import com.tikalk.time.copy
import com.tikalk.time.setToEndOfDay
import com.tikalk.time.setToStartOfDay
import com.tikalk.worktracker.db.TimeRecordEntity
import com.tikalk.worktracker.db.TrackerDatabase
import com.tikalk.worktracker.db.WholeTimeRecordEntity
import com.tikalk.worktracker.db.toTimeRecordEntity
import com.tikalk.worktracker.model.time.FormPage
import com.tikalk.worktracker.model.time.TimeListPage
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.time.formatSystemDate
import java.util.Calendar
import timber.log.Timber

class TimeListPageSaver(db: TrackerDatabase) : FormPageSaver<TimeRecord, TimeListPage>(db) {

    override suspend fun savePage(db: TrackerDatabase, page: TimeListPage): FormPage<*> {
        val result = super.savePage(db, page)
        saveRecords(db, page.date, page.records)
        return result
    }

    private suspend fun saveRecords(
        db: TrackerDatabase,
        date: Calendar,
        records: List<TimeRecord>
    ) {
        Timber.i("saveRecords ${formatSystemDate(date)}")
        val recordsDao = db.timeRecordDao()
        val recordsDb = queryRecords(db, date)
        val recordsDbById = recordsDb.associate { it.record.id to it.record }

        val recordsToInsert = ArrayList<TimeRecordEntity>()
        val recordsToUpdate = ArrayList<TimeRecordEntity>()
        val recordsToDelete = ArrayList<TimeRecordEntity>(recordsDbById.values)
        for (record in records) {
            val recordId = record.id
            if (recordsDbById.containsKey(recordId)) {
                val recordDb = recordsDbById[recordId]!!
                mergeRecord(recordDb, record)
                recordsToUpdate.add(recordDb)
                recordsToDelete.remove(recordDb)
            } else if (!record.isEmpty()) {
                recordsToInsert.add(record.toTimeRecordEntity())
            }
        }

        recordsDao.delete(recordsToDelete)
        recordsDao.insert(recordsToInsert)
        recordsDao.update(recordsToUpdate)
    }

    private suspend fun queryRecords(
        db: TrackerDatabase,
        date: Calendar
    ): List<WholeTimeRecordEntity> {
        val recordsDao = db.timeRecordDao()
        val start = date.copy()
        start.setToStartOfDay()
        val finish = date.copy()
        finish.setToEndOfDay()
        return recordsDao.queryByDate(start.timeInMillis, finish.timeInMillis)
    }

    /**
     * Merge known fields to the latest page record that are missing.
     * @param recordDb The cached record from the local database.
     * @param recordPage The parsed record from the page.
     */
    private fun mergeRecord(recordDb: TimeRecordEntity, recordPage: TimeRecord) {
        recordDb.apply {
            duration = recordPage.duration
            finish = recordPage.finish
            note = recordPage.note
            projectId = recordPage.project.id
            start = recordPage.start
            status = recordPage.status
            taskId = recordPage.task.id
        }
    }
}