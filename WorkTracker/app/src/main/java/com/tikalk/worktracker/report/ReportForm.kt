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

package com.tikalk.worktracker.report

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tikalk.compose.BooleanCallback
import com.tikalk.compose.CalendarCallback
import com.tikalk.compose.GenericCallback
import com.tikalk.compose.TikalTheme
import com.tikalk.compose.UnitCallback
import com.tikalk.worktracker.R
import com.tikalk.worktracker.model.DefaultTimePeriod
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.ReportTimePeriod
import com.tikalk.worktracker.model.time.ReportFilter
import com.tikalk.worktracker.time.DatePickerButton
import com.tikalk.worktracker.time.ErrorText
import com.tikalk.worktracker.time.FormSpinner
import com.tikalk.worktracker.time.ProjectCallback
import com.tikalk.worktracker.time.ProjectSpinner
import com.tikalk.worktracker.time.ProjectTaskCallback
import com.tikalk.worktracker.time.ProjectTaskSpinner
import com.tikalk.worktracker.time.TimeFormError
import com.tikalk.worktracker.time.iconIdFinish
import com.tikalk.worktracker.time.iconIdStart
import java.util.Calendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@DrawableRes
val iconIdPeriod = com.tikalk.core.R.drawable.ic_history

typealias PeriodCallback = GenericCallback<ReportTimePeriod>

@Composable
fun ReportStartDateButton(
    modifier: Modifier = Modifier,
    date: Calendar?,
    onDateSelected: CalendarCallback
) {
    DatePickerButton(
        modifier = modifier,
        date = date,
        iconId = iconIdStart,
        hint = stringResource(id = R.string.start_date_label),
        onDateSelected = onDateSelected
    )
}

@Composable
fun ReportFinishDateButton(
    modifier: Modifier = Modifier,
    date: Calendar?,
    onDateSelected: CalendarCallback
) {
    DatePickerButton(
        modifier = modifier,
        date = date,
        iconId = iconIdFinish,
        hint = stringResource(id = R.string.finish_date_label),
        onDateSelected = onDateSelected
    )
}

@Composable
fun ReportGenerateButton(
    modifier: Modifier = Modifier,
    onClick: UnitCallback
) {
    Button(
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.buttonStart)),
        onClick = onClick
    ) {
        Text(text = stringResource(id = R.string.action_generate))
        Icon(
            modifier = Modifier.padding(start = 8.dp),
            painter = rememberVectorPainter(image = ImageVector.vectorResource(id = com.tikalk.core.R.drawable.ic_play_arrow)),
            contentDescription = null
        )
    }
}

