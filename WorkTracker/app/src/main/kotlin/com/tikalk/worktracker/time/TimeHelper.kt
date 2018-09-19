package com.tikalk.worktracker.time

import android.text.format.DateFormat
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
