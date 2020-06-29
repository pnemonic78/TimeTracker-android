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

import android.content.Context
import android.os.Bundle
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.MainThread
import androidx.navigation.fragment.findNavController
import com.tikalk.app.DateTimePickerDialog
import com.tikalk.app.DateTimePickerDialog.OnDateTimeSetListener
import com.tikalk.app.findParentFragment
import com.tikalk.app.isNavDestination
import com.tikalk.app.runOnUiThread
import com.tikalk.widget.DateTimePicker
import com.tikalk.worktracker.R
import com.tikalk.worktracker.app.TrackerFragment
import com.tikalk.worktracker.auth.LoginFragment
import com.tikalk.worktracker.db.TimeRecordEntity
import com.tikalk.worktracker.db.toTimeRecord
import com.tikalk.worktracker.db.toTimeRecordEntity
import com.tikalk.worktracker.model.*
import com.tikalk.worktracker.model.time.TimeEditPage
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.model.time.split
import com.tikalk.worktracker.net.InternetFragment
import com.tikalk.worktracker.report.LocationItem
import com.tikalk.worktracker.report.findLocation
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.time_form.*
import retrofit2.Response
import timber.log.Timber
import java.util.*
import kotlin.math.max

class TimeEditFragment : TimeFormFragment() {

    private var date: Calendar = Calendar.getInstance()
    var listener: OnEditRecordListener? = null

