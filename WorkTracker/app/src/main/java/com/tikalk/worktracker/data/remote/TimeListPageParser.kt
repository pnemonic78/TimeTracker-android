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

import android.net.Uri
import com.tikalk.html.findParentElement
import com.tikalk.html.selectByName
import com.tikalk.html.value
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.time.MutableTimeListPage
import com.tikalk.worktracker.model.time.TaskRecordStatus
import com.tikalk.worktracker.model.time.TimeListPage
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.model.time.TimeTotals
import com.tikalk.worktracker.time.parseDuration
import com.tikalk.worktracker.time.parseSystemDate
import com.tikalk.worktracker.time.parseSystemTime
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.FormElement
import java.util.Calendar

class TimeListPageParser : FormPageParser<TimeRecord, TimeListPage, MutableTimeListPage>() {

    override fun createRecord(): TimeRecord {
        return TimeRecord.EMPTY.copy()
    }

    override fun createPage(page: MutableTimeListPage): TimeListPage {
        return TimeListPage(
            page.record,
            page.projects,
            page.errorMessage,
            page.date,
            page.records,
            page.totals
        )
    }

    override fun createMutablePage(record: TimeRecord): MutableTimeListPage {
        return MutableTimeListPage(record)
    }

    override fun populateForm(doc: Document, page: MutableTimeListPage, form: FormElement) {
        super.populateForm(doc, page, form)
        populateList(doc, page, form)
    }

    override fun populateForm(
        doc: Document,
        page: MutableTimeListPage,
        form: FormElement,
        inputProjects: Element,
        inputTasks: Element
    ) {
        val inputDate = form.selectByName("date") ?: return
        val dateValue = inputDate.value()
        page.date = parseSystemDate(dateValue) ?: return
        page.record.date = page.date

        super.populateForm(doc, page, form, inputProjects, inputTasks)
    }

    /** Populate the list. */
    private fun populateList(doc: Document, page: MutableTimeListPage, form: FormElement) {
        val date = page.date
        val projects = page.projects
        val records = ArrayList<TimeRecord>()

        // The first row of the table is the header
        val table = findRecordsTable(doc)
        if (table != null) {
            // loop through all the rows and parse each record
            val rows = table.getElementsByTag("tr")
            for (tr in rows) {
                val record = parseRecord(date, projects, tr) ?: continue
                records.add(record)
            }
        }

        page.records = records

        populateTotals(doc, page, form)
    }

    /**
     * Find the first table whose first row has both class="record-list" and labels 'Project' and 'Task' and 'Start'
     */
    private fun findRecordsTable(doc: Document): Element? {
        val body = doc.body()
        val div = body.selectFirst("div[class='record-list']") ?: return null
        val candidates = div.getElementsByTag("th")
        var th: Element
        var label: String

        for (candidate in candidates) {
            th = candidate
            label = th.ownText()
            if (label != "Project") {
                continue
            }
            th = th.nextElementSibling() ?: continue
            label = th.ownText()
            if (label != "Task") {
                continue
            }
            th = th.nextElementSibling() ?: continue
            label = th.ownText()
            if (label != "Start") {
                continue
            }
            return findParentElement(th, "table")
        }

        return null
    }

    private fun parseRecord(date: Calendar, projects: List<Project>, row: Element): TimeRecord? {
        val cols = row.getElementsByTag("td")
        if (cols.isEmpty()) return null

        val tdProject = cols[0]
        if (tdProject.attr("class") == "tableHeader") {
            return null
        }
        val projectName = tdProject.ownText()
        val project = parseRecordProject(projects, projectName)

        val tdTask = cols[1]
        val taskName = tdTask.ownText()
        val task = parseRecordTask(project, taskName)

        val tdStart = cols[2]
        val startText = tdStart.ownText()
        val start = parseRecordTime(date, startText)

        val tdFinish = cols[3]
        val finishText = tdFinish.ownText()
        val finish = parseRecordTime(date, finishText)

        val tdDuration = cols[4]
        val durationText = tdDuration.ownText()
        val duration = parseDuration(durationText) ?: 0L

        val tdNote = cols[5]
        val noteText = tdNote.text()
        val note = parseRecordNote(noteText)

        val tdEdit = cols[6]
        val editLink = tdEdit.child(0).attr("href")
        val id = parseRecordId(editLink)

        return TimeRecord(
            id = id,
            project = project,
            task = task,
            date = date,
            start = start,
            finish = finish,
            duration = duration,
            note = note,
            cost = 0.0,
            status = TaskRecordStatus.CURRENT
        )
    }

    private fun parseRecordProject(projects: List<Project>, name: String): Project {
        return projects.find { name == it.name } ?: Project(name = name)
    }

    private fun parseRecordTask(project: Project, name: String): ProjectTask {
        return project.tasks.find { task -> (task.name == name) } ?: ProjectTask(name = name)
    }

    private fun parseRecordTime(date: Calendar, text: String): Calendar? {
        return parseSystemTime(date, text)
    }

    private fun parseRecordNote(text: String): String {
        return text.trim()
    }

    private fun parseRecordId(link: String): Long {
        val uri = Uri.parse(link)
        val id = uri.getQueryParameter("id")!!
        return id.toLong()
    }

    private fun populateTotals(doc: Document, page: MutableTimeListPage, parent: Element?) {
        if (parent == null) {
            return
        }
        val totals = TimeTotals()

        val table = findTotalsTable(doc, parent) ?: return
        val cells = table.getElementsByTag("td")
        for (td in cells) {
            val hasClass = td.classNames().any { it.startsWith("day-totals") }
            if (!hasClass) continue

            val text = td.text()
            val value: String
            when {
                text.startsWith("Day total:") -> {
                    value = text.substring(text.indexOf(':') + 1).trim()
                    totals.daily = parseDuration(value) ?: TimeTotals.UNKNOWN
                }
                text.startsWith("Week total:") -> {
                    value = text.substring(text.indexOf(':') + 1).trim()
                    totals.weekly = parseDuration(value) ?: TimeTotals.UNKNOWN
                }
                text.startsWith("Month total:") -> {
                    value = text.substring(text.indexOf(':') + 1).trim()
                    totals.monthly = parseDuration(value) ?: TimeTotals.UNKNOWN
                }
                text.startsWith("Remaining quota:") -> {
                    value = text.substring(text.indexOf(':') + 1).trim()
                    totals.remaining = parseDuration(value) ?: TimeTotals.UNKNOWN
                }
            }
        }

        totals.status = TaskRecordStatus.CURRENT
        page.totals = totals
    }

    private fun findTotalsTable(doc: Document, parent: Element?): Element? {
        val body = doc.body()
        val div = body.selectFirst("div[class='day-totals']") ?: return null
        val candidates = div.getElementsByTag("td")
        var td: Element
        var label: String

        for (candidate in candidates) {
            td = candidate
            label = td.ownText()
            if (!label.startsWith("Week total:")) {
                continue
            }
            return findParentElement(td, "table")
        }

        return null
    }
}