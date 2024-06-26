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
package com.tikalk.worktracker.model

import android.widget.AdapterView
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import com.tikalk.util.compareTo

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

    @Ignore
    val tasksById: MutableMap<Long, ProjectTask> = HashMap()

    var tasks: List<ProjectTask>
        get() = tasksById.values.sortedBy { task -> task.name }
        set(value) {
            clearTasks()
            addTasks(value)
        }

    override fun toString(): String {
        return name
    }

    fun isEmpty(): Boolean {
        return (id == ID_NONE) || name.isEmpty()
    }

    fun clearTasks() {
        tasksById.clear()
    }

    fun addTask(task: ProjectTask) {
        tasksById[task.id] = task
    }

    fun addTasks(tasks: Collection<ProjectTask>) {
        for (task in tasks) {
            addTask(task)
        }
    }

    fun copy(withTasks: Boolean): Project {
        val clone = this.copy()
        if (withTasks) {
            clone.tasks = this.tasks.map { it.copy() }
        }
        return clone
    }

    fun compareTo(that: Project): Int {
        val id1 = this.id
        val id2 = that.id
        var c = id1.compareTo(id2)
        if (c != 0) return c

        val n1 = this.name
        val n2 = that.name
        c = n1.compareTo(n2)
        if (c != 0) return c

        val d1 = this.description
        val d2 = that.description
        c = d1.compareTo(d2)
        if (c != 0) return c

        val v1 = this.version
        val v2 = that.version
        c = v1.compareTo(v2)
        if (c != 0) return c

        return 0
    }

    companion object {
        val EMPTY = Project(name = "")

        init {
            EMPTY.addTask(ProjectTask.EMPTY)
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun Project?.isNullOrEmpty(): Boolean {
    return this == null || this.isEmpty()
}

fun findProject(projects: List<Project>, project: Project): Int {
    val index = projects.indexOf(project)
    if (index < 0) {
        val id = project.id
        for (i in projects.indices) {
            val p = projects[i]
            if (p == project) {
                return i
            }
            if (p.id == id) {
                return i
            }
        }
    }
    return index
}

fun findProject(projects: Array<Project>, project: Project): Int {
    val index = projects.indexOf(project)
    if (index >= 0) {
        return index
    }
    val id = project.id
    for (i in projects.indices) {
        val p = projects[i]
        if (p == project) {
            return i
        }
        if (p.id == id) {
            return i
        }
    }
    return AdapterView.INVALID_POSITION
}
