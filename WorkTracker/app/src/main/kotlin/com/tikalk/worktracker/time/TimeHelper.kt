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
package com.tikalk.worktracker.time

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import com.tikalk.worktracker.R
import java.text.SimpleDateFormat
import java.util.*

const val SYSTEM_DATE_PATTERN = "yyyy-MM-dd"
const val SYSTEM_TIME_PATTERN = "HH:mm"

fun formatSystemDate(date: Long = System.currentTimeMillis()): String = DateFormat.format(SYSTEM_DATE_PATTERN, date).toString()

fun formatSystemDate(date: Date?): String = if (date == null) "" else formatSystemDate(date.time)

fun formatSystemDate(date: Calendar?): String = if (date == null) "" else formatSystemDate(date.timeInMillis)

fun formatSystemTime(time: Long = System.currentTimeMillis()): String = DateFormat.format(SYSTEM_TIME_PATTERN, time).toString()

fun formatSystemTime(time: Date?): String = if (time == null) "" else formatSystemTime(time.time)

fun formatSystemTime(time: Calendar?): String = if (time == null) "" else formatSystemTime(time.timeInMillis)

fun parseSystemTime(date: Long, time: String?): Calendar? {
    if (time.isNullOrEmpty()) {
        return null
    }
    val cal = Calendar.getInstance()
    cal.timeInMillis = date
    return parseSystemTime(cal, time)
}

fun parseSystemTime(date: Date, time: String?): Calendar? {
    if (time.isNullOrEmpty()) {
        return null
    }
    val cal = Calendar.getInstance()
    cal.time = date
    return parseSystemTime(cal, time)
}

fun parseSystemTime(date: Calendar, time: String?): Calendar? {
    if (time.isNullOrEmpty()) {
        return null
    }
    val cal = date.clone() as Calendar
    val parsed = SimpleDateFormat(SYSTEM_TIME_PATTERN, Locale.US).parse(time)
    cal.set(Calendar.HOUR_OF_DAY, parsed.hours)
    cal.set(Calendar.MINUTE, parsed.minutes)
    cal.set(Calendar.SECOND, parsed.seconds)
    return cal
}

var Calendar.era: Int
    get() {
        return get(Calendar.ERA)
    }
    set(value) {
        set(Calendar.ERA, value)
    }

var Calendar.year: Int
    get() {
        return get(Calendar.YEAR)
    }
    set(value) {
        set(Calendar.YEAR, value)
    }

var Calendar.month: Int
    get() {
        return get(Calendar.MONTH)
    }
    set(value) {
        set(Calendar.MONTH, value)
    }

var Calendar.dayOfMonth: Int
    get() {
        return get(Calendar.DAY_OF_MONTH)
    }
    set(value) {
        set(Calendar.DAY_OF_MONTH, value)
    }

var Calendar.hourOfDay: Int
    get() {
        return get(Calendar.HOUR_OF_DAY)
    }
    set(value) {
        set(Calendar.HOUR_OF_DAY, value)
    }

var Calendar.hour: Int
    get() {
        return get(Calendar.HOUR)
    }
    set(value) {
        set(Calendar.HOUR, value)
    }

var Calendar.minute: Int
    get() {
        return get(Calendar.MINUTE)
    }
    set(value) {
        set(Calendar.MINUTE, value)
    }

var Calendar.second: Int
    get() {
        return get(Calendar.SECOND)
    }
    set(value) {
        set(Calendar.SECOND, value)
    }

var Calendar.millis: Int
    get() {
        return get(Calendar.MILLISECOND)
    }
    set(value) {
        set(Calendar.MILLISECOND, value)
    }

fun Calendar.isSameDay(that: Calendar): Boolean {
    return (this.era == that.era)
        && (this.year == that.year)
        && (this.month == that.month)
        && (this.dayOfMonth == that.dayOfMonth)
}

private var sElapsedFormatHMM: String? = null

fun formatElapsedTime(context: Context, formatter: Formatter, elapsedMs: Long): Formatter {
    // Break the elapsed seconds into hours, minutes, and seconds.
    val hours = elapsedMs / DateUtils.HOUR_IN_MILLIS
    val minutes = (elapsedMs % DateUtils.HOUR_IN_MILLIS) / DateUtils.MINUTE_IN_MILLIS

    var format = sElapsedFormatHMM
    if (format == null) {
        format = context.getString(R.string.elapsed_time_short_format_h_mm)
        sElapsedFormatHMM = format
    }
    return formatter.format(format, hours, minutes)
}
