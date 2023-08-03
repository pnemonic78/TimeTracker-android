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
import android.app.DatePickerDialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.tikalk.app.findFragmentByClass
import com.tikalk.app.isNavDestination
import com.tikalk.compose.TikalTheme
import com.tikalk.util.TikalFormatter
import com.tikalk.worktracker.R
import com.tikalk.worktracker.app.TrackerFragmentDelegate
import com.tikalk.worktracker.auth.LoginFragment
import com.tikalk.worktracker.data.remote.TimeListPageParser
import com.tikalk.worktracker.databinding.FragmentTimeListBinding
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.time.TaskRecordStatus
import com.tikalk.worktracker.model.time.TimeListPage
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.model.time.TimeTotals
import java.util.Calendar
import kotlin.math.absoluteValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import timber.log.Timber

class TimeListFragment : TimeFormFragment() {

    private var _binding: FragmentTimeListBinding? = null
    private val binding get() = _binding!!

    private var datePickerDialog: DatePickerDialog? = null
    private lateinit var formNavHostFragment: NavHostFragment
    private val dateData = MutableStateFlow<Calendar>(Calendar.getInstance())
    private val totalsData = MutableStateFlow<TimeTotals?>(null)
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
        binding.dateInput.setOnClickListener { pickDate() }
        binding.recordAdd.setOnClickListener { addTime() }

        binding.list.setContent {
            TikalTheme {
                TimeList(
                    itemsFlow = recordsData,
                    onClick = ::onRecordClick,
                    onSwipe = swipeDayListener
                )
            }
        }
        lifecycleScope.launch {
            dateData.collect { date ->
                bindDate(date)
            }
        }
        lifecycleScope.launch {
            totalsData.collect { totals ->
                if (totals != null) bindTotals(totals)
            }
        }
        lifecycleScope.launch {
            viewModel.deleted.collect { data ->
                if (data != null) onRecordEditDeleted(data.record, data.responseHtml)
            }
        }
        lifecycleScope.launch {
            viewModel.edited.collect { data ->
                if (data != null) onRecordEditSubmitted(data.record, data.isLast, data.responseHtml)
            }
        }
        lifecycleScope.launch {
            viewModel.editFailure.collect { data ->
                if (data != null) onRecordEditFailure(data.record, data.reason)
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
     * Load and then fetch.
     */
    private fun loadAndFetchPage(date: Calendar, refresh: Boolean) {
        Timber.i("loadAndFetchPage ${formatSystemDate(date)} refresh=$refresh")

        showProgress(true)
        lifecycleScope.launch {
            try {
                dataSource.timeListPage(date, refresh)
                    .flowOn(Dispatchers.IO)
                    .collect { page ->
                        processPage(page)
                        handleArguments()
                        showProgress(false)
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error loading page: ${e.message}")
                showProgress(false)
                handleError(e)
            }
        }
    }

    private var fetchingPage = false

    /**
     * Fetch from remote server.
     */
    private fun fetchPage(date: Calendar) {
        val dateFormatted = formatSystemDate(date)!!
        Timber.i("fetchPage $dateFormatted fetching=$fetchingPage")
        if (fetchingPage) return
        fetchingPage = true

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = service.fetchTimes(dateFormatted)
                if (isValidResponse(response)) {
                    val html = response.body()!!
                    processPage(html, date)
                } else {
                    authenticateMain(loginAutomatic)
                }
                fetchingPage = false
            } catch (e: Exception) {
                Timber.e(e, "Error fetching page: ${e.message}")
                handleErrorMain(e)
                fetchingPage = false
            }
        }
    }

    private suspend fun processPage(html: String, date: Calendar) {
        Timber.i("processPage ${formatSystemDate(date)}")
        val page = TimeListPageParser().parse(html)
        processPage(page)
        dataSource.savePage(page)
    }

    private suspend fun processPage(page: TimeListPage) {
        viewModel.projectsData.emit(page.projects.sortedBy { it.name })

        dateData.emit(page.date)

        recordsData.emit(page.records)
        var totals = totalsData.value
        if ((totals == null) || (page.totals.status == TaskRecordStatus.CURRENT)) {
            totals = page.totals
        }
        totalsData.emit(totals)
        setRecordValue(page.record)
    }

    @MainThread
    private fun bindDate(date: Calendar) {
        val binding = _binding ?: return
        binding.dateInput.text =
            DateUtils.formatDateTime(context, date.timeInMillis, FORMAT_DATE_BUTTON)
    }

    @MainThread
    private fun bindTotals(totals: TimeTotals) {
        val binding = _binding ?: return
        val bindingTotals = binding.totals
        bindingTotals.setContent {
            TikalTheme {
                TimeTotalsFooter(totals = totals)
            }
        }
    }

    override fun authenticate(submit: Boolean) {
        val navController = findNavController()
        Timber.i("authenticate submit=$submit currentDestination=${navController.currentDestination?.label}")
        if (!isNavDestination(R.id.loginFragment)) {
            Bundle().apply {
                putBoolean(LoginFragment.EXTRA_SUBMIT, submit)
                navController.navigate(R.id.action_timeList_to_login, this)
            }
        }
    }

    private fun pickDate() {
        val date = record.date
        val cal = date
        val year = cal.year
        val month = cal.month
        val dayOfMonth = cal.dayOfMonth
        var picker = datePickerDialog
        if (picker == null) {
            val listener =
                DatePickerDialog.OnDateSetListener { _, pickedYear, pickedMonth, pickedDayOfMonth ->
                    val oldYear = date.year
                    val oldMonth = date.month
                    val oldDayOfMonth = date.dayOfMonth
                    val refresh =
                        (pickedYear != oldYear) || (pickedMonth != oldMonth) || (pickedDayOfMonth != oldDayOfMonth)
                    cal.year = pickedYear
                    cal.month = pickedMonth
                    cal.dayOfMonth = pickedDayOfMonth
                    navigateDate(cal, refresh)
                }
            val context = requireContext()
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
            datePickerDialog = picker
        } else {
            picker.updateDate(year, month, dayOfMonth)
        }
        picker.show()
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
            Timber.i("editRecord editor.currentDestination=${formNavHostFragment.navController.currentDestination?.label}")
            Bundle().apply {
                putLong(TimeEditFragment.EXTRA_DATE, record.date.timeInMillis)
                putLong(TimeEditFragment.EXTRA_PROJECT_ID, record.project.id)
                putLong(TimeEditFragment.EXTRA_TASK_ID, record.task.id)
                putLong(TimeEditFragment.EXTRA_START_TIME, record.startTime)
                putLong(TimeEditFragment.EXTRA_FINISH_TIME, record.finishTime)
                putLong(TimeEditFragment.EXTRA_RECORD_ID, record.id)
                putLong(TimeEditFragment.EXTRA_LOCATION, record.location.id)
                putBoolean(TimeEditFragment.EXTRA_STOP, isTimer)
                formNavHostFragment.navController.navigate(R.id.action_puncher_to_timeEdit, this)
            }
        }
    }

