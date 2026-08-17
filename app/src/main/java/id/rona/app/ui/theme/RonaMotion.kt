package id.rona.app.ui.theme

import androidx.compose.animation.core.tween

/*
 * Rona motion tokens — restrained, calm. 180–220ms for selection.
 */
object RonaMotion {
    /** Selection / pill container transitions. */
    val Selection = tween<Float>(durationMillis = 200)

    /** Color transitions (icons, labels). */
    val Color = tween<Int>(durationMillis = 200)
}
