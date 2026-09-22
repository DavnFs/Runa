package id.rona.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/*
 * Runa shape tokens. Tighter radii in the Apple manner.
 * Components use these instead of raw dp radii.
 */
val RunaShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(26.dp),
)

/** Bottom sheet top corners — 24dp. */
val RunaBottomSheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

/** Full pill for the floating nav dock and pills. */
val RunaPillShape = RoundedCornerShape(999.dp)

/** Circular — used for cycle hero, companion buttons, dots. */
val RunaCircleShape = RoundedCornerShape(50)