    private fun deleteRecord(record: TimeRecord) {
        Timber.i("deleteRecord record=$record")

        showProgress(true)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = service.deleteTime(record.id)
                showProgressMain(false)
                if (isValidResponse(response)) {
                    val html = response.body()!!
                    processPage(html, record.date)
                } else {
                    authenticateMain()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting record: ${e.message}")
                showProgressMain(false)
                handleErrorMain(e)
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
        loadAndFetchPage(date, refresh)
        hideEditor()
    }

    @MainThread
    override fun run() {
        Timber.i("run first=$firstRun")
        loadAndFetchPage(record.date, firstRun)
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

    private fun onRecordEditSubmitted(record: TimeRecord, isLast: Boolean, responseHtml: String) {
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
                maybeFetchPage(record.date, responseHtml)
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
            maybeFetchPage(record.date, responseHtml)
        }
    }

    private fun onRecordEditDeleted(record: TimeRecord, responseHtml: String) {
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
            maybeFetchPage(record.date, responseHtml)
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
        if (view?.visibility == View.VISIBLE) {
            menuInflater.inflate(R.menu.time_list, menu)
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (view?.visibility != View.VISIBLE) {
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

    private fun findTopFormFragment(): TimeFormFragment {
        return formNavHostFragment.childFragmentManager.findFragmentByClass(TimeFormFragment::class.java)!!
    }

    private fun maybeFetchPage(date: Calendar, responseHtml: String) {
        if (responseHtml.isEmpty()) {
            fetchPage(date)
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                processPage(responseHtml, date)
            }
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