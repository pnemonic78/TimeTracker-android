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

import android.text.format.DateUtils
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tikalk.compose.TikalTheme
import com.tikalk.worktracker.model.Location
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.time.TimeRecord
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

/**
 * Callback to be invoked when an item in this list has been clicked.
 */
typealias OnTimeRecordClick = ((TimeRecord) -> Unit)

@Composable
fun TimeList(
    itemsFlow: Flow<List<TimeRecord>>,
    onClick: OnTimeRecordClick
) {
    val itemsState = itemsFlow.collectAsState(initial = emptyList<TimeRecord>())
    val items = itemsState.value

    TimeList(items, onClick)
}

@Composable
fun TimeList(
    items: List<TimeRecord>,
    onClick: OnTimeRecordClick
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = rememberLazyListState()
    ) {
        items(count = items.size) { index ->
            TimeItem(record = items[index], onClick = onClick)
        }
    }
}

@Preview(showBackground = true)
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
    val records = listOf(record)
    val onClick: OnTimeRecordClick = { println("record clicked: $it") }

    TikalTheme {
        TimeList(records, onClick)
    }
}
