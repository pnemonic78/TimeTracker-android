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
package com.tikalk.worktracker.model.time

import androidx.room.Entity
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.User
import java.util.*

/**
 * Time record entity. Represents some work done for a project task.
 *
 * @author Moshe Waisberg.
 */
@Entity
data class TimeRecord(
    var user: User,
    var project: Project,
    var task: ProjectTask,
    var start: Calendar? = null,
    var finish: Calendar? = null,
    var note: String = "",
    var status: TaskRecordStatus = TaskRecordStatus.INSERTED,
    override var id: Long = 0
) : TikalEntity(id) {

    var startTime: Long
        get() = start?.timeInMillis ?: 0L
        set(value) {
            val cal = start ?: Calendar.getInstance()
            cal.timeInMillis = value
            start = cal
        }
    var finishTime: Long
        get() = finish?.timeInMillis ?: 0L
        set(value) {
            val cal = finish ?: Calendar.getInstance()
            cal.timeInMillis = value
            finish = cal
        }

    fun isEmpty(): Boolean {
        return user.username.isEmpty()
            || (project.id <= 0L)
            || (task.id <= 0L)
            || (startTime <= 0L)
    }
}
