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

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.MainThread
import androidx.navigation.fragment.findNavController
import com.tikalk.app.findParentFragment
import com.tikalk.app.isNavDestination
import com.tikalk.app.runOnUiThread
import com.tikalk.html.selectByName
import com.tikalk.html.value
import com.tikalk.worktracker.R
import com.tikalk.worktracker.app.TrackerFragment
import com.tikalk.worktracker.auth.LoginFragment
import com.tikalk.worktracker.db.toTimeRecord
import com.tikalk.worktracker.db.toTimeRecordEntity
import com.tikalk.worktracker.model.*
import com.tikalk.worktracker.model.time.TaskRecordStatus
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.model.time.split
import com.tikalk.worktracker.net.InternetFragment
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.time_form.*
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.FormElement
import retrofit2.Response
import timber.log.Timber
import java.util.*
import kotlin.math.max

class TimeEditFragment : TimeFormFragment() {

    private var date: Calendar = Calendar.getInstance()
    var listener: OnEditRecordListener? = null

    private var startPickerDialog: TimePickerDialog? = null
    private var finishPickerDialog: TimePickerDialog? = null
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
        startInput.setOnClickListener { pickStartTime() }
        finishInput.setOnClickListener { pickFinishTime() }
    }

    /** Populate the record and then bind the form. */
    private fun populateForm(date: Calendar, html: String, id: Long) {
        populateForm(date, html)

        record.id = id
        record.status = if (id == TikalEntity.ID_NONE) TaskRecordStatus.DRAFT else TaskRecordStatus.CURRENT

        populateAndBind()
    }

    override fun populateForm(date: Calendar, doc: Document) {
        super.populateForm(date, doc)
        errorMessage = findError(doc)?.trim() ?: ""
    }

    override fun populateForm(date: Calendar, doc: Document, form: FormElement, inputProjects: Element, inputTasks: Element) {
        super.populateForm(date, doc, form, inputProjects, inputTasks)

        val inputStart = form.selectByName("start") ?: return
        val startValue = inputStart.value()
        record.start = parseSystemTime(date, startValue)

        val inputFinish = form.selectByName("finish") ?: return
        val finishValue = inputFinish.value()
        record.finish = parseSystemTime(date, finishValue)

        val inputNote = form.selectByName("note")
        val noteValue = inputNote?.value() ?: ""
        record.note = noteValue
    }

    override fun populateForm(record: TimeRecord) {
        Timber.v("populateForm record=$record")
        if (record.id == TikalEntity.ID_NONE) {
            val args = arguments
            if (args != null) {
                if (args.containsKey(EXTRA_PROJECT_ID)) {
                    val projectId = args.getLong(EXTRA_PROJECT_ID)
                    setRecordProject(projects.firstOrNull { it.id == projectId } ?: record.project)
                }
                if (args.containsKey(EXTRA_TASK_ID)) {
                    val taskId = args.getLong(EXTRA_TASK_ID)
                    setRecordTask(tasks.firstOrNull { it.id == taskId } ?: record.task)
                }
                if (args.containsKey(EXTRA_START_TIME)) {
                    val startTime = args.getLong(EXTRA_START_TIME)
                    if (startTime > 0L) {
                        record.startTime = startTime
                    } else {
                        record.start = null
                    }
                }
                if (args.containsKey(EXTRA_FINISH_TIME)) {
                    val finishTime = args.getLong(EXTRA_FINISH_TIME)
                    if (finishTime > 0L) {
                        record.finishTime = finishTime
                    } else {
                        record.finish = null
                    }
                }
            }
        }

        if (record.project.isNullOrEmpty() and record.task.isNullOrEmpty()) {
            val projectFavorite = preferences.getFavoriteProject()
            if (projectFavorite != TikalEntity.ID_NONE) {
                record.project = projects.firstOrNull { it.id == projectFavorite } ?: projectEmpty
            }
            val taskFavorite = preferences.getFavoriteTask()
            if (taskFavorite != TikalEntity.ID_NONE) {
                record.task = tasks.firstOrNull { it.id == taskFavorite } ?: taskEmpty
            }
        }
    }

    override fun bindForm(record: TimeRecord) {
        Timber.v("bindForm record=$record")
        if (!isVisible) return
        val context: Context = requireContext()

        // Populate the tasks spinner before projects so that it can be filtered.
        val taskItems = arrayOf(taskEmpty)
        taskInput.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, taskItems)

        val projectItems = projects.toTypedArray()
        projectInput.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, projectItems)
        if (projectItems.isNotEmpty()) {
            projectInput.setSelection(max(0, findProject(projectItems, record.project)))
            projectItemSelected(record.project)
        }
        projectInput.requestFocus()

        val startTime = record.startTime
        startInput.text = if (startTime > 0L)
            DateUtils.formatDateTime(context, startTime, FORMAT_TIME_BUTTON)
        else
            ""
        startInput.error = null
        startPickerDialog = null

        val finishTime = record.finishTime
        finishInput.text = if (finishTime > 0L)
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

    private fun pickStartTime() {
        val cal = getCalendar(record.start)
        val hour = cal.hourOfDay
        val minute = cal.minute
        var picker = startPickerDialog
        if (picker == null) {
            val context = requireContext()
            val listener = TimePickerDialog.OnTimeSetListener { _, pickedHour, pickedMinute ->
                cal.hourOfDay = pickedHour
                cal.minute = pickedMinute
                record.start = cal
                startInput.text = DateUtils.formatDateTime(context, cal.timeInMillis, FORMAT_TIME_BUTTON)
                startInput.error = null
            }
            picker = TimePickerDialog(context, listener, hour, minute, DateFormat.is24HourFormat(context))
            startPickerDialog = picker
        } else {
            picker.updateTime(hour, minute)
        }
        picker.show()
    }

    private fun pickFinishTime() {
        val cal = getCalendar(record.finish)
        val hour = cal.hourOfDay
        val minute = cal.minute
        var picker = finishPickerDialog
        if (picker == null) {
            val listener = TimePickerDialog.OnTimeSetListener { _, pickedHour, pickedMinute ->
                cal.hourOfDay = pickedHour
                cal.minute = pickedMinute
                record.finish = cal
                finishInput.text = DateUtils.formatDateTime(context, cal.timeInMillis, FORMAT_TIME_BUTTON)
                finishInput.error = null
            }
            picker = TimePickerDialog(context, listener, hour, minute, DateFormat.is24HourFormat(context))
            finishPickerDialog = picker
        } else {
            picker.updateTime(hour, minute)
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

        projectInputView.error = null
        projectInputView.isFocusableInTouchMode = false
        taskInputView.error = null
        taskInputView.isFocusableInTouchMode = false
        startInput.error = null
        startInput.isFocusableInTouchMode = false
        finishInput.error = null
        finishInput.isFocusableInTouchMode = false
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
        val context: Context = requireContext()
        val filtered = project.tasks
        val options = ArrayList<ProjectTask>(filtered.size + 1)
        options.add(taskEmpty)
        options.addAll(filtered)
        taskInput.adapter = ArrayAdapter<ProjectTask>(context, android.R.layout.simple_list_item_1, options)
        taskInput.setSelection(findTask(options, record.task))
    }

    private fun projectItemSelected(project: Project) {
        Timber.v("projectItemSelected $project")
        record.project = project
        filterTasks(project)
    }

    private fun taskItemSelected(task: ProjectTask) {
        Timber.v("taskItemSelected $task")
        record.task = task
    }

    fun run() {
        Timber.v("run")
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

        loadPage(recordId)
            .subscribe({
                populateForm(record)
                bindForm(record)
                maybeFetchPage(date, recordId)
            }, { err ->
                Timber.e(err, "Error loading page: ${err.message}")
                showProgress(false)
            })
            .addTo(disposables)
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

    private fun fetchPage(date: Calendar, id: Long) {
        val dateFormatted = formatSystemDate(date)
        Timber.v("fetchPage $dateFormatted id=$id")
        // Show a progress spinner, and kick off a background task to fetch the page.
        showProgress(true)

        // Fetch from remote server.
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
                    processPage(date, id, body)
                    showProgressMain(false)
                } else {
                    authenticateMain()
                }
            }, { err ->
                Timber.e(err, "Error fetching page: ${err.message}")
                handleErrorMain(err)
                showProgressMain(false)
            })
            .addTo(disposables)
    }

    private fun maybeFetchPage(date: Calendar, id: Long) {
        if (projects.isEmpty() or tasks.isEmpty() or (id != record.id)) {
            fetchPage(date, id)
        }
    }

    private fun processPage(date: Calendar, id: Long, html: String) {
        populateForm(date, html, id)
        savePage()
    }

    private fun loadPage(recordId: Long = TikalEntity.ID_NONE): Single<Unit> {
        return Single.fromCallable { loadFormFromDb(recordId) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    private fun loadFormFromDb(recordId: Long = TikalEntity.ID_NONE) {
        loadFormFromDb(db)
        loadRecord(recordId)
    }

    private fun loadRecord(recordId: Long) {
        if (recordId != TikalEntity.ID_NONE) {
            val recordsDao = db.timeRecordDao()
            val recordEntity = recordsDao.queryById(recordId)
            if (recordEntity != null) {
                setRecordValue(recordEntity.toTimeRecord(projects, tasks))
            }
        }
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
        Timber.v("authenticate submit=$submit")
        if (!isNavDestination(R.id.loginFragment)) {
            val args = Bundle()
            requireFragmentManager().putFragment(args, LoginFragment.EXTRA_CALLER, this)
            args.putBoolean(LoginFragment.EXTRA_SUBMIT, submit)
            findNavController().navigate(R.id.action_timeEdit_to_login, args)
        }
    }

    private fun submit() {
        val record = this.record
        Timber.v("submit $record")

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
        Timber.v("submit $record first=$first last=$last")
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
            .subscribe({ response ->
                if (record.id != TikalEntity.ID_NONE) {
                    saveRecord(record)
                }

                if (last) {
                    showProgressMain(false)
                }

                if (isValidResponse(response)) {
                    val body = response.body()!!
                    processSubmittedPage(body, last)
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

    private fun processSubmittedPage(html: String, last: Boolean) {
        Timber.v("processSubmittedPage last=$last")
        val errorMessage = getResponseError(html)
        if (errorMessage.isNullOrEmpty()) {
            listener?.onRecordEditSubmitted(this, record, last)
        } else {
            setErrorLabelMain(errorMessage)
            listener?.onRecordEditFailure(this, record, errorMessage)
        }
    }

    private fun deleteRecord() {
        deleteRecord(record)
    }

    private fun deleteRecord(record: TimeRecord) {
        Timber.v("deleteRecord $record")
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
                    listener?.onRecordEditDeleted(this, record)
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
        outState.putLong(STATE_RECORD_ID, record.id)
        outState.putParcelable(STATE_RECORD, record)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        date.timeInMillis = savedInstanceState.getLong(STATE_DATE)
        val recordParcel = savedInstanceState.getParcelable<TimeRecord>(STATE_RECORD)

        if (recordParcel != null) {
            setRecordValue(recordParcel)
            bindForm(record)
        } else {
            record.id = savedInstanceState.getLong(STATE_RECORD_ID)
        }
    }

    override fun markFavorite(record: TimeRecord) {
        super.markFavorite(record)
        listener?.onRecordEditFavorited(this, record)
    }

    fun editRecord(record: TimeRecord, date: Calendar) {
        Timber.v("editRecord record=$record")
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == LoginFragment.REQUEST_LOGIN) {
            if (resultCode == Activity.RESULT_OK) {
                maybeFetchPage(date, record.id)
            } else {
                activity?.finish()
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    @MainThread
    private fun setErrorLabel(text: CharSequence) {
        errorLabel.text = text
        errorLabel.visibility = if (text.isBlank()) View.GONE else View.VISIBLE
    }

    private fun setErrorLabelMain(text: CharSequence) {
        runOnUiThread { setErrorLabel(text) }
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
         */
        fun onRecordEditSubmitted(fragment: TimeEditFragment, record: TimeRecord, last: Boolean = true)

        /**
         * The record was deleted.
         * @param fragment the editor fragment.
         * @param record the record.
         */
        fun onRecordEditDeleted(fragment: TimeEditFragment, record: TimeRecord)

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

        private const val STATE_DATE = "date"
    }
}