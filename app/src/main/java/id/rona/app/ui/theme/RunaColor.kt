package id.rona.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Runa semantic palette — the ONLY place raw colors live.
 * Apple-gray neutral ramp (BitChord-inspired) with Runa's rose as the accent.
 * Screens must consume these through MaterialTheme.colorScheme / RunaColors.
 * Do not scatter hex values across screens.
 */

// ───────────────────────── Light ─────────────────────────
private val Light = object {
    // Canvas & surfaces (Apple grays)
    val pageCanvas = Color(0xFFFFFFFF)
    val surface = Color(0xFFF7F7F9)
    val surfaceSoft = Color(0xFFF2F2F7)
    val elevatedSurface = Color(0xFFFFFFFF)
    val dividerSubtle = Color(0xFFE5E5EA)

    // Ink (text)
    val inkPrimary = Color(0xFF000000)
    val inkSecondary = Color(0xFF6E6E73)
    val inkTertiary = Color(0xFF8E8E93)

    // Cycle (rose accent)
    val cyclePrimary = Color(0xFF8C4558)
    val onCyclePrimary = Color(0xFFFFFFFF)
    val cycleContainer = Color(0xFFF6E4E9)
    val onCycleContainer = Color(0xFF2A1A20)

    // Plum
    val plumAccent = Color(0xFF73556F)
    val plumContainer = Color(0xFFF0EDF3)
    val onPlumContainer = Color(0xFF3A2C3E)

    // Sage (wellness)
    val sageAccent = Color(0xFF5F755E)
    val sageContainer = Color(0xFFEDF2EC)
    val onSageContainer = Color(0xFF243424)

    // Amber (insight / support)
    val amberAccent = Color(0xFFA66A28)
    val amberContainer = Color(0xFFF7F0E6)
    val onAmberContainer = Color(0xFF4A3115)

    // Status
    val error = Color(0xFFBA1A1A)
    val onError = Color(0xFFFFFFFF)
    val errorContainer = Color(0xFFFFDAD6)
    val onErrorContainer = Color(0xFFBA1A1A)
    val warningContainer = Color(0xFFF7F0E6)

    // Privacy / dock
    val privacySurface = Color(0xFFF2F2F7)
    val dockSurface = Color(0xD9FFFFFF)
    val dockContent = Color(0xFFFFFFFF)
    val dockInactiveContent = Color(0xFF000000)
    val dockSelectedContainer = Color(0xFF8C4558)
    val onDockSelectedContainer = Color(0xFFFFFFFF)
    val dockBorder = Color(0x66FFFFFF)

    // Chart / support
    val supportAccent = Color(0xFFA95D70)
}

// ───────────────────────── Dark ─────────────────────────
private val Dark = object {
    // Canvas & surfaces (pure black + Apple grays)
    val pageCanvas = Color(0xFF000000)
    val surface = Color(0xFF0D0D0F)
    val surfaceSoft = Color(0xFF1C1C1E)
    val elevatedSurface = Color(0xFF1C1C1E)
    val dividerSubtle = Color(0xFF2C2C2E)

    // Ink (text)
    val inkPrimary = Color(0xFFFFFFFF)
    val inkSecondary = Color(0xFF8E8E93)
    val inkTertiary = Color(0xFF6E6E73)

    // Cycle (vivid rose accent — links, arcs, badges; never button fill)
    val cyclePrimary = Color(0xFFF0788F)
    val onCyclePrimary = Color(0xFF3A1018)
    val cycleContainer = Color(0xFF3A1F27)
    val onCycleContainer = Color(0xFFFFE4EA)

    // Plum
    val plumAccent = Color(0xFFDDB5D6)
    val plumContainer = Color(0xFF2A2430)
    val onPlumContainer = Color(0xFFF0E2F0)

    // Sage (wellness)
    val sageAccent = Color(0xFFB3CCB2)
    val sageContainer = Color(0xFF232D23)
    val onSageContainer = Color(0xFFE2F0E0)

    // Amber (insight / support)
    val amberAccent = Color(0xFFF0BF87)
    val amberContainer = Color(0xFF2E2619)
    val onAmberContainer = Color(0xFFFBE8D0)

    // Status
    val error = Color(0xFFFFB4AB)
    val onError = Color(0xFF690005)
    val errorContainer = Color(0xFF690005)
    val onErrorContainer = Color(0xFFFFDAD6)
    val warningContainer = Color(0xFF2E2619)

    // Privacy / dock
    val privacySurface = Color(0xFF1C1C1E)
    val dockSurface = Color(0xB31C1C1E)
    val dockContent = Color(0xFF000000)
    val dockInactiveContent = Color(0xFF8E8E93)
    val dockSelectedContainer = Color(0xFFFFFFFF)
    val onDockSelectedContainer = Color(0xFF000000)
    val dockBorder = Color(0x33FFFFFF)

    // Chart / support
    val supportAccent = Color(0xFFE8A8B8)
}

