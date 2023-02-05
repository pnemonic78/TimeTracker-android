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

package com.tikalk.worktracker.user

import android.graphics.RectF
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tikalk.compose.TikalTheme
import com.tikalk.worktracker.R
import com.tikalk.worktracker.model.User

typealias OnScrollIndexCallback = ((String, Int) -> Unit)

@Composable
fun UsersScroller(
    modifier: Modifier = Modifier,
    users: List<User>,
    onScrollIndex: OnScrollIndexCallback
) {
    val chars = users.filterNot { it.displayName.isNullOrEmpty() }
        .map { it.displayName!![0].uppercase() }
        .toSet()
    val indices = chars.distinct().sorted()
    val indicesBounds: MutableMap<String, RectF> = mutableMapOf()
    val positions: Map<String, Int> = chars.associateWith { c ->
        users.indexOfFirst { it.displayName?.startsWith(c) == true }
    }
    var indexTop: Float
    var indexBottom: Float
    val textSize =
        with(LocalDensity.current) { dimensionResource(id = R.dimen.index_textSize).toSp() }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(dimensionResource(id = R.dimen.index_width))
            .background(color = Color.Black.copy(alpha = 0.05f))
            .pointerInput(key1 = users) {
                detectVerticalDragGestures { change, _ ->
                    val y = change.position.y
                    findIndex(indicesBounds, y)?.let { index ->
                        val position = positions[index] ?: -1
                        if (position >= 0) {
                            onScrollIndex(index, position)
                        }
                    }
                }
            }
    ) {
        indicesBounds.clear()

        indices.forEach { index ->
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(Alignment.CenterVertically)
                    .onPlaced { lc ->
                        indexTop = lc.positionInParent().y
                        indexBottom = indexTop + lc.size.height
                        val rect = RectF(0f, indexTop, lc.size.width.toFloat(), indexBottom)
                        indicesBounds[index] = rect
                    }
                    .pointerInput(key1 = index) {
                        detectTapGestures {
                            val position = positions[index] ?: -1
                            if (position >= 0) {
                                onScrollIndex(index, position)
                            }
                        }
                    },
                text = index,
                textAlign = TextAlign.Center,
                fontSize = textSize
            )
        }
    }
}

private fun findIndex(indicesBounds: Map<String, RectF>, y: Float): String? {
    indicesBounds.forEach { (index, rect) ->
        if (rect.contains(0f, y)) {
            return index
        }
    }
    return null
}

@Preview(showBackground = true)
@Composable
private fun ThisPreview() {
    val user1 = User(
        username = "demo",
        email = "demo@tikalk.com",
        displayName = "Demo",
        roles = listOf("User", "Manager"),
        isUncompletedEntry = true
    )
    val user2 = User(
        username = "demo",
        email = "demo@tikalk.com",
        displayName = "John Doe",
        roles = listOf("User"),
        isUncompletedEntry = false
    )
    val onScrollIndex: OnScrollIndexCallback = { i, p ->
        println("scrolled to $i $p")
    }

    TikalTheme {
        UsersScroller(users = listOf(User.EMPTY, user1, user2), onScrollIndex = onScrollIndex)
    }
}
