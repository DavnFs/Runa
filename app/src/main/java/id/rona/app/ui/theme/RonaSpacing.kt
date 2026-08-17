package id.rona.app.ui.theme

import androidx.compose.ui.unit.dp

/*
 * Rona spacing scale. Screens use these tokens instead of raw dp.
 */
object RonaSpacing {
    val XS = 4.dp
    val S = 8.dp
    val M = 12.dp
    val L = 16.dp
    val XL = 20.dp
    val XXL = 24.dp
    val XXXL = 32.dp
    val HUGE = 40.dp
}

/** Standard horizontal page padding on phones. */
val RonaPageHorizontalPadding = RonaSpacing.XL

/** Standard vertical page padding. */
val RonaPageVerticalPadding = RonaSpacing.XL
