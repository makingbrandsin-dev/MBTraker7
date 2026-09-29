package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Interactive button click & hover lift-up effect modifier.
 * When touched/clicked/hovered, smoothly lifts the element up with animated
 * translationY, shadow elevation, and subtle scale spring effect.
 */
fun Modifier.liftOnPress(
    elevationLift: Dp = 8.dp,
    translateY: Dp = (-5).dp,
    scaleLift: Float = 1.03f,
    interactionSource: MutableInteractionSource? = null
): Modifier = composed {
    val actualSource = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by actualSource.collectIsPressedAsState()

    val animatedElevation by animateDpAsState(
        targetValue = if (isPressed) elevationLift else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "LiftElevation"
    )

    val animatedTranslationY by animateDpAsState(
        targetValue = if (isPressed) translateY else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "LiftTranslationY"
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) scaleLift else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "LiftScale"
    )

    this.graphicsLayer {
        this.translationY = animatedTranslationY.toPx()
        this.scaleX = animatedScale
        this.scaleY = animatedScale
        this.shadowElevation = animatedElevation.toPx()
    }
}
