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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.tikalk.worktracker.BuildConfig
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.LoginActivity
import com.tikalk.worktracker.db.TrackerDatabase
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.ProjectTaskKey
import com.tikalk.worktracker.model.User
import com.tikalk.worktracker.model.time.TaskRecordStatus
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.model.time.TimeTotals
import com.tikalk.worktracker.model.time.isNullOrEmpty
import com.tikalk.worktracker.net.InternetActivity
import com.tikalk.worktracker.net.TimeTrackerServiceFactory
import com.tikalk.worktracker.preference.TimeTrackerPrefs
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_time_list.*
import kotlinx.android.synthetic.main.progress.*
import kotlinx.android.synthetic.main.time_totals.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.collections.ArrayList

class TimeListActivity : InternetActivity(),
    TimeListAdapter.OnTimeListListener {

    companion object {
        private const val REQUEST_AUTHENTICATE = 0x109
        private const val REQUEST_EDIT = 0xED17
        private const val REQUEST_STOPPED = 0x5706

        private const val STATE_DATE = "date"
        private const val STATE_RECORD = "record"
        private const val STATE_LIST = "records"
        private const val STATE_TOTALS = "totals"

        const val ACTION_STOP = BuildConfig.APPLICATION_ID + ".STOP"

        const val EXTRA_PROJECT_ID = TimeEditActivity.EXTRA_PROJECT_ID
        const val EXTRA_TASK_ID = TimeEditActivity.EXTRA_TASK_ID
        const val EXTRA_START_TIME = TimeEditActivity.EXTRA_START_TIME
        const val EXTRA_FINISH_TIME = TimeEditActivity.EXTRA_FINISH_TIME
    }

    private val context: Context = this
    private var intentLater: Intent? = null

    // UI references
    private var datePickerDialog: DatePickerDialog? = null
    private var menuFavorite: MenuItem? = null

    private lateinit var prefs: TimeTrackerPrefs

    private val disposables = CompositeDisposable()
    private val date = Calendar.getInstance()
    private var user = User("")
    private var record = TimeRecord(user, Project(""), ProjectTask(""))
    private val projects = ArrayList<Project>()
    private val tasks = ArrayList<ProjectTask>()
    private val listAdapter = TimeListAdapter(this)
    private val listItems = ArrayList<TimeRecord>()
    private var projectEmpty: Project = Project.EMPTY
    private var taskEmpty: ProjectTask = ProjectTask.EMPTY
    private var timer: Disposable? = null
    private lateinit var gestureDetector: GestureDetector
    private var totals = TimeTotals()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = TimeTrackerPrefs(this)

        // Set up the form.
        setContentView(R.layout.activity_time_list)

        user.username = prefs.userCredentials.login
        user.email = user.username

        project_input.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(adapterView: AdapterView<*>) {
                projectItemSelected(projectEmpty)
            }

            override fun onItemSelected(adapterView: AdapterView<*>, view: View?, position: Int, id: Long) {
                val project = adapterView.adapter.getItem(position) as Project
                projectItemSelected(project)
            }
        }
        task_input.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(adapterView: AdapterView<*>) {
                taskItemSelected(taskEmpty)
            }

            override fun onItemSelected(adapterView: AdapterView<*>, view: View?, position: Int, id: Long) {
                val task = adapterView.adapter.getItem(position) as ProjectTask
                taskItemSelected(task)
            }
        }
        date_input.setOnClickListener { pickDate() }
        action_start.setOnClickListener { startTimer() }
        action_stop.setOnClickListener { stopTimer() }
        fab_add.setOnClickListener { addTime() }

        list.adapter = listAdapter
        // Disable swiping on item in favour of swiping days.
        //val swipeHandler = TimeListSwipeHandler(this)
        //val itemTouchHelper = ItemTouchHelper(swipeHandler)
        //itemTouchHelper.attachToRecyclerView(list)
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
                val vx = Math.abs(velocityX)
                val vy = Math.abs(velocityY)
                if ((vx > vy) && (vx > 500)) {
                    if (velocityX < 0) {    // Fling from right to left.
                        if (isLocaleRTL()) {
                            navigateYesterday()
                        } else {
                            navigateTomorrow()
                        }
                    } else {
                        if (isLocaleRTL()) {
                            navigateTomorrow()
                        } else {
                            navigateYesterday()
                        }
                    }
                    return true
                }
                return super.onFling(e1, e2, velocityX, velocityY)
            }
        })
        list.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                return gestureDetector.onTouchEvent(event)
            }
        })

        if (savedInstanceState == null) {
            fetchPage(date)
        } else {
            date.timeInMillis = savedInstanceState.getLong(STATE_DATE, date.timeInMillis)
            loadPage()
        }
        handleIntent(intent, savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        handleIntent(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.clear()
        menuInflater.inflate(R.menu.time_list, menu)
        menuFavorite = menu.findItem(R.id.menu_favorite)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_date -> {
                pickDate()
                return true
            }
            R.id.menu_favorite -> {
                markFavorite()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun fetchPage(date: Calendar) {
        val dateFormatted = formatSystemDate(date)
        Timber.d("fetchPage $dateFormatted")
        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        showProgress(true)

        // Fetch from local database.
        loadPage()

        // Fetch from remote server.
        val authToken = prefs.basicCredentials.authToken()
        val service = TimeTrackerServiceFactory.createPlain(authToken)

        service.fetchTimes(dateFormatted)
            .subscribeOn(Schedulers.io())
            //.observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                runOnUiThread { showProgress(false) }

                if (this.date != date) {
                    this.date.timeInMillis = date.timeInMillis
                }
                if (isValidResponse(response)) {
                    val body = response.body()!!
                    populateForm(body, date)
                    populateList(body, date)
                } else {
                    authenticate(true)
                }
            }, { err ->
                Timber.e(err, "Error fetching page: ${err.message}")
            })
            .addTo(disposables)
    }

    /** Populate the list. */
    private fun populateList(html: String, date: Calendar) {
        val records = ArrayList<TimeRecord>()
        val doc: Document = Jsoup.parse(html)
        val table = findTable(doc)

        val form = doc.selectFirst("form[name='timeRecordForm']")

        val inputProjects = form.selectFirst("select[name='project']")
        populateProjects(doc, inputProjects, projects)

        val inputTasks = form.selectFirst("select[name='task']")
        populateTasks(doc, inputTasks, tasks)

        // The first row of the table is the header
        if (table != null) {
            // loop through all the rows and parse each record
            val rows = table.getElementsByTag("tr")
            for (tr in rows) {
                val record = parseRecord(tr)
                if (record != null) {
                    records.add(record)
                }
            }
        }
        populateTotals(doc, form, totals)

        runOnUiThread {
            date_input.text = DateUtils.formatDateTime(context, date.timeInMillis, DateUtils.FORMAT_SHOW_DATE)
            bindList(records, totals)
        }
    }

    private fun bindList(records: List<TimeRecord>, totals: TimeTotals) {
        listItems.clear()
        listItems.addAll(records)
        listAdapter.submitList(records)

        val context: Context = this
        val timeBuffer = StringBuilder(20)
        val timeFormatter = Formatter(timeBuffer, Locale.getDefault())

        if (totals.daily == TimeTotals.UNKNOWN) {
            day_total_label.visibility = View.INVISIBLE
            day_total.text = null
        } else {
            day_total_label.visibility = View.VISIBLE
            day_total.text = formatElapsedTime(context, timeFormatter, totals.daily).toString()
        }
        if (totals.weekly == TimeTotals.UNKNOWN) {
            week_total_label.visibility = View.INVISIBLE
            week_total.text = null
        } else {
            timeBuffer.setLength(0)
            week_total_label.visibility = View.VISIBLE
            week_total.text = formatElapsedTime(context, timeFormatter, totals.weekly).toString()
        }
        if (totals.monthly == TimeTotals.UNKNOWN) {
            month_total_label.visibility = View.INVISIBLE
            month_total.text = null
        } else {
            timeBuffer.setLength(0)
            month_total_label.visibility = View.VISIBLE
            month_total.text = formatElapsedTime(context, timeFormatter, totals.monthly).toString()
        }
        if (totals.remaining == TimeTotals.UNKNOWN) {
            remaining_quota_label.visibility = View.INVISIBLE
            remaining_quota.text = null
        } else {
            timeBuffer.setLength(0)
            remaining_quota_label.visibility = View.VISIBLE
            remaining_quota.text = formatElapsedTime(context, timeFormatter, totals.remaining).toString()
        }
    }

    private fun authenticate(immediate: Boolean = false) {
        runOnUiThread { showProgress(true) }
        val intent = Intent(context, LoginActivity::class.java)
        intent.putExtra(LoginActivity.EXTRA_SUBMIT, immediate)
        startActivityForResult(intent, REQUEST_AUTHENTICATE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_AUTHENTICATE -> if (resultCode == RESULT_OK) {
                user.username = prefs.userCredentials.login
                user.email = user.username
                record.user = user
                // Fetch the list for the user.
                fetchPage(date)
            }
            REQUEST_EDIT -> if (resultCode == RESULT_OK) {
                intent.action = null
                // Refresh the list with the edited item.
                fetchPage(date)
            }
            REQUEST_STOPPED -> if (resultCode == RESULT_OK) {
                stopTimerCommit()
                intent.action = null
                // Refresh the list with the edited item.
                fetchPage(date)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(STATE_DATE, date.timeInMillis)
        outState.putParcelable(STATE_RECORD, record)
        outState.putParcelableArrayList(STATE_LIST, listItems)
        outState.putParcelable(STATE_TOTALS, totals)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        date.timeInMillis = savedInstanceState.getLong(STATE_DATE)
        val recordParcel = savedInstanceState.getParcelable<TimeRecord>(STATE_RECORD)
        val list = savedInstanceState.getParcelableArrayList<TimeRecord>(STATE_LIST)
        val totals = savedInstanceState.getParcelable<TimeTotals>(STATE_TOTALS)

        if (recordParcel != null) {
            record.project = recordParcel.project
            record.task = recordParcel.task
            record.start = recordParcel.start
            populateForm(record)
        }
        if (totals != null) {
            this.totals = totals
        }
        if (list != null) {
            bindList(list, this.totals)
        }
    }

    override fun onRecordClick(record: TimeRecord) {
        editRecord(record)
    }

    override fun onRecordSwipe(record: TimeRecord) {
        deleteRecord(record)
    }

    private fun pickDate() {
        if (datePickerDialog == null) {
            val cal = date
            val listener = DatePickerDialog.OnDateSetListener { picker, year, month, day ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, day)
                fetchPage(cal)
            }
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val day = cal.get(Calendar.DAY_OF_MONTH)
            datePickerDialog = DatePickerDialog(context, listener, year, month, day)
        }
        datePickerDialog!!.show()
    }

    /**
     * Shows the progress UI and hides the list.
     */
    private fun showProgress(show: Boolean) {
        val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        list.visibility = if (show) View.GONE else View.VISIBLE
        list.animate().setDuration(shortAnimTime).alpha(
            (if (show) 0 else 1).toFloat()).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                list.visibility = if (show) View.GONE else View.VISIBLE
            }
        })

        progress.visibility = if (show) View.VISIBLE else View.GONE
        progress.animate().setDuration(shortAnimTime).alpha(
            (if (show) 1 else 0).toFloat()).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                progress.visibility = if (show) View.VISIBLE else View.GONE
            }
        })

        fab_add.isEnabled = !show
    }

    private fun addTime() {
        val intent = Intent(context, TimeEditActivity::class.java)
        intent.putExtra(TimeEditActivity.EXTRA_DATE, date.timeInMillis)
        startActivityForResult(intent, REQUEST_EDIT)
    }

    /**
     * Find the first table whose first row has both class="tableHeader" and labels 'Project' and 'Task' and 'Start'
     */
    private fun findTable(doc: Document): Element? {
        val body = doc.body()
        val tables = body.select("table")
        var rows: Elements
        var tr: Element
        var cols: Elements
        var td: Element
        var classAttr: String
        var label: String

        for (table in tables) {
            rows = table.getElementsByTag("tr")
            tr = rows.first()
            if (tr.childNodeSize() < 6) {
                continue
            }
            cols = tr.getElementsByTag("td")
            td = cols[0]
            classAttr = td.attr("class")
            label = td.ownText()
            if ((classAttr != "tableHeader") || (label != "Project")) {
                continue
            }
            td = cols[1]
            classAttr = td.attr("class")
            label = td.ownText()
            if ((classAttr != "tableHeader") || (label != "Task")) {
                continue
            }
            td = cols[2]
            classAttr = td.attr("class")
            label = td.ownText()
            if ((classAttr != "tableHeader") || (label != "Start")) {
                continue
            }
            return table
        }

        return null
    }

    private fun parseRecord(row: Element): TimeRecord? {
        val cols = row.getElementsByTag("td")

        val tdProject = cols[0]
        if (tdProject.attr("class") == "tableHeader") {
            return null
        }
        val projectName = tdProject.ownText()
        val project = parseRecordProject(projectName) ?: return null

        val tdTask = cols[1]
        val taskName = tdTask.ownText()
        val task = parseRecordTask(project, taskName) ?: return null

        val tdStart = cols[2]
        val startText = tdStart.ownText()
        val start = parseRecordTime(startText) ?: return null

        val tdFinish = cols[3]
        val finishText = tdFinish.ownText()
        val finish = parseRecordTime(finishText) ?: return null

        val tdNote = cols[5]
        val noteText = tdNote.text()
        val note = parseRecordNote(noteText)

        val tdEdit = cols[6]
        val editLink = tdEdit.child(0).attr("href")
        val id = parseRecordId(editLink)

        return TimeRecord(user, project, task, start, finish, note, TaskRecordStatus.CURRENT, id)
    }

    private fun parseRecordProject(name: String): Project? {
        return projects.find { name == it.name }
    }

    private fun parseRecordTask(project: Project, name: String): ProjectTask? {
        return tasks.find { (it.id in project.taskIds) && (name == it.name) }
    }

    private fun parseRecordTime(text: String): Calendar? {
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

    private fun populateProjects(doc: Document, select: Element, target: MutableList<Project>) {
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

        val db = TrackerDatabase.getDatabase(this)
        val projectsDao = db.projectDao()
        projectsDao.deleteAll()
        Observable.fromArray(projectsDao.insert(projects))
            .subscribe(
                { ids ->
                    for (i in 0 until ids.size) {
                        projects[i].dbId = ids[i]
                    }

                    populateTaskIds(doc, projects)

                    target.clear()
                    target.addAll(projects)
                },
                { err -> Timber.e(err, "Error inserting projects into db: ${err.message}") }
            )
            .addTo(disposables)
    }

    private fun populateTasks(doc: Document, select: Element, target: MutableList<ProjectTask>) {
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

        val db = TrackerDatabase.getDatabase(this)
        val tasksDao = db.taskDao()
        tasksDao.deleteAll()
        Observable.fromArray(tasksDao.insert(tasks))
            .subscribe(
                { ids ->
                    for (i in 0 until ids.size) {
                        tasks[i].dbId = ids[i]
                    }

                    target.clear()
                    target.addAll(tasks)
                },
                { err -> Timber.e(err, "Error inserting tasks into db: ${err.message}") }
            )
            .addTo(disposables)
    }

    private fun populateTaskIds(doc: Document, projects: List<Project>) {
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

        val db = TrackerDatabase.getDatabase(this)
        val projectTasksDao = db.projectTaskKeyDao()
        projectTasksDao.deleteAll()
        Observable.fromArray(projectTasksDao.insert(pairs))
            .subscribe(
                { ids ->
                    for (i in 0 until ids.size) {
                        pairs[i].dbId = ids[i]
                    }
                },
                { err -> Timber.e(err, "Error inserting project-task pair into db: ${err.message}") }
            )
            .addTo(disposables)
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

    private fun editRecord(record: TimeRecord, requestId: Int = REQUEST_EDIT) {
        val intent = Intent(context, TimeEditActivity::class.java)
        if ((record.id == 0L) && !record.isEmpty()) {
            intent.putExtra(TimeEditActivity.EXTRA_DATE, record.startTime)
            intent.putExtra(TimeEditActivity.EXTRA_PROJECT_ID, record.project.id)
            intent.putExtra(TimeEditActivity.EXTRA_TASK_ID, record.task.id)
            intent.putExtra(TimeEditActivity.EXTRA_START_TIME, record.startTime)
            intent.putExtra(TimeEditActivity.EXTRA_FINISH_TIME, record.finishTime)
        } else {
            intent.putExtra(TimeEditActivity.EXTRA_DATE, date.timeInMillis)
            intent.putExtra(TimeEditActivity.EXTRA_RECORD, record.id)
        }
        startActivityForResult(intent, requestId)
    }

    private fun deleteRecord(record: TimeRecord) {
        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        showProgress(true)

        val authToken = prefs.basicCredentials.authToken()
        val service = TimeTrackerServiceFactory.createPlain(authToken)

        service.deleteTime(record.id)
            .subscribeOn(Schedulers.io())
            //.observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { response ->
                    runOnUiThread { showProgress(false) }

                    if (isValidResponse(response)) {
                        val body = response.body()!!
                        populateForm(body, date)
                        populateList(body, date)
                    } else {
                        authenticate(true)
                    }
                },
                { err ->
                    Timber.e(err, "Error deleting record: ${err.message}")
                }
            )
            .addTo(disposables)
    }

    /** Populate the record and then bind the form. */
    private fun populateForm(html: String, date: Calendar) {
        val doc: Document = Jsoup.parse(html)

        val form = doc.selectFirst("form[name='timeRecordForm']")

        val inputProjects = form.selectFirst("select[name='project']")
        populateProjects(doc, inputProjects, projects)
        record.project = findSelectedProject(inputProjects, projects)

        val inputTasks = form.selectFirst("select[name='task']")
        populateTasks(doc, inputTasks, tasks)
        record.task = findSelectedTask(inputTasks, tasks)

        val recordStarted = getStartedRecord()
        runOnUiThread { populateForm(recordStarted) }
    }

    private fun populateForm(recordStarted: TimeRecord?) {
        Timber.v("populateForm $recordStarted")
        if (recordStarted.isNullOrEmpty()) {
            val projectFavorite = prefs.getFavoriteProject()
            if (projectFavorite != 0L) {
                record.project = projects.firstOrNull { it.id == projectFavorite } ?: record.project
            }
            val taskFavorite = prefs.getFavoriteTask()
            if (taskFavorite != 0L) {
                record.task = tasks.firstOrNull { it.id == taskFavorite } ?: record.task
            }
        } else {
            record.project = projects.firstOrNull { it.id == recordStarted!!.project.id }
                ?: projectEmpty
            record.task = tasks.firstOrNull { it.id == recordStarted!!.task.id } ?: taskEmpty
            record.start = recordStarted!!.start
        }

        bindForm(record)
    }

    private fun bindForm(record: TimeRecord) {
        Timber.v("bindForm record=$record")
        project_input.adapter = ArrayAdapter<Project>(context, android.R.layout.simple_list_item_1, projects.toTypedArray())
        if (projects.isNotEmpty()) {
            project_input.setSelection(projects.indexOf(record.project))
        }
        task_input.adapter = ArrayAdapter<ProjectTask>(context, android.R.layout.simple_list_item_1, tasks.toTypedArray())
        if (tasks.isNotEmpty()) {
            task_input.setSelection(tasks.indexOf(record.task))
        }
        project_input.requestFocus()

        val startTime = record.startTime
        if (startTime <= 0L) {
            project_input.isEnabled = true
            task_input.isEnabled = true
            action_switcher.displayedChild = 0
        } else {
            project_input.isEnabled = false
            task_input.isEnabled = false
            action_switcher.displayedChild = 1

            maybeStartTimer()
            maybeStopTimer()
        }
    }

    private fun findSelectedProject(project: Element, projects: List<Project>): Project {
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

    private fun findSelectedTask(task: Element, tasks: List<ProjectTask>): ProjectTask {
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

    private fun startTimer() {
        Timber.v("startTimer")
        val now = System.currentTimeMillis()
        record.startTime = now

        val context: Context = this
        val service = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_PROJECT_ID, record.project.id)
            putExtra(TimerService.EXTRA_PROJECT_NAME, record.project.name)
            putExtra(TimerService.EXTRA_TASK_ID, record.task.id)
            putExtra(TimerService.EXTRA_TASK_NAME, record.task.name)
            putExtra(TimerService.EXTRA_START_TIME, record.startTime)
            putExtra(TimerService.EXTRA_NOTIFICATION, false)
        }
        startService(service)

        bindForm(record)
    }

    private fun stopTimer() {
        Timber.v("stopTimer")
        val now = System.currentTimeMillis()
        if (record.finish == null) {
            record.finishTime = now
        }

        val context: Context = this
        val service = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        stopService(service)

        editRecord(record, REQUEST_STOPPED)
    }

    private fun stopTimerCommit() {
        Timber.v("stopTimerCommit")
        timer?.dispose()

        record.start = null
        record.finish = null
        prefs.stopRecord()
        bindForm(record)
    }

    private fun filterTasks(project: Project) {
        val filtered = tasks.filter { it.id in project.taskIds }
        val options = ArrayList<ProjectTask>(filtered.size + 1)
        options.add(taskEmpty)
        options.addAll(filtered)
        task_input.adapter = ArrayAdapter<ProjectTask>(context, android.R.layout.simple_list_item_1, options)
        task_input.setSelection(options.indexOf(record.task))
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
        timer_text.text = DateUtils.formatElapsedTime(elapsedSeconds)
    }

    private fun projectItemSelected(project: Project) {
        record.project = project
        filterTasks(project)
        action_start.isEnabled = (record.project.id > 0L) && (record.task.id > 0L)
    }

    private fun taskItemSelected(task: ProjectTask) {
        record.task = task
        action_start.isEnabled = (record.project.id > 0L) && (record.task.id > 0L)
    }

    private fun handleIntent(intent: Intent, savedInstanceState: Bundle? = null) {
        intentLater = if (intent.action == ACTION_STOP) intent else null
    }

    private fun maybeStopTimer() {
        if (intentLater?.action == ACTION_STOP) {
            intentLater = null
            stopTimer()
        }
    }

    private fun getStartedRecord(): TimeRecord? {
        val started = prefs.getStartedRecord()
        if (started != null) {
            return started
        }

        val extras = intentLater?.extras
        if (extras != null) {
            val projectId = extras.getLong(EXTRA_PROJECT_ID)
            val taskId = extras.getLong(EXTRA_TASK_ID)
            val startTime = extras.getLong(EXTRA_START_TIME)
            val finishTime = extras.getLong(EXTRA_FINISH_TIME, System.currentTimeMillis())

            val project = projects.firstOrNull { it.id == projectId } ?: projectEmpty
            val task = tasks.firstOrNull { it.id == taskId } ?: taskEmpty

            val record = TimeRecord(user, project, task)
            if (startTime > 0L) {
                record.startTime = startTime
            }
            if (finishTime > 0L) {
                record.finishTime = finishTime
            }
            return record
        }

        return null
    }

    private fun markFavorite() {
        prefs.setFavorite(record)
    }

    private fun navigateTomorrow() {
        val cal = date
        cal.add(Calendar.DATE, 1)
        fetchPage(cal)
    }

    private fun navigateYesterday() {
        val cal = date
        cal.add(Calendar.DATE, -1)
        fetchPage(cal)
    }

    private fun isLocaleRTL(): Boolean {
        return Locale.getDefault().language == "iw"
    }


    private fun populateTotals(doc: Document, parent: Element?, totals: TimeTotals) {
        totals.clear(true)
        if (parent == null) {
            return
        }

        val table = findTotalsTable(doc, parent) ?: return
        val cells = table.getElementsByTag("td")
        for (td in cells) {
            val text = td.text()
            val value: String
            when {
                text.startsWith("Day total:") -> {
                    value = text.substring(text.indexOf(':') + 1).trim()
                    totals.daily = parseHours(value) ?: TimeTotals.UNKNOWN
                }
                text.startsWith("Week total:") -> {
                    value = text.substring(text.indexOf(':') + 1).trim()
                    totals.weekly = parseHours(value) ?: TimeTotals.UNKNOWN
                }
                text.startsWith("Month total:") -> {
                    value = text.substring(text.indexOf(':') + 1).trim()
                    totals.monthly = parseHours(value) ?: TimeTotals.UNKNOWN
                }
                text.startsWith("Remaining quota:") -> {
                    value = text.substring(text.indexOf(':') + 1).trim()
                    totals.remaining = parseHours(value) ?: TimeTotals.UNKNOWN
                }
            }
        }
    }

    private fun findTotalsTable(doc: Document, parent: Element?): Element? {
        val body = doc.body()
        val tables = body.select("table")
        var rows: Elements
        var tr: Element
        var cols: Elements
        var td: Element
        var label: String

        for (table in tables) {
            if ((parent != null) && (table.parent() != parent)) {
                continue
            }
            rows = table.getElementsByTag("tr")
            tr = rows.first()
            if (tr.childNodeSize() < 1) {
                continue
            }
            cols = tr.getElementsByTag("td")
            td = cols.first()
            label = td.ownText()
            if (label.startsWith("Week total:")) {
                return table
            }
        }

        return null
    }

    private fun loadPage() {
        Timber.v("loadPage")
        val db = TrackerDatabase.getDatabase(this)
        val projectsDao = db.projectDao()
        val tasksDao = db.taskDao()
        val projectTasksDao = db.projectTaskKeyDao()

        this.projects.clear()
        this.tasks.clear()

        projectsDao.queryAll()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { projects ->
                    this.projects.addAll(projects)
                    projectEmpty = this.projects.firstOrNull { it.isEmpty() } ?: projectEmpty
                    populateForm(record)
                    showProgress(false)
                },
                { err ->
                    Timber.e(err, "Error fetching projects from db: ${err.message}")
                })
            .addTo(disposables)

        tasksDao.queryAll()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { tasks ->
                    this.tasks.addAll(tasks)
                    taskEmpty = this.tasks.firstOrNull { it.isEmpty() } ?: taskEmpty
                    populateForm(record)
                    showProgress(false)
                },
                { err ->
                    Timber.e(err, "Error fetching tasks from db: ${err.message}")
                })
            .addTo(disposables)

        projectTasksDao.queryAll()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { pairs ->
                    if (projects.isNotEmpty()) {
                        projects.forEach { project ->
                            val pairsForProject = pairs.filter { it.projectId == project.id }
                            project.addKeys(pairsForProject)
                        }
                        populateForm(record)
                    }
                    showProgress(false)
                },
                { err ->
                    Timber.e(err, "Error fetching tasks from db: ${err.message}")
                })
            .addTo(disposables)
    }
}
