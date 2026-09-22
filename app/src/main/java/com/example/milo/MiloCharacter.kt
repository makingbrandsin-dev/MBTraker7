package com.example.milo

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.example.domain.milo.MiloState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.sin

/**
 * Reusable animated Milo Character Composable.
 * Adapts smoothly to all 13 states with event-driven transitions.
 */
@Composable
fun MiloCharacter(
    state: MiloState,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    showStateBadge: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    // Infinite Animation Transitions
    val infiniteTransition = rememberInfiniteTransition(label = "MiloAnimations")

    // Breathing loop
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingScale"
    )

    // Blink cycle (0f = open, 1f = closed)
    val blinkProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3600
                0f at 0
                0f at 3100
                1f at 3200
                0f at 3300
                0f at 3600
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "BlinkProgress"
    )

    // Wave / Typing / Bounce cycle
    val waveRotation by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "WaveRotation"
    )

    // Jump offset for celebration
    val jumpOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "JumpOffset"
    )

    // Glow pulse for thinking / warning
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val confetti = remember { generateConfettiParticles(20) }

    Box(
        modifier = modifier
            .size(size)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        // Subtle aura ring behind Milo based on current state color
        Surface(
            shape = CircleShape,
            color = state.primaryColor.copy(alpha = 0.12f),
            modifier = Modifier
                .fillMaxSize()
                .scale(breathingScale)
        ) {}

        AnimatedContent(
            targetState = state,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.85f) togetherWith
                        fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.9f)
            },
            label = "MiloStateAnimation"
        ) { targetState ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = if (targetState == MiloState.CELEBRATION) jumpOffset.dp else 0.dp),
                contentAlignment = Alignment.Center
            ) {
                // Render Milo Canvas Base (Head, Mane, Ears, Eyes, MB Polo, Nose, Smile)
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(breathingScale)
                ) {
                    drawMiloBase(
                        state = targetState,
                        blink = blinkProgress > 0.6f,
                        waveRot = waveRotation,
                        pulse = pulseAlpha
                    )
                }

                // Render State-Specific Floating Vector Overlays
                when (targetState) {
                    MiloState.IDLE -> MiloIdleOverlay()
                    MiloState.WELCOME -> MiloWelcomeOverlay(waveRotation)
                    MiloState.WORKING -> MiloWorkingOverlay(waveRotation)
                    MiloState.THINKING -> MiloThinkingOverlay(pulseAlpha)
                    MiloState.LEAD_IMPORTED -> MiloLeadImportedOverlay()
                    MiloState.NEW_LEAD -> MiloNewLeadOverlay()
                    MiloState.FOLLOW_UP -> MiloFollowUpOverlay()
                    MiloState.SUCCESS -> MiloSuccessOverlay()
                    MiloState.CONVERTED -> MiloConvertedOverlay(confetti, pulseAlpha)
                    MiloState.WARNING -> MiloWarningOverlay(pulseAlpha)
                    MiloState.ERROR -> MiloErrorOverlay()
                    MiloState.GOODBYE -> MiloGoodbyeOverlay(waveRotation)
                    MiloState.CELEBRATION -> MiloCelebrationOverlay(confetti)
                }
            }
        }

        // Optional State Emoji Badge at bottom-right
        if (showStateBadge) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(state.emoji, fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Custom Canvas Renderer for Milo the Lion.
 * Features:
 * - Golden Lion Mane
 * - Rounded Lion Ears with warm inner tone
 * - Expressive Lion Head & Muzzle
 * - Blinking expressive eyes
 * - Navy Blue MB Polo Shirt with embroidered "MB" badge
 */
private fun DrawScope.drawMiloBase(
    state: MiloState,
    blink: Boolean,
    waveRot: Float,
    pulse: Float
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f

    val maneColor = Color(0xFFD97706) // Rich golden-amber mane
    val maneHighlight = Color(0xFFF59E0B)
    val furColor = Color(0xFFFBBF24) // Warm lion fur
    val muzzleColor = Color(0xFFFEF3C7) // Soft cream muzzle
    val navyPoloColor = Color(0xFF0F172A) // Brand dark blue polo
    val noseColor = Color(0xFF78350F) // Chocolate brown nose

    // 1. Draw Mane (Lush circular scalloped mane petals)
    val maneRadius = w * 0.42f
    for (i in 0 until 10) {
        val angle = (i * 36.0 * Math.PI / 180.0).toFloat()
        val petalX = cx + (maneRadius * 0.72f) * kotlin.math.cos(angle)
        val petalY = (cy - h * 0.05f) + (maneRadius * 0.72f) * kotlin.math.sin(angle)
        drawCircle(
            color = if (i % 2 == 0) maneColor else maneHighlight,
            radius = maneRadius * 0.38f,
            center = Offset(petalX, petalY)
        )
    }

    // 2. Draw Navy MB Polo Shirt (Shoulders & Torso)
    val shirtTop = cy + h * 0.18f
    val shirtPath = Path().apply {
        moveTo(cx - w * 0.34f, h)
        lineTo(cx - w * 0.22f, shirtTop)
        quadraticBezierTo(cx, shirtTop - h * 0.02f, cx + w * 0.22f, shirtTop)
        lineTo(cx + w * 0.34f, h)
        close()
    }
    drawPath(shirtPath, color = navyPoloColor)

    // Polo Collar (White / Light trim)
    drawPath(
        path = Path().apply {
            moveTo(cx - w * 0.12f, shirtTop)
            lineTo(cx, shirtTop + h * 0.10f)
            lineTo(cx + w * 0.12f, shirtTop)
            close()
        },
        color = Color(0xFF1E293B)
    )

    // 3. Draw Ears
    val earRadius = w * 0.12f
    val leftEarCenter = Offset(cx - w * 0.26f, cy - h * 0.28f)
    val rightEarCenter = Offset(cx + w * 0.26f, cy - h * 0.28f)

    // Outer ears
    drawCircle(color = maneColor, radius = earRadius, center = leftEarCenter)
    drawCircle(color = maneColor, radius = earRadius, center = rightEarCenter)
    // Inner ears
    drawCircle(color = Color(0xFFFDE68A), radius = earRadius * 0.6f, center = leftEarCenter)
    drawCircle(color = Color(0xFFFDE68A), radius = earRadius * 0.6f, center = rightEarCenter)

    // 4. Draw Lion Head (Warm fur circle)
    val headRadius = w * 0.30f
    drawCircle(
        color = furColor,
        radius = headRadius,
        center = Offset(cx, cy - h * 0.04f)
    )

    // 5. Draw Muzzle (Cream lower face)
    val muzzleCenter = Offset(cx, cy + h * 0.06f)
    drawOval(
        color = muzzleColor,
        topLeft = Offset(muzzleCenter.x - w * 0.18f, muzzleCenter.y - h * 0.12f),
        size = Size(w * 0.36f, h * 0.22f)
    )

    // 6. Draw Nose
    val nosePath = Path().apply {
        moveTo(cx - w * 0.05f, muzzleCenter.y - h * 0.04f)
        lineTo(cx + w * 0.05f, muzzleCenter.y - h * 0.04f)
        lineTo(cx, muzzleCenter.y + h * 0.01f)
        close()
    }
    drawPath(nosePath, color = noseColor)

    // 7. Draw Smile & Mouth
    val mouthY = muzzleCenter.y + h * 0.05f
    when (state) {
        MiloState.ERROR, MiloState.WARNING -> {
            // Serious / Concerned straight line
            drawLine(
                color = noseColor,
                start = Offset(cx - w * 0.06f, mouthY),
                end = Offset(cx + w * 0.06f, mouthY),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }
        MiloState.CONVERTED, MiloState.CELEBRATION, MiloState.SUCCESS, MiloState.NEW_LEAD -> {
            // Big open happy smile
            drawArc(
                color = Color(0xFF991B1B),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(cx - w * 0.08f, mouthY - h * 0.02f),
                size = Size(w * 0.16f, h * 0.10f)
            )
        }
        else -> {
            // Gentle friendly smile curve
            drawArc(
                color = noseColor,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(cx - w * 0.07f, mouthY - h * 0.04f),
                size = Size(w * 0.14f, h * 0.08f),
                style = Stroke(width = 3.5f, cap = StrokeCap.Round)
            )
        }
    }

    // 8. Draw Eyes (With Blinking Support)
    val eyeY = cy - h * 0.09f
    val leftEyeX = cx - w * 0.11f
    val rightEyeX = cx + w * 0.11f
    val eyeRadius = w * 0.045f

    if (blink) {
        // Closed / Happy curved eyelids
        drawArc(
            color = Color(0xFF1E293B),
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(leftEyeX - eyeRadius, eyeY - eyeRadius * 0.5f),
            size = Size(eyeRadius * 2, eyeRadius),
            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0xFF1E293B),
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(rightEyeX - eyeRadius, eyeY - eyeRadius * 0.5f),
            size = Size(eyeRadius * 2, eyeRadius),
            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
        )
    } else {
        // Open expressive eyes
        drawCircle(color = Color(0xFF0F172A), radius = eyeRadius, center = Offset(leftEyeX, eyeY))
        drawCircle(color = Color(0xFF0F172A), radius = eyeRadius, center = Offset(rightEyeX, eyeY))

        // Eye catchlight sparkles (White reflections)
        drawCircle(color = Color.White, radius = eyeRadius * 0.35f, center = Offset(leftEyeX - eyeRadius * 0.3f, eyeY - eyeRadius * 0.3f))
        drawCircle(color = Color.White, radius = eyeRadius * 0.35f, center = Offset(rightEyeX - eyeRadius * 0.3f, eyeY - eyeRadius * 0.3f))
    }

    // 9. Rosy Cheeks
    drawCircle(
        color = Color(0xFFF87171).copy(alpha = 0.35f),
        radius = w * 0.045f,
        center = Offset(cx - w * 0.18f, cy + h * 0.02f)
    )
    drawCircle(
        color = Color(0xFFF87171).copy(alpha = 0.35f),
        radius = w * 0.045f,
        center = Offset(cx + w * 0.18f, cy + h * 0.02f)
    )
}

// -------------------------------------------------------------
// State Overlay Components
// -------------------------------------------------------------

@Composable
private fun MiloIdleOverlay() {
    // Subtle sparkles
}

@Composable
private fun MiloWelcomeOverlay(waveRot: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 4.dp, top = 14.dp)
                .rotate(waveRot)
                .size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("👋", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun MiloWorkingOverlay(waveRot: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF1E293B),
            shadowElevation = 3.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp)
                .width(42.dp)
                .height(20.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("MB", color = Color(0xFF38BDF8), fontWeight = FontWeight.Black, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun MiloThinkingOverlay(pulseAlpha: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            shape = CircleShape,
            color = Color(0xFFFEF3C7).copy(alpha = pulseAlpha),
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 2.dp, end = 2.dp)
                .size(26.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun MiloLeadImportedOverlay() {
    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF2563EB),
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("+12", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun MiloNewLeadOverlay() {
    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            shape = CircleShape,
            color = Color(0xFF7C3AED),
            shadowElevation = 3.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 4.dp, start = 4.dp)
                .size(20.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
private fun MiloFollowUpOverlay() {
    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            shape = CircleShape,
            color = Color(0xFF059669),
            shadowElevation = 3.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 2.dp)
                .size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun MiloSuccessOverlay() {
    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            shape = CircleShape,
            color = Color(0xFF10B981),
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 8.dp, end = 2.dp)
                .size(26.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ThumbUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun MiloConvertedOverlay(confetti: List<MiloConfettiParticle>, pulseAlpha: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Floating Confetti Dots
        confetti.take(12).forEach { p ->
            Box(
                modifier = Modifier
                    .offset(x = (p.xOffset * 0.4f).dp, y = (p.yOffset * 0.4f).dp)
                    .size(p.size.dp)
                    .clip(CircleShape)
                    .background(p.color)
                    .align(Alignment.Center)
            )
        }

        // Crown on Top of Mane
        Surface(
            shape = CircleShape,
            color = Color(0xFFF59E0B),
            shadowElevation = 3.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 0.dp)
                .size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("👑", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun MiloWarningOverlay(pulseAlpha: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            shape = CircleShape,
            color = Color(0xFFEA580C).copy(alpha = pulseAlpha),
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 2.dp)
                .size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun MiloErrorOverlay() {
    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            shape = CircleShape,
            color = Color(0xFFDC2626),
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 2.dp)
                .size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun MiloGoodbyeOverlay(waveRot: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            shape = CircleShape,
            color = Color(0xFF475569),
            shadowElevation = 2.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 4.dp)
                .rotate(-waveRot)
                .size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("🙋", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun MiloCelebrationOverlay(confetti: List<MiloConfettiParticle>) {
    Box(modifier = Modifier.fillMaxSize()) {
        confetti.forEach { p ->
            Box(
                modifier = Modifier
                    .offset(x = (p.xOffset * 0.45f).dp, y = (p.yOffset * 0.45f).dp)
                    .size(p.size.dp)
                    .clip(CircleShape)
                    .background(p.color)
                    .align(Alignment.Center)
            )
        }
    }
}
