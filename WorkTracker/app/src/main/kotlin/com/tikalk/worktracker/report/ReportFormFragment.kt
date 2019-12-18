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
import android.os.Bundle
import android.text.format.DateUtils
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.tikalk.app.isNavDestination
import com.tikalk.html.isChecked
import com.tikalk.html.selectByName
import com.tikalk.html.value
import com.tikalk.worktracker.BuildConfig
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.LoginFragment
import com.tikalk.worktracker.model.*
import com.tikalk.worktracker.model.time.ReportFilter
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.time.*
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.report_form.*
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.FormElement
import timber.log.Timber
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList
import kotlin.math.max

class ReportFormFragment : TimeFormFragment() {

    private val date: Calendar = Calendar.getInstance()
    private val filterData = MutableLiveData<ReportFilter>()
    private var startPickerDialog: DatePickerDialog? = null
    private var finishPickerDialog: DatePickerDialog? = null
    private var errorMessage: String = ""
    private val periods = ReportTimePeriod.values()
    private var firstRun = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        firstRun = (savedInstanceState == null)
        filterData.value = ReportFilter()
        filterData.observe(this, Observer<ReportFilter> { filter ->
            bindFilter(filter)
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_report_form, container, false)
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
        periodInput.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(adapterView: AdapterView<*>) {
                periodItemSelected(ReportTimePeriod.CUSTOM)
            }

            override fun onItemSelected(adapterView: AdapterView<*>, view: View?, position: Int, id: Long) {
                val period = periods[position]
                periodItemSelected(period)
            }
        }
        startInput.setOnClickListener { pickStartDate() }
        finishInput.setOnClickListener { pickFinishDate() }

