package com.example.milo

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.milo.MiloEvent
import com.example.domain.milo.MiloState
import com.example.ui.screens.MainViewModel
import com.example.ui.theme.*
import com.example.util.MiloHaptics
import kotlinx.coroutines.launch

enum class AdminMiloTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    BRIEFING("Executive Briefing", Icons.Default.Dashboard),
    AUTOPILOT("Autopilot Actions", Icons.Default.Bolt),
    TERMINAL("Executive AI Chat", Icons.Default.SmartToy),
    WORKLOAD("Workload Radar", Icons.Default.Speed)
}

/**
 * 🦁 Milo Admin Executive AI Assistant Sheet:
 * Advanced full-featured executive assistant tailored specifically to easing the Admin's daily operations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiloAdminExecutiveSheet(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onNavigateToTasks: () -> Unit = {},
    onNavigateToAttendance: () -> Unit = {},
    onNavigateToLeads: () -> Unit = {},
    onNavigateToInvoices: () -> Unit = {},
    onNavigateToClientWishes: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val adminEngine = remember { MiloAdminExecutiveEngine(context) }
    var briefing by remember { mutableStateOf<AdminExecutiveBriefing?>(null) }
    var isLoadingBriefing by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(AdminMiloTab.BRIEFING) }
    var isExecutingAction by remember { mutableStateOf(false) }
    var actionExecutionResult by remember { mutableStateOf<AdminActionResult?>(null) }

    // Chat state for AI Terminal
    var commandInput by remember { mutableStateOf("") }
    val chatMessages = remember {
        mutableStateListOf(
            MiloAssistantMessage(
                id = "admin_welcome",
                sender = "MILO EXECUTIVE",
                text = "🦁 **Milo Executive AI Co-Pilot Ready**\n\nI am configured for your administrative priorities: Attendance enforcement, Task bottleneck resolution, Overdue invoice recovery, and Lead acceleration.\n\nTap any executive action or ask me a direct question!",
                isUser = false
            )
        )
    }
    val chatListState = rememberLazyListState()

    // Load initial briefing
    LaunchedEffect(Unit) {
        isLoadingBriefing = true
        try {
            briefing = adminEngine.generateExecutiveBriefing()
        } finally {
            isLoadingBriefing = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceBg,
        dragHandle = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFCBD5E1),
                    modifier = Modifier.size(width = 40.dp, height = 4.dp)
                ) {}
            }
        },
        modifier = Modifier.testTag("milo_admin_executive_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 16.dp)
        ) {
            // 🏷️ Top Header with Executive Mascot & Dismiss Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFF59E0B), Color(0xFFEA580C))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🦁",
                            fontSize = 24.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Milo Executive Copilot",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFEF3C7)
                            ) {
                                Text(
                                    text = "ADMIN ONLY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFD97706),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = briefing?.timeGreeting ?: "Autonomous Operational Intelligence",
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("milo_admin_sheet_close")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 🧭 Horizontal Tab Bar
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(AdminMiloTab.values()) { tab ->
                    val isSelected = selectedTab == tab
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) BrandBlue else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) BrandBlue else Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .clickable {
                                MiloHaptics.performReactionTick(context, haptic)
                                selectedTab = tab
                            }
                            .testTag("admin_milo_tab_${tab.name.lowercase()}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else Color(0xFF64748B),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF334155)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ⚡ Main Tab Content
            when (selectedTab) {
                AdminMiloTab.BRIEFING -> {
                    BriefingTabContent(
                        briefing = briefing,
                        isLoading = isLoadingBriefing,
                        onExecuteRec = { rec ->
                            scope.launch {
                                isExecutingAction = true
                                MiloHaptics.performButtonClick(context, haptic)
                                actionExecutionResult = when (rec.actionType) {
                                    "REBALANCE_TASKS" -> adminEngine.executeAutoRebalanceTasks()
                                    "NUDGE_ATTENDANCE" -> adminEngine.executeNudgeUncheckedEmployees()
                                    "CHASE_INVOICE" -> adminEngine.executeChaseOverdueInvoices()
                                    "APPROVE_LEAVES" -> adminEngine.executeAutoApprovePendingLeaves()
                                    else -> adminEngine.executeAutoRebalanceTasks()
                                }
                                briefing = adminEngine.generateExecutiveBriefing()
                                isExecutingAction = false
                            }
                        },
                        onNavigateToAttendance = onNavigateToAttendance,
                        onNavigateToTasks = onNavigateToTasks,
                        onNavigateToLeads = onNavigateToLeads,
                        onNavigateToInvoices = onNavigateToInvoices
                    )
                }

                AdminMiloTab.AUTOPILOT -> {
                    AutopilotTabContent(
                        isExecuting = isExecutingAction,
                        onExecute = { actionName ->
                            if (actionName == "POSTER_WISH") {
                                onDismiss()
                                onNavigateToClientWishes()
                                return@AutopilotTabContent
                            }
                            scope.launch {
                                isExecutingAction = true
                                MiloHaptics.performButtonClick(context, haptic)
                                actionExecutionResult = when (actionName) {
                                    "REBALANCE" -> adminEngine.executeAutoRebalanceTasks()
                                    "NUDGE" -> adminEngine.executeNudgeUncheckedEmployees()
                                    "INVOICE" -> adminEngine.executeChaseOverdueInvoices()
                                    "LEAVES" -> adminEngine.executeAutoApprovePendingLeaves()
                                    "BROADCAST" -> adminEngine.executeBroadcastMilestone("Executive Notice", "Sprint goals achieved! Great work team.")
                                    else -> adminEngine.executeAutoRebalanceTasks()
                                }
                                briefing = adminEngine.generateExecutiveBriefing()
                                isExecutingAction = false
                            }
                        }
                    )
                }

                AdminMiloTab.TERMINAL -> {
                    TerminalTabContent(
                        chatMessages = chatMessages,
                        chatListState = chatListState,
                        commandInput = commandInput,
                        onInputChange = { commandInput = it },
                        onSubmit = { text ->
                            if (text.isNotBlank()) {
                                MiloHaptics.performMessageSent(context, haptic)
                                chatMessages.add(
                                    MiloAssistantMessage(
                                        id = System.currentTimeMillis().toString(),
                                        sender = "ADMIN",
                                        text = text,
                                        isUser = true
                                    )
                                )
                                val query = text
                                commandInput = ""
                                keyboardController?.hide()

                                scope.launch {
                                    chatListState.animateScrollToItem(chatMessages.size - 1)
                                    val (response, state) = adminEngine.processExecutiveCommand(query)
                                    viewModel.miloViewModel.handleEvent(MiloEvent.Thinking("executive analysis"))
                                    chatMessages.add(
                                        MiloAssistantMessage(
                                            id = (System.currentTimeMillis() + 1).toString(),
                                            sender = "MILO EXECUTIVE",
                                            text = response,
                                            isUser = false
                                        )
                                    )
                                    chatListState.animateScrollToItem(chatMessages.size - 1)
                                }
                            }
                        }
                    )
                }

                AdminMiloTab.WORKLOAD -> {
                    WorkloadTabContent(
                        viewModel = viewModel,
                        onNavigateToTasks = onNavigateToTasks
                    )
                }
            }
        }
    }

    // Result Snackbar / Dialog
    actionExecutionResult?.let { res ->
        AlertDialog(
            onDismissRequest = { actionExecutionResult = null },
            icon = {
                Icon(
                    imageVector = if (res.success) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (res.success) Color(0xFF10B981) else Color(0xFFEF4444),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(res.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Text(res.details, fontSize = 13.sp, color = Color(0xFF475569))
            },
            confirmButton = {
                Button(
                    onClick = { actionExecutionResult = null },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("OK", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun BriefingTabContent(
    briefing: AdminExecutiveBriefing?,
    isLoading: Boolean,
    onExecuteRec: (ExecutiveRecommendation) -> Unit,
    onNavigateToAttendance: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToLeads: () -> Unit,
    onNavigateToInvoices: () -> Unit
) {
    if (isLoading || briefing == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = BrandBlue)
        }
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 🦁 Milo Executive Health Score & Advice
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    if (briefing.executiveScore >= 80) listOf(Color(0xFF10B981), Color(0xFF059669))
                                    else listOf(Color(0xFFF59E0B), Color(0xFFEA580C))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${briefing.executiveScore}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = "HEALTH",
                                fontWeight = FontWeight.Bold,
                                fontSize = 7.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Executive Pulse • ${briefing.dateString}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = briefing.miloAdvice,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1E293B),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // 4-Card Executive KPI Matrix
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Attendance Card
                ExecutiveKpiCard(
                    title = "Attendance",
                    value = "${briefing.teamAttendance.clockedInCount}/${briefing.teamAttendance.totalEmployees}",
                    subtitle = "${briefing.teamAttendance.lateCount} late",
                    badgeColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToAttendance
                )

                // Tasks Card
                ExecutiveKpiCard(
                    title = "Critical Tasks",
                    value = "${briefing.taskBottlenecks.overdueHighPriorityCount}",
                    subtitle = "${briefing.taskBottlenecks.unassignedCount} unassigned",
                    badgeColor = if (briefing.taskBottlenecks.overdueHighPriorityCount > 0) Color(0xFFEF4444) else Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToTasks
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Hot Leads Card
                ExecutiveKpiCard(
                    title = "Hot Deals",
                    value = "${briefing.leadPipeline.hotLeadsCount}",
                    subtitle = "${briefing.leadPipeline.totalActiveLeads} active",
                    badgeColor = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToLeads
                )

                // Overdue Receivables Card
                ExecutiveKpiCard(
                    title = "Overdue Invoices",
                    value = "₹${String.format("%,.0f", briefing.invoiceReceivables.overdueTotalAmount / 1000)}k",
                    subtitle = "${briefing.invoiceReceivables.overdueCount} pending",
                    badgeColor = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToInvoices
                )
            }
        }

        // ⚡ Proactive Recommendations
        item {
            Text(
                text = "⚡ Proactive Admin Automations",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF0F172A),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(briefing.proactiveRecommendations, key = { it.id }) { rec ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = rec.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (rec.impactLevel == "CRITICAL") Color(0xFFFEF2F2) else Color(0xFFEFF6FF)
                        ) {
                            Text(
                                text = rec.impactLevel,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (rec.impactLevel == "CRITICAL") Color(0xFFDC2626) else BrandBlue,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = rec.description,
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { onExecuteRec(rec) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(rec.actionButtonText, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ExecutiveKpiCard(
    title: String,
    value: String,
    subtitle: String,
    badgeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 10.sp, color = Color(0xFF94A3B8))
        }
    }
}

@Composable
private fun AutopilotTabContent(
    isExecuting: Boolean,
    onExecute: (String) -> Unit
) {
    val autopilotList = listOf(
        Triple("POSTER_WISH", "🎉 Broadcast Festival Posters & Wishes", "Prepares personalized WhatsApp festival posters and holiday greetings for all clients in your database."),
        Triple("REBALANCE", "⚡ Auto-Rebalance Stalled Tasks", "Scans overdue/unassigned tasks and redistributes them evenly across available staff."),
        Triple("NUDGE", "📢 Nudge Late & Absent Staff", "Dispatches polite check-in reminders to employees who haven't punched in today."),
        Triple("INVOICE", "💰 Chase Overdue Client Receivables", "Drafts WhatsApp and email reminder messages for pending invoice accounts."),
        Triple("LEAVES", "✈️ Fast-Track Pending Normal Leaves", "Reviews and approves standard employee leave requests with shift coverage check."),
        Triple("BROADCAST", "🚀 Broadcast Company Sprint Milestone", "Publishes an executive celebration post to both Admin and MB EM employee app.")
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "🦁 Milo 1-Tap Executive Autopilots",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "Perform hours of administrative coordination in seconds.",
                fontSize = 11.5.sp,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        items(autopilotList) { (key, title, desc) ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(desc, fontSize = 11.5.sp, color = Color(0xFF64748B), lineHeight = 15.sp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = { onExecute(key) },
                        enabled = !isExecuting,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Run", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun TerminalTabContent(
    chatMessages: List<MiloAssistantMessage>,
    chatListState: androidx.compose.foundation.lazy.LazyListState,
    commandInput: String,
    onInputChange: (String) -> Unit,
    onSubmit: (String) -> Unit
) {
    val quickAdminPrompts = listOf(
        "Give me today's standup summary",
        "Rebalance high priority tasks",
        "Who is free to take new tasks?",
        "Send reminder for overdue invoices",
        "Check attendance status",
        "Broadcast milestone to team"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Quick Prompts Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(quickAdminPrompts) { prompt ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.clickable { onSubmit(prompt) }
                ) {
                    Text(
                        text = prompt,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Message Stream
        LazyColumn(
            state = chatListState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(chatMessages, key = { it.id }) { msg ->
                if (msg.isUser) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = BrandBlue,
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Text(
                                text = msg.text,
                                fontSize = 12.5.sp,
                                color = Color.White,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.widthIn(max = 300.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = msg.sender,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp,
                                    color = Color(0xFFD97706)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = msg.text,
                                    fontSize = 12.5.sp,
                                    color = Color(0xFF1E293B),
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Input Field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commandInput,
                onValueChange = onInputChange,
                placeholder = { Text("Ask Milo admin commands...", fontSize = 12.sp) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandBlue,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSubmit(commandInput) }),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { onSubmit(commandInput) },
                colors = IconButtonDefaults.iconButtonColors(containerColor = BrandBlue),
                modifier = Modifier.size(46.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun WorkloadTabContent(
    viewModel: MainViewModel,
    onNavigateToTasks: () -> Unit
) {
    val performanceState by viewModel.performanceTrendsState.collectAsState()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "⚡ Employee Workload & Burnout Radar",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "Identify bottlenecks and rebalance tasks before delivery delays occur.",
                fontSize = 11.5.sp,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (performanceState.employeeMetrics.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No active workload bottlenecks detected.", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                }
            }
        } else {
            items(performanceState.employeeMetrics) { metric ->
                val isHeavy = metric.tasksCompletedCount < 2 && metric.averageCompletionHours > 36.0f
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, if (isHeavy) Color(0xFFFECACA) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(metric.employeeName.take(1), fontWeight = FontWeight.Bold, color = BrandBlue)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(metric.employeeName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                Text("${metric.department} • ${metric.role}", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("${String.format("%.1f", metric.averageCompletionHours)}h avg", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = BrandBlue)
                            Text("${metric.tasksCompletedCount} done", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
