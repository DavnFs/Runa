package id.rona.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Adaptive helpers so Rona holds together across phone sizes, landscape, and
 * wide screens without a full WindowSizeClass treatment.
 */

/** Widest a page column may grow; content is centred beyond this (tablets). */
const val RonaPageMaxWidthDp = 480

/**
 * Caps page content at [RonaPageMaxWidthDp]. Pair with a parent
 * `Box(contentAlignment = Alignment.TopCenter)` so wide screens centre the page
 * instead of stretching it edge-to-edge. The cap sits outermost so the
 * composable itself reports the capped width and the parent can centre it.
 */
fun Modifier.ronaPageContainer(): Modifier =
    widthIn(max = RonaPageMaxWidthDp.dp).fillMaxWidth()

/**
 * Hero circle diameter that fits the actual screen: capped by [cap], by 72% of
 * the screen width, and by 40% of the height so short/landscape screens shrink
 * the hero instead of crowding the content around it.
 */
@Composable
fun ronaHeroDiameter(cap: Dp): Dp {
    val config = LocalConfiguration.current
    val byWidth = config.screenWidthDp.dp * 0.72f
    val byHeight = config.screenHeightDp.dp * 0.40f
    return minOf(cap, byWidth, byHeight)
}
