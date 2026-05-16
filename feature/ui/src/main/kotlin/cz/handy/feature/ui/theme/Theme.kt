package cz.handy.feature.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors =
    darkColorScheme(
        primary = Accent,
        onPrimary = Navy,
        background = Navy,
        onBackground = Ink,
        surface = Navy,
        onSurface = Ink,
    )

@Composable
fun HandyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography,
        content = content,
    )
}
