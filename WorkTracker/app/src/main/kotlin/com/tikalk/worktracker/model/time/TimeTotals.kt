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
package com.tikalk.worktracker.model.time

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Ignore
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * Totals for the day.
 *
 * @author Moshe Waisberg.
 */
@Parcelize
data class TimeTotals(
    @ColumnInfo(name = "daily")
    var daily: Long = UNKNOWN,
    @ColumnInfo(name = "weekly")
    var weekly: Long = UNKNOWN,
    @ColumnInfo(name = "monthly")
    var monthly: Long = UNKNOWN,
    @ColumnInfo(name = "remaining")
    var remaining: Long = UNKNOWN
) : Parcelable {

    @IgnoredOnParcel
    @Ignore
    var status: TaskRecordStatus = TaskRecordStatus.DRAFT

    fun clear(unknown: Boolean = true) {
        if (unknown) {
            daily = UNKNOWN
            weekly = UNKNOWN
            monthly = UNKNOWN
            remaining = UNKNOWN
        } else {
            daily = 0
            weekly = 0
            monthly = 0
            remaining = 0
        }
    }

    companion object {
        const val UNKNOWN = Long.MIN_VALUE
    }
}