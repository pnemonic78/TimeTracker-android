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

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tikalk.compose.TikalTheme
import com.tikalk.worktracker.R
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.model.time.TimeRecord.Companion.NEVER
import kotlinx.coroutines.flow.Flow

@Composable
fun TimeEditForm(
    modifier: Modifier = Modifier,
    projectsFlow: Flow<List<Project>>,
    taskEmpty: ProjectTask = ProjectTask.EMPTY,
    recordFlow: Flow<TimeRecord>,
    errorFlow: Flow<TimeFormError?>,
    onRecordChanged: RecordCallback
) {
    val projectsState = projectsFlow.collectAsState(initial = listOf(Project.EMPTY))
    val recordState = recordFlow.collectAsState(initial = TimeRecord.EMPTY)
    val errorState = errorFlow.collectAsState(initial = null)

    TimeEditForm(
        modifier,
        projectsState.value,
        taskEmpty,
        recordState.value,
        errorState.value,
        onRecordChanged
    )
}

@Composable
fun TimeEditForm(
    modifier: Modifier = Modifier,
    projects: List<Project>,
    taskEmpty: ProjectTask,
    record: TimeRecord,
    error: TimeFormError? = null,
    onRecordChanged: RecordCallback
) {
    val paddingTop = dimensionResource(id = R.dimen.form_marginTop)
    var projectSelected by remember { mutableStateOf(Project.EMPTY) }
    var taskSelected by remember { mutableStateOf(ProjectTask.EMPTY) }
    val isTimerStopped = (record.startTime <= NEVER)
    var startTime by remember { mutableLongStateOf(NEVER) }
    var finishTime by remember { mutableLongStateOf(NEVER) }
    var duration by remember { mutableLongStateOf(0L) }
    var note by remember { mutableStateOf("") }

    projectSelected = record.project
    taskSelected = record.task
    startTime = record.startTime
    finishTime = record.finishTime
    duration = record.duration
    note = record.note

    Card(
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            ProjectSpinner(
                projects = projects,
                project = projectSelected,
                enabled = isTimerStopped,
                error = error
            ) {
                record.project = it
                onRecordChanged(record)
                projectSelected = it
                taskSelected = ProjectTask.EMPTY
            }
            ProjectTaskSpinner(
                modifier = Modifier.padding(top = paddingTop),
                tasks = projectSelected.tasks,
                taskEmpty = taskEmpty,
                task = taskSelected,
                enabled = isTimerStopped,
                error = error
            ) {
                record.task = it
                onRecordChanged(record)
                taskSelected = it
            }
            StartTimePickerButton(
                modifier = Modifier.padding(top = paddingTop),
                record = record,
                time = startTime,
                onTimeSelected = { time ->
                    record.date = time.toCalendar()
                    record.startTime = time
                    onRecordChanged(record)
                    startTime = time
                    duration = record.duration
                },
                error = error
            )
            FinishTimePickerButton(
                modifier = Modifier.padding(top = paddingTop),
                record = record,
                time = finishTime,
                onTimeSelected = { time ->
                    record.finishTime = time
                    onRecordChanged(record)
                    finishTime = time
                    duration = record.duration
                },
                error = error
            )
            DurationPickerButton(
                modifier = Modifier.padding(top = paddingTop),
                record = record,
                duration = duration,
                onDurationSelected = { time ->
                    record.setDurationDateTime(time)
                    onRecordChanged(record)
                    startTime = NEVER
                    finishTime = NEVER
                    duration = record.duration
                },
                error = error
            )
            NoteText(
                modifier = Modifier.padding(top = paddingTop),
                text = note,
                onChanged = {
                    record.note = it
                    onRecordChanged(record)
                    note = it
                },
                error = error
            )
            ErrorText(
                modifier = Modifier.padding(top = paddingTop),
                text = error?.message
            )
        }
    }
}

@Preview(name = "default", showBackground = true)
@Preview(name = "dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ThisPreview() {
    val projects = listOf(Project("Project #1"), Project("Project #2"))
    val record = TimeRecord.EMPTY
    record.startTime = System.currentTimeMillis() - 10000
    val onRecordChanged: RecordCallback = {
        println("Record changed: $it")
    }

    TikalTheme {
        TimeEditForm(
            Modifier,
            projects,
            ProjectTask.EMPTY,
            record,
            onRecordChanged = onRecordChanged
        )
    }
}
