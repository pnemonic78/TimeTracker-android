/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2020, Tikal Knowledge, Ltd.
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

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * Where was the task done?
 *
 * @author Moshe Waisberg.
 */
@Entity(tableName = "location")
data class Location(
    @ColumnInfo(name = "name")
    var name: String,
    @ColumnInfo(name = "description")
    var description: String? = null
) : TikalEntity() {

    constructor(id: Long, name: String) : this(name, "") {
        this.id = id
    }

    override fun toString(): String {
        return name
    }

    fun compareTo(that: Location): Int {
        return this.id.compareTo(that.id)
    }

    companion object {
        val EMPTY = Location(ID_NONE, "")
        val CLIENT = Location(11L, "client")
        val HOME = Location(10L, "home")
        val OTHER = Location(13L, "other")
        val TIKAL = Location(12L, "tikal")

        val values = arrayOf(EMPTY, CLIENT, HOME, OTHER, TIKAL)

        fun valueOf(id: Long): Location {
            return values.firstOrNull { id == it.id } ?: OTHER
        }

        fun valueOf(value: Boolean): Location {
            return if (value) HOME else CLIENT
        }
    }
}
