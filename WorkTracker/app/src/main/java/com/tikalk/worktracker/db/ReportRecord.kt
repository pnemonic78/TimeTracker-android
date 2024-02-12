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
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.TypeConverters
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.time.TaskRecordStatus
import com.tikalk.worktracker.model.time.TimeRecord
import java.util.Calendar

/**
 * Report time record for database entity.
 *
 * @author Moshe Waisberg.
 */
@Entity(
    tableName = "report",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["project_id"]
        ),
        ForeignKey(
            entity = ProjectTask::class,
            parentColumns = ["id"],
            childColumns = ["task_id"]
        )
    ],
    inheritSuperIndices = true
)
@TypeConverters(TimeRecordConverters::class)
class ReportRecord(
    id: Long,
    projectId: Long,
    @ColumnInfo(name = "project_name")
    var projectName: String,
    taskId: Long,
    @ColumnInfo(name = "task_name")
    var taskName: String,
    start: Calendar? = null,
    finish: Calendar? = null,
    date: Calendar,
    duration: Long = 0L,
    note: String = "",
    cost: Double = 0.0,
    status: TaskRecordStatus = TaskRecordStatus.DRAFT
) : TimeRecordEntity(
    id = id,
    projectId = projectId,
    taskId = taskId,
    start = start,
    finish = finish,
    date = date,
    duration = duration,
    note = note,
    cost = cost,
    status = status
) {
    companion object CREATOR : Parcelable.Creator<ReportRecord?> {
        override fun createFromParcel(parcel: Parcel?): ReportRecord? {
            return null
        }

        override fun newArray(size: Int): Array<ReportRecord?> {
            return arrayOfNulls(size)
        }
    }
}

fun TimeRecord.toReportRecord(): ReportRecord =
    ReportRecord(
        id = this.id,
        projectId = this.project.id,
        projectName = this.project.name,
        taskId = this.task.id,
        taskName = this.task.name,
        start = this.start,
        finish = this.finish,
        date = this.date,
        duration = this.duration,
        note = this.note,
        cost = this.cost,
        status = this.status
    )

fun ReportRecord.toTimeRecord(
    projects: Collection<Project>? = null,
    tasks: Collection<ProjectTask>? = null
): TimeRecord {
    val value: ReportRecord = this
    val project = projects?.find { it.id == value.projectId }
        ?: projects?.find { it.name == value.projectName }
        ?: Project.EMPTY.copy().apply {
            id = value.projectId
            name = value.projectName
        }
    val projectTasks = tasks ?: project.tasks
    val task = projectTasks.find { it.id == value.taskId }
        ?: projectTasks.find { it.name == value.taskName }
        ?: ProjectTask.EMPTY.copy().apply {
            id = value.taskId
            name = value.taskName
        }

    return TimeRecord(
        id = value.id,
        project = project,
        task = task,
        start = value.start,
        finish = value.finish,
        date = value.date,
        duration = value.duration,
        note = value.note,
        cost = value.cost,
        status = value.status
    )
}
