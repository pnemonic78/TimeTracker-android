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
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import com.tikalk.worktracker.BuildConfig
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.LoginActivity
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.time.TaskRecordStatus
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.model.time.split
import com.tikalk.worktracker.net.TimeTrackerServiceFactory
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_time_edit.*
import kotlinx.android.synthetic.main.progress.*
import kotlinx.android.synthetic.main.time_form.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import retrofit2.Response
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max

class TimeEditActivity : TimeFormActivity() {

    companion object {
        private const val REQUEST_AUTHENTICATE = 1

        private const val STATE_DATE = "date"
        private const val STATE_RECORD_ID = "record_id"
        private const val STATE_RECORD = "record"

        const val EXTRA_DATE = BuildConfig.APPLICATION_ID + ".DATE"
        const val EXTRA_RECORD = BuildConfig.APPLICATION_ID + ".RECORD_ID"

        const val EXTRA_PROJECT_ID = BuildConfig.APPLICATION_ID + ".PROJECT_ID"
        const val EXTRA_TASK_ID = BuildConfig.APPLICATION_ID + ".TASK_ID"
        const val EXTRA_START_TIME = BuildConfig.APPLICATION_ID + ".START_TIME"
        const val EXTRA_FINISH_TIME = BuildConfig.APPLICATION_ID + ".FINISH_TIME"

        private const val FORMAT_DATE_BUTTON = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_WEEKDAY
    }

    private val context: Context = this

    // UI references
    private var submitMenuItem: MenuItem? = null
    private var startPickerDialog: TimePickerDialog? = null
    private var finishPickerDialog: TimePickerDialog? = null

    private var errorMessage: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up the form.
        setContentView(R.layout.activity_time_edit)

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
        start_input.setOnClickListener { pickStartTime() }
        finish_input.setOnClickListener { pickFinishTime() }

