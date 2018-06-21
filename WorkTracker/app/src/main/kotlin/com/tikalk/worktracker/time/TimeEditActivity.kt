package com.tikalk.worktracker.time

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.LoginActivity
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.User
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.net.TimeTrackerServiceFactory
import com.tikalk.worktracker.preference.TimeTrackerPrefs
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import retrofit2.Response
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList

class TimeEditActivity : AppCompatActivity() {

    companion object {
        private val TAG = "TimeEditActivity"

        private const val REQUEST_AUTHENTICATE = 1

        private const val STATE_DATE = "date"
    }

    private lateinit var prefs: TimeTrackerPrefs

    // UI references
    private lateinit var timeForm: ViewGroup
    private lateinit var projectSpinner: Spinner
    private lateinit var taskSpinner: Spinner
    private lateinit var dateText: TextView
    private lateinit var startTimeText: TextView
    private lateinit var finishTimeText: TextView
    private lateinit var noteText: EditText
    private lateinit var progressView: ProgressBar
    private var submitMenuItem: MenuItem? = null

    /** Keep track of the task to ensure we can cancel it if requested. */
    private var fetchTask: Disposable? = null
    /** Keep track of the task to ensure we can cancel it if requested. */
    private var submitTask: Disposable? = null
    private var date: Long = 0L
    private var user = User("")
    private var project = Project("")
    private var task = ProjectTask("")
    private var record = TimeRecord(user, project, task)
    private val projects = ArrayList<Project>()
    private val tasks = ArrayList<ProjectTask>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = TimeTrackerPrefs(this)
        setContentView(R.layout.activity_time_edit)

        user.username = prefs.userCredentials.login
        user.email = user.username

        timeForm = findViewById(R.id.time_form)
        projectSpinner = findViewById(R.id.project)
        taskSpinner = findViewById(R.id.task)
        dateText = findViewById(R.id.date)
        startTimeText = findViewById(R.id.start)
        finishTimeText = findViewById(R.id.finish)
        noteText = findViewById(R.id.note)
        progressView = findViewById(R.id.progress)

        projectSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(adapterView: AdapterView<*>) {
            }

