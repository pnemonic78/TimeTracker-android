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
package com.tikalk.worktracker.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

/**
 * Project-Task relational ID entity.
 *
 * @author Moshe Waisberg.
 */
@Entity(tableName = "project_task_key",
    foreignKeys = [
        ForeignKey(entity = Project::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("project_id")),
        ForeignKey(entity = ProjectTask::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("task_id"))
    ])
data class ProjectTaskKey(
    @ColumnInfo(name = "project_id")
    var projectId: Long,
    @ColumnInfo(name = "task_id")
    var taskId: Long
) : TikalEntity() {

    fun isEmpty(): Boolean {
        return (id == 0L) || (projectId == 0L) || (taskId == 0L)
    }

    override fun equals(other: Any?): Boolean {
        if (other is ProjectTaskKey) {
            return (this.projectId == other.projectId) and (this.taskId == other.taskId)
        }
        return super.equals(other)
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun ProjectTaskKey?.isNullOrEmpty(): Boolean {
    return this == null || this.isEmpty()
}
