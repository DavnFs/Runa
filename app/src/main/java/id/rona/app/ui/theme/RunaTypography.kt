package id.rona.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import id.rona.app.R

/*
 * Runa typography — Inter (OFL-licensed SF Pro alternative), heavy and tight,
 * in the Apple Music manner. User font scaling still applies (sp units).
 */
val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.W400),
    Font(R.font.inter_medium, FontWeight.W500),
    Font(R.font.inter_semibold, FontWeight.W600),
    Font(R.font.inter_bold, FontWeight.W700),
    Font(R.font.inter_extrabold, FontWeight.W800),
)

// Heavy, tight typography — the backbone of the Apple Music look.
val RunaTypography = Typography(
    // Hero / cycle day
    displayMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.W800,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.8).sp,
    ),
    // Screen title
    headlineSmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.W800,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.7).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.W700,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.4).sp,
    ),
    // Section heading
    titleMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.W600,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.W700,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.3).sp,
    ),
    // Main body
    bodyLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    // Supporting
    bodySmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.W400,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    ),
    // Metadata
    labelSmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.W600,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.W600,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.W600,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
)

/** Tabular figures for cycle statistics. */
val RunaStatsTextStyle = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.W700,
    fontSize = 15.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.sp,
    textAlign = TextAlign.Start,
)

/**
 * Brand wordmark style (RUNA), used on the lock screen and settings header.
 * Soft and quietly spaced — heavy weight plus wide tracking read as rigid.
 */
val RunaWordmarkStyle = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.W600,
    fontSize = 24.sp,
    lineHeight = 28.sp,
    letterSpacing = 0.5.sp,
    textAlign = TextAlign.Center,
)

/** Uppercase micro-label style (e.g. BERIKUTNYA, SIKLUS RATA-RATA). */
val RunaMicroLabelStyle = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.W700,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 1.2.sp,
)
