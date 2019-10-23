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

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.MainThread
import com.tikalk.app.runOnUiThread
import com.tikalk.worktracker.R
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.isNullOrEmpty
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.time.work.TimerWorker
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_timer.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.math.max

class TimerFragment : TimeFormFragment {

    constructor() : super()

    constructor(args: Bundle) : super(args)

    private var timer: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_timer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        projectInput.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(adapterView: AdapterView<*>) {
                projectItemSelected(projectEmpty)
            }

            override fun onItemSelected(adapterView: AdapterView<*>, view: View?, position: Int, id: Long) {
                val project = adapterView.adapter.getItem(position) as Project
                projectItemSelected(project)
            }
        }
        taskInput.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(adapterView: AdapterView<*>) {
                taskItemSelected(taskEmpty)
            }

            override fun onItemSelected(adapterView: AdapterView<*>, view: View?, position: Int, id: Long) {
                val task = adapterView.adapter.getItem(position) as ProjectTask
                taskItemSelected(task)
            }
        }

        actionStart.setOnClickListener { startTimer() }
        actionStop.setOnClickListener { stopTimer() }
    }

    @MainThread
    override fun bindForm(record: TimeRecord) {
        Timber.v("bindForm record=$record")
        val context: Context = requireContext()
        projectInput.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, projects.toTypedArray())
        if (projects.isNotEmpty()) {
            projectInput.setSelection(max(0, findProject(projects, record.project)))
        }
        taskInput.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, tasks.toTypedArray())
        if (tasks.isNotEmpty()) {
            taskInput.setSelection(max(0, findTask(tasks, record.task)))
        }
        projectInput.requestFocus()

        val startTime = record.startTime
        if (startTime <= 0L) {
            projectInput.isEnabled = true
            taskInput.isEnabled = true
            actionSwitcher.displayedChild = 0
            activity?.invalidateOptionsMenu()
        } else {
            projectInput.isEnabled = false
            taskInput.isEnabled = false
            actionSwitcher.displayedChild = 1
            activity?.invalidateOptionsMenu()

            maybeStartTimer()
        }
    }

    private fun startTimer() {
        Timber.v("startTimer")
        val context: Context = requireContext()
        val now = System.currentTimeMillis()
        record.startTime = now

        TimerWorker.startTimer(context, record)

        bindForm(record)
    }

    fun stopTimer() {
        Timber.v("stopTimer")
        val recordStarted = getStartedRecord()
        Timber.v("stopTimer recordStarted=$recordStarted")
        if (recordStarted != null) {
            record = recordStarted
        }
        if (record.finishTime <= 0L) {
            record.finishTime = System.currentTimeMillis()
        }

        editRecord(record)
    }

    fun stopTimerCommit() {
        Timber.v("stopTimerCommit")
        timer?.dispose()

        record.start = null
        record.finish = null
        preferences.stopRecord()
        val args = arguments
        if (args != null) {
            args.remove(EXTRA_PROJECT_ID)
            args.remove(EXTRA_TASK_ID)
            args.remove(EXTRA_START_TIME)
            args.remove(EXTRA_FINISH_TIME)
        }

        bindForm(record)
    }

    private fun filterTasks(project: Project) {
        val context: Context = requireContext()
        val filtered = project.tasks
        val options = ArrayList<ProjectTask>(filtered.size + 1)
        options.add(taskEmpty)
        options.addAll(filtered)
        taskInput.adapter = ArrayAdapter<ProjectTask>(context, android.R.layout.simple_list_item_1, options)
        taskInput.setSelection(findTask(options, record.task))
    }

    private fun maybeStartTimer() {
        val timer = this.timer
        if ((timer == null) || timer.isDisposed) {
            this.timer = Observable.interval(1L, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { updateTimer() }
                .addTo(disposables)
        }
        updateTimer()
    }

    private fun updateTimer() {
        val now = System.currentTimeMillis()
        val elapsedSeconds = (now - record.startTime) / DateUtils.SECOND_IN_MILLIS
        timerText?.text = DateUtils.formatElapsedTime(elapsedSeconds)
    }

    private fun projectItemSelected(project: Project) {
        record.project = project
        filterTasks(project)
        actionStart.isEnabled = (record.project.id > TikalEntity.ID_NONE) && (record.task.id > TikalEntity.ID_NONE)
    }

    private fun taskItemSelected(task: ProjectTask) {
        record.task = task
        actionStart.isEnabled = (record.project.id > TikalEntity.ID_NONE) && (record.task.id > TikalEntity.ID_NONE)
    }

    private fun getStartedRecord(args: Bundle? = arguments): TimeRecord? {
        val started = preferences.getStartedRecord()
        if (started != null) {
            return started
        }

        if (args != null) {
            if (args.containsKey(EXTRA_PROJECT_ID) and args.containsKey(EXTRA_TASK_ID)) {
                val projectId = args.getLong(EXTRA_PROJECT_ID)
                val taskId = args.getLong(EXTRA_TASK_ID)
                val startTime = args.getLong(EXTRA_START_TIME)
                val finishTime = args.getLong(EXTRA_FINISH_TIME, System.currentTimeMillis())

                val project = projects.firstOrNull { it.id == projectId } ?: projectEmpty
                val task = tasks.firstOrNull { it.id == taskId } ?: taskEmpty

                val record = TimeRecord(TikalEntity.ID_NONE, project, task)
                if (startTime > 0L) {
                    record.startTime = startTime
                }
                if (finishTime > 0L) {
                    record.finishTime = finishTime
                }
                return record
            }
        }

        return null
    }

    /** Populate the record and then bind the form. */
    fun populateForm(html: String, date: Calendar) {
        val doc: Document = Jsoup.parse(html)

        val form = doc.selectFirst("form[name='timeRecordForm']") ?: return

        val inputProjects = form.selectFirst("select[name='project']") ?: return
        populateProjects(inputProjects, projects)

        val inputTasks = form.selectFirst("select[name='task']") ?: return
        populateTasks(inputTasks, tasks)

        record.project = findSelectedProject(inputProjects, projects)
        record.task = findSelectedTask(inputTasks, tasks)

        populateTaskIds(doc, projects)

        val recordStarted = getStartedRecord()
        populateForm(recordStarted)
        runOnUiThread { bindForm(record) }
    }

    fun populateForm(recordStarted: TimeRecord?) {
        Timber.v("populateForm $recordStarted")
        // In case `this.projects` is modified while we loop through it.
        val projects = this.projects.toList()
        // In case `this.tasks` is modified while we loop through it.
        val tasks = this.tasks.toList()
        if ((recordStarted == null) or (recordStarted?.project.isNullOrEmpty() and recordStarted?.task.isNullOrEmpty())) {
            val projectFavorite = preferences.getFavoriteProject()
            if (projectFavorite != TikalEntity.ID_NONE) {
                record.project = projects.firstOrNull { it.id == projectFavorite } ?: record.project
            }
            val taskFavorite = preferences.getFavoriteTask()
            if (taskFavorite != TikalEntity.ID_NONE) {
                record.task = tasks.firstOrNull { it.id == taskFavorite } ?: record.task
            }
        } else if (recordStarted != null) {
            val recordStartedProjectId = recordStarted.project.id
            val recordStartedTaskId = recordStarted.task.id
            record.project = projects.firstOrNull { it.id == recordStartedProjectId }
                ?: projectEmpty
            record.task = tasks.firstOrNull { it.id == recordStartedTaskId } ?: taskEmpty
            record.start = recordStarted.start
        }
    }

    private fun editRecord(record: TimeRecord) {
        if (parentFragment is TimeListFragment) {
            (parentFragment as TimeListFragment).editRecord(record, true)
        } else {
            val intent = Intent(context, TimeEditActivity::class.java)
            if (record.id == TikalEntity.ID_NONE) {
                intent.putExtra(TimeEditFragment.EXTRA_PROJECT_ID, record.project.id)
                intent.putExtra(TimeEditFragment.EXTRA_TASK_ID, record.task.id)
                intent.putExtra(TimeEditFragment.EXTRA_START_TIME, record.startTime)
                intent.putExtra(TimeEditFragment.EXTRA_FINISH_TIME, record.finishTime)
            } else {
                intent.putExtra(TimeEditFragment.EXTRA_RECORD, record.id)
            }
            startActivityForResult(intent, REQUEST_EDIT)
        }
    }

    fun run() {
        val recordStarted = getStartedRecord()
        populateForm(recordStarted)
        bindForm(record)
    }

    override fun onStart() {
        super.onStart()
        run()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(STATE_RECORD, record)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val recordParcel = savedInstanceState.getParcelable<TimeRecord>(STATE_RECORD)

        if (recordParcel != null) {
            record.project = recordParcel.project
            record.task = recordParcel.task
            record.start = recordParcel.start
            populateForm(record)
            bindForm(record)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (view?.visibility == View.VISIBLE) {
            inflater.inflate(R.menu.timer, menu)
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (view?.visibility != View.VISIBLE) {
            return false
        }
        when (item.itemId) {
            R.id.menu_favorite -> {
                markFavorite()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_EDIT) {
            if (resultCode == RESULT_OK) {
                TODO("record submitted")
            } else {
                TODO("record edit cancelled")
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        const val EXTRA_PROJECT_ID = TimeFormFragment.EXTRA_PROJECT_ID
        const val EXTRA_TASK_ID = TimeFormFragment.EXTRA_TASK_ID
        const val EXTRA_START_TIME = TimeFormFragment.EXTRA_START_TIME
        const val EXTRA_FINISH_TIME = TimeFormFragment.EXTRA_FINISH_TIME

        private const val REQUEST_EDIT = 0xED17
    }
}