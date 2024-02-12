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

package com.tikalk.worktracker.time

import android.content.Context
import android.content.res.Configuration
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.tikalk.compose.TikalTheme
import com.tikalk.util.TikalFormatter
import com.tikalk.worktracker.R
import com.tikalk.worktracker.model.time.TimeTotals
import kotlin.math.absoluteValue
import kotlinx.coroutines.flow.Flow

@Composable
fun TimeTotalsFooter(
    totalsFlow: Flow<TimeTotals?>
) {
    val totalsState = totalsFlow.collectAsState(initial = null)
    val totals = totalsState.value ?: return
    TimeTotalsFooter(totals)
}

@Composable
fun TimeTotalsFooter(
    totals: TimeTotals
) {
    val context: Context = LocalContext.current
    val textColor = MaterialTheme.colorScheme.onBackground
    val balanceColor = if (totals.balance < 0) {
        colorResource(id = R.color.balanceNegative)
    } else {
        colorResource(id = R.color.balancePositive)
    }
    val timeFormatter = TikalFormatter()

    val configuration = LocalConfiguration.current
    if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        TimeTotalsFooterLandscape(
            totals = totals,
            context = context,
            textColor = textColor,
            balanceColor = balanceColor,
            timeFormatter = timeFormatter
        )
    } else {
        TimeTotalsFooterPortrait(
            totals = totals,
            context = context,
            textColor = textColor,
            balanceColor = balanceColor,
            timeFormatter = timeFormatter
        )
    }
}