        actionGenerate.setOnClickListener { generateReport() }
    }

    override fun populateForm(record: TimeRecord) {
    }

    override fun bindForm(record: TimeRecord) {
        Timber.v("bindForm record=$record")
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
        if (!isVisible) return
        val context: Context = requireContext()
        val filtered = project.tasks
        val options = ArrayList<ProjectTask>(filtered.size + 1)
        options.add(taskEmpty)
        options.addAll(filtered)
        taskInput.adapter = ArrayAdapter<ProjectTask>(context, android.R.layout.simple_list_item_1, options)

        val filter = filterData.value
        if (filter != null) {
            taskInput.setSelection(findTask(options, filter.task))
        }
    }

    private fun periodItemSelected(period: ReportTimePeriod) {
        Timber.d("periodItemSelected period=$period")
        val filter = filterData.value ?: return
        filter.period = period
        filter.updateDates(date)

        if (!isVisible) return
        val custom = (period == ReportTimePeriod.CUSTOM)
        val visibility = if (custom) View.VISIBLE else View.GONE
        startIcon.visibility = visibility
        startInput.visibility = visibility
        finishIcon.visibility = visibility
        finishInput.visibility = visibility

        val startTime = filter.startTime
        startInput.text = if (startTime > 0L)
            DateUtils.formatDateTime(context, startTime, FORMAT_DATE_BUTTON)
        else
            ""
        startInput.error = null

        val finishTime = filter.finishTime
        finishInput.text = if (finishTime > 0L)
            DateUtils.formatDateTime(context, finishTime, FORMAT_DATE_BUTTON)
        else
            ""
        finishInput.error = null
    }

    fun run() {
        Timber.v("run")

        loadPage()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (firstRun) {
                    fetchPage()
                }
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

    override fun authenticate(submit: Boolean) {
        Timber.v("authenticate submit=$submit")
        if (!isNavDestination(R.id.loginFragment)) {
            val args = Bundle()
            requireFragmentManager().putFragment(args, LoginFragment.EXTRA_CALLER, this)
            args.putBoolean(LoginFragment.EXTRA_SUBMIT, submit)
            findNavController().navigate(R.id.action_reportForm_to_login, args)
        }
    }

    private fun loadPage(): Single<Unit> {
        return Single.fromCallable { loadForm() }
    }

    private fun fetchPage() {
        Timber.d("fetchPage")
        // Show a progress spinner, and kick off a background task to fetch the page.
        showProgress(true)

        // Fetch from remote server.
        service.fetchReports()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                if (isValidResponse(response)) {
                    val body = response.body()!!
                    populateForm(date, body)
                    showProgress(false)
                } else {
                    authenticate()
                }
            }, { err ->
                Timber.e(err, "Error fetching page: ${err.message}")
                handleError(err)
                showProgress(false)
            })
            .addTo(disposables)
    }

    override fun findForm(doc: Document): FormElement? {
        return doc.selectFirst("form[name='reportForm']") as FormElement?
    }

    override fun populateForm(date: Calendar, doc: Document, form: FormElement, inputProjects: Element, inputTasks: Element) {
        super.populateForm(date, doc, form, inputProjects, inputTasks)

        val inputPeriod = form.selectByName("period") ?: return
        val periodSelected = findSelectedPeriod(inputPeriod, periods)

        val inputStart = form.selectByName("start_date") ?: return
        val startValue = inputStart.value()

        val inputFinish = form.selectByName("end_date") ?: return
        val finishValue = inputFinish.value()

        val filter = filterData.value ?: ReportFilter()
        filter.period = periodSelected
        filter.start = parseSystemDate(startValue)
        filter.finish = parseSystemDate(finishValue)

        val inputShowProject = form.selectByName("chproject")
        filter.showProjectField = inputShowProject?.isChecked() ?: filter.showProjectField

        val inputShowTask = form.selectByName("chtask")
        filter.showTaskField = inputShowTask?.isChecked() ?: filter.showTaskField

        val inputShowStart = form.selectByName("chstart")
        filter.showStartField = inputShowStart?.isChecked() ?: filter.showStartField

        val inputShowFinish = form.selectByName("chfinish")
        filter.showFinishField = inputShowFinish?.isChecked() ?: filter.showFinishField

        val inputShowDuration = form.selectByName("chduration")
        filter.showDurationField = inputShowDuration?.isChecked() ?: filter.showDurationField

        val inputShowNote = form.selectByName("chnote")
        filter.showNoteField = inputShowNote?.isChecked() ?: filter.showNoteField

        filterData.value = filter
    }

    override fun findTaskIds(doc: Document): String? {
        val tokenStart = "// Populate obj_tasks with task ids for each relevant project."
        val tokenEnd = "// Prepare an array of task names."
        return findScript(doc, tokenStart, tokenEnd)
    }

    override fun populateTaskIds(doc: Document, projects: List<Project>) {
        Timber.v("populateTaskIds")
        val scriptText = findTaskIds(doc) ?: return

        if (scriptText.isNotEmpty()) {
            for (project in projects) {
                project.clearTasks()
            }

            val pattern = Pattern.compile("project_property = project_prefix [+] (\\d+);\\s+obj_tasks\\[project_property\\] = \"(.+)\";")
            val matcher = pattern.matcher(scriptText)
            while (matcher.find()) {
                val projectId = matcher.group(1)!!.toLong()
                val project = projects.find { it.id == projectId }

                val taskIds: List<Long> = matcher.group(2)!!
                    .split(",")
                    .map { it.toLong() }
                val tasks = this.tasks.filter { it.id in taskIds }

                project?.addTasks(tasks)
            }
        }
    }

    fun findSelectedPeriod(periodInput: Element, periods: Array<ReportTimePeriod>): ReportTimePeriod {
        for (option in periodInput.children()) {
            if (option.hasAttr("selected")) {
                val value = option.value()
                if (value.isNotEmpty()) {
                    return periods.find { value == it.value }!!
                }
                break
            }
        }
        return ReportTimePeriod.CUSTOM
    }

    private fun bindFilter(filter: ReportFilter) {
        Timber.v("bindFilter filter=$filter")
        val context: Context = requireContext()

        // Populate the tasks spinner before projects so that it can be filtered.
        val taskItems = arrayOf(taskEmpty)
        taskInput.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, taskItems)

        val projectItems = projects.toTypedArray()
        projectInput.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, projectItems)
        if (projectItems.isNotEmpty()) {
            projectInput.setSelection(max(0, findProject(projectItems, filter.project)))
            projectItemSelected(filter.project)
        }
        projectInput.requestFocus()

        val periodList = ArrayList<String>(periods.size)
        for (period in periods) {
            periodList.add(context.getString(period.labelId))
        }
        val periodItems = periodList.toTypedArray()
        periodInput.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, periodItems)
        periodInput.setSelection(filter.period.ordinal)

        val startTime = filter.startTime
        startInput.text = if (startTime > 0L)
            DateUtils.formatDateTime(context, startTime, FORMAT_DATE_BUTTON)
        else
            ""
        startInput.error = null
        startPickerDialog = null

        val finishTime = filter.finishTime
        finishInput.text = if (finishTime > 0L)
            DateUtils.formatDateTime(context, finishTime, FORMAT_DATE_BUTTON)
        else
            ""
        finishInput.error = null
        finishPickerDialog = null

        showProjectField.isChecked = filter.showProjectField
        showTaskField.isChecked = filter.showTaskField
        showStartField.isChecked = filter.showStartField
        showFinishField.isChecked = filter.showFinishField
        showDurationField.isChecked = filter.showDurationField
        showNoteField.isChecked = filter.showNoteField

        setErrorLabel(errorMessage)
    }

    private fun pickStartDate() {
        val filter = filterData.value ?: return
        val cal = getCalendar(filter.start)
        val year = cal.year
        val month = cal.month
        val dayOfMonth = cal.dayOfMonth
        var picker = startPickerDialog
        if (picker == null) {
            val context = requireContext()
            val listener = DatePickerDialog.OnDateSetListener { _, pickedYear, pickedMonth, pickedDayOfMonth ->
                cal.year = pickedYear
                cal.month = pickedMonth
                cal.dayOfMonth = pickedDayOfMonth
                filter.start = cal
                startInput.text = DateUtils.formatDateTime(context, cal.timeInMillis, FORMAT_DATE_BUTTON)
                startInput.error = null
            }
            picker = DatePickerDialog(context, listener, year, month, dayOfMonth)
            startPickerDialog = picker
        } else {
            picker.updateDate(year, month, dayOfMonth)
        }
        picker.show()
    }

    private fun pickFinishDate() {
        val filter = filterData.value ?: return
        val cal = getCalendar(filter.finish)
        val year = cal.year
        val month = cal.month
        val dayOfMonth = cal.dayOfMonth
        var picker = finishPickerDialog
        if (picker == null) {
            val context = requireContext()
            val listener = DatePickerDialog.OnDateSetListener { _, pickedYear, pickedMonth, pickedDayOfMonth ->
                cal.year = pickedYear
                cal.month = pickedMonth
                cal.dayOfMonth = pickedDayOfMonth
                filter.finish = cal
                finishInput.text = DateUtils.formatDateTime(context, cal.timeInMillis, FORMAT_DATE_BUTTON)
                finishInput.error = null
            }
            picker = DatePickerDialog(context, listener, year, month, dayOfMonth)
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
        Timber.v("populateFilter")
        val filter = filterData.value ?: ReportFilter()
        filter.showProjectField = showProjectField.isChecked
        filter.showTaskField = showTaskField.isChecked
        filter.showStartField = showStartField.isChecked
        filter.showFinishField = showFinishField.isChecked
        filter.showDurationField = showDurationField.isChecked
        filter.showNoteField = showNoteField.isChecked
        filter.updateDates(date)
        return filter
    }

    private fun generateReport() {
        Timber.v("generateReport")

        if (!isNavDestination(R.id.reportFragment)) {
            val filter = populateFilter()
            val args = Bundle()
            requireFragmentManager().putFragment(args, ReportFragment.EXTRA_CALLER, this)
            args.putParcelable(ReportFragment.EXTRA_FILTER, filter)

            var reportFragmentController: NavController? = null
            val reportFragment = childFragmentManager.findFragmentById(R.id.nav_host_report) as NavHostFragment?
            if (reportFragment != null) {
                // Existing view means ready - avoid "NavController is not available before onCreate()"
                if (reportFragment.view != null) {
                    reportFragmentController = reportFragment.navController
                }
            }

            if (reportFragmentController != null) {
                reportFragmentController.navigate(R.id.reportFragment, args)
            } else {
                findNavController().navigate(R.id.action_reportForm_to_reportList, args)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Timber.v("onSaveInstanceState")
        super.onSaveInstanceState(outState)
        outState.putParcelable(STATE_FILTER, filterData.value)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        Timber.v("onRestoreInstanceState")
        super.onRestoreInstanceState(savedInstanceState)
        this.firstRun = false
        filterData.value = savedInstanceState.getParcelable(STATE_FILTER) ?: ReportFilter()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.report_form, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_generate -> {
                generateReport()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setErrorLabel(text: CharSequence) {
        errorLabel.text = text
        errorLabel.visibility = if (text.isBlank()) View.GONE else View.VISIBLE
    }

    override fun setRecordValue(record: TimeRecord) {
        // `record` must always point to `filter`.
    }

    override fun setRecordProject(project: Project) {
        super.setRecordProject(project)
        filterData.value?.project = project
    }

    override fun setRecordTask(task: ProjectTask) {
        super.setRecordTask(task)
        filterData.value?.task = task
    }

    companion object {
        private const val STATE_FILTER = BuildConfig.APPLICATION_ID + ".FILTER"
    }
}