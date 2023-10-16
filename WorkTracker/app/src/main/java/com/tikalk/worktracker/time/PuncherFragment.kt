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

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.tikalk.app.findParentFragment
import com.tikalk.compose.TikalTheme
import com.tikalk.util.getParcelableCompat
import com.tikalk.worktracker.BuildConfig
import com.tikalk.worktracker.R
import com.tikalk.worktracker.app.TrackerFragmentDelegate
import com.tikalk.worktracker.databinding.FragmentPuncherBinding
import com.tikalk.worktracker.db.TimeRecordEntity
import com.tikalk.worktracker.db.toTimeRecord
import com.tikalk.worktracker.db.toTimeRecordEntity
import com.tikalk.worktracker.model.Location
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.isNullOrEmpty
import com.tikalk.worktracker.model.time.PuncherPage
import com.tikalk.worktracker.model.time.TimeRecord
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import timber.log.Timber

class PuncherFragment : TimeFormFragment() {

    private var _binding: FragmentPuncherBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPuncherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @MainThread
    override fun bindForm(record: TimeRecord) {
        Timber.i("bindForm record=$record")
        val binding = _binding ?: return

        binding.composeView.setContent {
            TikalTheme {
                val projects = getProjects()
                val taskEmpty = getEmptyTask()
                PuncherForm(
                    projects = projects,
                    taskEmpty = taskEmpty,
                    record = record,
                    onStartClick = ::startTimer,
                    onStopClick = ::stopTimer
                )
            }
        }

        activity?.invalidateOptionsMenu()
    }

    private fun startTimer() {
        Timber.i("startTimer")
        val record = this.record
        if (record.project.isEmpty()) return
        if (record.task.isEmpty()) return

        val context = this.context ?: return
        val now = System.currentTimeMillis()
        record.startTime = now
        TimerWorker.startTimer(context, record)

        bindForm(record)
    }

    fun stopTimer() {
        Timber.i("stopTimer")
        val recordStarted = getStartedRecord()
        Timber.i("stopTimer recordStarted=$recordStarted")
        if (recordStarted != null) {
            setRecordValue(recordStarted)
        }
        if (record.finishTime <= TimeRecord.NEVER) {
            record.finishTime = System.currentTimeMillis()
        }

        editRecord(record)
    }

    private fun stopTimerCancel() {
        Timber.i("stopTimerCancel")

        preferences.stopRecord()
        record.apply {
            start = null
            finish = null
            bindForm(this)
        }
        arguments?.apply {
            remove(EXTRA_PROJECT_ID)
            remove(EXTRA_TASK_ID)
            remove(EXTRA_START_TIME)
            remove(EXTRA_FINISH_TIME)
            remove(EXTRA_LOCATION)
            remove(EXTRA_STOP)
        }
    }

    private fun getStartedRecord(args: Bundle? = arguments): TimeRecord? {
        val started = preferences.getStartedRecord()
        if (started != null) {
            return started
        }

        if (args != null) {
            if (args.containsKey(EXTRA_PROJECT_ID) and args.containsKey(EXTRA_TASK_ID)) {
                val projectId = args.getLong(EXTRA_PROJECT_ID)
                val taskId = args.getLong(EXTRA_TASK_ID)
                val startTime = args.getLong(EXTRA_START_TIME)
                val finishTime = args.getLong(EXTRA_FINISH_TIME, System.currentTimeMillis())
                val locationId = args.getLong(EXTRA_LOCATION)

                val projects = viewModel.projectsData.value
                val project = projects.find { it.id == projectId } ?: viewModel.projectEmpty
                val tasks = project.tasks
                val task = tasks.find { it.id == taskId } ?: viewModel.taskEmpty

                val record = TimeRecord(
                    id = TikalEntity.ID_NONE,
                    project = project,
                    task = task,
                    date = Calendar.getInstance().apply { timeInMillis = startTime }
                )
                if (startTime != TimeRecord.NEVER) {
                    record.startTime = startTime
                }
                if (finishTime != TimeRecord.NEVER) {
                    record.finishTime = finishTime
                }
                record.location = Location.valueOf(locationId)
                return record
            }
        }

        return null
    }

