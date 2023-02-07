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

package com.tikalk.worktracker.help

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tikalk.compose.TikalTheme
import com.tikalk.worktracker.R

private const val bullet = "\u2022"
private const val bulletPrefix = "$bullet "
private val paddingCategory = Modifier.padding(top = 12.dp)
private val paddingBullet = Modifier.padding(top = 4.dp)
private val paddingBullet2 = paddingBullet.padding(start = 16.dp)

@Composable
fun HelpScreen() {
    val styleCategory = MaterialTheme.typography.subtitle2
    val styleBullet = MaterialTheme.typography.body2.copy(
        textIndent = TextIndent(firstLine = 0.sp, restLine = 10.sp)
    )

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = stringResource(id = R.string.help_summary),
            style = MaterialTheme.typography.body1
        )

        Text(
            modifier = paddingCategory,
            text = stringResource(id = R.string.help_consulting),
            style = styleCategory
        )
        Text(
            modifier = paddingBullet,
            text = bulletPrefix + stringResource(id = R.string.help_consulting_1),
            style = styleBullet
        )

        Text(
            modifier = paddingCategory,
            text = stringResource(id = R.string.help_development),
            style = styleCategory
        )
        Text(
            modifier = paddingBullet,
            text = bulletPrefix + stringResource(id = R.string.help_development_1),
            style = styleBullet
        )
        Text(
            modifier = paddingBullet,
            text = bulletPrefix + stringResource(id = R.string.help_development_2),
            style = styleBullet
        )

        Text(
            modifier = paddingCategory,
            text = stringResource(id = R.string.help_army),
            style = styleCategory
        )
        Text(
            modifier = paddingBullet,
            text = bulletPrefix + stringResource(id = R.string.help_army_1),
            style = styleBullet
        )

        Text(
            modifier = paddingCategory,
            text = stringResource(id = R.string.help_illness),
            style = styleCategory
        )
        Text(
            modifier = paddingBullet,
            text = bulletPrefix + stringResource(id = R.string.help_illness_1),
            style = styleBullet
        )

        Text(
            modifier = paddingCategory,
            text = stringResource(id = R.string.help_vacation),
            style = styleCategory
        )
        Text(
            modifier = paddingBullet,
            text = bulletPrefix + stringResource(id = R.string.help_vacation_1),
            style = styleBullet
        )

        Text(
            modifier = paddingCategory,
            text = stringResource(id = R.string.help_absence),
            style = styleCategory
        )
        Text(
            modifier = paddingBullet,
            text = bulletPrefix + stringResource(id = R.string.help_absence_1),
            style = styleBullet
        )
        Text(
            modifier = paddingBullet2,
            text = bulletPrefix + stringResource(id = R.string.help_absence_1_1),
            style = styleBullet
        )
        Text(
            modifier = paddingBullet2,
            text = bulletPrefix + stringResource(id = R.string.help_absence_1_2),
            style = styleBullet
        )
        Text(
            modifier = paddingBullet2,
            text = bulletPrefix + stringResource(id = R.string.help_absence_1_3),
            style = styleBullet
        )

        Text(
            modifier = paddingCategory,
            text = stringResource(id = R.string.help_meeting),
            style = styleCategory
        )
        Text(
            modifier = paddingBullet,
            text = bulletPrefix + stringResource(id = R.string.help_meeting_1),
            style = styleBullet
        )
        Text(
            modifier = paddingBullet,
            text = bulletPrefix + stringResource(id = R.string.help_meeting_2),
            style = styleBullet
        )
        Text(
            modifier = paddingBullet,
            text = bulletPrefix + stringResource(id = R.string.help_meeting_3),
            style = styleBullet
        )
        Text(
            modifier = paddingBullet,
            text = bulletPrefix + stringResource(id = R.string.help_meeting_4),
            style = styleBullet
        )
        Text(
            modifier = paddingBullet,
            text = bulletPrefix + stringResource(id = R.string.help_meeting_5),
            style = styleBullet
        )
        Text(
            modifier = paddingBullet,
            text = bulletPrefix + stringResource(id = R.string.help_meeting_6),
            style = styleBullet
        )
        Text(
            modifier = paddingBullet,
            text = bulletPrefix + stringResource(id = R.string.help_meeting_7),
            style = styleBullet
        )

        Text(
            modifier = paddingCategory,
            text = stringResource(id = R.string.help_training),
            style = styleCategory
        )
        Text(
            modifier = paddingBullet,
            text = bulletPrefix + stringResource(id = R.string.help_training_1),
            style = styleBullet
        )
        Text(
            modifier = paddingBullet,
            text = bulletPrefix + stringResource(id = R.string.help_training_2),
            style = styleBullet
        )
        Text(
            modifier = paddingBullet2,
            text = bulletPrefix + stringResource(id = R.string.help_training_2_1),
            style = styleBullet
        )
        Text(
            modifier = paddingBullet2,
            text = bulletPrefix + stringResource(id = R.string.help_training_2_2),
            style = styleBullet
        )
        Text(
            modifier = paddingBullet2,
            text = bulletPrefix + stringResource(id = R.string.help_training_2_3),
            style = styleBullet
        )
        Text(
            modifier = paddingBullet,
            text = bulletPrefix + stringResource(id = R.string.help_training_3),
            style = styleBullet
        )
        Text(
            modifier = paddingBullet,
            text = bulletPrefix + stringResource(id = R.string.help_training_4),
            style = styleBullet
        )
        Text(
            modifier = paddingBullet,
            text = bulletPrefix + stringResource(id = R.string.help_training_5),
            style = styleBullet
        )
        Text(
            modifier = paddingBullet,
            text = bulletPrefix + stringResource(id = R.string.help_training_6),
            style = styleBullet
        )
        Text(
            modifier = paddingBullet,
            text = bulletPrefix + stringResource(id = R.string.help_training_7),
            style = styleBullet
        )

        Text(
            modifier = paddingCategory,
            text = stringResource(id = R.string.help_general),
            style = styleCategory
        )
        Text(
            modifier = paddingBullet,
            text = bulletPrefix + stringResource(id = R.string.help_general_1),
            style = styleBullet
        )

        Text(
            modifier = paddingCategory,
            text = stringResource(id = R.string.help_hr),
            style = styleCategory
        )
        Text(
            modifier = paddingBullet,
            text = bulletPrefix + stringResource(id = R.string.help_hr_1),
            style = styleBullet
        )
        Text(
            modifier = paddingBullet,
            text = bulletPrefix + stringResource(id = R.string.help_hr_2),
            style = styleBullet
        )
        Text(
            modifier = paddingBullet,
            text = bulletPrefix + stringResource(id = R.string.help_hr_3),
            style = styleBullet
        )

        Text(
            modifier = paddingCategory,
            text = stringResource(id = R.string.help_sales),
            style = styleCategory
        )
        Text(
            modifier = paddingBullet,
            text = bulletPrefix + stringResource(id = R.string.help_sales_1),
            style = styleBullet
        )
        Text(
            modifier = paddingBullet,
            text = bulletPrefix + stringResource(id = R.string.help_sales_2),
            style = styleBullet
        )
        Text(
            modifier = paddingBullet,
            text = bulletPrefix + stringResource(id = R.string.help_sales_3),
            style = styleBullet
        )

        Text(
            modifier = paddingCategory,
            text = stringResource(id = R.string.help_transport),
            style = styleCategory
        )
        Text(
            modifier = paddingBullet,
            text = bulletPrefix + stringResource(id = R.string.help_transport_1),
            style = styleBullet
        )
    }
}

@Preview(showBackground = true, locale = "en", heightDp = 600)
@Composable
private fun ThisPreview() {
    TikalTheme {
        Surface {
            HelpScreen()
        }
    }
}
