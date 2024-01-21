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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.tikalk.app.isDestination
import com.tikalk.compose.TikalTheme
import com.tikalk.util.add
import com.tikalk.util.getParcelableCompat
import com.tikalk.worktracker.BuildConfig
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.LoginFragment
import com.tikalk.worktracker.databinding.FragmentReportFormBinding
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.ReportTimePeriod
import com.tikalk.worktracker.model.time.ReportFilter
import com.tikalk.worktracker.model.time.ReportFormPage
import com.tikalk.worktracker.model.time.TaskRecordStatus
import com.tikalk.worktracker.time.TimeFormError
import com.tikalk.worktracker.time.TimeFormFragment
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

class ReportFormFragment : TimeFormFragment<ReportFilter>() {

    private var _binding: FragmentReportFormBinding? = null
    private val binding get() = _binding!!

    private val date: Calendar = Calendar.getInstance()
    private var error: TimeFormError? = null
    private var projectAll: Project = Project.EMPTY.copy()
    private var taskAll: ProjectTask = ProjectTask.EMPTY.copy()

    override fun createEmptyRecord(): ReportFilter {
        return ReportFilter()
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

        binding.form.setContent {
            TikalTheme {
                ReportForm(
                    projectsFlow = addEmptyProject(viewModel.projectsFlow),
                    taskEmpty = getEmptyTask(),
                    filterFlow = recordFlow,
                    error = error,
                    onProjectSelected = ::onProjectItemSelected,
                    onTaskSelected = ::onTaskItemSelected,
                    onPeriodSelected = ::onPeriodItemSelected,
                    onStartDateSelected = { setRecordStart(it) },
                    onFinishDateSelected = { setRecordFinish(it) },
                    onProjectFieldVisible = { record.isProjectFieldVisible = it },
                    onTaskFieldVisible = { record.isTaskFieldVisible = it },
                    onStartFieldVisible = { record.isStartFieldVisible = it },
                    onFinishFieldVisible = { record.isFinishFieldVisible = it },
                    onDurationFieldVisible = { record.isDurationFieldVisible = it },
                    onNoteFieldVisible = { record.isNoteFieldVisible = it },
                    onClick = ::generateReport
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun populateForm(record: ReportFilter) = Unit

    override fun bindForm(record: ReportFilter) = Unit

    private fun onProjectItemSelected(project: Project) {
        Timber.d("onProjectItemSelected project=$project")
        setRecordProject(project)
    }

    private fun onTaskItemSelected(task: ProjectTask) {
        Timber.d("onTaskItemSelected task=$task")
        setRecordTask(task)
    }

    private fun onPeriodItemSelected(period: ReportTimePeriod) {
        Timber.d("onPeriodItemSelected period=$period")
        val filter = record
        filter.period = period
        filter.updateDates(date)
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
        viewModel.projects = page.projects
        error = if (page.errorMessage.isNullOrEmpty()) {
            null
        } else {
            TimeFormError.General(page.errorMessage)
        }

        val filter = record
        if (filter.status == TaskRecordStatus.DRAFT) {
            setRecordValue(page.record)
        }
    }

    override fun authenticate(submit: Boolean) {
        val navController = findNavController()
        Timber.i("authenticate submit=$submit currentDestination=${navController.currentDestination?.label}")
        if (!navController.isDestination(R.id.loginFragment)) {
            Bundle().apply {
                putBoolean(LoginFragment.EXTRA_SUBMIT, submit)
                navController.navigate(R.id.action_reportForm_to_login, this)
            }
        }
    }

    private fun populateFilter(): ReportFilter {
        Timber.i("populateFilter")
        val filter = record
        filter.updateDates(date)
        return filter
    }

    private fun generateReport() {
        // TODO validate form, e.g. finish >= start

        val navController = findNavController()
        Timber.i("generateReport currentDestination=${navController.currentDestination?.label}")

        if (!navController.isDestination(R.id.reportFragment)) {
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
        outState.putParcelable(STATE_FILTER, record)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        Timber.i("onRestoreInstanceState")
        super.onRestoreInstanceState(savedInstanceState)
        val filter = savedInstanceState.getParcelableCompat<ReportFilter>(STATE_FILTER)
        if (filter != null) {
            setRecordValue(filter)
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

    override fun getEmptyProjectName() = requireContext().getString(R.string.project_name_all)

    override fun getEmptyTaskName() = requireContext().getString(R.string.task_name_all)

    override fun getEmptyTask(): ProjectTask {
        return taskAll.apply {
            name = getEmptyTaskName()
        }
    }

    override fun addEmptyProject(projectsFlow: Flow<List<Project>>): Flow<List<Project>> {
        return projectsFlow.map { projects ->
            val projectEmptyFind = projects.find { it.isEmpty() }
            val projectEmpty = projectEmptyFind ?: projectAll
            projectEmpty.name = getEmptyProjectName()
            val projectsWithEmpty = projects.filter { !it.isEmpty() }
                .sortedBy { it.name }
                .add(0, projectEmpty)
            projectAll = projectEmpty
            projectsWithEmpty
        }
    }

    override fun addEmptyTask(tasks: List<ProjectTask>): List<ProjectTask> {
        val taskEmptyFind = tasks.find { it.isEmpty() }
        val taskEmpty = taskEmptyFind ?: taskAll
        taskEmpty.name = getEmptyTaskName()
        val tasksWithEmpty = tasks.filter { !it.isEmpty() }
            .sortedBy { it.name }
            .add(0, taskEmpty)
        taskAll = taskEmpty
        return tasksWithEmpty
    }

    companion object {
        private const val STATE_FILTER = BuildConfig.APPLICATION_ID + ".FILTER"
    }
}