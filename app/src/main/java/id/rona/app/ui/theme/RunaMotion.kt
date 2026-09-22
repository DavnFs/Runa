package id.rona.app.ui.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

/**
 * Apple-grade fluid motion system for Rona.
 * Models iOS UIKit/SwiftUI spring physics and cubic-bezier response curves.
 */
object RonaMotion {

    /** Standard iOS Deceleration Curve (Ease-Out) */
    val AppleEaseOut = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)

    /** Standard iOS Acceleration Curve (Ease-In) */
    val AppleEaseIn = CubicBezierEasing(0.42f, 0.0f, 1.0f, 1.0f)

    /** iOS Navigation Push/Pop Physics Curve */
    val AppleNavCurve = CubicBezierEasing(0.32f, 0.72f, 0.0f, 1.0f)

    /** Fluid iOS Spring for Dock & Active Indicators */
    fun <T> appleSpring(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.82f,
        stiffness = 380f,
    )

    /** Bouncy tactile spring for button press / micro-interactions */
    fun <T> appleBouncySpring(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.68f,
        stiffness = 440f,
    )

    /** Gentle spring for bottom sheets / modal elements */
    fun <T> appleGentleSpring(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.90f,
        stiffness = 320f,
    )

    /** Color transition spec */
    val ColorSpec: AnimationSpec<Color> = tween(
        durationMillis = 240,
        easing = AppleEaseOut,
    )

    /** Float / Scale transition spec */
    val ScaleSpec: AnimationSpec<Float> = appleBouncySpring()

    // ───────────────────────── Navigation Transitions ─────────────────────────

    /** iOS Push Navigation Enter: Slide in from right with subtle fade */
    val NavPushEnter: EnterTransition = slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = spring(dampingRatio = 0.88f, stiffness = 360f),
    ) + fadeIn(
        animationSpec = tween(280, easing = AppleEaseOut),
    )

    /** iOS Push Navigation Exit: Slight parallax slide left with fade */
    val NavPushExit: ExitTransition = slideOutHorizontally(
        targetOffsetX = { (-it * 0.28f).roundToInt() },
        animationSpec = spring(dampingRatio = 0.88f, stiffness = 360f),
    ) + fadeOut(
        animationSpec = tween(220, easing = AppleEaseIn),
    )

    /** iOS Pop Navigation Enter: Return from left parallax */
    val NavPopEnter: EnterTransition = slideInHorizontally(
        initialOffsetX = { (-it * 0.28f).roundToInt() },
        animationSpec = spring(dampingRatio = 0.88f, stiffness = 360f),
    ) + fadeIn(
        animationSpec = tween(280, easing = AppleEaseOut),
    )

    /** iOS Pop Navigation Exit: Slide out to right */
    val NavPopExit: ExitTransition = slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = spring(dampingRatio = 0.88f, stiffness = 360f),
    ) + fadeOut(
        animationSpec = tween(220, easing = AppleEaseIn),
    )
}
