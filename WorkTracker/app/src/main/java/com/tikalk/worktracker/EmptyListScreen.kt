package com.tikalk.worktracker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tikalk.compose.TikalTheme

@Composable
fun EmptyListScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = stringResource(id = R.string.error_no_results)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ThisPreview() {
    TikalTheme {
        EmptyListScreen()
    }
}
