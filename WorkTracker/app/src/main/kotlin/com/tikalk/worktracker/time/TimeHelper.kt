package com.tikalk.worktracker.time

import android.text.format.DateFormat

const val DATE_PATTERN = "yyyy-MM-dd"

fun formatDate(date: Long) = DateFormat.format(DATE_PATTERN, date).toString()