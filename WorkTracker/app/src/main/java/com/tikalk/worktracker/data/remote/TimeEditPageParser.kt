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

import com.tikalk.html.selectByName
import com.tikalk.html.value
import com.tikalk.worktracker.model.Location
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.time.MutableTimeEditPage
import com.tikalk.worktracker.model.time.TaskRecordStatus
import com.tikalk.worktracker.model.time.TimeEditPage
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.time.parseDuration
import com.tikalk.worktracker.time.parseSystemTime
import java.util.Calendar
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.FormElement

class TimeEditPageParser : FormPageParser<TimeRecord, TimeEditPage, MutableTimeEditPage>() {

    override fun createRecord(): TimeRecord {
        return TimeRecord.EMPTY.copy()
    }

    override fun createPage(page: MutableTimeEditPage): TimeEditPage {
        return TimeEditPage(page.record, page.projects, page.errorMessage, page.date)
    }

    override fun createMutablePage(record: TimeRecord): MutableTimeEditPage {
        return MutableTimeEditPage(record)
    }

    override fun populateForm(
        doc: Document,
        page: MutableTimeEditPage,
        form: FormElement,
        inputProjects: Element,
        inputTasks: Element
    ) {
        super.populateForm(doc, page, form, inputProjects, inputTasks)
        val record = page.record

        findDate(form)?.let { page.date = it }
        record.date = page.date

        record.id = findId(form) ?: return
        record.start = findStartDate(page.date, form)
        record.finish = findFinishDate(page.date, form)
        record.duration = findDuration(form)
        record.note = findNote(form)
        record.location = findLocation(form)

        record.status = if (record.id == TikalEntity.ID_NONE) {
            TaskRecordStatus.DRAFT
        } else {
            TaskRecordStatus.CURRENT
        }
    }

    private fun findId(form: FormElement): Long? {
        val inputId = form.selectByName("id") ?: return null
        val idValue = inputId.value()
        return if (idValue.isBlank()) TikalEntity.ID_NONE else idValue.toLong()
    }

    private fun findStartDate(date: Calendar, form: FormElement): Calendar? {
        val inputStart = form.selectByName("start") ?: return null
        val startValue = inputStart.value()
        return parseSystemTime(date, startValue)
    }

    private fun findFinishDate(date: Calendar, form: FormElement): Calendar? {
        val inputFinish = form.selectByName("finish") ?: return null
        val finishValue = inputFinish.value()
        return parseSystemTime(date, finishValue)
    }

    private fun findDuration(form: FormElement): Long {
        val inputDuration = form.selectByName("duration") ?: return 0L
        val durationValue = inputDuration.value()
        return parseDuration(durationValue) ?: 0L
    }

    private fun findNote(form: FormElement): String {
        val inputNote = form.selectByName("note")
        return inputNote?.value().orEmpty()
    }

    private fun findLocation(form: FormElement): Location {
        val inputLocation = form.selectByName("time_field_5") ?: return Location.EMPTY
        return findSelectedLocation(inputLocation)
    }
}