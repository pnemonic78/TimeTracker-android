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

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tikalk.compose.TikalTheme
import com.tikalk.model.TikalResult
import com.tikalk.widget.ContentPaddingList
import com.tikalk.worktracker.EmptyListScreen
import com.tikalk.worktracker.LoadingScreen
import com.tikalk.worktracker.model.User
import kotlinx.coroutines.launch

@Composable
fun UsersScreen(viewState: UsersViewState) {
    val resultState = viewState.users.collectAsState(initial = TikalResult.Loading())
    val result: TikalResult<List<User>> = resultState.value

    val positionState = viewState.userSelectedPosition.collectAsState(initial = 0)
    val position = positionState.value
    val onScrollIndex: OnScrollIndexCallback = viewState::onScrollIndex

    when (result) {
        is TikalResult.Loading -> LoadingScreen()
        is TikalResult.Success -> {
            val users = result.data ?: emptyList()
            UsersScreen(users = users, position = position, onScrollIndex = onScrollIndex)
        }

        is TikalResult.Error -> UsersScreenError()
    }
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
private fun UsersScreen(
    users: List<User>,
    position: Int = 0,
    onScrollIndex: OnScrollIndexCallback
) {
    if (users.isEmpty()) {
        EmptyListScreen()
        return
    }

    UsersScreenList(users = users, position = position, onScrollIndex = onScrollIndex)
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
private fun UsersScreenList(
    users: List<User>,
    position: Int = 0,
    onScrollIndex: OnScrollIndexCallback
) {
    val scrollState = rememberLazyListState(initialFirstVisibleItemIndex = position)
    val scope = rememberCoroutineScope()
    scope.launch { scrollState.animateScrollToItem(position) }

    Row(
        modifier = Modifier
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            contentPadding = ContentPaddingList(end = false),
            state = scrollState
        ) {
            items(users) {
                UserItem(user = it)
                Spacer(modifier = Modifier.padding(4.dp))
            }
        }
        UsersScroller(
            modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
            users = users,
            onScrollIndex = onScrollIndex
        )
    }
}

@Composable
private fun UsersScreenError() {
    EmptyListScreen()
}

@Preview(name = "default", showBackground = true)
@Preview(name = "dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ThisPreview() {
    val user1 = User(
        username = "demo",
        email = "demo@tikalk.com",
        displayName = "Demo",
        roles = listOf("User", "Manager")
    )
    val user2 = User(
        username = "demo",
        email = "demo@tikalk.com",
        displayName = "John Doe",
        roles = listOf("User")
    )
    val onScrollIndex: OnScrollIndexCallback = { _, _ -> }
    val items = listOf(User.EMPTY, user1, user2)

    TikalTheme {
        UsersScreenList(users = items, onScrollIndex = onScrollIndex)
    }
}
