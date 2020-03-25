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
import androidx.navigation.fragment.findNavController
import com.tikalk.app.findParentFragment
import com.tikalk.worktracker.BuildConfig
import com.tikalk.worktracker.R
import com.tikalk.worktracker.app.TrackerFragment
import com.tikalk.worktracker.db.TimeRecordEntity
import com.tikalk.worktracker.db.toTimeRecord
import com.tikalk.worktracker.db.toTimeRecordEntity
import com.tikalk.worktracker.model.*
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.model.time.TimerPage
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_timer.*
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.math.max

class TimerFragment : TimeFormFragment() {

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
        Timber.i("bindForm record=$record")
        val context = this.context ?: return
        if (!isVisible) return

        // Populate the tasks spinner before projects so that it can be filtered.
        val taskItems = arrayOf(taskEmpty)
        taskInput.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, taskItems)

        val projects = projectsData.value
        bindProjects(context, record, projects)

        val startTime = record.startTime
        if (startTime <= TimeRecord.NEVER) {
            projectInput.isEnabled = true
            taskInput.isEnabled = true
            actionSwitcher.displayedChild = CHILD_START
            activity?.invalidateOptionsMenu()
        } else {
            projectInput.isEnabled = false
            taskInput.isEnabled = false
            actionSwitcher.displayedChild = CHILD_STOP
            activity?.invalidateOptionsMenu()

            maybeStartTimer()
        }
    }

    private fun bindProjects(context: Context, record: TimeRecord, projects: List<Project>?) {
        Timber.i("bindProjects record=$record projects=$projects")
        val projectItems = projects?.sortedBy { it.name }?.toTypedArray() ?: emptyArray()
        if (projectInput == null) return
        projectInput.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, projectItems)
        if (projectItems.isNotEmpty()) {
            projectInput.setSelection(max(0, findProject(projectItems, record.project)))
            projectItemSelected(record.project)
        }
        projectInput.requestFocus()
    }

    private fun startTimer() {
        Timber.i("startTimer")
        val context = this.context ?: return
        val now = System.currentTimeMillis()
        record.startTime = now

        TimerWorker.startTimer(context, record)

        bindForm(record)
    }

    fun stopTimer() {
        Timber.i("stopTimer")
        if (!isVisible or !isResumed) {
            // Save for "run" later.
            val args = arguments ?: Bundle()
            args.putString(EXTRA_ACTION, ACTION_STOP)
            if (arguments == null) {
                arguments = args
            }
            return
        }

        val recordStarted = getStartedRecord()
        Timber.i("stopTimer recordStarted=$recordStarted")
        if (recordStarted != null) {
            setRecordValue(recordStarted)
        }
        if (record.finishTime <= TimeRecord.NEVER) {
            record.finishTime = System.currentTimeMillis()
        }

        editRecord(record)
    }

    private fun stopTimerCommit() {
        Timber.i("stopTimerCommit")
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
        Timber.d("filterTasks project=$project")
        val context = this.context ?: return
        val filtered = project.tasks
        val options = ArrayList<ProjectTask>(filtered.size + 1)
        options.add(taskEmpty)
        options.addAll(filtered)
        if (taskInput == null) return
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
        Timber.d("projectItemSelected project=$project")
        setRecordProject(project)
        if (!isVisible) return
        filterTasks(project)
        actionStart.isEnabled = (record.project.id > TikalEntity.ID_NONE) && (record.task.id > TikalEntity.ID_NONE)
    }

    private fun taskItemSelected(task: ProjectTask) {
        Timber.d("taskItemSelected task=$task")
        setRecordTask(task)
        if (!isVisible) return
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

                val projects = projectsData.value
                val project = projects?.find { it.id == projectId } ?: projectEmpty
                val tasks = project.tasks
                val task = tasks.find { it.id == taskId } ?: taskEmpty

                val record = TimeRecord(TikalEntity.ID_NONE, project, task)
                if (startTime != TimeRecord.NEVER) {
                    record.startTime = startTime
                }
                if (finishTime != TimeRecord.NEVER) {
                    record.finishTime = finishTime
                }
                return record
            }
        }

        return null
    }

    override fun populateForm(record: TimeRecord) {
        Timber.i("populateForm record=$record")
        val recordStarted = getStartedRecord() ?: TimeRecord.EMPTY
        Timber.i("populateForm recordStarted=$recordStarted")
        if (recordStarted.project.isNullOrEmpty() and recordStarted.task.isNullOrEmpty()) {
            applyFavorite()
        } else if (!recordStarted.isEmpty()) {
            val projects = projectsData.value
            val recordStartedProjectId = recordStarted.project.id
            val recordStartedTaskId = recordStarted.task.id
            val project = projects?.find { it.id == recordStartedProjectId } ?: record.project
            setRecordProject(project)
            val tasks = project.tasks
            val task = tasks.find { it.id == recordStartedTaskId } ?: record.task
            setRecordTask(task)
            record.start = recordStarted.start
        }
    }

    private fun editRecord(record: TimeRecord) {
        Timber.i("editRecord record=$record currentDestination=${findNavController().currentDestination?.label}")
        val parent = findParentFragment(TimeListFragment::class.java)
        if (parent != null) {
            parent.editRecord(record, true)
        } else {
            val args = Bundle()
            args.putLong(TimeEditFragment.EXTRA_PROJECT_ID, record.project.id)
            args.putLong(TimeEditFragment.EXTRA_TASK_ID, record.task.id)
            args.putLong(TimeEditFragment.EXTRA_START_TIME, record.startTime)
            args.putLong(TimeEditFragment.EXTRA_FINISH_TIME, record.finishTime)
            args.putLong(TimeEditFragment.EXTRA_RECORD_ID, record.id)
            parentFragmentManager.putFragment(args, TimeEditFragment.EXTRA_CALLER, caller ?: this)
            findNavController().navigate(R.id.action_timer_to_timeEdit, args)
        }
    }

    fun run() {
        Timber.i("run first=$firstRun")
        dataSource.timerPage(firstRun)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ page ->
                processPage(page)
                populateAndBind()
                handleArguments()
            }, { err ->
                Timber.e(err, "Error loading page: ${err.message}")
                handleError(err)
            })
            .addTo(disposables)
    }

    private fun processPage(page: TimerPage) {
        projectsData.value = page.projects
        setRecordValue(page.record)
    }

    private fun handleArguments() {
        Timber.d("handleArguments")
        val args = arguments
        if (args != null) {
            if (args.containsKey(EXTRA_ACTION)) {
                val action = args.getString(EXTRA_ACTION)
                if (action == ACTION_STOP) {
                    args.remove(EXTRA_ACTION)
                    if (args.getBoolean(EXTRA_COMMIT)) {
                        stopTimerCommit()
                    } else {
                        stopTimer()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        run()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(STATE_RECORD, record.toTimeRecordEntity())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val recordParcel = savedInstanceState.getParcelable<TimeRecordEntity>(STATE_RECORD)

        if (recordParcel != null) {
            val projects = projectsData.value
            val record = recordParcel.toTimeRecord(projects)
            setRecordValue(record)
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
                Timber.i("record processed")
                stopTimerCommit()
            } else {
                Timber.i("record edit cancelled")
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun authenticate(submit: Boolean) {
        // Parent fragment responsible for authentication.
    }

    override fun onProjectsUpdated(projects: List<Project>) {
        val context = this.context ?: return
        super.onProjectsUpdated(projects)
        bindProjects(context, record, projects)
    }

    companion object {
        const val EXTRA_ACTION = TrackerFragment.EXTRA_ACTION
        const val EXTRA_CALLER = TrackerFragment.EXTRA_CALLER
        const val EXTRA_PROJECT_ID = TimeFormFragment.EXTRA_PROJECT_ID
        const val EXTRA_TASK_ID = TimeFormFragment.EXTRA_TASK_ID
        const val EXTRA_START_TIME = TimeFormFragment.EXTRA_START_TIME
        const val EXTRA_FINISH_TIME = TimeFormFragment.EXTRA_FINISH_TIME
        const val EXTRA_COMMIT = BuildConfig.APPLICATION_ID + ".COMMIT"

        const val ACTION_STOP = TrackerFragment.ACTION_STOP

        private const val REQUEST_EDIT = 0xED17

        private const val CHILD_START = 0
        private const val CHILD_STOP = 1
    }
}