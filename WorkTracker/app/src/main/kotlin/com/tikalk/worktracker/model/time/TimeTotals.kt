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

import android.os.Parcel
import android.os.Parcelable

/**
 * Totals.
 *
 * @author Moshe Waisberg.
 */
data class TimeTotals(
    var daily: Long = 0,
    var weekly: Long = 0,
    var monthly: Long = 0,
    var remaining: Long = 0
) : Parcelable {
    constructor(parcel: Parcel) : this() {
        daily = parcel.readLong()
        weekly = parcel.readLong()
        monthly = parcel.readLong()
        remaining = parcel.readLong()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(daily)
        parcel.writeLong(weekly)
        parcel.writeLong(monthly)
        parcel.writeLong(remaining)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun clear() {
        daily = 0
        weekly = 0
        monthly = 0
        remaining = 0
    }

    companion object CREATOR : Parcelable.Creator<TimeTotals> {
        override fun createFromParcel(parcel: Parcel): TimeTotals {
            return TimeTotals(parcel)
        }

        override fun newArray(size: Int): Array<TimeTotals?> {
            return arrayOfNulls(size)
        }
    }
}