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
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.view.*
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
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
import com.tikalk.worktracker.model.time.TaskRecordStatus
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.model.time.TimeTotals
import com.tikalk.worktracker.net.InternetFragment
import com.tikalk.worktracker.net.TimeTrackerServiceFactory
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

class TimeListFragment : InternetFragment(),
    TimeListAdapter.OnTimeListListener,
    LoginFragment.OnLoginListener {

    private var datePickerDialog: DatePickerDialog? = null
    private lateinit var timerFragment: TimerFragment
    private val listAdapter = TimeListAdapter(this)
    private lateinit var gestureDetector: GestureDetector
    private var totals = TimeTotals()

    private var date
        get() = timerFragment.date
        set(value) {
            timerFragment.date = value
        }
    private var record
        get() = timerFragment.record
        set(value) {
            timerFragment.record = value
        }
    private val projects
        get() = timerFragment.projects
    private val tasks
        get() = timerFragment.tasks
    private val records: MutableList<TimeRecord> = ArrayList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_time_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        timerFragment = childFragmentManager.findFragmentById(R.id.fragmentForm) as TimerFragment
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

    private fun fetchPage(date: Calendar) {
        val dateFormatted = formatSystemDate(date)
        Timber.d("fetchPage $dateFormatted")
        // Show a progress spinner, and kick off a background task to perform the user login attempt.
        showProgress(true)

        // Fetch from local database first.
        loadPage()
            .subscribe({
                populateForm(record)
                bindForm(record)

                // Fetch from remote server.
                val service = TimeTrackerServiceFactory.createPlain(context, preferences)

                service.fetchTimes(dateFormatted)
                    .subscribeOn(Schedulers.io())
                    .subscribe({ response ->
                        if (this.date != date) {
                            this.date.timeInMillis = date.timeInMillis
                        }
                        if (isValidResponse(response)) {
                            val body = response.body()!!
                            populateForm(body, date)
                            populateList(body, date)
                            savePage()
                            showProgressMain(false)
                        } else {
                            authenticate(true)
                        }
                    }, { err ->
                        Timber.e(err, "Error fetching page: ${err.message}")
                        showProgressMain(false)
                    })
                    .addTo(disposables)
            }, { err ->
                Timber.e(err, "Error loading page: ${err.message}")
                showProgress(false)
            })
            .addTo(disposables)
    }

    /** Populate the list. */
    private fun populateList(html: String, date: Calendar) {
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

        val form = doc.selectFirst("form[name='timeRecordForm']")
        populateTotals(doc, form, totals)

        runOnUiThread {
            bindList(date, records)
            bindTotals(totals)
        }
    }

    @MainThread
    private fun bindList(date: Calendar, records: List<TimeRecord>) {
        dateInput.text = DateUtils.formatDateTime(context, date.timeInMillis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY)

        if (records !== this.records) {
            this.records.clear()
            this.records.addAll(records)
        }
        listAdapter.submitList(records)
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

    private fun authenticate(immediate: Boolean = false) {
        val args = Bundle()
        args.putBoolean(LoginFragment.EXTRA_SUBMIT, immediate)
        val fragment = LoginFragment()
        fragment.arguments = args
        fragment.listener = this
        fragment.show(requireFragmentManager(), "login")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_EDIT -> if (resultCode == AppCompatActivity.RESULT_OK) {
                // Refresh the list with the edited item.
                fetchPage(date)
            }
            REQUEST_STOPPED -> if (resultCode == AppCompatActivity.RESULT_OK) {
                // Refresh the list with the edited item.
                fetchPage(date)
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(STATE_DATE, date.timeInMillis)
        outState.putParcelable(STATE_TOTALS, totals)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        date.timeInMillis = savedInstanceState.getLong(STATE_DATE)
        val totals = savedInstanceState.getParcelable<TimeTotals>(STATE_TOTALS)

        if (totals != null) {
            this.totals = totals
            bindTotals(totals)
        }
    }

    fun pickDate() {
        if (datePickerDialog == null) {
            val cal = date
            val listener = DatePickerDialog.OnDateSetListener { picker, year, month, day ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, day)
                fetchPage(cal)
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

        return TimeRecord(id, user, project, task, start, finish, note, TaskRecordStatus.CURRENT)
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

    private fun editRecord(record: TimeRecord, requestCode: Int = TimeListActivity.REQUEST_EDIT) {
        timerFragment.editRecord(record, requestCode)
    }

    private fun deleteRecord(record: TimeRecord) {
        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        showProgress(true)

        val context: Context = requireContext()
        val service = TimeTrackerServiceFactory.createPlain(context, preferences)

        service.deleteTime(record.id)
            .subscribeOn(Schedulers.io())
            //.observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { response ->
                    if (isValidResponse(response)) {
                        val body = response.body()!!
                        populateForm(body, date)
                        populateList(body, date)
                        savePage()
                        showProgressMain(false)
                    } else {
                        authenticate(true)
                    }
                },
                { err ->
                    Timber.e(err, "Error deleting record: ${err.message}")
                    showProgressMain(false)
                }
            )
            .addTo(disposables)
    }

    private fun populateForm(html: String, date: Calendar) {
        timerFragment.populateForm(html, date)
    }

    private fun populateForm(recordStarted: TimeRecord?) {
        timerFragment.populateForm(recordStarted)
    }

    private fun bindForm(record: TimeRecord) {
        timerFragment.bindForm(record)
    }

    fun stopTimer() {
        timerFragment.stopTimer()
    }

    private fun navigateTomorrow() {
        val cal = date
        cal.add(Calendar.DATE, 1)
        fetchPage(cal)
    }

    private fun navigateYesterday() {
        val cal = date
        cal.add(Calendar.DATE, -1)
        fetchPage(cal)
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
            timerFragment.loadForm()

            val db = TrackerDatabase.getDatabase(requireContext())
            loadRecords(db, date)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    private fun savePage() {
        timerFragment.savePage()

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
        records.clear()
        records.addAll(recordsDb.map { it.toTimeRecord(user, projects, tasks) })
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

    fun markFavorite() {
        timerFragment.markFavorite()
    }

    override fun onLoginSuccess(fragment: LoginFragment, email: String) {
        Timber.i("login success")
        fragment.dismissAllowingStateLoss()
        user = preferences.user
        record.user = user
        // Fetch the list for the user.
        fetchPage(date)
    }

    override fun onLoginFailure(fragment: LoginFragment, email: String, reason: String) {
        Timber.e("login failure: $reason")
    }

    companion object {
        const val REQUEST_EDIT = 0xED17
        const val REQUEST_STOPPED = 0x5706

        private const val STATE_DATE = "date"
        private const val STATE_TOTALS = "totals"

        const val ACTION_STOP = BuildConfig.APPLICATION_ID + ".STOP"

        const val EXTRA_PROJECT_ID = TimeEditFragment.EXTRA_PROJECT_ID
        const val EXTRA_TASK_ID = TimeEditFragment.EXTRA_TASK_ID
        const val EXTRA_START_TIME = TimeEditFragment.EXTRA_START_TIME
        const val EXTRA_FINISH_TIME = TimeEditFragment.EXTRA_FINISH_TIME
    }
}