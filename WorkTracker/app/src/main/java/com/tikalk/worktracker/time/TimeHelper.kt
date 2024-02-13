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

import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.text.format.DateFormat
import android.text.format.DateUtils
import com.tikalk.app.DateTimePickerDialog
import com.tikalk.compose.CalendarCallback
import com.tikalk.time.dayOfMonth
import com.tikalk.time.hourOfDay
import com.tikalk.time.millis
import com.tikalk.time.minute
import com.tikalk.time.month
import com.tikalk.time.second
import com.tikalk.time.toCalendar
import com.tikalk.time.year
import com.tikalk.widget.DateTimePicker
import com.tikalk.worktracker.model.time.TimeRecord
import java.util.Calendar

const val FORMAT_TIME_BUTTON =
    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_ALL
const val FORMAT_DATE_BUTTON =
    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_WEEKDAY
const val FORMAT_DURATION_BUTTON = DateUtils.FORMAT_SHOW_TIME

fun pickDate(
    context: Context,
    date: Calendar,
    onDateSelected: CalendarCallback
) {
    val listener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
        onDateSelected(toCalendar(year, month, dayOfMonth))
    }
    DatePickerDialog(
        context,
        listener,
        date.year,
        date.month,
        date.dayOfMonth
    ).apply {
        val picker = this
        setButton(
            DialogInterface.BUTTON_NEUTRAL,
            context.getText(com.tikalk.core.R.string.today)
        ) { dialog: DialogInterface, which: Int ->
            if ((dialog == picker) and (which == DialogInterface.BUTTON_NEUTRAL)) {
                val today = Calendar.getInstance()
                listener.onDateSet(picker.datePicker, today.year, today.month, today.dayOfMonth)
            }
        }
        show()
    }
}

fun pickDateTime(
    context: Context,
    record: TimeRecord?,
    time: Long = TimeRecord.NEVER,
    onTimeSelected: TimeCallback
) {
    val cal = getCalendar(record, time)
    val year = cal.year
    val month = cal.month
    val dayOfMonth = cal.dayOfMonth
    val hour = cal.hourOfDay
    val minute = cal.minute

    val listener = object : DateTimePickerDialog.OnDateTimeSetListener {
        override fun onDateTimeSet(
            view: DateTimePicker,
            year: Int,
            month: Int,
            dayOfMonth: Int,
            hourOfDay: Int,
            minute: Int
        ) {
            val date = toCalendar(year, month, dayOfMonth, hourOfDay, minute)
            onTimeSelected(date.timeInMillis)
        }
    }
    DateTimePickerDialog(
        context,
        listener,
        year,
        month,
        dayOfMonth,
        hour,
        minute,
        DateFormat.is24HourFormat(context)
    ).show()
}

fun pickDuration(
    context: Context,
    record: TimeRecord,
    duration: Long = 0L,
    onTimeSelected: TimeCallback
) {
    val cal = getCalendar(record, TimeRecord.NEVER)
    val year = cal.year
    val month = cal.month
    val dayOfMonth = cal.dayOfMonth
    val hour = (duration / DateUtils.HOUR_IN_MILLIS).toInt()
    val minute = ((duration % DateUtils.HOUR_IN_MILLIS) / DateUtils.MINUTE_IN_MILLIS).toInt()

    val listener = object : DateTimePickerDialog.OnDateTimeSetListener {
        override fun onDateTimeSet(
            view: DateTimePicker,
            year: Int,
            month: Int,
            dayOfMonth: Int,
            hourOfDay: Int,
            minute: Int
        ) {
            val date = toCalendar(year, month, dayOfMonth, hourOfDay, minute)
            onTimeSelected(date.timeInMillis)
        }
    }
    DateTimePickerDialog(
        context,
        listener,
        year,
        month,
        dayOfMonth,
        hour,
        minute,
        true
    ).show()
}

private fun getCalendar(record: TimeRecord?, time: Long = TimeRecord.NEVER): Calendar {
    return Calendar.getInstance().apply {
        if (time != TimeRecord.NEVER) {
            timeInMillis = time
        } else if (record != null) {
            val date = record.date
            year = date.year
            month = date.month
            dayOfMonth = date.dayOfMonth
        }
        // Server granularity is seconds.
        second = 0
        millis = 0
    }
}
