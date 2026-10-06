package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.DailyTaskCompletionTrendPoint
import com.example.ui.screens.EmployeeCompletionMetric
import com.example.ui.screens.MainViewModel
import com.example.ui.screens.PerformanceTrendsState
import com.example.ui.theme.*
import com.example.util.MiloHaptics
import kotlin.math.roundToInt

enum class PerformanceTimeframe(val days: Int, val label: String) {
    LAST_7_DAYS(7, "7 Days"),
    LAST_14_DAYS(14, "14 Days"),
    LAST_30_DAYS(30, "30 Days")
}

/**
 * Modern Recharts / D3-inspired Interactive Dashboard Card:
 * Visualizes the Average Task Completion Time for all employees over the last 30 days
 * with smooth Bezier curves, CartesianGrid, interactive hover cursor & tooltip,
 * and individual employee turnaround speed rankings.
 */
@Composable
fun PerformanceTrendsDashboardWidget(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onNavigateToTasks: () -> Unit = {}
) {
    val performanceState by viewModel.performanceTrendsState.collectAsState()
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    var selectedTimeframe by remember { mutableStateOf(PerformanceTimeframe.LAST_30_DAYS) }
    var selectedDepartment by remember { mutableStateOf("ALL") }
    var selectedPointIndex by remember { mutableIntStateOf(-1) }
    var showEmployeeLeaderboard by remember { mutableStateOf(true) }

    // Filter daily points based on timeframe
    val filteredPoints = remember(performanceState.dailyPoints, selectedTimeframe) {
        if (performanceState.dailyPoints.isEmpty()) emptyList()
        else performanceState.dailyPoints.takeLast(selectedTimeframe.days)
    }

    // Active hovered / scrubbed data point
    val activePoint = remember(filteredPoints, selectedPointIndex) {
        if (filteredPoints.isEmpty()) null
        else if (selectedPointIndex in filteredPoints.indices) filteredPoints[selectedPointIndex]
        else filteredPoints.lastOrNull()
    }

    // Filter employee breakdown by department
    val filteredEmployees = remember(performanceState.employeeMetrics, selectedDepartment) {
        if (selectedDepartment == "ALL") performanceState.employeeMetrics
        else performanceState.employeeMetrics.filter { it.department.equals(selectedDepartment, ignoreCase = true) }
    }

    // Smooth animation on metric & timeframe changes
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(selectedTimeframe) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("performance_trends_dashboard_widget")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 🏷️ Top Header with Recharts / D3 Icon & Timeframe Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF0284C7), Color(0xFF10B981))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Performance Trends",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Performance Trends",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFEFF6FF),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                            ) {
                                Text(
                                    text = "RECHARTS",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BrandBlue,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Avg Task Completion Time (Last 30 Days)",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Timeframe Selector Chips
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        PerformanceTimeframe.values().forEach { tf ->
                            val isSelected = selectedTimeframe == tf
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) BrandBlue else Color.Transparent)
                                    .clickable {
                                        if (selectedTimeframe != tf) {
                                            selectedTimeframe = tf
                                            MiloHaptics.performButtonClick(context, hapticFeedback)
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tf.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ⚡ 4 KPI Highlights Summary Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Avg Turnaround Time
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF0FDF4),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("30D Avg Time", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                            Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${performanceState.summary.overallAverageCompletionHours} hrs",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF14532D)
                        )
                        Text(
                            text = "↑ ${performanceState.summary.overallVelocityImprovementPct}% Faster",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A)
                        )
                    }
                }

                // 2. Fastest Task Time
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF6FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Fastest Task", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = performanceState.summary.fastestTaskCompletionTimeFormatted,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E3A8A)
                        )
                        Text(
                            text = "Lightning speed",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandBlue
                        )
                    }
                }

                // 3. Total Tasks Completed
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFAF5FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9D5FF)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Completed", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B21A8))
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF9333EA), modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${performanceState.summary.totalTasksCompleted30Days}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF581C87)
                        )
                        Text(
                            text = "Across all staff",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF9333EA)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 📊 Interactive Recharts Canvas Chart Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                    .pointerInput(filteredPoints) {
                        detectTapGestures { offset ->
                            val width = size.width
                            val step = width / (filteredPoints.size.coerceAtLeast(1))
                            val idx = (offset.x / step).toInt().coerceIn(0, filteredPoints.size - 1)
                            selectedPointIndex = idx
                            MiloHaptics.performReactionTick(context, hapticFeedback)
                        }
                    }
                    .pointerInput(filteredPoints) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val width = size.width
                            val step = width / (filteredPoints.size.coerceAtLeast(1))
                            val idx = (change.position.x / step).toInt().coerceIn(0, filteredPoints.size - 1)
                            if (idx != selectedPointIndex) {
                                selectedPointIndex = idx
                                MiloHaptics.performReactionTick(context, hapticFeedback)
                            }
                        }
                    }
            ) {
                RechartsCompletionTimeCanvas(
                    points = filteredPoints,
                    animProgress = animProgress.value,
                    selectedIndex = selectedPointIndex,
                    maxHours = 6.0f
                )

                // Floating Recharts Tooltip Card for Active Hover Point
                activePoint?.let { pt ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.94f),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${pt.dayOfWeek}, ${pt.displayDay}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${pt.tasksCompleted} tasks)",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Avg Time: ${pt.averageCompletionHours} hrs/task",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    color = Color(0xFF34D399)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF38BDF8))
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Top: ${pt.topPerformerName}",
                                    fontSize = 10.sp,
                                    color = Color(0xFFBAE6FD),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 🏆 Per-Employee Completion Time Leaderboard Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Leaderboard,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Employee Turnaround Rankings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF0F172A)
                    )
                }

                TextButton(
                    onClick = { showEmployeeLeaderboard = !showEmployeeLeaderboard },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        if (showEmployeeLeaderboard) "Hide" else "Show All (${filteredEmployees.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue
                    )
                }
            }

            if (showEmployeeLeaderboard) {
                Spacer(modifier = Modifier.height(6.dp))

                // Department Filter Pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(listOf("ALL", "ENGINEERING", "DESIGN", "MARKETING", "SALES")) { dept ->
                        val isSelected = selectedDepartment == dept
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) BrandBlue else Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) BrandBlue else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.clickable {
                                selectedDepartment = dept
                                MiloHaptics.performButtonClick(context, hapticFeedback)
                            }
                        ) {
                            Text(
                                text = if (dept == "ALL") "All Teams" else dept.take(4).lowercase().replaceFirstChar { it.uppercase() },
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    filteredEmployees.forEach { emp ->
                        EmployeeCompletionRowItem(emp = emp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer Navigation Action
            FilledTonalButton(
                onClick = onNavigateToTasks,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFFEFF6FF),
                    contentColor = BrandBlue
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(40.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("View Full Tasks & Velocity Analytics", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Recharts & D3 Styled Canvas Chart with CartesianGrid, Spline Curve, Area Gradient & Scrubber
 */
@Composable
private fun RechartsCompletionTimeCanvas(
    points: List<DailyTaskCompletionTrendPoint>,
    animProgress: Float,
    selectedIndex: Int,
    maxHours: Float = 6.0f
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        if (points.isEmpty()) return@Canvas

        val paddingLeft = 36f
        val paddingRight = 16f
        val paddingTop = 24f
        val paddingBottom = 32f

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        // 1. Draw D3 / Recharts CartesianGrid (Dotted horizontal grid lines)
        val gridLineCount = 4
        for (i in 0..gridLineCount) {
            val y = paddingTop + (chartHeight / gridLineCount) * i
            val hourLabel = maxHours - (maxHours / gridLineCount) * i

            drawLine(
                color = Color(0xFFE2E8F0),
                start = Offset(paddingLeft, y),
                end = Offset(width - paddingRight, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )
        }

        // 2. Build Cubic Bezier Spline Path for Average Completion Time
        val path = Path()
        val areaPath = Path()

        val stepX = if (points.size > 1) chartWidth / (points.size - 1) else chartWidth
        val coordinates = points.mapIndexed { index, pt ->
            val x = paddingLeft + (index * stepX)
            val normalizedY = (pt.averageCompletionHours.coerceIn(0f, maxHours) / maxHours)
            val y = (paddingTop + chartHeight * (1f - (normalizedY * animProgress))).coerceIn(paddingTop, paddingTop + chartHeight)
            Offset(x, y)
        }

        if (coordinates.isNotEmpty()) {
            path.moveTo(coordinates.first().x, coordinates.first().y)
            areaPath.moveTo(coordinates.first().x, paddingTop + chartHeight)
            areaPath.lineTo(coordinates.first().x, coordinates.first().y)

            for (i in 0 until coordinates.size - 1) {
                val current = coordinates[i]
                val next = coordinates[i + 1]
                val controlX1 = current.x + (next.x - current.x) / 2f
                val controlY1 = current.y
                val controlX2 = current.x + (next.x - current.x) / 2f
                val controlY2 = next.y

                path.cubicTo(controlX1, controlY1, controlX2, controlY2, next.x, next.y)
                areaPath.cubicTo(controlX1, controlY1, controlX2, controlY2, next.x, next.y)
            }

            areaPath.lineTo(coordinates.last().x, paddingTop + chartHeight)
            areaPath.close()

            // Draw Area Gradient Fill
            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF10B981).copy(alpha = 0.35f * animProgress),
                        Color(0xFF0284C7).copy(alpha = 0.12f * animProgress),
                        Color(0xFF0284C7).copy(alpha = 0.00f)
                    ),
                    startY = paddingTop,
                    endY = paddingTop + chartHeight
                )
            )

            // Draw Smooth Line Stroke
            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF0284C7), Color(0xFF10B981))
                ),
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // 3. Draw Active Hover Scrubber / Cursor
        if (selectedIndex in coordinates.indices) {
            val selectedOffset = coordinates[selectedIndex]

            // Vertical Cursor Line
            drawLine(
                color = Color(0xFF0284C7).copy(alpha = 0.8f),
                start = Offset(selectedOffset.x, paddingTop),
                end = Offset(selectedOffset.x, paddingTop + chartHeight),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
            )

            // Outer Pulsing Halo
            drawCircle(
                color = Color(0xFF10B981).copy(alpha = 0.25f),
                radius = 9.dp.toPx(),
                center = selectedOffset
            )

            // Inner Active Dot
            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx(),
                center = selectedOffset
            )
            drawCircle(
                color = Color(0xFF10B981),
                radius = 3.5.dp.toPx(),
                center = selectedOffset
            )
        }
    }
}