@Composable
fun ReportCheckBox(
    modifier: Modifier = Modifier,
    label: String,
    isChecked: Boolean,
    onCheckedChange: BooleanCallback?
) {
    val checked = remember { mutableStateOf(isChecked) }

    Row(
        modifier = modifier
            .clickable {
                checked.value = !checked.value
                onCheckedChange?.invoke(checked.value)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked.value,
            onCheckedChange = { isChecked ->
                checked.value = isChecked
                onCheckedChange?.invoke(isChecked)
            }
        )
        Text(
            modifier = Modifier
                .padding(start = 4.dp),
            text = label,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

private val periods = ReportTimePeriod.entries.toTypedArray()

@Composable
fun ReportPeriodSpinner(
    modifier: Modifier = Modifier,
    period: ReportTimePeriod? = null,
    enabled: Boolean = true,
    error: TimeFormError? = null,
    onPeriodSelected: PeriodCallback
) {
    val context: Context = LocalContext.current
    val items = periods.map { ReportTimePeriodItem(context, it) }

    FormSpinner(
        modifier = modifier,
        labelId = R.string.project_label,
        iconId = iconIdPeriod,
        items = items,
        selectedItem = items.find { it.period == period },
        enabled = enabled,
        isError = error is TimeFormError.Period,
        onItemSelected = { onPeriodSelected.invoke(it.period) }
    )
}

@Composable
fun ReportForm(
    modifier: Modifier = Modifier,
    projectsFlow: Flow<List<Project>>,
    taskEmpty: ProjectTask,
    filterFlow: Flow<ReportFilter>,
    error: TimeFormError? = null,
    onProjectSelected: ProjectCallback,
    onTaskSelected: ProjectTaskCallback,
    onPeriodSelected: PeriodCallback,
    onStartDateSelected: CalendarCallback,
    onFinishDateSelected: CalendarCallback,
    onProjectFieldVisible: BooleanCallback,
    onTaskFieldVisible: BooleanCallback,
    onStartFieldVisible: BooleanCallback,
    onFinishFieldVisible: BooleanCallback,
    onDurationFieldVisible: BooleanCallback,
    onNoteFieldVisible: BooleanCallback,
    onClick: UnitCallback
) {
    val paddingTop = dimensionResource(id = R.dimen.form_marginTop)

    val filterState = filterFlow.collectAsState(initial = ReportFilter())
    val filter = filterState.value
    val tasks = remember { mutableStateOf(emptyList<ProjectTask>()) }
    val period = remember { mutableStateOf(DefaultTimePeriod) }
    val start = remember { mutableStateOf<Calendar?>(null) }
    val finish = remember { mutableStateOf<Calendar?>(null) }

    tasks.value = filter.project.tasks
    period.value = filter.period
    start.value = filter.start
    finish.value = filter.finish

    Box(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        Card(elevation = CardDefaults.elevatedCardElevation()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                ProjectSpinner(
                    projectsFlow = projectsFlow,
                    project = filter.project,
                    error = error,
                    onProjectSelected = {
                        onProjectSelected(it)
                        tasks.value = it.tasks
                    }
                )
                ProjectTaskSpinner(
                    modifier = Modifier.padding(top = paddingTop),
                    tasks = tasks.value,
                    taskEmpty = taskEmpty,
                    task = filter.task,
                    error = error,
                    onTaskSelected = onTaskSelected
                )
                ReportPeriodSpinner(
                    modifier = Modifier.padding(top = paddingTop),
                    period = period.value,
                    onPeriodSelected = {
                        onPeriodSelected(it)
                        period.value = it
                        start.value = filter.start
                        finish.value = filter.finish
                    }
                )
                if (period.value == ReportTimePeriod.CUSTOM) {
                    ReportStartDateButton(
                        modifier = Modifier.padding(top = paddingTop),
                        date = start.value,
                        onDateSelected = {
                            onStartDateSelected(it)
                            start.value = it
                        }
                    )
                    ReportFinishDateButton(
                        modifier = Modifier.padding(top = paddingTop),
                        date = finish.value,
                        onDateSelected = {
                            onFinishDateSelected(it)
                            finish.value = it
                        }
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingTop)
                ) {
                    ReportCheckBox(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.project_label),
                        isChecked = filter.isProjectFieldVisible,
                        onCheckedChange = onProjectFieldVisible
                    )
                    ReportCheckBox(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.task_label),
                        isChecked = filter.isTaskFieldVisible,
                        onCheckedChange = onTaskFieldVisible
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingTop)
                ) {
                    ReportCheckBox(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.start_label),
                        isChecked = filter.isStartFieldVisible,
                        onCheckedChange = onStartFieldVisible
                    )
                    ReportCheckBox(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.finish_label),
                        isChecked = filter.isFinishFieldVisible,
                        onCheckedChange = onFinishFieldVisible
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = paddingTop)
                ) {
                    ReportCheckBox(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.duration_label),
                        isChecked = filter.isDurationFieldVisible,
                        onCheckedChange = onDurationFieldVisible
                    )
                    ReportCheckBox(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.note_hint),
                        isChecked = filter.isNoteFieldVisible,
                        onCheckedChange = onNoteFieldVisible
                    )
                }
                if (error != null) {
                    ErrorText(
                        modifier = Modifier.padding(top = paddingTop),
                        error = error
                    )
                }
                ReportGenerateButton(
                    modifier = Modifier.padding(top = paddingTop),
                    onClick = onClick
                )
            }
        }
    }
}

@Preview(name = "default", showBackground = true)
@Preview(name = "dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ThisPreview() {
    val projects = listOf(Project("Project #1"), Project("Project #2"))
    val tasks = listOf(ProjectTask("Task #1"), ProjectTask("Task #2"))
    projects[0].tasks = tasks
    val filter = ReportFilter().apply {
        project = Project.EMPTY.copy(name = "(All)")
    }
    val error = TimeFormError.Duration("Field Required")

    TikalTheme {
        Column {
            ReportForm(
                projectsFlow = flowOf(projects),
                taskEmpty = ProjectTask.EMPTY,
                filterFlow = flowOf(filter),
                error = error,
                onProjectSelected = { println("Project selected: $it") },
                onTaskSelected = { println("Task selected: $it") },
                onPeriodSelected = { println("Period selected: $it") },
                onStartDateSelected = { println("Start selected: $it") },
                onFinishDateSelected = { println("Finish selected: $it") },
                onProjectFieldVisible = { println("Project field: $it") },
                onTaskFieldVisible = { println("Task field: $it") },
                onStartFieldVisible = { println("Start field: $it") },
                onFinishFieldVisible = { println("Finish field: $it") },
                onDurationFieldVisible = { println("Duration field: $it") },
                onNoteFieldVisible = { println("Note field: $it") },
                onClick = { println("Clicked!") }
            )
        }
    }
}
