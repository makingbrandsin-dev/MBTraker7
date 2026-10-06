package com.example.ui.components

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.screens.DailyActivityCategory
import com.example.ui.screens.DailyActivityItem
import com.example.ui.screens.DailyActivityItemType
import com.example.ui.screens.DailyActivitySummaryState
import com.example.ui.screens.MainViewModel
import com.example.ui.theme.BrandBlue
import com.example.util.MiloHaptics
import java.util.Locale

/**
 * 📊 Daily Activity Summary Widget:
 * Aggregates attendance records, completed tasks, and CRM interactions
 * from the current day into a responsive, scrollable operations card.
 */
@Composable
fun DailyActivitySummaryWidget(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onNavigateToAttendance: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToCRM: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val summaryState by viewModel.dailyActivitySummaryState.collectAsState()

    var selectedFilterCategory by remember { mutableStateOf<DailyActivityCategory?>(null) }
    var selectedDetailItem by remember { mutableStateOf<DailyActivityItem?>(null) }
    var isExpanded by remember { mutableStateOf(true) }

    val displayedItems: List<DailyActivityItem> = remember(summaryState, selectedFilterCategory) {
        when (selectedFilterCategory) {
            DailyActivityCategory.ATTENDANCE -> summaryState.attendanceItems
            DailyActivityCategory.TASKS -> summaryState.completedTaskItems
            DailyActivityCategory.CRM -> summaryState.crmItems
            DailyActivityCategory.ALL, null -> summaryState.allItems
        }
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_activity_summary_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 🏷️ 1. Header Section: Title, Date Badge & Expand Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Summarize,
                            contentDescription = "Daily Activity Summary",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Daily Activity Summary",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFEDE9FE)
                            ) {
                                Text(
                                    text = "TODAY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF6D28D9),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = summaryState.dateFormatted,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        MiloHaptics.performButtonClick(context, haptic)
                        isExpanded = !isExpanded
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle Summary Expand",
                        tint = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 📊 2. High-Level Aggregated KPI Stat Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Attendance KPI
                SummaryKpiCard(
                    modifier = Modifier.weight(1f),
                    title = "Attendance",
                    value = "${summaryState.employeesPresentCount} Present",
                    subtitle = String.format(Locale.US, "%.1fh logged", summaryState.totalHoursWorked),
                    icon = Icons.Default.Badge,
                    containerColor = Color(0xFFECFDF5),
                    accentColor = Color(0xFF059669),
                    onClick = {
                        MiloHaptics.performButtonClick(context, haptic)
                        selectedFilterCategory = if (selectedFilterCategory == DailyActivityCategory.ATTENDANCE) null else DailyActivityCategory.ATTENDANCE
                    }
                )

                // Tasks KPI
                SummaryKpiCard(
                    modifier = Modifier.weight(1f),
                    title = "Tasks Done",
                    value = "${summaryState.completedTasksCount} Done",
                    subtitle = "100% Verified",
                    icon = Icons.Default.CheckCircleOutline,
                    containerColor = Color(0xFFEFF6FF),
                    accentColor = Color(0xFF2563EB),
                    onClick = {
                        MiloHaptics.performButtonClick(context, haptic)
                        selectedFilterCategory = if (selectedFilterCategory == DailyActivityCategory.TASKS) null else DailyActivityCategory.TASKS
                    }
                )

                // CRM KPI
                SummaryKpiCard(
                    modifier = Modifier.weight(1f),
                    title = "CRM Pipeline",
                    value = "${summaryState.crmInteractionsCount} Events",
                    subtitle = "${summaryState.callsLoggedCount} Calls",
                    icon = Icons.Default.ConnectWithoutContact,
                    containerColor = Color(0xFFFAF5FF),
                    accentColor = Color(0xFF7C3AED),
                    onClick = {
                        MiloHaptics.performButtonClick(context, haptic)
                        selectedFilterCategory = if (selectedFilterCategory == DailyActivityCategory.CRM) null else DailyActivityCategory.CRM
                    }
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))

                    // 🏷️ 3. Category Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CategoryFilterChip(
                            label = "All (${summaryState.allItems.size})",
                            isSelected = selectedFilterCategory == null || selectedFilterCategory == DailyActivityCategory.ALL,
                            onClick = {
                                MiloHaptics.performButtonClick(context, haptic)
                                selectedFilterCategory = null
                            },
                            modifier = Modifier.weight(1f)
                        )

                        CategoryFilterChip(
                            label = "Attendance (${summaryState.attendanceItems.size})",
                            isSelected = selectedFilterCategory == DailyActivityCategory.ATTENDANCE,
                            onClick = {
                                MiloHaptics.performButtonClick(context, haptic)
                                selectedFilterCategory = DailyActivityCategory.ATTENDANCE
                            },
                            modifier = Modifier.weight(1f)
                        )

                        CategoryFilterChip(
                            label = "Tasks (${summaryState.completedTaskItems.size})",
                            isSelected = selectedFilterCategory == DailyActivityCategory.TASKS,
                            onClick = {
                                MiloHaptics.performButtonClick(context, haptic)
                                selectedFilterCategory = DailyActivityCategory.TASKS
                            },
                            modifier = Modifier.weight(1f)
                        )

                        CategoryFilterChip(
                            label = "CRM (${summaryState.crmItems.size})",
                            isSelected = selectedFilterCategory == DailyActivityCategory.CRM,
                            onClick = {
                                MiloHaptics.performButtonClick(context, haptic)
                                selectedFilterCategory = DailyActivityCategory.CRM
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 📜 4. Scrollable Activity Stream Container (Nested Scrollable Box)
                    if (displayedItems.isEmpty()) {
                        EmptyDailyActivityState(category = selectedFilterCategory)
                    } else {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    items = displayedItems,
                                    key = { it.id }
                                ) { item: DailyActivityItem ->
                                    DailyActivityRowItem(
                                        item = item,
                                        onClick = {
                                            MiloHaptics.performButtonClick(context, haptic)
                                            selectedDetailItem = item
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 📊 CSV / PDF Offline Log Exporters Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val empName = viewModel.currentEmployeeName.value.ifBlank { "Employee" }
                        val dateLabel = summaryState.dateFormatted

                        OutlinedButton(
                            onClick = {
                                MiloHaptics.performButtonClick(context, haptic)
                                com.example.util.ActivityExportHelper.exportToCsv(context, empName, displayedItems)
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF475569)),
                            modifier = Modifier.weight(1f).height(34.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                MiloHaptics.performButtonClick(context, haptic)
                                com.example.util.ActivityExportHelper.exportToPdf(context, empName, dateLabel, displayedItems)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            modifier = Modifier.weight(1f).height(34.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export PDF Report", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 🔗 5. Hub Navigation Shortcuts Footer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Aggregated across Room DB & Firestore",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = onNavigateToAttendance,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Attendance", fontSize = 11.sp, color = BrandBlue, fontWeight = FontWeight.Bold)
                            }
                            TextButton(
                                onClick = onNavigateToTasks,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Tasks", fontSize = 11.sp, color = BrandBlue, fontWeight = FontWeight.Bold)
                            }
                            TextButton(
                                onClick = onNavigateToCRM,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("CRM", fontSize = 11.sp, color = BrandBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // 📋 Activity Inspection Modal Dialog
    selectedDetailItem?.let { item ->
        DailyActivityDetailDialog(
            item = item,
            onDismiss = { selectedDetailItem = null },
            onNavigateToAttendance = {
                selectedDetailItem = null
                onNavigateToAttendance()
            },
            onNavigateToTasks = {
                selectedDetailItem = null
                onNavigateToTasks()
            },
            onNavigateToCRM = {
                selectedDetailItem = null
                onNavigateToCRM()
            }
        )
    }
}

/**
 * High Level Metric KPI Tile
 */
@Composable
private fun SummaryKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Filter Chip Button
 */
@Composable
private fun CategoryFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) BrandBlue else Color.White,
        border = BorderStroke(1.dp, if (isSelected) BrandBlue else Color(0xFFE2E8F0)),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFF475569),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Activity Item Row with User Avatar, Timestamp & Badges
 */
@Composable
private fun DailyActivityRowItem(
    item: DailyActivityItem,
    onClick: () -> Unit
) {
    val (icon, iconBgColor, iconTint) = when (item.type) {
        DailyActivityItemType.ATTENDANCE_PUNCH_IN -> Triple(Icons.Default.Login, Color(0xFFDCFCE7), Color(0xFF16A34A))
        DailyActivityItemType.ATTENDANCE_PUNCH_OUT -> Triple(Icons.Default.Logout, Color(0xFFEDE9FE), Color(0xFF6D28D9))
        DailyActivityItemType.TASK_COMPLETED -> Triple(Icons.Default.TaskAlt, Color(0xFFDBEAFE), Color(0xFF2563EB))
        DailyActivityItemType.CRM_CALL_LOGGED -> Triple(Icons.Default.PhoneCallback, Color(0xFFF3E8FF), Color(0xFF9333EA))
        DailyActivityItemType.CRM_LEAD_PROGRESS -> Triple(Icons.Default.LocalFireDepartment, Color(0xFFFEF3C7), Color(0xFFD97706))
        DailyActivityItemType.CRM_MEETING_LOGGED -> Triple(Icons.Default.Groups, Color(0xFFCCFBF1), Color(0xFF0D9488))
        DailyActivityItemType.CRM_FOLLOWUP_DONE -> Triple(Icons.Default.EventAvailable, Color(0xFFFCE7F3), Color(0xFFDB2777))
    }

    val avatarGradient = remember(item.participantName) {
        val hash = item.participantName.hashCode()
        when (kotlin.math.abs(hash) % 5) {
            0 -> listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
            1 -> listOf(Color(0xFF10B981), Color(0xFF047857))
            2 -> listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))
            3 -> listOf(Color(0xFFF59E0B), Color(0xFFB45309))
            else -> listOf(Color(0xFFEC4899), Color(0xFFBE185D))
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User / Participant Avatar with gradient
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(avatarGradient)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.participantName.take(2).uppercase(Locale.ROOT).ifBlank { "MB" },
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.5.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Body text
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = item.timeFormatted,
                        fontSize = 10.5.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.description,
                    fontSize = 11.5.sp,
                    color = Color(0xFF475569),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                item.metadataTag?.let { tag ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = iconBgColor
                    ) {
                        Text(
                            text = tag,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = iconTint,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Event icon pill
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Empty State for Activity Categories
 */
@Composable
private fun EmptyDailyActivityState(category: DailyActivityCategory?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.EventNote,
                contentDescription = null,
                tint = Color(0xFFCBD5E1),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "No ${category?.name?.lowercase(Locale.ROOT) ?: "activity"} records for today yet",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Detailed Activity Inspection Dialog
 */
@Composable
private fun DailyActivityDetailDialog(
    item: DailyActivityItem,
    onDismiss: () -> Unit,
    onNavigateToAttendance: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToCRM: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEFF6FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (item.category) {
                            DailyActivityCategory.ATTENDANCE -> Icons.Default.PunchClock
                            DailyActivityCategory.TASKS -> Icons.Default.AssignmentTurnedIn
                            DailyActivityCategory.CRM, DailyActivityCategory.ALL -> Icons.Default.Leaderboard
                        },
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = item.title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = Color(0xFF0F172A)
                )

                Text(
                    text = "Logged today at ${item.timeFormatted}",
                    fontSize = 11.5.sp,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Action Summary:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.description,
                            fontSize = 12.5.sp,
                            color = Color(0xFF1E293B),
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Participant: ${item.participantName}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )

                            item.metadataTag?.let { tag ->
                                Text(
                                    text = tag,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close", color = Color(0xFF64748B))
                    }

                    Button(
                        onClick = {
                            when (item.category) {
                                DailyActivityCategory.ATTENDANCE -> onNavigateToAttendance()
                                DailyActivityCategory.TASKS -> onNavigateToTasks()
                                DailyActivityCategory.CRM, DailyActivityCategory.ALL -> onNavigateToCRM()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = when (item.category) {
                                DailyActivityCategory.ATTENDANCE -> "Attendance Hub"
                                DailyActivityCategory.TASKS -> "Task Hub"
                                DailyActivityCategory.CRM, DailyActivityCategory.ALL -> "CRM Pipeline"
                            },
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
