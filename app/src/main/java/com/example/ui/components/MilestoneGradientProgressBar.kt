package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.BrandBlue
import com.example.util.MiloHaptics
import java.util.Locale

enum class CssGradientPalette(
    val colors: List<Color>,
    val badgeBg: Color,
    val badgeText: Color,
    val label: String
) {
    ELECTRIC_CYAN_EMERALD(
        colors = listOf(Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFF3B82F6)),
        badgeBg = Color(0xFFECFDF5),
        badgeText = Color(0xFF059669),
        label = "Emerald & Cyan"
    ),
    ROYAL_INDIGO_VIOLET(
        colors = listOf(Color(0xFF2563EB), Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFEC4899)),
        badgeBg = Color(0xFFEFF6FF),
        badgeText = Color(0xFF2563EB),
        label = "Royal Indigo"
    ),
    SUNSET_AMBER_CORAL(
        colors = listOf(Color(0xFFF59E0B), Color(0xFFEA580C), Color(0xFFE11D48)),
        badgeBg = Color(0xFFFEF3C7),
        badgeText = Color(0xFFB45309),
        label = "Sunset Coral"
    ),
    AURORA_NEON(
        colors = listOf(Color(0xFF00F5D4), Color(0xFF00BBF9), Color(0xFF7B2CBF)),
        badgeBg = Color(0xFFF3E8FF),
        badgeText = Color(0xFF7C3AED),
        label = "Aurora Neon"
    )
}

/**
 * 🎯 Milestone Gradient Progress Bar Component:
 * Renders a high-fidelity, CSS-gradient progress bar inside Task Management cards
 * to visually display the completion percentage of current sprint deliverables and milestones.
 */
@Composable
fun MilestoneGradientProgressBar(
    completionPercentage: Float, // 0.0f to 1.0f
    completedMilestoneCount: Int = 0,
    totalMilestoneCount: Int = 0,
    activeMilestoneName: String? = null,
    modifier: Modifier = Modifier,
    barHeight: Dp = 10.dp,
    palette: CssGradientPalette = CssGradientPalette.ROYAL_INDIGO_VIOLET,
    showMilestonePills: Boolean = true,
    showCheckpoints: Boolean = true
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val clampedProgress = completionPercentage.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "milestone_css_gradient_progress"
    )

    var showMilestoneInspectionDialog by remember { mutableStateOf(false) }

    val percentageInt = (clampedProgress * 100).toInt()

    val (milestoneStageLabel, milestoneStatusColor) = when {
        percentageInt >= 100 -> "🎉 Milestone 4: Final Deliverables Complete" to Color(0xFF16A34A)
        percentageInt >= 75 -> "🧪 Milestone 4: Quality Review & Client UAT" to Color(0xFF2563EB)
        percentageInt >= 50 -> "⚙️ Milestone 3: Core Implementation & Integrations" to Color(0xFF7C3AED)
        percentageInt >= 25 -> "📋 Milestone 2: Wireframing & Sprint Setup" to Color(0xFFD97706)
        else -> "🚀 Milestone 1: Kickoff & Task Scoping" to Color(0xFF475569)
    }

    val displayMilestoneTitle = activeMilestoneName ?: milestoneStageLabel

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("milestone_gradient_progress_container")
    ) {
        // 1. Milestone Header Info: Title, Stage Badge & Percentage
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        MiloHaptics.performReactionTick(context, haptic)
                        showMilestoneInspectionDialog = true
                    }
            ) {
                Icon(
                    imageVector = if (percentageInt >= 100) Icons.Default.Verified else Icons.Default.Flag,
                    contentDescription = "Milestone Progress",
                    tint = if (percentageInt >= 100) Color(0xFF16A34A) else palette.colors.first(),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = displayMilestoneTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Percentage Badge with subtle border & background
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = palette.badgeBg,
                border = BorderStroke(1.dp, palette.badgeText.copy(alpha = 0.25f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        MiloHaptics.performReactionTick(context, haptic)
                        showMilestoneInspectionDialog = true
                    }
                    .testTag("milestone_percentage_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$percentageInt%",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.badgeText
                    )
                    if (totalMilestoneCount > 0) {
                        Text(
                            text = " ($completedMilestoneCount/$totalMilestoneCount)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.badgeText.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        // 2. Visual CSS Gradient Progress Bar Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(RoundedCornerShape(100.dp))
                .background(Color(0xFFF1F5F9))
                .clickable {
                    MiloHaptics.performReactionTick(context, haptic)
                    showMilestoneInspectionDialog = true
                }
                .testTag("css_gradient_progress_track")
        ) {
            // Subtle inner inset border
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color.Transparent)
            )

            // Animated Gradient Progress Bar Fill
            if (animatedProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(100.dp))
                        .background(Brush.horizontalGradient(palette.colors))
                ) {
                    // Soft light shine effect on the leading edge
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(width = 16.dp, height = barHeight)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, Color.White.copy(alpha = 0.45f))
                                )
                            )
                    )
                }
            }

            // 3. Milestone Checkpoints (25%, 50%, 75%)
            if (showCheckpoints) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(0.25f, 0.50f, 0.75f).forEach { checkpoint ->
                        val isPassed = animatedProgress >= checkpoint
                        Box(
                            modifier = Modifier
                                .size(if (isPassed) 5.dp else 4.dp)
                                .clip(CircleShape)
                                .background(if (isPassed) Color.White.copy(alpha = 0.9f) else Color(0xFFCBD5E1))
                        )
                    }
                }
            }
        }

        // 4. Milestone Checkpoint Labels Footer (Optional)
        if (showMilestonePills) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "0% Kickoff",
                    fontSize = 9.5.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "50% Core Build",
                    fontSize = 9.5.sp,
                    color = if (percentageInt >= 50) palette.badgeText else Color(0xFF94A3B8),
                    fontWeight = if (percentageInt >= 50) FontWeight.Bold else FontWeight.Medium
                )
                Text(
                    text = "100% Verified",
                    fontSize = 9.5.sp,
                    color = if (percentageInt >= 100) Color(0xFF16A34A) else Color(0xFF94A3B8),
                    fontWeight = if (percentageInt >= 100) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }

    // 📋 Milestone Inspection Dialog
    if (showMilestoneInspectionDialog) {
        MilestoneBreakdownDialog(
            percentage = percentageInt,
            displayTitle = displayMilestoneTitle,
            completedMilestones = completedMilestoneCount,
            totalMilestones = totalMilestoneCount,
            palette = palette,
            onDismiss = { showMilestoneInspectionDialog = false }
        )
    }
}

