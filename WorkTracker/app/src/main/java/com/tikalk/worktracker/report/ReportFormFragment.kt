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

package com.tikalk.worktracker.report

import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.tikalk.app.isNavDestination
import com.tikalk.util.getParcelableCompat
import com.tikalk.worktracker.BuildConfig
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.LoginFragment
import com.tikalk.worktracker.databinding.FragmentReportFormBinding
import com.tikalk.worktracker.model.Location
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.ReportTimePeriod
import com.tikalk.worktracker.model.findProject
import com.tikalk.worktracker.model.findTask
import com.tikalk.worktracker.model.time.ReportFilter
import com.tikalk.worktracker.model.time.ReportFormPage
import com.tikalk.worktracker.model.time.TaskRecordStatus
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.time.FORMAT_DATE_BUTTON
import com.tikalk.worktracker.time.TimeFormFragment
import com.tikalk.worktracker.time.dayOfMonth
import com.tikalk.worktracker.time.millis
import com.tikalk.worktracker.time.month
import com.tikalk.worktracker.time.year
import java.util.Calendar
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import timber.log.Timber

class ReportFormFragment : TimeFormFragment() {

    private var _binding: FragmentReportFormBinding? = null
    private val binding get() = _binding!!

    private val date: Calendar = Calendar.getInstance()
    private val filterData = MutableStateFlow(ReportFilter())
    private var startPickerDialog: DatePickerDialog? = null
    private var finishPickerDialog: DatePickerDialog? = null
    private var errorMessage: String = ""
    private val periods = ReportTimePeriod.values()

