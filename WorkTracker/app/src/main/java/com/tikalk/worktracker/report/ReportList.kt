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

package com.tikalk.worktracker.report

import android.content.res.Configuration
import android.text.format.DateUtils
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tikalk.compose.TikalTheme
import com.tikalk.worktracker.EmptyListScreen
import com.tikalk.worktracker.model.Location
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.time.ReportFilter
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.model.time.times
import com.tikalk.worktracker.time.OnRecordCallback
import com.tikalk.worktracker.time.TimeItem
import java.util.Calendar
import kotlinx.coroutines.flow.Flow

@Composable
fun ReportList(
    modifier: Modifier = Modifier,
    itemsFlow: Flow<List<TimeRecord>>,
    filterFlow: Flow<ReportFilter>
) {
    val itemsState = itemsFlow.collectAsState(initial = emptyList())
    val items = itemsState.value

    val filterState = filterFlow.collectAsState(initial = ReportFilter())
    val filter = filterState.value

    ReportList(modifier = modifier, items = items, filter = filter)
}

@Composable
fun ReportList(
    modifier: Modifier = Modifier,
    items: List<TimeRecord>,
    filter: ReportFilter
) {
    if (items.isEmpty()) {
        EmptyListScreen()
        return
    }

    val onClick: OnRecordCallback = {}

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        state = rememberLazyListState()
    ) {
        items(count = items.size) { index ->
            val record = items[index]
            TimeItem(
                record = record,
                isReport = true,
                isProjectFieldVisible = filter.isProjectFieldVisible,
                isTaskFieldVisible = filter.isTaskFieldVisible,
                isStartFieldVisible = filter.isStartFieldVisible,
                isFinishFieldVisible = filter.isFinishFieldVisible,
                isDurationFieldVisible = filter.isDurationFieldVisible,
                isNoteFieldVisible = filter.isNoteFieldVisible,
                isCostFieldVisible = filter.isCostFieldVisible,
                isLocationFieldVisible = filter.isLocationFieldVisible,
                onClick = onClick
            )
        }
    }
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
    val records = record * 10
    val filter = ReportFilter()

    TikalTheme {
        ReportList(items = records, filter = filter)
    }
}
