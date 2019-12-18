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

import androidx.room.*
import com.tikalk.worktracker.model.*
import com.tikalk.worktracker.model.time.TaskRecordStatus
import com.tikalk.worktracker.model.time.TimeRecord
import java.util.*

/**
 * Report time record for database entity.
 *
 * @author Moshe Waisberg.
 */
@Entity(tableName = "report",
    foreignKeys = [
        ForeignKey(entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["project_id"]),
        ForeignKey(entity = ProjectTask::class,
            parentColumns = ["id"],
            childColumns = ["task_id"])
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
    note: String = "",
    cost: Double = 0.0,
    status: TaskRecordStatus = TaskRecordStatus.DRAFT
) : TimeRecordEntity(id, projectId, taskId, start, finish, note, cost, status)

fun TimeRecord.toReportRecord(): ReportRecord =
    ReportRecord(
        this.id,
        this.project.id,
        this.project.name,
        this.task.id,
        this.task.name,
        this.start,
        this.finish,
        this.note,
        this.cost,
        this.status
    )

fun ReportRecord.toTimeRecord(projects: Collection<Project>? = null, tasks: Collection<ProjectTask>? = null): TimeRecord {
    val value: ReportRecord = this
    val project = projects?.firstOrNull { it.id == value.projectId }
        ?: projects?.firstOrNull { it.name == value.projectName }
        ?: Project.EMPTY.copy().apply { id = value.projectId }
    val task = tasks?.firstOrNull { it.id == value.taskId }
        ?: tasks?.firstOrNull { it.name == value.taskName }
        ?: ProjectTask.EMPTY.copy().apply { id = value.taskId }

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
