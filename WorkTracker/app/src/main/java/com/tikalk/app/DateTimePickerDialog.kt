/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2020, Tikal Knowledge, Ltd.
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

package com.tikalk.app

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.widget.TabHost
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import com.tikalk.widget.DateTimePicker
import com.tikalk.worktracker.R
import java.util.Calendar

class DateTimePickerDialog private constructor(
    context: Context,
    @StyleRes themeResId: Int,
    listener: OnDateTimeSetListener?,
    calendar: Calendar?,
    year: Int, monthOfYear: Int, dayOfMonth: Int,
    hourOfDay: Int, minute: Int, is24HourView: Boolean
) : AlertDialog(context, themeResId), DialogInterface.OnClickListener,
    DateTimePicker.OnDateTimeChangedListener {

    var dateTimePicker: DateTimePicker

    var dateTimeSetListener: OnDateTimeSetListener? = listener

    constructor(context: Context) :
        this(
            context,
            0,
            null,
            Calendar.getInstance(),
            0,
            0,
            0,
            0,
            0,
            DateFormat.is24HourFormat(context)
        )

    constructor(context: Context, listener: OnDateTimeSetListener?) :
        this(
            context,
            0,
            listener,
            Calendar.getInstance(),
            0,
            0,
            0,
            0,
            0,
            DateFormat.is24HourFormat(context)
        )

    constructor(
        context: Context,
        listener: OnDateTimeSetListener?,
        year: Int, monthOfYear: Int, dayOfMonth: Int,
        hourOfDay: Int, minute: Int, is24HourView: Boolean
    ) : this(
        context,
        0,
        listener,
        null,
        year,
        monthOfYear,
        dayOfMonth,
        hourOfDay,
        minute,
        is24HourView
    )

    init {
        val themeContext = getContext()
        val inflater = LayoutInflater.from(themeContext)
        val view: View = inflater.inflate(R.layout.date_time_picker_dialog, null)
        setView(view)

        dateTimePicker = view.findViewById(R.id.dateTimePicker)
        dateTimePicker.setIs24HourView(is24HourView)

        if (calendar != null) {
            val calYear = calendar[Calendar.YEAR]
            val calMonthOfYear = calendar[Calendar.MONTH]
            val calDayOfMonth = calendar[Calendar.DAY_OF_MONTH]
            val calHour = calendar[Calendar.HOUR_OF_DAY]
            val calMinute = calendar[Calendar.MINUTE]
            dateTimePicker.init(calYear, calMonthOfYear, calDayOfMonth, calHour, calMinute, this)
        } else {
            dateTimePicker.init(year, monthOfYear, dayOfMonth, hourOfDay, minute, this)
        }

        val tabs: TabHost = view.findViewById(android.R.id.tabhost)
        tabs.setup()

        val tabTime = tabs.newTabSpec(TAG_TIME)
        tabTime.setIndicator(null, ContextCompat.getDrawable(context, R.drawable.ic_date_time_time))
        tabTime.setContent(R.id.timePicker)
        tabs.addTab(tabTime)

        val tabDate = tabs.newTabSpec(TAG_DATE)
        tabDate.setIndicator(null, ContextCompat.getDrawable(context, R.drawable.ic_date_time_date))
        tabDate.setContent(R.id.datePicker)
        tabs.addTab(tabDate)

        setButton(
            DialogInterface.BUTTON_POSITIVE,
            themeContext.getString(android.R.string.ok),
            this
        )
        setButton(
            DialogInterface.BUTTON_NEGATIVE,
            themeContext.getString(android.R.string.cancel),
            this
        )
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        val listener = dateTimeSetListener
        when (which) {
            DialogInterface.BUTTON_POSITIVE -> if (listener != null) {
                // Clearing focus forces the dialog to commit any pending
                // changes, e.g. typed text in a NumberPicker.
                dateTimePicker.clearFocus()
                listener.onDateTimeSet(
                    dateTimePicker, dateTimePicker.getYear(),
                    dateTimePicker.getMonth(), dateTimePicker.getDayOfMonth(),
                    dateTimePicker.getHour(), dateTimePicker.getMinute()
                )
            }
            DialogInterface.BUTTON_NEGATIVE -> cancel()
        }
    }

    override fun onDateTimeChanged(
        view: DateTimePicker,
        year: Int,
        monthOfYear: Int,
        dayOfMonth: Int,
        hourOfDay: Int,
        minute: Int
    ) {
        dateTimePicker.init(year, monthOfYear, dayOfMonth, hourOfDay, minute, this)
    }

    /**
     * Sets the current date.
     *
     * @param year the year
     * @param month the month (0-11 for compatibility with
     * [Calendar.MONTH])
     * @param dayOfMonth the day of month (1-31, depending on month)
     */
    fun updateDate(year: Int, month: Int, dayOfMonth: Int) {
        dateTimePicker.updateDate(year, month, dayOfMonth)
    }

    /**
     * Sets the current time.
     *
     * @param hourOfDay The current hour within the day.
     * @param minuteOfHour The current minute within the hour.
     */
    fun updateTime(hourOfDay: Int, minuteOfHour: Int) {
        dateTimePicker.updateTime(hourOfDay, minuteOfHour)
    }

    fun updateDateTime(year: Int, month: Int, dayOfMonth: Int, hour: Int, minute: Int) {
        updateDate(year, month, dayOfMonth)
        updateTime(hour, minute)
    }

    override fun onSaveInstanceState(): Bundle {
        val state = super.onSaveInstanceState()
        state.putInt(YEAR, dateTimePicker.getYear())
        state.putInt(MONTH, dateTimePicker.getMonth())
        state.putInt(DAY, dateTimePicker.getDayOfMonth())
        state.putInt(HOUR, dateTimePicker.getHour())
        state.putInt(MINUTE, dateTimePicker.getMinute())
        state.putBoolean(IS_24_HOUR, dateTimePicker.is24HourView())
        return state
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val year = savedInstanceState.getInt(YEAR)
        val month = savedInstanceState.getInt(MONTH)
        val day = savedInstanceState.getInt(DAY)
        val hour = savedInstanceState.getInt(HOUR)
        val minute = savedInstanceState.getInt(MINUTE)
        dateTimePicker.setIs24HourView(savedInstanceState.getBoolean(IS_24_HOUR))
        dateTimePicker.init(year, month, day, hour, minute, this)
    }

    /**
     * The listener used to indicate the user has finished selecting a date.
     */
    interface OnDateTimeSetListener {
        /**
         * @param view the picker associated with the dialog
         * @param year the selected year
         * @param month the selected month (0-11 for compatibility with
         * [Calendar.MONTH])
         * @param dayOfMonth the selected day of the month (1-31, depending on
         * month)
         * @param hourOfDay the hour that was set
         * @param minute the minute that was set
         */
        fun onDateTimeSet(
            view: DateTimePicker,
            year: Int,
            month: Int,
            dayOfMonth: Int,
            hourOfDay: Int,
            minute: Int
        )
    }

    companion object {
        private const val YEAR = "year"
        private const val MONTH = "month"
        private const val DAY = "day"
        private const val HOUR = "hour"
        private const val MINUTE = "minute"
        private const val IS_24_HOUR = "is24hour"

        private const val TAG_DATE = "tab:date"
        private const val TAG_TIME = "tab:time"
    }
}