/** Runa semantic color container — screens read these via LocalRonaColors. */
data class RunaColors(
    val pageCanvas: Color,
    val surfaceSoft: Color,
    val elevatedSurface: Color,
    val dividerSubtle: Color,
    val inkPrimary: Color,
    val inkSecondary: Color,
    val inkTertiary: Color,
    val cyclePrimary: Color,
    val onCyclePrimary: Color,
    val cycleContainer: Color,
    val onCycleContainer: Color,
    val plumAccent: Color,
    val plumContainer: Color,
    val onPlumContainer: Color,
    val sageAccent: Color,
    val sageContainer: Color,
    val onSageContainer: Color,
    val amberAccent: Color,
    val amberContainer: Color,
    val onAmberContainer: Color,
    val warningContainer: Color,
    val privacySurface: Color,
    val dockSurface: Color,
    val dockContent: Color,
    val dockInactiveContent: Color,
    val dockSelectedContainer: Color,
    val onDockSelectedContainer: Color,
    val dockBorder: Color,
    val supportAccent: Color,
)

val LightRonaColors = RunaColors(
    pageCanvas = Light.pageCanvas,
    surfaceSoft = Light.surfaceSoft,
    elevatedSurface = Light.elevatedSurface,
    dividerSubtle = Light.dividerSubtle,
    inkPrimary = Light.inkPrimary,
    inkSecondary = Light.inkSecondary,
    inkTertiary = Light.inkTertiary,
    cyclePrimary = Light.cyclePrimary,
    onCyclePrimary = Light.onCyclePrimary,
    cycleContainer = Light.cycleContainer,
    onCycleContainer = Light.onCycleContainer,
    plumAccent = Light.plumAccent,
    plumContainer = Light.plumContainer,
    onPlumContainer = Light.onPlumContainer,
    sageAccent = Light.sageAccent,
    sageContainer = Light.sageContainer,
    onSageContainer = Light.onSageContainer,
    amberAccent = Light.amberAccent,
    amberContainer = Light.amberContainer,
    onAmberContainer = Light.onAmberContainer,
    warningContainer = Light.warningContainer,
    privacySurface = Light.privacySurface,
    dockSurface = Light.dockSurface,
    dockContent = Light.dockContent,
    dockInactiveContent = Light.dockInactiveContent,
    dockSelectedContainer = Light.dockSelectedContainer,
    onDockSelectedContainer = Light.onDockSelectedContainer,
    dockBorder = Light.dockBorder,
    supportAccent = Light.supportAccent,
)

val DarkRonaColors = RunaColors(
    pageCanvas = Dark.pageCanvas,
    surfaceSoft = Dark.surfaceSoft,
    elevatedSurface = Dark.elevatedSurface,
    dividerSubtle = Dark.dividerSubtle,
    inkPrimary = Dark.inkPrimary,
    inkSecondary = Dark.inkSecondary,
    inkTertiary = Dark.inkTertiary,
    cyclePrimary = Dark.cyclePrimary,
    onCyclePrimary = Dark.onCyclePrimary,
    cycleContainer = Dark.cycleContainer,
    onCycleContainer = Dark.onCycleContainer,
    plumAccent = Dark.plumAccent,
    plumContainer = Dark.plumContainer,
    onPlumContainer = Dark.onPlumContainer,
    sageAccent = Dark.sageAccent,
    sageContainer = Dark.sageContainer,
    onSageContainer = Dark.onSageContainer,
    amberAccent = Dark.amberAccent,
    amberContainer = Dark.amberContainer,
    onAmberContainer = Dark.onAmberContainer,
    warningContainer = Dark.warningContainer,
    privacySurface = Dark.privacySurface,
    dockSurface = Dark.dockSurface,
    dockContent = Dark.dockContent,
    dockInactiveContent = Dark.dockInactiveContent,
    dockSelectedContainer = Dark.dockSelectedContainer,
    onDockSelectedContainer = Dark.onDockSelectedContainer,
    dockBorder = Dark.dockBorder,
    supportAccent = Dark.supportAccent,
)

