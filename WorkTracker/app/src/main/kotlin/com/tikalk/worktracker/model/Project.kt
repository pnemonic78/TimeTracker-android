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
import androidx.room.Ignore

/**
 * Project entity.
 *
 * @author Moshe Waisberg.
 */
@Entity(tableName = "project")
data class Project(
    @ColumnInfo(name = "name")
    var name: String,
    @ColumnInfo(name = "description")
    var description: String? = null
) : TikalEntity() {

    override var id: Long
        get() = super.id
        set(value) {
            super.id = value
            for (task in tasks.values) {
                task.projectId = value
            }
        }

    @Ignore
    val tasks: MutableMap<Long, ProjectTaskKey> = HashMap()

    val taskIds: Set<Long>
        get() = tasks.keys

    override fun toString(): String {
        return name
    }

    fun isEmpty(): Boolean {
        return (id == 0L) || name.isEmpty()
    }

    fun clearTasks() {
        tasks.clear()
    }

    fun addTask(taskId: Long) {
        addKey(ProjectTaskKey(id, taskId))
    }

    fun addTasks(taskIds: List<Long>) {
        for (id in taskIds) {
            addTask(id)
        }
    }

    fun addKey(key: ProjectTaskKey) {
        if (key.projectId <= 0L) key.projectId = id
        tasks[key.taskId] = key
    }

    fun addKeys(keys: List<ProjectTaskKey>) {
        for (key in keys) {
            addKey(key)
        }
    }

    companion object {
        val EMPTY = Project("")
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun Project?.isNullOrEmpty(): Boolean {
    return this == null || this.isEmpty()
}
