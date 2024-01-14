/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2023, Tikal Knowledge, Ltd.
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
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tikalk.app.DateTimePickerDialog
import com.tikalk.compose.TikalTheme
import com.tikalk.util.TikalFormatter
import com.tikalk.widget.DateTimePicker
import com.tikalk.worktracker.R
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.model.time.TimeRecord.Companion.NEVER
import java.util.Calendar
import kotlin.math.max

typealias GenericCallback<T> = ((T) -> Unit)
typealias ProjectCallback = GenericCallback<Project>
typealias ProjectTaskCallback = GenericCallback<ProjectTask>
typealias CalendarCallback = GenericCallback<Calendar>
typealias LongCallback = GenericCallback<Long>
typealias TimeCallback = LongCallback
typealias DurationCallback = LongCallback
typealias StringCallback = GenericCallback<String>

@DrawableRes
private val iconIdProject = com.tikalk.core.R.drawable.ic_business

@DrawableRes
private val iconIdTask = com.tikalk.core.R.drawable.ic_folder_open

@DrawableRes
private val iconIdStart = com.tikalk.core.R.drawable.ic_timer

@DrawableRes
private val iconIdFinish = com.tikalk.core.R.drawable.ic_timer_off

@DrawableRes
private val iconIdDuration = com.tikalk.core.R.drawable.ic_timelapse

@DrawableRes
private val iconIdNote = com.tikalk.core.R.drawable.ic_note

