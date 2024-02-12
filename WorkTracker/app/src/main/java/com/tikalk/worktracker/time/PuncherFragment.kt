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
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.tikalk.app.findParentFragment
import com.tikalk.compose.TikalTheme
import com.tikalk.core.databinding.FragmentComposeBinding
import com.tikalk.util.getParcelableCompat
import com.tikalk.widget.PaddedBox
import com.tikalk.worktracker.BuildConfig
import com.tikalk.worktracker.R
import com.tikalk.worktracker.app.TrackerFragmentDelegate
import com.tikalk.worktracker.db.TimeRecordEntity
import com.tikalk.worktracker.db.toTimeRecord
import com.tikalk.worktracker.db.toTimeRecordEntity
import com.tikalk.worktracker.lang.isFalse
import com.tikalk.worktracker.lang.isTrue
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.isNullOrEmpty
import com.tikalk.worktracker.model.time.PuncherPage
import com.tikalk.worktracker.model.time.TimeRecord
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import timber.log.Timber

class PuncherFragment : TimeFormFragment<TimeRecord>() {

    private var _binding: FragmentComposeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentComposeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            viewModel.deleted.collect { data ->
                if (data != null) onRecordDeleted(data.record)
            }
        }
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
                PaddedBox {
                    PuncherForm(
                        projects = getProjects(),
                        taskEmpty = getEmptyTask(),
                        record = record,
                        onRecordCallback = ::setRecordValue,
                        onStartClick = ::startTimer,
                        onStopClick = ::stopTimer
                    )
                }
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

        viewModel.stopRecord()
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
            remove(EXTRA_DURATION)
            remove(EXTRA_STOP)
        }
    }

    private fun getStartedRecord(args: Bundle? = arguments): TimeRecord? {
        val started = viewModel.getStartedRecord()
        if (started != null) {
            return started
        }

        if (args != null) {
            if (args.containsKey(EXTRA_PROJECT_ID) and args.containsKey(EXTRA_TASK_ID)) {
                val projectId = args.getLong(EXTRA_PROJECT_ID)
                val taskId = args.getLong(EXTRA_TASK_ID)
                val startTime = args.getLong(EXTRA_START_TIME)
                val finishTime = args.getLong(EXTRA_FINISH_TIME, System.currentTimeMillis())
                val duration = args.getLong(EXTRA_DURATION, 0L)

                val projects = viewModel.projects
                val project = projects.find { it.id == projectId } ?: viewModel.projectEmpty
                val tasks = project.tasks
                val task = tasks.find { it.id == taskId } ?: viewModel.taskEmpty

                val record = TimeRecord(
                    id = TikalEntity.ID_NONE,
                    project = project,
                    task = task,
                    date = Calendar.getInstance().apply { timeInMillis = startTime }
                )
                if (duration > 0L) {
                    record.duration = duration
                }
                if (startTime != TimeRecord.NEVER) {
                    record.startTime = startTime
                }
                if (finishTime != TimeRecord.NEVER) {
                    record.finishTime = finishTime
                }
                return record
            }
        }

        return null
    }

    override fun populateForm(record: TimeRecord) {
        Timber.i("populateForm record=$record")
        val recordStarted = getStartedRecord() ?: TimeRecord.EMPTY
        Timber.i("populateForm recordStarted=$recordStarted")
        if (recordStarted.project.isNullOrEmpty() && recordStarted.task.isNullOrEmpty()) {
            applyFavorite(record)
        } else if (!recordStarted.isEmpty()) {
            val projects = viewModel.projects
            val recordStartedProjectId = recordStarted.project.id
            val recordStartedTaskId = recordStarted.task.id
            val project = projects.find { it.id == recordStartedProjectId } ?: record.project
            setRecordProject(record, project)
            val tasks = project.tasks
            val task = tasks.find { it.id == recordStartedTaskId } ?: record.task
            setRecordTask(record, task)
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
                putLong(TimeEditFragment.EXTRA_DURATION, record.duration)
                putLong(TimeEditFragment.EXTRA_FINISH_TIME, record.finishTime)
                putLong(TimeEditFragment.EXTRA_PROJECT_ID, record.project.id)
                putLong(TimeEditFragment.EXTRA_RECORD_ID, record.id)
                putLong(TimeEditFragment.EXTRA_START_TIME, record.startTime)
                putLong(TimeEditFragment.EXTRA_TASK_ID, record.task.id)
                putBoolean(TimeEditFragment.EXTRA_STOP, true)
                navController.navigate(R.id.action_puncher_to_timeEdit, this)
            }
        }
    }

    override fun run() {
        Timber.i("run first=$firstRun")
        lifecycleScope.launch {
            try {
                viewModel.puncherPage(firstRun)
                    .flowOn(Dispatchers.IO)
                    .collect { page ->
                        processPage(page)
                        populateAndBind(page.record)
                        handleArguments()
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error loading page: ${e.message}")
                handleError(e)
            }
        }
    }

    private fun processPage(page: PuncherPage) {
        viewModel.projects = page.projects
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
            val projects = viewModel.projects
            val record = recordParcel.toTimeRecord(projects)
            setRecordValue(record)
            populateForm(record)
            bindForm(record)
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        if (view?.isVisible.isTrue) {
            menuInflater.inflate(R.menu.timer, menu)
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (view?.isVisible.isFalse) {
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

    override fun authenticate(submit: Boolean) {
        // Parent fragment responsible for authentication.
    }

    private fun onRecordDeleted(record: TimeRecord) {
        if (record.id == TikalEntity.ID_NONE) {
            stopTimerCancel()
            setRecordValue(record)
            bindForm(record)
        }
    }

    companion object {
        const val EXTRA_ACTION = TrackerFragmentDelegate.EXTRA_ACTION
        const val EXTRA_PROJECT_ID = TimeFormFragment.EXTRA_PROJECT_ID
        const val EXTRA_TASK_ID = TimeFormFragment.EXTRA_TASK_ID
        const val EXTRA_START_TIME = TimeFormFragment.EXTRA_START_TIME
        const val EXTRA_FINISH_TIME = TimeFormFragment.EXTRA_FINISH_TIME
        const val EXTRA_CANCEL = BuildConfig.APPLICATION_ID + ".CANCEL"

        const val ACTION_STOP = TrackerFragmentDelegate.ACTION_STOP
    }
}