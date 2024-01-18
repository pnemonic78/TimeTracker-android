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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.zIndex
import com.tikalk.compose.TikalTheme
import com.tikalk.worktracker.R

// Each rotation is 1 and 1/3 seconds, but 1332ms divides more evenly
private const val RotationDuration = 1332

@Composable
fun TikalProgress() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(10000f)
            .clickable(true) {}
            .focusable(true)
            .background(color = colorResource(id = com.tikalk.core.R.color.black_x80))
    ) {
        TikalProgressIndicator(
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun TikalProgressIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "splash_transition")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = RotationDuration,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Image(
        modifier = modifier
            .progressSemantics()
            .size(dimensionResource(id = R.dimen.progress_bar_size_large))
            .rotate(rotation),
        bitmap = ImageBitmap.imageResource(id = R.drawable.tikal_flower),
        contentDescription = null
    )
}

@Preview(name = "default", showBackground = true, device = Devices.DEFAULT)
@Composable
private fun ThisPreview() {
    TikalTheme {
        TikalProgress()
    }
}
