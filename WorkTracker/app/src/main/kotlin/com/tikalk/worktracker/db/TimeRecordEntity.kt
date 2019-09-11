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
package com.tikalk.worktracker.db

import androidx.room.*
import com.tikalk.worktracker.model.*
import com.tikalk.worktracker.model.time.TaskRecordStatus
import com.tikalk.worktracker.model.time.TimeRecord
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
    ]
)
@TypeConverters(TimeRecordConverters::class)
data class TimeRecordEntity(
    @Ignore
    override var id: Long,
    @ColumnInfo(name = "user_id")
    var userId: Long,
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
    @ColumnInfo(name = "status")
    var status: TaskRecordStatus = TaskRecordStatus.DRAFT
) : TikalEntity(id)

open class TimeRecordConverters : Converters() {
    @TypeConverter
    fun fromRecordStatus(value: TaskRecordStatus): Int = value.ordinal

    @TypeConverter
    fun toRecordStatus(value: Int): TaskRecordStatus = TaskRecordStatus.values()[value]
}

fun toTimeRecordEntity(value: TimeRecord): TimeRecordEntity =
    TimeRecordEntity(
        value.id,
        value.user.id,
        value.project.id,
        value.task.id,
        value.start,
        value.finish,
        value.note,
        value.status
    )

fun toTimeRecord(value: TimeRecordEntity, user: User = User.EMPTY.copy(), projects: Collection<Project>? = null, tasks: Collection<ProjectTask>? = null): TimeRecord {
    val project = projects?.firstOrNull { it.id == value.projectId }
        ?: Project.EMPTY.copy().apply { id = value.projectId }
    val task = tasks?.firstOrNull { it.id == value.taskId }
        ?: ProjectTask.EMPTY.copy().apply { id = value.taskId }

    return TimeRecord(
        value.id,
        user,
        project,
        task,
        value.start,
        value.finish,
        value.note,
        value.status
    )
}
