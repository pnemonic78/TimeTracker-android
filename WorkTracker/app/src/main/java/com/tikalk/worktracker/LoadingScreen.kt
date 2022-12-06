package com.tikalk.worktracker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
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
        CircularProgressIndicator(
            modifier = Modifier
                .wrapContentWidth()
                .align(Alignment.Center)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ThisPreview() {
    TikalTheme {
        LoadingScreen()
    }
}
