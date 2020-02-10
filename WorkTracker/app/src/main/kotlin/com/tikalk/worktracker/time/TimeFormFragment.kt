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

import android.os.Bundle
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.tikalk.app.runOnUiThread
import com.tikalk.html.selectByName
import com.tikalk.html.value
import com.tikalk.worktracker.BuildConfig
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.LoginFragment
import com.tikalk.worktracker.db.ProjectTaskKey
import com.tikalk.worktracker.db.ProjectWithTasks
import com.tikalk.worktracker.db.TrackerDatabase
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.net.InternetFragment
import io.reactivex.Single
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.FormElement
import timber.log.Timber
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

abstract class TimeFormFragment : InternetFragment(),
    LoginFragment.OnLoginListener {

    open var record: TimeRecord = TimeRecord.EMPTY.copy()
    val projectsData = MutableLiveData<List<Project>>()
    val tasksData = MutableLiveData<List<ProjectTask>>()
    var projectEmpty: Project = Project.EMPTY
    var taskEmpty: ProjectTask = ProjectTask.EMPTY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectsData.observe(this, Observer { projects ->
            this.projectEmpty = projects.firstOrNull { it.isEmpty() } ?: projectEmpty
            onProjectsUpdated(projects)
        })
        tasksData.observe(this, Observer { tasks ->
            this.taskEmpty = tasks.firstOrNull { it.isEmpty() } ?: taskEmpty
            onTasksUpdated(tasks)
        })
    }

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
        return projectEmpty
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
        return taskEmpty
    }

    private fun populateProjects(select: Element, target: MutableLiveData<List<Project>>): List<Project> {
        Timber.i("populateProjects")
        val projects = ArrayList<Project>()

        val options = select.select("option")
        var value: String
        var name: String
        for (option in options) {
            name = option.ownText()
            value = option.value()
            val item = Project(name)
            if (value.isEmpty()) {
                projectEmpty = item
            } else {
                item.id = value.toLong()
            }
            projects.add(item)
        }

        target.postValue(projects.sortedBy { it.name })
        saveProjects(db, projects)
        return projects
    }

    private fun populateTasks(select: Element, target: MutableLiveData<List<ProjectTask>>): List<ProjectTask> {
        Timber.i("populateTasks")
        val tasks = ArrayList<ProjectTask>()

        val options = select.select("option")
        var value: String
        var name: String
        for (option in options) {
            name = option.ownText()
            value = option.value()
            val item = ProjectTask(name)
            if (value.isEmpty()) {
                taskEmpty = item
            } else {
                item.id = value.toLong()
            }
            tasks.add(item)
        }

        target.postValue(tasks.sortedBy { it.name })
        saveTasks(db, tasks)
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

        saveProjectTaskKeys(db, projects)
    }

    fun populateForm(date: Calendar, html: String): Document {
        val doc: Document = Jsoup.parse(html)
        populateForm(date, doc)
        return doc
    }

    open fun populateForm(date: Calendar, doc: Document) {
        val form = findForm(doc) ?: return
        populateForm(date, doc, form)
        populateForm(record)
    }

    open fun populateForm(date: Calendar, doc: Document, form: FormElement) {
        val inputProjects = form.selectByName("project") ?: return
        val inputTasks = form.selectByName("task") ?: return
        populateForm(date, doc, form, inputProjects, inputTasks)
    }

    open fun populateForm(date: Calendar, doc: Document, form: FormElement, inputProjects: Element, inputTasks: Element) {
        val projects = populateProjects(inputProjects, projectsData)
        val tasks = populateTasks(inputTasks, tasksData)
        populateTaskIds(doc, projects, tasks)

        setRecordProject(findSelectedProject(inputProjects, projects))
        setRecordTask(findSelectedTask(inputTasks, tasks))
    }

    protected open fun findForm(doc: Document): FormElement? {
        return doc.selectFirst("form[name='timeRecordForm']") as FormElement?
    }

    protected open fun saveProjects(db: TrackerDatabase, projects: List<Project>) {
        val projectsDao = db.projectDao()
        val projectsDb = projectsDao.queryAll()
        val projectsDbById: MutableMap<Long, Project> = HashMap()
        for (project in projectsDb) {
            val projectId = project.id
            projectsDbById[projectId] = project
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

    protected open fun saveTasks(db: TrackerDatabase, tasks: List<ProjectTask>) {
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

    protected open fun saveProjectTaskKeys(db: TrackerDatabase, projects: List<Project>) {
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

    abstract fun populateForm(record: TimeRecord)

    @MainThread
    abstract fun bindForm(record: TimeRecord)

    fun markFavorite() {
        markFavorite(record)
    }

    protected open fun markFavorite(record: TimeRecord) {
        Timber.i("markFavorite $record")
        preferences.setFavorite(record)
        Toast.makeText(requireContext(), getString(R.string.favorite_marked, record.project.name, record.task.name), Toast.LENGTH_LONG).show()
    }

    fun populateAndBind() {
        val record = this.record
        Timber.i("populateAndBind record=$record")
        populateForm(record)
        runOnUiThread { bindForm(record) }
    }

    override fun onLoginSuccess(fragment: LoginFragment, login: String) {
        Timber.i("login success")
        fragment.dismissAllowingStateLoss()
    }

    override fun onLoginFailure(fragment: LoginFragment, login: String, reason: String) {
        Timber.e("login failure: $reason")
    }

    protected open fun setRecordValue(record: TimeRecord) {
        Timber.d("setRecordValue record=$record")
        this.record = record
    }

    protected open fun setRecordProject(project: Project) {
        Timber.d("setRecordProject project=$project")
        record.project = project
    }

    protected open fun setRecordTask(task: ProjectTask) {
        Timber.d("setRecordTask task=$task")
        record.task = task
    }

    protected open fun onProjectsUpdated(projects: List<Project>) {
        populateForm(record)
        bindForm(record)
    }

    protected open fun onTasksUpdated(tasks: List<ProjectTask>) {
        populateForm(record)
        bindForm(record)
    }

    protected fun applyFavorite() {
        val projects = projectsData.value ?: return
        val tasks = tasksData.value ?: return

        val projectFavorite = preferences.getFavoriteProject()
        if (projectFavorite != TikalEntity.ID_NONE) {
            setRecordProject(projects.firstOrNull { it.id == projectFavorite } ?: record.project)
        }
        val taskFavorite = preferences.getFavoriteTask()
        if (taskFavorite != TikalEntity.ID_NONE) {
            setRecordTask(tasks.firstOrNull { it.id == taskFavorite } ?: record.task)
        }
    }

    companion object {
        const val STATE_RECORD = "record"

        const val EXTRA_DATE = BuildConfig.APPLICATION_ID + ".form.DATE"
        const val EXTRA_RECORD_ID = BuildConfig.APPLICATION_ID + ".form.RECORD_ID"

        const val EXTRA_PROJECT_ID = BuildConfig.APPLICATION_ID + ".form.PROJECT_ID"
        const val EXTRA_TASK_ID = BuildConfig.APPLICATION_ID + ".form.TASK_ID"
        const val EXTRA_START_TIME = BuildConfig.APPLICATION_ID + ".form.START_TIME"
        const val EXTRA_FINISH_TIME = BuildConfig.APPLICATION_ID + ".form.FINISH_TIME"
    }
}