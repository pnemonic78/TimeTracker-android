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

import android.text.format.DateUtils
import com.tikalk.worktracker.model.Location
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.time.copy
import com.tikalk.worktracker.time.isSameDay
import com.tikalk.worktracker.time.millis
import com.tikalk.worktracker.time.setToEndOfDay
import com.tikalk.worktracker.time.setToStartOfDay
import java.util.Calendar

/**
 * Time record entity. Represents some work done for a project task.
 *
 * @author Moshe Waisberg.
 */
open class TimeRecord(
    id: Long = ID_NONE,
    var project: Project,
    var task: ProjectTask,
    start: Calendar? = null,
    finish: Calendar? = null,
    var note: String = "",
    var cost: Double = 0.0,
    var status: TaskRecordStatus = TaskRecordStatus.DRAFT,
    var location: Location = Location.EMPTY
) : TikalEntity(id) {

    var start: Calendar? = start
        set(value) {
            // Server granularity is seconds.
            value?.millis = 0
            field = value
        }
    var finish: Calendar? = finish
        set(value) {
            // Server granularity is seconds.
            value?.millis = 0
            field = value
        }

    var startTime: Long
        get() = start?.timeInMillis ?: NEVER
        set(value) {
            val cal = start ?: Calendar.getInstance()
            cal.timeInMillis = value
            start = cal
        }
    var finishTime: Long
        get() = finish?.timeInMillis ?: NEVER
        set(value) {
            val cal = finish ?: Calendar.getInstance()
            cal.timeInMillis = value
            finish = cal
        }

    fun isEmpty(): Boolean {
        return project.isEmpty()
            || task.isEmpty()
            || (startTime <= NEVER)
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
            status,
            location
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
            status,
            location
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
        return "{id: $id, project: $project, task: $task, location: $location, start: $startTime, finish: $finishTime, version: $version, $status}"
    }

    override fun hashCode(): Int {
        var result = project.hashCode()
        result = 31 * result + task.hashCode()
        result = 31 * result + startTime.hashCode()
        return result
    }

    operator fun compareTo(that: TimeRecord): Int {
        val id1 = this.id
        val id2 = that.id
        var c = id1.compareTo(id2)
        if (c != 0) return c

        val s1 = this.status
        val s2 = that.status
        c = s1.compareTo(s2)
        if (c != 0) return c

        val v1 = this.version
        val v2 = that.version
        c = v1.compareTo(v2)
        if (c != 0) return c

        val st1 = this.startTime
        val st2 = that.startTime
        c = st1.compareTo(st2)
        if (c != 0) return c

        val f1 = this.finishTime
        val f2 = that.finishTime
        c = f1.compareTo(f2)
        if (c != 0) return c

        val p1 = this.project
        val p2 = that.project
        c = p1.compareTo(p2)
        if (c != 0) return c

        val t1 = this.task
        val t2 = that.task
        c = t1.compareTo(t2)
        if (c != 0) return c

        val l1 = this.location
        val l2 = that.location
        c = l1.compareTo(l2)
        if (c != 0) return c

        val c1 = this.cost
        val c2 = that.cost
        c = c1.compareTo(c2)
        if (c != 0) return c

        return 0
    }

    companion object {
        val EMPTY: TimeRecord = TimeRecord(ID_NONE, Project.EMPTY, ProjectTask.EMPTY)

        const val NEVER = 0L
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
