package com.purnendu.contactly.utils

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Provides Material 3 Expressive animation utilities
 */

// A reusable animation that simu
// lates the "expressive" feel with bouncy transitions
@Suppress("UNCHECKED_CAST")
@Composable
fun <T> rememberExpressiveAnimation(
    targetValue: T,
    animationSpec: AnimationSpec<T> = spring(
        stiffness = Spring.StiffnessMedium,
        dampingRatio = Spring.DampingRatioMediumBouncy
    )
): State<T> {
    return when (targetValue) {
        is Float -> animateFloatAsState(
            targetValue = targetValue as Float,
            animationSpec = animationSpec as AnimationSpec<Float>,
            label = "Expressive Animation Float"
        ) as State<T>

        is Int -> animateIntAsState(
            targetValue = targetValue as Int,
            animationSpec = animationSpec as AnimationSpec<Int>,
            label = "Expressive Animation Int"
        ) as State<T>

        is Dp -> animateDpAsState(
            targetValue = targetValue as Dp,
            animationSpec = animationSpec as AnimationSpec<Dp>,
            label = "Expressive Animation Dp"
        ) as State<T>

        else -> throw IllegalArgumentException("Type not supported for expressive animation")
    }
}

// Extension function to add expressive scaling animation to any composable
fun Modifier.expressiveScale(
    scale: Float = 1f,
    animationSpec: AnimationSpec<Float> = spring(
        stiffness = Spring.StiffnessMedium,
        dampingRatio = Spring.DampingRatioMediumBouncy
    )
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "expressiveScale"
        value = scale
    }
) {
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = animationSpec,
        label = "Expressive Scale Animation"
    )
    scale(animatedScale)
}

// Animation that simulates the expressive material motion
fun Modifier.expressiveElevation(
    elevation: Dp = 0.dp,
    animationSpec: AnimationSpec<Dp> = tween(durationMillis = 300)
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "expressiveElevation"
        value = elevation
    }
) {
    val animatedElevation by animateDpAsState(
        targetValue = elevation,
        animationSpec = animationSpec,
        label = "Expressive Elevation Animation"
    )
    graphicsLayer {
        shadowElevation = animatedElevation.toPx()
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        clip = true
    }
}