package com.tikalk.worktracker

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import com.tikalk.compose.TikalTheme

@Composable
fun LoadingScreen() {
    ShowProgress()
}

@Composable
internal fun ShowProgress(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) { }
    ) {
        if (LocalInspectionMode.current) {
            CircularProgressIndicator(
                modifier = Modifier
                    .wrapContentWidth()
                    .align(Alignment.Center),
                progress = 0.75f
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier
                    .wrapContentWidth()
                    .align(Alignment.Center)
            )
        }
    }
}

@Preview(name = "default", showBackground = true)
@Preview(name = "dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ThisPreview() {
    TikalTheme {
        LoadingScreen()
    }
}
