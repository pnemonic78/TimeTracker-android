/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2024, Tikal Knowledge, Ltd.
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

package com.tikalk.time

import java.util.Calendar

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

var Calendar.dayOfWeek: Int
    get() {
        return get(Calendar.DAY_OF_WEEK)
    }
    set(value) {
        set(Calendar.DAY_OF_WEEK, value)
    }

var Calendar.dayOfYear: Int
    get() {
        return get(Calendar.DAY_OF_YEAR)
    }
    set(value) {
        set(Calendar.DAY_OF_YEAR, value)
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

fun Calendar.copy(): Calendar {
    return clone() as Calendar
}

fun Long.toCalendar(): Calendar {
    val cal = Calendar.getInstance()
    cal.timeInMillis = this
    return cal
}

fun Calendar.setToStartOfDay(): Calendar {
    this.hourOfDay = 0
    this.minute = 0
    this.second = 0
    this.millis = 0
    return this
}

fun Calendar.setToEndOfDay(): Calendar {
    this.hourOfDay = getActualMaximum(Calendar.HOUR_OF_DAY)
    this.minute = getActualMaximum(Calendar.MINUTE)
    this.second = getActualMaximum(Calendar.SECOND)
    this.millis = getActualMaximum(Calendar.MILLISECOND)
    return this
}

fun toCalendar(
    year: Int,
    month: Int,
    dayOfMonth: Int,
    hourOfDay: Int = 0,
    minute: Int = 0
): Calendar {
    return Calendar.getInstance().apply {
        this.year = year
        this.month = month
        this.dayOfMonth = dayOfMonth
        this.hourOfDay = hourOfDay
        this.minute = minute
    }
}

