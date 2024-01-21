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

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tikalk.compose.TikalTheme
import com.tikalk.worktracker.R
import com.tikalk.worktracker.model.User

@OptIn(ExperimentalTextApi::class)
@Composable
fun UserItem(user: User) {
    val username = user.username
    val email = user.email
    val uriHandler = LocalUriHandler.current

    Card(elevation = CardDefaults.elevatedCardElevation()) {
        Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 4.dp)) {
            Row {
                Text(
                    modifier = Modifier
                        .weight(0.3f),
                    text = stringResource(id = R.string.name_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(0.7f),
                    text = user.displayName.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Row(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    modifier = Modifier
                        .weight(0.3f),
                    text = stringResource(id = R.string.login_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                if (email.isNullOrEmpty()) {
                    Text(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .weight(0.7f),
                        text = username,
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    ClickableText(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .weight(0.7f),
                        text = buildAnnotatedString {
                            append(username)
                            addUrlAnnotation(
                                urlAnnotation = UrlAnnotation(email),
                                start = 0,
                                end = username.length
                            )
                            addStyle(
                                style = SpanStyle(color = MaterialTheme.colorScheme.secondary),
                                start = 0,
                                end = username.length
                            )
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        onClick = { uriHandler.openUri("mailto:$email") }
                    )
                }
            }
            Row(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    modifier = Modifier
                        .weight(0.3f),
                    text = stringResource(id = R.string.role_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(0.7f),
                    text = user.roles?.joinToString(", ").orEmpty(),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Preview(name = "default", showBackground = true)
@Preview(name = "dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ThisPreview() {
    TikalTheme {
        UserItem(user = UserDemo)
    }
}

internal val UserDemo = User(
    username = "demo",
    email = "demo@tikalk.com",
    displayName = "Demo",
    roles = listOf("User", "Manager")
)