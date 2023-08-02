package com.tikalk.worktracker

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tikalk.compose.TikalTheme

@Composable
fun EmptyListScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.subtitle1,
            text = stringResource(id = R.string.error_no_items),
            color = MaterialTheme.colors.onBackground
        )
    }
}

@Preview(name = "default", showBackground = true)
@Preview(name = "dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ThisPreview() {
    TikalTheme {
        EmptyListScreen()
    }
}
