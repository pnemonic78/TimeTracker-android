package com.tikalk.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

typealias ComposableContent = @Composable (() -> Unit)
typealias UnitCallback = (() -> Unit)

@Composable
fun TikalTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: ComposableContent
) {
    MaterialTheme(
        colorScheme = if (isDarkTheme) tikalDarkColors else tikalLightColors,
        content = content
    )
}
