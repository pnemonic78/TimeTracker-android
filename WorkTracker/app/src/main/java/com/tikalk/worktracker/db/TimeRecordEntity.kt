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

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.tikalk.worktracker.model.Converters
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.TikalEntity.Companion.ID_NONE
import com.tikalk.worktracker.model.time.TaskRecordStatus
import com.tikalk.worktracker.model.time.TimeRecord
import kotlinx.parcelize.Parcelize
import java.util.Calendar

/**
 * Time record database entity.
 *
 * @author Moshe Waisberg.
 */
@Entity(
    tableName = "record",
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
    indices = [Index("project_id"), Index("task_id")]
)
@TypeConverters(TimeRecordConverters::class)
@Parcelize
open class TimeRecordEntity(
    @ColumnInfo(name = "id")
    @PrimaryKey(autoGenerate = false)
    override var id: Long,
    @ColumnInfo(name = "project_id")
    var projectId: Long,
    @ColumnInfo(name = "task_id")
    var taskId: Long,
    @ColumnInfo(name = "date")
    var date: Calendar,
    @ColumnInfo(name = "start")
    var start: Calendar? = null,
    @ColumnInfo(name = "finish")
    var finish: Calendar? = null,
    @ColumnInfo(name = "duration")
    var duration: Long = 0L,
    @ColumnInfo(name = "note")
    var note: String = "",
    @ColumnInfo(name = "cost")
    var cost: Double = 0.0,
    @ColumnInfo(name = "status")
    var status: TaskRecordStatus = TaskRecordStatus.DRAFT
) : TikalEntity(id), Parcelable

open class TimeRecordConverters : Converters() {
    @TypeConverter
    fun fromRecordStatus(value: TaskRecordStatus): Int = value.ordinal

    @TypeConverter
    fun toRecordStatus(value: Int): TaskRecordStatus = TaskRecordStatus.values()[value]
}

fun TimeRecord.toTimeRecordEntity(): TimeRecordEntity =
    TimeRecordEntity(
        id = this.id,
        projectId = this.project.id,
        taskId = this.task.id,
        start = this.start,
        finish = this.finish,
        date = this.date,
        duration = this.duration,
        note = this.note,
        cost = this.cost,
        status = this.status
    )

fun TimeRecordEntity.toTimeRecord(
    projects: Collection<Project>? = null,
    tasks: Collection<ProjectTask>? = null
): TimeRecord {
    val value: TimeRecordEntity = this
    val project = projects?.find { it.id == value.projectId }
        ?: Project.EMPTY.copy().apply { id = value.projectId }
    val projectTasks = project.tasks
    val task = (tasks ?: projectTasks).find { it.id == value.taskId }
        ?: ProjectTask.EMPTY.copy().apply { id = value.taskId }

    if ((projects == null) and (project.id != ID_NONE) and project.name.isEmpty()) {
        project.name = "project"
    }
    if ((tasks == null) and (task.id != ID_NONE) and task.name.isEmpty()) {
        task.name = "task"
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
