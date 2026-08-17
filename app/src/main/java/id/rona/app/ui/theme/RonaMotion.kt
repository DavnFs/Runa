package id.rona.app.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color

/*
 * Rona motion tokens — restrained, calm. 200ms for selection transitions.
 */
object RonaMotion {

    /** Generic tween spec (Color, Float, Dp…). */
    fun <T> spec(): AnimationSpec<T> = tween(durationMillis = 200)

    /** Selection pill/container transitions (Color). */
    val Selection: AnimationSpec<Color> = tween(durationMillis = 200)

    /** Size transitions. */
    val Size: AnimationSpec<Float> = tween(durationMillis = 200)
}
