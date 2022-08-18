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
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
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
import com.tikalk.worktracker.auth.LoginFragment
import com.tikalk.worktracker.databinding.TimeFormBinding
import com.tikalk.worktracker.db.TimeRecordEntity
import com.tikalk.worktracker.db.toTimeRecord
import com.tikalk.worktracker.db.toTimeRecordEntity
import com.tikalk.worktracker.model.Location
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.findProject
import com.tikalk.worktracker.model.findTask
import com.tikalk.worktracker.model.isNullOrEmpty
import com.tikalk.worktracker.model.time.TaskRecordStatus
import com.tikalk.worktracker.model.time.TimeEditPage
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.model.time.split
import com.tikalk.worktracker.net.InternetFragment
import com.tikalk.worktracker.report.LocationItem
import com.tikalk.worktracker.report.findLocation
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.schedulers.Schedulers
import retrofit2.Response
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max

class TimeEditFragment : TimeFormFragment() {

    private var _binding: TimeFormBinding? = null
    private val binding get() = _binding!!

    private var date: Calendar = Calendar.getInstance()

    private var startPickerDialog: DateTimePickerDialog? = null
    private var finishPickerDialog: DateTimePickerDialog? = null
    private var errorMessage: String = ""
    private val recordsToSubmit = CopyOnWriteArrayList<TimeRecord>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = TimeFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.projectInput.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(adapterView: AdapterView<*>) {
                //projectItemSelected(projectEmpty)
            }

