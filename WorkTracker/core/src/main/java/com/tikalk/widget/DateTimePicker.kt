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

package com.tikalk.widget

import android.content.Context
import android.os.Build
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.DatePicker
import android.widget.FrameLayout
import android.widget.TimePicker
import android.widget.ViewSwitcher
import com.google.android.material.tabs.TabLayout
import com.tikalk.core.databinding.DateTimePickerBinding

/**
 * Provides a widget for selecting a date and time.
 */
class DateTimePicker(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr),
    TabLayout.OnTabSelectedListener,
    DatePicker.OnDateChangedListener,
    TimePicker.OnTimeChangedListener {

    constructor(context: Context) : this(context, null, 0)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    private val binding: DateTimePickerBinding
    private val datePicker: DatePicker
    private val timePicker: TimePicker

    private var listener: OnDateTimeChangedListener? = null

    init {
        val inflater = LayoutInflater.from(context)
        binding = DateTimePickerBinding.inflate(inflater, this, true)
        datePicker = binding.datePicker
        timePicker = binding.timePicker

        val tabhost = binding.tabhost
        tabhost.addOnTabSelectedListener(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            datePicker.setOnDateChangedListener(this)
        }
        timePicker.setOnTimeChangedListener(this)
        timePicker.setIs24HourView(DateFormat.is24HourFormat(context))
    }

    /**
     * Initialize the state. If the provided values designate an inconsistent
     * date the values are normalized before updating the spinners.
     *
     * @param year The initial year.
     * @param monthOfYear The initial month <strong>starting from zero</strong>.
     * @param dayOfMonth The initial day of the month.
     * @param hourOfDay The current hour.
     * @param minute The current minute.
     * @param listener How user is notified date is changed by user, can be null.
     */
    fun init(
        year: Int,
        monthOfYear: Int,
        dayOfMonth: Int,
        hourOfDay: Int,
        minute: Int,
        listener: OnDateTimeChangedListener?
    ) {
        this.listener = listener
        datePicker.init(year, monthOfYear, dayOfMonth, this)
        timePicker.setOnTimeChangedListener(null)
        updateTime(hourOfDay, minute)
        timePicker.setOnTimeChangedListener(this)
    }

    override fun onTabSelected(tab: TabLayout.Tab) {
        binding.tabcontent.displayedChild = tab.position
    }

    override fun onTabUnselected(tab: TabLayout.Tab) = Unit

    override fun onTabReselected(tab: TabLayout.Tab) = Unit

    fun updateDate(year: Int, month: Int, dayOfMonth: Int) {
        datePicker.updateDate(year, month, dayOfMonth)
    }

    fun updateTime(hourOfDay: Int, minuteOfHour: Int) {
        timePicker.hour = hourOfDay
        timePicker.minute = minuteOfHour
    }

    fun getYear(): Int {
        return datePicker.year
    }

    fun getMonth(): Int {
        return datePicker.month
    }

    fun getDayOfMonth(): Int {
        return datePicker.dayOfMonth
    }

    fun getHour(): Int {
        return timePicker.hour
    }

    fun getMinute(): Int {
        return timePicker.minute
    }

    fun is24HourView(): Boolean {
        return timePicker.is24HourView
    }

    fun setIs24HourView(is24HourView: Boolean) {
        timePicker.setIs24HourView(is24HourView)
    }

    override fun onDateChanged(view: DatePicker, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        listener?.onDateTimeChanged(this, year, monthOfYear, dayOfMonth, getHour(), getMinute())
    }

    override fun onTimeChanged(view: TimePicker, hourOfDay: Int, minute: Int) {
        listener?.onDateTimeChanged(this, getYear(), getMonth(), getDayOfMonth(), hourOfDay, minute)
    }

    /**
     * The callback used to indicate the user changed the date and time.
     */
    interface OnDateTimeChangedListener {
        /**
         * Called upon either a date or a time change.
         *
         * @param view The view associated with this listener.
         * @param year The year that was set.
         * @param monthOfYear The month that was set (0-11) for compatibility
         * with [java.util.Calendar].
         * @param dayOfMonth The day of the month that was set.
         * @param hourOfDay the hour that was set
         * @param minute the minute that was set
         */
        fun onDateTimeChanged(
            view: DateTimePicker,
            year: Int,
            monthOfYear: Int,
            dayOfMonth: Int,
            hourOfDay: Int,
            minute: Int
        )
    }
}