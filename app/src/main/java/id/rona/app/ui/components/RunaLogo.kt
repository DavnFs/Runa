package id.rona.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.rona.app.R
import id.rona.app.ui.theme.LocalRonaColors

/**
 * The Runa brand mark: the swirl symbol on its own, tinted with the theme's
 * rose so it reads correctly on both light and dark glass. Used where a
 * wordmark would otherwise shout — the top bar.
 */
@Composable
fun RonaLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
    contentDescription: String? = "Runa",
) {
    val colors = LocalRonaColors.current
    Image(
        painter = painterResource(R.drawable.ic_logo_symbol),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(colors.cyclePrimary),
        modifier = modifier.size(size),
    )
}
