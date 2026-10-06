package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.milo.AdminExecutiveBriefing
import com.example.milo.MiloAdminExecutiveEngine
import com.example.milo.MiloAdminExecutiveSheet
import com.example.ui.screens.MainViewModel
import com.example.ui.theme.*
import com.example.util.MiloHaptics
import kotlinx.coroutines.launch

/**
 * 🦁 Milo Admin Copilot Dashboard Card:
 * Executive AI companion on the Admin Dashboard that proactively analyzes
 * team workloads, attendance status, overdue client invoices, and bottleneck tasks,
 * providing 1-click executive automations to ease the Admin's daily operations.
 */
@Composable
fun MiloAdminCopilotWidget(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onNavigateToTasks: () -> Unit = {},
    onNavigateToAttendance: () -> Unit = {},
    onNavigateToLeads: () -> Unit = {},
    onNavigateToInvoices: () -> Unit = {},
    onNavigateToClientWishes: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val adminEngine = remember { MiloAdminExecutiveEngine(context) }
    var briefing by remember { mutableStateOf<AdminExecutiveBriefing?>(null) }
    var showExecutiveSheet by remember { mutableStateOf(false) }
    var isQuickExecuting by remember { mutableStateOf(false) }
    var quickToastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        briefing = adminEngine.generateExecutiveBriefing()
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("milo_admin_copilot_widget")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 🏷️ Top Header with Executive Mascot & Launch Button
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
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFF59E0B), Color(0xFFEA580C))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🦁", fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Milo Executive Copilot",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFEF3C7)
                            ) {
                                Text(
                                    text = "20X AI",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFD97706),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = briefing?.timeGreeting ?: "Autonomous Operational Intelligence",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Button(
                    onClick = {
                        MiloHaptics.performReactionTick(context, haptic)
                        showExecutiveSheet = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("open_milo_executive_sheet_button")
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copilot", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 📊 Live Executive Health Strip
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Attendance: ${briefing?.teamAttendance?.clockedInCount ?: 5}/${briefing?.teamAttendance?.totalEmployees ?: 6} In",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )
                    }

                    Text("•", color = Color(0xFFCBD5E1), fontSize = 12.sp)

                    Text(
                        text = "Tasks: ${briefing?.taskBottlenecks?.overdueHighPriorityCount ?: 0} Delayed",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if ((briefing?.taskBottlenecks?.overdueHighPriorityCount ?: 0) > 0) Color(0xFFEF4444) else Color(0xFF334155)
                    )

                    Text("•", color = Color(0xFFCBD5E1), fontSize = 12.sp)

                    Text(
                        text = "Deals: ${briefing?.leadPipeline?.hotLeadsCount ?: 3} Hot",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF7C3AED)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ⚡ Quick Autopilot Action Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    AutopilotChip(
                        icon = Icons.Default.Reorder,
                        label = "⚡ Auto-Rebalance",
                        onClick = {
                            scope.launch {
                                isQuickExecuting = true
                                MiloHaptics.performButtonClick(context, haptic)
                                val res = adminEngine.executeAutoRebalanceTasks()
                                briefing = adminEngine.generateExecutiveBriefing()
                                isQuickExecuting = false
                                quickToastMessage = res.details
                            }
                        }
                    )
                }

                item {
                    AutopilotChip(
                        icon = Icons.Default.NotificationsActive,
                        label = "📢 Nudge Late Staff",
                        onClick = {
                            scope.launch {
                                isQuickExecuting = true
                                MiloHaptics.performButtonClick(context, haptic)
                                val res = adminEngine.executeNudgeUncheckedEmployees()
                                briefing = adminEngine.generateExecutiveBriefing()
                                isQuickExecuting = false
                                quickToastMessage = res.details
                            }
                        }
                    )
                }

                item {
                    AutopilotChip(
                        icon = Icons.Default.ReceiptLong,
                        label = "💰 Overdue Invoices",
                        onClick = {
                            scope.launch {
                                isQuickExecuting = true
                                MiloHaptics.performButtonClick(context, haptic)
                                val res = adminEngine.executeChaseOverdueInvoices()
                                briefing = adminEngine.generateExecutiveBriefing()
                                isQuickExecuting = false
                                quickToastMessage = res.details
                            }
                        }
                    )
                }

                item {
                    AutopilotChip(
                        icon = Icons.Default.Campaign,
                        label = "🚀 Team Broadcast",
                        onClick = {
                            showExecutiveSheet = true
                        }
                    )
                }
            }
        }
    }

    // Quick Toast Alert Dialog
    quickToastMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { quickToastMessage = null },
            icon = {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(32.dp))
            },
            title = { Text("Milo Autopilot Executed", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = { Text(msg, fontSize = 12.5.sp, color = Color(0xFF475569)) },
            confirmButton = {
                Button(
                    onClick = { quickToastMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Done", color = Color.White)
                }
            }
        )
    }

    // Full Executive Sheet
    if (showExecutiveSheet) {
        MiloAdminExecutiveSheet(
            viewModel = viewModel,
            onDismiss = { showExecutiveSheet = false },
            onNavigateToTasks = {
                showExecutiveSheet = false
                onNavigateToTasks()
            },
            onNavigateToAttendance = {
                showExecutiveSheet = false
                onNavigateToAttendance()
            },
            onNavigateToLeads = {
                showExecutiveSheet = false
                onNavigateToLeads()
            },
            onNavigateToInvoices = {
                showExecutiveSheet = false
                onNavigateToInvoices()
            },
            onNavigateToClientWishes = {
                showExecutiveSheet = false
                onNavigateToClientWishes()
            }
        )
    }
}

@Composable
private fun AutopilotChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFEFF6FF),
        border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
        }
    }
}