    override fun populateForm(record: TimeRecord) {
        Timber.i("populateForm record=$record")
        val recordStarted = getStartedRecord() ?: TimeRecord.EMPTY
        Timber.i("populateForm recordStarted=$recordStarted")
        if (recordStarted.project.isNullOrEmpty() and recordStarted.task.isNullOrEmpty()) {
            applyFavorite()
        } else if (!recordStarted.isEmpty()) {
            val projects = viewModel.projectsData.value
            val recordStartedProjectId = recordStarted.project.id
            val recordStartedTaskId = recordStarted.task.id
            val project = projects.find { it.id == recordStartedProjectId } ?: record.project
            setRecordProject(project)
            val tasks = project.tasks
            val task = tasks.find { it.id == recordStartedTaskId } ?: record.task
            setRecordTask(task)
            record.start = recordStarted.start
            record.location = recordStarted.location
        }
    }

    private fun editRecord(record: TimeRecord) {
        val navController = findNavController()
        Timber.i("editRecord record=$record currentDestination=${navController.currentDestination?.label}")
        val parent = findParentFragment(TimeListFragment::class.java)
        if (parent != null) {
            parent.editRecord(record, true)
        } else {
            Bundle().apply {
                putLong(TimeEditFragment.EXTRA_PROJECT_ID, record.project.id)
                putLong(TimeEditFragment.EXTRA_TASK_ID, record.task.id)
                putLong(TimeEditFragment.EXTRA_START_TIME, record.startTime)
                putLong(TimeEditFragment.EXTRA_FINISH_TIME, record.finishTime)
                putLong(TimeEditFragment.EXTRA_RECORD_ID, record.id)
                putLong(TimeEditFragment.EXTRA_LOCATION, record.location.id)
                putBoolean(TimeEditFragment.EXTRA_STOP, true)
                navController.navigate(R.id.action_puncher_to_timeEdit, this)
            }
        }
    }

    override fun run() {
        Timber.i("run first=$firstRun")
        lifecycleScope.launch {
            try {
                dataSource.puncherPage(firstRun)
                    .flowOn(Dispatchers.IO)
                    .collect { page ->
                        processPage(page)
                        populateAndBind()
                        handleArguments()
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error loading page: ${e.message}")
                handleError(e)
            }
        }
    }

    private fun processPage(page: PuncherPage) {
        viewModel.projectsData.value = page.projects
        setRecordValue(page.record)
    }

    private fun handleArguments() {
        Timber.d("handleArguments $arguments")
        arguments?.let { args ->
            if (args.containsKey(EXTRA_ACTION)) {
                val action = args.getString(EXTRA_ACTION)
                if (action == ACTION_STOP) {
                    args.remove(EXTRA_ACTION)
                    if (args.getBoolean(EXTRA_CANCEL)) {
                        stopTimerCancel()
                    } else {
                        stopTimer()
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(STATE_RECORD, record.toTimeRecordEntity())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val recordParcel = savedInstanceState.getParcelableCompat<TimeRecordEntity>(STATE_RECORD)
        if (recordParcel != null) {
            val projects = viewModel.projectsData.value
            val record = recordParcel.toTimeRecord(projects)
            setRecordValue(record)
            populateForm(record)
            bindForm(record)
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        if (view?.visibility == View.VISIBLE) {
            menuInflater.inflate(R.menu.timer, menu)
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (view?.visibility != View.VISIBLE) {
            return false
        }
        when (menuItem.itemId) {
            R.id.menu_favorite -> {
                markFavorite()
                return true
            }
        }
        return super.onMenuItemSelected(menuItem)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_EDIT) {
            if (resultCode == RESULT_OK) {
                Timber.i("record processed")
                stopTimerCancel()
            } else {
                Timber.i("record edit cancelled")
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun authenticate(submit: Boolean) {
        // Parent fragment responsible for authentication.
    }

    companion object {
        const val EXTRA_ACTION = TrackerFragmentDelegate.EXTRA_ACTION
        const val EXTRA_PROJECT_ID = TimeFormFragment.EXTRA_PROJECT_ID
        const val EXTRA_TASK_ID = TimeFormFragment.EXTRA_TASK_ID
        const val EXTRA_START_TIME = TimeFormFragment.EXTRA_START_TIME
        const val EXTRA_FINISH_TIME = TimeFormFragment.EXTRA_FINISH_TIME
        const val EXTRA_CANCEL = BuildConfig.APPLICATION_ID + ".CANCEL"

        const val ACTION_STOP = TrackerFragmentDelegate.ACTION_STOP

        private const val REQUEST_EDIT = 0xED17

        private const val CHILD_START = 0
        private const val CHILD_STOP = 1
    }
}