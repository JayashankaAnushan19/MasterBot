package com.masterbot.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MasterBotAccent = Color(0xFF00E5A0)
private val MasterBotAccentDark = Color(0xFF00B383)

private val DarkColors = darkColorScheme(
    primary = MasterBotAccent,
    secondary = MasterBotAccentDark,
    background = Color(0xFF101820),
    surface = Color(0xFF17212B),
)

private val LightColors = lightColorScheme(
    primary = MasterBotAccentDark,
    secondary = MasterBotAccent,
)

@Composable
fun MasterBotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
