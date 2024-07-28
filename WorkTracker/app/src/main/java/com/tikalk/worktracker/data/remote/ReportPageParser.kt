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
import com.tikalk.worktracker.BuildConfig
import com.tikalk.worktracker.db.ProjectWithTasks
import com.tikalk.worktracker.db.TrackerDatabase
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.TikalEntity.Companion.ID_NONE
import com.tikalk.worktracker.model.time.MutableReportPage
import com.tikalk.worktracker.model.time.ReportFilter
import com.tikalk.worktracker.model.time.ReportPage
import com.tikalk.worktracker.model.time.ReportTotals
import com.tikalk.worktracker.model.time.TaskRecordStatus
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.net.TimeTrackerService
import com.tikalk.worktracker.time.parseDuration
import com.tikalk.worktracker.time.parseSystemDate
import com.tikalk.worktracker.time.parseSystemTime
import java.util.Calendar
import kotlin.math.max
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class ReportPageParser(private val filter: ReportFilter) {

    suspend fun parse(html: String, db: TrackerDatabase?): ReportPage {
        val doc: Document = Jsoup.parse(html)
        return parse(doc, db)
    }

    private suspend fun parse(doc: Document, db: TrackerDatabase?): ReportPage {
        val page = createMutablePage(filter)

        if (db != null) {
            val projectsWithTasks = loadProjectsWithTasks(db)
            val projects = page.projects
            val tasks = ArrayList<ProjectTask>()
            populateProjectsWithTasks(projectsWithTasks, projects, tasks)
        }

        populatePage(doc, page)

        return createPage(page)
    }

    private fun createPage(page: MutableReportPage): ReportPage {
        return ReportPage(page.filter, page.records, page.totals)
    }

    private fun createMutablePage(filter: ReportFilter): MutableReportPage {
        return MutableReportPage(filter)
    }

    private suspend fun loadProjectsWithTasks(db: TrackerDatabase): List<ProjectWithTasks> {
        val projectsDao = db.projectDao()
        return projectsDao.queryAllWithTasks()
    }

    private fun populateProjectsWithTasks(
        projectsWithTasks: List<ProjectWithTasks>,
        projects: MutableCollection<Project>,
        tasks: MutableCollection<ProjectTask>
    ) {
        projects.clear()
        tasks.clear()

        for (projectWithTasks in projectsWithTasks) {
            val project = projectWithTasks.project
            project.tasks = projectWithTasks.tasks
            projects.add(project)
            tasks.addAll(projectWithTasks.tasks)
        }
    }

    private fun populatePage(doc: Document, page: MutableReportPage) {
        populateList(doc, page)
        populateTotals(page.records, page)
    }

    /** Populate the list. */
    private fun populateList(doc: Document, page: MutableReportPage) {
        val records = ArrayList<TimeRecord>()
        val projects = page.projects

        var columnIndexDate = -1
        var columnIndexProject = -1
        var columnIndexTask = -1
        var columnIndexStart = -1
        var columnIndexFinish = -1
        var columnIndexDuration = -1
        var columnIndexNote = -1
        var columnIndexCost = -1
        var columnIndexEdit = -1

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
                            "Duration" -> columnIndexDuration = col
                            "Note" -> columnIndexNote = col
                            "Cost" -> columnIndexCost = col
                        }
                    }
                    columnIndexEdit = childrenSize - 1

                    val totalsBlankRowIndex = totalsRowIndex - 1
                    for (i in 1 until totalsBlankRowIndex) {
                        val tr = rows[i]
                        val record = parseRecord(
                            tr,
                            i,
                            columnIndexDate,
                            columnIndexProject,
                            columnIndexTask,
                            columnIndexStart,
                            columnIndexFinish,
                            columnIndexDuration,
                            columnIndexNote,
                            columnIndexCost,
                            columnIndexEdit,
                            projects
                        ) ?: continue
                        records.add(record)
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
        val th = form.selectFirst("th") ?: return null

        val label = th.ownText()
        if (label == "Date") {
            return findParentElement(th, "table")
        }

        return null
    }

    private fun parseRecord(
        row: Element,
        index: Int,
        columnIndexDate: Int,
        columnIndexProject: Int,
        columnIndexTask: Int,
        columnIndexStart: Int,
        columnIndexFinish: Int,
        columnIndexDuration: Int,
        columnIndexNote: Int,
        columnIndexCost: Int,
        columnIndexEdit: Int,
        projects: MutableCollection<Project>
    ): TimeRecord? {
        val cols = row.getElementsByTag("td")
        if (cols.isEmpty()) return null

        val record = TimeRecord.EMPTY.copy()
        record.id = index + 1L
        record.status = TaskRecordStatus.CURRENT

        val tdDate = cols[columnIndexDate]
        val date = parseSystemDate(tdDate.ownText()) ?: return null
        record.date = date

        var project: Project = record.project
        if (columnIndexProject >= 0) {
            val tdProject = cols[columnIndexProject]
            if (tdProject.attr("class") == "tableHeader") {
                return null
            }
            val projectName = tdProject.ownText()
            project = parseRecordProject(projectName, projects)
            record.project = project
        }

        if (columnIndexTask >= 0) {
            val tdTask = cols[columnIndexTask]
            val taskName = tdTask.ownText()
            val task = parseRecordTask(project, taskName)
            record.task = task
        }

        if (columnIndexStart >= 0) {
            val tdStart = cols[columnIndexStart]
            val startText = tdStart.ownText()
            val start = parseRecordTime(date, startText)
            record.start = start
        }

        if (columnIndexFinish >= 0) {
            val tdFinish = cols[columnIndexFinish]
            val finishText = tdFinish.ownText()
            val finish = parseRecordTime(date, finishText)
            record.finish = finish
        }

        if (columnIndexDuration >= 0) {
            val tdDuration = cols[columnIndexDuration]
            val durationText = tdDuration.ownText()
            val duration = parseDuration(durationText) ?: 0L
            record.duration = duration
        }

        if (columnIndexNote >= 0) {
            val tdNote = cols[columnIndexNote]
            val noteText = tdNote.ownText()
            val note = parseRecordNote(noteText)
            record.note = note
        }

        if (columnIndexCost >= 0) {
            val tdCost = cols[columnIndexCost]
            val costText = tdCost.ownText()
            val cost = parseCost(costText)
            record.cost = cost
        }

        if (columnIndexEdit >= 0) {
            val tdEdit = cols[columnIndexEdit]
            val editAnchor = tdEdit.selectFirst("a");
            val id = parseEditId(editAnchor)
            record.id = id
        }

        return record
    }

    private fun parseRecordProject(name: String, projects: MutableCollection<Project>): Project {
        var project = projects.find { it.name == name }
        if (project == null) {
            project = Project(name = name)
            projects.add(project)
        }
        return project
    }

    private fun parseRecordTask(project: Project, name: String): ProjectTask {
        var task = project.tasks.find { task -> (task.name == name) }
        if (task == null) {
            task = ProjectTask(name = name)
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

    private fun parseCost(text: String): Double {
        return if (text.isBlank()) 0.00 else text.toDouble()
    }

    private fun parseEditId(anchor: Element?): Long {
        if (anchor == null) return ID_NONE
        val href = anchor.attr("href")
        if (href.isNullOrEmpty()) return ID_NONE
        val uri = Uri.parse(BuildConfig.API_URL + href)
        val page = uri.lastPathSegment ?: return ID_NONE
        if (page != TimeTrackerService.PHP_EDIT) return ID_NONE
        val id = uri.getQueryParameter("id") ?: return ID_NONE
        return id.toLong()
    }

    private fun populateTotals(records: List<TimeRecord>, page: MutableReportPage) {
        val totals = ReportTotals()

        var duration: Long
        for (record in records) {
            duration = record.duration
            totals.duration += max(duration, 0L)
            totals.cost += record.cost
        }

        page.totals = totals
    }
}