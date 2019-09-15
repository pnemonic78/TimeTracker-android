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

import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import com.tikalk.worktracker.R
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.time.TaskRecordStatus
import com.tikalk.worktracker.model.time.TimeRecord
import kotlinx.android.synthetic.main.time_form.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import timber.log.Timber
import java.util.*
import kotlin.math.max

class TimeEditFragment : TimeFormFragment() {

    private var startPickerDialog: TimePickerDialog? = null
    private var finishPickerDialog: TimePickerDialog? = null
    private var errorMessage: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.time_form, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        project_input.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(adapterView: AdapterView<*>) {
                projectItemSelected(projectEmpty)
            }

            override fun onItemSelected(adapterView: AdapterView<*>, view: View?, position: Int, id: Long) {
                val project = adapterView.adapter.getItem(position) as Project
                projectItemSelected(project)
            }
        }
        task_input.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(adapterView: AdapterView<*>) {
                taskItemSelected(taskEmpty)
            }

            override fun onItemSelected(adapterView: AdapterView<*>, view: View?, position: Int, id: Long) {
                val task = adapterView.adapter.getItem(position) as ProjectTask
                taskItemSelected(task)
            }
        }
        start_input.setOnClickListener { pickStartTime() }
        finish_input.setOnClickListener { pickFinishTime() }
    }

    override fun populateForm(html: String, date: Calendar) {
    }

    /** Populate the record and then bind the form. */
    fun populateForm(html: String, date: Calendar, id: Long) {
        populateForm(html, date)
        val doc: Document = Jsoup.parse(html)

        record.id = id

        errorMessage = findError(doc)?.trim() ?: ""

        val form = doc.selectFirst("form[name='timeRecordForm']") ?: return

        val inputProjects = form.selectFirst("select[name='project']") ?: return
        populateProjects(inputProjects, projects)

        val inputTasks = form.selectFirst("select[name='task']") ?: return
        populateTasks(inputTasks, tasks)

        populateTaskIds(doc, projects)

        val inputStart = form.selectFirst("input[name='start']") ?: return
        val startValue = inputStart.attr("value")

        val inputFinish = form.selectFirst("input[name='finish']") ?: return
        val finishValue = inputFinish.attr("value")

        val inputNote = form.selectFirst("textarea[name='note']")

        record.project = findSelectedProject(inputProjects, projects)
        record.task = findSelectedTask(inputTasks, tasks)
        record.start = parseSystemTime(date, startValue)
        record.finish = parseSystemTime(date, finishValue)
        record.note = inputNote?.text() ?: ""

        if (id == TikalEntity.ID_NONE) {
            val projectFavorite = prefs.getFavoriteProject()
            val taskFavorite = prefs.getFavoriteTask()

            val args = arguments
            if (args != null) {
                var projectId = args.getLong(TimeEditActivity.EXTRA_PROJECT_ID)
                var taskId = args.getLong(TimeEditActivity.EXTRA_TASK_ID)
                val startTime = args.getLong(TimeEditActivity.EXTRA_START_TIME)
                val finishTime = args.getLong(TimeEditActivity.EXTRA_FINISH_TIME)

                if (projectId == TikalEntity.ID_NONE) projectId = projectFavorite
                if (taskId == TikalEntity.ID_NONE) taskId = taskFavorite

                val project = projects.firstOrNull { it.id == projectId } ?: projectEmpty
                val task = tasks.firstOrNull { it.id == taskId } ?: taskEmpty

                record = TimeRecord(id, user, project, task)
                if (startTime > 0L) {
                    record.startTime = startTime
                } else {
                    record.start = null
                }
                if (finishTime > 0L) {
                    record.finishTime = finishTime
                } else {
                    record.finish = null
                }
            } else {
                record.project = projects.firstOrNull { it.id == projectFavorite } ?: record.project
                record.task = tasks.firstOrNull { it.id == taskFavorite } ?: record.task
            }
        } else {
            record.status = TaskRecordStatus.CURRENT
        }

        runOnUiThread { bindForm(record) }
    }

    fun populateForm(record: TimeRecord) {
        Timber.v("populateForm $record")
        bindForm(record)
    }

    override fun bindForm(record: TimeRecord) {
        val context: Context = this.context ?: return

        error_label.text = errorMessage
        project_input.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, projects.toTypedArray())
        if (projects.isNotEmpty()) {
            project_input.setSelection(max(0, projects.indexOf(record.project)))
        }
        task_input.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, tasks.toTypedArray())
        if (tasks.isNotEmpty()) {
            task_input.setSelection(max(0, tasks.indexOf(record.task)))
        }
        project_input.requestFocus()

        val startTime = record.startTime
        start_input.text = if (startTime > 0L)
            DateUtils.formatDateTime(context, startTime, TimeEditActivity.FORMAT_DATE_BUTTON)
        else
            ""
        start_input.error = null
        startPickerDialog = null

        val finishTime = record.finishTime
        finish_input.text = if (finishTime > 0L)
            DateUtils.formatDateTime(context, finishTime, TimeEditActivity.FORMAT_DATE_BUTTON)
        else
            ""
        finish_input.error = null
        finishPickerDialog = null

        note_input.setText(record.note)
    }

    fun bindRecord(record: TimeRecord) {
        record.note = note_input.text.toString()
    }

    private fun pickStartTime() {
        if (startPickerDialog == null) {
            val cal = getCalendar(record.start)
            val listener = TimePickerDialog.OnTimeSetListener { picker, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                record.start = cal
                start_input.text = DateUtils.formatDateTime(context, cal.timeInMillis, TimeEditActivity.FORMAT_DATE_BUTTON)
                start_input.error = null
            }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)
            startPickerDialog = TimePickerDialog(context, listener, hour, minute, DateFormat.is24HourFormat(context))
        }
        startPickerDialog!!.show()
    }

    private fun pickFinishTime() {
        if (finishPickerDialog == null) {
            val cal = getCalendar(record.finish)
            val listener = TimePickerDialog.OnTimeSetListener { picker, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                record.finish = cal
                finish_input.text = DateUtils.formatDateTime(context, cal.timeInMillis, TimeEditActivity.FORMAT_DATE_BUTTON)
                finish_input.error = null
            }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)
            finishPickerDialog = TimePickerDialog(context, listener, hour, minute, DateFormat.is24HourFormat(context))
        }
        finishPickerDialog!!.show()
    }

    private fun getCalendar(cal: Calendar?): Calendar {
        if (cal == null) {
            val calDate = Calendar.getInstance()
            calDate.timeInMillis = date.timeInMillis
            return calDate
        }
        return cal
    }

    fun validateForm(record: TimeRecord): Boolean {
        var valid = true

        if (record.project.id <= 0) {
            valid = false
            (project_input.selectedView as TextView).error = getString(R.string.error_field_required)
        } else {
            (project_input.selectedView as TextView).error = null
        }
        if (record.task.id <= 0) {
            valid = false
            (task_input.selectedView as TextView).error = getString(R.string.error_field_required)
        } else {
            (task_input.selectedView as TextView).error = null
        }
        if (record.start == null) {
            valid = false
            start_input.error = getString(R.string.error_field_required)
        } else {
            start_input.error = null
        }
        if (record.finish == null) {
            valid = false
            finish_input.error = getString(R.string.error_field_required)
        } else if (record.startTime + DateUtils.MINUTE_IN_MILLIS > record.finishTime) {
            valid = false
            finish_input.error = getString(R.string.error_finish_time_before_start_time)
        } else {
            finish_input.error = null
        }

        return valid
    }

    fun filterTasks(project: Project) {
        val context: Context = this.context ?: return
        val filtered = project.tasks
        val options = ArrayList<ProjectTask>(filtered.size + 1)
        options.add(taskEmpty)
        options.addAll(filtered)
        task_input.adapter = ArrayAdapter<ProjectTask>(context, android.R.layout.simple_list_item_1, options)
        task_input.setSelection(options.indexOf(record.task))
    }

    private fun projectItemSelected(project: Project) {
        record.project = project
        filterTasks(project)
    }

    private fun taskItemSelected(task: ProjectTask) {
        record.task = task
    }
}