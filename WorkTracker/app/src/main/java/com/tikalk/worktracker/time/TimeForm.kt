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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import com.tikalk.compose.TikalTheme
import com.tikalk.worktracker.R
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask

typealias ProjectCallback = ((Project) -> Unit)
typealias ProjectTaskCallback = ((ProjectTask) -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSpinner(
    modifier: Modifier = Modifier,
    projects: List<Project>,
    project: Project,
    enabled: Boolean = true,
    onProjectSelected: ProjectCallback
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
            value = project.name,
            onValueChange = { },
            readOnly = true,
            leadingIcon = {
                Icon(
                    modifier = Modifier
                        .size(iconSize),
                    painter = rememberVectorPainter(
                        image = ImageVector.vectorResource(id = com.tikalk.core.R.drawable.ic_business)
                    ),
                    contentDescription = stringResource(id = R.string.project_label)
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
            },
            placeholder = {
                Text(text = stringResource(id = R.string.project_label))
            },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
            enabled = enabled,
        )
        ExposedDropdownMenu(
            modifier = Modifier.exposedDropdownSize(),
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            projects.map { project ->
                DropdownMenuItem(
                    text = { Text(project.name) },
                    onClick = {
                        isExpanded = false
                        onProjectSelected(project)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectTaskSpinner(
    modifier: Modifier = Modifier,
    tasks: List<ProjectTask>,
    taskEmpty: ProjectTask = ProjectTask.EMPTY,
    task: ProjectTask,
    enabled: Boolean = true,
    onTaskSelected: ProjectTaskCallback
) {
    val iconSize = dimensionResource(id = R.dimen.icon_form)
    var isExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        modifier = modifier
            .fillMaxWidth(),
        expanded = isExpanded,
        onExpandedChange = { if (enabled) isExpanded = !isExpanded }
    ) {
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            value = task.name,
            onValueChange = { },
            readOnly = true,
            leadingIcon = {
                Icon(
                    modifier = Modifier
                        .size(iconSize),
                    painter = rememberVectorPainter(
                        image = ImageVector.vectorResource(id = com.tikalk.core.R.drawable.ic_folder_open)
                    ),
                    contentDescription = stringResource(id = R.string.task_label)
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
            },
            placeholder = {
                Text(text = stringResource(id = R.string.task_label))
            },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
            enabled = enabled
        )
        ExposedDropdownMenu(
            modifier = Modifier.exposedDropdownSize(),
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            taskEmpty.also { task ->
                DropdownMenuItem(
                    text = { Text(task.name) },
                    onClick = {
                        isExpanded = false
                        onTaskSelected(task)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
            tasks.filter { !it.isEmpty() }
                .map { task ->
                DropdownMenuItem(
                    text = { Text(task.name) },
                    onClick = {
                        isExpanded = false
                        onTaskSelected(task)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
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

    TikalTheme {
        Column {
            ProjectSpinner(projects = projects, project = Project.EMPTY) {
                println("Project selected: $it")
            }
            ProjectTaskSpinner(
                tasks = tasks,
                taskEmpty = ProjectTask.EMPTY,
                task = ProjectTask.EMPTY
            ) {
                println("Task selected: $it")
            }
        }
    }
}
