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
package com.tikalk.worktracker.db

import android.os.Parcel
import android.os.Parcelable
import androidx.room.*
import com.tikalk.worktracker.model.Converters
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.TikalEntity.Companion.ID_NONE
import com.tikalk.worktracker.model.time.TaskRecordStatus
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.time.toCalendar
import java.util.*

/**
 * Time record database entity.
 *
 * @author Moshe Waisberg.
 */
@Entity(tableName = "record",
    foreignKeys = [
        ForeignKey(entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["project_id"]),
        ForeignKey(entity = ProjectTask::class,
            parentColumns = ["id"],
            childColumns = ["task_id"])
    ],
    indices = [Index("project_id"), Index("task_id")]
)
@TypeConverters(TimeRecordConverters::class)
//TODO @Parcelize
open class TimeRecordEntity(
    id: Long,
    @ColumnInfo(name = "project_id")
    var projectId: Long,
    @ColumnInfo(name = "task_id")
    var taskId: Long,
    @ColumnInfo(name = "start")
    var start: Calendar? = null,
    @ColumnInfo(name = "finish")
    var finish: Calendar? = null,
    @ColumnInfo(name = "note")
    var note: String = "",
    @ColumnInfo(name = "cost")
    var cost: Double = 0.0,
    @ColumnInfo(name = "status")
    var status: TaskRecordStatus = TaskRecordStatus.DRAFT
) : TikalEntity(id), Parcelable {

    constructor(parcel: Parcel) : this(ID_NONE, ID_NONE, ID_NONE) {
        id = parcel.readLong()
        version = parcel.readInt()
        projectId = parcel.readLong()
        taskId = parcel.readLong()
        val startTime = parcel.readLong()
        start = if (startTime == NEVER) null else startTime.toCalendar()
        val finishTime = parcel.readLong()
        finish = if (finishTime == NEVER) null else finishTime.toCalendar()
        note = parcel.readString() ?: ""
        status = TaskRecordStatus.values()[parcel.readInt()]
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeInt(version)
        parcel.writeLong(projectId)
        parcel.writeLong(taskId)
        parcel.writeLong(start?.timeInMillis ?: NEVER)
        parcel.writeLong(finish?.timeInMillis ?: NEVER)
        parcel.writeString(note)
        parcel.writeInt(status.ordinal)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {

        private const val NEVER = TimeRecord.NEVER

        @JvmField
        val CREATOR = object : Parcelable.Creator<TimeRecordEntity> {
            override fun createFromParcel(parcel: Parcel): TimeRecordEntity {
                return TimeRecordEntity(parcel)
            }

            override fun newArray(size: Int): Array<TimeRecordEntity?> {
                return arrayOfNulls(size)
            }
        }
    }
}

open class TimeRecordConverters : Converters() {
    @TypeConverter
    fun fromRecordStatus(value: TaskRecordStatus): Int = value.ordinal

    @TypeConverter
    fun toRecordStatus(value: Int): TaskRecordStatus = TaskRecordStatus.values()[value]
}

fun TimeRecord.toTimeRecordEntity(): TimeRecordEntity =
    TimeRecordEntity(
        this.id,
        this.project.id,
        this.task.id,
        this.start,
        this.finish,
        this.note,
        this.cost,
        this.status
    )

fun TimeRecordEntity.toTimeRecord(projects: Collection<Project>? = null, tasks: Collection<ProjectTask>? = null): TimeRecord {
    val value: TimeRecordEntity = this
    val project = projects?.firstOrNull { it.id == value.projectId }
        ?: Project.EMPTY.copy().apply { id = value.projectId }
    val task = tasks?.firstOrNull { it.id == value.taskId }
        ?: ProjectTask.EMPTY.copy().apply { id = value.taskId }

    if ((project.id != ID_NONE) and project.name.isEmpty()) {
        project.name = "project"
    }
    if ((task.id != ID_NONE) and task.name.isEmpty()) {
        task.name = "task"
    }

    return TimeRecord(
        value.id,
        project,
        task,
        value.start,
        value.finish,
        value.note,
        value.cost,
        value.status
    )
}
