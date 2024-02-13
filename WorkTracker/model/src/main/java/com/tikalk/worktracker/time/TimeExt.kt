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
package com.tikalk.worktracker.time

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import com.tikalk.time.copy
import com.tikalk.time.hourOfDay
import com.tikalk.time.millis
import com.tikalk.time.minute
import com.tikalk.time.second
import com.tikalk.util.TikalFormatter
import com.tikalk.worktracker.model.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

const val SYSTEM_DATE_PATTERN = "yyyy-MM-dd"
const val SYSTEM_TIME_PATTERN = "HH:mm"
const val SYSTEM_HOURS_PATTERN = "HH:mm"

fun formatSystemDate(date: Long = System.currentTimeMillis()): String =
    DateFormat.format(SYSTEM_DATE_PATTERN, date).toString()

fun formatSystemDate(date: Date?): String? =
    if (date == null) null else DateFormat.format(SYSTEM_DATE_PATTERN, date).toString()

fun formatSystemDate(date: Calendar?): String? =
    if (date == null) null else DateFormat.format(SYSTEM_DATE_PATTERN, date).toString()

fun formatSystemTime(time: Long = System.currentTimeMillis()): String =
    DateFormat.format(SYSTEM_TIME_PATTERN, time).toString()

fun formatSystemTime(time: Date?): String? =
    if (time == null) null else DateFormat.format(SYSTEM_TIME_PATTERN, time).toString()

fun formatSystemTime(time: Calendar?): String? =
    if (time == null) null else DateFormat.format(SYSTEM_TIME_PATTERN, time).toString()

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

@Suppress("DEPRECATION")
fun parseSystemTime(date: Calendar, time: String?): Calendar? {
    if (time.isNullOrEmpty()) {
        return null
    }
    val parsed = SimpleDateFormat(SYSTEM_TIME_PATTERN, Locale.US).parse(time)
    if (parsed != null) {
        val cal = date.copy()
        cal.hourOfDay = parsed.hours
        cal.minute = parsed.minutes
        cal.second = parsed.seconds
        cal.millis = 0
        return cal
    }
    return null
}

fun parseSystemDate(date: String?): Calendar? {
    if (date.isNullOrEmpty()) {
        return null
    }
    val parsed = SimpleDateFormat(SYSTEM_DATE_PATTERN, Locale.US).parse(date)
    if (parsed != null) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = parsed.time
        return cal
    }
    return null
}

fun parseDuration(time: String?): Long? {
    if (time.isNullOrEmpty()) {
        return null
    }
    val dateFormat = SimpleDateFormat(SYSTEM_HOURS_PATTERN, Locale.US)
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    val parsed = dateFormat.parse(time)
    return parsed?.time
}

fun formatDuration(elapsedMs: Long): String {
    val dateFormat = SimpleDateFormat(SYSTEM_HOURS_PATTERN, Locale.US)
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    return dateFormat.format(Date(elapsedMs))
}

private var sElapsedFormatHMM: String? = null

private const val SECONDS_FOR_MINUTE = DateUtils.MINUTE_IN_MILLIS / 2L

fun formatElapsedTime(context: Context, formatter: TikalFormatter, elapsedMs: Long): String {
    val hours = elapsedMs / DateUtils.HOUR_IN_MILLIS
    val seconds = elapsedMs % DateUtils.HOUR_IN_MILLIS
    var minutes = seconds / DateUtils.MINUTE_IN_MILLIS

    if ((SECONDS_FOR_MINUTE < seconds) && (seconds < DateUtils.MINUTE_IN_MILLIS)) {
        minutes++
    }

    var format = sElapsedFormatHMM
    if (format == null) {
        format = context.getString(R.string.elapsed_time_short_format_h_mm)
        sElapsedFormatHMM = format
    }
    return formatter.format(format, hours, minutes)
}

fun formatCurrency(formatter: TikalFormatter, amount: Double): String {
    return formatter.format("%.2f", amount)
}
