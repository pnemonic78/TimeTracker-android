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

import android.annotation.SuppressLint
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
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.tikalk.app.findFragmentByClass
import com.tikalk.app.isDestination
import com.tikalk.auth.AuthenticationException
import com.tikalk.compose.TikalTheme
import com.tikalk.lang.isFalse
import com.tikalk.lang.isTrue
import com.tikalk.time.*
import com.tikalk.widget.PaddedBox
import com.tikalk.worktracker.R
import com.tikalk.worktracker.app.TrackerFragmentDelegate
import com.tikalk.worktracker.auth.LoginFragment
import com.tikalk.worktracker.databinding.FragmentTimeListBinding
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.time.TaskRecordStatus
import com.tikalk.worktracker.model.time.TimeListPage
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.model.time.TimeTotals
import java.net.ConnectException
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import timber.log.Timber

class TimeListFragment : TimeFormFragment<TimeRecord>() {

    private var _binding: FragmentTimeListBinding? = null
    private val binding get() = _binding!!

    private lateinit var formNavHostFragment: NavHostFragment
    private val _dateFlow = MutableStateFlow<Calendar>(Calendar.getInstance())
    private val dateFlow: StateFlow<Calendar> = _dateFlow
    private val _totalsFlow = MutableStateFlow<TimeTotals?>(null)
    private val totalsFlow: StateFlow<TimeTotals?> = _totalsFlow
    private val recordsData = MutableStateFlow<List<TimeRecord>>(emptyList())

    /** Is the record from the "timer" or "+" FAB? */
    private var recordForTimer = false
    private var loginAutomatic = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimeListBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        formNavHostFragment =
            childFragmentManager.findFragmentById(R.id.nav_host_form) as NavHostFragment

