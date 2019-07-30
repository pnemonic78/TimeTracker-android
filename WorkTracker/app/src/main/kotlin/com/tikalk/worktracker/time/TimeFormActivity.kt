package com.tikalk.worktracker.time

import android.os.Bundle
import com.tikalk.worktracker.db.TrackerDatabase
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.ProjectTaskKey
import com.tikalk.worktracker.model.User
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.net.InternetActivity
import com.tikalk.worktracker.preference.TimeTrackerPrefs
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function3
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
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
    protected var user = User("")
    protected var record = TimeRecord(user, Project(""), ProjectTask(""))
    protected val projects = ArrayList<Project>()
    protected val tasks = ArrayList<ProjectTask>()
    protected var projectEmpty: Project = Project.EMPTY
    protected var taskEmpty: ProjectTask = ProjectTask.EMPTY

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
        val pairs = ArrayList<ProjectTaskKey>()

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
                    val taskIds: List<Long> = matcher.group(2)
                        .split(",")
                        .map { it.toLong() }
                    val project = projects.find { it.id == projectId }
                    project?.apply {
                        addTasks(taskIds)
                        pairs.addAll(tasks.values)
                    }
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
        val projectsDao = db.projectDao()
        val tasksDao = db.taskDao()
        val projectTasksDao = db.projectTaskKeyDao()

        val projects = this.projects
        val tasks = this.tasks
        val keys = ArrayList<ProjectTaskKey>()
        projects.map { project -> keys.addAll(project.tasks.values) }

        projectsDao.queryAll()
            .subscribe { projectsDb ->
                val projectsDbById: MutableMap<Long, Project> = HashMap()
                for (project in projectsDb) {
                    projectsDbById[project.id] = project
                }

                val projectsToInsert = ArrayList<Project>()
                val projectsToUpdate = ArrayList<Project>()
                for (project in projects) {
                    val projectId = project.id
                    if (projectsDbById.containsKey(projectId)){
                        projectsToUpdate.add(project)
                    } else {
                        projectsToInsert.add(project)
                    }
                    projectsDbById.remove(projectId)
                }

                val projectsToDelete = projectsDbById.values
                projectsDao.delete(projectsToDelete)

                val projectIds = projectsDao.insert(projectsToInsert)
                for (i in 0 until projectIds.size) {
                    projectsToInsert[i].dbId = projectIds[i]
                }

                projectsDao.update(projectsToUpdate)
            }
            .addTo(disposables)

        tasksDao.queryAll()
            .subscribe { tasksDb ->
                val tasksDbById: MutableMap<Long, ProjectTask> = HashMap()
                for (task in tasksDb) {
                    tasksDbById[task.id] = task
                }

                val tasksToInsert = ArrayList<ProjectTask>()
                val tasksToUpdate = ArrayList<ProjectTask>()
                for (task in tasks) {
                    val taskId = task.id
                    if (tasksDbById.containsKey(taskId)){
                        tasksToUpdate.add(task)
                    } else {
                        tasksToInsert.add(task)
                    }
                    tasksDbById.remove(taskId)
                }

                val tasksToDelete = tasksDbById.values
                tasksDao.delete(tasksToDelete)

                val taskIds = tasksDao.insert(tasksToInsert)
                for (i in 0 until taskIds.size) {
                    tasksToInsert[i].dbId = taskIds[i]
                }

                tasksDao.update(tasksToUpdate)
            }
            .addTo(disposables)

        projectTasksDao.deleteAll()
        val keyIds = projectTasksDao.insert(keys)
        for (i in 0 until keyIds.size) {
            keys[i].dbId = keyIds[i]
        }
    }

    protected fun loadFormFromDb(): Single<Any> {
        Timber.v("loadPage")
        val db = TrackerDatabase.getDatabase(this)
        val projectsDao = db.projectDao()
        val tasksDao = db.taskDao()
        val projectTasksDao = db.projectTaskKeyDao()

        val zipper = Function3<List<Project>, List<ProjectTask>, List<ProjectTaskKey>, Any> { projects, tasks, pairs ->
            this.projects.clear()
            this.tasks.clear()

            this.projects.addAll(projects)
            this.projectEmpty = this.projects.firstOrNull { it.isEmpty() } ?: projectEmpty

            this.tasks.addAll(tasks)
            this.taskEmpty = this.tasks.firstOrNull { it.isEmpty() } ?: taskEmpty

            if (projects.isNotEmpty()) {
                projects.forEach { project ->
                    val pairsForProject = pairs.filter { it.projectId == project.id }
                    project.addKeys(pairsForProject)
                }
            }

            return@Function3 this
        }

        return Single.zip(
            projectsDao.queryAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()),
            tasksDao.queryAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()),
            projectTasksDao.queryAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()),
            zipper)
    }
}