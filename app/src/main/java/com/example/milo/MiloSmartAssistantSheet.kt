package com.example.milo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LeadEntity
import com.example.data.model.TaskEntity
import com.example.domain.milo.MiloState
import com.example.ui.components.liftOnPress
import com.example.ui.screens.MainViewModel
import com.example.ui.theme.*
import com.example.util.MiloHaptics
import com.example.util.WhatsAppHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Message data model for Ask Milo AI chat.
 */
data class MiloAssistantMessage(
    val id: String,
    val sender: String,
    val text: String,
    val isUser: Boolean = false,
    val actionExecuted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Time-of-day greeting generator.
 */
fun getTimeBasedGreeting(employeeName: String): Pair<String, String> {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val name = employeeName.ifBlank { "Teammate" }
    return when (hour) {
        in 5..11 -> "Good Morning, $name! ☀️" to "Ready to conquer today's goals with 20X energy!"
        in 12..16 -> "Good Afternoon, $name! ⚡" to "Keeping the momentum strong across the pipeline!"
        in 17..20 -> "Good Evening, $name! 🌇" to "Reviewing today's victories and planning tomorrow!"
        else -> "Good Night, $name! 🌙" to "Rest well, the pride has your back 24/7!"
    }
}

/**
 * Unified, Comprehensive Milo Smart Assistant Sheet.
 * Provides:
 * 1. Time-based dynamic greeting
 * 2. Ask Milo AI conversation with voice and predefined questions
 * 3. Auto-aligning of leads across the CRM funnel
 * 4. Live Leads Funnel Inspector with conversion stats & stage drilldown
 * 5. Employee Pending Works & Missed Leads alerts
 * 6. Daily Action Summary & Monthly Performance Breakdown
 * 7. Live real-time updates connected to Room Database Flows
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiloSmartAssistantSheet(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onNavigateToLeads: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToCalls: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val employeeName by viewModel.currentEmployeeName.collectAsState()
    val allLeads by viewModel.leads.collectAsState()
    val allTasks by viewModel.tasks.collectAsState()
    val allCallLogs by viewModel.callLogs.collectAsState()
    val attendanceRecord by viewModel.latestAttendance.collectAsState()
    val miloState by viewModel.miloViewModel.state.collectAsState()
    val isMiloMuted by MiloVoiceHelper.isMutedFlow.collectAsState()

    val (greetingTitle, greetingSubtitle) = remember(employeeName) {
        getTimeBasedGreeting(employeeName)
    }

    var activeTab by remember { mutableIntStateOf(0) } // 0: Ask Milo, 1: Auto-Align, 2: Funnel, 3: Pending & Missed, 4: Summaries
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Chat state
    var inputText by remember { mutableStateOf("") }
    val chatMessages = remember {
        mutableStateListOf(
            MiloAssistantMessage(
                id = "1",
                sender = "MILO",
                text = "🦁 **$greetingTitle**\n\nI'm your **20X AI Smart Assistant**.\nI can auto-align your leads, inspect the leads funnel, highlight pending works, and summarize your daily & monthly performance.\n\nTap any quick prompt below or speak to me!",
                isUser = false
            )
        )
    }
    val chatListState = rememberLazyListState()

    // Voice mode integration
    val (voiceState, toggleVoice) = rememberMiloVoiceState { recognizedText ->
        inputText = recognizedText
    }

    // Auto-align feedback state
    var isAligningLeads by remember { mutableStateOf(false) }
    var alignResultMessage by remember { mutableStateOf<String?>(null) }

    // Predefined quick questions
    val predefinedQuestions = listOf(
        "☀️ What is my morning briefing?",
        "⚡ Auto-align all CRM leads",
        "🎯 Check leads funnel & conversion",
        "📌 What are my pending works?",
        "🚨 Show missed leads & follow-ups",
        "📅 Give me today's action summary",
        "📆 Monthly performance summary",
        "📞 Who should I call right now?",
        "Add lead Ramesh | Apex Tech | +91 98111 22333 | Website Revamp",
        "Create task Prepare formal quotation for Zenith Retail"
    )

    // Execute Smart Assistant Commands
    fun processSmartCommand(rawText: String) {
        val query = rawText.trim()
        if (query.isBlank()) return

        MiloHaptics.performButtonTap(context)
        chatMessages.add(
            MiloAssistantMessage(
                id = System.currentTimeMillis().toString(),
                sender = "You",
                text = query,
                isUser = true
            )
        )
        inputText = ""
        viewModel.miloViewModel.setState(MiloState.THINKING)

        scope.launch {
            delay(400)
            val lower = query.lowercase()

            val responseText: String = when {
                // 1. GREETING / BRIEFING
                lower.contains("briefing") || lower.contains("good morning") || lower.contains("good afternoon") || lower.contains("good evening") || lower.contains("hello") || lower.contains("hi milo") -> {
                    viewModel.miloViewModel.setState(MiloState.WELCOME)
                    val pendingCount = allTasks.count { !it.isCompleted }
                    val missedLeadsCount = allLeads.count {
                        val isWonOrLost = it.stage.equals("Won", true) || it.stage.equals("Lost", true)
                        !isWonOrLost && (it.leadScore < 60 || it.nextFollowUp.contains("Today") || it.stage.equals("New", true))
                    }
                    val totalPipelineValue = allLeads.filter { !it.stage.equals("Lost", true) }.sumOf {
                        it.potentialValue.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
                    }
                    val checkedInStatus = if (attendanceRecord?.isWorking == true) "Checked In at ${attendanceRecord?.checkInTime}" else "Not Checked In Yet"

                    "🦁 **$greetingTitle**\n\nHere is your real-time **Executive Briefing**:\n\n" +
                            "📌 **Pending Works:** $pendingCount tasks in your queue\n" +
                            "🚨 **Actionable Leads:** $missedLeadsCount leads requiring attention\n" +
                            "💼 **Active Pipeline:** ₹${String.format("%,.0f", totalPipelineValue)} across ${allLeads.size} leads\n" +
                            "⏱️ **Attendance:** $checkedInStatus\n\n" +
                            "What task would you like me to execute next?"
                }

                // 2. AUTO-ALIGN LEADS
                lower.contains("auto-align") || lower.contains("auto align") || lower.contains("align leads") -> {
                    viewModel.miloViewModel.setState(MiloState.WORKING)
                    activeTab = 1
                    val alignedCount = allLeads.size
                    "🦁 **Auto-Aligning Engine Activated!**\n\nAnalyzing $alignedCount leads based on lead score, requirement sentiment, and follow-up urgency.\nSwitching to the **Auto-Align Tab** so you can review and commit the alignment."
                }

                // 3. LEADS FUNNEL
                lower.contains("funnel") || lower.contains("conversion") || lower.contains("pipeline") -> {
                    viewModel.miloViewModel.setState(MiloState.WORKING)
                    activeTab = 2
                    val newCount = allLeads.count { it.stage.equals("New", true) }
                    val contactedCount = allLeads.count { it.stage.equals("Contacted", true) }
                    val proposalCount = allLeads.count { it.stage.equals("Proposal", true) || it.stage.equals("Interested", true) }
                    val negotiationCount = allLeads.count { it.stage.equals("Negotiation", true) || it.stage.equals("Follow-up", true) }
                    val wonCount = allLeads.count { it.stage.equals("Won", true) }
                    val lostCount = allLeads.count { it.stage.equals("Lost", true) }
                    val conversionRate = if (allLeads.isNotEmpty()) (wonCount * 100) / allLeads.size else 0

                    "🦁 **Leads Funnel Telemetry**:\n\n" +
                            "📥 **New Leads:** $newCount\n" +
                            "📞 **Contacted:** $contactedCount\n" +
                            "📑 **Proposal:** $proposalCount\n" +
                            "🤝 **Negotiation:** $negotiationCount\n" +
                            "🏆 **Won / Converted:** $wonCount\n" +
                            "🚫 **Lost:** $lostCount\n\n" +
                            "📈 **Conversion Rate:** $conversionRate%\n" +
                            "Detailed view opened in the **Funnel Tab**!"
                }

                // 4. PENDING WORKS & MISSED LEADS
                lower.contains("pending") || lower.contains("works") || lower.contains("missed") || lower.contains("overdue") -> {
                    viewModel.miloViewModel.setState(MiloState.WARNING)
                    activeTab = 3
                    val pendingTasks = allTasks.filter { !it.isCompleted }
                    val urgentTasks = pendingTasks.filter { it.priority.equals("High", true) }
                    "🦁 Found **${pendingTasks.size} Pending Works** (${urgentTasks.size} High Priority) and active follow-up targets.\nOpened the **Pending & Missed Tab** for quick resolution!"
                }

                // 5. DAILY SUMMARY
                lower.contains("today") || lower.contains("daily") || lower.contains("summarize") || lower.contains("summary") -> {
                    viewModel.miloViewModel.setState(MiloState.WORKING)
                    activeTab = 4
                    val todayCalls = allCallLogs.size
                    val completedTasks = allTasks.count { it.isCompleted }
                    val workDuration = if (attendanceRecord?.isWorking == true) {
                        "${attendanceRecord!!.durationMinutes / 60}h ${attendanceRecord!!.durationMinutes % 60}m"
                    } else "Logged"
                    "🦁 **Today's Action Summary**:\n\n" +
                            "📞 **Calls Logged:** $todayCalls calls\n" +
                            "✅ **Tasks Completed:** $completedTasks tasks\n" +
                            "⏱️ **Attendance Hours:** $workDuration\n" +
                            "Detailed daily breakdown is live in the **Summaries Tab**!"
                }

                // 5b. DRAFT PROPOSAL
                lower.contains("draft") || lower.contains("proposal") -> {
                    viewModel.miloViewModel.setState(MiloState.CELEBRATION)
                    val draftName = allLeads.firstOrNull { !it.stage.equals("Won", true) && !it.stage.equals("Lost", true) }?.name ?: "Valued Customer"
                    "🦁 **Milo Drafted WhatsApp Proposal**:\n\n" +
                            "*Proposal for $draftName*:\n" +
                            "\"Hello $draftName, thank you for connecting with Making Brands! We are excited to submit our customized proposal covering Mobile App, CRM Integrations, and Digital Strategy. Let's grow your brand 20X together!\"\n\n" +
                            "💡 *Prompt:* Say \"Who to call\" or use WhatsApp Dispatcher in CRM to send this instantly!"
                }

                // 6. MONTHLY SUMMARY
                lower.contains("month") || lower.contains("monthly") -> {
                    viewModel.miloViewModel.setState(MiloState.CELEBRATION)
                    activeTab = 4
                    val wonLeads = allLeads.filter { it.stage.equals("Won", true) }
                    val wonValue = wonLeads.sumOf { it.potentialValue.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0 }
                    "🦁 **Monthly Performance Snapshot**:\n\n" +
                            "🏆 **Deals Won This Month:** ${wonLeads.size} clients\n" +
                            "💰 **Revenue Closed:** ₹${String.format("%,.0f", wonValue)}\n" +
                            "📋 **Task Completion Rate:** ${(allTasks.count { it.isCompleted } * 100) / allTasks.size.coerceAtLeast(1)}%\n\n" +
                            "Performance rating: **20X Top Performer! ⭐**"
                }

                // 7. WHO TO CALL
                lower.contains("call") && lower.contains("who") -> {
                    viewModel.miloViewModel.setState(MiloState.FOLLOW_UP)
                    val topLead = allLeads.filter { !it.stage.equals("Won", true) && !it.stage.equals("Lost", true) }
                        .maxByOrNull { it.leadScore }
                    if (topLead != null) {
                        "🦁 **Highest Priority Lead to Call Right Now**:\n\n" +
                                "👤 **${topLead.name}** (${topLead.company})\n" +
                                "📞 **Phone:** ${topLead.phone}\n" +
                                "⭐ **Lead Score:** ${topLead.leadScore}/100\n" +
                                "🎯 **Need:** ${topLead.requirement}\n" +
                                "💼 **Value:** ${topLead.potentialValue}\n\n" +
                                "Shall I dial ${topLead.name} for you?"
                    } else {
                        "🦁 All active leads are engaged! You can add a new lead anytime."
                    }
                }

                // 8. ADD LEAD COMMAND
                lower.startsWith("add lead") || lower.startsWith("new lead") || lower.startsWith("create lead") -> {
                    viewModel.miloViewModel.setState(MiloState.NEW_LEAD)
                    val body = query.substringAfter("lead", "").removePrefix(":").trim()
                    val parts = body.split("|").map { it.trim() }
                    val name = parts.getOrNull(0)?.ifBlank { "New Client" } ?: "New Client"
                    val company = parts.getOrNull(1)?.ifBlank { "Making Brands Client" } ?: "Making Brands Client"
                    val phone = parts.getOrNull(2)?.ifBlank { "+91 98765 43210" } ?: "+91 98765 43210"
                    val req = parts.getOrNull(3)?.ifBlank { "Branding & Social Media" } ?: "Branding & Social Media"

                    viewModel.addLead(
                        name = name,
                        company = company,
                        phone = phone,
                        requirement = req,
                        value = "₹ 1,50,000",
                        stage = "New",
                        score = 85,
                        source = "Milo AI Assistant"
                    )

                    "🦁 **ROAR! New Lead Created & Locked into CRM!**\n\n" +
                            "👤 **Name:** $name\n" +
                            "🏢 **Company:** $company\n" +
                            "📞 **Phone:** $phone\n" +
                            "🎯 **Requirement:** $req\n" +
                            "Stage: **New (Score: 85)** • Live Synced to Room & Cloud! ⚡"
                }

                // 9. CREATE TASK COMMAND
                lower.startsWith("create task") || lower.startsWith("add task") || lower.startsWith("remind me") -> {
                    viewModel.miloViewModel.setState(MiloState.SUCCESS)
                    val taskTitle = when {
                        lower.startsWith("create task") -> query.substringAfter("create task").trim()
                        lower.startsWith("add task") -> query.substringAfter("add task").trim()
                        else -> query.substringAfter("remind me").removePrefix(":").trim()
                    }
                    if (taskTitle.isBlank()) {
                        "🦁 Please specify what task to schedule! Example: `Create task Follow up with Apex Tech`"
                    } else {
                        viewModel.addTask(
                            title = taskTitle,
                            projectName = "Priority Operations",
                            priority = "High",
                            dueDate = "Today",
                            category = "Work",
                            estimatedTimeNeeded = "2 Hours",
                            assignee = employeeName
                        )
                        "🦁 **Task Locked In!**\n\n📌 **Task:** $taskTitle\n⚡ **Priority:** High\n📅 **Due:** Today\n👤 **Assignee:** $employeeName"
                    }
                }

                // 10. DEFAULT SMART ASSISTANT RESPONSE
                else -> {
                    viewModel.miloViewModel.setState(MiloState.IDLE)
                    "🦁 **Milo AI Assistant Ready!**\n\nI parsed: \"$query\"\n\nHere are commands you can execute:\n" +
                            "• `Auto-align all leads`\n" +
                            "• `Check leads funnel`\n" +
                            "• `What are my pending works?`\n" +
                            "• `Daily action summary`\n" +
                            "• `Add lead Name | Company | Phone | Need`\n" +
                            "• `Create task [Title]`"
                }
            }

            chatMessages.add(
                MiloAssistantMessage(
                    id = (System.currentTimeMillis() + 1).toString(),
                    sender = "MILO",
                    text = responseText,
                    isUser = false
                )
            )
            chatListState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.50f),
        dragHandle = null,
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(SurfaceBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .imePadding()
                    .padding(16.dp)
            ) {
            // Header: Milo Avatar + Time-Based Greeting
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ImportantCardBg,
                            border = BorderStroke(2.dp, ButtonPrimary),
                            modifier = Modifier.size(118.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                MiloCharacter(
                                    state = miloState,
                                    size = 110.dp,
                                    showStateBadge = true
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = greetingTitle,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Text(
                                text = greetingSubtitle,
                                fontSize = 13.sp,
                                color = ButtonPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val currentMuted = MiloVoiceHelper.isMuted(context)
                                if (currentMuted) {
                                    MiloVoiceHelper.setMuted(context, false)
                                    MiloHaptics.performButtonTap(context)
                                    android.widget.Toast.makeText(context, "🔊 Milo voice unmuted", android.widget.Toast.LENGTH_SHORT).show()
                                    MiloVoiceHelper.speakText(context, "Milo voice is ready.")
                                } else {
                                    MiloVoiceHelper.setMuted(context, true)
                                    MiloHaptics.performButtonTap(context)
                                    android.widget.Toast.makeText(context, "🔇 Milo voice muted", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (isMiloMuted) Color(0xFFFEF2F2) else Color(0xFFF1F5F9), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isMiloMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = if (isMiloMuted) "Unmute Milo Voice" else "Mute Milo Voice",
                                tint = if (isMiloMuted) Color(0xFFEF4444) else TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFF1F5F9), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation Tabs (5 Features)
            ScrollableTabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.Transparent,
                contentColor = BrandBlue,
                edgePadding = 0.dp,
                divider = {}
            ) {
                val tabs = listOf(
                    "💬 Ask Milo" to 0,
                    "⚡ Auto-Align" to 1,
                    "🎯 Funnel" to 2,
                    "📌 Pending & Missed" to 3,
                    "📊 Summaries" to 4
                )
                tabs.forEach { (title, index) ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            HorizontalDivider(
                color = Color(0xFFE2E8F0),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Dynamic Tab Content
            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    0 -> AskMiloChatTab(
                        messages = chatMessages,
                        listState = chatListState,
                        predefinedQuestions = predefinedQuestions,
                        inputText = inputText,
                        onInputTextChange = { inputText = it },
                        onSendMessage = { processSmartCommand(it) },
                        voiceState = voiceState,
                        onToggleVoice = toggleVoice
                    )
                    1 -> AutoAlignLeadsTab(
                        leads = allLeads,
                        isAligning = isAligningLeads,
                        resultMessage = alignResultMessage,
                        onExecuteAutoAlign = {
                            isAligningLeads = true
                            alignResultMessage = null
                            scope.launch {
                                delay(800) // Realistic processing simulation
                                var alignedCount = 0
                                allLeads.forEach { lead ->
                                    val parsedValue = lead.potentialValue.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
                                    val analysis = MiloLeadAnalyzer.analyzeLead(lead.requirement, parsedValue)

                                    val newStage = when {
                                        lead.stage.equals("Won", true) -> "Won"
                                        lead.stage.equals("Lost", true) -> "Lost"
                                        analysis.urgencyScore >= 80 -> "Negotiation"
                                        analysis.urgencyScore >= 60 -> "Proposal"
                                        lead.stage.equals("New", true) && analysis.urgencyScore >= 40 -> "Contacted"
                                        else -> lead.stage
                                    }

                                    val updatedLead = lead.copy(
                                        leadScore = analysis.urgencyScore,
                                        stage = newStage,
                                        nextFollowUp = if (lead.nextFollowUp.isBlank()) "Today, 4:00 PM" else lead.nextFollowUp
                                    )
                                    viewModel.updateLeadStage(updatedLead, newStage)
                                    alignedCount++
                                }
                                isAligningLeads = false
                                alignResultMessage = "🦁 Successfully auto-aligned $alignedCount CRM leads!\nScores updated, high-intent deals escalated to Proposal/Negotiation, and follow-ups aligned."
                                MiloHaptics.performActionSuccess(context)
                            }
                        }
                    )
                    2 -> LeadsFunnelTab(
                        leads = allLeads,
                        onLeadClick = { lead ->
                            onDismiss()
                            onNavigateToLeads()
                        },
                        onCallLead = { lead ->
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${lead.phone}"))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Calling ${lead.name}...", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onWhatsAppLead = { lead ->
                            WhatsAppHelper.openWhatsAppDirectChat(
                                context = context,
                                phoneNumber = lead.phone,
                                initialMessage = "Hello ${lead.name}, regarding your requirement for ${lead.requirement} with Making Brands."
                            )
                        }
                    )
                    3 -> PendingAndMissedWorksTab(
                        tasks = allTasks,
                        leads = allLeads,
                        onToggleTask = { task ->
                            viewModel.toggleTaskCompletion(task)
                        },
                        onCallLead = { lead ->
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${lead.phone}"))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        onWhatsAppLead = { lead ->
                            WhatsAppHelper.openWhatsAppDirectChat(
                                context = context,
                                phoneNumber = lead.phone,
                                initialMessage = "Hello ${lead.name}, checking in regarding our scheduled follow-up."
                            )
                        }
                    )
                    4 -> ActionSummariesTab(
                        callLogs = allCallLogs,
                        tasks = allTasks,
                        leads = allLeads,
                        attendance = attendanceRecord
                    )
                }
            }
        }
    }
}
}

/**
 * Tab 0: Interactive Ask Milo Chat with Voice & Predefined Question Chips
 */
@Composable
private fun AskMiloChatTab(
    messages: List<MiloAssistantMessage>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    predefinedQuestions: List<String>,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    voiceState: MiloVoiceState,
    onToggleVoice: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Predefined Quick Prompt Pills (Horizontal Scroll)
        Text(
            text = "⚡ PREDEFINED QUICK ACTIONS:",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(predefinedQuestions) { question ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.3f)),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .liftOnPress(elevationLift = 4.dp, translateY = (-3).dp)
                        .clickable { onSendMessage(question) }
                ) {
                    Text(
                        text = question,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandBlue,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chat Message History
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                MiloChatBubble(msg = msg)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input Row with Voice & Send
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChange,
                placeholder = { Text("Ask Milo anything or type command...", fontSize = 13.sp, color = TextMuted) },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandBlue,
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Voice Mic Button
            FilledIconButton(
                onClick = onToggleVoice,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (voiceState.isListening) StatusRed else Color(0xFFF1F5F9),
                    contentColor = if (voiceState.isListening) Color.White else TextPrimary
                ),
                modifier = Modifier
                    .size(46.dp)
                    .liftOnPress()
            ) {
                Icon(
                    imageVector = if (voiceState.isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Voice Input"
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Send Button
            FilledIconButton(
                onClick = { onSendMessage(inputText) },
                enabled = inputText.isNotBlank(),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = BrandBlue,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .size(46.dp)
                    .liftOnPress()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MiloChatBubble(msg: MiloAssistantMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!msg.isUser) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, BrandBlue),
                modifier = Modifier
                    .size(32.dp)
                    .padding(end = 4.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🦁", fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (msg.isUser) 16.dp else 4.dp,
                bottomEnd = if (msg.isUser) 4.dp else 16.dp
            ),
            color = if (msg.isUser) BrandBlue else Color.White,
            border = if (msg.isUser) null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = msg.text,
                fontSize = 13.sp,
                color = if (msg.isUser) Color.White else TextPrimary,
                modifier = Modifier.padding(12.dp),
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * Tab 1: Auto-Aligning of Leads Engine
 */
@Composable
private fun AutoAlignLeadsTab(
    leads: List<LeadEntity>,
    isAligning: Boolean,
    resultMessage: String?,
    onExecuteAutoAlign: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoMode, contentDescription = null, tint = BrandBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Milo AI Lead Alignment Engine",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandDarkBlue
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Scans all ${leads.size} leads, calculates urgency & deal value scores, aligns them into proper pipeline stages (New ➔ Contacted ➔ Proposal ➔ Negotiation), and schedules priority follow-ups.",
                        fontSize = 12.sp,
                        color = Color(0xFF1E3A8A),
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onExecuteAutoAlign,
                        enabled = !isAligning,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .liftOnPress()
                    ) {
                        if (isAligning) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyzing & Aligning Leads...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Bolt, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Execute Auto-Alignment (${leads.size} Leads)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (resultMessage != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = resultMessage,
                            fontSize = 12.sp,
                            color = Color(0xFF166534),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        item {
            Text(
                "CURRENT FUNNEL ALIGNMENT DISTRIBUTION:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
        }

        items(leads.take(8)) { lead ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(lead.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                        Text("${lead.company} • ${lead.requirement}", fontSize = 11.sp, color = TextSecondary, maxLines = 1)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (lead.stage.lowercase()) {
                                "won" -> Color(0xFFDCFCE7)
                                "negotiation" -> Color(0xFFFEF3C7)
                                "proposal" -> Color(0xFFEDE9FE)
                                else -> Color(0xFFEFF6FF)
                            }
                        ) {
                            Text(
                                text = lead.stage,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (lead.stage.lowercase()) {
                                    "won" -> StatusGreen
                                    "negotiation" -> Color(0xFFD97706)
                                    "proposal" -> Color(0xFF7C3AED)
                                    else -> BrandBlue
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "Score: ${lead.leadScore}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (lead.leadScore >= 75) StatusGreen else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tab 2: Leads Funnel Inspector & Telemetry
 */
@Composable
private fun LeadsFunnelTab(
    leads: List<LeadEntity>,
    onLeadClick: (LeadEntity) -> Unit,
    onCallLead: (LeadEntity) -> Unit,
    onWhatsAppLead: (LeadEntity) -> Unit
) {
    val newLeads = remember(leads) { leads.filter { it.stage.equals("New", true) } }
    val contactedLeads = remember(leads) { leads.filter { it.stage.equals("Contacted", true) } }
    val proposalLeads = remember(leads) { leads.filter { it.stage.equals("Proposal", true) || it.stage.equals("Interested", true) } }
    val negotiationLeads = remember(leads) { leads.filter { it.stage.equals("Negotiation", true) || it.stage.equals("Follow-up", true) } }
    val wonLeads = remember(leads) { leads.filter { it.stage.equals("Won", true) } }
    val lostLeads = remember(leads) { leads.filter { it.stage.equals("Lost", true) } }

    val totalPipeline = remember(leads) {
        leads.filter { !it.stage.equals("Lost", true) }.sumOf {
            it.potentialValue.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
        }
    }
    val conversionRate = if (leads.isNotEmpty()) (wonLeads.size * 100) / leads.size else 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Funnel Overview Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Active Pipeline Value", fontSize = 11.sp, color = BrandDarkBlue, fontWeight = FontWeight.Medium)
                        Text("₹${String.format("%,.0f", totalPipeline)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Conversion Rate", fontSize = 11.sp, color = Color(0xFF166534), fontWeight = FontWeight.Medium)
                        Text("$conversionRate% Won", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
                    }
                }
            }
        }

        item {
            Text("PIPELINE FUNNEL STAGES:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
        }

        // Funnel Bars
        val stages = listOf(
            Triple("📥 1. Inbound Leads", newLeads, BrandBlue),
            Triple("📞 2. Contacted & Engaged", contactedLeads, Color(0xFF0284C7)),
            Triple("📑 3. Proposal Submitted", proposalLeads, Color(0xFF7C3AED)),
            Triple("🤝 4. In Negotiation", negotiationLeads, Color(0xFFD97706)),
            Triple("🏆 5. Closed Won", wonLeads, StatusGreen),
            Triple("🚫 6. Closed Lost", lostLeads, StatusRed)
        )

        items(stages) { (title, stageLeads, color) ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                        Text("${stageLeads.size} leads", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = color)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val fraction = if (leads.isNotEmpty()) (stageLeads.size.toFloat() / leads.size.toFloat()).coerceIn(0.04f, 1f) else 0f
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = color,
                        trackColor = color.copy(alpha = 0.15f)
                    )

                    if (stageLeads.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Top deal: ${stageLeads.first().name} (${stageLeads.first().potentialValue})",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tab 3: Employee Pending Works & Missed Leads
 */
@Composable
private fun PendingAndMissedWorksTab(
    tasks: List<TaskEntity>,
    leads: List<LeadEntity>,
    onToggleTask: (TaskEntity) -> Unit,
    onCallLead: (LeadEntity) -> Unit,
    onWhatsAppLead: (LeadEntity) -> Unit
) {
    val pendingTasks = remember(tasks) { tasks.filter { !it.isCompleted } }
    val missedLeads = remember(leads) {
        leads.filter {
            !it.stage.equals("Won", true) && !it.stage.equals("Lost", true) &&
                    (it.nextFollowUp.contains("Today") || it.leadScore < 60 || it.stage.equals("New", true))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFEF2F2),
                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = StatusRed)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Action Required Alert", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF991B1B))
                        Text(
                            "You have ${pendingTasks.size} pending tasks and ${missedLeads.size} leads requiring immediate engagement.",
                            fontSize = 11.sp,
                            color = Color(0xFFB91C1C)
                        )
                    }
                }
            }
        }

        item {
            Text("📌 PENDING TASKS (${pendingTasks.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
        }

        if (pendingTasks.isEmpty()) {
            item {
                Text("🎉 All tasks completed! Great work, teammate.", fontSize = 12.sp, color = StatusGreen)
            }
        } else {
            items(pendingTasks) { task ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = task.isCompleted,
                            onCheckedChange = { onToggleTask(task) }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(task.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                            Text("${task.projectName} • Priority: ${task.priority} • Due: ${task.dueDate}", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text("🚨 MISSED & OVERDUE LEADS (${missedLeads.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusRed)
        }

        items(missedLeads) { lead ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(lead.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                        Text("${lead.company} • ${lead.phone}", fontSize = 11.sp, color = TextSecondary)
                        Text("Follow-up: ${lead.nextFollowUp}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StatusRed)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = { onCallLead(lead) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFEFF6FF), CircleShape)
                                .liftOnPress()
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Call", tint = BrandBlue, modifier = Modifier.size(18.dp))
                        }

                        IconButton(
                            onClick = { onWhatsAppLead(lead) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFF0FDF4), CircleShape)
                                .liftOnPress()
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = StatusGreen, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 4: Daily Action Summary & Monthly Performance Breakdown
 */
@Composable
private fun ActionSummariesTab(
    callLogs: List<com.example.data.model.CallLogEntity>,
    tasks: List<TaskEntity>,
    leads: List<LeadEntity>,
    attendance: com.example.data.model.AttendanceRecord?
) {
    val totalCalls = callLogs.size
    val completedTasksCount = tasks.count { it.isCompleted }
    val wonLeads = leads.filter { it.stage.equals("Won", true) }
    val wonRevenue = wonLeads.sumOf { it.potentialValue.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0 }
    val activeHours = if (attendance?.isWorking == true) {
        "${attendance.durationMinutes / 60}h ${attendance.durationMinutes % 60}m"
    } else "4h 30m"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Daily Action Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Today, contentDescription = null, tint = BrandBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📅 Today's Action Summary", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MetricItem("Calls Handled", "$totalCalls", BrandBlue)
                        MetricItem("Tasks Done", "$completedTasksCount", StatusGreen)
                        MetricItem("Work Hours", activeHours, Color(0xFF7C3AED))
                    }
                }
            }
        }

        // Monthly Performance Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFFD97706))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📆 Monthly Performance Report", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MetricItem("Deals Closed", "${wonLeads.size}", StatusGreen)
                        MetricItem("Revenue Won", "₹${String.format("%,.0f", wonRevenue)}", BrandBlue)
                        MetricItem("Task Ratio", "${(completedTasksCount * 100) / tasks.size.coerceAtLeast(1)}%", Color(0xFF0284C7))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEF3C7)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🦁", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Milo Performance Rating: 20X Top Tier Executive! Leading the Pride in output.",
                                fontStyle = null,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String, color: Color) {
    Column {
        Text(label, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
    }
}
