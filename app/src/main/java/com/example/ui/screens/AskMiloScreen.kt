package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.milo.MiloAssistantMessage
import com.example.milo.MiloCharacter
import com.example.milo.MiloLeadAnalyzer
import com.example.milo.getTimeBasedGreeting
import com.example.milo.rememberMiloVoiceState
import com.example.ui.components.StandardScreenHeader
import com.example.ui.components.hoverLiftClickable
import com.example.ui.components.liftOnPress
import com.example.ui.theme.*
import com.example.util.MiloHaptics
import com.example.util.WhatsAppHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Dedicated, Full-Screen Ask Milo AI Smart CRM Copilot & Assistant Screen.
 * Provides complete access to:
 * - Dynamic Time-of-Day Greeting ("Good Morning", "Good Afternoon", "Good Evening", "Good Night")
 * - Ask Milo Conversational AI with Voice Recognition & Predefined Quick Questions
 * - Auto-Aligning CRM Leads Engine (Room DB live updates)
 * - Leads Funnel Inspector & Telemetry
 * - Employee Pending Works & Missed Leads alerts
 * - Daily Action Summaries & Monthly Performance Breakdown
 * - Smooth button lift-up hover animations on every interactive control
 */
@Composable
fun AskMiloScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
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

    val (greetingTitle, greetingSubtitle) = remember(employeeName) {
        getTimeBasedGreeting(employeeName)
    }

    var activeTab by remember { mutableIntStateOf(0) } // 0: Chat, 1: Auto-Align, 2: Funnel, 3: Pending & Missed, 4: Summaries

    // Chat state
    var inputText by remember { mutableStateOf("") }
    val chatMessages = remember {
        mutableStateListOf(
            MiloAssistantMessage(
                id = "1",
                sender = "MILO",
                text = "🦁 **$greetingTitle**\n\nI'm your **20X AI Smart Assistant**.\n" +
                        "I can auto-align your leads, inspect the leads funnel, highlight pending works, and summarize your daily & monthly performance.\n\n" +
                        "Tap any predefined quick action below or speak/type your command!",
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

    // Predefined quick questions chips
    val predefinedQuestions = remember {
        listOf(
            "☀️ Morning briefing",
            "⚡ Auto-align all CRM leads",
            "🎯 Check leads funnel & conversion",
            "📌 What are my pending works?",
            "🚨 Show missed leads & follow-ups",
            "📅 Today's action summary",
            "📆 Monthly performance summary",
            "📞 Who should I call right now?",
            "➕ Add lead Ramesh | Apex Tech | +91 98111 22333 | Website Revamp",
            "✅ Create task Prepare formal proposal for Zenith"
        )
    }

    // Smart Command Processor
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
                lower.contains("briefing") || lower.contains("good morning") || lower.contains("good afternoon") ||
                        lower.contains("good evening") || lower.contains("good night") || lower.contains("hello") || lower.contains("hi milo") -> {
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

                    "🦁 **$greetingTitle**\n\nHere is your live **Executive Briefing**:\n\n" +
                            "📌 **Pending Works:** $pendingCount tasks in your queue\n" +
                            "🚨 **Actionable Leads:** $missedLeadsCount leads requiring prompt attention\n" +
                            "💼 **Active Pipeline:** ₹${String.format("%,.0f", totalPipelineValue)} across ${allLeads.size} leads\n" +
                            "⏱️ **Attendance:** $checkedInStatus\n\n" +
                            "Ready to execute! What task shall we tackle next?"
                }

                // 2. AUTO-ALIGN LEADS
                lower.contains("auto-align") || lower.contains("auto align") || lower.contains("align leads") -> {
                    viewModel.miloViewModel.setState(MiloState.WORKING)
                    activeTab = 1
                    val count = allLeads.size
                    "🦁 **Auto-Aligning Engine Activated!**\n\n" +
                            "Evaluating all $count leads based on deal size, urgency sentiment, and follow-up priority.\n" +
                            "Switched to the **Auto-Align Tab** where you can review and commit the live alignment."
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
                            "Detailed stage breakdown is now visible in the **Funnel Tab**!"
                }

                // 4. PENDING WORKS & MISSED LEADS
                lower.contains("pending") || lower.contains("works") || lower.contains("missed") || lower.contains("overdue") -> {
                    viewModel.miloViewModel.setState(MiloState.WARNING)
                    activeTab = 3
                    val pendingTasks = allTasks.filter { !it.isCompleted }
                    val urgentTasks = pendingTasks.filter { it.priority.equals("High", true) }
                    val missed = allLeads.count {
                        !it.stage.equals("Won", true) && !it.stage.equals("Lost", true) &&
                                (it.nextFollowUp.contains("Today") || it.leadScore < 60)
                    }
                    "🦁 Found **${pendingTasks.size} Pending Works** (${urgentTasks.size} High Priority) and **$missed Missed Leads** requiring follow-up.\n" +
                            "Switched to the **Pending & Missed Tab** for instant 1-tap action!"
                }

                // 5. DAILY SUMMARY
                lower.contains("today") || lower.contains("daily") -> {
                    viewModel.miloViewModel.setState(MiloState.WORKING)
                    activeTab = 4
                    val todayCalls = allCallLogs.size
                    val completedTasks = allTasks.count { it.isCompleted }
                    val workDuration = if (attendanceRecord?.isWorking == true) {
                        "${attendanceRecord!!.durationMinutes / 60}h ${attendanceRecord!!.durationMinutes % 60}m"
                    } else "Logged"

                    "🦁 **Today's Action Summary**:\n\n" +
                            "📞 **Calls Handled:** $todayCalls calls\n" +
                            "✅ **Tasks Completed:** $completedTasks tasks\n" +
                            "⏱️ **Attendance Hours:** $workDuration\n\n" +
                            "Full live daily breakdown opened in the **Summaries Tab**!"
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
                            "Performance Rating: **20X Pride Leader! ⭐**"
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
                                "Tap Call in the Funnel Tab or dial ${topLead.phone} to close this deal!"
                    } else {
                        "🦁 All active leads are engaged! You can add a new lead anytime."
                    }
                }

                // 8. ADD LEAD COMMAND
                lower.startsWith("add lead") || lower.startsWith("new lead") || lower.startsWith("create lead") || lower.startsWith("➕ add lead") -> {
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
                            "Stage: **New (Score: 85)** • Live Synced to Room Database! ⚡"
                }

                // 9. CREATE TASK COMMAND
                lower.startsWith("create task") || lower.startsWith("add task") || lower.startsWith("remind me") || lower.startsWith("✅ create task") -> {
                    viewModel.miloViewModel.setState(MiloState.SUCCESS)
                    val taskTitle = when {
                        lower.contains("create task") -> query.substringAfter("create task").trim()
                        lower.contains("add task") -> query.substringAfter("add task").trim()
                        else -> query.substringAfter("remind me").removePrefix(":").trim()
                    }
                    if (taskTitle.isBlank()) {
                        "🦁 Please specify what task to schedule! Example: `Create task Prepare quotation for Apex Tech`"
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
                    "🦁 **Milo Smart Assistant Ready!**\n\n" +
                            "I've received your request: \"$query\"\n\n" +
                            "Here is what I can do for you right now:\n" +
                            "• `Auto-align all leads` to optimize the pipeline\n" +
                            "• `Check leads funnel` for conversion metrics\n" +
                            "• `What are my pending works?` for task alerts\n" +
                            "• `Daily action summary` or `Monthly summary`\n" +
                            "• `Add lead Name | Company | Phone | Need`\n" +
                            "• `Create task [Task title]`\n" +
                            "• Or ask any strategic sales advice!"
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

    Scaffold(
        topBar = {
            StandardScreenHeader(
                viewModel = viewModel,
                subMenuTitle = "Ask Milo AI",
                subMenuSubtitle = "20X Smart CRM Assistant & Executive Copilot",
                onBack = onBack
            )
        },
        containerColor = SurfaceBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Top Proactive Time-of-Day Greeting Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .liftOnPress(elevationLift = 6.dp, translateY = (-3).dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Clean Milo Character - NO circle shape, transparent background
                    Box(
                        modifier = Modifier.size(124.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        MiloCharacter(
                            state = miloState,
                            size = 120.dp,
                            showStateBadge = false
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
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

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                    ) {
                        Text(
                            text = "LIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = StatusGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
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
                        onClick = {
                            MiloHaptics.performButtonTap(context)
                            activeTab = index
                        },
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
                    0 -> AskMiloChatContent(
                        messages = chatMessages,
                        listState = chatListState,
                        predefinedQuestions = predefinedQuestions,
                        inputText = inputText,
                        onInputTextChange = { inputText = it },
                        onSendMessage = { processSmartCommand(it) },
                        voiceState = voiceState,
                        onToggleVoice = toggleVoice
                    )

                    1 -> AutoAlignLeadsContent(
                        leads = allLeads,
                        isAligning = isAligningLeads,
                        resultMessage = alignResultMessage,
                        onExecuteAutoAlign = {
                            isAligningLeads = true
                            alignResultMessage = null
                            scope.launch {
                                delay(700)
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
                                alignResultMessage = "🦁 Successfully auto-aligned $alignedCount CRM leads!\nScores updated, high-intent deals escalated to Proposal/Negotiation, and follow-ups aligned in Room DB."
                                MiloHaptics.performActionSuccess(context)
                            }
                        },
                        onCallLead = { lead ->
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${lead.phone}"))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Dialing ${lead.name}...", Toast.LENGTH_SHORT).show()
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

                    2 -> LeadsFunnelContent(
                        leads = allLeads,
                        onCallLead = { lead ->
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${lead.phone}"))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Dialing ${lead.name}...", Toast.LENGTH_SHORT).show()
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

                    3 -> PendingAndMissedWorksContent(
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

                    4 -> ActionSummariesContent(
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

/**
 * Tab 0: Ask Milo Chat Content with Voice & Predefined Question Chips
 */
@Composable
private fun AskMiloChatContent(
    messages: List<MiloAssistantMessage>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    predefinedQuestions: List<String>,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    voiceState: com.example.milo.MiloVoiceState,
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
                    border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.35f)),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .liftOnPress(elevationLift = 5.dp, translateY = (-3).dp)
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
                MiloMessageBubble(msg = msg)
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
private fun MiloMessageBubble(msg: MiloAssistantMessage) {
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
 * Tab 1: Auto-Aligning Leads Content
 */
@Composable
private fun AutoAlignLeadsContent(
    leads: List<LeadEntity>,
    isAligning: Boolean,
    resultMessage: String?,
    onExecuteAutoAlign: () -> Unit,
    onCallLead: (LeadEntity) -> Unit,
    onWhatsAppLead: (LeadEntity) -> Unit
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
                        "Scans all ${leads.size} leads in Room DB, analyzes urgency & deal value scores, auto-aligns stages (New ➔ Contacted ➔ Proposal ➔ Negotiation), and schedules priority follow-ups.",
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
                            .liftOnPress(elevationLift = 6.dp, translateY = (-3).dp)
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

        items(leads) { lead ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .liftOnPress(elevationLift = 4.dp, translateY = (-2).dp)
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
                        Text("Follow-up: ${lead.nextFollowUp}", fontSize = 10.sp, color = BrandBlue, fontWeight = FontWeight.Medium)
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

                        IconButton(
                            onClick = { onCallLead(lead) },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFFEFF6FF), CircleShape)
                                .liftOnPress()
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Call", tint = BrandBlue, modifier = Modifier.size(16.dp))
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = { onWhatsAppLead(lead) },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFFF0FDF4), CircleShape)
                                .liftOnPress()
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = StatusGreen, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 2: Leads Funnel Inspector Content
 */
@Composable
private fun LeadsFunnelContent(
    leads: List<LeadEntity>,
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

    var selectedStageFilter by remember { mutableStateOf<String?>(null) }

    val filteredLeads = remember(leads, selectedStageFilter) {
        if (selectedStageFilter == null) leads
        else leads.filter { it.stage.equals(selectedStageFilter, true) }
    }

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
                    modifier = Modifier
                        .weight(1f)
                        .liftOnPress(elevationLift = 4.dp, translateY = (-2).dp)
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
                    modifier = Modifier
                        .weight(1f)
                        .liftOnPress(elevationLift = 4.dp, translateY = (-2).dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Conversion Rate", fontSize = 11.sp, color = Color(0xFF166534), fontWeight = FontWeight.Medium)
                        Text("$conversionRate% Won", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
                    }
                }
            }
        }

        item {
            Text("PIPELINE FUNNEL STAGES (Tap to filter):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
        }

        // Funnel Bars
        val stages = listOf(
            Triple("📥 1. Inbound Leads", newLeads, BrandBlue to "New"),
            Triple("📞 2. Contacted & Engaged", contactedLeads, Color(0xFF0284C7) to "Contacted"),
            Triple("📑 3. Proposal Submitted", proposalLeads, Color(0xFF7C3AED) to "Proposal"),
            Triple("🤝 4. In Negotiation", negotiationLeads, Color(0xFFD97706) to "Negotiation"),
            Triple("🏆 5. Closed Won", wonLeads, StatusGreen to "Won"),
            Triple("🚫 6. Closed Lost", lostLeads, StatusRed to "Lost")
        )

        items(stages) { (title, stageLeads, colorAndStage) ->
            val (color, stageKey) = colorAndStage
            val isSelected = selectedStageFilter.equals(stageKey, true)

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) color.copy(alpha = 0.08f) else Color.White,
                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) color else Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .liftOnPress(elevationLift = 5.dp, translateY = (-3).dp)
                    .clickable {
                        selectedStageFilter = if (isSelected) null else stageKey
                    }
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
                        Spacer(modifier = Modifier.height(6.dp))
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

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedStageFilter != null) "LEADS IN '$selectedStageFilter' (${filteredLeads.size}):" else "ALL CRM LEADS (${filteredLeads.size}):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                if (selectedStageFilter != null) {
                    TextButton(onClick = { selectedStageFilter = null }) {
                        Text("Show All", fontSize = 11.sp, color = BrandBlue)
                    }
                }
            }
        }

        items(filteredLeads) { lead ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .liftOnPress()
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
                        Text("${lead.company} • ${lead.potentialValue}", fontSize = 11.sp, color = TextSecondary)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = { onCallLead(lead) },
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFFEFF6FF), CircleShape)
                                .liftOnPress()
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Call", tint = BrandBlue, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { onWhatsAppLead(lead) },
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFFF0FDF4), CircleShape)
                                .liftOnPress()
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = StatusGreen, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 3: Employee Pending Works & Missed Leads Content
 */
@Composable
private fun PendingAndMissedWorksContent(
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
                modifier = Modifier
                    .fillMaxWidth()
                    .liftOnPress(elevationLift = 4.dp, translateY = (-2).dp)
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
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF0FDF4),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "🎉 All tasks completed! Great work, teammate.",
                        fontSize = 12.sp,
                        color = StatusGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        } else {
            items(pendingTasks) { task ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .liftOnPress(elevationLift = 4.dp, translateY = (-2).dp)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .liftOnPress(elevationLift = 4.dp, translateY = (-2).dp)
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
 * Tab 4: Action Summaries Content
 */
@Composable
private fun ActionSummariesContent(
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
                modifier = Modifier
                    .fillMaxWidth()
                    .liftOnPress(elevationLift = 5.dp, translateY = (-3).dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Today, contentDescription = null, tint = BrandBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📅 Today's Action Summary", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        SummaryMetric("Calls Handled", "$totalCalls", BrandBlue)
                        SummaryMetric("Tasks Done", "$completedTasksCount", StatusGreen)
                        SummaryMetric("Work Hours", activeHours, Color(0xFF7C3AED))
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
                modifier = Modifier
                    .fillMaxWidth()
                    .liftOnPress(elevationLift = 5.dp, translateY = (-3).dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFFD97706))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📆 Monthly Performance Report", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        SummaryMetric("Deals Closed", "${wonLeads.size}", StatusGreen)
                        SummaryMetric("Revenue Won", "₹${String.format("%,.0f", wonRevenue)}", BrandBlue)
                        SummaryMetric("Task Ratio", "${(completedTasksCount * 100) / tasks.size.coerceAtLeast(1)}%", Color(0xFF0284C7))
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
                                fontSize = 12.sp,
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
private fun SummaryMetric(label: String, value: String, color: Color) {
    Column {
        Text(label, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
    }
}
