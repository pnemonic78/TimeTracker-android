package com.tikalk.worktracker.time

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.ItemTouchHelper
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.LoginActivity
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.User
import com.tikalk.worktracker.model.time.TaskRecordStatus
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.net.InternetActivity
import com.tikalk.worktracker.net.TimeTrackerService
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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import retrofit2.Response
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.collections.ArrayList

class TimeListActivity : InternetActivity(),
    TimeListAdapter.OnTimeListListener {

    companion object {
        private const val REQUEST_AUTHENTICATE = 1
        private const val REQUEST_ADD = 2
        private const val REQUEST_EDIT = 3

        private const val STATE_DATE = "date"
        private const val STATE_PROJECTS = "projects"
        private const val STATE_TASKS = "tasks"
        private const val STATE_PROJECT_ID = "project_id"
        private const val STATE_TASK_ID = "task_id"
    }

    private val context: Context = this

    // UI references
    private var datePickerDialog: DatePickerDialog? = null

    private lateinit var prefs: TimeTrackerPrefs

    private val disposables = CompositeDisposable()
    private val date = Calendar.getInstance()
    private var user = User("")
    private var record = TimeRecord(user, Project(""), ProjectTask(""))
    private val projects = ArrayList<Project>()
    private val tasks = ArrayList<ProjectTask>()
    private val listAdapter = TimeListAdapter(this)
    private var projectEmpty: Project = Project.EMPTY
    private var taskEmpty: ProjectTask = ProjectTask.EMPTY
    private var timer: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = TimeTrackerPrefs(this)

        // Set up the form.
        setContentView(R.layout.activity_time_list)

        user.username = prefs.userCredentials.login
        user.email = user.username

        project_input.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(adapterView: AdapterView<*>) {
                record.project = projectEmpty
            }

            override fun onItemSelected(adapterView: AdapterView<*>, view: View?, position: Int, id: Long) {
                val project = adapterView.adapter.getItem(position) as Project
                projectItemSelected(project)
            }
        }
        task_input.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(adapterView: AdapterView<*>) {
                record.task = taskEmpty
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
        val swipeHandler = TimeListSwipeHandler(this)
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(list)

        val now = date.timeInMillis
        date.timeInMillis = savedInstanceState?.getLong(STATE_DATE, now) ?: now
        if (savedInstanceState == null) {
            fetchPage(date)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
    }

    override fun onStop() {
        super.onStop()
        maybeShowNotification()
    }

    override fun onStart() {
        super.onStart()
        hideNotification()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.clear()
        menuInflater.inflate(R.menu.time_list, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_date -> {
                pickDate()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun fetchPage(date: Calendar) {
        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        showProgress(true)

        val authToken = prefs.basicCredentials.authToken()
        val service = TimeTrackerServiceFactory.createPlain(authToken)

        val dateFormatted = formatSystemDate(date)
        service.fetchTimes(dateFormatted)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                showProgress(false)

                if (this.date != date) {
                    this.date.timeInMillis = date.timeInMillis
                }
                if (validResponse(response)) {
                    populateForm(response.body()!!, date)
                    populateList(response.body()!!, date)
                } else {
                    authenticate(true)
                }
            }, { err ->
                Timber.e(err, "Error fetching page: ${err.message}")
            })
            .addTo(disposables)
    }

    private fun validResponse(response: Response<String>): Boolean {
        val body = response.body()
        if (response.isSuccessful && (body != null)) {
            val networkResponse = response.raw().networkResponse()
            val priorResponse = response.raw().priorResponse()
            if ((networkResponse != null) && (priorResponse != null) && priorResponse.isRedirect) {
                val networkUrl = networkResponse.request().url()
                val priorUrl = priorResponse.request().url()
                if (networkUrl == priorUrl) {
                    return true
                }
                if (networkUrl.pathSegments()[networkUrl.pathSize() - 1] == TimeTrackerService.PHP_TIME) {
                    return true
                }
                return false
            }
            return true
        }
        return false
    }

    /** Populate the list. */
    private fun populateList(html: String, date: Calendar) {
        date_input.text = DateUtils.formatDateTime(context, date.timeInMillis, DateUtils.FORMAT_SHOW_DATE)

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

        bindList(records)
    }

    private fun bindList(records: List<TimeRecord>) {
        listAdapter.submitList(records)
    }

    private fun authenticate(immediate: Boolean = false) {
        showProgress(true)
        val intent = Intent(context, LoginActivity::class.java)
        intent.putExtra(LoginActivity.EXTRA_SUBMIT, immediate)
        startActivityForResult(intent, REQUEST_AUTHENTICATE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_AUTHENTICATE -> if (resultCode == RESULT_OK) {
                // Fetch the list for the user.
                fetchPage(date)
            }
            REQUEST_ADD -> if (resultCode == RESULT_OK) {
                // Refresh the list with the newly added item.
                fetchPage(date)
            }
            REQUEST_EDIT -> if (resultCode == RESULT_OK) {
                // Refresh the list with the edited item.
                fetchPage(date)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(STATE_DATE, date.timeInMillis)
        outState.putParcelableArrayList(STATE_PROJECTS, projects)
        outState.putParcelableArrayList(STATE_TASKS, tasks)
        outState.putLong(STATE_PROJECT_ID, record.project.id)
        outState.putLong(STATE_TASK_ID, record.task.id)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        date.timeInMillis = savedInstanceState.getLong(STATE_DATE)
        val projectsList = savedInstanceState.getParcelableArrayList<Project>(STATE_PROJECTS)
        val tasksList = savedInstanceState.getParcelableArrayList<ProjectTask>(STATE_TASKS)

        projects.clear()
        if (projectsList != null)  {
            projects.addAll(projectsList)
            projectEmpty = projectsList.first { it.isEmpty() }
        }
        tasks.clear()
        if (tasksList != null) {
            tasks.addAll(tasksList)
            taskEmpty = tasksList.first { it.isEmpty() }
        }
        val recordStarted = prefs.getStartedRecord()
        if ((recordStarted == null) || recordStarted.isEmpty()) {
            val projectId = savedInstanceState.getLong(STATE_PROJECT_ID)
            val taskId = savedInstanceState.getLong(STATE_TASK_ID)
            val project = projects.firstOrNull { it.id == projectId } ?: projectEmpty
            val task = tasks.firstOrNull { it.id == taskId } ?: taskEmpty
            record.project = project
            record.task = task
            populateForm(record)
        } else {
            populateForm(recordStarted)
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
        startActivityForResult(intent, REQUEST_ADD)
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
        val start = parseRecordTime(startText)

        val tdFinish = cols[3]
        val finishText = tdFinish.ownText()
        val finish = parseRecordTime(finishText)

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

    private fun populateProjects(doc: Document, select: Element, projects: MutableList<Project>) {
        projects.clear()

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

        populateTaskIds(doc, projects)
    }

    private fun populateTasks(doc: Document, select: Element, tasks: MutableList<ProjectTask>) {
        tasks.clear()

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
    }

    private fun populateTaskIds(doc: Document, projects: List<Project>) {
        val tokenStart = "var task_ids = new Array();"
        val tokenEnd = "// Prepare an array of task names."
        val scriptText = findScript(doc, tokenStart, tokenEnd)

        for (project in projects) {
            project.taskIds.clear()
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
                    projects.find { it.id == projectId }!!
                        .taskIds.addAll(taskIds)
                }
            }
        }
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

    private fun editRecord(record: TimeRecord) {
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
        startActivityForResult(intent, REQUEST_EDIT)
    }

    private fun deleteRecord(record: TimeRecord) {
        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        showProgress(true)

        val authToken = prefs.basicCredentials.authToken()
        val service = TimeTrackerServiceFactory.createPlain(authToken)

        service.deleteTime(record.id)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { response ->
                    showProgress(false)

                    if (validResponse(response)) {
                        populateForm(response.body()!!, date)
                        populateList(response.body()!!, date)
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

        val recordStarted = prefs.getStartedRecord()
        populateForm(recordStarted)
    }

    private fun populateForm(recordStarted: TimeRecord?) {
        if ((recordStarted == null) || recordStarted.isEmpty()) {
            showForm(DateUtils.isToday(date.timeInMillis))
        } else {
            record.project = projects.firstOrNull { it.id == recordStarted.project.id } ?: projectEmpty
            record.task = tasks.firstOrNull { it.id == recordStarted.task.id } ?: taskEmpty
            record.start = recordStarted.start
            showForm(!record.isEmpty())
        }

        bindForm(record)
    }

    private fun showForm(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        time_form_group.visibility = visibility
    }

    private fun bindForm(record: TimeRecord) {
        project_input.adapter = ArrayAdapter<Project>(context, android.R.layout.simple_list_item_1, projects.toTypedArray())
        project_input.setSelection(projects.indexOf(record.project))
        task_input.adapter = ArrayAdapter<ProjectTask>(context, android.R.layout.simple_list_item_1, tasks.toTypedArray())
        task_input.setSelection(tasks.indexOf(record.task))
        project_input.requestFocus()

        val startTime = record.start
        if ((startTime == null) || (startTime.timeInMillis <= 0L)) {
            project_input.isEnabled = true
            task_input.isEnabled = true
            action_switcher.displayedChild = 0
        } else {
            project_input.isEnabled = false
            task_input.isEnabled = false
            action_switcher.displayedChild = 1

            maybeStartTimer()
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
        return projects[0]
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
        return tasks[0]
    }

    private fun startTimer() {
        val now = System.currentTimeMillis()
        record.startTime = now

        showNotification(false)
    }

    private fun showNotification(notify: Boolean = false) {
        val service = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_PROJECT_ID, record.project.id)
            putExtra(TimerService.EXTRA_PROJECT_NAME, record.project.name)
            putExtra(TimerService.EXTRA_TASK_ID, record.task.id)
            putExtra(TimerService.EXTRA_TASK_NAME, record.task.name)
            putExtra(TimerService.EXTRA_START_TIME, record.startTime)
            putExtra(TimerService.EXTRA_NOTIFICATION, notify)
        }
        startService(service)

        bindForm(record)
    }

    private fun stopTimer() {
        val now = System.currentTimeMillis()
        record.finishTime = now
        timer?.dispose()

        val service = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
            putExtra(TimerService.EXTRA_PROJECT_ID, record.project.id)
            putExtra(TimerService.EXTRA_TASK_ID, record.task.id)
            putExtra(TimerService.EXTRA_START_TIME, record.start?.timeInMillis ?: return)
            putExtra(TimerService.EXTRA_FINISH_TIME, now)
        }
        startService(service)
        editRecord(record)

        record.start = null
        record.finish = null
        bindForm(record)
    }

    private fun maybeShowNotification() {
        if ((record.project.id > 0L) && (record.task.id > 0L) && (record.start != null)) {
            showNotification(true)
        }
    }

    private fun hideNotification() {
        val service = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_NOTIFY
            putExtra(TimerService.EXTRA_NOTIFICATION, false)
        }
        startService(service)
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
}
