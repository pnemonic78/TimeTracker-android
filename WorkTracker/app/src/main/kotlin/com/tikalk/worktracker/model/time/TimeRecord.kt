/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2017, Tikal Knowledge, Ltd.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
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
import androidx.room.Entity
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.User
import com.tikalk.worktracker.time.*
import java.util.*

/**
 * Time record entity. Represents some work done for a project task.
 *
 * @author Moshe Waisberg.
 */
@Entity
data class TimeRecord(
    var user: User,
    var project: Project,
    var task: ProjectTask,
    var start: Calendar? = null,
    var finish: Calendar? = null,
    var note: String = "",
    var status: TaskRecordStatus = TaskRecordStatus.INSERTED,
    override var id: Long = 0
) : TikalEntity(id), Parcelable {

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

    fun isEmpty(): Boolean {
        return user.username.isEmpty()
            || (project.id <= 0L)
            || (task.id <= 0L)
            || (startTime <= 0L)
    }

    constructor(parcel: Parcel) : this(User("", ""), Project.EMPTY, ProjectTask.EMPTY) {
        id = parcel.readLong()
        dbId = parcel.readLong()
        version = parcel.readInt()

        user = User.CREATOR.createFromParcel(parcel)
        project = Project.CREATOR.createFromParcel(parcel)
        task = ProjectTask.CREATOR.createFromParcel(parcel)
        startTime = parcel.readLong()
        finishTime = parcel.readLong()
        note = parcel.readString() ?: ""
        status = TaskRecordStatus.values()[parcel.readInt()]
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeLong(dbId)
        parcel.writeInt(version)

        parcel.writeParcelable(user, flags)
        parcel.writeParcelable(project, flags)
        parcel.writeParcelable(task, flags)
        parcel.writeLong(startTime)
        parcel.writeLong(finishTime)
        parcel.writeString(note)
        parcel.writeInt(status.ordinal)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TimeRecord> {
        override fun createFromParcel(parcel: Parcel): TimeRecord {
            return TimeRecord(parcel)
        }

        override fun newArray(size: Int): Array<TimeRecord?> {
            return arrayOfNulls(size)
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
        val finishFirst = startFirst.clone() as Calendar
        finishFirst.hourOfDay = 23
        finishFirst.minute = 59
        finishFirst.second = 59
        finishFirst.millis = 999
        results.add(this.copy(start = startFirst, finish = finishFirst))
        diffMillis -= finishFirst.timeInMillis - startFirst.timeInMillis + 1L

        // Intermediate days.
        var startDay = startFirst
        var finishDay: Calendar
        while (diffMillis >= DateUtils.DAY_IN_MILLIS) {
            startDay = startDay.clone() as Calendar
            startDay.add(Calendar.DAY_OF_MONTH, 1)  // Next day
            startDay.hourOfDay = 0
            startDay.minute = 0
            startDay.second = 0
            startDay.millis = 0
            finishDay = startDay.clone() as Calendar
            finishDay.hourOfDay = 23
            finishDay.minute = 59
            finishDay.second = 59
            finishDay.millis = 999
            results.add(this.copy(start = startDay, finish = finishDay))
            diffMillis -= DateUtils.DAY_IN_MILLIS
        }

        // The last day.
        val startLast = finish.clone() as Calendar
        val finishLast = finish
        startLast.hourOfDay = 0
        startLast.minute = 0
        startLast.second = 0
        startLast.millis = 0
        results.add(this.copy(start = startLast, finish = finishLast))
    }

    return results
}
