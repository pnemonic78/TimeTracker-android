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

package com.tikalk.compose

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun PasswordTextField(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    onDoneAction: UnitCallback? = null,
    onValueChange: StringCallback
) {
    val showPassword = remember { mutableStateOf(false) }

    OutlinedTextField(
        modifier = modifier,
        label = {
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        },
        value = value,
        trailingIcon = {
            IconButton(onClick = { showPassword.value = !showPassword.value }) {
                Icon(
                    imageVector = if (showPassword.value) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                    contentDescription = "Visibility"
                )
            }
        },
        singleLine = true,
        onValueChange = onValueChange,
        textStyle = MaterialTheme.typography.bodyLarge,
        visualTransformation = if (showPassword.value) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = if (onDoneAction != null) ImeAction.Done else ImeAction.Default
        ),
        keyboardActions = KeyboardActions(onDone = { onDoneAction?.invoke() }),
        enabled = enabled,
        readOnly = readOnly,
        isError = isError
    )
}

@Preview(name = "default", showBackground = true)
@Preview(name = "dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ThisPreview() {
    TikalTheme {
        PasswordTextField(
            label = "Hello",
            value = "Value",
            onValueChange = { println("Value [$it]") }
        )
    }
}
