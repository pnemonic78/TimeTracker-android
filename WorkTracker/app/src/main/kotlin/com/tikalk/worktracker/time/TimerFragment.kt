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

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.tikalk.app.findParentFragment
import com.tikalk.app.runOnUiThread
import com.tikalk.html.selectByName
import com.tikalk.html.value
import com.tikalk.worktracker.BuildConfig
import com.tikalk.worktracker.R
import com.tikalk.worktracker.app.TrackerFragment
import com.tikalk.worktracker.db.*
import com.tikalk.worktracker.model.*
import com.tikalk.worktracker.model.time.TimeRecord
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_timer.*
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.FormElement
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.math.max

class TimerFragment : TimeFormFragment() {

    private var timer: Disposable? = null
    private var projectEntities: LiveData<List<ProjectWithTasks>> = MutableLiveData<List<ProjectWithTasks>>()
    private var taskEntities: LiveData<List<ProjectTask>> = MutableLiveData<List<ProjectTask>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_timer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        projectInput.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(adapterView: AdapterView<*>) {
                projectItemSelected(projectEmpty)
            }

            override fun onItemSelected(adapterView: AdapterView<*>, view: View?, position: Int, id: Long) {
                val project = adapterView.adapter.getItem(position) as Project
                projectItemSelected(project)
            }
        }
        taskInput.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(adapterView: AdapterView<*>) {
                taskItemSelected(taskEmpty)
            }

            override fun onItemSelected(adapterView: AdapterView<*>, view: View?, position: Int, id: Long) {
                val task = adapterView.adapter.getItem(position) as ProjectTask
                taskItemSelected(task)
            }
        }

        actionStart.setOnClickListener { startTimer() }
        actionStop.setOnClickListener { stopTimer() }
    }

    @MainThread
    override fun bindForm(record: TimeRecord) {
        Timber.i("bindForm record=$record")
        val context = this.context ?: return
        if (!isVisible) return

        // Populate the tasks spinner before projects so that it can be filtered.
        val taskItems = arrayOf(taskEmpty)
        taskInput.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, taskItems)

        val projects = projectsData.value
        bindProjects(context, record, projects)

        val startTime = record.startTime
        if (startTime <= TimeRecord.NEVER) {
            projectInput.isEnabled = true
            taskInput.isEnabled = true
            actionSwitcher.displayedChild = CHILD_START
            activity?.invalidateOptionsMenu()
        } else {
            projectInput.isEnabled = false
            taskInput.isEnabled = false
            actionSwitcher.displayedChild = CHILD_STOP
            activity?.invalidateOptionsMenu()

            maybeStartTimer()
        }
    }

    private fun bindProjects(context: Context, record: TimeRecord, projects: List<Project>?) {
        Timber.i("bindProjects record=$record projects=$projects")
        val projectItems = projects?.toTypedArray() ?: emptyArray()
        if (projectInput == null) return
        projectInput.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, projectItems)
        if (projectItems.isNotEmpty()) {
            projectInput.setSelection(max(0, findProject(projectItems, record.project)))
            projectItemSelected(record.project)
        }
        projectInput.requestFocus()
    }

    private fun startTimer() {
        Timber.i("startTimer")
        val context = this.context ?: return
        val now = System.currentTimeMillis()
        record.startTime = now

        TimerWorker.startTimer(context, record)

        bindForm(record)
    }

    fun stopTimer() {
        Timber.i("stopTimer")
        if (!isVisible or !isResumed) {
            // Save for "run" later.
            val args = arguments ?: Bundle()
            args.putString(EXTRA_ACTION, ACTION_STOP)
            if (arguments == null) {
                arguments = args
            }
            return
        }

        val recordStarted = getStartedRecord()
        Timber.i("stopTimer recordStarted=$recordStarted")
        if (recordStarted != null) {
            setRecordValue(recordStarted)
        }
        if (record.finishTime <= TimeRecord.NEVER) {
            record.finishTime = System.currentTimeMillis()
        }

        editRecord(record)
    }

    private fun stopTimerCommit() {
        Timber.i("stopTimerCommit")
        timer?.dispose()

        record.start = null
        record.finish = null
        preferences.stopRecord()
        val args = arguments
        if (args != null) {
            args.remove(EXTRA_PROJECT_ID)
            args.remove(EXTRA_TASK_ID)
            args.remove(EXTRA_START_TIME)
            args.remove(EXTRA_FINISH_TIME)
        }

        bindForm(record)
    }

    private fun filterTasks(project: Project) {
        Timber.d("filterTasks project=$project")
        val context = this.context ?: return
        val filtered = project.tasks
        val options = ArrayList<ProjectTask>(filtered.size + 1)
        options.add(taskEmpty)
        options.addAll(filtered)
        if (taskInput == null) return
        taskInput.adapter = ArrayAdapter<ProjectTask>(context, android.R.layout.simple_list_item_1, options)
        taskInput.setSelection(findTask(options, record.task))
    }

    private fun maybeStartTimer() {
        val timer = this.timer
        if ((timer == null) || timer.isDisposed) {
            this.timer = Observable.interval(1L, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { updateTimer() }
                .addTo(disposables)
        }
        updateTimer()
    }

    private fun updateTimer() {
        val now = System.currentTimeMillis()
        val elapsedSeconds = (now - record.startTime) / DateUtils.SECOND_IN_MILLIS
        timerText?.text = DateUtils.formatElapsedTime(elapsedSeconds)
    }

    private fun projectItemSelected(project: Project) {
        Timber.d("projectItemSelected project=$project")
        setRecordProject(project)
        if (!isVisible) return
        filterTasks(project)
        actionStart.isEnabled = (record.project.id > TikalEntity.ID_NONE) && (record.task.id > TikalEntity.ID_NONE)
    }

    private fun taskItemSelected(task: ProjectTask) {
        Timber.d("taskItemSelected task=$task")
        setRecordTask(task)
        if (!isVisible) return
        actionStart.isEnabled = (record.project.id > TikalEntity.ID_NONE) && (record.task.id > TikalEntity.ID_NONE)
    }

    private fun getStartedRecord(args: Bundle? = arguments): TimeRecord? {
        val started = preferences.getStartedRecord()
        if (started != null) {
            return started
        }

        if (args != null) {
            if (args.containsKey(EXTRA_PROJECT_ID) and args.containsKey(EXTRA_TASK_ID)) {
                val projectId = args.getLong(EXTRA_PROJECT_ID)
                val taskId = args.getLong(EXTRA_TASK_ID)
                val startTime = args.getLong(EXTRA_START_TIME)
                val finishTime = args.getLong(EXTRA_FINISH_TIME, System.currentTimeMillis())

                val projects = projectsData.value
                val project = projects?.firstOrNull { it.id == projectId } ?: projectEmpty
                val tasks = tasksData.value
                val task = tasks?.firstOrNull { it.id == taskId } ?: taskEmpty

                val record = TimeRecord(TikalEntity.ID_NONE, project, task)
                if (startTime != TimeRecord.NEVER) {
                    record.startTime = startTime
                }
                if (finishTime != TimeRecord.NEVER) {
                    record.finishTime = finishTime
                }
                return record
            }
        }

        return null
    }

    fun populateForm(date: Calendar, doc: Document) {
        val form = findForm(doc) ?: return
        populateForm(date, doc, form)
        populateForm(record)

        runOnUiThread { bindForm(record) }
    }

    private fun populateForm(date: Calendar, doc: Document, form: FormElement) {
        val inputProjects = form.selectByName("project") ?: return
        val inputTasks = form.selectByName("task") ?: return
        populateForm(date, doc, form, inputProjects, inputTasks)
    }

    private fun populateForm(date: Calendar, doc: Document, form: FormElement, inputProjects: Element, inputTasks: Element) {
        val projects = populateProjects(inputProjects, projectsData)
        val tasks = populateTasks(inputTasks, tasksData)
        populateTaskIds(doc, projects, tasks)

        setRecordProject(findSelectedProject(inputProjects, projects))
        setRecordTask(findSelectedTask(inputTasks, tasks))
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

    private fun findTaskIds(doc: Document): String? {
        val tokenStart = "var task_ids = new Array();"
        val tokenEnd = "// Prepare an array of task names."
        return findScript(doc, tokenStart, tokenEnd)
    }

    private fun findForm(doc: Document): FormElement? {
        return doc.selectFirst("form[name='timeRecordForm']") as FormElement?
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

    private fun populateTaskIds(doc: Document, projects: List<Project>, tasks: List<ProjectTask>) {
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

    private fun saveProjects(db: TrackerDatabase, projects: List<Project>) {
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

    private fun saveTasks(db: TrackerDatabase, tasks: List<ProjectTask>) {
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

    private fun saveProjectTaskKeys(db: TrackerDatabase, projects: List<Project>) {
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

    override fun populateForm(record: TimeRecord) {
        Timber.i("populateForm record=$record")
        val recordStarted = getStartedRecord() ?: TimeRecord.EMPTY
        Timber.i("populateForm recordStarted=$recordStarted")
        val projects = projectsData.value ?: return
        val tasks = tasksData.value ?: return
        if (recordStarted.project.isNullOrEmpty() and recordStarted.task.isNullOrEmpty()) {
            applyFavorite()
        } else if (!recordStarted.isEmpty()) {
            val recordStartedProjectId = recordStarted.project.id
            val recordStartedTaskId = recordStarted.task.id
            setRecordProject(projects.firstOrNull { it.id == recordStartedProjectId }
                ?: record.project)
            setRecordTask(tasks.firstOrNull { it.id == recordStartedTaskId } ?: record.task)
            record.start = recordStarted.start
        }
    }

    private fun editRecord(record: TimeRecord) {
        Timber.i("editRecord record=$record currentDestination=${findNavController().currentDestination?.label}")
        val parent = findParentFragment(TimeListFragment::class.java)
        if (parent != null) {
            parent.editRecord(record, true)
        } else {
            val args = Bundle()
            args.putLong(TimeEditFragment.EXTRA_PROJECT_ID, record.project.id)
            args.putLong(TimeEditFragment.EXTRA_TASK_ID, record.task.id)
            args.putLong(TimeEditFragment.EXTRA_START_TIME, record.startTime)
            args.putLong(TimeEditFragment.EXTRA_FINISH_TIME, record.finishTime)
            args.putLong(TimeEditFragment.EXTRA_RECORD_ID, record.id)
            parentFragmentManager.putFragment(args, TimeEditFragment.EXTRA_CALLER, caller ?: this)
            findNavController().navigate(R.id.action_timer_to_timeEdit, args)
        }
    }

    fun run() {
        Timber.i("run")
        loadForm()
            .subscribeOn(Schedulers.io())
            .subscribe({
                populateAndBind()
                runOnUiThread { handleArguments() }
            }, { err ->
                Timber.e(err, "Error loading page: ${err.message}")
            })
            .addTo(disposables)
    }

    private fun handleArguments() {
        Timber.d("handleArguments")
        val args = arguments
        if (args != null) {
            if (args.containsKey(EXTRA_ACTION)) {
                val action = args.getString(EXTRA_ACTION)
                if (action == ACTION_STOP) {
                    args.remove(EXTRA_ACTION)
                    if (args.getBoolean(EXTRA_COMMIT)) {
                        stopTimerCommit()
                    } else {
                        stopTimer()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        run()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(STATE_RECORD, record.toTimeRecordEntity())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val recordParcel = savedInstanceState.getParcelable<TimeRecordEntity>(STATE_RECORD)

        if (recordParcel != null) {
            val projects = projectsData.value
            val tasks = tasksData.value
            val record = recordParcel.toTimeRecord(projects, tasks)
            setRecordValue(record)
            populateForm(record)
            bindForm(record)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (view?.visibility == View.VISIBLE) {
            inflater.inflate(R.menu.timer, menu)
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (view?.visibility != View.VISIBLE) {
            return false
        }
        when (item.itemId) {
            R.id.menu_favorite -> {
                markFavorite()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_EDIT) {
            if (resultCode == RESULT_OK) {
                Timber.i("record processed")
                stopTimerCommit()
            } else {
                Timber.i("record edit cancelled")
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun authenticate(submit: Boolean) {
        // Parent fragment responsible for authentication.
    }

    override fun onProjectsUpdated(projects: List<Project>) {
        val context = this.context ?: return
        super.onProjectsUpdated(projects)
        bindProjects(context, record, projects)
    }

    private fun loadForm(): Single<Unit> {
        Timber.i("loadForm")
        return Single.fromCallable { loadFormFromDb(db) }
    }

    @Synchronized
    private fun loadFormFromDb(db: TrackerDatabase) {
        Timber.i("loadFormFromDb")
        loadProjectsWithTasks(db)
    }

    private fun loadProjectsWithTasks(db: TrackerDatabase) {
        if (projectEntities.value == null) {
            val projectsDao = db.projectDao()
            val projectsWithTasks = projectsDao.queryAllWithTasksLive()
            val tasksDao = db.taskDao()
            val tasksAll = tasksDao.queryAllLive()

            runOnUiThread {
                projectsWithTasks.observe(this, Observer<List<ProjectWithTasks>> { entities ->
                    val projectsDb = ArrayList<Project>()
                    val tasksDb = HashSet<ProjectTask>()
                    for (projectWithTasks in entities) {
                        val project = projectWithTasks.project
                        project.tasks = projectWithTasks.tasks
                        projectsDb.add(project)
                        tasksDb.addAll(projectWithTasks.tasks)
                    }
                    val projects = projectsDb.sortedBy { it.name }
                    projectsData.postValue(projects)

                    val tasks = tasksDb.sortedBy { it.name }
                    tasksData.postValue(tasks)
                })
                projectEntities.removeObservers(this)
                projectEntities = projectsWithTasks

                tasksAll.observe(this, Observer<List<ProjectTask>> { entities ->
                    val tasks = entities.sortedBy { it.name }
                    tasksData.postValue(tasks)
                })
                taskEntities.removeObservers(this)
                taskEntities = tasksAll
            }
        }
    }

    companion object {
        const val EXTRA_ACTION = TrackerFragment.EXTRA_ACTION
        const val EXTRA_CALLER = TrackerFragment.EXTRA_CALLER
        const val EXTRA_PROJECT_ID = TimeFormFragment.EXTRA_PROJECT_ID
        const val EXTRA_TASK_ID = TimeFormFragment.EXTRA_TASK_ID
        const val EXTRA_START_TIME = TimeFormFragment.EXTRA_START_TIME
        const val EXTRA_FINISH_TIME = TimeFormFragment.EXTRA_FINISH_TIME
        const val EXTRA_COMMIT = BuildConfig.APPLICATION_ID + ".COMMIT"

        const val ACTION_STOP = TrackerFragment.ACTION_STOP

        private const val REQUEST_EDIT = 0xED17

        private const val CHILD_START = 0
        private const val CHILD_STOP = 1
    }
}