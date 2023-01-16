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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.tikalk.compose.PasswordTextField
import com.tikalk.compose.TextFieldViewState
import com.tikalk.compose.TikalTheme
import com.tikalk.compose.UnitCallback
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.model.UserCredentials

@Composable
fun ProfileDialog(viewState: ProfileViewState) {
    val marginTop = dimensionResource(id = R.dimen.form_marginTop)

    Column(
        modifier = Modifier
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth(),
            label = {
                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = stringResource(id = R.string.prompt_name),
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Medium
                )
            },
            value = viewState.userDisplayName.value,
            trailingIcon = {
                Icon(
                    painter = rememberVectorPainter(
                        image = ImageVector.vectorResource(
                            id = R.drawable.ic_person
                        )
                    ),
                    contentDescription = ""
                )
            },
            singleLine = true,
            onValueChange = { value: String ->
                viewState.userDisplayName.value = value
            },
            textStyle = MaterialTheme.typography.body1,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            readOnly = viewState.userDisplayName.isReadOnly,
            isError = viewState.userDisplayName.isError
        )
        OutlinedTextField(
            modifier = Modifier
                .padding(top = marginTop)
                .fillMaxWidth(),
            label = {
                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = stringResource(id = R.string.prompt_email),
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Medium
                )
            },
            value = viewState.userEmail.value,
            trailingIcon = {
                Icon(
                    painter = rememberVectorPainter(
                        image = ImageVector.vectorResource(
                            id = R.drawable.ic_email
                        )
                    ),
                    contentDescription = ""
                )
            },
            singleLine = true,
            onValueChange = { value: String ->
                viewState.userEmail.value = value
            },
            textStyle = MaterialTheme.typography.body1,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            readOnly = viewState.userEmail.isReadOnly,
            isError = viewState.userEmail.isError
        )
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
            value = viewState.credentialsLogin.value,
            trailingIcon = {
                Icon(
                    painter = rememberVectorPainter(
                        image = ImageVector.vectorResource(
                            id = R.drawable.ic_lock_open
                        )
                    ),
                    contentDescription = ""
                )
            },
            singleLine = true,
            onValueChange = { value: String ->
                viewState.credentialsLogin.value = value
            },
            textStyle = MaterialTheme.typography.body1,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            readOnly = viewState.credentialsLogin.isReadOnly,
            isError = viewState.credentialsLogin.isError
        )
        PasswordTextField(
            modifier = Modifier
                .padding(top = marginTop)
                .fillMaxWidth(),
            label = stringResource(id = R.string.prompt_password),
            value = viewState.credentialsPassword.value,
            onValueChange = { value: String ->
                viewState.credentialsPassword.value = value
            },
            readOnly = viewState.credentialsPassword.isReadOnly,
            isError = viewState.credentialsPassword.isError
        )
        PasswordTextField(
            modifier = Modifier
                .padding(top = marginTop)
                .fillMaxWidth(),
            label = stringResource(id = R.string.prompt_confirmPassword),
            value = viewState.credentialsPasswordConfirmation.value,
            onValueChange = { value: String ->
                viewState.credentialsPasswordConfirmation.value = value
            },
            readOnly = viewState.credentialsPasswordConfirmation.isReadOnly,
            isError = viewState.credentialsPasswordConfirmation.isError
        )
        if (viewState.errorMessage.isNotEmpty()) {
            Text(
                modifier = Modifier
                    .padding(top = marginTop)
                    .fillMaxWidth(),
                text = viewState.errorMessage,
                style = MaterialTheme.typography.body1,
                color = Color.Red,
                textAlign = TextAlign.Center
            )
        }
        Button(
            modifier = Modifier
                .padding(top = marginTop)
                .fillMaxWidth(),
            onClick = viewState.onConfirmClick
        ) {
            Text(text = stringResource(id = R.string.action_submit))
            Icon(
                modifier = Modifier.padding(start = 8.dp),
                painter = rememberVectorPainter(image = ImageVector.vectorResource(id = R.drawable.ic_save)),
                contentDescription = ""
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ThisPreview() {
    val user = UserDemo
    val credentials = UserCredentials(
        login = user.email ?: "",
        password = "demo"
    )

    val viewState = object : ProfileViewState {
        override val userDisplayName: TextFieldViewState =
            TextFieldViewState(user.displayName ?: "")
        override val userEmail: TextFieldViewState = TextFieldViewState(user.email ?: "")
        override val credentialsLogin: TextFieldViewState = TextFieldViewState(credentials.login)
        override val credentialsPassword: TextFieldViewState =
            TextFieldViewState(credentials.password)
        override val credentialsPasswordConfirmation: TextFieldViewState =
            TextFieldViewState("", isError = true)
        override val errorMessage: String = "Error!"
        override var onConfirmClick: UnitCallback = { println("Button clicked") }
    }

    TikalTheme {
        ProfileDialog(viewState = viewState)
    }
}