@Composable
fun ProjectSpinner(
    modifier: Modifier = Modifier,
    projects: List<Project>,
    project: Project,
    enabled: Boolean = true,
    error: TimeFormError? = null,
    onProjectSelected: ProjectCallback
) {
    FormSpinner(
        modifier = modifier,
        labelId = R.string.project_label,
        iconId = iconIdProject,
        items = projects,
        selectedItem = if (project.isEmpty()) null else project,
        enabled = enabled,
        isError = error is TimeFormError.Project,
        onItemSelected = onProjectSelected
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FormSpinner(
    modifier: Modifier = Modifier,
    @StringRes labelId: Int,
    @DrawableRes iconId: Int,
    items: List<T>,
    selectedItem: T? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    onItemSelected: GenericCallback<T>
) {
    val iconSize = dimensionResource(id = R.dimen.icon_form)
    var isExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        modifier = modifier
            .fillMaxWidth(),
        expanded = isExpanded,
        onExpandedChange = { if (enabled) isExpanded = !isExpanded },
    ) {
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            value = selectedItem?.toString() ?: "",
            onValueChange = { },
            readOnly = true,
            leadingIcon = {
                Icon(
                    modifier = Modifier.size(iconSize),
                    painter = rememberVectorPainter(
                        image = ImageVector.vectorResource(id = iconId)
                    ),
                    contentDescription = stringResource(id = labelId)
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
            },
            placeholder = {
                Text(text = stringResource(id = labelId))
            },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
            enabled = enabled,
            isError = isError
        )
        ExposedDropdownMenu(
            modifier = Modifier.exposedDropdownSize(),
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            items.map { item ->
                DropdownMenuItem(
                    text = { Text(item.toString()) },
                    onClick = {
                        isExpanded = false
                        onItemSelected(item)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
fun ProjectTaskSpinner(
    modifier: Modifier = Modifier,
    tasks: List<ProjectTask>,
    taskEmpty: ProjectTask = ProjectTask.EMPTY,
    task: ProjectTask,
    enabled: Boolean = true,
    error: TimeFormError? = null,
    onTaskSelected: ProjectTaskCallback
) {
    val items = listOf(taskEmpty) + tasks.filter { !it.isEmpty() }
    FormSpinner(
        modifier = modifier,
        labelId = R.string.task_label,
        iconId = iconIdTask,
        items = items,
        selectedItem = if (task.isEmpty()) null else task,
        enabled = enabled,
        isError = error is TimeFormError.Task,
        onItemSelected = onTaskSelected
    )
}

@Composable
fun StartTimePickerButton(
    modifier: Modifier = Modifier,
    record: TimeRecord,
    time: Long = record.startTime,
    error: TimeFormError? = null,
    onTimeSelected: TimeCallback
) {
    DateTimePickerButton(
        modifier = modifier,
        record = record,
        timeInMillis = time,
        iconId = iconIdStart,
        hint = stringResource(id = R.string.start_label),
        onTimeSelected = onTimeSelected,
        isError = error is TimeFormError.Start
    )
}

@Composable
fun FinishTimePickerButton(
    modifier: Modifier = Modifier,
    record: TimeRecord,
    time: Long = record.finishTime,
    error: TimeFormError? = null,
    onTimeSelected: TimeCallback
) {
    DateTimePickerButton(
        modifier = modifier,
        record = record,
        timeInMillis = time,
        iconId = iconIdFinish,
        hint = stringResource(id = R.string.finish_label),
        onTimeSelected = onTimeSelected,
        isError = error is TimeFormError.Finish
    )
}

@Composable
fun DurationPickerButton(
    modifier: Modifier = Modifier,
    record: TimeRecord,
    duration: Long = record.duration,
    error: TimeFormError? = null,
    onDurationSelected: DurationCallback
) {
    DurationPickerButton(
        modifier = modifier,
        record = record,
        duration = duration,
        iconId = iconIdDuration,
        hint = stringResource(id = R.string.duration_hint),
        error = error,
        onDurationSelected = onDurationSelected
    )
}

@Composable
fun DateTimePickerButton(
    modifier: Modifier = Modifier,
    record: TimeRecord,
    timeInMillis: Long = NEVER,
    @DrawableRes iconId: Int,
    hint: String,
    isError: Boolean = false,
    onTimeSelected: TimeCallback
) {
    val context: Context = LocalContext.current
    val iconSize = dimensionResource(id = R.dimen.icon_form)
    val text = if (timeInMillis == NEVER)
        hint
    else
        DateUtils.formatDateTime(context, timeInMillis, FORMAT_TIME_BUTTON)
    val modifierError = if (isError) {
        modifier.background(color = MaterialTheme.colorScheme.error)
    } else {
        modifier
    }

    Button(
        modifier = modifierError.fillMaxWidth(),
        onClick = {
            pickDateTime(context, record, timeInMillis) {
                onTimeSelected(it)
            }
        },
        enabled = !record.project.isEmpty() && !record.task.isEmpty()
    ) {
        Icon(
            modifier = Modifier.size(iconSize),
            painter = rememberVectorPainter(image = ImageVector.vectorResource(id = iconId)),
            contentDescription = hint
        )
        Text(
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f),
            text = text
        )
    }
}

private val timeFormatter = TikalFormatter()

@Composable
fun DurationPickerButton(
    modifier: Modifier = Modifier,
    record: TimeRecord,
    duration: Long = 0L,
    @DrawableRes iconId: Int,
    hint: String,
    error: TimeFormError? = null,
    onDurationSelected: DurationCallback
) {
    val context: Context = LocalContext.current
    val iconSize = dimensionResource(id = R.dimen.icon_form)
    val text = if (duration <= 0L)
        hint
    else
        formatElapsedTime(context, timeFormatter, duration)
    val isError = error is TimeFormError.Duration
    val modifierError = if (isError) {
        modifier.background(color = MaterialTheme.colorScheme.error)
    } else {
        modifier
    }

    OutlinedButton(
        modifier = modifierError.fillMaxWidth(),
        onClick = {
            pickDuration(context, record, duration) {
                onDurationSelected(max(it, 0L))
            }
        },
        enabled = !record.project.isEmpty() && !record.task.isEmpty()
    ) {
        Icon(
            modifier = Modifier.size(iconSize),
            painter = rememberVectorPainter(image = ImageVector.vectorResource(id = iconId)),
            contentDescription = hint
        )
        Text(
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f),
            text = text
        )
    }
}

private fun pickDateTime(
    context: Context,
    record: TimeRecord,
    time: Long = NEVER,
    onTimeSelected: TimeCallback
) {
    val cal = getCalendar(record, time)
    val year = cal.year
    val month = cal.month
    val dayOfMonth = cal.dayOfMonth
    val hour = cal.hourOfDay
    val minute = cal.minute

    val listener = object : DateTimePickerDialog.OnDateTimeSetListener {
        override fun onDateTimeSet(
            view: DateTimePicker,
            year: Int,
            month: Int,
            dayOfMonth: Int,
            hourOfDay: Int,
            minute: Int
        ) {
            val date = toCalendar(year, month, dayOfMonth, hourOfDay, minute)
            onTimeSelected(date.timeInMillis)
        }
    }
    DateTimePickerDialog(
        context,
        listener,
        year,
        month,
        dayOfMonth,
        hour,
        minute,
        DateFormat.is24HourFormat(context)
    ).show()
}

private fun pickDuration(
    context: Context,
    record: TimeRecord,
    duration: Long = 0L,
    onTimeSelected: TimeCallback
) {
    val cal = getCalendar(record, NEVER)
    val year = cal.year
    val month = cal.month
    val dayOfMonth = cal.dayOfMonth
    val hour = (duration / DateUtils.HOUR_IN_MILLIS).toInt()
    val minute = ((duration % DateUtils.HOUR_IN_MILLIS) / DateUtils.MINUTE_IN_MILLIS).toInt()

    val listener = object : DateTimePickerDialog.OnDateTimeSetListener {
        override fun onDateTimeSet(
            view: DateTimePicker,
            year: Int,
            month: Int,
            dayOfMonth: Int,
            hourOfDay: Int,
            minute: Int
        ) {
            val date = toCalendar(year, month, dayOfMonth, hourOfDay, minute)
            onTimeSelected(date.timeInMillis)
        }
    }
    DateTimePickerDialog(
        context,
        listener,
        year,
        month,
        dayOfMonth,
        hour,
        minute,
        true
    ).show()
}

private fun getCalendar(record: TimeRecord, time: Long = NEVER): Calendar {
    return Calendar.getInstance().apply {
        timeInMillis = if (time != NEVER) {
            time
        } else {
            record.dateTime
        }
        // Server granularity is seconds.
        second = 0
        millis = 0
    }
}

private fun toCalendar(
    year: Int,
    month: Int,
    dayOfMonth: Int,
    hourOfDay: Int,
    minute: Int
): Calendar {
    return Calendar.getInstance().apply {
        this.year = year
        this.month = month
        this.dayOfMonth = dayOfMonth
        this.hourOfDay = hourOfDay
        this.minute = minute
    }
}

@Composable
fun NoteText(
    modifier: Modifier = Modifier,
    text: String,
    error: TimeFormError? = null,
    onChanged: StringCallback
) {
    TextField(
        modifier = modifier.fillMaxWidth(),
        label = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.note_hint)
            )
        },
        value = text,
        leadingIcon = {
            Icon(
                painter = rememberVectorPainter(
                    image = ImageVector.vectorResource(id = iconIdNote)
                ),
                contentDescription = ""
            )
        },
        maxLines = 4,
        onValueChange = onChanged,
        isError = error is TimeFormError.Note
    )
}

@Composable
fun ErrorText(
    modifier: Modifier = Modifier,
    text: String?
) {
    if (text.isNullOrEmpty()) return
    Text(
        modifier = modifier.fillMaxWidth(),
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center
    )
}

@Composable
fun ErrorText(
    modifier: Modifier = Modifier,
    error: TimeFormError?
) {
    ErrorText(modifier, error?.message)
}

@Preview(name = "default", showBackground = true)
@Preview(name = "dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ThisPreview() {
    val projects = listOf(Project("Project #1"), Project("Project #2"))
    val tasks = listOf(ProjectTask("Task #1"), ProjectTask("Task #2"))
    projects[0].tasks = tasks
    val record = TimeRecord.EMPTY.copy().apply {
        project = Project.EMPTY.copy(name = "(Select)")
    }
    val error = TimeFormError.Duration("Field Required")

    TikalTheme {
        Column {
            ProjectSpinner(
                projects = projects,
                project = record.project,
                error = error
            ) {
                println("Project selected: $it")
            }
            ProjectTaskSpinner(
                tasks = tasks,
                taskEmpty = ProjectTask.EMPTY,
                task = record.task,
                error = error
            ) {
                println("Task selected: $it")
            }
            StartTimePickerButton(
                record = record,
                error = error
            ) {
                println("Start clicked: $it")
            }
            FinishTimePickerButton(
                record = record,
                error = error
            ) {
                println("Finish clicked: $it")
            }
            DurationPickerButton(
                record = record,
                error = error
            ) {
                println("Duration clicked: $it")
            }
            NoteText(
                text = "",
                error = error
            ) {
                println("Note: $it")
            }
            ErrorText(error = error)
        }
    }
}
