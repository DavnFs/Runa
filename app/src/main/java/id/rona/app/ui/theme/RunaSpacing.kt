package id.rona.app.ui.theme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Runa spacing scale. Screens use these tokens instead of raw dp.
 */
object RunaSpacing {
    val XS = 4.dp
    val S = 8.dp
    val M = 12.dp
    val L = 16.dp
    val XL = 20.dp
    val XXL = 24.dp
    val XXXL = 32.dp
    val HUGE = 40.dp

    /**
     * The single page margin. Every screen and the top bar share it so their
     * content aligns on one vertical line down the whole app.
     */
    val screenHorizontal = 20.dp

    /** Padding inside a card, so nested content sits on its own consistent inset. */
    val cardPadding = 16.dp

    /** Vertical gap between stacked cards. */
    val cardGap = 16.dp
    val topContent = 16.dp
    val dockVisualHeight = 64.dp
    val dockBottomMargin = 12.dp
    val dockScrollSafety = 16.dp
}

/** Standard horizontal page padding on phones. */
val RunaPageHorizontalPadding = RunaSpacing.screenHorizontal

/** Standard vertical page padding. */
val RunaPageVerticalPadding = RunaSpacing.XL

/**
 * Dynamic dock clearance calculation.
 * Accounts for visual dock height + bottom margin + navigationBars insets + scroll safety.
 */
@Composable
fun runaDynamicDockClearance(
    additionalPadding: Dp = RunaSpacing.dockScrollSafety
): Dp {
    val navBarsInset = WindowInsets.navigationBars
    val density = LocalDensity.current
    val navBarBottomDp = with(density) { navBarsInset.getBottom(density).toDp() }
    return RunaSpacing.dockVisualHeight + RunaSpacing.dockBottomMargin + navBarBottomDp + additionalPadding
}
