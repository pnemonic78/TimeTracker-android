/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2022, Tikal Knowledge, Ltd.
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

package com.tikalk.worktracker.report

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tikalk.compose.TikalTheme
import com.tikalk.worktracker.R
import com.tikalk.worktracker.model.time.ReportTotals
import com.tikalk.worktracker.time.formatCurrency
import com.tikalk.worktracker.time.formatElapsedTime
import java.util.*

private val timeBuffer = StringBuilder(20)
private val timeFormatter: Formatter = Formatter(timeBuffer, Locale.getDefault())
private val currencyBuffer = StringBuilder(20)
private val currencyFormatter = Formatter(currencyBuffer, Locale.getDefault())

@Composable
fun ReportTotalsFooter(
    totals: ReportTotals,
    isDurationFieldVisible: Boolean = true,
    isCostFieldVisible: Boolean = false
) {
    val context: Context = LocalContext.current
    timeBuffer.clear()
    currencyBuffer.clear()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp, end = 16.dp)
    ) {
        if (isDurationFieldVisible) {
            Text(
                modifier = Modifier.padding(start = 16.dp),
                text = stringResource(id = R.string.duration_total),
                style = MaterialTheme.typography.body2
            )
            Text(
                modifier = Modifier.padding(start = 4.dp),
                text = formatElapsedTime(context, timeFormatter, totals.duration),
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Medium
            )
        }
        if (isCostFieldVisible) {
            Text(
                modifier = Modifier.padding(start = 16.dp),
                text = stringResource(id = R.string.cost_total),
                style = MaterialTheme.typography.body2
            )
            Text(
                modifier = Modifier.padding(start = 4.dp),
                text = formatCurrency(currencyFormatter, totals.cost),
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ThisPreview() {
    val totals = ReportTotals(DateUtils.WEEK_IN_MILLIS, 1.23)
    TikalTheme {
        Surface {
            ReportTotalsFooter(totals, isCostFieldVisible = true)
        }
    }
}