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

package com.tikalk.worktracker.task

import android.content.res.Configuration
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tikalk.compose.TikalTheme
import com.tikalk.model.TikalResult
import com.tikalk.util.set
import com.tikalk.widget.ContentPaddingList
import com.tikalk.worktracker.EmptyListScreen
import com.tikalk.worktracker.LoadingScreen
import com.tikalk.worktracker.model.ProjectTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun ProjectTasksScreen(viewState: ProjectTasksViewState) {
    val resultState = viewState.tasks.collectAsState(initial = TikalResult.Loading())
    val result: TikalResult<List<ProjectTask>> = resultState.value
    val resultsSuccess = remember { mutableListOf<ProjectTask>() }

    when (result) {
        is TikalResult.Loading -> LoadingScreen()

        is TikalResult.Success -> {
            val tasks = result.data ?: emptyList()
            resultsSuccess.set(tasks)
            ProjectTasksScreenList(tasks = tasks)
        }

        is TikalResult.Error -> {
            ProjectTasksScreenError(tasks = resultsSuccess)
        }
    }
}

@Composable
private fun ProjectTasksScreenList(tasks: List<ProjectTask>) {
    if (tasks.isEmpty()) {
        EmptyListScreen()
        return
    }

    val scrollState = rememberLazyListState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = ContentPaddingList(),
        state = scrollState
    ) {
        items(tasks) {
            ProjectTaskItem(task = it)
            Spacer(modifier = Modifier.padding(4.dp))
        }
    }
}

@Composable
private fun ProjectTasksScreenError(tasks: List<ProjectTask>? = null) {
    if (tasks.isNullOrEmpty()) {
        EmptyListScreen()
    } else {
        ProjectTasksScreenList(tasks = tasks)
    }
}

@Preview(name = "default", showBackground = true)
@Preview(name = "dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ThisPreview() {
    val viewState: ProjectTasksViewState = object : ProjectTasksViewState {
        override val tasks: Flow<TikalResult<List<ProjectTask>>> =
            MutableStateFlow(TikalResult.Success(listOf(ProjectTask.EMPTY)))
    }

    TikalTheme {
        ProjectTasksScreen(viewState)
    }
}
