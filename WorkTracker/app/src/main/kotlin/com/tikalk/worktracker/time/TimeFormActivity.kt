/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2017, Tikal Knowledge, Ltd.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
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
import com.tikalk.worktracker.db.TimeRecordEntity
import com.tikalk.worktracker.db.TrackerDatabase
import com.tikalk.worktracker.db.toTimeRecord
import com.tikalk.worktracker.db.toTimeRecordEntity
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.db.ProjectTaskKey
import com.tikalk.worktracker.model.User
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.net.InternetActivity
import com.tikalk.worktracker.preference.TimeTrackerPrefs
import io.reactivex.disposables.CompositeDisposable
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import timber.log.Timber
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

abstract class TimeFormActivity : InternetActivity() {

    protected val disposables = CompositeDisposable()
    protected var date = Calendar.getInstance()
    protected var user = User.EMPTY.copy()
    protected var record = TimeRecord(user, Project(""), ProjectTask(""))
    protected val projects = ArrayList<Project>()
    protected val tasks = ArrayList<ProjectTask>()
    protected var projectEmpty: Project = Project.EMPTY
    protected var taskEmpty: ProjectTask = ProjectTask.EMPTY
    protected val records = ArrayList<TimeRecord>()

    protected lateinit var prefs: TimeTrackerPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = TimeTrackerPrefs(this)

