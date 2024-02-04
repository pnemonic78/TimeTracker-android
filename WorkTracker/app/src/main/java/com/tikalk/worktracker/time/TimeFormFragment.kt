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

import android.os.Bundle
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.tikalk.util.add
import com.tikalk.worktracker.BuildConfig
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.net.InternetFragment
import java.util.Calendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

abstract class TimeFormFragment<R : TimeRecord> : InternetFragment(), Runnable {

    override val viewModel by activityViewModels<TimeViewModel>()

    private val _recordFlow = MutableStateFlow(createEmptyRecord())
    protected val recordFlow: StateFlow<R> = _recordFlow
    var record: R
        get() = recordFlow.value
        set(value) {
            lifecycleScope.launch {
                delay(100)
                _recordFlow.emit(value)
            }
        }

    @Suppress("UNCHECKED_CAST")
    protected open fun createEmptyRecord(): R {
        return TimeRecord.EMPTY.copy() as R
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.projects = getProjects()
    }

    abstract fun populateForm(record: R)

    @MainThread
    abstract fun bindForm(record: R)

    protected fun markFavorite() {
        markFavorite(record)
    }

    protected open fun markFavorite(record: R) {
        Timber.i("markFavorite $record")
        services.preferences.setFavorite(record)
        Toast.makeText(
            requireContext(),
            getString(
                com.tikalk.worktracker.R.string.favorite_marked,
                record.project.name,
                record.task.name
            ),
            Toast.LENGTH_LONG
        ).show()
    }

    @MainThread
    protected fun populateAndBind(record: R) {
        Timber.i("populateAndBind record=$record")
        populateForm(record)
        bindForm(record)
    }

    protected open fun setRecordValue(record: R) {
        Timber.d("setRecordValue record=$record")
        this.record = record
    }

    protected open fun setRecordProject(record: R, project: Project): Boolean {
        Timber.d("setRecordProject project=$project")
        if (record.project != project) {
            record.project = project
            return true
        }
        return false
    }

    protected open fun setRecordTask(record: R, task: ProjectTask): Boolean {
        Timber.d("setRecordTask task=$task")
        if (record.task != task) {
            record.task = task
            return true
        }
        return false
    }

    protected open fun setRecordStart(record: R, time: Calendar): Boolean {
        Timber.d("setRecordStart time=$time")
        if (record.start != time) {
            record.date = time
            record.start = time
            return true
        }
        return false
    }

    protected open fun setRecordFinish(record: R, time: Calendar): Boolean {
        Timber.d("setRecordFinish time=$time")
        if (record.finish != time) {
            record.finish = time
            return true
        }
        return false
    }

    protected open fun setRecordDuration(record: R, date: Calendar): Boolean {
        Timber.d("setRecordDuration date=$date")
        record.setDurationDateTime(date.timeInMillis)
        return true
    }

    protected fun applyFavorite(record: R) {
        val projectFavorite = services.preferences.getFavoriteProject()
        if (projectFavorite != TikalEntity.ID_NONE) {
            val projects = viewModel.projects
            val project = projects.find { it.id == projectFavorite } ?: record.project
            setRecordProject(record, project)

            val tasks = project.tasks
            val taskFavorite = services.preferences.getFavoriteTask()
            if (taskFavorite != TikalEntity.ID_NONE) {
                val task = tasks.find { it.id == taskFavorite } ?: record.task
                setRecordTask(record, task)
            }
        }
    }

    protected open fun getEmptyProjectName() =
        requireContext().getString(com.tikalk.worktracker.R.string.project_name_select)

    protected open fun getEmptyTaskName() =
        requireContext().getString(com.tikalk.worktracker.R.string.task_name_select)

    protected fun addEmptyProject(projects: List<Project>?): List<Project> {
        val projectEmptyFind = projects?.find { it.isEmpty() }
        val projectEmpty = projectEmptyFind ?: viewModel.projectEmpty
        projectEmpty.name = getEmptyProjectName()
        val projectsWithEmpty = if (projects != null) {
            projects.filter { !it.isEmpty() }
                .sortedBy { it.name }
                .add(0, projectEmpty)
        } else {
            listOf(projectEmpty)
        }
        viewModel.projectEmpty = projectEmpty

        return projectsWithEmpty
    }

    protected open fun addEmptyProject(projectsFlow: Flow<List<Project>>): Flow<List<Project>> {
        return projectsFlow.map { projects ->
            val projectEmptyFind = projects.find { it.isEmpty() }
            val projectEmpty = projectEmptyFind ?: viewModel.projectEmpty
            projectEmpty.name = getEmptyProjectName()
            val projectsWithEmpty = projects.filter { !it.isEmpty() }
                .sortedBy { it.name }
                .add(0, projectEmpty)
            viewModel.projectEmpty = projectEmpty
            projectsWithEmpty
        }
    }

    protected open fun addEmptyTask(tasks: List<ProjectTask>): List<ProjectTask> {
        val taskEmptyFind = tasks.find { it.isEmpty() }
        val taskEmpty = taskEmptyFind ?: getEmptyTask()
        taskEmpty.name = getEmptyTaskName()
        val tasksWithEmpty = tasks.filter { !it.isEmpty() }
            .sortedBy { it.name }
            .add(0, taskEmpty)
        viewModel.taskEmpty = taskEmpty
        return tasksWithEmpty
    }

    protected fun getProjects(): List<Project> {
        return addEmptyProject(viewModel.projects)
    }

    protected open fun getEmptyTask(): ProjectTask {
        return viewModel.taskEmpty.apply {
            name = getEmptyTaskName()
        }
    }

    companion object {
        const val STATE_RECORD = "record"

        const val EXTRA_DATE = BuildConfig.APPLICATION_ID + ".form.DATE"
        const val EXTRA_RECORD_ID = BuildConfig.APPLICATION_ID + ".form.RECORD_ID"

        const val EXTRA_PROJECT_ID = BuildConfig.APPLICATION_ID + ".form.PROJECT_ID"
        const val EXTRA_TASK_ID = BuildConfig.APPLICATION_ID + ".form.TASK_ID"
        const val EXTRA_START_TIME = BuildConfig.APPLICATION_ID + ".form.START_TIME"
        const val EXTRA_FINISH_TIME = BuildConfig.APPLICATION_ID + ".form.FINISH_TIME"
        const val EXTRA_DURATION = BuildConfig.APPLICATION_ID + ".form.DURATION"
        const val EXTRA_STOP = BuildConfig.APPLICATION_ID + ".form.STOP"
    }
}