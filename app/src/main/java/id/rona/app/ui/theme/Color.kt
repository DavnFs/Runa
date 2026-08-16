package id.rona.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val WarmLightColors = lightColorScheme(
    primary = Color(0xFF8C5A4B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF3A0B02),
    secondary = Color(0xFF6F7D5F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE7F0D8),
    onSecondaryContainer = Color(0xFF1C2514),
    tertiary = Color(0xFF7C6078),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD7F6),
    onTertiaryContainer = Color(0xFF311D2F),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFDF8F3),
    onBackground = Color(0xFF211A17),
    surface = Color(0xFFFDF8F3),
    onSurface = Color(0xFF211A17),
    surfaceVariant = Color(0xFFF4EBE2),
    onSurfaceVariant = Color(0xFF53443D),
    outline = Color(0xFF85736B),
    outlineVariant = Color(0xFFD7C1B8),
    surfaceContainer = Color(0xFFF4EBE2),
    surfaceContainerHigh = Color(0xFFECE1D8),
    surfaceContainerHighest = Color(0xFFE6DAD1),
    surfaceContainerLow = Color(0xFFFFFBFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE4D7D0),
    surfaceBright = Color(0xFFFDF8F3),
    inverseSurface = Color(0xFF372F2C),
    inverseOnSurface = Color(0xFFF0E8E4),
    inversePrimary = Color(0xFFFFB59E),
    surfaceTint = Color(0xFF8C5A4B),
)

private val WarmDarkColors = darkColorScheme(
    primary = Color(0xFFFFB59E),
    onPrimary = Color(0xFF542C1F),
    primaryContainer = Color(0xFF714235),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Color(0xFFC4D3B1),
    onSecondary = Color(0xFF333F28),
    secondaryContainer = Color(0xFF49563D),
    onSecondaryContainer = Color(0xFFE7F0D8),
    tertiary = Color(0xFFE7BAD9),
    onTertiary = Color(0xFF462C42),
    tertiaryContainer = Color(0xFF5F4259),
    onTertiaryContainer = Color(0xFFFFD7F6),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF171412),
    onBackground = Color(0xFFEDE0D9),
    surface = Color(0xFF171412),
    onSurface = Color(0xFFEDE0D9),
    surfaceVariant = Color(0xFF53443D),
    onSurfaceVariant = Color(0xFFD7C1B8),
    outline = Color(0xFFA08C83),
    outlineVariant = Color(0xFF53443D),
    surfaceContainer = Color(0xFF211C19),
    surfaceContainerHigh = Color(0xFF2C2623),
    surfaceContainerHighest = Color(0xFF37312D),
    surfaceContainerLow = Color(0xFF211A17),
    surfaceContainerLowest = Color(0xFF120F0D),
    surfaceDim = Color(0xFF171412),
    surfaceBright = Color(0xFF3D3734),
    inverseSurface = Color(0xFFEDE0D9),
    inverseOnSurface = Color(0xFF372F2C),
    inversePrimary = Color(0xFF8C5A4B),
    surfaceTint = Color(0xFFFFB59E),
)

@Composable
fun RonaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> WarmDarkColors
        else -> WarmLightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = RonaTypography,
        content = content
    )
}
