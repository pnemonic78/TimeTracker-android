package com.tikalk.compose

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable

typealias ComposableContent = @Composable (() -> Unit)

@Composable
fun TikalTheme(
    // isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: ComposableContent
) {
    MaterialTheme(
        colors = tikalColors,
        content = content
    )
}
