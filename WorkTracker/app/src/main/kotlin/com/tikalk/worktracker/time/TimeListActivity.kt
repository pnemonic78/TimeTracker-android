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
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
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
import java.util.regex.Pattern
import kotlin.collections.ArrayList

class TimeListActivity : InternetActivity(),
    TimeListAdapter.OnTimeListListener {

    companion object {
        private const val REQUEST_AUTHENTICATE = 1
        private const val REQUEST_ADD = 2
        private const val REQUEST_EDIT = 3

        private const val STATE_DATE = "date"
    }

    private val context: Context = this

    // UI references
    private var datePickerDialog: DatePickerDialog? = null

    private lateinit var prefs: TimeTrackerPrefs

    private val disposables = CompositeDisposable()
    private val date = Calendar.getInstance()
    private var user = User("")
    private val projects = ArrayList<Project>()
    private val tasks = ArrayList<ProjectTask>()
    private val listAdapter = TimeListAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = TimeTrackerPrefs(this)

        // Set up the form.
        setContentView(R.layout.activity_time_list)

        user.username = prefs.userCredentials.login
        user.email = user.username

        date_input.setOnClickListener { pickDate() }
        fab_add.setOnClickListener { addTime() }

        list.adapter = listAdapter
        val swipeHandler = TimeListSwipeHandler(this)
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(list)

        val now = date.timeInMillis
        date.timeInMillis = savedInstanceState?.getLong(STATE_DATE, now) ?: now
        fetchPage(date)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
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
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        date.timeInMillis = savedInstanceState.getLong(STATE_DATE)
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
            if (!value.isEmpty()) {
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
            if (!value.isEmpty()) {
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
        intent.putExtra(TimeEditActivity.EXTRA_DATE, date)
        intent.putExtra(TimeEditActivity.EXTRA_RECORD, record.id)
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
}
