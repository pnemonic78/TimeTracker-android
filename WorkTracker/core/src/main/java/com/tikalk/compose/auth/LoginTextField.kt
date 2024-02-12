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

package com.tikalk.compose.auth

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.tikalk.compose.StringCallback
import com.tikalk.compose.TikalTheme
import com.tikalk.compose.UnitCallback
import com.tikalk.core.R
import kotlinx.coroutines.launch

@Composable
fun LoginTextField(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    onDoneAction: UnitCallback? = null,
    onValueChange: StringCallback
) {
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
            Icon(
                painter = rememberVectorPainter(
                    image = ImageVector.vectorResource(
                        id = R.drawable.ic_lock_open
                    )
                ),
                contentDescription = null
            )
        },
        singleLine = true,
        onValueChange = onValueChange,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = if (onDoneAction != null) ImeAction.Done else ImeAction.Default
        ),
        keyboardActions = KeyboardActions(onDone = { onDoneAction?.invoke() }),
        enabled = enabled,
        readOnly = readOnly,
        isError = isError
    )
}

//fun LF() {
//    OutlinedTextField(
//        modifier = Modifier
//            .padding(top = marginTop)
//            .fillMaxWidth(),
//        label = {
//            Text(
//                modifier = Modifier
//                    .fillMaxWidth(),
//                text = stringResource(id = R.string.prompt_login),
//                style = MaterialTheme.typography.titleMedium,
//                fontWeight = FontWeight.Medium
//            )
//        },
//        value = credentialsLogin.value,
//        trailingIcon = {
//            Icon(
//                painter = rememberVectorPainter(
//                    image = ImageVector.vectorResource(
//                        id = R.drawable.ic_lock_open
//                    )
//                ),
//                contentDescription = null
//            )
//        },
//        singleLine = true,
//        onValueChange = { value: String ->
//            coroutineScope.launch {
//                viewState.credentialsLogin.emit(credentialsLogin.copy(value = value))
//            }
//        },
//        textStyle = MaterialTheme.typography.bodyLarge,
//        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
//        readOnly = credentialsLogin.isReadOnly,
//        isError = error is LoginError.Name
//    )
//}

@Preview(name = "default", showBackground = true)
@Preview(name = "dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ThisPreview() {
    TikalTheme {
        LoginTextField(
            label = "Hello",
            value = "Value",
            onValueChange = { println("Value [$it]") }
        )
    }
}
