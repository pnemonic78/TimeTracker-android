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

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.view.*
import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.tikalk.app.findFragmentByClass
import com.tikalk.app.isNavDestination
import com.tikalk.app.runOnUiThread
import com.tikalk.html.findParentElement
import com.tikalk.worktracker.R
import com.tikalk.worktracker.app.TrackerFragment
import com.tikalk.worktracker.auth.LoginFragment
import com.tikalk.worktracker.data.remote.TimeListPageParser
import com.tikalk.worktracker.db.TimeRecordEntity
import com.tikalk.worktracker.db.TrackerDatabase
import com.tikalk.worktracker.db.WholeTimeRecordEntity
import com.tikalk.worktracker.db.toTimeRecordEntity
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.time.TaskRecordStatus
import com.tikalk.worktracker.model.time.TimeListPage
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.model.time.TimeTotals
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_time_list.*
import kotlinx.android.synthetic.main.time_totals.*
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

class TimeListFragment : TimeFormFragment(),
    TimeListAdapter.OnTimeListListener,
    TimeEditFragment.OnEditRecordListener {

    private var datePickerDialog: DatePickerDialog? = null
    private lateinit var formNavHostFragment: NavHostFragment
    private val listAdapter = TimeListAdapter(this)
    private lateinit var gestureDetector: GestureDetector
    private val totalsData = MutableLiveData<TimeTotals>()

    private var date: Calendar = Calendar.getInstance()
    private val recordsData = MutableLiveData<List<TimeRecord>>()
    /** Is the record from the "timer" or "+" FAB? */
    private var recordForTimer = false
    private var loginAutomatic = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        recordsData.observe(this, Observer<List<TimeRecord>> { records ->
            bindList(date, records)
        })
        totalsData.observe(this, Observer<TimeTotals> { totals ->
            bindTotals(totals)
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_time_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        formNavHostFragment = childFragmentManager.findFragmentById(R.id.nav_host_form) as NavHostFragment

        dateInput.setOnClickListener { pickDate() }
        recordAdd.setOnClickListener { addTime() }

        list.adapter = listAdapter
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
                val vx = abs(velocityX)
                val vy = abs(velocityY)
                if ((vx > vy) && (vx > 500)) {
                    if (velocityX < 0) {    // Fling from right to left.
                        if (isLocaleRTL()) {
                            navigateYesterday()
                        } else {
                            navigateTomorrow()
                        }
                    } else {
                        if (isLocaleRTL()) {
                            navigateTomorrow()
                        } else {
                            navigateYesterday()
                        }
                    }
                    return true
                }
                return super.onFling(e1, e2, velocityX, velocityY)
            }
        })
        list.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }
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

        dataSource.timeListPage(date, refresh)
            .subscribeOn(Schedulers.io())
            .doOnSubscribe { showProgressMain(true) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ page ->
                processPageMain(page)
                handleArguments()
                showProgress(false)
            }, { err ->
                Timber.e(err, "Error loading page: ${err.message}")
                showProgress(false)
                handleError(err)
            })
            .addTo(disposables)
    }

    private var fetchingPage = false

    /**
     * Fetch from remote server.
     */
    private fun fetchPage(date: Calendar) {
        val dateFormatted = formatSystemDate(date)
        Timber.i("fetchPage $dateFormatted fetching=$fetchingPage")
        if (fetchingPage) return
        fetchingPage = true

        service.fetchTimes(dateFormatted)
            .subscribeOn(Schedulers.io())
            .subscribe({ response ->
                if (isValidResponse(response)) {
                    this.date = date
                    val html = response.body()!!
                    processPage(html, date)
                } else {
                    authenticateMain(loginAutomatic)
                }
                fetchingPage = false
            }, { err ->
                Timber.e(err, "Error fetching page: ${err.message}")
                handleErrorMain(err)
                fetchingPage = false
            })
            .addTo(disposables)
    }

    private fun processPage(html: String, date: Calendar) {
        Timber.i("processPage ${formatSystemDate(date)}")
        val page = TimeListPageParser().parse(html)
        processPage(page)
    }

    private fun processPageMain(page: TimeListPage) {
        projectsData.value = page.projects
        recordsData.value = page.records
        if ((totalsData.value == null) or (page.totals.status == TaskRecordStatus.CURRENT)) {
            totalsData.value = page.totals
        }
        setRecordValue(page.record)
    }

    private fun processPage(page: TimeListPage) {
        projectsData.postValue(page.projects)
        recordsData.postValue(page.records)
        totalsData.postValue(page.totals)
        setRecordValue(page.record)
    }

    @MainThread
    private fun bindList(date: Calendar, records: List<TimeRecord>) {
        dateInput.text = DateUtils.formatDateTime(context, date.timeInMillis, FORMAT_DATE_BUTTON)
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
            dayTotalLabel.visibility = View.INVISIBLE
            dayTotal.text = null
        } else {
            dayTotalLabel.visibility = View.VISIBLE
            dayTotal.text = formatElapsedTime(context, timeFormatter, totals.daily).toString()
        }
        if (totals.weekly == TimeTotals.UNKNOWN) {
            weekTotalLabel.visibility = View.INVISIBLE
            weekTotal.text = null
        } else {
            timeBuffer.setLength(0)
            weekTotalLabel.visibility = View.VISIBLE
            weekTotal.text = formatElapsedTime(context, timeFormatter, totals.weekly).toString()
        }
        if (totals.monthly == TimeTotals.UNKNOWN) {
            monthTotalLabel.visibility = View.INVISIBLE
            monthTotal.text = null
        } else {
            timeBuffer.setLength(0)
            monthTotalLabel.visibility = View.VISIBLE
            monthTotal.text = formatElapsedTime(context, timeFormatter, totals.monthly).toString()
        }
        if (totals.remaining == TimeTotals.UNKNOWN) {
            remainingQuotaLabel.visibility = View.INVISIBLE
            remainingQuota.text = null
        } else {
            timeBuffer.setLength(0)
            remainingQuotaLabel.visibility = View.VISIBLE
            remainingQuota.text = formatElapsedTime(context, timeFormatter, totals.remaining).toString()
        }
    }

    override fun authenticate(submit: Boolean) {
        Timber.i("authenticate submit=$submit currentDestination=${findNavController().currentDestination?.label}")
        if (!isNavDestination(R.id.loginFragment)) {
            val args = Bundle()
            parentFragmentManager.putFragment(args, LoginFragment.EXTRA_CALLER, this)
            args.putBoolean(LoginFragment.EXTRA_SUBMIT, submit)
            findNavController().navigate(R.id.action_timeList_to_login, args)
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
        totalsData.value = savedInstanceState.getParcelable(STATE_TOTALS) ?: TimeTotals()
    }

    private fun pickDate() {
        val cal = date
        val year = cal.year
        val month = cal.month
        val dayOfMonth = cal.dayOfMonth
        var picker = datePickerDialog
        if (picker == null) {
            val listener = DatePickerDialog.OnDateSetListener { _, pickedYear, pickedMonth, pickedDayOfMonth ->
                cal.year = pickedYear
                cal.month = pickedMonth
                cal.dayOfMonth = pickedDayOfMonth
                loadAndFetchPage(cal, true)
                hideEditor()
            }
            picker = DatePickerDialog(requireContext(), listener, year, month, dayOfMonth)
            datePickerDialog = picker
        } else {
            picker.updateDate(year, month, dayOfMonth)
        }
        picker.show()
    }

    private fun addTime() {
        editRecord(TimeRecord.EMPTY)
    }

    /**
     * Find the first table whose first row has both class="tableHeader" and labels 'Project' and 'Task' and 'Start'
     */
    private fun findRecordsTable(doc: Document): Element? {
        val body = doc.body()
        val candidates = body.select("td[class='tableHeader']")
        var td: Element
        var label: String

        for (candidate in candidates) {
            td = candidate
            label = td.ownText()
            if (label != "Project") {
                continue
            }
            td = td.nextElementSibling() ?: continue
            label = td.ownText()
            if (label != "Task") {
                continue
            }
            td = td.nextElementSibling() ?: continue
            label = td.ownText()
            if (label != "Start") {
                continue
            }
            return findParentElement(td, "table")
        }

        return null
    }

    private fun parseRecord(row: Element): TimeRecord? {
        val cols = row.getElementsByTag("td")

        val tdProject = cols[0]
        if (tdProject.attr("class") == "tableHeader") {
            return null
        }
        val projectName = tdProject.ownText()
        val project = parseRecordProject(projectName) ?: return null

        val tdTask = cols[1]
        val taskName = tdTask.ownText()
        val task = parseRecordTask(project, taskName) ?: return null

        val tdStart = cols[2]
        val startText = tdStart.ownText()
        val start = parseRecordTime(startText) ?: return null

        val tdFinish = cols[3]
        val finishText = tdFinish.ownText()
        val finish = parseRecordTime(finishText) ?: return null

        val tdNote = cols[5]
        val noteText = tdNote.text()
        val note = parseRecordNote(noteText)

        val tdEdit = cols[6]
        val editLink = tdEdit.child(0).attr("href")
        val id = parseRecordId(editLink)

        return TimeRecord(id, project, task, start, finish, note, 0.0, TaskRecordStatus.CURRENT)
    }

    private fun parseRecordProject(name: String): Project? {
        val projects = projectsData.value
        return projects?.find { name == it.name }
    }

    private fun parseRecordTask(project: Project, name: String): ProjectTask? {
        return project.tasks.find { task -> (task.name == name) }
    }

    private fun parseRecordTime(text: String): Calendar? {
        return parseSystemTime(date, text)
    }

    private fun parseRecordNote(text: String): String {
        return text.trim()
    }

    private fun parseRecordId(link: String): Long {
        val uri = Uri.parse(link)
        val id = uri.getQueryParameter("id")!!
        return id.toLong()
    }

    fun editRecord(record: TimeRecord, timer: Boolean = false) {
        Timber.i("editRecord record=$record timer=$timer")
        recordForTimer = timer

        val form = findTopFormFragment()
        if (form is TimeEditFragment) {
            form.listener = this
            form.editRecord(record, date)
        } else {
            Timber.i("editRecord editor.currentDestination=${formNavHostFragment.navController.currentDestination?.label}")
            val args = Bundle()
            args.putLong(TimeEditFragment.EXTRA_DATE, date.timeInMillis)
            args.putLong(TimeEditFragment.EXTRA_PROJECT_ID, record.project.id)
            args.putLong(TimeEditFragment.EXTRA_TASK_ID, record.task.id)
            args.putLong(TimeEditFragment.EXTRA_START_TIME, record.startTime)
            args.putLong(TimeEditFragment.EXTRA_FINISH_TIME, record.finishTime)
            args.putLong(TimeEditFragment.EXTRA_RECORD_ID, record.id)
            parentFragmentManager.putFragment(args, TimeEditFragment.EXTRA_CALLER, this)
            formNavHostFragment.navController.navigate(R.id.action_timer_to_timeEdit, args)
        }
    }

    private fun deleteRecord(record: TimeRecord) {
        Timber.i("deleteRecord record=$record")

        service.deleteTime(record.id)
            .subscribeOn(Schedulers.io())
            .doOnSubscribe { showProgressMain(true) }
            .doAfterTerminate { showProgressMain(false) }
            .subscribe({ response ->
                if (isValidResponse(response)) {
                    val html = response.body()!!
                    processPage(html, date)
                } else {
                    authenticateMain()
                }
            }, { err ->
                Timber.e(err, "Error deleting record: ${err.message}")
                handleErrorMain(err)
            })
            .addTo(disposables)
    }

    override fun populateForm(record: TimeRecord) {
    }

    @MainThread
    override fun bindForm(record: TimeRecord) {
    }

    fun stopTimer() {
        Timber.i("stopTimer")
        val form = findTopFormFragment()
        if (form is TimerFragment) {
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

    private fun navigateTomorrow() {
        Timber.i("navigateTomorrow")
        val cal = date
        cal.add(Calendar.DATE, 1)
        loadAndFetchPage(cal, true)
        hideEditor()
    }

    private fun navigateYesterday() {
        Timber.i("navigateYesterday")
        val cal = date
        cal.add(Calendar.DATE, -1)
        loadAndFetchPage(cal, true)
        hideEditor()
    }

    private fun isLocaleRTL(): Boolean {
        return Locale.getDefault().language == "iw"
    }

    private fun saveRecords(db: TrackerDatabase, day: Calendar? = null, records: List<TimeRecord>) {
        Timber.i("saveRecords ${formatSystemDate(day)}")
        val recordsDao = db.timeRecordDao()
        val recordsDb = queryRecords(db, day)
        val recordsDbById: MutableMap<Long, TimeRecordEntity> = HashMap()
        for (entity in recordsDb) {
            recordsDbById[entity.record.id] = entity.record
        }

        val recordsToInsert = ArrayList<TimeRecord>()
        val recordsToUpdate = ArrayList<TimeRecord>()
        //var recordDb: TimeRecordEntity
        for (record in records) {
            val recordId = record.id
            if (recordsDbById.containsKey(recordId)) {
                //recordDb = recordsDbById[recordId]!!
                //record.dbId = recordDb.dbId
                recordsToUpdate.add(record)
            } else {
                recordsToInsert.add(record)
            }
            recordsDbById.remove(recordId)
        }

        val recordsToDelete = recordsDbById.values
        recordsDao.delete(recordsToDelete)

        val recordIds = recordsDao.insert(recordsToInsert.map { it.toTimeRecordEntity() })
        //for (i in recordIds.indices) {
        //    recordsToInsert[i].dbId = recordIds[i]
        //}

        recordsDao.update(recordsToUpdate.map { it.toTimeRecordEntity() })
    }

    private fun queryRecords(db: TrackerDatabase, day: Calendar? = null): List<WholeTimeRecordEntity> {
        val recordsDao = db.timeRecordDao()
        return if (day == null) {
            recordsDao.queryAll()
        } else {
            val start = day.copy()
            start.setToStartOfDay()
            val finish = day.copy()
            finish.setToEndOfDay()
            recordsDao.queryByDate(start.timeInMillis, finish.timeInMillis)
        }
    }

    @MainThread
    fun run() {
        Timber.i("run first=$firstRun")
        loadAndFetchPage(date, firstRun)
    }

    private fun handleArguments() {
        val args = arguments
        if (args != null) {
            if (args.containsKey(EXTRA_ACTION)) {
                val action = args.getString(EXTRA_ACTION)
                if (action == ACTION_STOP) {
                    stopTimer()
                    args.remove(EXTRA_ACTION)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        run()
    }

    override fun onLoginSuccess(fragment: LoginFragment, login: String) {
        super.onLoginSuccess(fragment, login)
        run()
    }

    override fun onLoginFailure(fragment: LoginFragment, login: String, reason: String) {
        super.onLoginFailure(fragment, login, reason)
        loginAutomatic = false
        if (login.isEmpty() or (reason == "onCancel")) {
            activity?.finish()
        }
    }

    override fun onRecordEditSubmitted(fragment: TimeEditFragment, record: TimeRecord, last: Boolean, responseHtml: String) {
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
                val args = Bundle()
                args.putString(TimerFragment.EXTRA_ACTION, TimerFragment.ACTION_STOP)
                args.putBoolean(TimerFragment.EXTRA_COMMIT, true)
                parentFragmentManager.putFragment(args, TimerFragment.EXTRA_CALLER, this)
                showTimer(args, true)
                // Refresh the list with the inserted item.
                maybeFetchPage(date, responseHtml)
                return
            }
        }

        if (last) {
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

    override fun onRecordEditDeleted(fragment: TimeEditFragment, record: TimeRecord, responseHtml: String) {
        Timber.i("record deleted: $record")
        if (record.id == TikalEntity.ID_NONE) {
            if (recordForTimer) {
                val args = Bundle()
                args.putString(TimerFragment.EXTRA_ACTION, TimerFragment.ACTION_STOP)
                args.putBoolean(TimerFragment.EXTRA_COMMIT, true)
                parentFragmentManager.putFragment(args, TimerFragment.EXTRA_CALLER, this)
                showTimer(args, true)
            } else {
                showTimer()
            }
        } else {
            showTimer()
            // Refresh the list with the deleted item.
            val records = recordsData.value
            if (records != null) {
                val index = records.indexOf(record)
                if (index >= 0) {
                    val recordsNew: MutableList<TimeRecord> = ArrayList(records)
                    recordsNew.removeAt(index)
                    bindList(date, recordsNew)
                }
            }
            maybeFetchPage(date, responseHtml)
        }
    }

    override fun onRecordEditFavorited(fragment: TimeEditFragment, record: TimeRecord) {
        Timber.i("record favorited: ${record.project} / ${record.task}")
    }

    override fun onRecordEditFailure(fragment: TimeEditFragment, record: TimeRecord, reason: String) {
        Timber.e("record failure: $reason")
    }

    override fun onBackPressed(): Boolean {
        if (formNavHostFragment.navController.popBackStack()) {
            return true
        }
        return super.onBackPressed()
    }

    private fun showTimer(args: Bundle? = null, popInclusive: Boolean = false) {
        Timber.i("showTimer timer.currentDestination=${formNavHostFragment.navController.currentDestination?.label}")
        formNavHostFragment.navController.popBackStack(R.id.timerFragment, popInclusive)
        if (popInclusive) {
            formNavHostFragment.navController.navigate(R.id.timerFragment, args)
        }
    }

    private fun hideEditor() {
        showTimer()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (view?.visibility == View.VISIBLE) {
            inflater.inflate(R.menu.time_list, menu)
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (view?.visibility != View.VISIBLE) {
            return false
        }
        when (item.itemId) {
            R.id.menu_date -> {
                pickDate()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun findTopFormFragment(): TimeFormFragment {
        return formNavHostFragment.childFragmentManager.findFragmentByClass(TimeFormFragment::class.java)!!
    }

    private fun maybeFetchPage(date: Calendar, responseHtml: String) {
        if (responseHtml.isEmpty()) {
            fetchPage(date)
        } else {
            Single.just(responseHtml)
                .subscribeOn(Schedulers.io())
                .subscribe { html ->
                    processPage(html, date)
                }
                .addTo(disposables)
        }
    }

    companion object {
        private const val STATE_DATE = "date"
        private const val STATE_TOTALS = "totals"

        const val ACTION_STOP = TrackerFragment.ACTION_STOP

        const val EXTRA_ACTION = TrackerFragment.EXTRA_ACTION
    }
}