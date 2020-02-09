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
import com.tikalk.html.textBr
import com.tikalk.html.value
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.time.FormPage
import com.tikalk.worktracker.model.time.MutableFormPage
import com.tikalk.worktracker.model.time.TimeRecord
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.FormElement
import timber.log.Timber
import java.util.regex.Pattern

open class FormPageParser<R : TimeRecord, P : FormPage<R>, MP : MutableFormPage<R>> {

    protected fun findScript(doc: Document, tokenStart: String, tokenEnd: String): String {
        val scripts = doc.select("script")
        var scriptText: String
        var indexStart: Int
        var indexEnd: Int

        for (script in scripts) {
            scriptText = script.html()
            indexStart = scriptText.indexOf(tokenStart)
            if (indexStart >= 0) {
                indexStart += tokenStart.length
                indexEnd = scriptText.indexOf(tokenEnd, indexStart)
                if (indexEnd < 0) {
                    indexEnd = scriptText.length
                }
                return scriptText.substring(indexStart, indexEnd)
            }
        }

        return ""
    }

    private fun findSelectedProject(projectInput: Element, projects: List<Project>): Project {
        for (option in projectInput.children()) {
            if (option.hasAttr("selected")) {
                val value = option.value()
                if (value.isNotEmpty()) {
                    val id = value.toLong()
                    return projects.find { id == it.id }!!
                }
                break
            }
        }
        return Project.EMPTY
    }

    private fun findSelectedTask(taskInput: Element, tasks: List<ProjectTask>): ProjectTask {
        for (option in taskInput.children()) {
            if (option.hasAttr("selected")) {
                val value = option.value()
                if (value.isNotEmpty()) {
                    val id = value.toLong()
                    return tasks.find { id == it.id }!!
                }
                break
            }
        }
        return ProjectTask.EMPTY
    }

    private fun parseProjects(select: Element): List<Project> {
        Timber.i("populateProjects")
        val projects = ArrayList<Project>()

        val options = select.select("option")
        var value: String
        var name: String
        for (option in options) {
            name = option.ownText()
            value = option.value()
            val item = Project(name)
            if (value.isNotBlank()) {
                item.id = value.toLong()
            }
            projects.add(item)
        }

        return projects
    }

    private fun parseTasks(select: Element): List<ProjectTask> {
        Timber.i("populateTasks")
        val tasks = ArrayList<ProjectTask>()

        val options = select.select("option")
        var value: String
        var name: String
        for (option in options) {
            name = option.ownText()
            value = option.value()
            val item = ProjectTask(name)
            if (value.isNotBlank()) {
                item.id = value.toLong()
            }
            tasks.add(item)
        }

        return tasks
    }

    protected open fun findTaskIds(doc: Document): String? {
        val tokenStart = "var task_ids = new Array();"
        val tokenEnd = "// Prepare an array of task names."
        return findScript(doc, tokenStart, tokenEnd)
    }

    open fun populateTaskIds(doc: Document, projects: List<Project>, tasks: List<ProjectTask>) {
        Timber.i("populateTaskIds")
        val scriptText = findTaskIds(doc) ?: return

        if (scriptText.isNotEmpty()) {
            for (project in projects) {
                project.clearTasks()
            }

            val pattern = Pattern.compile("task_ids\\[(\\d+)\\] = \"(.+)\";")
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

    /**
     * Find the first error table element.
     */
    protected open fun findError(doc: Document): String? {
        val body = doc.body()

        val errorNode = body.selectFirst("td[class='error']")
        if (errorNode != null) {
            return errorNode.textBr()
        }

        return null
    }

    open fun populateForm(doc: Document, page: MP) {
        val form = findForm(doc) ?: return
        populateForm(doc, page, form)
        page.errorMessage = findError(doc)?.trim()
    }

    open fun populateForm(doc: Document, page: MP, form: FormElement) {
        val inputProjects = form.selectByName("project") ?: return
        val inputTasks = form.selectByName("task") ?: return
        populateForm(doc, page, form, inputProjects, inputTasks)
    }

    open fun populateForm(doc: Document, page: MP, form: FormElement, inputProjects: Element, inputTasks: Element) {
        val projects = parseProjects(inputProjects)
        val tasks = parseTasks(inputTasks)
        populateTaskIds(doc, projects, tasks)

        page.projects = projects
        page.tasks = tasks
        page.record.project = findSelectedProject(inputProjects, projects)
        page.record.task = findSelectedTask(inputTasks, tasks)
    }

    protected open fun findForm(doc: Document): FormElement? {
        return doc.selectFirst("form[name='timeRecordForm']") as FormElement?
    }

    fun parse(html: String): P {
        val doc: Document = Jsoup.parse(html)
        return parse(doc)
    }

    protected fun parse(doc: Document): P {
        val record: R = createRecord()
        val page: MP = createMutablePage(record)
        populatePage(doc, page)
        return createPage(
            page.record,
            page.projects,
            page.tasks,
            page.errorMessage
        )
    }

    protected open fun createRecord(): R {
        return TimeRecord.EMPTY.copy() as R
    }

    protected open fun createPage(record: R, projects: List<Project>, tasks: List<ProjectTask>, errorMessage: String?): P {
        return FormPage(record, projects, tasks, errorMessage) as P
    }

    protected open fun createMutablePage(record: R): MP {
        return MutableFormPage(record) as MP
    }

    protected fun populatePage(doc: Document, page: MP) {
        populateForm(doc, page)
    }
}