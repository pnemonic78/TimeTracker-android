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
import android.text.format.DateUtils
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
import com.tikalk.app.isDestination
import com.tikalk.auth.AuthenticationException
import com.tikalk.compose.TikalTheme
import com.tikalk.core.databinding.FragmentComposeBinding
import com.tikalk.lang.isFalse
import com.tikalk.lang.isTrue
import com.tikalk.util.getParcelableCompat
import com.tikalk.widget.PaddedBox
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.LoginFragment
import com.tikalk.worktracker.db.TimeRecordEntity
import com.tikalk.worktracker.db.toTimeRecord
import com.tikalk.worktracker.db.toTimeRecordEntity
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.isNullOrEmpty
import com.tikalk.worktracker.model.time.FormPage
import com.tikalk.worktracker.model.time.TaskRecordStatus
import com.tikalk.worktracker.model.time.TimeEditPage
import com.tikalk.worktracker.model.time.TimeListPage
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.model.time.TimeRecord.Companion.NEVER
import com.tikalk.worktracker.model.time.split
import com.tikalk.worktracker.net.InternetFragment
import java.net.ConnectException
import java.util.Calendar
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import timber.log.Timber

class TimeEditFragment : TimeFormFragment<TimeRecord>() {

    private var _binding: FragmentComposeBinding? = null
    private val binding get() = _binding!!

    private val recordsToSubmit = CopyOnWriteArrayList<TimeRecord>()
    private val errorFlow = MutableStateFlow<TimeFormError?>(null)

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

