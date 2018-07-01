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
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
import kotlinx.android.synthetic.main.activity_time_edit.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
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
    private var submitMenuItem: MenuItem? = null
    private var datePickerDialog: DatePickerDialog? = null
    private var startPickerDialog: TimePickerDialog? = null
    private var finishPickerDialog: TimePickerDialog? = null

    /** Keep track of the task to ensure we can cancel it if requested. */
    private var fetchTask: Disposable? = null
    /** Keep track of the task to ensure we can cancel it if requested. */
    private var submitTask: Disposable? = null
    private var date: Long = 0L
    private var user = User("")
    private var record = TimeRecord(user, Project(""), ProjectTask(""))
    private val projects = ArrayList<Project>()
    private val tasks = ArrayList<ProjectTask>()
    private var errorMessage: String = ""
    private var projectEmpty: Project? = null
    private var taskEmpty: ProjectTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = TimeTrackerPrefs(this)

        // Set up the form.
        setContentView(R.layout.activity_time_edit)

        user.username = prefs.userCredentials.login
        user.email = user.username

        project_input.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(adapterView: AdapterView<*>) {
                record.project = projectEmpty ?: Project.EMPTY
            }

            override fun onItemSelected(adapterView: AdapterView<*>, view: View?, position: Int, id: Long) {
                val project = project_input.adapter.getItem(position) as Project
                filterTasks(project)
                record.project = project
            }
        }
        task_input.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(adapterView: AdapterView<*>) {
                record.task = taskEmpty ?: ProjectTask.EMPTY
            }

            override fun onItemSelected(adapterView: AdapterView<*>, view: View?, position: Int, id: Long) {
                val task = task_input.adapter.getItem(position) as ProjectTask
                record.task = task
            }
        }
        date_input.setOnClickListener { pickDate() }
        start_input.setOnClickListener { pickStartTime() }
        finish_input.setOnClickListener { pickFinishTime() }

        var date: Long
        val now = System.currentTimeMillis()
        if (savedInstanceState == null) {
            date = now
        } else {
            date = savedInstanceState.getLong(STATE_DATE, now)
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

        val authToken = prefs.basicCredentials.authToken()
        val service = TimeTrackerServiceFactory.createPlain(authToken)

        val dateFormatted = formatSystemDate(date)
        fetchTask = service.fetchTimes(dateFormatted)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    showProgress(false)

                    this.date = date
                    if (validResponse(response)) {
                        populateForm(response.body()!!, date)
                    } else {
                        authenticate(true)
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
                val networkUrl = networkResponse.request().url()
                val priorUrl = priorResponse.request().url()
                return networkUrl == priorUrl
            }
            return true
        }
        return false
    }

    /** Populate the record and then bind the form. */
    private fun populateForm(html: String, date: Long) {
        val doc: Document = Jsoup.parse(html)

        val errorNode = doc.selectFirst("td[class='error']")
        errorMessage = errorNode?.text()?.trim() ?: ""

        val form = doc.selectFirst("form[name='timeRecordForm']")

        val inputProjects = form.selectFirst("select[name='project']")
        populateProjects(doc, inputProjects, projects)
        record.project = projects[0]

        val inputTasks = form.selectFirst("select[name='task']")
        populateTasks(doc, inputTasks, tasks)

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

    private fun bindForm(record: TimeRecord) {
        val context = this
        error_label.text = errorMessage
        project_input.adapter = ArrayAdapter<Project>(context, android.R.layout.simple_list_item_1, projects.toTypedArray())
        task_input.adapter = ArrayAdapter<ProjectTask>(context, android.R.layout.simple_list_item_1, tasks.toTypedArray())
        date_input.text = DateUtils.formatDateTime(context, date, DateUtils.FORMAT_SHOW_DATE)
        datePickerDialog = null
        start_input.text = if (record.start != null) DateUtils.formatDateTime(context, record.start!!.timeInMillis, DateUtils.FORMAT_SHOW_TIME) else ""
        start_input.error = null
        startPickerDialog = null
        finish_input.text = if (record.finish != null) DateUtils.formatDateTime(context, record.finish!!.timeInMillis, DateUtils.FORMAT_SHOW_TIME) else ""
        finish_input.error = null
        finishPickerDialog = null
        note_input.setText(record.note)
        project_input.requestFocus()
    }

    private fun bindRecord(record: TimeRecord) {
        record.note = note_input.text.toString()
    }

    private fun authenticate(immediate: Boolean = false) {
        showProgress(true)
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra(LoginActivity.EXTRA_SUBMIT, immediate)
        startActivityForResult(intent, REQUEST_AUTHENTICATE)
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

        if (!validateForm()) {
            return
        }
        bindRecord(record)

        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        showProgress(true)

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

                    if (validResponse(response)) {
                        populateForm(response.body()!!, date)
                    } else {
                        authenticate(true)
                    }
                }, { err ->
                    Log.e(TAG, "Error saving page: ${err.message}", err)
                    showProgress(false)
                })
    }

    private fun pickDate() {
        if (datePickerDialog == null) {
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
                date_input.text = DateUtils.formatDateTime(context, cal.timeInMillis, DateUtils.FORMAT_SHOW_DATE)
            }
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val day = cal.get(Calendar.DAY_OF_MONTH)
            datePickerDialog = DatePickerDialog(context, listener, year, month, day)
        }
        datePickerDialog!!.show()
    }

    private fun pickStartTime() {
        if (startPickerDialog == null) {
            val context = this
            val cal = getCalendar(record.start)
            val listener = TimePickerDialog.OnTimeSetListener { picker, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                record.start = cal
                start_input.text = DateUtils.formatDateTime(context, cal.timeInMillis, DateUtils.FORMAT_SHOW_TIME)
                start_input.error = null
            }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)
            startPickerDialog = TimePickerDialog(context, listener, hour, minute, DateFormat.is24HourFormat(context))
        }
        startPickerDialog!!.show()
    }

    private fun pickFinishTime() {
        if (finishPickerDialog == null) {
            val context = this
            val cal = getCalendar(record.finish)
            val listener = TimePickerDialog.OnTimeSetListener { picker, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                record.finish = cal
                finish_input.text = DateUtils.formatDateTime(context, cal.timeInMillis, DateUtils.FORMAT_SHOW_TIME)
                finish_input.error = null
            }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)
            finishPickerDialog = TimePickerDialog(context, listener, hour, minute, DateFormat.is24HourFormat(context))
        }
        finishPickerDialog!!.show()
    }

    private fun getCalendar(cal: Calendar?): Calendar {
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
            project_label.error = getString(R.string.error_field_required)
        } else {
            project_label.error = null
        }
        if (record.task.id <= 0) {
            valid = false
            task_label.error = getString(R.string.error_field_required)
        } else {
            task_label.error = null
        }
        if (record.start == null) {
            valid = false
            start_input.error = getString(R.string.error_field_required)
        } else {
            start_input.error = null
        }
        if (record.finish == null) {
            valid = false
            finish_input.error = getString(R.string.error_field_required)
        } else if (record.start!! >= record.finish!!) {
            valid = false
            finish_input.error = getString(R.string.error_finish_time_before_start_time)
        } else {
            finish_input.error = null
        }

        return valid
    }


    private fun filterTasks(project: Project) {
        val context = this
        var objects = tasks.toTypedArray().filter { it.id in project.taskIds }
        if (objects.isEmpty()) {
            objects = arrayListOf(taskEmpty ?: ProjectTask.EMPTY)
        }
        task_input.adapter = ArrayAdapter<ProjectTask>(context, android.R.layout.simple_list_item_1, objects)
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private fun showProgress(show: Boolean) {
        val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        time_form.visibility = if (show) View.GONE else View.VISIBLE
        time_form.animate().setDuration(shortAnimTime).alpha(
                (if (show) 0 else 1).toFloat()).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                time_form.visibility = if (show) View.GONE else View.VISIBLE
            }
        })

        progress.visibility = if (show) View.VISIBLE else View.GONE
        progress.animate().setDuration(shortAnimTime).alpha(
                (if (show) 1 else 0).toFloat()).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                progress.visibility = if (show) View.VISIBLE else View.GONE
            }
        })

        submitMenuItem?.isEnabled = !show
    }
}
