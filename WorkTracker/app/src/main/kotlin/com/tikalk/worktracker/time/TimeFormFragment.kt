/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2019, Tikal Knowledge, Ltd.
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

import android.widget.Toast
import androidx.annotation.MainThread
import androidx.navigation.fragment.findNavController
import com.tikalk.app.isShowing
import com.tikalk.app.runOnUiThread
import com.tikalk.html.selectByName
import com.tikalk.worktracker.BuildConfig
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.LoginFragment
import com.tikalk.worktracker.db.ProjectTaskKey
import com.tikalk.worktracker.db.TrackerDatabase
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.net.InternetFragment
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.FormElement
import timber.log.Timber
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.regex.Pattern
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

abstract class TimeFormFragment : InternetFragment(),
    LoginFragment.OnLoginListener {

    open var record: TimeRecord = TimeRecord.EMPTY.copy()
    val projects: MutableList<Project> = CopyOnWriteArrayList()
    val tasks: MutableList<ProjectTask> = CopyOnWriteArrayList()
    var projectEmpty: Project = Project.EMPTY
    var taskEmpty: ProjectTask = ProjectTask.EMPTY

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

    fun findSelectedProject(projectInput: Element, projects: List<Project>): Project {
        for (option in projectInput.children()) {
            if (option.hasAttr("selected")) {
                val value = option.attr("value")
                if (value.isNotEmpty()) {
                    val id = value.toLong()
                    return projects.find { id == it.id }!!
                }
                break
            }
        }
        return projectEmpty
    }

    fun findSelectedTask(taskInput: Element, tasks: List<ProjectTask>): ProjectTask {
        for (option in taskInput.children()) {
            if (option.hasAttr("selected")) {
                val value = option.attr("value")
                if (value.isNotEmpty()) {
                    val id = value.toLong()
                    return tasks.find { id == it.id }!!
                }
                break
            }
        }
        return taskEmpty
    }

    fun populateProjects(select: Element, target: MutableList<Project>) {
        Timber.v("populateProjects")
        val projects = ArrayList<Project>()

        val options = select.select("option")
        var value: String
        var name: String
        for (option in options) {
            name = option.ownText()
            value = option.attr("value")
            val item = Project(name)
            if (value.isEmpty()) {
                projectEmpty = item
            } else {
                item.id = value.toLong()
            }
            projects.add(item)
        }

        target.clear()
        target.addAll(projects.sortedBy { it.name })
    }

    fun populateTasks(select: Element, target: MutableList<ProjectTask>) {
        Timber.v("populateTasks")
        val tasks = ArrayList<ProjectTask>()

        val options = select.select("option")
        var value: String
        var name: String
        for (option in options) {
            name = option.ownText()
            value = option.attr("value")
            val item = ProjectTask(name)
            if (value.isEmpty()) {
                taskEmpty = item
            } else {
                item.id = value.toLong()
            }
            tasks.add(item)
        }

        target.clear()
        target.addAll(tasks.sortedBy { it.name })
    }

    protected open fun findTaskIds(doc: Document): String? {
        val tokenStart = "var task_ids = new Array();"
        val tokenEnd = "// Prepare an array of task names."
        return findScript(doc, tokenStart, tokenEnd)
    }

    open fun populateTaskIds(doc: Document, projects: List<Project>) {
        Timber.v("populateTaskIds")
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
                val tasks = this.tasks.filter { it.id in taskIds }

                project?.addTasks(tasks)
            }
        }
    }

    open fun populateForm(date: Calendar, html: String) {
        val doc: Document = Jsoup.parse(html)
        populateForm(date, doc)
    }

    open fun populateForm(date: Calendar, doc: Document) {
        val form = findForm(doc) ?: return
        populateForm(date, doc, form)
    }

    open fun populateForm(date: Calendar, doc: Document, form: FormElement) {
        val inputProjects = form.selectByName("project") ?: return
        val inputTasks = form.selectByName("task") ?: return
        populateForm(date, doc, form, inputProjects, inputTasks)
    }

    open fun populateForm(date: Calendar, doc: Document, form: FormElement, inputProjects: Element, inputTasks: Element) {
        populateProjects(inputProjects, projects)
        populateTasks(inputTasks, tasks)
        populateTaskIds(doc, projects)

        record.project = findSelectedProject(inputProjects, projects)
        record.task = findSelectedTask(inputTasks, tasks)
    }

    protected open fun findForm(doc: Document): FormElement? {
        return doc.selectFirst("form[name='timeRecordForm']") as FormElement?
    }

    fun savePage() {
        saveFormToDb()
    }

    open fun saveFormToDb() {
        Timber.v("saveFormToDb")
        saveProjects(db)
        saveTasks(db)
        saveProjectTaskKeys(db)
    }

    protected open fun saveProjects(db: TrackerDatabase) {
        val projects = this.projects
        val projectsDao = db.projectDao()
        val projectsDb = projectsDao.queryAll()
        val projectsDbById: MutableMap<Long, Project> = HashMap()
        for (project in projectsDb) {
            projectsDbById[project.id] = project
        }

        val projectsToInsert = ArrayList<Project>()
        val projectsToUpdate = ArrayList<Project>()
        //var projectDb: Project
        for (project in projects) {
            val projectId = project.id
            if (projectsDbById.containsKey(projectId)) {
                //projectDb = projectsDbById[projectId]!!
                //project.dbId = projectDb.dbId
                projectsToUpdate.add(project)
            } else {
                projectsToInsert.add(project)
            }
            projectsDbById.remove(projectId)
        }

        val projectsToDelete = projectsDbById.values
        projectsDao.delete(projectsToDelete)

        val projectIds = projectsDao.insert(projectsToInsert)
        //for (i in projectIds.indices) {
        //    projectsToInsert[i].dbId = projectIds[i]
        //}

        projectsDao.update(projectsToUpdate)
    }

    protected open fun saveTasks(db: TrackerDatabase) {
        val tasks = this.tasks
        val tasksDao = db.taskDao()
        val tasksDb = tasksDao.queryAll()
        val tasksDbById: MutableMap<Long, ProjectTask> = HashMap()
        for (task in tasksDb) {
            tasksDbById[task.id] = task
        }

        val tasksToInsert = ArrayList<ProjectTask>()
        val tasksToUpdate = ArrayList<ProjectTask>()
        //var taskDb: ProjectTask
        for (task in tasks) {
            val taskId = task.id
            if (tasksDbById.containsKey(taskId)) {
                //taskDb = tasksDbById[taskId]!!
                //task.dbId = taskDb.dbId
                tasksToUpdate.add(task)
            } else {
                tasksToInsert.add(task)
            }
            tasksDbById.remove(taskId)
        }

        val tasksToDelete = tasksDbById.values
        tasksDao.delete(tasksToDelete)

        val taskIds = tasksDao.insert(tasksToInsert)
        //for (i in taskIds.indices) {
        //    tasksToInsert[i].dbId = taskIds[i]
        //}

        tasksDao.update(tasksToUpdate)
    }

    protected open fun saveProjectTaskKeys(db: TrackerDatabase) {
        val keys: List<ProjectTaskKey> = projects.flatMap { project ->
            project.tasks.map { task -> ProjectTaskKey(project.id, task.id) }
        }

        val projectTasksDao = db.projectTaskKeyDao()
        val keysDb = projectTasksDao.queryAll()
        val keysDbMutable = keysDb.toMutableList()
        val keysToInsert = ArrayList<ProjectTaskKey>()
        val keysToUpdate = ArrayList<ProjectTaskKey>()
        var keyDbFound: ProjectTaskKey?
        for (key in keys) {
            keyDbFound = null
            for (keyDb in keysDbMutable) {
                if (key == keyDb) {
                    keyDbFound = keyDb
                    break
                }
            }
            if (keyDbFound != null) {
                //key.dbId = keyDbFound.dbId
                keysToUpdate.add(key)
                keysDbMutable.remove(keyDbFound)
            } else {
                keysToInsert.add(key)
            }
        }

        val keysToDelete = keysDbMutable
        projectTasksDao.delete(keysToDelete)

        val keyIds = projectTasksDao.insert(keysToInsert)
        //for (i in keyIds.indices) {
        //    keysToInsert[i].dbId = keyIds[i]
        //}

        projectTasksDao.update(keysToUpdate)
    }

    fun loadForm() {
        loadFormFromDb()

        val recordStarted = preferences.getStartedRecord()
        if (recordStarted != null) {
            record = recordStarted
        }
    }

    protected fun loadFormFromDb() {
        Timber.v("loadFormFromDb")
        loadFormFromDb(db)
    }

    @Synchronized
    protected open fun loadFormFromDb(db: TrackerDatabase) {
        Timber.v("loadFormFromDb")
        loadProjectsWithTasks(db)
    }

    private fun loadProjectsWithTasks(db: TrackerDatabase) {
        val projectsDao = db.projectDao()
        val projectsWithTasks = projectsDao.queryAllWithTasks()
        val projectsDb = ArrayList<Project>()
        val tasksDb = HashSet<ProjectTask>()
        for (projectWithTasks in projectsWithTasks) {
            val project = projectWithTasks.project
            project.tasks = projectWithTasks.tasks
            projectsDb.add(project)
            tasksDb.addAll(projectWithTasks.tasks)
        }
        projects.clear()
        projects.addAll(projectsDb.sortedBy { it.name })
        this.projectEmpty = projects.firstOrNull { it.isEmpty() } ?: projectEmpty

        tasks.clear()
        tasks.addAll(tasksDb.sortedBy { it.name })
        this.taskEmpty = tasks.firstOrNull { it.isEmpty() } ?: taskEmpty
    }

    abstract fun populateForm(record: TimeRecord)

    @MainThread
    abstract fun bindForm(record: TimeRecord)

    fun markFavorite() {
        markFavorite(record)
    }

    protected open fun markFavorite(record: TimeRecord) {
        Timber.v("markFavorite $record")
        preferences.setFavorite(record)
        Toast.makeText(requireContext(), getString(R.string.favorite_marked, record.project.name, record.task.name), Toast.LENGTH_LONG).show()
    }

    fun populateAndBind() {
        val record = this.record
        populateForm(record)
        runOnUiThread { bindForm(record) }
    }

    override fun onLoginSuccess(fragment: LoginFragment, login: String) {
        Timber.i("login success")
        if (fragment.isShowing()) {
            findNavController().popBackStack()
        }
    }

    override fun onLoginFailure(fragment: LoginFragment, login: String, reason: String) {
        Timber.e("login failure: $reason")
    }

    companion object {
        const val STATE_RECORD_ID = "record_id"
        const val STATE_RECORD = "record"

        const val EXTRA_DATE = BuildConfig.APPLICATION_ID + ".form.DATE"
        const val EXTRA_RECORD = BuildConfig.APPLICATION_ID + ".form.RECORD_ID"

        const val EXTRA_PROJECT_ID = BuildConfig.APPLICATION_ID + ".form.PROJECT_ID"
        const val EXTRA_TASK_ID = BuildConfig.APPLICATION_ID + ".form.TASK_ID"
        const val EXTRA_START_TIME = BuildConfig.APPLICATION_ID + ".form.START_TIME"
        const val EXTRA_FINISH_TIME = BuildConfig.APPLICATION_ID + ".form.FINISH_TIME"
    }
}