            override fun onItemSelected(
                adapterView: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val project = adapterView.adapter.getItem(position) as Project
                projectItemSelected(project)
            }
        }
        binding.taskInput.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(adapterView: AdapterView<*>) {
                //taskItemSelected(taskEmpty)
            }

            override fun onItemSelected(
                adapterView: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val task = adapterView.adapter.getItem(position) as ProjectTask
                taskItemSelected(task)
            }
        }
        binding.locationInput.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(adapterView: AdapterView<*>) {
                //locationItemSelected(locationEmpty)
            }

            override fun onItemSelected(
                adapterView: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val task = adapterView.adapter.getItem(position) as LocationItem
                locationItemSelected(task)
            }
        }
        binding.startInput.setOnClickListener { pickStartTime() }
        binding.finishInput.setOnClickListener { pickFinishTime() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        recordsToSubmit.clear()
    }

    override fun populateForm(record: TimeRecord) {
        Timber.i("populateForm record=$record")

        if (record.id == TikalEntity.ID_NONE) {
            val args = arguments
            if (args != null) {
                if (args.containsKey(EXTRA_RECORD_ID)) {
                    val recordId = args.getLong(EXTRA_RECORD_ID)
                    record.id = recordId
                }
                if (args.containsKey(EXTRA_PROJECT_ID)) {
                    val projectId = args.getLong(EXTRA_PROJECT_ID)
                    val projects = timeViewModel.projectsData.value
                    setRecordProject(projects?.find { it.id == projectId }
                        ?: timeViewModel.projectEmpty)
                }
                if (args.containsKey(EXTRA_TASK_ID)) {
                    val taskId = args.getLong(EXTRA_TASK_ID)
                    val tasks = record.project.tasks
                    setRecordTask(tasks.find { it.id == taskId } ?: timeViewModel.taskEmpty)
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
        val taskItems = arrayOf(timeViewModel.taskEmpty)
        binding.taskInput.adapter =
            ArrayAdapter(context, android.R.layout.simple_list_item_1, taskItems)

        val projects = timeViewModel.projectsData.value
        bindProjects(context, record, projects)

        bindLocation(context, record)

        val startTime = record.startTime
        binding.startInput.text = if (startTime != TimeRecord.NEVER)
            DateUtils.formatDateTime(context, startTime, FORMAT_TIME_BUTTON)
        else
            ""
        binding.startInput.error = null
        startPickerDialog = null

        val finishTime = record.finishTime
        binding.finishInput.text = if (finishTime != TimeRecord.NEVER)
            DateUtils.formatDateTime(context, finishTime, FORMAT_TIME_BUTTON)
        else
            ""
        binding.finishInput.error = null
        finishPickerDialog = null

        binding.noteInput.setText(record.note)

        setErrorLabel(errorMessage)
    }

    private fun bindRecord(record: TimeRecord) {
        record.note = binding.noteInput.text.toString()
    }

    private fun bindProjects(context: Context, record: TimeRecord, projects: List<Project>?) {
        Timber.i("bindProjects record=$record projects=$projects")
        val projectItems = projects?.toTypedArray() ?: emptyArray()
        binding.projectInput.adapter =
            ArrayAdapter(context, android.R.layout.simple_list_item_1, projectItems)
        if (projectItems.isNotEmpty()) {
            binding.projectInput.setSelection(max(0, findProject(projectItems, record.project)))
            projectItemSelected(record.project)
        }
        binding.projectInput.requestFocus()
    }

    private fun bindLocation(context: Context, record: TimeRecord) {
        Timber.i("bindLocation record=$record")
        val locations = buildLocations(context)
        binding.locationInput.adapter =
            ArrayAdapter(context, android.R.layout.simple_list_item_1, locations)
        if (locations.isNotEmpty()) {
            val index = findLocation(locations, record.location)
            binding.locationInput.setSelection(max(0, index))
            val selectedItem = if (index >= 0) locations[index] else timeViewModel.locationEmpty
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
                override fun onDateTimeSet(
                    view: DateTimePicker,
                    year: Int,
                    month: Int,
                    dayOfMonth: Int,
                    hourOfDay: Int,
                    minute: Int
                ) {
                    setRecordStart(year, month, dayOfMonth, hourOfDay, minute)
                }
            }
            picker = DateTimePickerDialog(
                context,
                listener,
                year,
                month,
                dayOfMonth,
                hour,
                minute,
                DateFormat.is24HourFormat(context)
            )
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
                override fun onDateTimeSet(
                    view: DateTimePicker,
                    year: Int,
                    month: Int,
                    dayOfMonth: Int,
                    hourOfDay: Int,
                    minute: Int
                ) {
                    setRecordFinish(year, month, dayOfMonth, hourOfDay, minute)
                }
            }
            picker = DateTimePickerDialog(
                context,
                listener,
                year,
                month,
                dayOfMonth,
                hour,
                minute,
                DateFormat.is24HourFormat(context)
            )
            finishPickerDialog = picker
        } else {
            picker.updateDateTime(year, month, dayOfMonth, hour, minute)
        }
        picker.show()
    }

    private fun getCalendar(cal: Calendar?): Calendar {
        if (cal == null) {
            val calDate = Calendar.getInstance()
            calDate.year = date.year
            calDate.month = date.month
            calDate.dayOfMonth = date.dayOfMonth
            return calDate
        }
        return cal
    }

    private fun validateForm(record: TimeRecord): Boolean {
        val projectInputView = binding.projectInput.selectedView as TextView
        val taskInputView = binding.taskInput.selectedView as TextView
        val locationInputView = binding.locationInput.selectedView as TextView

        projectInputView.error = null
        projectInputView.isFocusableInTouchMode = false
        taskInputView.error = null
        taskInputView.isFocusableInTouchMode = false
        binding.startInput.error = null
        binding.startInput.isFocusableInTouchMode = false
        binding.finishInput.error = null
        binding.finishInput.isFocusableInTouchMode = false
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
        if (record.location.id == TikalEntity.ID_NONE) {
            locationInputView.error = getText(R.string.error_location_field_required)
            setErrorLabel(getText(R.string.error_location_field_required))
            locationInputView.isFocusableInTouchMode = true
            locationInputView.post { locationInputView.requestFocus() }
            return false
        }
        if (record.start == null) {
            binding.startInput.error = getText(R.string.error_start_field_required)
            setErrorLabel(getText(R.string.error_start_field_required))
            binding.startInput.isFocusableInTouchMode = true
            binding.startInput.requestFocus()
            return false
        }
        if (record.finish == null) {
            binding.finishInput.error = getText(R.string.error_finish_field_required)
            setErrorLabel(getText(R.string.error_finish_field_required))
            binding.finishInput.isFocusableInTouchMode = true
            binding.finishInput.requestFocus()
            return false
        }
        if (record.startTime + DateUtils.MINUTE_IN_MILLIS > record.finishTime) {
            binding.finishInput.error = getText(R.string.error_finish_time_before_start_time)
            setErrorLabel(getText(R.string.error_finish_time_before_start_time))
            binding.finishInput.isFocusableInTouchMode = true
            binding.finishInput.requestFocus()
            return false
        }

        return true
    }

    private fun filterTasks(project: Project) {
        Timber.i("filterTasks $project")
        val context = this.context ?: return
        val options = addEmpty(project.tasks)
        binding.taskInput.adapter =
            ArrayAdapter(context, android.R.layout.simple_list_item_1, options)
        binding.taskInput.setSelection(findTask(options, record.task))
    }

    private fun projectItemSelected(project: Project) {
        Timber.i("projectItemSelected $project")
        if (setRecordProject(project)) {
            markRecordModified()
        }
        filterTasks(project)
    }

    private fun taskItemSelected(task: ProjectTask) {
        Timber.i("taskItemSelected $task")
        if (setRecordTask(task)) {
            markRecordModified()
        }
    }

    private fun locationItemSelected(location: LocationItem) {
        Timber.d("locationItemSelected location=$location")
        if (setRecordLocation(location.location)) {
            markRecordModified()
        }
    }

    override fun run() {
        Timber.i("run first=$firstRun")
        if (maybeResubmit()) return

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

        delegate.dataSource.editPage(recordId, firstRun)
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
        timeViewModel.projectsData.value = addEmpties(page.projects)
        errorMessage = page.errorMessage ?: ""
        setRecordValue(page.record)
    }

    override fun onStart() {
        super.onStart()
        run()
    }

    override fun onLoginFailure(login: String, reason: String) {
        super.onLoginFailure(login, reason)
        activity?.finish()
    }

    private fun saveRecord(record: TimeRecord) {
        val recordDao = delegate.db.timeRecordDao()
        if (record.id == TikalEntity.ID_NONE) {
            recordDao.insert(record.toTimeRecordEntity())
        } else {
            recordDao.update(record.toTimeRecordEntity())
        }
    }

    override fun authenticate(submit: Boolean) {
        val navController = findNavController()
        Timber.i("authenticate submit=$submit currentDestination=${navController.currentDestination?.label}")
        if (!isNavDestination(R.id.loginFragment)) {
            Bundle().apply {
                putBoolean(LoginFragment.EXTRA_SUBMIT, submit)
                navController.navigate(R.id.action_timeEdit_to_login, this)
            }
        }
    }

    private fun submit(): Boolean {
        val record = this.record
        Timber.i("submit $record")

        bindRecord(record)
        if (!validateForm(record)) {
            return false
        }

        val records = recordsToSubmit
        records.clear()
        if (record.id == TikalEntity.ID_NONE) {
            val splits = record.split()
            records.addAll(splits)
        } else {
            records.add(record)
        }
        submit(records)

        return true
    }

    private fun submit(records: List<TimeRecord>) {
        val size = records.size
        if (size == 0) return
        val lastIndex = size - 1
        submit(records[0], true, 0 == lastIndex)
        if (size > 1) {
            for (i in 1 until size) {
                submit(records[i], false, i == lastIndex)
            }
        }
    }

    private fun submit(record: TimeRecord, isFirst: Boolean = true, isLast: Boolean = true) {
        Timber.i("submit $record first=$isFirst last=$isLast")
        // Show a progress spinner, and kick off a background task to submit the form.
        if (isFirst) {
            showProgress(true)
            setErrorLabel("")
        }

        val submitter: Single<Response<String>> = if (record.id == TikalEntity.ID_NONE) {
            delegate.service.addTime(
                record.project.id,
                record.task.id,
                formatSystemDate(record.start),
                formatSystemTime(record.start),
                formatSystemTime(record.finish),
                record.note,
                record.location.id
            )
        } else {
            delegate.service.editTime(
                record.id,
                record.project.id,
                record.task.id,
                formatSystemDate(record.start),
                formatSystemTime(record.start),
                formatSystemTime(record.finish),
                record.note,
                record.location.id
            )
        }
        submitter
            .subscribeOn(Schedulers.io())
            .subscribe({ response ->
                if (record.id != TikalEntity.ID_NONE) {
                    saveRecord(record)
                }

                if (isLast) {
                    showProgressMain(false)
                }

                if (isValidResponse(response)) {
                    val html = response.body()!!
                    processSubmittedPage(record, isLast, html)
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

    private fun processSubmittedPage(record: TimeRecord, isLast: Boolean, html: String) {
        val errorMessage = getResponseError(html)
        Timber.i("processSubmittedPage last=$isLast err=[$errorMessage]")
        if (errorMessage.isNullOrEmpty()) {
            onRecordSubmitted(record, isLast, html)
        } else {
            onRecordError(record, errorMessage)
        }
    }

    private fun onRecordSubmitted(record: TimeRecord, isLast: Boolean, html: String) {
        recordsToSubmit.remove(record)
        timeViewModel.onRecordEditSubmitted(record, isLast, html)

        if (isLast) {
            val isStop = arguments?.getBoolean(EXTRA_STOP, false) ?: false
            if (isStop) {
                stopTimer()
            }
        }
    }

    private fun onRecordError(record: TimeRecord, errorMessage: String) {
        setErrorLabelMain(errorMessage)
        timeViewModel.onRecordEditFailure(record, errorMessage)
    }

    private fun deleteRecord() {
        deleteRecord(record)
    }

    private fun deleteRecord(record: TimeRecord) {
        Timber.i("deleteRecord $record")
        if (record.id == TikalEntity.ID_NONE) {
            record.status = TaskRecordStatus.DELETED
            timeViewModel.onRecordEditDeleted(record)
            return
        }

        // Show a progress spinner, and kick off a background task to fetch the page.
        showProgress(true)

        // Fetch from remote server.
        delegate.service.deleteTime(record.id)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                showProgress(false)
                if (isValidResponse(response)) {
                    val html = response.body()!!
                    processDeletePage(record, html)
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

    private fun processDeletePage(record: TimeRecord, html: String) {
        Timber.i("processDeletePage")
        val errorMessage = getResponseError(html)
        if (errorMessage.isNullOrEmpty()) {
            onRecordDeleted(record, html)
        } else {
            onRecordError(record, errorMessage)
        }
    }

    private fun onRecordDeleted(record: TimeRecord, html: String) {
        record.status = TaskRecordStatus.DELETED
        recordsToSubmit.remove(record)
        timeViewModel.onRecordEditDeleted(record, html)

        val isStop = arguments?.getBoolean(EXTRA_STOP, false) ?: false
        if (isStop) {
            stopTimer()
        }
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
        val recordParcel = savedInstanceState.getParcelable<TimeRecordEntity?>(STATE_RECORD)
        if (recordParcel != null) {
            val projects = timeViewModel.projectsData.value
            val record = recordParcel.toTimeRecord(projects)
            setRecordValue(record)
            bindForm(record)
        }
    }

    override fun markFavorite(record: TimeRecord) {
        super.markFavorite(record)
        timeViewModel.onRecordEditFavorited(record)
    }

    fun editRecord(record: TimeRecord, date: Calendar, isStop: Boolean = false) {
        Timber.i("editRecord record=$record")
        setRecordValue(record.copy())
        this.date = date
        var args = arguments
        if (args == null) {
            args = Bundle()
            arguments = args
        }
        args.apply {
            clear()
            putLong(EXTRA_DATE, date.timeInMillis)
            putLong(EXTRA_PROJECT_ID, record.project.id)
            putLong(EXTRA_TASK_ID, record.task.id)
            putLong(EXTRA_START_TIME, record.startTime)
            putLong(EXTRA_FINISH_TIME, record.finishTime)
            putLong(EXTRA_RECORD_ID, record.id)
            putLong(EXTRA_LOCATION, record.location.id)
            putBoolean(EXTRA_STOP, isStop)
        }
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
        binding.errorLabel.text = text
        binding.errorLabel.visibility = if (text.isBlank()) View.GONE else View.VISIBLE
    }

    private fun setErrorLabelMain(text: CharSequence) {
        runOnUiThread { setErrorLabel(text) }
    }

    override fun onProjectsUpdated(projects: List<Project>) {
        super.onProjectsUpdated(projects)
        bindProjects(requireContext(), record, projects)
    }

    override fun setRecordValue(record: TimeRecord) {
        if ((record.id != this.record.id) || (record > this.record)) {
            super.setRecordValue(record)
        }
    }

    override fun setRecordStart(time: Calendar): Boolean {
        if (super.setRecordStart(time)) {
            binding.startInput.text =
                DateUtils.formatDateTime(context, time.timeInMillis, FORMAT_TIME_BUTTON)
            binding.startInput.error = null
            return true
        }
        return false
    }

    override fun setRecordFinish(time: Calendar): Boolean {
        if (super.setRecordFinish(time)) {
            binding.finishInput.text =
                DateUtils.formatDateTime(context, time.timeInMillis, FORMAT_TIME_BUTTON)
            binding.finishInput.error = null
            return true
        }
        return false
    }

    private fun markRecordModified() {
        if (record.status == TaskRecordStatus.CURRENT) {
            record.status = TaskRecordStatus.MODIFIED
            record.version++
        }
    }

    /** Maybe we tried to submit the form and were asked to login first? */
    private fun maybeResubmit(): Boolean {
        val records = recordsToSubmit
        Timber.i("maybeResubmit records=$records")
        if (records.isNotEmpty()) {
            submit(records)
            return true
        }
        return false
    }

    private fun stopTimer() {
        preferences.stopRecord()
    }

    companion object {
        const val EXTRA_DATE = TimeFormFragment.EXTRA_DATE
        const val EXTRA_PROJECT_ID = TimeFormFragment.EXTRA_PROJECT_ID
        const val EXTRA_TASK_ID = TimeFormFragment.EXTRA_TASK_ID
        const val EXTRA_START_TIME = TimeFormFragment.EXTRA_START_TIME
        const val EXTRA_FINISH_TIME = TimeFormFragment.EXTRA_FINISH_TIME
        const val EXTRA_RECORD_ID = TimeFormFragment.EXTRA_RECORD_ID
        const val EXTRA_LOCATION = TimeFormFragment.EXTRA_LOCATION
        const val EXTRA_STOP = TimeFormFragment.EXTRA_STOP

        private const val STATE_DATE = "date"
    }
}
