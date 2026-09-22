package id.rona.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/*
 * Rona shape tokens. Components use these instead of raw dp radii.
 */
val RonaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

/** Bottom sheet top corners — 32dp. */
val RonaBottomSheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

/** Full pill for the floating nav dock and pills. */
val RonaPillShape = RoundedCornerShape(999.dp)

/** Circular — used for cycle hero, companion buttons, dots. */
val RonaCircleShape = RoundedCornerShape(50)
