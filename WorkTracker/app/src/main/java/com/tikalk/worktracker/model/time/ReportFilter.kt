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
package com.tikalk.worktracker.model.time

import android.os.Parcelable
import com.tikalk.worktracker.model.DefaultTimePeriod
import com.tikalk.worktracker.model.Location
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.ReportTimePeriod
import com.tikalk.worktracker.time.copy
import com.tikalk.worktracker.time.dayOfMonth
import com.tikalk.worktracker.time.dayOfWeek
import com.tikalk.worktracker.time.formatSystemDate
import com.tikalk.worktracker.time.month
import com.tikalk.worktracker.time.setToEndOfDay
import com.tikalk.worktracker.time.setToStartOfDay
import kotlinx.parcelize.Parcelize
import java.util.Calendar

/**
 * Report filter entity.
 *
 * @author Moshe Waisberg.
 */
@Parcelize
class ReportFilter(
    var period: ReportTimePeriod = DefaultTimePeriod,
    var favorite: String? = null,
    var isProjectFieldVisible: Boolean = true,
    var isTaskFieldVisible: Boolean = true,
    var isStartFieldVisible: Boolean = true,
    var isFinishFieldVisible: Boolean = true,
    var isDurationFieldVisible: Boolean = true,
    var isNoteFieldVisible: Boolean = true,
    var isCostFieldVisible: Boolean = false
) : TimeRecord(
    id = ID_NONE,
    project = Project.EMPTY,
    task = ProjectTask.EMPTY,
    date = Calendar.getInstance()
), Parcelable {

    constructor(
        project: Project = Project.EMPTY,
        task: ProjectTask = ProjectTask.EMPTY,
        start: Calendar? = null,
        finish: Calendar? = null,
        period: ReportTimePeriod = DefaultTimePeriod,
        favorite: String? = null,
        location: Location = Location.EMPTY,
        isProjectFieldVisible: Boolean = true,
        isTaskFieldVisible: Boolean = true,
        isStartFieldVisible: Boolean = true,
        isFinishFieldVisible: Boolean = true,
        isDurationFieldVisible: Boolean = true,
        isNoteFieldVisible: Boolean = true,
        isCostFieldVisible: Boolean = false
    ) : this(
        period = period,
        favorite = favorite,
        isProjectFieldVisible = isProjectFieldVisible,
        isTaskFieldVisible = isTaskFieldVisible,
        isStartFieldVisible = isStartFieldVisible,
        isFinishFieldVisible = isFinishFieldVisible,
        isDurationFieldVisible = isDurationFieldVisible,
        isNoteFieldVisible = isNoteFieldVisible,
        isCostFieldVisible = isCostFieldVisible
    ) {
        this.project = project
        this.task = task
        this.start = start
        this.finish = finish
        this.location = location
    }

    fun toFields(): Map<String, String> {
        return HashMap<String, String>().apply {
            // Main form
            put("project", if (project.id == ID_NONE) "" else project.id.toString())
            put("task", if (task.id == ID_NONE) "" else task.id.toString())
            put("period", period.toString())
            formatSystemDate(start)?.let { put("start_date", it) }
            formatSystemDate(finish)?.let { put("end_date", it) }
            put("time_field_5", location.id.toString())

            // Always fetch these fields - just hide them in UI.
            put("chproject", "1")
            put("chtask", "1")
            put("chstart", "1")
            put("chfinish", "1")

            if (isNoteFieldVisible) {
                put("chnote", "1")
            }
            if (isDurationFieldVisible) {
                put("chduration", "1")
            }
            //put("chcost", "1")
            //put("chtotalsonly", "1")

            // Grouping
            put("group_by1", "no_grouping")
            put("group_by2", "no_grouping")
            put("group_by3", "no_grouping")

            // Favorite
            put("favorite_report", "-1")
            put("new_fav_report", "")
            put("fav_report_changed", "")
        }
    }

    fun updateDates(today: Calendar) {
        when (period) {
            ReportTimePeriod.CUSTOM -> {
                if (start == null) {
                    this.start = today.copy()
                }
                if (finish == null) {
                    this.finish = today.copy()
                }
            }
            ReportTimePeriod.PREVIOUS_MONTH -> {
                val first = today.copy().apply {
                    month--
                    dayOfMonth = 1
                }
                val last = first.copy().apply {
                    dayOfMonth = getActualMaximum(Calendar.DAY_OF_MONTH)
                }
                this.start = first
                this.finish = last
            }
            ReportTimePeriod.PREVIOUS_WEEK -> {
                val first = today.copy().apply {
                    dayOfWeek = firstDayOfWeek
                    dayOfMonth -= 7
                }
                val last = first.copy().apply {
                    dayOfMonth += 6
                }
                this.start = first
                this.finish = last
            }
            ReportTimePeriod.THIS_MONTH -> {
                val first = today.copy().apply {
                    dayOfMonth = 1
                }
                val last = today.copy().apply {
                    dayOfMonth = getActualMaximum(Calendar.DAY_OF_MONTH)
                }
                this.start = first
                this.finish = last
            }
            ReportTimePeriod.THIS_WEEK -> {
                val first = today.copy().apply {
                    dayOfWeek = firstDayOfWeek
                }
                val last = first.copy().apply {
                    dayOfMonth += 6
                }
                this.start = first
                this.finish = last
            }
            ReportTimePeriod.TODAY -> {
                this.start = today
                this.finish = today
            }
            ReportTimePeriod.YESTERDAY -> {
                val yesterday = today.copy().apply {
                    dayOfMonth--
                }
                this.start = yesterday
                this.finish = yesterday
            }
        }

        // Server granularity for report is days.
        this.start?.setToStartOfDay()
        this.finish?.setToEndOfDay()
    }

    override fun toString(): String {
        return "{project: $project, task: $task, start: $startTime, finish: $finishTime, period: ${period.name}, show: ${toShowString()}, status: $status}"
    }

    private fun toShowString(): CharSequence {
        val s = StringBuffer()
        if (isProjectFieldVisible) s.append('P')
        if (isTaskFieldVisible) s.append('T')
        if (isStartFieldVisible) s.append('S')
        if (isFinishFieldVisible) s.append('F')
        if (isDurationFieldVisible) s.append('D')
        if (isNoteFieldVisible) s.append('N')
        if (isCostFieldVisible) s.append('C')
        return s
    }
}
