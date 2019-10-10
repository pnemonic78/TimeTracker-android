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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.TikalEntity

/**
 * Project-Task relational ID entity.
 *
 * @author Moshe Waisberg.
 */
@Entity(tableName = "project_task_key",
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
data class ProjectTaskKey(
    @ColumnInfo(name = "project_id")
    private var _projectId: Long,
    @ColumnInfo(name = "task_id")
    private var _taskId: Long
) : TikalEntity() {

    init {
        updateId()
    }

    var projectId: Long
        get() = _projectId
        set(value) {
            _projectId = value
            updateId()
        }

    var taskId: Long
        get() = _taskId
        set(value) {
            _taskId = value
            updateId()
        }

    fun isEmpty(): Boolean {
        return (projectId == ID_NONE) || (taskId == ID_NONE)
    }

    override fun equals(other: Any?): Boolean {
        if (other is ProjectTaskKey) {
            return (this.projectId == other.projectId) and (this.taskId == other.taskId)
        }
        return super.equals(other)
    }

    private fun updateId() {
        id = ((projectId and 0xFFFFFFFF) shl 32) + (taskId and 0xFFFFFFFF)
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun ProjectTaskKey?.isNullOrEmpty(): Boolean {
    return this == null || this.isEmpty()
}
