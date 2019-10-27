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

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.view.*
import android.view.animation.AnimationUtils
import androidx.annotation.MainThread
import com.tikalk.app.runOnUiThread
import com.tikalk.worktracker.BuildConfig
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.LoginFragment
import com.tikalk.worktracker.db.TimeRecordEntity
import com.tikalk.worktracker.db.TrackerDatabase
import com.tikalk.worktracker.db.toTimeRecord
import com.tikalk.worktracker.db.toTimeRecordEntity
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.time.TaskRecordStatus
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.model.time.TimeTotals
import com.tikalk.worktracker.net.InternetFragment
import com.tikalk.worktracker.net.TimeTrackerServiceProvider
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_time_list.*
import kotlinx.android.synthetic.main.time_totals.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

class TimeListFragment : InternetFragment,
    TimeListAdapter.OnTimeListListener,
    LoginFragment.OnLoginListener,
    TimeEditFragment.OnEditRecordListener {

    constructor() : super()

    constructor(args: Bundle) : super(args)

    private var datePickerDialog: DatePickerDialog? = null
    private lateinit var timerFragment: TimerFragment
    private lateinit var editFragment: TimeEditFragment
    private val listAdapter = TimeListAdapter(this)
    private lateinit var gestureDetector: GestureDetector
    private var totals = TimeTotals()

    private var date: Calendar = Calendar.getInstance()
    private var record
        get() = timerFragment.record
        set(value) {
            timerFragment.record = value
        }
    private val projects
        get() = timerFragment.projects
    private val tasks
        get() = timerFragment.tasks
    private var records: List<TimeRecord> = ArrayList()
    /** Is the record from the "timer" or "+" FAB? */
    private var recordForTimer = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_time_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        timerFragment = childFragmentManager.findFragmentById(R.id.fragmentTimer) as TimerFragment
        editFragment = childFragmentManager.findFragmentById(R.id.fragmentEdit) as TimeEditFragment
        editFragment.listener = this

        switcherForm.inAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_in_form)
        switcherForm.outAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_out_form)

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

        if (savedInstanceState == null) {
            fetchPage(date, false)
        }
    }

    override fun onRecordClick(record: TimeRecord) {
        editRecord(record)
    }

    override fun onRecordSwipe(record: TimeRecord) {
        deleteRecord(record)
    }

    private var fetchingPage = false

    private fun fetchPage(date: Calendar, progress: Boolean = true) {
        val dateFormatted = formatSystemDate(date)
        Timber.d("fetchPage $dateFormatted fetching=$fetchingPage")
        if (fetchingPage) return
        fetchingPage = true
        // Show a progress spinner, and kick off a background task to perform the user login attempt.
        if (progress) showProgress(true)

        // Fetch from local database first.
        loadPage()
            .subscribe({
                populateForm(record)
                bindForm(record)
                bindList(date, records)

                // Fetch from remote server.
                val service = TimeTrackerServiceProvider.providePlain(context, preferences)

                service.fetchTimes(dateFormatted)
                    .subscribeOn(Schedulers.io())
                    .subscribe({ response ->
                        if (this.date != date) {
                            this.date.timeInMillis = date.timeInMillis
                        }
                        if (isValidResponse(response)) {
                            val html = response.body()!!
                            processPage(html, date, progress)
                        } else {
                            authenticate(true)
                        }
                        fetchingPage = false
                    }, { err ->
                        Timber.e(err, "Error fetching page: ${err.message}")
                        if (progress) showProgressMain(false)
                        fetchingPage = false
                    })
                    .addTo(disposables)
            }, { err ->
                Timber.e(err, "Error loading page: ${err.message}")
                if (progress) showProgress(false)
                fetchingPage = false
            })
            .addTo(disposables)
    }

    private fun processPage(html: String, date: Calendar, progress: Boolean = true) {
        populateForm(html)
        populateList(html)
        savePage()
        runOnUiThread {
            bindList(date, records)
            bindTotals(totals)
            if (progress) showProgressMain(false)
        }
    }

    /** Populate the list. */
    private fun populateList(html: String) {
        val records = ArrayList<TimeRecord>()
        val doc: Document = Jsoup.parse(html)

        // The first row of the table is the header
        val table = findRecordsTable(doc)
        if (table != null) {
            // loop through all the rows and parse each record
            val rows = table.getElementsByTag("tr")
            for (tr in rows) {
                val record = parseRecord(tr)
                if (record != null) {
                    records.add(record)
                }
            }
        }

        this.records = records

        val form = doc.selectFirst("form[name='timeRecordForm']")
        populateTotals(doc, form, totals)
    }

    @MainThread
    private fun bindList(date: Calendar, records: List<TimeRecord>) {
        dateInput.text = DateUtils.formatDateTime(context, date.timeInMillis, FORMAT_DATE_BUTTON)
        listAdapter.submitList(records)
        if (records === this.records) {
            listAdapter.notifyDataSetChanged()
        }
    }

    @MainThread
    private fun bindTotals(totals: TimeTotals) {
        val context: Context = requireContext()
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

    private fun authenticate(submit: Boolean = false) {
        Timber.v("authenticate submit=$submit")
        LoginFragment.show(this, submit, this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(STATE_DATE, date.timeInMillis)
        outState.putParcelable(STATE_TOTALS, totals)
        if (switcherForm != null) {
            outState.putInt(STATE_DISPLAYED_CHILD, switcherForm.displayedChild)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        date.timeInMillis = savedInstanceState.getLong(STATE_DATE)
        val totals = savedInstanceState.getParcelable<TimeTotals>(STATE_TOTALS)
        val displayedChild = savedInstanceState.getInt(STATE_DISPLAYED_CHILD, -1)

        if (totals != null) {
            this.totals = totals
            bindTotals(totals)
        }
        when (displayedChild) {
            CHILD_TIMER -> showTimer()
            CHILD_EDITOR -> showEditor()
        }
    }

    private fun pickDate() {
        if (datePickerDialog == null) {
            val cal = date
            val listener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, day)
                fetchPage(cal)
                hideEditor()
            }
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val day = cal.get(Calendar.DAY_OF_MONTH)
            datePickerDialog = DatePickerDialog(requireContext(), listener, year, month, day)
        }
        datePickerDialog!!.show()
    }

    private fun addTime() {
        editRecord(TimeRecord.EMPTY)
    }

    /**
     * Find the first table whose first row has both class="tableHeader" and labels 'Project' and 'Task' and 'Start'
     */
    private fun findRecordsTable(doc: Document): Element? {
        val body = doc.body()
        val tables = body.select("table")
        var rows: Elements
        var tr: Element
        var cols: Elements
        var td: Element
        var classAttr: String
        var label: String

        for (table in tables) {
            rows = table.getElementsByTag("tr")
            tr = rows.first()
            if (tr.childNodeSize() < 6) {
                continue
            }
            cols = tr.getElementsByTag("td")
            td = cols[0]
            classAttr = td.attr("class")
            label = td.ownText()
            if ((classAttr != "tableHeader") || (label != "Project")) {
                continue
            }
            td = cols[1]
            classAttr = td.attr("class")
            label = td.ownText()
            if ((classAttr != "tableHeader") || (label != "Task")) {
                continue
            }
            td = cols[2]
            classAttr = td.attr("class")
            label = td.ownText()
            if ((classAttr != "tableHeader") || (label != "Start")) {
                continue
            }
            return table
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

        return TimeRecord(id, project, task, start, finish, note, TaskRecordStatus.CURRENT)
    }

    private fun parseRecordProject(name: String): Project? {
        return projects.find { name == it.name }
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
        recordForTimer = timer
        editFragment.editRecord(record, date)
        showEditor()
    }

    private fun deleteRecord(record: TimeRecord) {
        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        showProgress(true)

        val context: Context = requireContext()
        val service = TimeTrackerServiceProvider.providePlain(context, preferences)

        service.deleteTime(record.id)
            .subscribeOn(Schedulers.io())
            //.observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { response ->
                    if (isValidResponse(response)) {
                        val html = response.body()!!
                        processPage(html, date)
                    } else {
                        authenticate()
                    }
                },
                { err ->
                    Timber.e(err, "Error deleting record: ${err.message}")
                    showProgressMain(false)
                }
            )
            .addTo(disposables)
    }

    private fun populateForm(html: String) {
        timerFragment.populateForm(html)
    }

    private fun populateForm(recordStarted: TimeRecord?) {
        timerFragment.populateForm(recordStarted)
    }

    private fun bindForm(record: TimeRecord) {
        timerFragment.bindForm(record)
    }

    fun stopTimer() {
        if (::timerFragment.isInitialized) {
            timerFragment.stopTimer()
        } else {
            // Save for "run" later.
            val args = arguments ?: Bundle()
            args.putString(EXTRA_ACTION, ACTION_STOP)
            if (arguments == null) {
                arguments = args
            }
        }
    }

    private fun navigateTomorrow() {
        val cal = date
        cal.add(Calendar.DATE, 1)
        fetchPage(cal)
        hideEditor()
    }

    private fun navigateYesterday() {
        val cal = date
        cal.add(Calendar.DATE, -1)
        fetchPage(cal)
        hideEditor()
    }

    private fun isLocaleRTL(): Boolean {
        return Locale.getDefault().language == "iw"
    }

    private fun populateTotals(doc: Document, parent: Element?, totals: TimeTotals) {
        totals.clear(true)
        if (parent == null) {
            return
        }

        val table = findTotalsTable(doc, parent) ?: return
        val cells = table.getElementsByTag("td")
        for (td in cells) {
            val text = td.text()
            val value: String
            when {
                text.startsWith("Day total:") -> {
                    value = text.substring(text.indexOf(':') + 1).trim()
                    totals.daily = parseHours(value) ?: TimeTotals.UNKNOWN
                }
                text.startsWith("Week total:") -> {
                    value = text.substring(text.indexOf(':') + 1).trim()
                    totals.weekly = parseHours(value) ?: TimeTotals.UNKNOWN
                }
                text.startsWith("Month total:") -> {
                    value = text.substring(text.indexOf(':') + 1).trim()
                    totals.monthly = parseHours(value) ?: TimeTotals.UNKNOWN
                }
                text.startsWith("Remaining quota:") -> {
                    value = text.substring(text.indexOf(':') + 1).trim()
                    totals.remaining = parseHours(value) ?: TimeTotals.UNKNOWN
                }
            }
        }
    }

    private fun findTotalsTable(doc: Document, parent: Element?): Element? {
        val body = doc.body()
        val tables = body.select("table")
        var rows: Elements
        var tr: Element
        var cols: Elements
        var td: Element
        var label: String

        for (table in tables) {
            if ((parent != null) && (table.parent() != parent)) {
                continue
            }
            rows = table.getElementsByTag("tr")
            tr = rows.first()
            if (tr.childNodeSize() < 1) {
                continue
            }
            cols = tr.getElementsByTag("td")
            td = cols.first()
            label = td.ownText()
            if (label.startsWith("Week total:")) {
                return table
            }
        }

        return null
    }

    private fun loadPage(): Single<Unit> {
        return Single.fromCallable {
            val context: Context = this.context ?: return@fromCallable

            if (isTimerShowing()) {
                timerFragment.loadForm()
            } else if (!recordForTimer) {
                editFragment.loadForm()
            }

            val db = TrackerDatabase.getDatabase(context)
            loadRecords(db, date)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    private fun savePage() {
        if (isTimerShowing()) {
            timerFragment.savePage()
        } else {
            editFragment.savePage()
        }

        val db = TrackerDatabase.getDatabase(requireContext())
        saveRecords(db, date)
    }

    private fun saveRecords(db: TrackerDatabase, day: Calendar? = null) {
        val records = this.records
        val recordsDao = db.timeRecordDao()
        val recordsDb = queryRecords(db, day)
        val recordsDbById: MutableMap<Long, TimeRecordEntity> = HashMap()
        for (record in recordsDb) {
            recordsDbById[record.id] = record
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

    private fun loadRecords(db: TrackerDatabase, day: Calendar? = null) {
        val recordsDb = queryRecords(db, day)
        records = recordsDb.map { it.toTimeRecord(projects, tasks) }
    }

    private fun queryRecords(db: TrackerDatabase, day: Calendar? = null): List<TimeRecordEntity> {
        val recordsDao = db.timeRecordDao()
        return if (day == null) {
            recordsDao.queryAll()
        } else {
            val cal = day.clone() as Calendar
            cal.hourOfDay = 0
            cal.minute = 0
            cal.second = 0
            cal.millis = 0
            val start = cal.timeInMillis
            cal.hourOfDay = cal.getMaximum(Calendar.HOUR_OF_DAY)
            cal.minute = cal.getMaximum(Calendar.MINUTE)
            cal.second = cal.getMaximum(Calendar.SECOND)
            cal.millis = cal.getMaximum(Calendar.MILLISECOND)
            val finish = cal.timeInMillis
            recordsDao.queryByDate(start, finish)
        }
    }

    override fun setArguments(args: Bundle?) {
        super.setArguments(args)

        if (isAdded) {
            if ((childFragmentManager.fragments.size > 0)) {
                timerFragment.arguments = arguments
                timerFragment.run()
            }
        }
    }

    @MainThread
    fun run() {
        showProgress(true)
        loadPage()
            .subscribe({
                populateForm(record)
                bindForm(record)
                bindList(date, records)
                if (projects.isEmpty() or tasks.isEmpty()) {
                    fetchPage(date)
                }

                val args = arguments
                if (args != null) {
                    if (args.containsKey(EXTRA_ACTION)) {
                        val action = args.getString(EXTRA_ACTION)
                        if (action == ACTION_STOP) {
                            args.remove(EXTRA_ACTION)
                            stopTimer()
                        }
                    }
                }

                showProgress(false)
            }, { err ->
                Timber.e(err, "Error loading page: ${err.message}")
                showProgress(false)
            })
            .addTo(disposables)
    }

    override fun onStart() {
        super.onStart()
        run()
    }

    override fun onLoginSuccess(fragment: LoginFragment, email: String) {
        Timber.i("login success")
        fragment.dismissAllowingStateLoss()
        user = preferences.user
        // Fetch the list for the user.
        fetchPage(date)
    }

    override fun onLoginFailure(fragment: LoginFragment, email: String, reason: String) {
        Timber.e("login failure: $reason")
        if (email.isEmpty() or (reason == "onCancel")) {
            activity?.finish()
        }
    }

    override fun onRecordEditSubmitted(fragment: TimeEditFragment, record: TimeRecord, last: Boolean) {
        Timber.i("record submitted: $record")
        if (record.id == TikalEntity.ID_NONE) {
            if (recordForTimer) {
                timerFragment.stopTimerCommit()
            }
        }
        if (last) {
            showTimer()
            // Refresh the list with the edited item.
            fetchPage(date)
        }
    }

    override fun onRecordEditDeleted(fragment: TimeEditFragment, record: TimeRecord) {
        Timber.i("record deleted: $record")
        showTimer()
        if (record.id == TikalEntity.ID_NONE) {
            if (recordForTimer) {
                timerFragment.stopTimerCommit()
            }
        } else {
            // Refresh the list with the edited item.
            fetchPage(date)
        }
    }

    override fun onRecordEditFavorited(fragment: TimeEditFragment, record: TimeRecord) {
        Timber.i("record favorited: ${record.project} / ${record.task}")
    }

    override fun onRecordEditFailure(fragment: TimeEditFragment, record: TimeRecord, reason: String) {
        Timber.e("record failure: $reason")
    }

    override fun onBackPressed(): Boolean {
        val child = switcherForm?.displayedChild ?: -1
        if (child == CHILD_EDITOR) {
            cancelEditRecord()
            return true
        }
        return super.onBackPressed()
    }

    private fun cancelEditRecord() {
        showTimer()
    }

    private fun showTimer() {
        val switcher = switcherForm
        if (switcher != null) {
            if (switcher.displayedChild != CHILD_TIMER) {
                switcher.displayedChild = CHILD_TIMER
            }
        }
        activity?.invalidateOptionsMenu()
    }

    private fun showEditor() {
        val switcher = switcherForm
        if (switcher != null) {
            if (switcher.displayedChild != CHILD_EDITOR) {
                switcher.displayedChild = CHILD_EDITOR
            }
        }
        activity?.invalidateOptionsMenu()
    }

    private fun hideEditor() {
        showTimer()
    }

    private fun isTimerShowing() = (switcherForm?.displayedChild == CHILD_TIMER)

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == LoginFragment.REQUEST_LOGIN) {
            if (resultCode == Activity.RESULT_OK) {
                user = preferences.user
                // Fetch the list for the user.
                fetchPage(date)
            } else {
                activity?.finish()
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        private const val STATE_DATE = "date"
        private const val STATE_TOTALS = "totals"
        private const val STATE_DISPLAYED_CHILD = "switcher.displayedChild"

        const val ACTION_STOP = BuildConfig.APPLICATION_ID + ".STOP"

        private const val EXTRA_ACTION = BuildConfig.APPLICATION_ID + ".ACTION"

        const val FORMAT_DATE_BUTTON = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY

        private const val CHILD_TIMER = 0
        private const val CHILD_EDITOR = 1
    }
}