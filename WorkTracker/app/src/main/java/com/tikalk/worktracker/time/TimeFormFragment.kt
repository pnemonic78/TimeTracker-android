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
import android.view.View
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.tikalk.util.add
import com.tikalk.worktracker.BuildConfig
import com.tikalk.worktracker.R
import com.tikalk.worktracker.model.Location
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.net.InternetFragment
import com.tikalk.worktracker.report.LocationItem
import com.tikalk.worktracker.report.toLocationItem
import java.util.Calendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

abstract class TimeFormFragment : InternetFragment(), Runnable {

    protected val recordFlow = MutableStateFlow(TimeRecord.EMPTY.copy())
    var record: TimeRecord
        get() = recordFlow.value
        set(value) {
            recordFlow.tryEmit(value)
        }
    override val viewModel by activityViewModels<TimeViewModel>()
    protected val projectsFlow = MutableStateFlow<List<Project>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectsFlow.tryEmit(getProjects())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            viewModel.projectsData.collect { projects ->
                onProjectsUpdated(projects)
            }
        }
    }

    abstract fun populateForm(record: TimeRecord)

    @MainThread
    abstract fun bindForm(record: TimeRecord)

    protected fun markFavorite() {
        markFavorite(record)
    }

    protected open fun markFavorite(record: TimeRecord) {
        Timber.i("markFavorite $record")
        preferences.setFavorite(record)
        Toast.makeText(
            requireContext(),
            getString(R.string.favorite_marked, record.project.name, record.task.name),
            Toast.LENGTH_LONG
        ).show()
    }

    @MainThread
    protected fun populateAndBind() {
        val record = this.record
        Timber.i("populateAndBind record=$record")
        populateForm(record)
        bindForm(record)
    }

    private var rid = 0

    protected open fun setRecordValue(record: TimeRecord) {
        Timber.d("setRecordValue record=$record")
        lifecycleScope.launch {
            delay(100)
            recordFlow.emit(record)
        }
    }

    protected open fun setRecordProject(project: Project): Boolean {
        Timber.d("setRecordProject project=$project")
        if (record.project != project) {
            record.project = project
            return true
        }
        return false
    }

    protected open fun setRecordTask(task: ProjectTask): Boolean {
        Timber.d("setRecordTask task=$task")
        if (record.task != task) {
            record.task = task
            return true
        }
        return false
    }

    protected open fun setRecordLocation(location: Location): Boolean {
        Timber.d("setRecordLocation location=$location")
        if (record.location != location) {
            record.location = location
            return true
        }
        return false
    }

    protected open fun setRecordStart(time: Calendar): Boolean {
        Timber.d("setRecordStart time=$time")
        if (record.start != time) {
            record.date = time
            record.start = time
            return true
        }
        return false
    }

    protected fun setRecordStart(
        year: Int,
        month: Int,
        dayOfMonth: Int,
        hourOfDay: Int,
        minute: Int
    ): Boolean {
        Timber.d("setRecordStart date=$year-${month + 1}-$dayOfMonth $hourOfDay:$minute")
        val time = Calendar.getInstance()
        time.year = year
        time.month = month
        time.dayOfMonth = dayOfMonth
        time.hourOfDay = hourOfDay
        time.minute = minute
        return setRecordStart(time)
    }

    protected open fun setRecordFinish(time: Calendar): Boolean {
        Timber.d("setRecordFinish time=$time")
        if (record.finish != time) {
            record.finish = time
            return true
        }
        return false
    }

    protected fun setRecordFinish(
        year: Int,
        month: Int,
        dayOfMonth: Int,
        hourOfDay: Int,
        minute: Int
    ): Boolean {
        Timber.d("setRecordFinish date=$year-${month + 1}-$dayOfMonth $hourOfDay:$minute")
        val time = Calendar.getInstance()
        time.year = year
        time.month = month
        time.dayOfMonth = dayOfMonth
        time.hourOfDay = hourOfDay
        time.minute = minute
        return setRecordFinish(time)
    }

    protected open fun setRecordDuration(date: Calendar): Boolean {
        Timber.d("setRecordDuration date=$date")
        record.setDurationDateTime(date.timeInMillis)
        return true
    }

    protected fun setRecordDuration(
        year: Int,
        month: Int,
        dayOfMonth: Int,
        hourOfDay: Int,
        minute: Int
    ): Boolean {
        Timber.d("setRecordDuration date=$year-${month + 1}-$dayOfMonth $hourOfDay:$minute")
        val date = Calendar.getInstance()
        date.year = year
        date.month = month
        date.dayOfMonth = dayOfMonth
        date.hourOfDay = hourOfDay
        date.minute = minute
        return setRecordDuration(date)
    }

    protected open fun onProjectsUpdated(projects: List<Project>) {
        projectsFlow.tryEmit(projects)
        val record = this.record
        populateForm(record)
        bindForm(record)
    }

    protected fun applyFavorite() {
        val projectFavorite = preferences.getFavoriteProject()
        if (projectFavorite != TikalEntity.ID_NONE) {
            val projects = viewModel.projectsData.value
            val project = projects.find { it.id == projectFavorite } ?: record.project
            setRecordProject(project)

            val tasks = project.tasks
            val taskFavorite = preferences.getFavoriteTask()
            if (taskFavorite != TikalEntity.ID_NONE) {
                val task = tasks.find { it.id == taskFavorite } ?: record.task
                setRecordTask(task)
            }

            val locationFavorite = preferences.getFavoriteLocation()
            val location = Location.valueOf(locationFavorite)
            setRecordLocation(location)
        }
    }

    protected open fun getEmptyProjectName() =
        requireContext().getString(R.string.project_name_select)

    protected open fun getEmptyTaskName() = requireContext().getString(R.string.task_name_select)

    protected fun addEmptyProject(projects: List<Project>?): List<Project> {
        val projectEmptyFind = projects?.find { it.isEmpty() }
        val projectEmpty = projectEmptyFind ?: viewModel.projectEmpty
        projectEmpty.name = getEmptyProjectName()
        val projectsWithEmpty = if (projects != null) {
            if (projectEmptyFind != null) {
                projects.sortedBy { it.name }
            } else {
                projects.sortedBy { it.name }.add(0, projectEmpty)
            }
        } else {
            listOf(projectEmpty)
        }
        viewModel.projectEmpty = projectEmpty

        return projectsWithEmpty
    }

    protected fun addEmptyTask(tasks: List<ProjectTask>): List<ProjectTask> {
        val taskEmptyFind = tasks.find { it.isEmpty() }
        return if (taskEmptyFind != null) {
            taskEmptyFind.name = getEmptyTaskName()
            tasks.sortedBy { it.name }
        } else {
            val taskEmpty = getEmptyTask()
            tasks.sortedBy { it.name }.add(0, taskEmpty)
        }
    }

    protected fun getProjects(): List<Project> {
        return addEmptyProject(viewModel.projectsData.value)
    }

    protected fun getEmptyTask(): ProjectTask {
        val taskEmpty = viewModel.taskEmpty
        taskEmpty.name = getEmptyTaskName()
        return taskEmpty
    }

    protected open fun buildLocations(context: Context): List<LocationItem> {
        val items = ArrayList<LocationItem>()
        val values = Location.values
        for (value in values) {
            items.add(value.toLocationItem(context))
        }

        val select =
            LocationItem(items[0].location, context.getString(R.string.location_label_select))
        items[0] = select
        viewModel.locationEmpty = select

        return items
    }

    companion object {
        const val STATE_RECORD = "record"

        const val EXTRA_DATE = BuildConfig.APPLICATION_ID + ".form.DATE"
        const val EXTRA_RECORD_ID = BuildConfig.APPLICATION_ID + ".form.RECORD_ID"

        const val EXTRA_PROJECT_ID = BuildConfig.APPLICATION_ID + ".form.PROJECT_ID"
        const val EXTRA_TASK_ID = BuildConfig.APPLICATION_ID + ".form.TASK_ID"
        const val EXTRA_START_TIME = BuildConfig.APPLICATION_ID + ".form.START_TIME"
        const val EXTRA_FINISH_TIME = BuildConfig.APPLICATION_ID + ".form.FINISH_TIME"
        const val EXTRA_LOCATION = BuildConfig.APPLICATION_ID + ".form.LOCATION"
        const val EXTRA_STOP = BuildConfig.APPLICATION_ID + ".form.STOP"
    }
}