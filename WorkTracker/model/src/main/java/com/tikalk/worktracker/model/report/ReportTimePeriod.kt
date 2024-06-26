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
package com.tikalk.worktracker.model.report

import androidx.annotation.StringRes
import com.tikalk.worktracker.model.R

/**
 * Time period for report filter.
 *
 * @author Moshe Waisberg.
 */
enum class ReportTimePeriod(val value: String, @StringRes val labelId: Int) {

    /** Custom (start, finish). */
    CUSTOM("", R.string.period_custom),

    /** Today. */
    TODAY("1", R.string.period_today),

    /** This week. */
    THIS_WEEK("2", R.string.period_this_week),

    /** This month. */
    THIS_MONTH("3", R.string.period_this_month),

    /** The previous week. */
    PREVIOUS_WEEK("6", R.string.period_previous_week),

    /** The previous month. */
    PREVIOUS_MONTH("7", R.string.period_previous_month),

    /** Yesterday. */
    YESTERDAY("8", R.string.period_yesterday);

    override fun toString(): String {
        return value
    }
}

val DefaultTimePeriod = ReportTimePeriod.THIS_MONTH
