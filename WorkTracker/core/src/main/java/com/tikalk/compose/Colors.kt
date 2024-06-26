package com.tikalk.compose

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// text is `onBackground`
// button background is primary

val tikalLightColors = lightColorScheme(
    primary = Color(0xFFFF6E40),
    primaryContainer = Color(0xFFFFCC80),
    secondary = Color(0xFFFF6E40),
    secondaryContainer = Color(0xFFFFCC80),
    surfaceVariant = Color(0xFFFFF5E7)
)

val tikalDarkColors = darkColorScheme(
    primary = Color(0XFFFF8A65),
    primaryContainer = Color(0xFFFFCC80),
    secondary = Color(0xFFFFCC80)
)

val ColorScheme.link
    @Composable
    get() = MaterialTheme.colorScheme.secondary