            override fun onItemSelected(adapterView: AdapterView<*>, view: View, position: Int, id: Long) {
                val project = projects[position]
                filterTasks(project)
            }
        }
        dateText.setOnClickListener { pickDate() }
        startTimeText.setOnClickListener { pickStartTime() }
        finishTimeText.setOnClickListener { pickFinishTime() }

        var date: Long
        if (savedInstanceState == null) {
            date = System.currentTimeMillis()
        } else {
            date = savedInstanceState.getLong(STATE_DATE, System.currentTimeMillis())
        }
        fetchPage(date)
    }

    override fun onDestroy() {
        super.onDestroy()
        fetchTask?.dispose()
        submitTask?.dispose()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.clear()
        menuInflater.inflate(R.menu.time_edit, menu)
        submitMenuItem = menu.findItem(R.id.menu_submit)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_cancel -> finish()
            R.id.menu_submit -> submit()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun fetchPage(date: Long) {
        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        showProgress(true)
        //TODO disable menu items

        val authToken = prefs.basicCredentials.authToken()
        val service = TimeTrackerServiceFactory.createPlain(authToken)

        val dateFormatted = formatSystemDate(date)
        fetchTask = service.fetchTimes(dateFormatted)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    showProgress(false)
                    //TODO enable menu items

                    this.date = date
                    if (validResponse(response)) {
                        this.record = TimeRecord(user, project, task)
                        populateForm(response.body()!!, date)
                    } else {
                        authenticate()
                    }
                }, { err ->
                    Log.e(TAG, "Error fetching page: ${err.message}", err)
                })
    }

    private fun validResponse(response: Response<String>): Boolean {
        val body = response.body()
        if (response.isSuccessful && (body != null)) {
            val networkResponse = response.raw().networkResponse()
            val priorResponse = response.raw().priorResponse()
            if ((networkResponse != null) && (priorResponse != null) && priorResponse.isRedirect) {
                //val redirectUrl = networkResponse.request().url()
                //val paths = redirectUrl.pathSegments()
                //return paths.last() != "login.php"
                return false
            }
            return true
        }
        return false
    }

    /** Populate the record and then bind the form. */
    private fun populateForm(html: String, date: Long) {
        val doc: Document = Jsoup.parse(html)
        val form = doc.selectFirst("form[name='timeRecordForm']")

        populateProjects(doc, projects)
        populateTasks(doc, tasks)

        val inputStart = form.selectFirst("input[name='start']")
        val startValue = inputStart.attr("value")
        record.start = parseSystemTime(date, startValue)

        val inputFinish = form.selectFirst("input[name='finish']")
        val finishValue = inputFinish.attr("value")
        record.finish = parseSystemTime(date, finishValue)

        val inputNote = form.selectFirst("textarea[name='note']")
        record.note = inputNote.text()

        bindForm(record)
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

    private fun populateProjects(doc: Document, projects: MutableList<Project>) {
        projects.clear()

        val tokenStart = "var project_names = new Array();"
        val tokenEnd = "populate project dropdown"
        val scriptText = findScript(doc, tokenStart, tokenEnd)

        if (scriptText.isNotEmpty()) {
            val pattern = Pattern.compile("project_names\\[(\\d+)\\] = \"(.+)\"")
            val lines = scriptText.split(";")
            for (line in lines) {
                val matcher = pattern.matcher(line)
                if (matcher.find()) {
                    val project = Project(matcher.group(2))
                    project.id = matcher.group(1).toLong()
                    projects.add(project)
                }
            }
        }

        populateTaskIds(doc, projects)
    }

    private fun populateTasks(doc: Document, tasks: MutableList<ProjectTask>) {
        tasks.clear()

        val tokenStart = "var task_names = new Array();"
        val tokenEnd = "// Mandatory top options for project and task dropdowns."
        val scriptText = findScript(doc, tokenStart, tokenEnd)

        if (scriptText.isNotEmpty()) {
            val pattern = Pattern.compile("task_names\\[(\\d+)\\] = \"(.+)\"")
            val lines = scriptText.split(";")
            for (line in lines) {
                val matcher = pattern.matcher(line)
                if (matcher.find()) {
                    val task = ProjectTask(matcher.group(2))
                    task.id = matcher.group(1).toLong()
                    tasks.add(task)
                }
            }
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

    private fun bindForm(record: TimeRecord) {
        val context = this
        projectSpinner.adapter = ArrayAdapter<Project>(context, android.R.layout.simple_list_item_1, projects.toTypedArray())
        taskSpinner.adapter = ArrayAdapter<ProjectTask>(context, android.R.layout.simple_list_item_1, tasks.toTypedArray())
        dateText.text = DateUtils.formatDateTime(context, date, DateUtils.FORMAT_SHOW_DATE)
        startTimeText.text = if (record.start != null) DateUtils.formatDateTime(context, record.start!!.timeInMillis, DateUtils.FORMAT_SHOW_TIME) else ""
        finishTimeText.text = if (record.finish != null) DateUtils.formatDateTime(context, record.finish!!.timeInMillis, DateUtils.FORMAT_SHOW_TIME) else ""
        noteText.setText(record.note)
        validateForm()
    }

    private fun bindRecord(record: TimeRecord) {
        record.note = noteText.text.toString()
    }

    private fun authenticate() {
        startActivityForResult(Intent(this, LoginActivity::class.java), REQUEST_AUTHENTICATE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_AUTHENTICATE) {
            if (resultCode == Activity.RESULT_OK) {
                fetchPage(date)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(STATE_DATE, date)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        date = savedInstanceState.getLong(STATE_DATE)
    }

    private fun submit() {
        val record = this.record
        bindRecord(record)

        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        showProgress(true)
        //TODO disable menu items

        val authToken = prefs.basicCredentials.authToken()
        val service = TimeTrackerServiceFactory.createPlain(authToken)

        submitTask = service.addTime(record.project.id,
                record.task.id,
                formatSystemDate(date),
                formatSystemTime(record.start),
                formatSystemTime(record.finish),
                record.note)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    showProgress(false)
                    //TODO enable menu items

                    if (validResponse(response)) {
                        populateForm(response.body()!!, date)
                    } else {
                        authenticate()
                    }
                }, { err ->
                    Log.e(TAG, "Error saving page: ${err.message}", err)
                    showProgress(false)
                    //TODO enable menu items
                })
    }

    private fun pickDate() {
        val context = this
        val cal = Calendar.getInstance()
        cal.timeInMillis = date
        val listener = DatePickerDialog.OnDateSetListener { picker, year, month, day ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, day)
            this@TimeEditActivity.date = cal.timeInMillis
            val start = record.start
            if (start != null) {
                start.set(Calendar.YEAR, year)
                start.set(Calendar.MONTH, month)
                start.set(Calendar.DAY_OF_MONTH, day)
            }
            val finish = record.finish
            if (finish != null) {
                finish.set(Calendar.YEAR, year)
                finish.set(Calendar.MONTH, month)
                finish.set(Calendar.DAY_OF_MONTH, day)
            }
            dateText.text = DateUtils.formatDateTime(context, cal.timeInMillis, DateUtils.FORMAT_SHOW_DATE)
            validateForm()
        }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        DatePickerDialog(context, listener, year, month, day).show()
    }

    private fun pickStartTime() {
        val context = this
        val cal = getCalendar(record.start)
        val listener = TimePickerDialog.OnTimeSetListener { picker, hour, minute ->
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            record.start = cal
            startTimeText.text = DateUtils.formatDateTime(context, cal.timeInMillis, DateUtils.FORMAT_SHOW_TIME)
            validateForm()
        }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        TimePickerDialog(context, listener, hour, minute, DateFormat.is24HourFormat(context)).show()
    }

    private fun pickFinishTime() {
        val context = this
        val cal = getCalendar(record.finish)
        val listener = TimePickerDialog.OnTimeSetListener { picker, hour, minute ->
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            record.finish = cal
            finishTimeText.text = DateUtils.formatDateTime(context, cal.timeInMillis, DateUtils.FORMAT_SHOW_TIME)
            validateForm()
        }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        TimePickerDialog(context, listener, hour, minute, DateFormat.is24HourFormat(context)).show()
    }

    private inline fun getCalendar(cal: Calendar?): Calendar {
        if (cal == null) {
            val calDate = Calendar.getInstance()
            calDate.timeInMillis = date
            return calDate
        }
        return cal
    }

    private fun validateForm(): Boolean {
        var valid = true

        if (record.project.id <= 0) {
            valid = false
            //TODO mark the field as invalid, e.g. red background
        }
        if (record.task.id <= 0) {
            valid = false
            //TODO mark the field as invalid, e.g. red background
        }
        if ((record.start == null) || (record.finish == null) || (record.start!! >= record.finish!!)) {
            valid = false
            //TODO mark the field as invalid, e.g. red background
        }

        submitMenuItem?.isEnabled = valid
        return valid
    }


    private fun filterTasks(project: Project) {
        val context = this
        taskSpinner.adapter = ArrayAdapter<ProjectTask>(context, android.R.layout.simple_list_item_1, tasks.toTypedArray().filter { it.id in project.taskIds })
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private fun showProgress(show: Boolean) {
        val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        timeForm.visibility = if (show) View.GONE else View.VISIBLE
        timeForm.animate().setDuration(shortAnimTime).alpha(
                (if (show) 0 else 1).toFloat()).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                timeForm.visibility = if (show) View.GONE else View.VISIBLE
            }
        })

        progressView.visibility = if (show) View.VISIBLE else View.GONE
        progressView.animate().setDuration(shortAnimTime).alpha(
                (if (show) 1 else 0).toFloat()).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                progressView.visibility = if (show) View.VISIBLE else View.GONE
            }
        })
    }
}
