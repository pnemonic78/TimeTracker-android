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
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.tikalk.app.findFragmentByClass
import com.tikalk.app.isNavDestination
import com.tikalk.app.runOnUiThread
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
import java.util.Formatter
import java.util.Locale

class TimeListFragment : TimeFormFragment(),
    TimeListAdapter.OnTimeListListener {

    private var _binding: FragmentTimeListBinding? = null
    private val binding get() = _binding!!
    private val bindingTotals get() = binding.totals

    private var datePickerDialog: DatePickerDialog? = null
    private lateinit var formNavHostFragment: NavHostFragment
    private val listAdapter = TimeListAdapter(this)
    private val totalsData = MutableLiveData<TimeTotals?>()

    private var date: Calendar = Calendar.getInstance()
    private val recordsData = MutableLiveData<List<TimeRecord>>()

    /** Is the record from the "timer" or "+" FAB? */
    private var recordForTimer = false
    private var loginAutomatic = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().addMenuProvider(this, this, Lifecycle.State.RESUMED)
        recordsData.observe(this) { records ->
            bindList(date, records)
        }
        totalsData.observe(this) { totals ->
            if (totals != null) bindTotals(totals)
        }

        timeViewModel.deleted.observe(this) { data ->
            onRecordEditDeleted(data.record, data.responseHtml)
        }
        timeViewModel.edited.observe(this) { data ->
            onRecordEditSubmitted(data.record, data.isLast, data.responseHtml)
        }
        timeViewModel.editFailure.observe(this) { data ->
            onRecordEditFailure(data.record, data.reason)
        }
        timeViewModel.favorite.observe(this) { record ->
            onRecordEditFavorited(record)
        }
    }

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
        val context: Context = view.context

        formNavHostFragment =
            childFragmentManager.findFragmentById(R.id.nav_host_form) as NavHostFragment

        binding.dateInput.setOnClickListener { pickDate() }
        binding.recordAdd.setOnClickListener { addTime() }

        binding.list.adapter = listAdapter
        val swipeDay = TimeListSwipeDay(context, object : TimeListSwipeDay.OnSwipeListener {
            override fun onSwipePreviousDay() {
                navigatePreviousDay()
            }

            override fun onSwipeNextDay() {
                navigateNextDay()
            }
        })
        binding.list.setOnTouchListener { _, event -> swipeDay.onTouchEvent(event) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onRecordClick(record: TimeRecord) {
        editRecord(record)
    }

    override fun onRecordSwipe(record: TimeRecord) {
        deleteRecord(record)
    }

    /**
     * Load and then fetch.
     */
    private fun loadAndFetchPage(date: Calendar, refresh: Boolean) {
        val dateFormatted = formatSystemDate(date)
        Timber.i("loadAndFetchPage $dateFormatted refresh=$refresh")
        this.date = date

        showProgress(true)
        lifecycleScope.launch {
            try {
                delegate.dataSource.timeListPage(date, refresh)
                    .flowOn(Dispatchers.IO)
                    .collect { page ->
                        processPageMain(page)
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
                val response = delegate.service.fetchTimes(dateFormatted)
                if (isValidResponse(response)) {
                    this@TimeListFragment.date = date
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
        delegate.dataSource.savePage(page)
    }

    private fun processPageMain(page: TimeListPage) {
        timeViewModel.projectsData.value = page.projects.sortedBy { it.name }
        recordsData.value = page.records
        var totals = totalsData.value
        if ((totals == null) || (page.totals.status == TaskRecordStatus.CURRENT)) {
            totals = page.totals
        }
        totalsData.value = totals
        setRecordValue(page.record)
    }

    private fun processPage(page: TimeListPage) {
        timeViewModel.projectsData.postValue(page.projects.sortedBy { it.name })
        recordsData.postValue(page.records)
        var totals = totalsData.value
        if ((totals == null) || (page.totals.status == TaskRecordStatus.CURRENT)) {
            totals = page.totals
        }
        totalsData.postValue(totals)
        setRecordValue(page.record)
    }

    @MainThread
    private fun bindList(date: Calendar, records: List<TimeRecord>) {
        binding.dateInput.text =
            DateUtils.formatDateTime(context, date.timeInMillis, FORMAT_DATE_BUTTON)
        listAdapter.submitList(records)
        if (records === recordsData.value) {
            listAdapter.notifyDataSetChanged()
        }
    }

    @MainThread
    private fun bindTotals(totals: TimeTotals) {
        val context = this.context ?: return
        val timeBuffer = StringBuilder(20)
        val timeFormatter = Formatter(timeBuffer, Locale.getDefault())

        if (totals.daily == TimeTotals.UNKNOWN) {
            bindingTotals.dayTotalLabel.visibility = View.INVISIBLE
            bindingTotals.dayTotal.text = null
        } else {
            bindingTotals.dayTotalLabel.visibility = View.VISIBLE
            bindingTotals.dayTotal.text =
                formatElapsedTime(context, timeFormatter, totals.daily).toString()
        }
        if (totals.weekly == TimeTotals.UNKNOWN) {
            bindingTotals.weekTotalLabel.visibility = View.INVISIBLE
            bindingTotals.weekTotal.text = null
        } else {
            timeBuffer.clear()
            bindingTotals.weekTotalLabel.visibility = View.VISIBLE
            bindingTotals.weekTotal.text =
                formatElapsedTime(context, timeFormatter, totals.weekly).toString()
        }
        if (totals.monthly == TimeTotals.UNKNOWN) {
            bindingTotals.monthTotalLabel.visibility = View.INVISIBLE
            bindingTotals.monthTotal.text = null
        } else {
            timeBuffer.clear()
            bindingTotals.monthTotalLabel.visibility = View.VISIBLE
            bindingTotals.monthTotal.text =
                formatElapsedTime(context, timeFormatter, totals.monthly).toString()
        }
        if (totals.remaining == TimeTotals.UNKNOWN) {
            bindingTotals.remainingQuotaLabel.visibility = View.INVISIBLE
            bindingTotals.remainingQuota.text = null
        } else {
            timeBuffer.clear()
            bindingTotals.remainingQuotaLabel.visibility = View.VISIBLE
            bindingTotals.remainingQuota.text =
                formatElapsedTime(context, timeFormatter, totals.remaining).toString()
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(STATE_DATE, date.timeInMillis)
        outState.putParcelable(STATE_TOTALS, totalsData.value)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        date.timeInMillis = savedInstanceState.getLong(STATE_DATE)
        totalsData.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            savedInstanceState.getParcelable(STATE_TOTALS, TimeTotals::class.java) ?: TimeTotals()
        } else {
            savedInstanceState.getParcelable(STATE_TOTALS) ?: TimeTotals()
        }
    }

    private fun pickDate() {
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
        editRecord(TimeRecord.EMPTY)
    }

    fun editRecord(record: TimeRecord, isTimer: Boolean = false) {
        Timber.i("editRecord record=$record timer=$isTimer")
        recordForTimer = isTimer

        val form = findTopFormFragment()
        if (form is TimeEditFragment) {
            form.editRecord(record, date, isStop = isTimer)
        } else {
            Timber.i("editRecord editor.currentDestination=${formNavHostFragment.navController.currentDestination?.label}")
            Bundle().apply {
                putLong(TimeEditFragment.EXTRA_DATE, date.timeInMillis)
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
                val response = delegate.service.deleteTime(record.id)
                showProgressMain(false)
                if (isValidResponse(response)) {
                    val html = response.body()!!
                    processPage(html, date)
                } else {
                    authenticateMain()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting record: ${e.message}")
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
        val cal = date
        cal.add(Calendar.DATE, 1)
        navigateDate(cal)
    }

    private fun navigatePreviousDay() {
        Timber.i("navigatePreviousDay")
        val cal = date
        cal.add(Calendar.DATE, -1)
        navigateDate(cal)
    }

    private fun navigateToday() {
        Timber.i("navigateToday")
        val today = Calendar.getInstance()
        navigateDate(today)
    }

    private fun navigateDate(date: Calendar, refresh: Boolean = true) {
        Timber.i("navigateDate $date")
        loadAndFetchPage(date, refresh)
        hideEditor()
    }

    private fun isLocaleRTL(): Boolean {
        return Locale.getDefault().language == "iw"
    }

    @MainThread
    override fun run() {
        Timber.i("run first=$firstRun")
        loadAndFetchPage(date, firstRun)
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

    override fun onStart() {
        super.onStart()
        run()
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
            if (records != null) {
                val recordsNew: MutableList<TimeRecord> = ArrayList(records)
                recordsNew.add(record)
                recordsNew.sortBy { it.startTime }
                runOnUiThread { bindList(date, recordsNew) }
            }

            if (recordForTimer) {
                Bundle().apply {
                    putString(PuncherFragment.EXTRA_ACTION, PuncherFragment.ACTION_STOP)
                    putBoolean(PuncherFragment.EXTRA_CANCEL, true)
                    showTimer(this, true)
                }
                // Refresh the list with the inserted item.
                maybeFetchPage(date, responseHtml)
                return
            }
        }

        if (isLast) {
            showTimer()
            // Refresh the list with the edited item.
            if (record.id != TikalEntity.ID_NONE) {
                val records = recordsData.value
                if (records != null) {
                    val recordsNew: MutableList<TimeRecord> = ArrayList(records)
                    val index = recordsNew.indexOfFirst { it.id == record.id }
                    if (index >= 0) {
                        recordsNew[index] = record
                        recordsNew.sortBy { it.startTime }
                        runOnUiThread { bindList(date, recordsNew) }
                    }
                }
            }
            maybeFetchPage(date, responseHtml)
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
            recordsData.value?.let { records ->
                val recordsActive = records.filter { it.status != TaskRecordStatus.DELETED }
                bindList(date, recordsActive)
            }
            maybeFetchPage(date, responseHtml)
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

    companion object {
        private const val STATE_DATE = "date"
        private const val STATE_TOTALS = "totals"

        const val ACTION_DATE = TrackerFragmentDelegate.ACTION_DATE
        const val ACTION_STOP = TrackerFragmentDelegate.ACTION_STOP
        const val ACTION_TODAY = TrackerFragmentDelegate.ACTION_TODAY

        const val EXTRA_ACTION = TrackerFragmentDelegate.EXTRA_ACTION
        const val EXTRA_DATE = TrackerFragmentDelegate.EXTRA_DATE
    }
}