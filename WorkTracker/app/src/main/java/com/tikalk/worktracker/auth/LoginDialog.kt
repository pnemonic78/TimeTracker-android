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

package com.tikalk.worktracker.auth

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.tikalk.compose.PasswordTextField
import com.tikalk.compose.TextFieldViewState
import com.tikalk.compose.TikalTheme
import com.tikalk.compose.UnitCallback
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.model.UserCredentials
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Composable
fun LoginDialog(viewState: LoginViewState) {
    Dialog(onDismissRequest = viewState.onDismiss) {
        LoginForm(viewState)
    }
}

@Composable
fun LoginForm(viewState: LoginViewState) {
    val marginTop = dimensionResource(id = R.dimen.form_marginTop)
    val coroutineScope = rememberCoroutineScope()

    val credentialsLoginState = viewState.credentialsLogin.collectAsState()
    val credentialsPasswordState = viewState.credentialsPassword.collectAsState()
    val errorMessageState = viewState.errorMessage.collectAsState()

    val credentialsLogin = credentialsLoginState.value
    val credentialsPassword = credentialsPasswordState.value
    val errorMessage = errorMessageState.value
    val onConfirmClick = viewState.onConfirmClick

    Card {
        Column(
            modifier = Modifier
                .defaultMinSize(minWidth = dimensionResource(id = R.dimen.dialog_form_minWidth))
                .padding(8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .padding(top = marginTop)
                    .fillMaxWidth(),
                label = {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = stringResource(id = R.string.prompt_login),
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Medium
                    )
                },
                value = credentialsLogin.value,
                trailingIcon = {
                    Icon(
                        painter = rememberVectorPainter(
                            image = ImageVector.vectorResource(
                                id = com.tikalk.core.R.drawable.ic_lock_open
                            )
                        ),
                        contentDescription = ""
                    )
                },
                singleLine = true,
                onValueChange = { value: String ->
                    coroutineScope.launch {
                        viewState.credentialsLogin.emit(credentialsLogin.copy(value = value))
                    }
                },
                textStyle = MaterialTheme.typography.body1,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                readOnly = credentialsLogin.isReadOnly,
                isError = credentialsLogin.isError
            )
            PasswordTextField(
                modifier = Modifier
                    .padding(top = marginTop)
                    .fillMaxWidth(),
                label = stringResource(id = R.string.prompt_password),
                value = credentialsPassword.value,
                onValueChange = { value: String ->
                    coroutineScope.launch {
                        viewState.credentialsPassword.emit(credentialsPassword.copy(value = value))
                    }
                },
                readOnly = credentialsPassword.isReadOnly,
                isError = credentialsPassword.isError
            )
            if (errorMessage.isNotEmpty()) {
                Text(
                    modifier = Modifier
                        .padding(top = marginTop)
                        .fillMaxWidth(),
                    text = errorMessage,
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.error,
                    textAlign = TextAlign.Center
                )
            }
            Button(
                modifier = Modifier
                    .padding(top = marginTop)
                    .fillMaxWidth(),
                onClick = onConfirmClick
            ) {
                Text(text = stringResource(id = R.string.action_sign_in))
                Icon(
                    modifier = Modifier.padding(start = 8.dp),
                    painter = rememberVectorPainter(image = ImageVector.vectorResource(id = com.tikalk.core.R.drawable.ic_lock_open)),
                    contentDescription = ""
                )
            }
        }
    }
}

@Preview(name = "default", showBackground = true, widthDp = 350, heightDp = 400)
@Preview(
    name = "dark",
    showBackground = true,
    widthDp = 350,
    heightDp = 400,
    uiMode = UI_MODE_NIGHT_YES
)
@Composable
private fun ThisPreview() {
    val credentials = UserCredentials(
        login = "demo@tikalk.com",
        password = "demo"
    )

    val viewState = object : LoginViewState {
        override val credentialsLogin = MutableStateFlow(TextFieldViewState(credentials.login))
        override val credentialsPassword =
            MutableStateFlow(TextFieldViewState(credentials.password))
        override val errorMessage = MutableStateFlow("Error!")
        override val onConfirmClick: UnitCallback = { println("Button clicked") }
        override val onDismiss: UnitCallback = { println("Dismissed") }
    }

    TikalTheme {
        LoginDialog(viewState = viewState)
    }
}
