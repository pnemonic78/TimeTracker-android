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
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.time.MutableTimeEditPage
import com.tikalk.worktracker.model.time.TaskRecordStatus
import com.tikalk.worktracker.model.time.TimeEditPage
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.time.parseSystemDate
import com.tikalk.worktracker.time.parseSystemTime
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.FormElement
import java.util.*

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

    override fun populateForm(doc: Document, page: MutableTimeEditPage, form: FormElement, inputProjects: Element, inputTasks: Element) {
        super.populateForm(doc, page, form, inputProjects, inputTasks)
        val record = page.record

        val inputDate = form.selectByName("date") ?: return
        val dateValue = inputDate.value()
        page.date = parseSystemDate(dateValue) ?: Calendar.getInstance()

        val inputId = form.selectByName("id") ?: return
        val idValue = inputId.value()
        record.id = if (idValue.isBlank()) TikalEntity.ID_NONE else idValue.toLong()

        val inputStart = form.selectByName("start") ?: return
        val startValue = inputStart.value()
        record.start = parseSystemTime(page.date, startValue)

        val inputFinish = form.selectByName("finish") ?: return
        val finishValue = inputFinish.value()
        record.finish = parseSystemTime(page.date, finishValue)

        val inputNote = form.selectByName("note")
        val noteValue = inputNote?.value() ?: ""
        record.note = noteValue

        record.status = if (record.id == TikalEntity.ID_NONE) TaskRecordStatus.DRAFT else TaskRecordStatus.CURRENT
    }
}