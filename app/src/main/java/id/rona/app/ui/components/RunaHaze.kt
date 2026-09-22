package id.rona.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * HazeState shared across the main shell so the top bar and floating dock can
 * blur the page content scrolling behind them. Null in previews/screens that
 * have no glass source registered — [Modifier.runaGlass] then falls back to a
 * plain translucent fill.
 */
val LocalRonaHazeState = staticCompositionLocalOf<HazeState?> { null }

/**
 * Frosted-glass surface in the BitChord manner: real backdrop blur through
 * Haze when a [LocalRonaHazeState] is provided, a translucent [backgroundColor]
 * fill otherwise (previews, or Android < 12 where Haze itself degrades to a
 * scrim).
 */
@Composable
fun Modifier.runaGlass(
    shape: Shape,
    backgroundColor: Color,
    tint: Color = Color.Unspecified,
    blurRadius: Dp = 20.dp,
): Modifier {
    val state = LocalRonaHazeState.current
    return if (state != null) {
        this.hazeEffect(
            state = state,
            style = HazeStyle(
                backgroundColor = backgroundColor,
                tint = if (tint == Color.Unspecified) HazeTint.Unspecified else HazeTint(tint),
                blurRadius = blurRadius,
            ),
        )
    } else {
        this.background(color = backgroundColor, shape = shape)
    }
}
