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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.tikalk.app.DateTimePickerDialog
import com.tikalk.app.DateTimePickerDialog.OnDateTimeSetListener
import com.tikalk.app.findParentFragment
import com.tikalk.app.isNavDestination
import com.tikalk.app.runOnUiThread
import com.tikalk.util.getParcelableCompat
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
import com.tikalk.worktracker.model.time.TimeRecord.Companion.NEVER
import com.tikalk.worktracker.model.time.split
import com.tikalk.worktracker.net.InternetFragment
import com.tikalk.worktracker.report.LocationItem
import com.tikalk.worktracker.report.findLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
import java.util.Formatter
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max

class TimeEditFragment : TimeFormFragment() {

    private var _binding: TimeFormBinding? = null
    private val binding get() = _binding!!

    private var startPickerDialog: DateTimePickerDialog? = null
    private var finishPickerDialog: DateTimePickerDialog? = null
    private var durationPickerDialog: TimePickerDialog? = null
    private var errorMessage: String = ""
    private val recordsToSubmit = CopyOnWriteArrayList<TimeRecord>()
    private val timeBuffer = StringBuilder(20)
    private val timeFormatter: Formatter = Formatter(timeBuffer, Locale.getDefault())

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
        binding.durationInput.setOnClickListener { pickDuration() }
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
                    val projects = viewModel.projectsData.value
                    setRecordProject(projects.find { it.id == projectId }
                        ?: viewModel.projectEmpty)
                }
                if (args.containsKey(EXTRA_TASK_ID)) {
                    val taskId = args.getLong(EXTRA_TASK_ID)
                    val tasks = record.project.tasks
                    setRecordTask(tasks.find { it.id == taskId } ?: viewModel.taskEmpty)
                }
                if (args.containsKey(EXTRA_DATE)) {
                    val dateTime = args.getLong(EXTRA_DATE)
                    record.date = Calendar.getInstance().apply { timeInMillis = dateTime }
                }
                if (args.containsKey(EXTRA_START_TIME)) {
                    val startTime = args.getLong(EXTRA_START_TIME)
                    if (startTime != NEVER) {
                        record.startTime = startTime
                    } else {
                        record.start = null
                    }
                }
                if (args.containsKey(EXTRA_FINISH_TIME)) {
                    val finishTime = args.getLong(EXTRA_FINISH_TIME)
                    if (finishTime != NEVER) {
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
        val taskItems = arrayOf(viewModel.taskEmpty)
        binding.taskInput.adapter =
            ArrayAdapter(context, android.R.layout.simple_list_item_1, taskItems)

        val projects = viewModel.projectsData.value
        bindProjects(context, record, projects)

        bindLocation(context, record)

        bindStartTime(context, record.start)
        startPickerDialog = null

        bindFinishTime(context, record.finish)
        finishPickerDialog = null

        bindDuration(context, record.duration)
        durationPickerDialog = null

        binding.noteInput.setText(record.note)

        setErrorLabel(errorMessage)
    }

    private fun bindRecord(record: TimeRecord) {
        record.note = binding.noteInput.text.toString()
    }

    private fun bindProjects(context: Context, record: TimeRecord, projects: List<Project>?) {
        Timber.i("bindProjects record=$record projects=$projects")
        val options = addEmptyProject(projects).toTypedArray()
        binding.projectInput.adapter =
            ArrayAdapter(context, android.R.layout.simple_list_item_1, options)
        if (options.isNotEmpty()) {
            binding.projectInput.setSelection(max(0, findProject(options, record.project)))
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
            val selectedItem = if (index >= 0) locations[index] else viewModel.locationEmpty
            locationItemSelected(selectedItem)
        }
    }

    private fun pickStartTime() {
        val cal = getCalendar(record.start)
        // Server granularity is seconds.
        cal.second = 0
        cal.millis = 0
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
        // Server granularity is seconds.
        cal.second = 0
        cal.millis = 0
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
        return cal ?: Calendar.getInstance().apply { timeInMillis = record.date.timeInMillis }
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
        binding.durationInput.error = null
        binding.durationInput.isFocusableInTouchMode = false
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
        if (record.duration < DateUtils.MINUTE_IN_MILLIS) {
            if (record.startTime == NEVER) {
                binding.startInput.error = getText(R.string.error_start_field_required)
                setErrorLabel(getText(R.string.error_start_field_required))
                binding.startInput.isFocusableInTouchMode = true
                binding.startInput.requestFocus()
                return false
            }
            if (record.finishTime == NEVER) {
                binding.finishInput.error = getText(R.string.error_finish_field_required)
                setErrorLabel(getText(R.string.error_finish_field_required))
                binding.finishInput.isFocusableInTouchMode = true
                binding.finishInput.requestFocus()
                return false
            }

            binding.durationInput.error = getText(R.string.error_finish_time_before_start_time)
            setErrorLabel(getText(R.string.error_finish_time_before_start_time))
            binding.durationInput.isFocusableInTouchMode = true
            binding.durationInput.requestFocus()
            return false
        }

        return true
    }

    private fun filterTasks(project: Project) {
        Timber.i("filterTasks $project")
        val context = this.context ?: return
        val options = addEmptyTask(project.tasks)
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
        record.date.timeInMillis = args.getLong(EXTRA_DATE, record.date.timeInMillis)

        val recordId = args.getLong(EXTRA_RECORD_ID, record.id)

        showProgress(true)
        lifecycleScope.launch {
            try {
                dataSource.editPage(recordId, firstRun)
                    .flowOn(Dispatchers.IO)
                    .collect { page ->
                        processPage(page)
                        populateAndBind()
                        showProgress(false)
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error loading page: ${e.message}")
                showProgress(false)
                handleError(e)
            }
        }
    }

    private fun processPage(page: TimeEditPage) {
        viewModel.projectsData.value = page.projects
        errorMessage = page.errorMessage ?: ""
        setRecordValue(page.record)
    }

    override fun onLoginFailure(login: String, reason: String) {
        super.onLoginFailure(login, reason)
        activity?.finish()
    }

    private suspend fun saveRecord(record: TimeRecord) {
        val recordDao = db.timeRecordDao()
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

        val dateValue = formatSystemDate(record.date)!!

        var startValue: String? = null
        var finishValue: String? = null
        var durationValue: String? = null
        if ((record.start != null) && (record.finish != null)) {
            startValue = formatSystemTime(record.start)
            finishValue = formatSystemTime(record.finish)
        } else {
            durationValue = formatDuration(record.duration)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = if (record.id == TikalEntity.ID_NONE) {
                    service.addTime(
                        projectId = record.project.id,
                        taskId = record.task.id,
                        date = dateValue,
                        start = startValue,
                        finish = finishValue,
                        duration = durationValue,
                        note = record.note,
                        locationId = record.location.id
                    )
                } else {
                    service.editTime(
                        id = record.id,
                        projectId = record.project.id,
                        taskId = record.task.id,
                        date = dateValue,
                        start = startValue,
                        finish = finishValue,
                        duration = durationValue,
                        note = record.note,
                        locationId = record.location.id
                    )
                }

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
            } catch (e: Exception) {
                Timber.e(e, "Error saving record: ${e.message}")
                showProgressMain(false)
                handleErrorMain(e)
            }
        }
    }

    private suspend fun processSubmittedPage(record: TimeRecord, isLast: Boolean, html: String) {
        val errorMessage = getResponseError(html)
        Timber.i("processSubmittedPage last=$isLast err=[$errorMessage]")
        if (errorMessage.isNullOrEmpty()) {
            onRecordSubmitted(record, isLast, html)
        } else {
            onRecordError(record, errorMessage)
        }
    }

    private suspend fun onRecordSubmitted(record: TimeRecord, isLast: Boolean, html: String) {
        recordsToSubmit.remove(record)
        viewModel.onRecordEditSubmitted(record, isLast, html)

        if (isLast) {
            val isStop = arguments?.getBoolean(EXTRA_STOP, false) ?: false
            if (isStop) {
                stopTimer()
            }
        }
    }

    private suspend fun onRecordError(record: TimeRecord, errorMessage: String) {
        setErrorLabelMain(errorMessage)
        viewModel.onRecordEditFailure(record, errorMessage)
    }

    private fun deleteRecord() {
        lifecycleScope.launch { deleteRecord(record) }
    }

    private suspend fun deleteRecord(record: TimeRecord) {
        Timber.i("deleteRecord $record")
        if (record.id == TikalEntity.ID_NONE) {
            record.status = TaskRecordStatus.DELETED
            viewModel.onRecordEditDeleted(record)
            return
        }

        // Show a progress spinner, and kick off a background task to fetch the page.
        showProgress(true)

        // Fetch from remote server.
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = service.deleteTime(record.id)
                showProgressMain(false)
                if (isValidResponse(response)) {
                    val html = response.body()!!
                    processDeletePage(record, html)
                } else {
                    authenticateMain()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting record: ${e.message}")
                showProgressMain(false)
                handleErrorMain(e)
            }
        }
    }

    private suspend fun processDeletePage(record: TimeRecord, html: String) {
        Timber.i("processDeletePage")
        val errorMessage = getResponseError(html)
        if (errorMessage.isNullOrEmpty()) {
            onRecordDeleted(record, html)
        } else {
            onRecordError(record, errorMessage)
        }
    }

    private suspend fun onRecordDeleted(record: TimeRecord, html: String) {
        record.status = TaskRecordStatus.DELETED
        recordsToSubmit.remove(record)
        viewModel.onRecordEditDeleted(record, html)

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
        outState.putParcelable(STATE_RECORD, record.toTimeRecordEntity())
    }

    @Suppress("DEPRECATION")
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val recordParcel: TimeRecordEntity? =
            savedInstanceState.getParcelableCompat<TimeRecordEntity>(STATE_RECORD)
        if (recordParcel != null) {
            val projects = viewModel.projectsData.value
            val record = recordParcel.toTimeRecord(projects)
            setRecordValue(record)
            bindForm(record)
        }
    }

    override fun markFavorite(record: TimeRecord) {
        super.markFavorite(record)
        lifecycleScope.launch { viewModel.onRecordEditFavorited(record) }
    }

    fun editRecord(record: TimeRecord, isStop: Boolean = false) {
        Timber.i("editRecord record=$record")
        setRecordValue(record.copy())

        var args = arguments
        if (args == null) {
            args = Bundle()
            arguments = args
        }
        args.apply {
            clear()
            putLong(EXTRA_DATE, record.date.timeInMillis)
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

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        if (view?.visibility == View.VISIBLE) {
            menuInflater.inflate(R.menu.time_edit, menu)
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (view?.visibility != View.VISIBLE) {
            return false
        }
        when (menuItem.itemId) {
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
        return super.onMenuItemSelected(menuItem)
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
            val context = requireContext()
            bindStartTime(context, time)
            bindDuration(context, record.duration)
            return true
        }
        return false
    }

    private fun bindStartTime(context: Context, time: Calendar?) {
        val timeMillis = time?.timeInMillis ?: NEVER
        binding.startInput.text = if (timeMillis != NEVER)
            DateUtils.formatDateTime(context, timeMillis, FORMAT_TIME_BUTTON)
        else
            ""
        binding.startInput.error = null
    }

    override fun setRecordFinish(time: Calendar): Boolean {
        if (super.setRecordFinish(time)) {
            val context = requireContext()
            bindFinishTime(context, time)
            bindDuration(context, record.duration)
            return true
        }
        return false
    }

    private fun bindFinishTime(context: Context, time: Calendar?) {
        val timeMillis = time?.timeInMillis ?: NEVER
        binding.finishInput.text = if (timeMillis != NEVER)
            DateUtils.formatDateTime(context, timeMillis, FORMAT_TIME_BUTTON)
        else
            ""
        binding.finishInput.error = null
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

    private fun bindDuration(context: Context, duration: Long) {
        binding.durationInput.text = if (duration > 0L) {
            timeBuffer.clear()
            formatElapsedTime(context, timeFormatter, duration).toString()
        } else {
            ""
        }
        binding.durationInput.error = null
    }

    private fun pickDuration() {
        val elapsedMs = record.duration
        val hours = (elapsedMs / DateUtils.HOUR_IN_MILLIS).toInt()
        val minutes = ((elapsedMs % DateUtils.HOUR_IN_MILLIS) / DateUtils.MINUTE_IN_MILLIS).toInt()
        var picker = durationPickerDialog
        if (picker == null) {
            val context = requireContext()
            val listener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                setRecordDuration(
                    hourOfDay,
                    minute
                )
            }
            picker = TimePickerDialog(
                context,
                listener,
                hours,
                minutes,
                true
            )
            durationPickerDialog = picker
        } else {
            picker.updateTime(hours, minutes)
        }
        picker.show()
    }

    override fun setRecordDuration(time: Long): Boolean {
        if (super.setRecordDuration(time)) {
            val context = requireContext()
            bindStartTime(context, record.start)
            bindFinishTime(context, record.finish)
            bindDuration(context, record.duration)
            return true
        }
        return false
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
    }
}
