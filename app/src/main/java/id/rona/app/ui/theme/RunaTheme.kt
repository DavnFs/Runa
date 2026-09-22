package id.rona.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import id.rona.app.domain.model.ThemeMode

val LocalRonaColors = staticCompositionLocalOf { LightRonaColors }

@Composable
fun RunaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        // Runa palette is DEFAULT. Dynamic color is optional opt-in only.
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> RunaDarkColorScheme
        else -> RunaLightColorScheme
    }

    val runaColors = if (darkTheme) DarkRonaColors else LightRonaColors

    CompositionLocalProvider(LocalRonaColors provides runaColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = RunaTypography,
            shapes = RunaShapes,
            content = content,
        )
    }
}
