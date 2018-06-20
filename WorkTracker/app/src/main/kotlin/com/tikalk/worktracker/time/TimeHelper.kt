package com.tikalk.worktracker.time

import android.text.format.DateFormat
import java.util.*

const val SYSTEM_DATE_PATTERN = "yyyy-MM-dd"
const val SYSTEM_TIME_PATTERN = "HH:mm"

fun formatSystemDate(date: Long = System.currentTimeMillis()): String = DateFormat.format(SYSTEM_DATE_PATTERN, date).toString()

fun formatSystemDate(date: Date?): String = if (date == null) "" else formatSystemDate(date.time)

fun formatSystemDate(date: Calendar?): String = if (date == null) "" else formatSystemDate(date.timeInMillis)

fun formatSystemTime(time: Long = System.currentTimeMillis()): String = DateFormat.format(SYSTEM_TIME_PATTERN, time).toString()

fun formatSystemTime(time: Date?): String = if (time == null) "" else formatSystemDate(time.time)

fun formatSystemTime(time: Calendar?): String = if (time == null) "" else formatSystemDate(time.timeInMillis)