/** Maps Runa palette onto Material 3 slots so Material components inherit it. */
val RunaLightColorScheme = lightColorScheme(
    primary = Light.cyclePrimary,
    onPrimary = Light.onCyclePrimary,
    primaryContainer = Light.cycleContainer,
    onPrimaryContainer = Light.onCycleContainer,
    secondary = Light.plumAccent,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Light.plumContainer,
    onSecondaryContainer = Light.onPlumContainer,
    tertiary = Light.sageAccent,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Light.sageContainer,
    onTertiaryContainer = Light.onSageContainer,
    error = Light.error,
    onError = Light.onError,
    errorContainer = Light.errorContainer,
    onErrorContainer = Light.onErrorContainer,
    background = Light.pageCanvas,
    onBackground = Light.inkPrimary,
    surface = Light.surface,
    onSurface = Light.inkPrimary,
    surfaceVariant = Light.surfaceSoft,
    onSurfaceVariant = Light.inkSecondary,
    outline = Light.inkTertiary,
    outlineVariant = Light.dividerSubtle,
    surfaceContainer = Light.surfaceSoft,
    surfaceContainerHigh = Light.surfaceSoft,
    surfaceContainerHighest = Color(0xFFEDEDF2),
    surfaceContainerLow = Light.surface,
    surfaceContainerLowest = Light.surface,
    surfaceDim = Color(0xFFEDEDF2),
    surfaceBright = Light.surface,
    inverseSurface = Light.dockSurface,
    inverseOnSurface = Light.dockContent,
    inversePrimary = Light.cyclePrimary,
    surfaceTint = Light.cyclePrimary,
)

val RunaDarkColorScheme = darkColorScheme(
    // Apple-style: primary buttons are white on black; rose is the accent.
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Dark.cycleContainer,
    onPrimaryContainer = Dark.onCycleContainer,
    secondary = Dark.plumAccent,
    onSecondary = Color(0xFF2A2430),
    secondaryContainer = Dark.plumContainer,
    onSecondaryContainer = Dark.onPlumContainer,
    tertiary = Dark.sageAccent,
    onTertiary = Color(0xFF232D23),
    tertiaryContainer = Dark.sageContainer,
    onTertiaryContainer = Dark.onSageContainer,
    error = Dark.error,
    onError = Dark.onError,
    errorContainer = Dark.errorContainer,
    onErrorContainer = Dark.onErrorContainer,
    background = Dark.pageCanvas,
    onBackground = Dark.inkPrimary,
    surface = Dark.surface,
    onSurface = Dark.inkPrimary,
    surfaceVariant = Dark.surfaceSoft,
    onSurfaceVariant = Dark.inkSecondary,
    outline = Dark.inkTertiary,
    outlineVariant = Dark.dividerSubtle,
    surfaceContainer = Dark.surfaceSoft,
    surfaceContainerHigh = Dark.surfaceSoft,
    surfaceContainerHighest = Color(0xFF2C2C2E),
    surfaceContainerLow = Dark.surface,
    surfaceContainerLowest = Dark.pageCanvas,
    surfaceDim = Dark.pageCanvas,
    surfaceBright = Color(0xFF2C2C2E),
    inverseSurface = Dark.dockContent,
    inverseOnSurface = Dark.dockSurface,
    inversePrimary = Dark.cyclePrimary,
    surfaceTint = Dark.cyclePrimary,
)