        handleIntent(intent, savedInstanceState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent, savedInstanceState: Bundle? = null) {
        val now = date.timeInMillis
        var recordId = record.id

        val extras = intent.extras
        if (extras != null) {
            date.timeInMillis = extras.getLong(EXTRA_DATE, now)
            recordId = extras.getLong(EXTRA_RECORD, recordId)
        }
        if (savedInstanceState != null) {
            date.timeInMillis = savedInstanceState.getLong(STATE_DATE, now)
            record.id = savedInstanceState.getLong(STATE_RECORD_ID, recordId)
            loadPage()
                .subscribe({
                    populateForm(record)
                    showProgress(false)
                }, { err ->
                    Timber.e(err, "Error loading page: ${err.message}")
                    showProgress(false)
                })
                .addTo(disposables)
        } else {
            fetchPage(date, recordId)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.clear()
        menuInflater.inflate(R.menu.time_edit, menu)
        submitMenuItem = menu.findItem(R.id.menu_submit)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                setResult(RESULT_CANCELED)
                onBackPressed()
                return true
            }
            R.id.menu_delete -> {
                deleteRecord()
                return true
            }
            R.id.menu_submit -> {
                submit()
                return true
            }
            R.id.menu_favorite -> {
                markFavorite()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun fetchPage(date: Calendar, id: Long) {
        val dateFormatted = formatSystemDate(date)
        Timber.d("fetchPage $dateFormatted")
        // Show a progress spinner, and kick off a background task to perform the user login attempt.
        showProgress(true)

        // Fetch from local database.
        //FIXME loadPage()

        val authToken = prefs.basicCredentials.authToken()
        val service = TimeTrackerServiceFactory.createPlain(this, authToken)

        val fetcher: Single<Response<String>> = if (id == 0L) {
            service.fetchTimes(dateFormatted)
        } else {
            service.fetchTimes(id)
        }
        fetcher
            .subscribeOn(Schedulers.io())
            .subscribe({ response ->
                this.date = date
                if (isValidResponse(response)) {
                    val body = response.body()!!
                    populateForm(body, date, id)
                    showProgressMain(false)
                } else {
                    authenticate(true)
                }
            }, { err ->
                Timber.e(err, "Error fetching page: ${err.message}")
                showProgressMain(false)
            })
            .addTo(disposables)
    }

    /** Populate the record and then bind the form. */
    private fun populateForm(html: String, date: Calendar, id: Long) {
        val doc: Document = Jsoup.parse(html)

        record.id = id

        errorMessage = findError(doc)?.trim() ?: ""

        val form = doc.selectFirst("form[name='timeRecordForm']") ?: return

        val inputProjects = form.selectFirst("select[name='project']") ?: return
        populateProjects(inputProjects, projects)

        val inputTasks = form.selectFirst("select[name='task']") ?: return
        populateTasks(inputTasks, tasks)

        populateTaskIds(doc, projects)

        val inputStart = form.selectFirst("input[name='start']") ?: return
        val startValue = inputStart.attr("value")

        val inputFinish = form.selectFirst("input[name='finish']") ?: return
        val finishValue = inputFinish.attr("value")

        val inputNote = form.selectFirst("textarea[name='note']")

        record.project = findSelectedProject(inputProjects, projects)
        record.task = findSelectedTask(inputTasks, tasks)
        record.start = parseSystemTime(date, startValue)
        record.finish = parseSystemTime(date, finishValue)
        record.note = inputNote?.text() ?: ""

        if (id == 0L) {
            val projectFavorite = prefs.getFavoriteProject()
            val taskFavorite = prefs.getFavoriteTask()

            val extras = intent.extras
            if (extras != null) {
                var projectId = extras.getLong(EXTRA_PROJECT_ID)
                var taskId = extras.getLong(EXTRA_TASK_ID)
                val startTime = extras.getLong(EXTRA_START_TIME)
                val finishTime = extras.getLong(EXTRA_FINISH_TIME)

                if (projectId == 0L) projectId = projectFavorite
                if (taskId == 0L) taskId = taskFavorite

                val project = projects.firstOrNull { it.id == projectId } ?: projectEmpty
                val task = tasks.firstOrNull { it.id == taskId } ?: taskEmpty

                record = TimeRecord(user, project, task)
                if (startTime > 0L) {
                    record.startTime = startTime
                } else {
                    record.start = null
                }
                if (finishTime > 0L) {
                    record.finishTime = finishTime
                } else {
                    record.finish = null
                }
            } else {
                record.project = projects.firstOrNull { it.id == projectFavorite } ?: record.project
                record.task = tasks.firstOrNull { it.id == taskFavorite } ?: record.task
            }
        } else {
            record.status = TaskRecordStatus.CURRENT
        }

        savePage()

        runOnUiThread { bindForm(record) }
    }

    private fun populateForm(record: TimeRecord) {
        Timber.v("populateForm $record")
        bindForm(record)
    }

    private fun bindForm(record: TimeRecord) {
        error_label.text = errorMessage
        project_input.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, projects.toTypedArray())
        if (projects.isNotEmpty()) {
            project_input.setSelection(max(0, projects.indexOf(record.project)))
        }
        task_input.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, tasks.toTypedArray())
        if (tasks.isNotEmpty()) {
            task_input.setSelection(max(0, tasks.indexOf(record.task)))
        }
        project_input.requestFocus()

        val startTime = record.startTime
        start_input.text = if (startTime > 0L)
            DateUtils.formatDateTime(context, startTime, FORMAT_DATE_BUTTON)
        else
            ""
        start_input.error = null
        startPickerDialog = null

        val finishTime = record.finishTime
        finish_input.text = if (finishTime > 0L)
            DateUtils.formatDateTime(context, finishTime, FORMAT_DATE_BUTTON)
        else
            ""
        finish_input.error = null
        finishPickerDialog = null

        note_input.setText(record.note)
    }

    private fun bindRecord(record: TimeRecord) {
        record.note = note_input.text.toString()
    }