/**
 * Inspection Modal detailing Milestone Stages
 */
@Composable
private fun MilestoneBreakdownDialog(
    percentage: Int,
    displayTitle: String,
    completedMilestones: Int,
    totalMilestones: Int,
    palette: CssGradientPalette,
    onDismiss: () -> Unit
) {
    val stages = listOf(
        MilestoneStageInfo(1, "Milestone 1: Kickoff & Requirements", "Initial client briefing, requirements gathering, and task distribution.", 25),
        MilestoneStageInfo(2, "Milestone 2: Core Build & Architecture", "Implementing core UI components, database schemas, and business logic.", 50),
        MilestoneStageInfo(3, "Milestone 3: Verification, Integrations & QA", "End-to-end testing, cloud database synchronization, and manager reviews.", 75),
        MilestoneStageInfo(4, "Milestone 4: Final Signoff & Client Delivery", "Complete deliverables verified, final timesheets locked, and client signoff.", 100)
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(palette.colors)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsScore,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Milestone Completion Overview",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = Color(0xFF0F172A)
                )

                Text(
                    text = "$percentage% Overall Progress • ${if (percentage >= 100) "All Milestones Achieved" else "Sprint Tracking On Schedule"}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.badgeText
                )

                Spacer(modifier = Modifier.height(14.dp))

                // CSS Gradient mini bar in dialog
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color(0xFFF1F5F9))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((percentage / 100f).coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(100.dp))
                            .background(Brush.horizontalGradient(palette.colors))
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Milestone Stages Breakdown List
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    stages.forEach { stage ->
                        val isAchieved = percentage >= stage.threshold
                        val isCurrent = !isAchieved && (percentage >= (stage.threshold - 25))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isAchieved) Color(0xFFECFDF5) else if (isCurrent) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (isAchieved) Color(0xFF86EFAC) else if (isCurrent) Color(0xFFBFDBFE) else Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (isAchieved) Color(0xFF10B981) else if (isCurrent) Color(0xFF2563EB) else Color(0xFF94A3B8)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isAchieved) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    } else {
                                        Text("${stage.stepNumber}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stage.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isAchieved) Color(0xFF065F46) else if (isCurrent) Color(0xFF1E3A8A) else Color(0xFF475569)
                                    )
                                    Text(
                                        text = stage.description,
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF64748B),
                                        lineHeight = 14.sp
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isAchieved) Color(0xFFD1FAE5) else Color(0xFFF1F5F9)
                                ) {
                                    Text(
                                        text = if (isAchieved) "DONE" else "${stage.threshold}%",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAchieved) Color(0xFF047857) else Color(0xFF64748B),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private data class MilestoneStageInfo(
    val stepNumber: Int,
    val title: String,
    val description: String,
    val threshold: Int
)