        user.username = prefs.userCredentials.login
        user.email = user.username
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
    }

    private fun findScript(doc: Document, tokenStart: String, tokenEnd: String): String {
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

    protected fun findSelectedProject(project: Element, projects: List<Project>): Project {
        for (option in project.children()) {
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

    protected fun findSelectedTask(task: Element, tasks: List<ProjectTask>): ProjectTask {
        for (option in task.children()) {
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

    protected fun populateProjects(select: Element, target: MutableList<Project>) {
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
        target.addAll(projects)
    }

    protected fun populateTasks(select: Element, target: MutableList<ProjectTask>) {
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
        target.addAll(tasks)
    }

    protected fun populateTaskIds(doc: Document, projects: List<Project>) {
        Timber.v("populateTaskIds")
        val tokenStart = "var task_ids = new Array();"
        val tokenEnd = "// Prepare an array of task names."
        val scriptText = findScript(doc, tokenStart, tokenEnd)

        for (project in projects) {
            project.clearTasks()
        }

        if (scriptText.isNotEmpty()) {
            val pattern = Pattern.compile("task_ids\\[(\\d+)\\] = \"(.+)\"")
            val lines = scriptText.split(";")
            for (line in lines) {
                val matcher = pattern.matcher(line)
                if (matcher.find()) {
                    val projectId = matcher.group(1).toLong()
                    val project = projects.find { it.id == projectId }

                    val taskIds: List<Long> = matcher.group(2)
                        .split(",")
                        .map { it.toLong() }
                    val tasks = this.tasks.filter { it.id in taskIds }

                    project?.addTasks(tasks)
                }
            }
        }
    }

    protected fun markFavorite() {
        prefs.setFavorite(record)
    }

    /**
     * Shows the progress UI and hides the login form.
     * @param show visible?
     */
    abstract fun showProgress(show: Boolean)

    protected fun showProgressMain(show: Boolean) {
        runOnUiThread { showProgress(show) }
    }

    protected fun saveFormToDb() {
        Timber.v("saveFormToDb")
        val db = TrackerDatabase.getDatabase(this)

        saveProjects(db)
        saveTasks(db)
        saveProjectTaskKeys(db)
        saveRecords(db)
    }

    private fun saveProjects(db: TrackerDatabase) {
        val projects = this.projects
        val projectsDao = db.projectDao()
        val projectsDb = projectsDao.queryAll()
        val projectsDbById: MutableMap<Long, Project> = HashMap()
        for (project in projectsDb) {
            projectsDbById[project.id] = project
        }

        val projectsToInsert = ArrayList<Project>()
        val projectsToUpdate = ArrayList<Project>()
        var projectDb: Project
        for (project in projects) {
            val projectId = project.id
            if (projectsDbById.containsKey(projectId)) {
                projectDb = projectsDbById[projectId]!!
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

    private fun saveTasks(db: TrackerDatabase) {
        val tasks = this.tasks
        val tasksDao = db.taskDao()
        val tasksDb = tasksDao.queryAll()
        val tasksDbById: MutableMap<Long, ProjectTask> = HashMap()
        for (task in tasksDb) {
            tasksDbById[task.id] = task
        }

        val tasksToInsert = ArrayList<ProjectTask>()
        val tasksToUpdate = ArrayList<ProjectTask>()
        var taskDb: ProjectTask
        for (task in tasks) {
            val taskId = task.id
            if (tasksDbById.containsKey(taskId)) {
                taskDb = tasksDbById[taskId]!!
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

    private fun saveProjectTaskKeys(db: TrackerDatabase) {
        val keys: List<ProjectTaskKey> = projects.flatMap { project ->
            project.tasks.values.map { task -> ProjectTaskKey(project.id, task.id) }
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

    private fun saveRecords(db: TrackerDatabase) {
        val records = this.records
        val recordsDao = db.timeRecordDao()
        val recordsDb = recordsDao.queryAll()
        val recordsDbById: MutableMap<Long, TimeRecordEntity> = HashMap()
        for (record in recordsDb) {
            recordsDbById[record.id] = record
        }

        val recordsToInsert = ArrayList<TimeRecord>()
        val recordsToUpdate = ArrayList<TimeRecord>()
        var recordDb: TimeRecordEntity
        for (record in records) {
            val recordId = record.id
            if (recordsDbById.containsKey(recordId)) {
                recordDb = recordsDbById[recordId]!!
                //record.dbId = recordDb.dbId
                recordsToUpdate.add(record)
            } else {
                recordsToInsert.add(record)
            }
            recordsDbById.remove(recordId)
        }

        val recordsToDelete = recordsDbById.values
        recordsDao.delete(recordsToDelete)

        val recordIds = recordsDao.insert(recordsToInsert.map { toTimeRecordEntity(it) })
        //for (i in recordIds.indices) {
        //    recordsToInsert[i].dbId = recordIds[i]
        //}

        recordsDao.update(recordsToUpdate.map { toTimeRecordEntity(it) })
    }

    protected fun loadFormFromDb() {
        Timber.v("loadFormFromDb")
        val db = TrackerDatabase.getDatabase(this)

        loadProjects(db)
        loadTasks(db)
        loadProjectTaskKeys(db)
        loadRecords(db)
    }

    private fun loadProjects(db: TrackerDatabase) {
        val projectsDao = db.projectDao()
        val projectsDb = projectsDao.queryAll()
        projects.clear()
        projects.addAll(projectsDb)
        this.projectEmpty = projects.firstOrNull { it.isEmpty() } ?: projectEmpty
    }

    private fun loadTasks(db: TrackerDatabase) {
        val tasksDao = db.taskDao()
        val tasksDb = tasksDao.queryAll()
        tasks.clear()
        tasks.addAll(tasksDb)
        this.taskEmpty = tasks.firstOrNull { it.isEmpty() } ?: taskEmpty
    }

    private fun loadProjectTaskKeys(db: TrackerDatabase) {
        val projectsById: Map<Long, Project> = projects.map { project -> (project.id to project) }.toMap()
        val tasksById: Map<Long, ProjectTask> = tasks.map { task -> (task.id to task) }.toMap()
        projects.forEach { project -> project.clearTasks() }

        val projectTasksDao = db.projectTaskKeyDao()
        val keysDb = projectTasksDao.queryAll()

        keysDb.forEach { key ->
            val project = projectsById[key.projectId]
            val task = tasksById[key.taskId]
            if ((project != null) && (task != null)) {
                project.addTask(task)
            }
        }
    }

    private fun loadRecords(db: TrackerDatabase) {
        val recordsDao = db.timeRecordDao()
        val recordsDb = recordsDao.queryAll()
        records.clear()
        records.addAll(recordsDb.map { toTimeRecord(it, user, projects, tasks) })
        //TODO this.record = records.firstOrNull { it.isEmpty() } ?: record
    }

}