    private fun authenticate(immediate: Boolean = false) {
        showProgressMain(true)
        val intent = Intent(context, LoginActivity::class.java)
        intent.putExtra(LoginActivity.EXTRA_SUBMIT, immediate)
        startActivityForResult(intent, REQUEST_AUTHENTICATE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_AUTHENTICATE) {
            if (resultCode == RESULT_OK) {
                fetchPage(date, record.id)
            } else {
                showProgress(false)
                finish()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(STATE_DATE, date.timeInMillis)

        bindRecord(record)
        outState.putLong(STATE_RECORD_ID, record.id)
        outState.putParcelable(STATE_RECORD, record)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        date.timeInMillis = savedInstanceState.getLong(STATE_DATE)
        val recordParcel = savedInstanceState.getParcelable<TimeRecord>(STATE_RECORD)

        if (recordParcel != null) {
            record = recordParcel
            populateForm(record)
        } else {
            record.id = savedInstanceState.getLong(STATE_RECORD_ID)
        }
    }

    private fun submit() {
        val record = this.record

        if (!validateForm(record)) {
            return
        }
        bindRecord(record)

        if (record.id == 0L) {
            val splits = record.split()
            val size = splits.size
            val lastIndex = size - 1
            for (i in 0 until size) {
                submit(splits[i], i == 0, i == lastIndex)
            }
        } else {
            submit(record, true, true)
        }
    }

    private fun submit(record: TimeRecord, first: Boolean = true, last: Boolean = true) {
        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        if (first) {
            showProgress(true)
            error_label.text = ""
        }

        val authToken = prefs.basicCredentials.authToken()
        val service = TimeTrackerServiceFactory.createPlain(this, authToken)

        val submitter: Single<Response<String>> = if (record.id == 0L) {
            service.addTime(record.project.id,
                record.task.id,
                formatSystemDate(record.start),
                formatSystemTime(record.start),
                formatSystemTime(record.finish),
                record.note)
        } else {
            service.editTime(record.id,
                record.project.id,
                record.task.id,
                formatSystemDate(record.start),
                formatSystemTime(record.start),
                formatSystemTime(record.finish),
                record.note)
        }
        submitter
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                if (last) {
                    showProgress(false)
                }

                if (isValidResponse(response)) {
                    val body = response.body()!!
                    val errorMessage = getResponseError(body)
                    if (errorMessage.isNullOrEmpty()) {
                        if (last) {
                            setResult(RESULT_OK)
                            finish()
                        }
                    } else {
                        error_label.text = errorMessage
                    }
                } else {
                    authenticate(true)
                }
            }, { err ->
                Timber.e(err, "Error saving record: ${err.message}")
                showProgress(false)
            })
            .addTo(disposables)
    }

    private fun pickStartTime() {
        if (startPickerDialog == null) {
            val cal = getCalendar(record.start)
            val listener = TimePickerDialog.OnTimeSetListener { picker, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                record.start = cal
                start_input.text = DateUtils.formatDateTime(context, cal.timeInMillis, FORMAT_DATE_BUTTON)
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
            val cal = getCalendar(record.finish)
            val listener = TimePickerDialog.OnTimeSetListener { picker, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                record.finish = cal
                finish_input.text = DateUtils.formatDateTime(context, cal.timeInMillis, FORMAT_DATE_BUTTON)
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
            calDate.timeInMillis = date.timeInMillis
            return calDate
        }
        return cal
    }

    private fun validateForm(record: TimeRecord): Boolean {
        var valid = true

        if (record.project.id <= 0) {
            valid = false
            (project_input.selectedView as TextView).error = getString(R.string.error_field_required)
        } else {
            (project_input.selectedView as TextView).error = null
        }
        if (record.task.id <= 0) {
            valid = false
            (task_input.selectedView as TextView).error = getString(R.string.error_field_required)
        } else {
            (task_input.selectedView as TextView).error = null
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
        } else if (record.startTime + DateUtils.MINUTE_IN_MILLIS > record.finishTime) {
            valid = false
            finish_input.error = getString(R.string.error_finish_time_before_start_time)
        } else {
            finish_input.error = null
        }

        return valid
    }

    private fun filterTasks(project: Project) {
        val filtered = tasks.filter { it.id in project.taskIds }
        val options = ArrayList<ProjectTask>(filtered.size + 1)
        options.add(taskEmpty)
        options.addAll(filtered)
        task_input.adapter = ArrayAdapter<ProjectTask>(context, android.R.layout.simple_list_item_1, options)
        task_input.setSelection(options.indexOf(record.task))
    }

    override fun showProgress(show: Boolean) {
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

    private fun deleteRecord() {
        if (record.id == 0L) {
            setResult(RESULT_OK)
            finish()
        } else {
            deleteRecord(record)
        }
    }

    private fun deleteRecord(record: TimeRecord) {
        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        showProgress(true)

        val authToken = prefs.basicCredentials.authToken()
        val service = TimeTrackerServiceFactory.createPlain(this, authToken)

        service.deleteTime(record.id)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                if (isValidResponse(response)) {
                    showProgress(false)
                    setResult(RESULT_OK)
                    finish()
                } else {
                    authenticate(true)
                }
            }, { err ->
                Timber.e(err, "Error deleting record: ${err.message}")
                showProgress(false)
            })
            .addTo(disposables)
    }

    private fun projectItemSelected(project: Project) {
        record.project = project
        filterTasks(project)
    }

    private fun taskItemSelected(task: ProjectTask) {
        record.task = task
    }

    private fun loadPage(): Single<Unit> {
        return Single.fromCallable { loadFormFromDb() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    private fun savePage() {
        return saveFormToDb()
    }
}
