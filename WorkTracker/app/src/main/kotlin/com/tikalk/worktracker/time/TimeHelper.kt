package com.tikalk.worktracker.time

import android.text.format.DateFormat
import java.util.*

const val DATE_PATTERN = "yyyy-MM-dd"

fun formatSystemDate(date: Long = System.currentTimeMillis()): String = DateFormat.format(DATE_PATTERN, date).toString()

fun formatSystemDate(date: Date?): String = if (date == null) "" else formatSystemDate(date.time)
