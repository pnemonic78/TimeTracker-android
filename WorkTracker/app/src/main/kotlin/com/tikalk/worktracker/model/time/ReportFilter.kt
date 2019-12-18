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

import android.os.Parcel
import android.os.Parcelable
import com.tikalk.os.readBool
import com.tikalk.os.writeBool
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.ReportTimePeriod
import com.tikalk.worktracker.time.*
import java.util.*

/**
 * Report filter entity.
 *
 * @author Moshe Waisberg.
 */
//TODO @Parcelize
class ReportFilter : TimeRecord {

    var period: ReportTimePeriod = ReportTimePeriod.CUSTOM
    var favorite: String? = null
    var showProjectField: Boolean = true
    var showTaskField: Boolean = true
    var showStartField: Boolean = true
    var showFinishField: Boolean = true
    var showDurationField: Boolean = true
    var showNoteField: Boolean = true
    var showCostField: Boolean = false

    constructor() : super(ID_NONE, Project.EMPTY, ProjectTask.EMPTY)

    constructor(
        project: Project = Project.EMPTY,
        task: ProjectTask = ProjectTask.EMPTY,
        start: Calendar? = null,
        finish: Calendar? = null,
        period: ReportTimePeriod = ReportTimePeriod.CUSTOM,
        favorite: String? = null,
        showProjectField: Boolean = true,
        showTaskField: Boolean = true,
        showStartField: Boolean = true,
        showFinishField: Boolean = true,
        showDurationField: Boolean = true,
        showNoteField: Boolean = true,
        showCostField: Boolean = false
    ) : super(ID_NONE, project, task, start, finish) {
        this.period = period
        this.favorite = favorite
        this.showProjectField = showProjectField
        this.showTaskField = showTaskField
        this.showStartField = showStartField
        this.showFinishField = showFinishField
        this.showDurationField = showDurationField
        this.showNoteField = showNoteField
        this.showCostField = showCostField
    }

    constructor(parcel: Parcel) : super(parcel) {
        period = ReportTimePeriod.values()[parcel.readInt()]
        favorite = parcel.readString()
        showProjectField = parcel.readBool()
        showTaskField = parcel.readBool()
        showStartField = parcel.readBool()
        showFinishField = parcel.readBool()
        showDurationField = parcel.readBool()
        showNoteField = parcel.readBool()
        showCostField = parcel.readBool()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeInt(period.ordinal)
        parcel.writeString(favorite)
        parcel.writeBool(showProjectField)
        parcel.writeBool(showTaskField)
        parcel.writeBool(showStartField)
        parcel.writeBool(showFinishField)
        parcel.writeBool(showDurationField)
        parcel.writeBool(showNoteField)
        parcel.writeBool(showCostField)
    }

    fun toFields(): Map<String, String> {
        return HashMap<String, String>().apply {
            // Main form
            put("project", if (project.id == ID_NONE) "" else project.id.toString())
            put("task", if (task.id == ID_NONE) "" else task.id.toString())
            put("period", period.toString())
            put("start_date", formatSystemDate(start))
            put("end_date", formatSystemDate(finish))

            // Always fetch these fields - just hide them in UI.
            put("chproject", "1")
            put("chtask", "1")
            put("chstart", "1")
            put("chfinish", "1")

            if (showNoteField) {
                put("chnote", "1")
            }
            if (showDurationField) {
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
                val first = today.copy()
                first.month--
                first.dayOfMonth = 1
                val last = first.copy()
                last.dayOfMonth = last.getActualMaximum(Calendar.DAY_OF_MONTH)
                this.start = first
                this.finish = last
            }
            ReportTimePeriod.PREVIOUS_WEEK -> {
                val first = today.copy()
                first.dayOfWeek = first.firstDayOfWeek
                first.dayOfMonth -= 7
                val last = first.copy()
                last.dayOfMonth += 6
                this.start = first
                this.finish = last
            }
            ReportTimePeriod.THIS_MONTH -> {
                val first = today.copy()
                first.dayOfMonth = 1
                val last = today.copy()
                last.dayOfMonth = last.getActualMaximum(Calendar.DAY_OF_MONTH)
                this.start = first
                this.finish = last
            }
            ReportTimePeriod.THIS_WEEK -> {
                val first = today.copy()
                first.dayOfWeek = first.firstDayOfWeek
                val last = first.copy()
                last.dayOfMonth += 6
                this.start = first
                this.finish = last
            }
            ReportTimePeriod.TODAY -> {
                this.start = today.copy()
                this.finish = today.copy()
            }
            ReportTimePeriod.YESTERDAY -> {
                val yesterday = today.copy()
                yesterday.dayOfMonth--
                this.start = yesterday
                this.finish = yesterday.copy()
            }
        }

        // Server granularity for report is days.
        this.start?.setToStartOfDay()
        this.finish?.setToEndOfDay()
    }

    companion object {

        @JvmField
        val CREATOR = object : Parcelable.Creator<ReportFilter> {
            override fun createFromParcel(parcel: Parcel): ReportFilter {
                return ReportFilter(parcel)
            }

            override fun newArray(size: Int): Array<ReportFilter?> {
                return arrayOfNulls(size)
            }
        }
    }
}
