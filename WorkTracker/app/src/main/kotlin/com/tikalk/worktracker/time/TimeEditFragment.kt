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

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.tikalk.app.runOnUiThread
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.LoginActivity
import com.tikalk.worktracker.auth.LoginFragment
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.isNullOrEmpty
import com.tikalk.worktracker.model.time.TaskRecordStatus
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.model.time.split
import com.tikalk.worktracker.net.TimeTrackerServiceFactory
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.time_form.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import retrofit2.Response
import timber.log.Timber
import java.util.*
import kotlin.math.max

class TimeEditFragment : TimeFormFragment() {

    private var startPickerDialog: TimePickerDialog? = null
    private var finishPickerDialog: TimePickerDialog? = null
    private var errorMessage: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.time_form, container, false)
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
        startInput.setOnClickListener { pickStartTime() }
        finishInput.setOnClickListener { pickFinishTime() }
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

        if (id == TikalEntity.ID_NONE) {
            val args = arguments
            if (args != null) {
                val projectId = args.getLong(EXTRA_PROJECT_ID)
                val taskId = args.getLong(EXTRA_TASK_ID)
                val startTime = args.getLong(EXTRA_START_TIME)
                val finishTime = args.getLong(EXTRA_FINISH_TIME)

                val project = projects.firstOrNull { it.id == projectId } ?: projectEmpty
                val task = tasks.firstOrNull { it.id == taskId } ?: taskEmpty

                record = TimeRecord(id, user, project, task)
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
            }
        } else {
            record.status = TaskRecordStatus.CURRENT
        }

        populateForm(record)
        runOnUiThread { bindForm(record) }
    }

    private fun populateForm(record: TimeRecord) {
        if (record.project.isNullOrEmpty() and record.task.isNullOrEmpty()) {
            val projectFavorite = preferences.getFavoriteProject()
            if (projectFavorite != TikalEntity.ID_NONE) {
                record.project = projects.firstOrNull { it.id == projectFavorite } ?: record.project
            }
            val taskFavorite = preferences.getFavoriteTask()
            if (taskFavorite != TikalEntity.ID_NONE) {
                record.task = tasks.firstOrNull { it.id == taskFavorite } ?: record.task
            }
        }
    }

    override fun bindForm(record: TimeRecord) {
        Timber.v("bindForm $record")
        val context: Context = requireContext()

        errorLabel.text = errorMessage
        projectInput.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, projects.toTypedArray())
        if (projects.isNotEmpty()) {
            projectInput.setSelection(max(0, projects.indexOf(record.project)))
        }
        taskInput.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, tasks.toTypedArray())
        if (tasks.isNotEmpty()) {
            taskInput.setSelection(max(0, tasks.indexOf(record.task)))
        }
        projectInput.requestFocus()

        val startTime = record.startTime
        startInput.text = if (startTime > 0L)
            DateUtils.formatDateTime(context, startTime, FORMAT_DATE_BUTTON)
        else
            ""
        startInput.error = null
        startPickerDialog = null

        val finishTime = record.finishTime
        finishInput.text = if (finishTime > 0L)
            DateUtils.formatDateTime(context, finishTime, FORMAT_DATE_BUTTON)
        else
            ""
        finishInput.error = null
        finishPickerDialog = null

        noteInput.setText(record.note)
    }

    fun bindRecord(record: TimeRecord) {
        record.note = noteInput.text.toString()
    }

    private fun pickStartTime() {
        if (startPickerDialog == null) {
            val cal = getCalendar(record.start)
            val listener = TimePickerDialog.OnTimeSetListener { picker, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                record.start = cal
                startInput.text = DateUtils.formatDateTime(context, cal.timeInMillis, FORMAT_DATE_BUTTON)
                startInput.error = null
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
                finishInput.text = DateUtils.formatDateTime(context, cal.timeInMillis, FORMAT_DATE_BUTTON)
                finishInput.error = null
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

    fun validateForm(record: TimeRecord): Boolean {
        var valid = true

        if (record.project.id <= 0) {
            valid = false
            (projectInput.selectedView as TextView).error = getString(R.string.error_field_required)
        } else {
            (projectInput.selectedView as TextView).error = null
        }
        if (record.task.id <= 0) {
            valid = false
            (taskInput.selectedView as TextView).error = getString(R.string.error_field_required)
        } else {
            (taskInput.selectedView as TextView).error = null
        }
        if (record.start == null) {
            valid = false
            startInput.error = getString(R.string.error_field_required)
        } else {
            startInput.error = null
        }
        if (record.finish == null) {
            valid = false
            finishInput.error = getString(R.string.error_field_required)
        } else if (record.startTime + DateUtils.MINUTE_IN_MILLIS > record.finishTime) {
            valid = false
            finishInput.error = getString(R.string.error_finish_time_before_start_time)
        } else {
            finishInput.error = null
        }

        return valid
    }

    private fun filterTasks(project: Project) {
        val context: Context = requireContext()
        val filtered = project.tasks
        val options = ArrayList<ProjectTask>(filtered.size + 1)
        options.add(taskEmpty)
        options.addAll(filtered)
        taskInput.adapter = ArrayAdapter<ProjectTask>(context, android.R.layout.simple_list_item_1, options)
        taskInput.setSelection(options.indexOf(record.task))
    }

    private fun projectItemSelected(project: Project) {
        record.project = project
        filterTasks(project)
    }

    private fun taskItemSelected(task: ProjectTask) {
        record.task = task
    }

    override fun handleIntent(intent: Intent, savedInstanceState: Bundle?) {
        super.handleIntent(intent, savedInstanceState)
        val args = arguments ?: Bundle()
        if (intent.extras != null) {
            args.putAll(intent.extras)
        }
        arguments = args
        date.timeInMillis = args.getLong(EXTRA_DATE, date.timeInMillis)

        var recordId = record.id

        val extras = intent.extras
        if (extras != null) {
            recordId = extras.getLong(EXTRA_RECORD, recordId)
        }
        if (savedInstanceState != null) {
            date.timeInMillis = savedInstanceState.getLong(STATE_DATE, date.timeInMillis)
            recordId = savedInstanceState.getLong(STATE_RECORD_ID, recordId)
            record.id = recordId
        }
        loadPage()
            .subscribe({
                populateForm(record)
                bindForm(record)
                if (projects.isEmpty() or tasks.isEmpty() or (record.id != recordId)) {
                    fetchPage(date, recordId)
                }
            }, { err ->
                Timber.e(err, "Error loading page: ${err.message}")
                showProgress(false)
            })
            .addTo(disposables)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_AUTHENTICATE) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                fetchPage(date, record.id)
            } else {
                activity?.finish()
            }
            return
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    fun fetchPage(date: Calendar, id: Long) {
        val context: Context = requireContext()
        val dateFormatted = formatSystemDate(date)
        Timber.d("fetchPage $dateFormatted")
        // Show a progress spinner, and kick off a background task to perform the user login attempt.
        showProgress(true)

        val service = TimeTrackerServiceFactory.createPlain(context, preferences)

        val fetcher: Single<Response<String>> = if (id == TikalEntity.ID_NONE) {
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
                    savePage()
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

    private fun loadPage(): Single<Unit> {
        return Single.fromCallable { loadFormFromDb() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    fun savePage() {
        return saveFormToDb()
    }

    fun authenticate(immediate: Boolean = false) {
        val intent = Intent(context, LoginActivity::class.java)
        intent.putExtra(LoginFragment.EXTRA_SUBMIT, immediate)
        startActivityForResult(intent, REQUEST_AUTHENTICATE)
    }

    fun submit() {
        val record = this.record

        if (!validateForm(record)) {
            return
        }
        bindRecord(record)

        if (record.id == TikalEntity.ID_NONE) {
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
            errorLabel.text = ""
        }

        val service = TimeTrackerServiceFactory.createPlain(context, preferences)

        val submitter: Single<Response<String>> = if (record.id == TikalEntity.ID_NONE) {
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
                            //FIXME call listener
                            activity?.setResult(AppCompatActivity.RESULT_OK)
                            activity?.finish()
                        }
                    } else {
                        errorLabel.text = errorMessage
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

    fun deleteRecord() {
        if (record.id == TikalEntity.ID_NONE) {
            //FIXME call listener
            activity?.setResult(AppCompatActivity.RESULT_OK)
            activity?.finish()
        } else {
            deleteRecord(record)
        }
    }

    private fun deleteRecord(record: TimeRecord) {
        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        showProgress(true)

        val service = TimeTrackerServiceFactory.createPlain(context, preferences)

        service.deleteTime(record.id)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                if (isValidResponse(response)) {
                    showProgress(false)
                    //FIXME call listener
                    activity?.setResult(AppCompatActivity.RESULT_OK)
                    activity?.finish()
                } else {
                    authenticate(true)
                }
            }, { err ->
                Timber.e(err, "Error deleting record: ${err.message}")
                showProgress(false)
            })
            .addTo(disposables)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        bindRecord(record)
        outState.putLong(STATE_RECORD_ID, record.id)
        outState.putParcelable(STATE_RECORD, record)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val recordParcel = savedInstanceState.getParcelable<TimeRecord>(STATE_RECORD)

        if (recordParcel != null) {
            record = recordParcel
            bindForm(record)
        } else {
            record.id = savedInstanceState.getLong(STATE_RECORD_ID)
        }
    }

    companion object {
        const val REQUEST_AUTHENTICATE = 0xAECA
    }
}