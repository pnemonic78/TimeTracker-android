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
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tikalk.compose.TikalTheme
import com.tikalk.worktracker.R
import com.tikalk.worktracker.model.Location
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.report.toLocationItem
import java.util.Calendar
import java.util.Formatter

private val timeBuffer = StringBuilder(20)
private const val FORMAT_DURATION = DateUtils.FORMAT_SHOW_TIME

typealias OnRecordCallback = ((TimeRecord) -> Unit)

@Composable
fun TimeItem(
    record: TimeRecord,
    isProjectFieldVisible: Boolean = true,
    isTaskFieldVisible: Boolean = true,
    isStartFieldVisible: Boolean = true,
    isFinishFieldVisible: Boolean = true,
    isDurationFieldVisible: Boolean = true,
    isNoteFieldVisible: Boolean = true,
    isCostFieldVisible: Boolean = false,
    isLocationFieldVisible: Boolean = false,
    onClick: OnRecordCallback
) {
    val context: Context = LocalContext.current
    val iconSize = dimensionResource(id = R.dimen.icon_item)

    val dateTime = record.date.timeInMillis
    val startTime = record.startTime
    val endTime = record.finishTime
    val timeRange = if ((startTime == TimeRecord.NEVER) || (endTime == TimeRecord.NEVER)) {
        if (isStartFieldVisible || isFinishFieldVisible) {
            DateUtils.formatDateTime(context, dateTime, DateUtils.FORMAT_SHOW_DATE)
        } else {
            ""
        }
    } else if (isStartFieldVisible) {
        if (isFinishFieldVisible) {
            timeBuffer.clear()
            val timeFormatter = Formatter(timeBuffer)
            val formatterRange = DateUtils.formatDateRange(
                context,
                timeFormatter,
                startTime,
                endTime,
                FORMAT_DURATION
            )
            formatterRange.toString()
        } else {
            DateUtils.formatDateTime(context, startTime, DateUtils.FORMAT_SHOW_DATE)
        }
    } else if (isFinishFieldVisible) {
        DateUtils.formatDateTime(context, endTime, DateUtils.FORMAT_SHOW_DATE)
    } else {
        ""
    }

    val color = calculateColor(record, isSystemInDarkTheme())

    Card(
        modifier = Modifier
            .padding(4.dp)
            .clickable { onClick(record) },
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row {
                if (isProjectFieldVisible) {
                    Image(
                        modifier = Modifier
                            .size(iconSize)
                            .align(Alignment.CenterVertically),
                        painter = rememberVectorPainter(
                            image = ImageVector.vectorResource(id = R.drawable.ic_business)
                        ),
                        contentDescription = stringResource(id = R.string.project_label)
                    )
                    Text(
                        modifier = Modifier.padding(start = 4.dp),
                        text = record.project.name,
                        fontWeight = FontWeight.Medium,
                        color = color
                    )
                }
                if (isStartFieldVisible || isFinishFieldVisible) {
                    Spacer(modifier = Modifier.weight(1f))
                    Image(
                        modifier = Modifier
                            .size(iconSize)
                            .align(Alignment.CenterVertically),
                        painter = rememberVectorPainter(
                            image = ImageVector.vectorResource(id = R.drawable.ic_time)
                        ),
                        contentDescription = stringResource(id = R.string.duration_label)
                    )
                    Text(
                        modifier = Modifier.padding(start = 4.dp),
                        text = timeRange,
                        color = color
                    )
                }
            }
            Row {
                if (isTaskFieldVisible) {
                    Image(
                        modifier = Modifier
                            .size(iconSize)
                            .align(Alignment.CenterVertically),
                        painter = rememberVectorPainter(
                            image = ImageVector.vectorResource(id = R.drawable.ic_folder_open)
                        ),
                        contentDescription = stringResource(id = R.string.task_label)
                    )
                    Text(
                        modifier = Modifier.padding(start = 4.dp),
                        text = record.task.name,
                        color = color
                    )
                }
                if (isDurationFieldVisible) {
                    timeBuffer.clear()
                    val timeFormatter = Formatter(timeBuffer)
                    val duration = formatElapsedTime(context, timeFormatter, record.duration)

                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        modifier = Modifier,
                        text = duration,
                        fontWeight = FontWeight.Medium,
                        color = color
                    )
                }
            }
            Row {
                if (isLocationFieldVisible) {
                    Image(
                        modifier = Modifier
                            .size(iconSize)
                            .align(Alignment.CenterVertically),
                        painter = rememberVectorPainter(
                            image = ImageVector.vectorResource(id = R.drawable.ic_home_work)
                        ),
                        contentDescription = stringResource(id = R.string.location_label)
                    )
                    Text(
                        modifier = Modifier.padding(start = 4.dp),
                        text = record.location.toLocationItem(context).label,
                        color = color
                    )
                }
                if (isCostFieldVisible) {
                    Spacer(modifier = Modifier.weight(1f))
                    Image(
                        modifier = Modifier
                            .size(iconSize)
                            .align(Alignment.CenterVertically),
                        painter = rememberVectorPainter(
                            image = ImageVector.vectorResource(id = R.drawable.ic_attach_money)
                        ),
                        contentDescription = stringResource(id = R.string.cost_total)
                    )
                    Text(
                        modifier = Modifier.padding(start = 4.dp),
                        text = formatCost(record.cost),
                        color = color
                    )
                }
            }
            Row {
                if (isNoteFieldVisible) {
                    Image(
                        modifier = Modifier
                            .size(iconSize)
                            .align(Alignment.CenterVertically),
                        painter = rememberVectorPainter(
                            image = ImageVector.vectorResource(id = R.drawable.ic_note)
                        ),
                        contentDescription = stringResource(id = R.string.note_hint)
                    )
                    Text(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .fillMaxWidth(),
                        text = record.note,
                        color = color
                    )
                }
            }
        }
    }
}

private fun formatCost(cost: Double): String {
    return if (cost <= 0.0) "" else cost.toString()
}

private fun calculateColor(record: TimeRecord, isDark: Boolean): Color {
    val projectHash: Int =
        if (record.project.id != TikalEntity.ID_NONE) record.project.id.toInt() else record.project.hashCode()
    val taskHash: Int =
        if (record.task.id != TikalEntity.ID_NONE) record.task.id.toInt() else record.task.hashCode()
    val spread = (projectHash * projectHash * taskHash)
    val spreadBits = spread.and(511)

    // 512 combinations => 3 bits per color
    val redBits = spreadBits.and(0x07)
    val greenBits = spreadBits.shr(3).and(0x07)
    val blueBits = spreadBits.shr(6).and(0x07)
    val r = redBits * 24 //*32 => some colors too bright
    val g = greenBits * 24 //*32 => some colors too bright
    val b = blueBits * 24 //*32 => some colors too bright

    return if (isDark) Color(255 - r, 255 - g, 255 - b) else Color(r, g, b)
}

@Preview(name = "default", showBackground = true)
@Preview(name = "dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ThisPreview() {
    val record = TimeRecord(
        project = Project("Project"),
        task = ProjectTask("Task"),
        date = Calendar.getInstance(),
        duration = DateUtils.HOUR_IN_MILLIS,
        note = "Note",
        location = Location.OTHER,
        cost = 1.23
    )
    val onClick: ((TimeRecord) -> Unit) = { println("record clicked: $it") }

    TikalTheme {
        TimeItem(record, onClick = onClick)
    }
}
