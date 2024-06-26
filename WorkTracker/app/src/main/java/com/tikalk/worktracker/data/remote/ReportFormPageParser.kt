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

import com.tikalk.html.isChecked
import com.tikalk.html.selectByName
import com.tikalk.html.value
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.report.DefaultTimePeriod
import com.tikalk.worktracker.model.report.ReportTimePeriod
import com.tikalk.worktracker.model.time.MutableReportFormPage
import com.tikalk.worktracker.model.time.ReportFilter
import com.tikalk.worktracker.model.time.ReportFormPage
import com.tikalk.worktracker.model.time.TaskRecordStatus
import com.tikalk.worktracker.time.parseSystemDate
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.FormElement
import timber.log.Timber
import java.util.regex.Pattern

class ReportFormPageParser : FormPageParser<ReportFilter, ReportFormPage, MutableReportFormPage>() {

    override fun createRecord(): ReportFilter {
        return ReportFilter()
    }

    override fun createPage(page: MutableReportFormPage): ReportFormPage {
        return ReportFormPage(page.record, page.projects, page.errorMessage)
    }

    override fun createMutablePage(record: ReportFilter): MutableReportFormPage {
        return MutableReportFormPage(record)
    }

    override fun findForm(doc: Document): FormElement? {
        return doc.selectFirst("form[name='reportForm']") as FormElement?
    }

    override fun findTaskIds(doc: Document): String? {
        val tokenStart = "// Populate obj_tasks with task ids for each relevant project."
        val tokenEnd = "// Prepare an array of task names."
        return findScript(doc, tokenStart, tokenEnd)
    }

    override fun populateTaskIds(doc: Document, projects: List<Project>, tasks: List<ProjectTask>) {
        Timber.i("populateTaskIds")
        val scriptText = findTaskIds(doc) ?: return

        if (scriptText.isNotEmpty()) {
            for (project in projects) {
                project.clearTasks()
            }

            val pattern =
                Pattern.compile("project_property = project_prefix [+] (\\d+);\\s+obj_tasks\\[project_property\\] = \"(.+)\";")
            val matcher = pattern.matcher(scriptText)
            while (matcher.find()) {
                val projectId = matcher.group(1)!!.toLong()
                val project = projects.find { it.id == projectId }

                val taskIds: List<Long> = matcher.group(2)!!
                    .split(",")
                    .map { it.toLong() }
                val tasksPerProject = tasks.filter { it.id in taskIds }

                project?.addTasks(tasksPerProject)
            }
        }
    }

    override fun populateForm(
        doc: Document,
        page: MutableReportFormPage,
        form: FormElement,
        inputProjects: Element,
        inputTasks: Element
    ) {
        super.populateForm(doc, page, form, inputProjects, inputTasks)

        val periods = ReportTimePeriod.values()
        val inputPeriod = form.selectByName("period") ?: return
        val periodSelected = findSelectedPeriod(inputPeriod, periods)

        val inputStart = form.selectByName("start_date") ?: return
        val startValue = inputStart.value()

        val inputFinish = form.selectByName("end_date") ?: return
        val finishValue = inputFinish.value()

        val filter: ReportFilter = page.record
        filter.period = periodSelected
        filter.start = parseSystemDate(startValue)
        filter.finish = parseSystemDate(finishValue)

        val inputShowProject = form.selectByName("chproject")
        filter.isProjectFieldVisible = inputShowProject?.isChecked() ?: filter.isProjectFieldVisible

        val inputShowTask = form.selectByName("chtask")
        filter.isTaskFieldVisible = inputShowTask?.isChecked() ?: filter.isTaskFieldVisible

        val inputShowStart = form.selectByName("chstart")
        filter.isStartFieldVisible = inputShowStart?.isChecked() ?: filter.isStartFieldVisible

        val inputShowFinish = form.selectByName("chfinish")
        filter.isFinishFieldVisible = inputShowFinish?.isChecked() ?: filter.isFinishFieldVisible

        val inputShowDuration = form.selectByName("chduration")
        filter.isDurationFieldVisible = inputShowDuration?.isChecked() ?: filter.isDurationFieldVisible

        val inputShowNote = form.selectByName("chnote")
        filter.isNoteFieldVisible = inputShowNote?.isChecked() ?: filter.isNoteFieldVisible

        filter.status = TaskRecordStatus.CURRENT
    }

    private fun findSelectedPeriod(
        periodInput: Element,
        periods: Array<ReportTimePeriod>
    ): ReportTimePeriod {
        for (option in periodInput.children()) {
            if (option.hasAttr("selected")) {
                val value = option.value()
                if (value.isNotEmpty()) {
                    return periods.find { value == it.value } ?: DefaultTimePeriod
                }
                break
            }
        }
        return DefaultTimePeriod
    }
}