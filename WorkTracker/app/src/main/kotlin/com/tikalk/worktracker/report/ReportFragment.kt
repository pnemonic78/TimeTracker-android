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

package com.tikalk.worktracker.report

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.navigation.fragment.findNavController
import com.tikalk.app.isNavDestination
import com.tikalk.app.runOnUiThread
import com.tikalk.worktracker.R
import com.tikalk.worktracker.app.TrackerFragment
import com.tikalk.worktracker.auth.LoginFragment
import com.tikalk.worktracker.db.TimeRecordEntity
import com.tikalk.worktracker.db.TrackerDatabase
import com.tikalk.worktracker.db.toTimeRecord
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.time.ReportFilter
import com.tikalk.worktracker.model.time.TaskRecordStatus
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.model.time.TimeTotals
import com.tikalk.worktracker.net.InternetFragment
import com.tikalk.worktracker.net.TimeTrackerServiceProvider
import com.tikalk.worktracker.time.formatElapsedTime
import com.tikalk.worktracker.time.parseSystemDate
import com.tikalk.worktracker.time.parseSystemTime
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_report_list.*
import kotlinx.android.synthetic.main.time_totals.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList

class ReportFragment : InternetFragment(),
    LoginFragment.OnLoginListener {

    private val listAdapter = ReportAdapter()
    private var records: List<TimeRecord> = ArrayList()
    private var filter: ReportFilter = ReportFilter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_report_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        list.adapter = listAdapter
    }

    private fun fetchPage(filter: ReportFilter, progress: Boolean = true) {
        Timber.d("fetchPage filter=$filter")
        // Show a progress spinner, and kick off a background task to fetch the page.
        if (progress) showProgress(true)

        // Fetch from local database first.
        loadPage()
            .subscribe({
                bindList(records)

                // Fetch from remote server.
                val service = TimeTrackerServiceProvider.providePlain(context, preferences)

                service.generateReport(filter.toFields())
                    .subscribeOn(Schedulers.io())
                    .subscribe({ response ->
                        if (isValidResponse(response)) {
                            val html = response.body()!!
                            processPage(html, progress)
                        } else {
                            authenticate()
                        }
                    }, { err ->
                        Timber.e(err, "Error fetching page: ${err.message}")
                        if (progress) showProgressMain(false)
                    })
                    .addTo(disposables)
            }, { err ->
                Timber.e(err, "Error loading page: ${err.message}")
                if (progress) showProgressMain(false)
            })
            .addTo(disposables)
    }

    private fun processPage(html: String, progress: Boolean = true) {
        val records = ArrayList<TimeRecord>()
        populateList(html, records)
        this.records = records
        runOnUiThread {
            bindList(records)
            if (progress) showProgress(false)
        }
    }

    /** Populate the list. */
    private fun populateList(html: String, records: MutableList<TimeRecord>) {
        records.clear()
        val doc: Document = Jsoup.parse(html)
        var columnIndexDate = -1
        var columnIndexProject = -1
        var columnIndexTask = -1
        var columnIndexStart = -1
        var columnIndexFinish = -1
        var columnIndexNote = -1
        var columnIndexCost = -1

        // The first row of the table is the header
        val table = findRecordsTable(doc)
        if (table != null) {
            // loop through all the rows and parse each record
            val rows = table.getElementsByTag("tr")
            val size = rows.size
            val totalsRowIndex = size - 1
            if (size > 1) {
                val headerRow = rows.first()
                if (headerRow != null) {
                    val children = headerRow.children()
                    val childrenSize = children.size
                    for (col in 0 until childrenSize) {
                        val th = children[col]
                        when (th.ownText()) {
                            "Date" -> columnIndexDate = col
                            "Project" -> columnIndexProject = col
                            "Task" -> columnIndexTask = col
                            "Start" -> columnIndexStart = col
                            "Finish" -> columnIndexFinish = col
                            "Note" -> columnIndexNote = col
                            "Cost" -> columnIndexCost = col
                        }
                    }

                    val totalsBlankRowIndex = totalsRowIndex - 1
                    for (i in 1 until totalsBlankRowIndex) {
                        val tr = rows[i]
                        val record = parseRecord(tr,
                            columnIndexDate,
                            columnIndexProject,
                            columnIndexTask,
                            columnIndexStart,
                            columnIndexFinish,
                            columnIndexNote,
                            columnIndexCost)
                        if (record != null) {
                            records.add(record)
                        }
                    }
                }
            }
        }
    }

    @MainThread
    private fun bindList(records: List<TimeRecord>) {
        if (!isVisible) return
        listAdapter.submitList(records)
        if (records === this.records) {
            listAdapter.notifyDataSetChanged()
        }
        if (records.isNotEmpty()) {
            listSwitcher.displayedChild = CHILD_LIST
        } else {
            listSwitcher.displayedChild = CHILD_EMPTY
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
        if (!isNavDestination(R.id.loginFragment)) {
            val args = Bundle()
            requireFragmentManager().putFragment(args, LoginFragment.EXTRA_CALLER, this)
            args.putBoolean(LoginFragment.EXTRA_SUBMIT, submit)
            findNavController().navigate(R.id.action_reportList_to_login, args)
        }
    }

    /**
     * Find the first table whose first row has both class="tableHeader" and its label is 'Date'
     */
    private fun findRecordsTable(doc: Document): Element? {
        val body = doc.body()
        val form = body.selectFirst("form[name='reportViewForm']") ?: return null
        val td = form.selectFirst("td[class='tableHeader']") ?: return null

        val label = td.ownText()
        if (label == "Date") {
            return td.parent().parent()
        }

        return null
    }

    private fun parseRecord(row: Element,
                            columnIndexDate: Int,
                            columnIndexProject: Int,
                            columnIndexTask: Int,
                            columnIndexStart: Int,
                            columnIndexFinish: Int,
                            columnIndexNote: Int,
                            columnIndexCost: Int): TimeRecord? {
        val cols = row.getElementsByTag("td")
        val record = TimeRecord.EMPTY.copy()
        record.status = TaskRecordStatus.CURRENT

        val tdDate = cols[columnIndexDate]
        val date = parseSystemDate(tdDate.ownText()) ?: return null

        var project: Project = record.project
        if (columnIndexProject > 0) {
            val tdProject = cols[columnIndexProject]
            if (tdProject.attr("class") == "tableHeader") {
                return null
            }
            val projectName = tdProject.ownText()
            project = parseRecordProject(projectName) ?: return null
            record.project = project
        }

        if (columnIndexTask > 0) {
            val tdTask = cols[columnIndexTask]
            val taskName = tdTask.ownText()
            val task = parseRecordTask(project, taskName) ?: return null
            record.task = task
        }

        if (columnIndexStart > 0) {
            val tdStart = cols[columnIndexStart]
            val startText = tdStart.ownText()
            val start = parseRecordTime(date, startText) ?: return null
            record.start = start
        }

        if (columnIndexFinish > 0) {
            val tdFinish = cols[columnIndexFinish]
            val finishText = tdFinish.ownText()
            val finish = parseRecordTime(date, finishText) ?: return null
            record.finish = finish
        }

        if (columnIndexNote > 0) {
            val tdNote = cols[columnIndexNote]
            val noteText = tdNote.ownText()
            val note = parseRecordNote(noteText)
            record.note = note
        }

        if (columnIndexCost > 0) {
            val tdCost = cols[columnIndexCost]
            val costText = tdCost.ownText()
            val cost = parseCost(costText)
            record.cost = cost
        }

        return record
    }

    private fun parseRecordProject(name: String): Project? {
        return Project(name)
    }

    private fun parseRecordTask(project: Project, name: String): ProjectTask? {
        return ProjectTask(name)
    }

    private fun parseRecordTime(date: Calendar, text: String): Calendar? {
        return parseSystemTime(date, text)
    }

    private fun parseRecordNote(text: String): String {
        return text.trim()
    }

    private fun parseCost(cost: String): Double {
        return if (cost.isBlank()) 0.00 else cost.toDouble()
    }

    private fun loadPage(): Single<Unit> {
        return Single.fromCallable {
            val context: Context = this.context ?: return@fromCallable

            val db = TrackerDatabase.getDatabase(context)
            loadRecords(db, filter.startTime, filter.finishTime)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    private fun loadRecords(db: TrackerDatabase, start: Long, finish: Long) {
        val recordsDb = queryRecords(db, start, finish)
        records = recordsDb.map { it.toTimeRecord(null, null) }
    }

    private fun queryRecords(db: TrackerDatabase, start: Long, finish: Long): List<TimeRecordEntity> {
        val recordsDao = db.timeRecordDao()
        return recordsDao.queryByDate(start, finish)
    }

    @MainThread
    fun run() {
        Timber.v("run")
        showProgress(true)
        loadPage()
            .subscribe({
                bindList(records)
                handleArguments()
                fetchPage(filter)
                showProgress(false)
            }, { err ->
                Timber.e(err, "Error loading page: ${err.message}")
                showProgress(false)
            })
            .addTo(disposables)
    }

    private fun handleArguments() {
        val args = arguments
        if (args != null) {
            if (args.containsKey(EXTRA_FILTER)) {
                val filter = args.getParcelable<ReportFilter>(EXTRA_FILTER)
                if (filter != null) {
                    this.filter = filter
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        run()
    }

    override fun onLoginSuccess(fragment: LoginFragment, login: String) {
        run()
    }

    override fun onLoginFailure(fragment: LoginFragment, login: String, reason: String) {
    }

    companion object {
        const val EXTRA_CALLER = TrackerFragment.EXTRA_CALLER
        const val EXTRA_FILTER = "filter"

        private const val CHILD_LIST = 0
        private const val CHILD_EMPTY = 1
    }
}