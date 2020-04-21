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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.tikalk.util.add
import com.tikalk.worktracker.BuildConfig
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.LoginFragment
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.net.InternetFragment
import timber.log.Timber

abstract class TimeFormFragment : InternetFragment(),
    LoginFragment.OnLoginListener {

    open var record: TimeRecord = TimeRecord.EMPTY.copy()
    val projectsData = MutableLiveData<List<Project>>()
    var projectEmpty: Project = Project.EMPTY.copy(true)
    var taskEmpty: ProjectTask = projectEmpty.tasksById[TikalEntity.ID_NONE]
        ?: ProjectTask.EMPTY.copy()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectsData.observe(this, Observer { projects ->
            onProjectsUpdated(projects)
        })
    }

    abstract fun populateForm(record: TimeRecord)

    @MainThread
    abstract fun bindForm(record: TimeRecord)

    fun markFavorite() {
        markFavorite(record)
    }

    protected open fun markFavorite(record: TimeRecord) {
        Timber.i("markFavorite $record")
        preferences.setFavorite(record)
        Toast.makeText(requireContext(), getString(R.string.favorite_marked, record.project.name, record.task.name), Toast.LENGTH_LONG).show()
    }

    @MainThread
    protected fun populateAndBind() {
        val record = this.record
        Timber.i("populateAndBind record=$record")
        populateForm(record)
        bindForm(record)
    }

    override fun onLoginSuccess(fragment: LoginFragment, login: String) {
        Timber.i("login success")
        fragment.dismissAllowingStateLoss()
    }

    override fun onLoginFailure(fragment: LoginFragment, login: String, reason: String) {
        Timber.e("login failure: $reason")
    }

    protected open fun setRecordValue(record: TimeRecord) {
        Timber.d("setRecordValue record=$record")
        this.record = record
    }

    protected open fun setRecordProject(project: Project) {
        Timber.d("setRecordProject project=$project")
        record.project = project
    }

    protected open fun setRecordTask(task: ProjectTask) {
        Timber.d("setRecordTask task=$task")
        record.task = task
    }

    protected open fun onProjectsUpdated(projects: List<Project>) {
        populateForm(record)
        bindForm(record)
    }

    protected fun applyFavorite() {
        val projectFavorite = preferences.getFavoriteProject()
        if (projectFavorite != TikalEntity.ID_NONE) {
            val projects = projectsData.value
            val project = projects?.find { it.id == projectFavorite } ?: record.project
            setRecordProject(project)

            val tasks = project.tasks
            val taskFavorite = preferences.getFavoriteTask()
            if (taskFavorite != TikalEntity.ID_NONE) {
                val task = tasks.find { it.id == taskFavorite } ?: record.task
                setRecordTask(task)
            }
        }
    }

    open protected fun getEmptyProjectName() = requireContext().getString(R.string.project_name_select)

    open protected fun getEmptyTaskName() = requireContext().getString(R.string.task_name_select)

    protected fun addEmpties(projects: List<Project>): List<Project> {
        val projectEmptyFind = projects.find { it.isEmpty() }
        val projectEmpty = projectEmptyFind ?: projectEmpty
        projectEmpty.name = getEmptyProjectName()
        val projectsWithEmpty = if (projectEmptyFind != null) {
            projects.sortedBy { it.name }
        } else {
            projects.sortedBy { it.name }.add(0, projectEmpty)
        }
        this.projectEmpty = projectEmpty

        val taskEmpty = projectEmpty.tasksById[TikalEntity.ID_NONE] ?: taskEmpty
        taskEmpty.name = getEmptyTaskName()
        this.taskEmpty = taskEmpty

        return projectsWithEmpty
    }

    protected fun addEmpty(tasks: List<ProjectTask>): List<ProjectTask> {
        val taskEmptyFind = tasks.find { it.isEmpty() }
        val taskEmpty = taskEmptyFind ?: taskEmpty
        taskEmpty.name = getEmptyTaskName()
        return if (taskEmptyFind != null) {
            tasks.sortedBy { it.name }
        } else {
            tasks.sortedBy { it.name }.add(0, taskEmpty)
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
    }
}