        val binding = this.binding
        binding.dateInput.setContent {
            TikalTheme {
                TimeListDateButton(
                    dateFlow = dateFlow
                ) { pickedDate ->
                    val date = record.date
                    val oldYear = date.year
                    val oldMonth = date.month
                    val oldDayOfMonth = date.dayOfMonth
                    val pickedYear = pickedDate.year
                    val pickedMonth = pickedDate.month
                    val pickedDayOfMonth = pickedDate.dayOfMonth
                    val refresh = (pickedYear != oldYear)
                        || (pickedMonth != oldMonth)
                        || (pickedDayOfMonth != oldDayOfMonth)
                    navigateDate(pickedDate, refresh)
                }
            }
        }
        binding.list.setContent {
            TikalTheme {
                PaddedBox(isVertical = false) {
                    TimeList(
                        itemsFlow = recordsData,
                        onClick = ::onRecordClick,
                        onSwipe = swipeDayListener
                    )
                }
            }
        }
        binding.totals.composeView.setContent {
            TikalTheme {
                TimeTotalsFooter(totalsFlow = totalsFlow)
            }
        }
        binding.recordAdd.setContent {
            TikalTheme {
                FloatingAddButton {
                    addTime()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.deleted.collect { data ->
                if (data != null) {
                    viewModel.clearEvents()
                    onRecordEditDeleted(data.record, data.page)
                }
            }
        }
        lifecycleScope.launch {
            viewModel.edited.collect { data ->
                if (data != null) {
                    viewModel.clearEvents()
                    onRecordEditSubmitted(data.record, data.isLast, data.page)
                }
            }
        }
        lifecycleScope.launch {
            viewModel.editFailure.collect { data ->
                if (data != null) {
                    viewModel.clearEvents()
                    onRecordEditFailure(data.record, data.reason)
                }
            }
        }
        lifecycleScope.launch {
            viewModel.favorite.collect { record ->
                if (record != null) onRecordEditFavorited(record)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onRecordClick(record: TimeRecord) {
        editRecord(record)
    }

    /**
     * Fetch the page.
     */
    @MainThread
    private fun fetchPage(date: Calendar, refresh: Boolean) {
        Timber.i("loadAndFetchPage ${formatSystemDate(date)} refresh=$refresh")

        lifecycleScope.launch {
            try {
                showProgress(true)
                viewModel.timeListPage(date, refresh)
                    .flowOn(Dispatchers.IO)
                    .collect { page ->
                        processPage(page)
                        handleArguments()
                        showProgress(false)
                    }
            } catch (ae: AuthenticationException) {
                authenticate(loginAutomatic)
            } catch (ce: ConnectException) {
                Timber.e(ce, "Error loading page: ${ce.message}")
                if (refresh) {
                    fetchPage(date, false)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading page: ${e.message}")
                showProgress(false)
                handleError(e)
            }
        }
    }

    private suspend fun processPage(page: TimeListPage) {
        viewModel.projects = page.projects

        _dateFlow.emit(page.date.copy())

        recordsData.emit(page.records)
        var totals = totalsFlow.value
        if ((totals == null) || (page.totals.status == TaskRecordStatus.CURRENT)) {
            totals = page.totals
        }
        _totalsFlow.emit(totals)
        setRecordValue(page.record)
    }

    override fun authenticate(submit: Boolean) {
        val navController = findNavController()
        Timber.i("authenticate submit=$submit currentDestination=${navController.currentDestination?.label}")
        if (!navController.isDestination(R.id.loginFragment)) {
            Bundle().apply {
                putBoolean(LoginFragment.EXTRA_SUBMIT, submit)
                navController.navigate(R.id.action_timeList_to_login, this)
            }
        }
    }

    private fun pickDate() {
        val date = record.date
        val oldYear = date.year
        val oldMonth = date.month
        val oldDayOfMonth = date.dayOfMonth
        pickDate(requireContext(), date) { pickedDate ->
            val pickedYear = pickedDate.year
            val pickedMonth = pickedDate.month
            val pickedDayOfMonth = pickedDate.dayOfMonth
            val refresh = (pickedYear != oldYear)
                || (pickedMonth != oldMonth)
                || (pickedDayOfMonth != oldDayOfMonth)
            navigateDate(pickedDate, refresh)
        }
    }

    private fun addTime() {
        editRecord(TimeRecord.EMPTY.copy().apply { date = record.date })
    }

    fun editRecord(record: TimeRecord, isTimer: Boolean = false) {
        Timber.i("editRecord record=$record timer=$isTimer")
        recordForTimer = isTimer

        val form = findTopFormFragment()
        if (form is TimeEditFragment) {
            form.editRecord(record, isStop = isTimer)
        } else {
            val navController = formNavHostFragment.navController
            val currentDestination = navController.currentDestination ?: return
            Timber.i("editRecord editor.currentDestination=${currentDestination.label}")
            if (currentDestination.id == R.id.puncherFragment) {
                Bundle().apply {
                    putLong(TimeEditFragment.EXTRA_DATE, record.dateTime)
                    putLong(TimeEditFragment.EXTRA_DURATION, record.duration)
                    putLong(TimeEditFragment.EXTRA_FINISH_TIME, record.finishTime)
                    putLong(TimeEditFragment.EXTRA_PROJECT_ID, record.project.id)
                    putLong(TimeEditFragment.EXTRA_RECORD_ID, record.id)
                    putLong(TimeEditFragment.EXTRA_START_TIME, record.startTime)
                    putLong(TimeEditFragment.EXTRA_TASK_ID, record.task.id)
                    putBoolean(TimeEditFragment.EXTRA_STOP, isTimer)
                    navController.navigate(R.id.action_puncher_to_timeEdit, this)
                }
            }
        }
    }

    override fun populateForm(record: TimeRecord) = Unit

    @MainThread
    override fun bindForm(record: TimeRecord) = Unit

    fun stopTimer() {
        Timber.i("stopTimer")
        val form = findTopFormFragment()
        if (form is PuncherFragment) {
            form.stopTimer()
            return
        }
        // Save for "run" later.
        val args = arguments ?: Bundle()
        args.putString(EXTRA_ACTION, ACTION_STOP)
        if (arguments == null) {
            arguments = args
        }
    }

    private fun navigateNextDay() {
        Timber.i("navigateNextDay")
        val cal = record.date
        cal.add(Calendar.DATE, 1)
        navigateDate(cal)
    }

    private fun navigatePreviousDay() {
        Timber.i("navigatePreviousDay")
        val cal = record.date
        cal.add(Calendar.DATE, -1)
        navigateDate(cal)
    }

    private fun navigateToday() {
        Timber.i("navigateToday")
        val today = Calendar.getInstance()
        navigateDate(today)
    }

    private fun navigateDate(date: Calendar, refresh: Boolean = true) {
        Timber.i("navigateDate ${formatSystemDate(date)}")
        fetchPage(date, refresh)
        hideEditor()
    }

    @MainThread
    override fun run() {
        Timber.i("run first=$firstRun")
        fetchPage(record.date, firstRun)
    }

    private fun handleArguments() {
        val args = arguments
        if (args != null) {
            if (args.containsKey(EXTRA_ACTION)) {
                when (args.getString(EXTRA_ACTION)) {
                    ACTION_DATE -> {
                        val dateMs = args.getLong(EXTRA_DATE, System.currentTimeMillis())
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = dateMs
                        navigateDate(cal, true)
                        args.remove(EXTRA_ACTION)
                    }

                    ACTION_STOP -> {
                        stopTimer()
                        args.remove(EXTRA_ACTION)
                    }

                    ACTION_TODAY -> {
                        navigateToday()
                        args.remove(EXTRA_ACTION)
                    }
                }
            }
        }
    }

    override fun onLoginFailure(login: String, reason: String) {
        super.onLoginFailure(login, reason)
        loginAutomatic = false
        if (login.isEmpty() || (reason == LoginFragment.REASON_CANCEL)) {
            activity?.finish()
        }
    }

    private fun onRecordEditSubmitted(record: TimeRecord, isLast: Boolean, page: TimeListPage?) {
        Timber.i("record submitted: $record")
        if (record.id == TikalEntity.ID_NONE) {
            val records = recordsData.value
            val recordsNew: MutableList<TimeRecord> = ArrayList(records)
            recordsNew.add(record)
            recordsNew.sortBy { it.startTime }
            lifecycleScope.launch { recordsData.emit(recordsNew) }

            if (recordForTimer) {
                Bundle().apply {
                    putString(PuncherFragment.EXTRA_ACTION, PuncherFragment.ACTION_STOP)
                    putBoolean(PuncherFragment.EXTRA_CANCEL, true)
                    showTimer(this, true)
                }
                // Refresh the list with the inserted item.
                maybeFetchPage(record.date, page)
                return
            }
        }

        if (isLast) {
            showTimer()
            // Refresh the list with the edited item.
            if (record.id != TikalEntity.ID_NONE) {
                val records = recordsData.value
                val recordsNew: MutableList<TimeRecord> = ArrayList(records)
                val index = recordsNew.indexOfFirst { it.id == record.id }
                if (index >= 0) {
                    recordsNew[index] = record
                    recordsNew.sortBy { it.startTime }
                    lifecycleScope.launch { recordsData.emit(recordsNew) }
                }
            }
            maybeFetchPage(record.date, page)
        }
    }

    private fun onRecordEditDeleted(record: TimeRecord, page: TimeListPage?) {
        Timber.i("record deleted: $record")
        if (record.id == TikalEntity.ID_NONE) {
            if (recordForTimer) {
                Bundle().apply {
                    putString(PuncherFragment.EXTRA_ACTION, PuncherFragment.ACTION_STOP)
                    putBoolean(PuncherFragment.EXTRA_CANCEL, true)
                    showTimer(this, true)
                }
            } else {
                showTimer()
            }
        } else {
            showTimer()
            // Refresh the list with the deleted item.
            val records = recordsData.value
            val recordsActive = records.filter { it.status != TaskRecordStatus.DELETED }
            lifecycleScope.launch { recordsData.emit(recordsActive) }
            maybeFetchPage(record.date, page)
        }
    }

    private fun onRecordEditFavorited(record: TimeRecord) {
        Timber.i("record favorited: ${record.project} / ${record.task}")
    }

    private fun onRecordEditFailure(record: TimeRecord, reason: String) {
        Timber.e("record failure: $record $reason")
    }

    override fun onBackPressed(): Boolean {
        if (formNavHostFragment.navController.popBackStack()) {
            return true
        }
        return super.onBackPressed()
    }

    private fun showTimer(args: Bundle? = null, popInclusive: Boolean = false) {
        Timber.i("showTimer timer.currentDestination=${formNavHostFragment.navController.currentDestination?.label}")
        formNavHostFragment.navController.popBackStack(R.id.puncherFragment, popInclusive)
        if (popInclusive) {
            formNavHostFragment.navController.navigate(R.id.puncherFragment, args)
        }
    }

    private fun hideEditor() {
        showTimer()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
        if (view?.isVisible.isTrue) {
            menuInflater.inflate(R.menu.time_list, menu)
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (view?.isVisible.isFalse) {
            return false
        }
        when (menuItem.itemId) {
            R.id.menu_date -> {
                pickDate()
                return true
            }

            R.id.menu_today -> {
                navigateToday()
                return true
            }
        }
        return super.onMenuItemSelected(menuItem)
    }

    @Suppress("UNCHECKED_CAST")
    private fun findTopFormFragment(): TimeFormFragment<TimeRecord> {
        return formNavHostFragment.childFragmentManager.findFragmentByClass(TimeFormFragment::class.java) as TimeFormFragment<TimeRecord>
    }

    private fun maybeFetchPage(date: Calendar, page: TimeListPage? = null) {
        if (page != null) {
            CoroutineScope(Dispatchers.IO).launch {
                processPage(page)
            }
        } else {
            fetchPage(date, true)
        }
    }

    private val swipeDayListener = object : OnSwipeDayListener {
        override fun onSwipePreviousDay() {
            navigatePreviousDay()
        }

        override fun onSwipeNextDay() {
            navigateNextDay()
        }
    }

    companion object {
        const val ACTION_DATE = TrackerFragmentDelegate.ACTION_DATE
        const val ACTION_STOP = TrackerFragmentDelegate.ACTION_STOP
        const val ACTION_TODAY = TrackerFragmentDelegate.ACTION_TODAY

        const val EXTRA_ACTION = TrackerFragmentDelegate.EXTRA_ACTION
        const val EXTRA_DATE = TrackerFragmentDelegate.EXTRA_DATE
    }
}