    init {
        record = ReportFilter()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bindingForm = binding.form
        bindingForm.projectInput.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(adapterView: AdapterView<*>) {
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
        bindingForm.taskInput.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(adapterView: AdapterView<*>) {
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
        bindingForm.locationInput.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(adapterView: AdapterView<*>) {
                    //locationItemSelected(locationEmpty)
                }

                override fun onItemSelected(
                    adapterView: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val location = adapterView.adapter.getItem(position) as LocationItem
                    locationItemSelected(location)
                }
            }
        bindingForm.periodInput.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(adapterView: AdapterView<*>) {
                    //periodItemSelected(ReportTimePeriod.CUSTOM)
                }

                override fun onItemSelected(
                    adapterView: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val period = periods[position]
                    periodItemSelected(period)
                }
            }
        bindingForm.startInput.setOnClickListener { pickStartDate() }
        bindingForm.finishInput.setOnClickListener { pickFinishDate() }

        bindingForm.showProjectField.setOnCheckedChangeListener { _, isChecked ->
            (record as ReportFilter).isProjectFieldVisible = isChecked
        }
        bindingForm.showTaskField.setOnCheckedChangeListener { _, isChecked ->
            (record as ReportFilter).isTaskFieldVisible = isChecked
        }
        bindingForm.showStartField.setOnCheckedChangeListener { _, isChecked ->
            (record as ReportFilter).isStartFieldVisible = isChecked
        }
        bindingForm.showFinishField.setOnCheckedChangeListener { _, isChecked ->
            (record as ReportFilter).isFinishFieldVisible = isChecked
        }
        bindingForm.showDurationField.setOnCheckedChangeListener { _, isChecked ->
            (record as ReportFilter).isDurationFieldVisible = isChecked
        }
        bindingForm.showNoteField.setOnCheckedChangeListener { _, isChecked ->
            (record as ReportFilter).isNoteFieldVisible = isChecked
        }
//        bindingForm.showLocationField.setOnCheckedChangeListener { _, isChecked ->
//            (record as ReportFilter).isLocationFieldVisible = isChecked
//        }

        bindingForm.actionGenerate.setOnClickListener { generateReport() }

        lifecycleScope.launch {
            filterData.collect { filter ->
                setRecordValue(filter)
                bindFilter(filter)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun populateForm(record: TimeRecord) = Unit

    override fun bindForm(record: TimeRecord) {
        Timber.i("bindForm record=$record")
        bindFilter(record as ReportFilter)
    }

    private fun projectItemSelected(project: Project) {
        Timber.d("projectItemSelected project=$project")
        setRecordProject(project)
        filterTasks(project)
    }

    private fun taskItemSelected(task: ProjectTask) {
        Timber.d("taskItemSelected task=$task")
        setRecordTask(task)
    }

    private fun filterTasks(project: Project) {
        Timber.d("filterTasks project=$project")
        val context = this.context ?: return
        val bindingForm = _binding?.form ?: return
        val options = addEmptyTask(project.tasks)
        bindingForm.taskInput.adapter =
            ArrayAdapter(context, android.R.layout.simple_list_item_1, options)

        val filter = filterData.value
        bindingForm.taskInput.setSelection(findTask(options, filter.task))
    }

    private fun periodItemSelected(period: ReportTimePeriod) {
        Timber.d("periodItemSelected period=$period")
        val filter = filterData.value
        filter.period = period
        filter.updateDates(date)

        val bindingForm = _binding?.form ?: return

        val startTime = filter.startTime
        bindingForm.startInput.text = if (startTime != TimeRecord.NEVER)
            DateUtils.formatDateTime(context, startTime, FORMAT_DATE_BUTTON)
        else
            ""
        bindingForm.startInput.error = null

        val finishTime = filter.finishTime
        bindingForm.finishInput.text = if (finishTime != TimeRecord.NEVER)
            DateUtils.formatDateTime(context, finishTime, FORMAT_DATE_BUTTON)
        else
            ""
        bindingForm.finishInput.error = null

        val custom = (period == ReportTimePeriod.CUSTOM)
        bindingForm.startIcon.isVisible = custom
        bindingForm.startInput.isVisible = custom
        bindingForm.finishIcon.isVisible = custom
        bindingForm.finishInput.isVisible = custom
    }

    override fun run() {
        Timber.i("run first=$firstRun")
        lifecycleScope.launch {
            try {
                dataSource.reportFormPage(firstRun)
                    .flowOn(Dispatchers.IO)
                    .collect { page ->
                        processPage(page)
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error loading page: ${e.message}")
                handleError(e)
            }
        }
    }

    private fun processPage(page: ReportFormPage) {
        viewModel.projectsData.value = page.projects
        errorMessage = page.errorMessage ?: ""

        val filter = filterData.value
        if (filter.status == TaskRecordStatus.DRAFT) {
            filterData.value = page.record
        }
    }

    override fun authenticate(submit: Boolean) {
        val navController = findNavController()
        Timber.i("authenticate submit=$submit currentDestination=${navController.currentDestination?.label}")
        if (!isNavDestination(R.id.loginFragment)) {
            Bundle().apply {
                putBoolean(LoginFragment.EXTRA_SUBMIT, submit)
                navController.navigate(R.id.action_reportForm_to_login, this)
            }
        }
    }

    private fun bindFilter(filter: ReportFilter) {
        Timber.i("bindFilter filter=$filter")
        val context = this.context ?: return
        val bindingForm = _binding?.form ?: return

        // Populate the tasks spinner before projects so that it can be filtered.
        val taskItems = arrayOf(viewModel.taskEmpty)
        bindingForm.taskInput.adapter =
            ArrayAdapter(context, android.R.layout.simple_list_item_1, taskItems)

        val projects = viewModel.projectsData.value
        bindProjects(context, filter, projects)

        val periodList = ArrayList<String>(periods.size)
        for (period in periods) {
            periodList.add(context.getString(period.labelId))
        }
        val periodItems = periodList.toTypedArray()
        bindingForm.periodInput.adapter =
            ArrayAdapter(context, android.R.layout.simple_list_item_1, periodItems)
        bindingForm.periodInput.setSelection(filter.period.ordinal)

        if (BuildConfig.LOCATION) {
            bindLocation(context, filter)
        }

        val startTime = filter.startTime
        bindingForm.startInput.text = if (startTime != TimeRecord.NEVER)
            DateUtils.formatDateTime(context, startTime, FORMAT_DATE_BUTTON)
        else
            ""
        bindingForm.startInput.error = null
        startPickerDialog = null

        val finishTime = filter.finishTime
        bindingForm.finishInput.text = if (finishTime != TimeRecord.NEVER)
            DateUtils.formatDateTime(context, finishTime, FORMAT_DATE_BUTTON)
        else
            ""
        bindingForm.finishInput.error = null
        finishPickerDialog = null

        bindingForm.showProjectField.isChecked = filter.isProjectFieldVisible
        bindingForm.showTaskField.isChecked = filter.isTaskFieldVisible
        bindingForm.showStartField.isChecked = filter.isStartFieldVisible
        bindingForm.showFinishField.isChecked = filter.isFinishFieldVisible
        bindingForm.showDurationField.isChecked = filter.isDurationFieldVisible
        bindingForm.showNoteField.isChecked = filter.isNoteFieldVisible
//        bindingForm.showLocationField.isChecked = filter.isLocationFieldVisible

        setErrorLabel(errorMessage)
    }

    private fun bindProjects(context: Context, filter: ReportFilter, projects: List<Project>?) {
        Timber.i("bindProjects filter=$filter projects=$projects")
        val options = addEmptyProject(projects).toTypedArray()
        val bindingForm = _binding?.form ?: return
        bindingForm.projectInput.adapter =
            ArrayAdapter(context, android.R.layout.simple_list_item_1, options)
        if (options.isNotEmpty()) {
            bindingForm.projectInput.setSelection(max(0, findProject(options, filter.project)))
            projectItemSelected(filter.project)
        }
        bindingForm.projectInput.requestFocus()
    }

    private fun bindLocation(context: Context, filter: ReportFilter) {
        Timber.i("bindLocation filter=$filter")
        val bindingForm = _binding?.form ?: return
        bindingForm.locationIcon.isVisible = true
        bindingForm.locationInput.isVisible = true
        val locations = buildLocations(context)
        bindingForm.locationInput.adapter =
            ArrayAdapter(context, android.R.layout.simple_list_item_1, locations)
        if (locations.isNotEmpty()) {
            bindingForm.locationInput.setSelection(max(0, findLocation(locations, filter.location)))
            locationItemSelected(filter.location)
        }
    }

    override fun buildLocations(context: Context): List<LocationItem> {
        val items = ArrayList(super.buildLocations(context))
        val all = LocationItem(items[0].location, context.getString(R.string.location_label_all))
        items[0] = all
        viewModel.locationEmpty = all
        return items
    }

    private fun pickStartDate() {
        val filter = filterData.value
        val cal = getCalendar(filter.start)
        val year = cal.year
        val month = cal.month
        val dayOfMonth = cal.dayOfMonth
        var picker = startPickerDialog
        if (picker == null) {
            val context = getContext() ?: return
            val bindingForm = _binding?.form ?: return
            val listener =
                DatePickerDialog.OnDateSetListener { _, pickedYear, pickedMonth, pickedDayOfMonth ->
                    cal.year = pickedYear
                    cal.month = pickedMonth
                    cal.dayOfMonth = pickedDayOfMonth
                    filter.start = cal
                    bindingForm.startInput.text =
                        DateUtils.formatDateTime(context, cal.timeInMillis, FORMAT_DATE_BUTTON)
                    bindingForm.startInput.error = null
                }
            picker = DatePickerDialog(context, listener, year, month, dayOfMonth)
            picker.setButton(
                DialogInterface.BUTTON_NEUTRAL,
                context.getText(R.string.today)
            ) { dialog: DialogInterface, which: Int ->
                if ((dialog == picker) and (which == DialogInterface.BUTTON_NEUTRAL)) {
                    val today = Calendar.getInstance()
                    listener.onDateSet(picker.datePicker, today.year, today.month, today.dayOfMonth)
                }
            }
            startPickerDialog = picker
        } else {
            picker.updateDate(year, month, dayOfMonth)
        }
        picker.show()
    }

    private fun pickFinishDate() {
        val filter = filterData.value
        val cal = getCalendar(filter.finish)
        val year = cal.year
        val month = cal.month
        val dayOfMonth = cal.dayOfMonth
        var picker = finishPickerDialog
        if (picker == null) {
            val context = getContext() ?: return
            val bindingForm = _binding?.form ?: return
            val listener =
                DatePickerDialog.OnDateSetListener { _, pickedYear, pickedMonth, pickedDayOfMonth ->
                    cal.year = pickedYear
                    cal.month = pickedMonth
                    cal.dayOfMonth = pickedDayOfMonth
                    filter.finish = cal
                    bindingForm.finishInput.text =
                        DateUtils.formatDateTime(context, cal.timeInMillis, FORMAT_DATE_BUTTON)
                    bindingForm.finishInput.error = null
                }
            picker = DatePickerDialog(context, listener, year, month, dayOfMonth)
            picker.setButton(
                DialogInterface.BUTTON_NEUTRAL,
                context.getText(R.string.today)
            ) { dialog: DialogInterface, which: Int ->
                if ((dialog == picker) and (which == DialogInterface.BUTTON_NEUTRAL)) {
                    val today = Calendar.getInstance()
                    listener.onDateSet(picker.datePicker, today.year, today.month, today.dayOfMonth)
                }
            }
            finishPickerDialog = picker
        } else {
            picker.updateDate(year, month, dayOfMonth)
        }
        picker.show()
    }

    private fun getCalendar(cal: Calendar?): Calendar {
        if (cal == null) {
            val calDate = Calendar.getInstance()
            calDate.timeInMillis = date.timeInMillis
            // Server granularity is seconds.
            calDate.millis = 0
            return calDate
        }
        return cal
    }

    private fun populateFilter(): ReportFilter {
        Timber.i("populateFilter")
        val filter = filterData.value
        filter.updateDates(date)
        return filter
    }

    private fun generateReport() {
        val navController = findNavController()
        Timber.i("generateReport currentDestination=${navController.currentDestination?.label}")

        if (!isNavDestination(R.id.reportFragment)) {
            val filter = populateFilter()
            val args = Bundle().apply {
                putParcelable(ReportFragment.EXTRA_FILTER, filter)
            }

            var reportFragmentController: NavController? = null
            val reportFragment =
                childFragmentManager.findFragmentById(R.id.nav_host_report) as NavHostFragment?
            if (reportFragment != null) {
                // Existing view means ready - avoid "NavController is not available before onCreate()"
                val reportFragmentView = reportFragment.view
                if ((reportFragmentView != null) && (reportFragmentView.parent != null)) {
                    reportFragmentController = reportFragment.navController
                }
            }

            if (reportFragmentController != null) {
                reportFragmentController.navigate(R.id.reportFragment, args)
            } else {
                navController.navigate(R.id.action_reportForm_to_reportList, args)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Timber.i("onSaveInstanceState")
        super.onSaveInstanceState(outState)
        outState.putParcelable(STATE_FILTER, filterData.value)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        Timber.i("onRestoreInstanceState")
        super.onRestoreInstanceState(savedInstanceState)
        val filter = savedInstanceState.getParcelableCompat<ReportFilter>(STATE_FILTER)
        if (filter != null) {
            filterData.value = filter
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
        menuInflater.inflate(R.menu.report_form, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.menu_generate -> {
                generateReport()
                return true
            }
        }
        return super.onMenuItemSelected(menuItem)
    }

    private fun setErrorLabel(text: CharSequence) {
        val bindingForm = _binding?.form ?: return
        bindingForm.errorLabel.text = text
        bindingForm.errorLabel.isVisible = text.isNotBlank()
    }

    override fun setRecordValue(record: TimeRecord) {
        // `record` must always point to `filter`.
        if (record is ReportFilter) {
            super.setRecordValue(record)
        }
    }

    override fun setRecordProject(project: Project): Boolean {
        if (super.setRecordProject(project)) {
            filterData.value.project = project
            return true
        }
        return false
    }

    override fun setRecordTask(task: ProjectTask): Boolean {
        if (super.setRecordTask(task)) {
            filterData.value.task = task
            return true
        }
        return false
    }

    override fun onProjectsUpdated(projects: List<Project>) {
        super.onProjectsUpdated(projects)
        val filter = filterData.value
        bindProjects(requireContext(), filter, projects)
    }

    override fun getEmptyProjectName() = requireContext().getString(R.string.project_name_all)

    override fun getEmptyTaskName() = requireContext().getString(R.string.task_name_all)

    private fun locationItemSelected(location: LocationItem) {
        locationItemSelected(location.location)
    }

    private fun locationItemSelected(location: Location) {
        Timber.d("remoteItemSelected location=$location")
        setRecordLocation(location)
    }

    override fun setRecordLocation(location: Location): Boolean {
        if (super.setRecordLocation(location)) {
            filterData.value.location = location
            return true
        }
        return false
    }

    companion object {
        private const val STATE_FILTER = BuildConfig.APPLICATION_ID + ".FILTER"
    }
}