@Composable
private fun TimeTotalsFooterPortrait(
    totals: TimeTotals,
    context: Context,
    textColor: Color,
    balanceColor: Color,
    timeFormatter: TikalFormatter
) {
    val dailyValid = totals.daily != TimeTotals.UNKNOWN
    val weeklyValid = totals.weekly != TimeTotals.UNKNOWN
    val monthlyValid = totals.monthly != TimeTotals.UNKNOWN
    val balanceValid = totals.balance != TimeTotals.UNKNOWN

    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 16.dp)
    ) {
        val (dayLabel, dayValue, monthLabel, monthValue, weekLabel, weekValue, balanceLabel, balanceValue) = createRefs()
        val firstBarrier = createEndBarrier(dayLabel, monthLabel)
        val secondBarrier = createEndBarrier(dayValue, monthValue)
        val thirdBarrier = createEndBarrier(weekLabel, balanceLabel)

        Text(
            modifier = Modifier.constrainAs(dayLabel) {
                start.linkTo(parent.start)
                top.linkTo(parent.top)
                bottom.linkTo(dayValue.bottom)
            },
            text = if (dailyValid) stringResource(id = R.string.day_total) else "",
            style = MaterialTheme.typography.bodySmall,
            color = textColor
        )
        Text(
            modifier = Modifier
                .padding(start = 4.dp)
                .constrainAs(dayValue) {
                    start.linkTo(firstBarrier)
                    top.linkTo(parent.top)
                },
            text = if (dailyValid) formatHours(context, timeFormatter, totals.daily) else "",
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            fontWeight = FontWeight.Medium
        )

        Text(
            modifier = Modifier
                .padding(top = 4.dp)
                .constrainAs(monthLabel) {
                    start.linkTo(dayLabel.start)
                    top.linkTo(dayLabel.bottom)
                    bottom.linkTo(monthValue.bottom)
                },
            text = if (monthlyValid) stringResource(id = R.string.month_total) else "",
            style = MaterialTheme.typography.bodySmall,
            color = textColor
        )
        Text(
            modifier = Modifier
                .padding(start = 4.dp, top = 4.dp)
                .constrainAs(monthValue) {
                    start.linkTo(dayValue.start)
                    top.linkTo(dayValue.bottom)
                },
            text = if (monthlyValid) formatHours(context, timeFormatter, totals.monthly) else "",
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            fontWeight = FontWeight.Medium
        )

        Text(
            modifier = Modifier
                .padding(start = 16.dp)
                .constrainAs(weekLabel) {
                    start.linkTo(secondBarrier)
                    top.linkTo(dayLabel.top)
                    bottom.linkTo(weekValue.bottom)
                },
            text = if (weeklyValid) stringResource(id = R.string.week_total) else "",
            style = MaterialTheme.typography.bodySmall,
            color = textColor
        )
        Text(
            modifier = Modifier
                .padding(start = 4.dp)
                .constrainAs(weekValue) {
                    start.linkTo(thirdBarrier)
                    top.linkTo(parent.top)
                },
            text = if (weeklyValid) formatHours(context, timeFormatter, totals.weekly) else "",
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            fontWeight = FontWeight.Medium
        )

        Text(
            modifier = Modifier
                .padding(start = 16.dp, top = 4.dp)
                .constrainAs(balanceLabel) {
                    start.linkTo(weekLabel.start)
                    top.linkTo(weekLabel.bottom)
                    bottom.linkTo(balanceValue.bottom)
                },
            text = if (balanceValid) stringResource(id = R.string.balance) else "",
            style = MaterialTheme.typography.bodySmall,
            color = textColor
        )
        Text(
            modifier = Modifier
                .padding(start = 4.dp, top = 4.dp)
                .constrainAs(balanceValue) {
                    start.linkTo(thirdBarrier)
                    top.linkTo(weekValue.bottom)
                },
            text = if (balanceValid) formatHours(
                context,
                timeFormatter,
                totals.balance.absoluteValue
            ) else "",
            style = MaterialTheme.typography.bodyMedium,
            color = balanceColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TimeTotalsFooterLandscape(
    totals: TimeTotals,
    context: Context,
    textColor: Color,
    balanceColor: Color,
    timeFormatter: TikalFormatter
) {
    val dailyValid = totals.daily != TimeTotals.UNKNOWN
    val weeklyValid = totals.weekly != TimeTotals.UNKNOWN
    val monthlyValid = totals.monthly != TimeTotals.UNKNOWN
    val balanceValid = totals.balance != TimeTotals.UNKNOWN

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dailyValid) {
            Text(
                modifier = Modifier,
                text = stringResource(id = R.string.day_total),
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )
            Text(
                modifier = Modifier
                    .padding(start = 4.dp),
                text = formatHours(context, timeFormatter, totals.daily),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = FontWeight.Medium
            )
        }

        if (weeklyValid) {
            Text(
                modifier = Modifier
                    .padding(start = 16.dp),
                text = stringResource(id = R.string.week_total),
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )
            Text(
                modifier = Modifier
                    .padding(start = 4.dp),
                text = formatHours(context, timeFormatter, totals.weekly),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = FontWeight.Medium
            )
        }

        if (monthlyValid) {
            Text(
                modifier = Modifier
                    .padding(start = 16.dp),
                text = stringResource(id = R.string.month_total),
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )
            Text(
                modifier = Modifier
                    .padding(start = 4.dp),
                text = formatHours(context, timeFormatter, totals.monthly),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = FontWeight.Medium
            )
        }

        if (balanceValid) {
            Text(
                modifier = Modifier
                    .padding(start = 16.dp),
                text = stringResource(id = R.string.balance),
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )
            Text(
                modifier = Modifier
                    .padding(start = 4.dp),
                text = formatHours(context, timeFormatter, totals.balance.absoluteValue),
                style = MaterialTheme.typography.bodyMedium,
                color = balanceColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun formatHours(context: Context, formatter: TikalFormatter, elapsedMs: Long): String {
    return if (LocalInspectionMode.current) {
        val hours = elapsedMs / DateUtils.HOUR_IN_MILLIS
        val seconds = elapsedMs % DateUtils.HOUR_IN_MILLIS
        val minutes = seconds / DateUtils.MINUTE_IN_MILLIS

        "${hours}:${minutes}"
    } else {
        formatElapsedTime(context, formatter, elapsedMs)
    }
}

@Preview(name = "default", showBackground = true, device = Devices.DEFAULT)
@Preview(
    name = "dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = Devices.DEFAULT
)
@Preview(name = "land", showBackground = true, device = Devices.TABLET)
@Preview(
    name = "land_dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = Devices.TABLET
)
@Composable
private fun ThisPreview() {
    val totals = TimeTotals(
        daily = 1 * DateUtils.HOUR_IN_MILLIS,
        weekly = 2 * DateUtils.HOUR_IN_MILLIS,
        monthly = 3 * DateUtils.HOUR_IN_MILLIS,
        balance = -4 * DateUtils.HOUR_IN_MILLIS
    )

    TikalTheme {
        TimeTotalsFooter(totals = totals)
    }
}
