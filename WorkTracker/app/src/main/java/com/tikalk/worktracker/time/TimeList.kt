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
import android.text.format.DateUtils
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tikalk.compose.CalendarCallback
import com.tikalk.compose.GenericCallback
import com.tikalk.compose.TikalTheme
import com.tikalk.compose.UnitCallback
import com.tikalk.util.isLocaleRTL
import com.tikalk.worktracker.R
import com.tikalk.worktracker.model.Location
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.time.TimeRecord
import java.util.Calendar
import kotlin.math.absoluteValue
import kotlinx.coroutines.flow.Flow

/**
 * Callback to be invoked when an item in this list has been clicked.
 */
typealias OnTimeRecordClick = GenericCallback<TimeRecord>

@Composable
fun TimeList(
    modifier: Modifier = Modifier,
    itemsFlow: Flow<List<TimeRecord>>,
    onClick: OnTimeRecordClick,
    onSwipe: OnSwipeDayListener
) {
    val itemsState = itemsFlow.collectAsState(initial = emptyList())
    val items = itemsState.value

    TimeList(modifier, items, onClick, onSwipe)
}

@Composable
fun TimeList(
    modifier: Modifier = Modifier,
    items: List<TimeRecord>,
    onClick: OnTimeRecordClick,
    onSwipe: OnSwipeDayListener
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(key1 = items) {
                detectHorizontalFling(onSwipe)
            },
        state = rememberLazyListState()
    ) {
        items(count = items.size) { index ->
            TimeItem(record = items[index], onClick = onClick)
        }
    }
}

private suspend fun PointerInputScope.detectHorizontalFling(onSwipe: OnSwipeDayListener) {
    val velocityTracker = VelocityTracker()
    val velocityThreshold = 125.dp //SwipeableDefaults.VelocityThreshold
    val velocityThresholdPx = velocityThreshold.toPx()
    val isLocaleRTL = isLocaleRTL(Locale.current)

    detectHorizontalDragGestures(
        onDragEnd = {
            val velocity = velocityTracker.calculateVelocity().x
            velocityTracker.resetTracking()

            if (velocity.absoluteValue < velocityThresholdPx) return@detectHorizontalDragGestures

            if (velocity > 0) {
                if (isLocaleRTL) {
                    onSwipe.onSwipeNextDay()
                } else {
                    onSwipe.onSwipePreviousDay()
                }
            } else if (isLocaleRTL) {
                onSwipe.onSwipePreviousDay()
            } else {
                onSwipe.onSwipeNextDay()
            }
        }
    ) { change, _ ->
        velocityTracker.addPointerInputChange(change)
    }
}

@DrawableRes
private val iconIdDate = com.tikalk.core.R.drawable.ic_day

@Composable
fun TimeListDateButton(
    modifier: Modifier = Modifier,
    dateFlow: Flow<Calendar>,
    onDateSelected: CalendarCallback
) {
    val dateState = dateFlow.collectAsState(initial = Calendar.getInstance())
    TimeListDateButton(modifier, dateState.value, onDateSelected)
}

@Composable
fun TimeListDateButton(
    modifier: Modifier = Modifier,
    date: Calendar,
    onDateSelected: CalendarCallback
) {
    DatePickerButton(
        modifier = modifier,
        date = date,
        iconId = iconIdDate,
        hint = stringResource(id = R.string.date_label),
        onDateSelected = onDateSelected
    )
}

@Composable
fun FloatingAddButton(
    onClick: UnitCallback
) {
    FloatingActionButton(
        onClick = onClick
    ) {
        Icon(Icons.Filled.Add, stringResource(id = R.string.action_add))
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
    val records = listOf(record, record)
    val onClick: OnTimeRecordClick = { println("record clicked: $it") }
    val onSwipe = object : OnSwipeDayListener {
        override fun onSwipePreviousDay() {
            println("swipe previous day")
        }

        override fun onSwipeNextDay() {
            println("swipe next day")
        }
    }

    TikalTheme {
        Column {
            FloatingAddButton {
                println("fab clicked")
            }
            TimeListDateButton(date = Calendar.getInstance()) {
                println("date clicked: $it")
            }
            TimeList(items = records, onClick = onClick, onSwipe = onSwipe)
        }
    }
}
