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

package com.tikalk.worktracker.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

typealias TableScope = BoxScope
typealias TableContent = @Composable (TableScope.(rowIndex: Int, columnIndex: Int) -> Unit)

private typealias TableRowScope = RowScope
private typealias TableRowContent = @Composable (TableRowScope.() -> Unit)

private typealias TableCellScope = BoxScope
private typealias TableCellContent = @Composable (TableCellScope.() -> Unit)

private val PaddingNone = 0.dp

@Composable
fun Table(
    modifier: Modifier = Modifier,
    rowCount: Int,
    columnCount: Int,
    columnPadding: Dp = PaddingNone,
    rowPadding: Dp = PaddingNone,
    content: TableContent
) {
    val columnWidths = remember { mutableStateMapOf<Int, Int>() }

    Column(
        modifier = modifier
    ) {
        for (rowIndex in 0 until rowCount) {
            TableRow(
                modifier = Modifier
                    .padding(top = if (rowIndex > 0) rowPadding else PaddingNone)
            ) {
                for (columnIndex in 0 until columnCount) {
                    TableCell(
                        modifier = Modifier
                            .padding(start = if (columnIndex > 0) columnPadding else PaddingNone)
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                val columnWidth = columnWidths[columnIndex] ?: 0
                                val maxWidth = maxOf(columnWidth, placeable.width)
                                if (maxWidth > columnWidth) {
                                    columnWidths[columnIndex] = maxWidth
                                }
                                layout(width = maxWidth, height = placeable.height) {
                                    placeable.placeRelative(0, 0)
                                }
                            }
                    ) {
                        content(rowIndex, columnIndex)
                    }
                }
            }
        }
    }
}

@Composable
private fun TableRow(modifier: Modifier = Modifier, content: TableRowContent) {
    Row(modifier = modifier, content = content)
}

@Composable
private fun TableCell(modifier: Modifier = Modifier, content: TableCellContent) {
    Box(modifier = modifier, content = content)
}
