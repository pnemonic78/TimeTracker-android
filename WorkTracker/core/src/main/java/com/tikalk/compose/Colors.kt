package com.tikalk.compose

import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color

// text is `onBackground`
// button background is primary

val tikalLightColors = lightColors(
    primary = Color(0xFFFFCC80),
    primaryVariant = Color(0XFFFF8A65),
    secondary = Color(0xFFFF6E40),
    secondaryVariant = Color(0xFFFF6E40).copy(alpha = 0.75f)

)

val tikalDarkColors = darkColors(
    primary = Color(0xFFFFCC80),
    primaryVariant = Color(0XFFFF8A65),
    secondary = Color(0xFFFF6E40),
    secondaryVariant = Color(0xFFFF6E40).copy(alpha = 0.75f)
)