/**
 * Individual Employee Turnaround Speed Row with Bar Chart Progress
 */
@Composable
private fun EmployeeCompletionRowItem(emp: EmployeeCompletionMetric) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Badge
            Surface(
                shape = CircleShape,
                color = when (emp.efficiencyRank) {
                    1 -> Color(0xFFFEF3C7)
                    2 -> Color(0xFFF1F5F9)
                    3 -> Color(0xFFFFEDD5)
                    else -> Color(0xFFF1F5F9)
                },
                modifier = Modifier.size(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = when (emp.efficiencyRank) {
                            1 -> "🥇"
                            2 -> "🥈"
                            3 -> "🥉"
                            else -> "#${emp.efficiencyRank}"
                        },
                        fontSize = if (emp.efficiencyRank <= 3) 12.sp else 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = emp.employeeName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "${emp.averageCompletionHours} hrs/task",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = if (emp.averageCompletionHours <= 2.5f) Color(0xFF16A34A) else BrandBlue
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Progress Bar (Inverse scaling: faster = fuller green bar)
                val speedProgress = (1.0f - (emp.averageCompletionHours - 1.0f) / 4.0f).coerceIn(0.15f, 1.0f)
                LinearProgressIndicator(
                    progress = { speedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (emp.averageCompletionHours <= 2.5f) Color(0xFF10B981) else BrandBlue,
                    trackColor = Color(0xFFE2E8F0)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${emp.role} • ${emp.tasksCompletedCount} tasks",
                        fontSize = 9.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "${emp.onTimeCompletionRate.toInt()}% on-time",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                }
            }
        }
    }
}
