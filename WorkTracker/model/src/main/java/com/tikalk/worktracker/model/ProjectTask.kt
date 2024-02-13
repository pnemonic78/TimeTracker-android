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
import com.tikalk.util.compareTo

/**
 * Task that belongs to a project entity.
 *
 * @author Moshe Waisberg.
 */
@Entity(tableName = "project_task")
data class ProjectTask(
    @ColumnInfo(name = "name")
    var name: String,
    @ColumnInfo(name = "description")
    var description: String? = null
) : TikalEntity() {

    override fun toString(): String {
        return name
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other) || ((other is ProjectTask) && (this.id == other.id))
    }

    fun isEmpty(): Boolean {
        return (id == ID_NONE) || name.isEmpty()
    }

    fun compareTo(that: ProjectTask): Int {
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
        val EMPTY = ProjectTask(name = "")
    }
}

fun ProjectTask?.isNullOrEmpty(): Boolean {
    return this == null || this.isEmpty()
}

fun findTask(tasks: List<ProjectTask>, task: ProjectTask): Int {
    val index = tasks.indexOf(task)
    if (index >= 0) {
        return index
    }
    val id = task.id
    for (i in tasks.indices) {
        val t = tasks[i]
        if (t == task) {
            return i
        }
        if (t.id == id) {
            return i
        }
    }
    return AdapterView.INVALID_POSITION
}

fun findTask(tasks: Array<ProjectTask>, task: ProjectTask): Int {
    val index = tasks.indexOf(task)
    if (index >= 0) {
        return index
    }
    val id = task.id
    for (i in tasks.indices) {
        val t = tasks[i]
        if (t == task) {
            return i
        }
        if (t.id == id) {
            return i
        }
    }
    return AdapterView.INVALID_POSITION
}
