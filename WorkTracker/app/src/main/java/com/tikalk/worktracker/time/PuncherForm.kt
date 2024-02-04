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
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tikalk.compose.TikalTheme
import com.tikalk.compose.UnitCallback
import com.tikalk.worktracker.R
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.time.TimeRecord
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

@Composable
fun PuncherForm(
    projectsFlow: Flow<List<Project>>,
    taskEmptyFlow: Flow<ProjectTask>,
    recordFlow: Flow<TimeRecord>,
    onStartClick: UnitCallback,
    onStopClick: UnitCallback
) {
    val projectsState = projectsFlow.collectAsState(initial = listOf(Project.EMPTY))
    val taskEmptyState = taskEmptyFlow.collectAsState(initial = ProjectTask.EMPTY)
    val recordState = recordFlow.collectAsState(initial = TimeRecord.EMPTY)

    PuncherForm(
        projectsState.value,
        taskEmptyState.value,
        recordState.value,
        onStartClick,
        onStopClick
    )
}

@Composable
fun PuncherForm(
    projects: List<Project>,
    taskEmpty: ProjectTask,
    record: TimeRecord,
    onStartClick: UnitCallback,
    onStopClick: UnitCallback
) {
    val paddingTop = dimensionResource(id = R.dimen.form_marginTop)
    var projectSelected by remember { mutableStateOf(Project.EMPTY) }
    var taskSelected by remember { mutableStateOf(ProjectTask.EMPTY) }
    val isTimerStopped = (record.startTime <= TimeRecord.NEVER)

    projectSelected = record.project
    taskSelected = record.task

    Card(elevation = CardDefaults.elevatedCardElevation()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            ProjectSpinner(
                projects = projects,
                project = projectSelected,
                enabled = isTimerStopped
            ) {
                record.project = it
                projectSelected = it
                taskSelected = ProjectTask.EMPTY
            }
            ProjectTaskSpinner(
                modifier = Modifier.padding(top = paddingTop),
                tasks = projectSelected.tasks,
                taskEmpty = taskEmpty,
                task = taskSelected,
                enabled = isTimerStopped
            ) {
                record.task = it
                taskSelected = it
            }
            if (isTimerStopped) {
                PuncherStartButton(
                    modifier = Modifier.padding(top = paddingTop),
                    record = record,
                    onClick = onStartClick
                )
            } else {
                PuncherTimerRow(
                    modifier = Modifier.padding(top = paddingTop),
                    record = record,
                    onClick = onStopClick
                )
            }
        }
    }
}

@Composable
fun PuncherStartButton(modifier: Modifier = Modifier, record: TimeRecord, onClick: UnitCallback) {
    Button(
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.buttonStart)),
        onClick = onClick,
        enabled = !record.project.isEmpty() && !record.task.isEmpty()
    ) {
        Text(text = stringResource(id = R.string.action_start))
        Icon(
            modifier = Modifier.padding(start = 8.dp),
            painter = rememberVectorPainter(image = ImageVector.vectorResource(id = com.tikalk.core.R.drawable.ic_play_arrow)),
            contentDescription = null
        )
    }
}

@Composable
fun PuncherStopButton(modifier: Modifier = Modifier, onClick: UnitCallback) {
    Button(
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.buttonStop)),
        onClick = onClick,
    ) {
        Text(text = stringResource(id = R.string.action_stop))
        Icon(
            modifier = Modifier.padding(start = 8.dp),
            painter = rememberVectorPainter(image = ImageVector.vectorResource(id = com.tikalk.core.R.drawable.ic_stop)),
            contentDescription = null
        )
    }
}

@Composable
fun PuncherTimerRow(modifier: Modifier = Modifier, record: TimeRecord, onClick: UnitCallback) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val elapsedSeconds = TimeUnit.MILLISECONDS.toSeconds(now - record.startTime)

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        PuncherStopButton(
            modifier = Modifier.weight(3f),
            onClick = onClick
        )
        Text(
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(2f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            text = DateUtils.formatElapsedTime(elapsedSeconds)
        )
    }

    LaunchedEffect(key1 = record) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
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
    val onStartClick = { println("Start clicked!") }
    val onStopClick = { println("Stop clicked!") }

    TikalTheme {
        PuncherForm(projects, ProjectTask.EMPTY, record, onStartClick, onStopClick)
    }
}
