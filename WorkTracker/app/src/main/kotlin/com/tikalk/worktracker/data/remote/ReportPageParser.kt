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

import com.tikalk.html.findParentElement
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.time.*
import com.tikalk.worktracker.time.parseSystemDate
import com.tikalk.worktracker.time.parseSystemTime
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.*
import kotlin.collections.ArrayList

class ReportPageParser {

    fun parse(html: String): ReportPage {
        val filter = createFilter()
        val page = createMutablePage(filter)

        val doc: Document = Jsoup.parse(html)
        populatePage(doc, page)

        return createPage(page)
    }

    private fun createFilter(): ReportFilter {
        return ReportFilter()
    }

    private fun createPage(page: MutableReportPage): ReportPage {
        return ReportPage(page.filter, page.records, page.totals)
    }

    private fun createMutablePage(filter: ReportFilter): MutableReportPage {
        return MutableReportPage(filter)
    }

    private fun populatePage(doc: Document, page: MutableReportPage) {
        populateList(doc, page)
        populateTotals(page.records, page)
    }

    /** Populate the list. */
    private fun populateList(doc: Document, page: MutableReportPage) {
        val records = ArrayList<TimeRecord>()
        val projects = ArrayList<Project>()

        var columnIndexDate = -1
        var columnIndexProject = -1
        var columnIndexTask = -1
        var columnIndexStart = -1
        var columnIndexFinish = -1
        var columnIndexNote = -1
        var columnIndexCost = -1

        // The first row of the table is the header
        val table = findRecordsTable(doc)
        if (table != null) {
            // loop through all the rows and parse each record
            val rows = table.getElementsByTag("tr")
            val size = rows.size
            val totalsRowIndex = size - 1
            if (size > 1) {
                val headerRow = rows.first()
                if (headerRow != null) {
                    val children = headerRow.children()
                    val childrenSize = children.size
                    for (col in 0 until childrenSize) {
                        val th = children[col]
                        when (th.ownText()) {
                            "Date" -> columnIndexDate = col
                            "Project" -> columnIndexProject = col
                            "Task" -> columnIndexTask = col
                            "Start" -> columnIndexStart = col
                            "Finish" -> columnIndexFinish = col
                            "Note" -> columnIndexNote = col
                            "Cost" -> columnIndexCost = col
                        }
                    }

                    val totalsBlankRowIndex = totalsRowIndex - 1
                    for (i in 1 until totalsBlankRowIndex) {
                        val tr = rows[i]
                        val record = parseRecord(tr,
                            i,
                            columnIndexDate,
                            columnIndexProject,
                            columnIndexTask,
                            columnIndexStart,
                            columnIndexFinish,
                            columnIndexNote,
                            columnIndexCost,
                            projects)
                        if (record != null) {
                            records.add(record)
                        }
                    }
                }
            }
        }

        page.records = records
    }

    /**
     * Find the first table whose first row has both class="tableHeader" and its label is 'Date'
     */
    private fun findRecordsTable(doc: Document): Element? {
        val body = doc.body()
        val form = body.selectFirst("form[name='reportViewForm']") ?: return null
        val td = form.selectFirst("td[class='tableHeader']") ?: return null

        val label = td.ownText()
        if (label == "Date") {
            return findParentElement(td, "table")
        }

        return null
    }

    private fun parseRecord(row: Element,
                            index: Int,
                            columnIndexDate: Int,
                            columnIndexProject: Int,
                            columnIndexTask: Int,
                            columnIndexStart: Int,
                            columnIndexFinish: Int,
                            columnIndexNote: Int,
                            columnIndexCost: Int,
                            projects: MutableList<Project>): TimeRecord? {
        val cols = row.getElementsByTag("td")
        val record = TimeRecord.EMPTY.copy()
        record.id = index + 1L
        record.status = TaskRecordStatus.CURRENT

        val tdDate = cols[columnIndexDate]
        val date = parseSystemDate(tdDate.ownText()) ?: return null

        var project: Project = record.project
        if (columnIndexProject > 0) {
            val tdProject = cols[columnIndexProject]
            if (tdProject.attr("class") == "tableHeader") {
                return null
            }
            val projectName = tdProject.ownText()
            project = parseRecordProject(projectName, projects)
            record.project = project
        }

        if (columnIndexTask > 0) {
            val tdTask = cols[columnIndexTask]
            val taskName = tdTask.ownText()
            val task = parseRecordTask(project, taskName)
            record.task = task
        }

        if (columnIndexStart > 0) {
            val tdStart = cols[columnIndexStart]
            val startText = tdStart.ownText()
            val start = parseRecordTime(date, startText) ?: return null
            record.start = start
        }

        if (columnIndexFinish > 0) {
            val tdFinish = cols[columnIndexFinish]
            val finishText = tdFinish.ownText()
            val finish = parseRecordTime(date, finishText) ?: return null
            record.finish = finish
        }

        if (columnIndexNote > 0) {
            val tdNote = cols[columnIndexNote]
            val noteText = tdNote.ownText()
            val note = parseRecordNote(noteText)
            record.note = note
        }

        if (columnIndexCost > 0) {
            val tdCost = cols[columnIndexCost]
            val costText = tdCost.ownText()
            val cost = parseCost(costText)
            record.cost = cost
        }

        return record
    }

    private fun parseRecordProject(name: String, projects: MutableList<Project>): Project {
        var project = projects.find { it.name == name }
        if (project == null) {
            project = Project(name)
            projects.add(project)
        }
        return project
    }

    private fun parseRecordTask(project: Project, name: String): ProjectTask {
        var task = project.tasks.find { task -> (task.name == name) }
        if (task == null) {
            task = ProjectTask(name)
            project.addTask(task)
        }
        return task
    }

    private fun parseRecordTime(date: Calendar, text: String): Calendar? {
        return parseSystemTime(date, text)
    }

    private fun parseRecordNote(text: String): String {
        return text.trim()
    }

    private fun parseCost(cost: String): Double {
        return if (cost.isBlank()) 0.00 else cost.toDouble()
    }

    private fun populateTotals(records: List<TimeRecord>?, page: MutableReportPage) {
        val totals = ReportTotals()

        var duration: Long
        if (records != null) {
            for (record in records) {
                duration = record.finishTime - record.startTime
                if (duration > 0L) {
                    totals.duration += duration
                }
                totals.cost += record.cost
            }
        }

        page.totals = totals
    }
}