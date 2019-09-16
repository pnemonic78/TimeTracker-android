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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.view.*
import androidx.annotation.MainThread
import com.tikalk.worktracker.BuildConfig
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.LoginActivity
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.time.TaskRecordStatus
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.model.time.TimeTotals
import com.tikalk.worktracker.net.TimeTrackerServiceFactory
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_time_list.*
import kotlinx.android.synthetic.main.progress.*
import kotlinx.android.synthetic.main.time_totals.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

class TimeListActivity : TimeFormActivity(),
    TimeListAdapter.OnTimeListListener {

    companion object {
        private const val REQUEST_AUTHENTICATE = 0x109
        const val REQUEST_EDIT = 0xED17
        const val REQUEST_STOPPED = 0x5706

        private const val STATE_DATE = "date"
        private const val STATE_RECORD = "record"
        private const val STATE_TOTALS = "totals"

        const val ACTION_STOP = BuildConfig.APPLICATION_ID + ".STOP"

        const val EXTRA_PROJECT_ID = TimeEditActivity.EXTRA_PROJECT_ID
        const val EXTRA_TASK_ID = TimeEditActivity.EXTRA_TASK_ID
        const val EXTRA_START_TIME = TimeEditActivity.EXTRA_START_TIME
        const val EXTRA_FINISH_TIME = TimeEditActivity.EXTRA_FINISH_TIME
    }

    private val context: Context = this

    // UI references
    private var datePickerDialog: DatePickerDialog? = null
    private lateinit var timerFragment: TimerFragment
    private val listAdapter = TimeListAdapter(this)
    private lateinit var gestureDetector: GestureDetector
    private var totals = TimeTotals()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up the form.
        setContentView(R.layout.activity_time_list)

        formFragment = supportFragmentManager.findFragmentById(R.id.fragment_form) as TimeFormFragment
        timerFragment = formFragment as TimerFragment
        date_input.setOnClickListener { pickDate() }
        fab_add.setOnClickListener { addTime() }

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

        handleIntent(intent, savedInstanceState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        handleIntent(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.clear()
        menuInflater.inflate(R.menu.time_list, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_date -> {
                pickDate()
                return true
            }
            R.id.menu_favorite -> {
                markFavorite()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
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
                val authToken = prefs.basicCredentials.authToken()
                val service = TimeTrackerServiceFactory.createPlain(this, authToken)

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
        date_input.text = DateUtils.formatDateTime(context, date.timeInMillis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY)

        if (records !== this.records) {
            this.records.clear()
            this.records.addAll(records)
        }
        listAdapter.submitList(records)
    }

    @MainThread
    private fun bindTotals(totals: TimeTotals) {
        val context: Context = this
        val timeBuffer = StringBuilder(20)
        val timeFormatter = Formatter(timeBuffer, Locale.getDefault())

        if (totals.daily == TimeTotals.UNKNOWN) {
            day_total_label.visibility = View.INVISIBLE
            day_total.text = null
        } else {
            day_total_label.visibility = View.VISIBLE
            day_total.text = formatElapsedTime(context, timeFormatter, totals.daily).toString()
        }
        if (totals.weekly == TimeTotals.UNKNOWN) {
            week_total_label.visibility = View.INVISIBLE
            week_total.text = null
        } else {
            timeBuffer.setLength(0)
            week_total_label.visibility = View.VISIBLE
            week_total.text = formatElapsedTime(context, timeFormatter, totals.weekly).toString()
        }
        if (totals.monthly == TimeTotals.UNKNOWN) {
            month_total_label.visibility = View.INVISIBLE
            month_total.text = null
        } else {
            timeBuffer.setLength(0)
            month_total_label.visibility = View.VISIBLE
            month_total.text = formatElapsedTime(context, timeFormatter, totals.monthly).toString()
        }
        if (totals.remaining == TimeTotals.UNKNOWN) {
            remaining_quota_label.visibility = View.INVISIBLE
            remaining_quota.text = null
        } else {
            timeBuffer.setLength(0)
            remaining_quota_label.visibility = View.VISIBLE
            remaining_quota.text = formatElapsedTime(context, timeFormatter, totals.remaining).toString()
        }
    }

    private fun authenticate(immediate: Boolean = false) {
        val intent = Intent(context, LoginActivity::class.java)
        intent.putExtra(LoginActivity.EXTRA_SUBMIT, immediate)
        startActivityForResult(intent, REQUEST_AUTHENTICATE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_AUTHENTICATE -> if (resultCode == RESULT_OK) {
                user.username = prefs.userCredentials.login
                user.email = user.username
                record.user = user
                // Fetch the list for the user.
                fetchPage(date)
            } else {
                showProgress(false)
                finish()
            }
            REQUEST_EDIT -> if (resultCode == RESULT_OK) {
                intent.action = null
                // Refresh the list with the edited item.
                fetchPage(date)
            } else {
                showProgress(false)
            }
            REQUEST_STOPPED -> if (resultCode == RESULT_OK) {
                stopTimerCommit()
                intent.action = null
                // Refresh the list with the edited item.
                fetchPage(date)
            } else {
                showProgress(false)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(STATE_DATE, date.timeInMillis)
        outState.putParcelable(STATE_RECORD, record)
        outState.putParcelable(STATE_TOTALS, totals)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        date.timeInMillis = savedInstanceState.getLong(STATE_DATE)
        val recordParcel = savedInstanceState.getParcelable<TimeRecord>(STATE_RECORD)
        val totals = savedInstanceState.getParcelable<TimeTotals>(STATE_TOTALS)

        if (recordParcel != null) {
            record.project = recordParcel.project
            record.task = recordParcel.task
            record.start = recordParcel.start
            populateForm(record)
            bindForm(record)
        }
        if (totals != null) {
            this.totals = totals
            bindTotals(totals)
        }
    }

    override fun onRecordClick(record: TimeRecord) {
        editRecord(record)
    }

    override fun onRecordSwipe(record: TimeRecord) {
        deleteRecord(record)
    }

    private fun pickDate() {
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
            datePickerDialog = DatePickerDialog(context, listener, year, month, day)
        }
        datePickerDialog!!.show()
    }

    override fun showProgress(show: Boolean) {
        val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        list.visibility = if (show) View.GONE else View.VISIBLE
        list.animate().setDuration(shortAnimTime).alpha(
            (if (show) 0 else 1).toFloat()).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                list.visibility = if (show) View.GONE else View.VISIBLE
            }
        })

        progress.visibility = if (show) View.VISIBLE else View.GONE
        progress.animate().setDuration(shortAnimTime).alpha(
            (if (show) 1 else 0).toFloat()).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                progress.visibility = if (show) View.VISIBLE else View.GONE
            }
        })

        fab_add.isEnabled = !show
    }

    private fun addTime() {
        editRecord(record)
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

    private fun editRecord(record: TimeRecord, requestId: Int = REQUEST_EDIT) {
        val intent = Intent(context, TimeEditActivity::class.java)
        intent.putExtra(TimeEditActivity.EXTRA_DATE, date.timeInMillis)
        if (record.id == TikalEntity.ID_NONE) {
            intent.putExtra(TimeEditActivity.EXTRA_PROJECT_ID, record.project.id)
            intent.putExtra(TimeEditActivity.EXTRA_TASK_ID, record.task.id)
            intent.putExtra(TimeEditActivity.EXTRA_START_TIME, record.startTime)
            intent.putExtra(TimeEditActivity.EXTRA_FINISH_TIME, record.finishTime)
        } else {
            intent.putExtra(TimeEditActivity.EXTRA_RECORD, record.id)
        }
        startActivityForResult(intent, requestId)
    }

    private fun deleteRecord(record: TimeRecord) {
        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        showProgress(true)

        val authToken = prefs.basicCredentials.authToken()
        val service = TimeTrackerServiceFactory.createPlain(this, authToken)

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
        formFragment.populateForm(html, date)
    }

    private fun populateForm(recordStarted: TimeRecord?) {
        timerFragment.populateForm(recordStarted)
    }

    private fun bindForm(record: TimeRecord) {
        formFragment.bindForm(record)
    }

    private fun stopTimerCommit() {
        timerFragment.stopTimerCommit()
    }

    private fun handleIntent(intent: Intent, savedInstanceState: Bundle? = null) {
        timerFragment.later(if (intent.action == ACTION_STOP) intent else null)

        if (savedInstanceState == null) {
            fetchPage(date)
        } else {
            date.timeInMillis = savedInstanceState.getLong(STATE_DATE, date.timeInMillis)
            loadPage()
                .subscribe({
                    populateForm(record)
                    bindForm(record)
                    bindList(date, records)
                    showProgress(false)
                }, { err ->
                    Timber.e(err, "Error loading page: ${err.message}")
                    showProgress(false)
                })
                .addTo(disposables)
        }
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
        return Single.fromCallable { loadFormFromDb() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    private fun savePage() {
        return formFragment.saveFormToDb()
    }
}