    private var startPickerDialog: DateTimePickerDialog? = null
    private var finishPickerDialog: DateTimePickerDialog? = null
    private var errorMessage: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        val caller = this.caller
        if (caller != null) {
            if (caller is OnEditRecordListener) {
                this.listener = caller
            }
        } else {
            val activity = this.activity
            if (activity != null) {
                if (activity is OnEditRecordListener) {
                    this.listener = activity
                }
            }
        }
    }

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
        locationInput.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(adapterView: AdapterView<*>) {
                locationItemSelected(locationEmpty)
            }

            override fun onItemSelected(adapterView: AdapterView<*>, view: View?, position: Int, id: Long) {
                val task = adapterView.adapter.getItem(position) as LocationItem
                locationItemSelected(task)
            }
        }
        startInput.setOnClickListener { pickStartTime() }
        finishInput.setOnClickListener { pickFinishTime() }
    }

    override fun populateForm(record: TimeRecord) {
        Timber.i("populateForm record=$record")

        if (record.id == TikalEntity.ID_NONE) {
            val args = arguments
            if (args != null) {
                if (args.containsKey(EXTRA_PROJECT_ID)) {
                    val projectId = args.getLong(EXTRA_PROJECT_ID)
                    val projects = projectsData.value
                    setRecordProject(projects?.find { it.id == projectId } ?: projectEmpty)
                }
                if (args.containsKey(EXTRA_TASK_ID)) {
                    val taskId = args.getLong(EXTRA_TASK_ID)
                    val tasks = record.project.tasks
                    setRecordTask(tasks.find { it.id == taskId } ?: taskEmpty)
                }
                if (args.containsKey(EXTRA_START_TIME)) {
                    val startTime = args.getLong(EXTRA_START_TIME)
                    if (startTime != TimeRecord.NEVER) {
                        record.startTime = startTime
                    } else {
                        record.start = null
                    }
                }
                if (args.containsKey(EXTRA_FINISH_TIME)) {
                    val finishTime = args.getLong(EXTRA_FINISH_TIME)
                    if (finishTime != TimeRecord.NEVER) {
                        record.finishTime = finishTime
                    } else {
                        record.finish = null
                    }
                }
                if (args.containsKey(EXTRA_LOCATION)) {
                    val locationId = args.getLong(EXTRA_LOCATION, TikalEntity.ID_NONE)
                    record.location = Location.valueOf(locationId)
                }
            }
        }

        if (record.project.isNullOrEmpty() and record.task.isNullOrEmpty()) {
            applyFavorite()
        }
    }

    override fun bindForm(record: TimeRecord) {
        Timber.i("bindForm record=$record")
        val context = this.context ?: return
        if (!isVisible) return

        // Populate the tasks spinner before projects so that it can be filtered.
        val taskItems = arrayOf(taskEmpty)
        taskInput.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, taskItems)

        val projects = projectsData.value
        bindProjects(context, record, projects)

        bindLocation(context, record)

        val startTime = record.startTime
        startInput.text = if (startTime != TimeRecord.NEVER)
            DateUtils.formatDateTime(context, startTime, FORMAT_TIME_BUTTON)
        else
            ""
        startInput.error = null
        startPickerDialog = null

        val finishTime = record.finishTime
        finishInput.text = if (finishTime != TimeRecord.NEVER)
            DateUtils.formatDateTime(context, finishTime, FORMAT_TIME_BUTTON)
        else
            ""
        finishInput.error = null
        finishPickerDialog = null

        noteInput.setText(record.note)

        setErrorLabel(errorMessage)
    }

    private fun bindRecord(record: TimeRecord) {
        record.note = noteInput.text.toString()
    }

    private fun bindProjects(context: Context, record: TimeRecord, projects: List<Project>?) {
        Timber.i("bindProjects record=$record projects=$projects")
        if (projectInput == null) return
        val projectItems = projects?.toTypedArray() ?: emptyArray()
        projectInput.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, projectItems)
        if (projectItems.isNotEmpty()) {
            projectInput.setSelection(max(0, findProject(projectItems, record.project)))
            projectItemSelected(record.project)
        }
        projectInput.requestFocus()
    }

    private fun bindLocation(context: Context, record: TimeRecord) {
        Timber.i("bindLocation record=$record")
        if (locationInput == null) return
        val locations = buildLocations()
        locationInput.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, locations)
        if (locations.isNotEmpty()) {
            val index = findLocation(locations, record.location)
            locationInput.setSelection(max(0, index))
            val selectedItem = if (index >= 0) locations[index] else locationEmpty
            locationItemSelected(selectedItem)
        }
    }

    private fun pickStartTime() {
        val cal = getCalendar(record.start)
        val year = cal.year
        val month = cal.month
        val dayOfMonth = cal.dayOfMonth
        val hour = cal.hourOfDay
        val minute = cal.minute
        var picker = startPickerDialog
        if (picker == null) {
            val context = requireContext()
            val listener = object : OnDateTimeSetListener {
                override fun onDateTimeSet(view: DateTimePicker, year: Int, month: Int, dayOfMonth: Int, hourOfDay: Int, minute: Int) {
                    cal.year = year
                    cal.month = month
                    cal.dayOfMonth = dayOfMonth
                    cal.hourOfDay = hourOfDay
                    cal.minute = minute
                    record.start = cal
                    startInput.text = DateUtils.formatDateTime(context, cal.timeInMillis, FORMAT_TIME_BUTTON)
                    startInput.error = null
                }
            }
            picker = DateTimePickerDialog(context, listener, year, month, dayOfMonth, hour, minute, DateFormat.is24HourFormat(context))
            startPickerDialog = picker
        } else {
            picker.updateDateTime(year, month, dayOfMonth, hour, minute)
        }
        picker.show()
    }

    private fun pickFinishTime() {
        val cal = getCalendar(record.finish)
        val year = cal.year
        val month = cal.month
        val dayOfMonth = cal.dayOfMonth
        val hour = cal.hourOfDay
        val minute = cal.minute
        var picker = finishPickerDialog
        if (picker == null) {
            val context = requireContext()
            val listener = object : OnDateTimeSetListener {
                override fun onDateTimeSet(view: DateTimePicker, year: Int, month: Int, dayOfMonth: Int, hourOfDay: Int, minute: Int) {
                    cal.year = year
                    cal.month = month
                    cal.dayOfMonth = dayOfMonth
                    cal.hourOfDay = hourOfDay
                    cal.minute = minute
                    record.finish = cal
                    finishInput.text = DateUtils.formatDateTime(context, cal.timeInMillis, FORMAT_TIME_BUTTON)
                    finishInput.error = null
                }
            }
            picker = DateTimePickerDialog(context, listener, year, month, dayOfMonth, hour, minute, DateFormat.is24HourFormat(context))
            finishPickerDialog = picker
        } else {
            picker.updateDateTime(year, month, dayOfMonth, hour, minute)
        }
        picker.show()
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
        val projectInputView = projectInput.selectedView as TextView
        val taskInputView = taskInput.selectedView as TextView
        val locationInputView = locationInput.selectedView as TextView

        projectInputView.error = null
        projectInputView.isFocusableInTouchMode = false
        taskInputView.error = null
        taskInputView.isFocusableInTouchMode = false
        startInput.error = null
        startInput.isFocusableInTouchMode = false
        finishInput.error = null
        finishInput.isFocusableInTouchMode = false
        locationInputView.error = null
        locationInputView.isFocusableInTouchMode = false
        setErrorLabel("")

        if (record.project.id == TikalEntity.ID_NONE) {
            projectInputView.error = getText(R.string.error_project_field_required)
            setErrorLabel(getText(R.string.error_project_field_required))
            projectInputView.isFocusableInTouchMode = true
            projectInputView.post { projectInputView.requestFocus() }
            return false
        }
        if (record.task.id == TikalEntity.ID_NONE) {
            taskInputView.error = getText(R.string.error_task_field_required)
            setErrorLabel(getText(R.string.error_task_field_required))
            taskInputView.isFocusableInTouchMode = true
            taskInputView.post { taskInputView.requestFocus() }
            return false
        }
        if (record.start == null) {
            startInput.error = getText(R.string.error_start_field_required)
            setErrorLabel(getText(R.string.error_start_field_required))
            startInput.isFocusableInTouchMode = true
            startInput.requestFocus()
            return false
        }
        if (record.finish == null) {
            finishInput.error = getText(R.string.error_finish_field_required)
            setErrorLabel(getText(R.string.error_finish_field_required))
            finishInput.isFocusableInTouchMode = true
            finishInput.requestFocus()
            return false
        }
        if (record.startTime + DateUtils.MINUTE_IN_MILLIS > record.finishTime) {
            finishInput.error = getText(R.string.error_finish_time_before_start_time)
            setErrorLabel(getText(R.string.error_finish_time_before_start_time))
            finishInput.isFocusableInTouchMode = true
            finishInput.requestFocus()
            return false
        }

        return true
    }

    private fun filterTasks(project: Project) {
        val context = this.context ?: return
        val options = addEmpty(project.tasks)
        if (taskInput == null) return
        taskInput.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, options)
        taskInput.setSelection(findTask(options, record.task))
    }

    private fun projectItemSelected(project: Project) {
        Timber.i("projectItemSelected $project")
        setRecordProject(project)
        filterTasks(project)
    }

    private fun taskItemSelected(task: ProjectTask) {
        Timber.i("taskItemSelected $task")
        setRecordTask(task)
    }

    private fun locationItemSelected(location: LocationItem) {
        Timber.d("remoteItemSelected location=$location")
        setRecordLocation(location.location)
    }

    fun run() {
        Timber.i("run first=$firstRun")
        val args = arguments ?: Bundle()
        if (args.isEmpty) {
            if (view?.visibility != View.VISIBLE) {
                return
            }
            // The parent fragment should be responsible for authentication.
            if (findParentFragment(InternetFragment::class.java) != null) {
                return
            }
        }
        date.timeInMillis = args.getLong(EXTRA_DATE, date.timeInMillis)

        val recordId = args.getLong(EXTRA_RECORD_ID, record.id)

        dataSource.editPage(recordId, firstRun)
            .subscribeOn(Schedulers.io())
            .doOnSubscribe { showProgressMain(true) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ page ->
                processPage(page)
                populateAndBind()
                showProgress(false)
            }, { err ->
                Timber.e(err, "Error loading page: ${err.message}")
                showProgress(false)
                handleError(err)
            })
            .addTo(disposables)
    }

    private fun processPage(page: TimeEditPage) {
        projectsData.value = addEmpties(page.projects)
        errorMessage = page.errorMessage ?: ""
        setRecordValue(page.record)
    }

    override fun onStart() {
        super.onStart()
        run()
    }

    override fun onLoginSuccess(fragment: LoginFragment, login: String) {
        super.onLoginSuccess(fragment, login)
        run()
    }

    override fun onLoginFailure(fragment: LoginFragment, login: String, reason: String) {
        super.onLoginFailure(fragment, login, reason)
        activity?.finish()
    }

    private fun saveRecord(record: TimeRecord) {
        val recordDao = db.timeRecordDao()
        if (record.id == TikalEntity.ID_NONE) {
            recordDao.insert(record.toTimeRecordEntity())
        } else {
            recordDao.update(record.toTimeRecordEntity())
        }
    }

    override fun authenticate(submit: Boolean) {
        Timber.i("authenticate submit=$submit currentDestination=${findNavController().currentDestination?.label}")
        if (!isNavDestination(R.id.loginFragment)) {
            val args = Bundle()
            parentFragmentManager.putFragment(args, LoginFragment.EXTRA_CALLER, this)
            args.putBoolean(LoginFragment.EXTRA_SUBMIT, submit)
            findNavController().navigate(R.id.action_timeEdit_to_login, args)
        }
    }

    private fun submit() {
        val record = this.record
        Timber.i("submit $record")

        bindRecord(record)
        if (!validateForm(record)) {
            return
        }

        if (record.id == TikalEntity.ID_NONE) {
            val splits = record.split()
            val size = splits.size
            val lastIndex = size - 1
            submit(splits[0], true, 0 == lastIndex)
            if (size > 1) {
                for (i in 1 until size) {
                    submit(splits[i], false, i == lastIndex)
                }
            }
        } else {
            submit(record, first = true, last = true)
        }
    }

    private fun submit(record: TimeRecord, first: Boolean = true, last: Boolean = true) {
        Timber.i("submit $record first=$first last=$last")
        // Show a progress spinner, and kick off a background task to submit the form.
        if (first) {
            showProgress(true)
            setErrorLabel("")
        }

        val submitter: Single<Response<String>> = if (record.id == TikalEntity.ID_NONE) {
            service.addTime(record.project.id,
                record.task.id,
                formatSystemDate(record.start),
                formatSystemTime(record.start),
                formatSystemTime(record.finish),
                record.note,
                record.location.id)
        } else {
            service.editTime(record.id,
                record.project.id,
                record.task.id,
                formatSystemDate(record.start),
                formatSystemTime(record.start),
                formatSystemTime(record.finish),
                record.note,
                record.location.id)
        }
        submitter
            .subscribeOn(Schedulers.io())
            .subscribe({ response ->
                if (record.id != TikalEntity.ID_NONE) {
                    saveRecord(record)
                }

                if (last) {
                    showProgressMain(false)
                }

                if (isValidResponse(response)) {
                    val html = response.body()!!
                    processSubmittedPage(record, html, last)
                } else {
                    authenticateMain(true)
                }
            }, { err ->
                Timber.e(err, "Error saving record: ${err.message}")
                handleErrorMain(err)
                showProgressMain(false)
            })
            .addTo(disposables)
    }

    private fun processSubmittedPage(record: TimeRecord, html: String, last: Boolean) {
        Timber.i("processSubmittedPage last=$last")
        val errorMessage = getResponseError(html)
        if (errorMessage.isNullOrEmpty()) {
            listener?.onRecordEditSubmitted(this, record, last, html)
        } else {
            setErrorLabelMain(errorMessage)
            listener?.onRecordEditFailure(this, record, errorMessage)
        }
    }

    private fun deleteRecord() {
        deleteRecord(record)
    }

    private fun deleteRecord(record: TimeRecord) {
        Timber.i("deleteRecord $record")
        if (record.id == TikalEntity.ID_NONE) {
            listener?.onRecordEditDeleted(this, record)
            return
        }

        // Show a progress spinner, and kick off a background task to fetch the page.
        showProgress(true)

        // Fetch from remote server.
        service.deleteTime(record.id)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                showProgress(false)
                if (isValidResponse(response)) {
                    val html = response.body()!!
                    listener?.onRecordEditDeleted(this, record, html)
                } else {
                    authenticate()
                }
            }, { err ->
                Timber.e(err, "Error deleting record: ${err.message}")
                handleError(err)
                showProgress(false)
            })
            .addTo(disposables)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (isVisible) {
            bindRecord(record)
        }
        outState.putLong(STATE_DATE, date.timeInMillis)
        outState.putParcelable(STATE_RECORD, record.toTimeRecordEntity())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        date.timeInMillis = savedInstanceState.getLong(STATE_DATE)
        val recordParcel = savedInstanceState.getParcelable<TimeRecordEntity>(STATE_RECORD)

        if (recordParcel != null) {
            val projects = projectsData.value
            val record = recordParcel.toTimeRecord(projects)
            setRecordValue(record)
            bindForm(record)
        }
    }

    override fun markFavorite(record: TimeRecord) {
        super.markFavorite(record)
        listener?.onRecordEditFavorited(this, record)
    }

    fun editRecord(record: TimeRecord, date: Calendar) {
        Timber.i("editRecord record=$record")
        setRecordValue(record.copy())
        this.date = date
        var args = arguments
        if (args == null) {
            args = Bundle()
            arguments = args
        }
        args.clear()
        args.putLong(EXTRA_DATE, date.timeInMillis)
        args.putLong(EXTRA_PROJECT_ID, record.project.id)
        args.putLong(EXTRA_TASK_ID, record.task.id)
        args.putLong(EXTRA_START_TIME, record.startTime)
        args.putLong(EXTRA_FINISH_TIME, record.finishTime)
        args.putLong(EXTRA_RECORD_ID, record.id)
        args.putLong(EXTRA_LOCATION, record.location.id)
        run()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (view?.visibility == View.VISIBLE) {
            inflater.inflate(R.menu.time_edit, menu)
        }
        return super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (view?.visibility != View.VISIBLE) {
            return false
        }
        when (item.itemId) {
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

    @MainThread
    private fun setErrorLabel(text: CharSequence) {
        errorLabel.text = text
        errorLabel.visibility = if (text.isBlank()) View.GONE else View.VISIBLE
    }

    private fun setErrorLabelMain(text: CharSequence) {
        runOnUiThread { setErrorLabel(text) }
    }

    override fun onProjectsUpdated(projects: List<Project>) {
        super.onProjectsUpdated(projects)
        bindProjects(requireContext(), record, projects)
    }

    /**
     * Listener for editing a record callbacks.
     */
    interface OnEditRecordListener {
        /**
         * The record was submitted.
         * @param fragment the editor fragment.
         * @param record the record.
         * @param last is this the last record in a series that was submitted?
         * @param responseHtml the response HTML.
         */
        fun onRecordEditSubmitted(fragment: TimeEditFragment, record: TimeRecord, last: Boolean = true, responseHtml: String = "")

        /**
         * The record was deleted.
         * @param fragment the editor fragment.
         * @param record the record.
         * @param responseHtml the response HTML.
         */
        fun onRecordEditDeleted(fragment: TimeEditFragment, record: TimeRecord, responseHtml: String = "")

        /**
         * The record was marked as favorite.
         * @param fragment the editor fragment.
         * @param record the record.
         */
        fun onRecordEditFavorited(fragment: TimeEditFragment, record: TimeRecord)

        /**
         * Editing record failed.
         * @param fragment the login fragment.
         * @param record the record.
         * @param reason the failure reason.
         */
        fun onRecordEditFailure(fragment: TimeEditFragment, record: TimeRecord, reason: String)
    }

    companion object {
        const val EXTRA_CALLER = TrackerFragment.EXTRA_CALLER
        const val EXTRA_DATE = TimeFormFragment.EXTRA_DATE
        const val EXTRA_PROJECT_ID = TimeFormFragment.EXTRA_PROJECT_ID
        const val EXTRA_TASK_ID = TimeFormFragment.EXTRA_TASK_ID
        const val EXTRA_START_TIME = TimeFormFragment.EXTRA_START_TIME
        const val EXTRA_FINISH_TIME = TimeFormFragment.EXTRA_FINISH_TIME
        const val EXTRA_RECORD_ID = TimeFormFragment.EXTRA_RECORD_ID
        const val EXTRA_LOCATION = TimeFormFragment.EXTRA_LOCATION

        private const val STATE_DATE = "date"
    }
}
