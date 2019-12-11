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
import android.text.format.DateUtils
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.time.*
import java.util.*

/**
 * Time record entity. Represents some work done for a project task.
 *
 * @author Moshe Waisberg.
 */
//TODO @Parcelize
open class TimeRecord : TikalEntity, Parcelable {

    var project: Project
    var task: ProjectTask
    var start: Calendar? = null
        set(value) {
            // Server granularity is seconds.
            value?.millis = 0
            field = value
        }
    var finish: Calendar? = null
        set(value) {
            // Server granularity is seconds.
            value?.millis = 0
            field = value
        }
    var note: String = ""
    var cost: Double = 0.0
    var status: TaskRecordStatus = TaskRecordStatus.DRAFT

    var startTime: Long
        get() = start?.timeInMillis ?: 0L
        set(value) {
            val cal = start ?: Calendar.getInstance()
            cal.timeInMillis = value
            start = cal
        }
    var finishTime: Long
        get() = finish?.timeInMillis ?: 0L
        set(value) {
            val cal = finish ?: Calendar.getInstance()
            cal.timeInMillis = value
            finish = cal
        }

    constructor(
        id: Long = ID_NONE,
        project: Project,
        task: ProjectTask,
        start: Calendar? = null,
        finish: Calendar? = null,
        note: String = "",
        cost: Double = 0.0,
        status: TaskRecordStatus = TaskRecordStatus.DRAFT
    ) {
        this.id = id
        this.project = project
        this.task = task
        this.start = start
        this.finish = finish
        this.note = note
        this.cost = cost
        this.status = status
    }

    constructor(parcel: Parcel) : this(ID_NONE, Project.EMPTY.copy(), ProjectTask.EMPTY.copy()) {
        id = parcel.readLong()
        version = parcel.readInt()
        project.id = parcel.readLong()
        task.id = parcel.readLong()
        startTime = parcel.readLong()
        finishTime = parcel.readLong()
        note = parcel.readString() ?: ""
        status = TaskRecordStatus.values()[parcel.readInt()]
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeInt(version)
        parcel.writeLong(project.id)
        parcel.writeLong(task.id)
        parcel.writeLong(startTime)
        parcel.writeLong(finishTime)
        parcel.writeString(note)
        parcel.writeInt(status.ordinal)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun isEmpty(): Boolean {
        return project.isEmpty()
            || task.isEmpty()
            || (startTime <= 0L)
    }

    open fun copy(): TimeRecord {
        return TimeRecord(
            id,
            project,
            task,
            start,
            finish,
            note,
            cost,
            status
        )
    }

    fun copy(start: Calendar?, finish: Calendar?): TimeRecord {
        return TimeRecord(
            id,
            project,
            task,
            start,
            finish,
            note,
            cost,
            status
        )
    }

    override fun equals(other: Any?): Boolean {
        if (other is TimeRecord) {
            return (this.id == other.id)
                && (this.project == other.project)
                && (this.task == other.task)
                && (this.startTime == other.startTime)
                && (this.finishTime == other.finishTime)
        }
        return super.equals(other)
    }

    override fun toString(): String {
        return "{id: $id, project: $project, task: $task, start: $startTime, finish: $finishTime, status: $status}"
    }

    companion object {
        val EMPTY: TimeRecord = TimeRecord(ID_NONE, Project.EMPTY, ProjectTask.EMPTY)

        @JvmField
        val CREATOR = object : Parcelable.Creator<TimeRecord> {
            override fun createFromParcel(parcel: Parcel): TimeRecord {
                return TimeRecord(parcel)
            }

            override fun newArray(size: Int): Array<TimeRecord?> {
                return arrayOfNulls(size)
            }
        }
    }
}

fun TimeRecord.split(): List<TimeRecord> {
    val results = ArrayList<TimeRecord>()

    if (isEmpty()) return results
    val start = start ?: return results
    val startMillis = start.timeInMillis
    val finish = finish ?: return results
    var diffMillis = finish.timeInMillis - startMillis
    if (diffMillis < DateUtils.MINUTE_IN_MILLIS) return results

    if (start.isSameDay(finish)) {
        results.add(this)
    } else {
        // The first day.
        val startFirst = start
        val finishFirst = startFirst.copy()
        finishFirst.setToEndOfDay()
        results.add(this.copy(start = startFirst, finish = finishFirst))
        diffMillis -= finishFirst.timeInMillis - startFirst.timeInMillis + 1L

        // Intermediate days.
        var startDay = startFirst
        var finishDay: Calendar
        while (diffMillis >= DateUtils.DAY_IN_MILLIS) {
            startDay = startDay.copy()
            startDay.add(Calendar.DAY_OF_MONTH, 1)  // Next day
            startDay.setToStartOfDay()
            finishDay = startDay.copy()
            finishDay.setToEndOfDay()
            results.add(this.copy(start = startDay, finish = finishDay))
            diffMillis -= DateUtils.DAY_IN_MILLIS
        }

        // The last day.
        val startLast = finish.copy()
        val finishLast = finish
        startLast.setToStartOfDay()
        results.add(this.copy(start = startLast, finish = finishLast))
    }

    return results
}

@Suppress("NOTHING_TO_INLINE")
inline fun TimeRecord?.isNullOrEmpty(): Boolean {
    return (this == null) || isEmpty()
}
