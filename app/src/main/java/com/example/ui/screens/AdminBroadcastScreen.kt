package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.FcmBroadcastLog
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Dedicated Admin Panel View for composing and broadcasting Firebase Cloud Messaging (FCM)
 * push notifications to all users or specific employee departments.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBroadcastScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val fcmToken by viewModel.fcmToken.collectAsState()
    val isFcmReady by viewModel.isFcmReady.collectAsState()
    val registeredDeviceCount by viewModel.registeredDeviceCount.collectAsState()
    val broadcastHistory by viewModel.fcmBroadcastHistory.collectAsState()

    var notifTitle by remember { mutableStateOf("") }
    var notifMessage by remember { mutableStateOf("") }
    var selectedAudience by remember { mutableStateOf("All Employees") }
    var selectedTopic by remember { mutableStateOf("all_users") }
    var selectedPriority by remember { mutableStateOf("High") }
    var selectedCategory by remember { mutableStateOf("announcement") }
    var selectedRoute by remember { mutableStateOf("notifications") }
    var isSending by remember { mutableStateOf(false) }

    var showTokenDetailsDialog by remember { mutableStateOf(false) }
    var broadcastToDelete by remember { mutableStateOf<FcmBroadcastLog?>(null) }

    val audienceOptions = listOf(
        Triple("All Employees", "all_users", "Broadcasting to entire enterprise team"),
        Triple("Sales & CRM Team", "sales_team", "Telecallers, BDA, and Account Managers"),
        Triple("Engineering & Tech", "dev_team", "Mobile, Backend, and QA Developers"),
        Triple("Field Operations", "field_team", "Site reps and field executives"),
        Triple("Leadership & Admin", "management", "Branch managers and Directors")
    )

    val templates = listOf(
        "📢 All-Hands Meeting" to "Urgent: Team All-Hands meeting starting in 15 minutes. Please join the company bridge.",
        "🎯 Target Milestone" to "Phenomenal performance team! Q3 revenue milestone achieved with 100% commission bonus.",
        "⚡ Flash CRM Leads" to "Attention Sales: 24 incoming Meta & Justdial leads require immediate WhatsApp follow-up.",
        "🚨 System Maintenance" to "Routine cloud infrastructure maintenance scheduled tonight from 11:00 PM to 11:30 PM.",
        "🎉 Festive Holiday" to "Company holiday announced for the upcoming festival. View your updated holiday roster."
    )

    val availableRoutes = listOf(
        "notifications" to "Notifications Center",
        "leads" to "CRM Leads Pipeline",
        "tasks" to "My Tasks & Assignments",
        "milo_ai" to "Milo AI Copilot",
        "invoices" to "Quotations & Invoices",
        "chat" to "Team Chat Channels",
        "attendance" to "Attendance Punch-In"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "FCM Push Notifications",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isFcmReady) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (isFcmReady) StatusGreen else StatusOrange)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isFcmReady) "FCM Topic: all_users" else "Initializing",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFcmReady) Color(0xFF166534) else Color(0xFF92400E)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Firebase Cloud Messaging & System Alerts Broadcast",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_broadcast_back")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showTokenDetailsDialog = true }) {
                        Icon(
                            Icons.Default.VpnKey,
                            contentDescription = "View FCM Token",
                            tint = BrandBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 1. FCM Health & Registered Devices Banner
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.CloudQueue,
                                            contentDescription = null,
                                            tint = StatusOrange,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Firebase Cloud Messaging (FCM)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "High-priority push dispatch via Google Play services",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Devices,
                                        contentDescription = null,
                                        tint = StatusGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$registeredDeviceCount Device${if (registeredDeviceCount > 1) "s" else ""}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusGreen
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Active FCM Token:",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = fcmToken?.take(36)?.let { "$it..." } ?: "Generating FCM Device Token...",
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = Color(0xFFCBD5E1),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            TextButton(
                                onClick = {
                                    fcmToken?.let { token ->
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("FCM Token", token))
                                        Toast.makeText(context, "FCM Device Token copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy", fontSize = 11.sp, color = BrandBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. Compose New Push Notification Form
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFFF7ED),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.EditNote,
                                        contentDescription = null,
                                        tint = StatusOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Compose Push Message",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Dispatches instant Android system notification & in-app alert",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Templates
                        Text(
                            text = "Quick Templates:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(templates) { (tTitle, tMsg) ->
                                FilterChip(
                                    selected = notifTitle == tTitle,
                                    onClick = {
                                        notifTitle = tTitle
                                        notifMessage = tMsg
                                    },
                                    label = { Text(tTitle, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFEFF6FF),
                                        selectedLabelColor = BrandBlue
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Title Field
                        OutlinedTextField(
                            value = notifTitle,
                            onValueChange = { notifTitle = it },
                            label = { Text("Notification Title *") },
                            placeholder = { Text("e.g. Urgent All-Hands Call") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("fcm_notif_title_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Body Field
                        OutlinedTextField(
                            value = notifMessage,
                            onValueChange = { notifMessage = it },
                            label = { Text("Notification Body / Message *") },
                            placeholder = { Text("Enter detailed broadcast announcement...") },
                            minLines = 3,
                            maxLines = 4,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("fcm_notif_body_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Target Audience Selector
                        Text(
                            text = "Target Audience & Topic:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            audienceOptions.forEach { (audName, audTopic, audDesc) ->
                                val isSelected = selectedAudience == audName
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) BrandBlue else Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedAudience = audName
                                            selectedTopic = audTopic
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                selectedAudience = audName
                                                selectedTopic = audTopic
                                            },
                                            colors = RadioButtonDefaults.colors(selectedColor = BrandBlue),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = audName,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) BrandBlue else TextPrimary
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (isSelected) BrandBlue.copy(alpha = 0.12f) else Color(0xFFE2E8F0)
                                                ) {
                                                    Text(
                                                        text = "topic: $audTopic",
                                                        fontSize = 9.sp,
                                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                        color = if (isSelected) BrandBlue else Color(0xFF64748B),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = audDesc,
                                                fontSize = 10.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Priority and Action Target
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Priority Selector
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Priority Level:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("High", "Urgent", "Normal").forEach { pr ->
                                        FilterChip(
                                            selected = selectedPriority == pr,
                                            onClick = { selectedPriority = pr },
                                            label = { Text(pr, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = if (pr == "Urgent") Color(0xFFFEE2E2) else Color(0xFFEFF6FF),
                                                selectedLabelColor = if (pr == "Urgent") StatusRed else BrandBlue
                                            )
                                        )
                                    }
                                }
                            }

                            // Tap Action Route Dropdown
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Tap Action Screen:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                var routeExpanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = routeExpanded,
                                    onExpandedChange = { routeExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = availableRoutes.firstOrNull { it.first == selectedRoute }?.second ?: selectedRoute,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = routeExpanded) },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = routeExpanded,
                                        onDismissRequest = { routeExpanded = false }
                                    ) {
                                        availableRoutes.forEach { (rk, rl) ->
                                            DropdownMenuItem(
                                                text = { Text(rl, fontSize = 12.sp) },
                                                onClick = {
                                                    selectedRoute = rk
                                                    routeExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Live Android Notification Preview
                        Text(
                            text = "Live Android Notification Preview:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = StatusOrange,
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Campaign,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "MB TRAKER • NOW",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF64748B),
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (selectedPriority == "Urgent") Color(0xFFFEE2E2) else Color(0xFFEFF6FF)
                                    ) {
                                        Text(
                                            text = "$selectedPriority Priority",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedPriority == "Urgent") StatusRed else BrandBlue,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = notifTitle.ifBlank { "📢 Notification Headline" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (notifMessage.isNotBlank()) "[$selectedAudience] $notifMessage" else "Preview of the broadcast text as it will appear in the system status bar and lockscreen.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF475569),
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                if (notifTitle.isBlank() || notifMessage.isBlank()) return@Button
                                isSending = true
                                coroutineScope.launch {
                                    viewModel.sendAdminBroadcastPushNotification(
                                        title = notifTitle.trim(),
                                        message = notifMessage.trim(),
                                        audience = selectedAudience,
                                        priority = selectedPriority,
                                        category = selectedCategory,
                                        topic = selectedTopic,
                                        actionRoute = selectedRoute,
                                        context = context
                                    )
                                    isSending = false
                                    Toast.makeText(
                                        context,
                                        "FCM Broadcast sent successfully to $selectedAudience!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    notifTitle = ""
                                    notifMessage = ""
                                }
                            },
                            enabled = notifTitle.isNotBlank() && notifMessage.isNotBlank() && !isSending,
                            colors = ButtonDefaults.buttonColors(containerColor = StatusOrange),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("broadcast_push_submit_button")
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Broadcasting via FCM...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Send FCM Push Notification to $selectedAudience",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // 3. Sent Broadcast History & Cloud Delivery Logs
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SENT BROADCASTS & CLOUD LOGS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${broadcastHistory.size} Broadcasts Sent",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue
                    )
                }
            }

            if (broadcastHistory.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Campaign,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No Broadcasts Sent Yet",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Compose and send a message above to dispatch an FCM alert to all employees.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(broadcastHistory, key = { it.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("broadcast_log_${item.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFDCFCE7),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = StatusGreen,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.deliveryStatus,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF166534)
                                    )
                                }

                                Text(
                                    text = item.formattedDate,
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = item.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.message,
                                fontSize = 11.sp,
                                color = Color(0xFF475569)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFFFF7ED)
                                    ) {
                                        Text(
                                            text = item.audience,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusOrange,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFEFF6FF)
                                    ) {
                                        Text(
                                            text = "topic: ${item.topic}",
                                            fontSize = 9.sp,
                                            color = BrandBlue,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Re-send button
                                    TextButton(
                                        onClick = {
                                            notifTitle = item.title
                                            notifMessage = item.message
                                            selectedAudience = item.audience
                                            selectedTopic = item.topic
                                            selectedPriority = item.priority
                                            Toast.makeText(context, "Loaded into composer above", Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Reuse", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    IconButton(
                                        onClick = { broadcastToDelete = item },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Log",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Dialog: FCM Token Details
    if (showTokenDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showTokenDetailsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = BrandBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("FCM Device Registration", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "This device is registered with Firebase Cloud Messaging and synchronized with Firestore collection 'fcm_tokens'.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("DEVICE FCM TOKEN:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = fcmToken ?: "Generating token...",
                                fontSize = 10.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = TextPrimary
                            )
                        }
                    }

                    Text(
                        "Subscribed Topics: all_users, announcements, company_broadcasts",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StatusGreen
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        fcmToken?.let { token ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("FCM Token", token))
                            Toast.makeText(context, "FCM Token copied!", Toast.LENGTH_SHORT).show()
                        }
                        showTokenDetailsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Copy Token & Close")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTokenDetailsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Dialog: Delete Broadcast Log Confirmation
    broadcastToDelete?.let { b ->
        AlertDialog(
            onDismissRequest = { broadcastToDelete = null },
            title = { Text("Delete Broadcast Log?", fontWeight = FontWeight.Bold) },
            text = { Text("Remove \"${b.title}\" from Firestore 'PushBroadcasts' history?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBroadcastLog(b.id)
                        broadcastToDelete = null
                        Toast.makeText(context, "Broadcast log deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { broadcastToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