        binding.composeView.setContent {
            TikalTheme {
                PaddedBox {
                    TimeEditForm(
                        projectsFlow = viewModel.projectsFlow,
                        taskEmpty = getEmptyTask(),
                        recordFlow = recordFlow,
                        errorFlow = errorFlow,
                        onRecordChanged = ::onRecordChanged
                    )
                }
            }
        }
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
                    val projects = viewModel.projects
                    setRecordProject(record, projects.find { it.id == projectId }
                        ?: viewModel.projectEmpty)
                }
                if (args.containsKey(EXTRA_TASK_ID)) {
                    val taskId = args.getLong(EXTRA_TASK_ID)
                    val tasks = record.project.tasks
                    val task = tasks.find { it.id == taskId } ?: viewModel.taskEmpty
                    setRecordTask(record, task)
                }
                if (args.containsKey(EXTRA_DATE)) {
                    val dateTime = args.getLong(EXTRA_DATE)
                    record.date = Calendar.getInstance().apply { timeInMillis = dateTime }
                }
                if (args.containsKey(EXTRA_DURATION)) {
                    val duration = args.getLong(EXTRA_DURATION)
                    if (duration > 0L) {
                        record.duration = duration
                    }
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
            }
        }

        if (record.project.isNullOrEmpty() and record.task.isNullOrEmpty()) {
            applyFavorite(record)
        }
    }

    override fun bindForm(record: TimeRecord) {
        Timber.i("bindForm record=$record")
        this.record = record
    }

    private suspend fun validateForm(record: TimeRecord): Boolean {
        if (record.project.id == TikalEntity.ID_NONE) {
            setErrorLabel(TimeFormError.Project(getString(R.string.error_project_field_required)))
            return false
        }
        if (record.task.id == TikalEntity.ID_NONE) {
            setErrorLabel(TimeFormError.Task(getString(R.string.error_task_field_required)))
            return false
        }
        if (record.duration <= DateUtils.MINUTE_IN_MILLIS) {
            if (record.startTime == NEVER) {
                if (record.duration == 0L) {
                    setErrorLabel(TimeFormError.Start(getString(R.string.error_start_field_required)))
                } else {
                    setErrorLabel(TimeFormError.Duration(getString(R.string.error_finish_time_before_start_time)))
                }
                return false
            }
            if (record.finishTime == NEVER) {
                setErrorLabel(TimeFormError.Finish(getString(R.string.error_finish_field_required)))
                return false
            }

            setErrorLabel(TimeFormError.Finish(getString(R.string.error_finish_time_before_start_time)))
            return false
        }

        setErrorLabel("")
        return true
    }

    override fun run() {
        Timber.i("run first=$firstRun")
        run(arguments)
    }

    @MainThread
    private fun run(arguments: Bundle?) {
        run(arguments, firstRun)
    }

    @MainThread
    private fun run(arguments: Bundle?, refresh: Boolean) {
        if (maybeResubmit()) return

        val args = arguments ?: Bundle()
        if (args.isEmpty) {
            if (view?.isVisible.isFalse) {
                return
            }
            // The parent fragment should be responsible for authentication.
            if (findParentFragment(InternetFragment::class.java) != null) {
                return
            }
        }
        val record = this.record
        val recordId = args.getLong(EXTRA_RECORD_ID, record.id)

        lifecycleScope.launch {
            try {
                showProgress(true)
                viewModel.editPage(recordId, refresh)
                    .flowOn(Dispatchers.IO)
                    .collect { page ->
                        processPage(page)
                        populateAndBind(page.record)
                        showProgress(false)
                    }
            } catch (ce: ConnectException) {
                Timber.e(ce, "Error loading page: ${ce.message}")
                if (refresh) {
                    run(arguments, false)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading page: ${e.message}")
                showProgress(false)
                handleError(e)
            }
        }
    }

    private suspend fun processPage(page: TimeEditPage) {
        setRecordValue(page.record)
        viewModel.projects = page.projects
        val errorMessage = page.errorMessage
        val error = if (errorMessage.isNullOrEmpty()) null else TimeFormError.General(errorMessage)
        errorFlow.emit(error)
    }

    override fun onLoginFailure(login: String, reason: String) {
        super.onLoginFailure(login, reason)
        activity?.finish()
    }

    override fun authenticate(submit: Boolean) {
        val navController = findNavController()
        Timber.i("authenticate submit=$submit currentDestination=${navController.currentDestination?.label}")
        if (!navController.isDestination(R.id.loginFragment)) {
            Bundle().apply {
                putBoolean(LoginFragment.EXTRA_SUBMIT, submit)
                navController.navigate(R.id.action_timeEdit_to_login, this)
            }
        }
    }

    @MainThread
    private fun submitRecord() {
        lifecycleScope.launch { submit() }
    }

    @MainThread
    private suspend fun submit(): Boolean {
        val record = this.record
        Timber.i("submit $record")

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

    @MainThread
    private suspend fun submit(records: List<TimeRecord>) {
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

    @MainThread
    private suspend fun submit(
        record: TimeRecord,
        isFirst: Boolean = true,
        isLast: Boolean = true
    ) {
        Timber.i("submit $record first=$isFirst last=$isLast")
        try {
            // Show a progress spinner, and kick off a background task to submit the form.
            if (isFirst) {
                showProgress(true)
                setErrorLabel("")
            }

            viewModel.editRecord(record)
                .flowOn(Dispatchers.IO)
                .collect { page ->
                    if (page.projects.isEmpty()) return@collect
                    if (isLast) {
                        showProgress(false)
                    }
                    processSubmittedPage(record, isLast, page)
                }
        } catch (ae: AuthenticationException) {
            authenticate(true)
        } catch (e: Exception) {
            Timber.e(e, "Error saving record: ${e.message}")
            showProgress(false)
            handleError(e)
        }
    }

    private suspend fun processSubmittedPage(
        record: TimeRecord,
        isLast: Boolean,
        page: FormPage<*>
    ) {
        val errorMessage = page.errorMessage
        Timber.i("processSubmittedPage last=$isLast error=[$errorMessage]")
        if (errorMessage.isNullOrEmpty() && (page is TimeListPage)) {
            onRecordSubmitted(record, isLast, page)
        } else {
            onRecordError(record, errorMessage.orEmpty())
        }
    }

    private suspend fun onRecordSubmitted(record: TimeRecord, isLast: Boolean, page: TimeListPage) {
        recordsToSubmit.remove(record)
        viewModel.onRecordEditSubmitted(record, isLast, page)

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

    @MainThread
    private suspend fun deleteRecord(record: TimeRecord) {
        Timber.i("deleteRecord $record")
        if (record.id == TikalEntity.ID_NONE) {
            record.start = null
            record.finish = null
            record.status = TaskRecordStatus.DELETED
            viewModel.onRecordEditDeleted(record)
            return
        }

        // Update the remote server.
        try {
            // Show a progress spinner, and kick off a background task to fetch the page.
            showProgress(true)

            viewModel.deleteRecord(record)
                .flowOn(Dispatchers.IO)
                .collect { page ->
                    showProgress(false)
                    processDeletePage(record, page)
                }
        } catch (ae: AuthenticationException) {
            authenticate(true)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting record: ${e.message}")
            showProgress(false)
            handleError(e)
        }
    }

    private suspend fun processDeletePage(record: TimeRecord, page: FormPage<*>) {
        Timber.i("processDeletePage $record $page")
        val errorMessage = page.errorMessage
        if (errorMessage.isNullOrEmpty() && (page is TimeListPage)) {
            onRecordDeleted(record, page)
        } else {
            onRecordError(record, errorMessage.orEmpty())
        }
    }

    private suspend fun onRecordDeleted(record: TimeRecord, page: TimeListPage?) {
        record.status = TaskRecordStatus.DELETED
        recordsToSubmit.remove(record)
        viewModel.onRecordEditDeleted(record, page)

        val isStop = arguments?.getBoolean(EXTRA_STOP, false) ?: false
        if (isStop) {
            stopTimer()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val record = this.record
        outState.putParcelable(STATE_RECORD, record.toTimeRecordEntity())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val recordParcel: TimeRecordEntity? =
            savedInstanceState.getParcelableCompat<TimeRecordEntity>(STATE_RECORD)
        if (recordParcel != null) {
            val projects = viewModel.projects
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
            putLong(EXTRA_DATE, record.dateTime)
            putLong(EXTRA_DURATION, record.duration)
            putLong(EXTRA_FINISH_TIME, record.finishTime)
            putLong(EXTRA_PROJECT_ID, record.project.id)
            putLong(EXTRA_RECORD_ID, record.id)
            putLong(EXTRA_START_TIME, record.startTime)
            putLong(EXTRA_TASK_ID, record.task.id)
            putBoolean(EXTRA_STOP, isStop)
        }
        delegate.markFirst()
        run(args)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        if (view?.isVisible.isTrue) {
            menuInflater.inflate(R.menu.time_edit, menu)
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (view?.isVisible.isFalse) {
            return false
        }
        when (menuItem.itemId) {
            R.id.menu_delete -> {
                deleteRecord()
                return true
            }

            R.id.menu_submit -> {
                submitRecord()
                return true
            }

            R.id.menu_favorite -> {
                markFavorite()
                return true
            }
        }
        return super.onMenuItemSelected(menuItem)
    }

    private suspend fun setErrorLabel(text: String) {
        val error = if (text.isEmpty()) null else TimeFormError.General(text)
        setErrorLabel(error)
    }

    private suspend fun setErrorLabel(error: TimeFormError?) {
        errorFlow.emit(error)
    }

    private fun setErrorLabelMain(text: String) {
        lifecycleScope.launch(Dispatchers.Main) { setErrorLabel(text) }
    }

    /** Maybe we tried to submit the form and were asked to login first? */
    @MainThread
    private fun maybeResubmit(): Boolean {
        val records = recordsToSubmit
        Timber.i("maybeResubmit records=$records")
        if (records.isNotEmpty()) {
            lifecycleScope.launch {
                submit(records)
            }
            return true
        }
        return false
    }

    private fun stopTimer() {
        viewModel.stopRecord()
    }

    private fun onRecordChanged(record: TimeRecord) {
        markRecordModified(record)
        this.record = record
    }

    private fun markRecordModified(record: TimeRecord) {
        if (record.status == TaskRecordStatus.CURRENT) {
            record.status = TaskRecordStatus.MODIFIED
            record.version++
        }
    }


    companion object {
        const val EXTRA_DATE = TimeFormFragment.EXTRA_DATE
        const val EXTRA_PROJECT_ID = TimeFormFragment.EXTRA_PROJECT_ID
        const val EXTRA_TASK_ID = TimeFormFragment.EXTRA_TASK_ID
        const val EXTRA_START_TIME = TimeFormFragment.EXTRA_START_TIME
        const val EXTRA_FINISH_TIME = TimeFormFragment.EXTRA_FINISH_TIME
        const val EXTRA_DURATION = TimeFormFragment.EXTRA_DURATION
        const val EXTRA_RECORD_ID = TimeFormFragment.EXTRA_RECORD_ID
        const val EXTRA_STOP = TimeFormFragment.EXTRA_STOP
    }
}
