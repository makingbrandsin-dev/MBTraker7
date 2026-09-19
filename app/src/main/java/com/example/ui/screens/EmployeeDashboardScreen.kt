package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AutoBrochureConfigEntity
import com.example.data.model.CallRecordingEntity
import com.example.data.model.SocialReviewConfigEntity
import com.example.ui.components.AppHeader
import com.example.ui.components.DailyTaskManagementSection
import com.example.ui.components.StatusTag
import com.example.ui.components.formatLiveSeconds
import com.example.ui.components.WhatsAppQuickChatDialog
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun EmployeeDashboardScreen(
    viewModel: MainViewModel,
    onNavigateToAttendance: () -> Unit,
    onNavigateToProjects: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToLeads: () -> Unit,
    onNavigateToCalls: () -> Unit,
    onNavigateToHolidays: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToManager: () -> Unit = {},
    onNavigateToProfile: (() -> Unit)? = null,
    onNavigateToInvoices: () -> Unit = {},
    onNavigateToChat: () -> Unit = {}
) {
    val attendance by viewModel.latestAttendance.collectAsState()
    val liveSeconds by viewModel.liveActiveDurationSeconds.collectAsState()
    val pendingTasks by viewModel.pendingTaskCount.collectAsState()
    val completedTasks by viewModel.completedTaskCount.collectAsState()
    val employeeName by viewModel.currentEmployeeName.collectAsState()
    val employeeRole by viewModel.currentEmployeeRole.collectAsState()
    val unreadNotifications by viewModel.unreadNotificationCount.collectAsState()
    val showFirstTimeDialog by viewModel.showFirstTimeCheckInDialog.collectAsState()
    val firebaseSyncStatus by viewModel.firebaseSyncStatus.collectAsState()
    val isFirebaseConnected by viewModel.isFirebaseConnected.collectAsState()

    // Break & Chat State
    val isOnBreak by viewModel.isOnBreak.collectAsState()
    val currentBreakType by viewModel.currentBreakType.collectAsState()
    val liveBreakSeconds by viewModel.liveBreakDurationSeconds.collectAsState()
    val isQuickChatOpen by viewModel.isQuickChatOpen.collectAsState()
    val unreadChatCount by viewModel.unreadChatCount.collectAsState()
    var showBreakOptionsDialog by remember { mutableStateOf(false) }

    // Quick Action Dialog States
    var showBrochureDialog by remember { mutableStateOf(false) }
    var showReviewsQrDialog by remember { mutableStateOf(false) }
    var showCallRecorderDialog by remember { mutableStateOf(false) }

    val isWorking = attendance?.isWorking ?: false
    val snackbarMsg by viewModel.attendanceSnackbarMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentDeviceTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentDeviceTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000L)
        }
    }

    val deviceTimeString = remember(currentDeviceTime) {
        java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(java.util.Date(currentDeviceTime))
    }
    val deviceDateString = remember(currentDeviceTime) {
        java.text.SimpleDateFormat("EEEE, dd MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(currentDeviceTime))
    }
    val greetingText = remember(currentDeviceTime) {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = currentDeviceTime
        when (cal.get(java.util.Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good Morning,"
            in 12..16 -> "Good Afternoon,"
            else -> "Good Evening,"
        }
    }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearAttendanceSnackbarMessage()
        }
    }

    // First-time check in popup asking for Name & Role
    if (showFirstTimeDialog) {
        FirstTimeCheckInDialog(
            initialName = employeeName,
            initialRole = employeeRole,
            onDismiss = { viewModel.dismissFirstTimeDialog() },
            onConfirm = { name, role ->
                viewModel.completeFirstTimeCheckIn(name, role)
            }
        )
    }

    // ☕ Break Selection Dialog
    if (showBreakOptionsDialog) {
        BreakOptionsDialog(
            onDismiss = { showBreakOptionsDialog = false },
            onSelectBreak = { breakType ->
                viewModel.startBreak(breakType)
                showBreakOptionsDialog = false
            }
        )
    }

    // 💬 WhatsApp Style Quick Chat Popup
    if (isQuickChatOpen) {
        WhatsAppQuickChatDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.closeQuickChat() }
        )
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "MB Traker",
                showBrandLogo = true,
                onOpenChat = onNavigateToChat,
                unreadChatCount = unreadChatCount,
                onNavigateToNotifications = onNavigateToNotifications,
                unreadNotificationCount = unreadNotifications,
                onNavigateToProfile = onNavigateToProfile
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SurfaceBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Greeting & Live Work Badge
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(greetingText, fontSize = 13.sp, color = TextSecondary)
                    Text(employeeName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(employeeRole, fontSize = 12.sp, color = TextMuted)
                }

                StatusTag(
                    text = if (isWorking) "Working" else "Off-Clock",
                    isGreen = isWorking
                )
            }
        }

        // Inline Team Live Chat Feed & Instant Messenger (Replaced Time Display Below Name)
        item {
            InlineLiveTeamChatCard(
                viewModel = viewModel,
                onNavigateToChat = onNavigateToChat
            )
        }

        // Connected Realtime Attendance Card (Rich Blue Background & Gold Accents)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A)), // Executive Blue Background
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAttendance() }
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isWorking) Color(0xFF4ADE80) else Color(0xFFF59E0B),
                                    modifier = Modifier.size(8.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Today's Attendance", color = Color(0xFF93C5FD), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (attendance != null && attendance?.checkInTime != null) attendance!!.checkInTime!! else "--:--",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                if (isWorking) "Checked In" else "Ready to Check In",
                                color = if (isWorking) Color(0xFF4ADE80) else Color(0xFFFBBF24),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        // ☕ Break Options in place of Expected Out
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Break Management", color = Color(0xFF93C5FD), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))

                            if (isOnBreak) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF59E0B),
                                    modifier = Modifier.clickable { viewModel.resumeFromBreak() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Coffee,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = "On $currentBreakType",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "${liveBreakSeconds / 60}m ${liveBreakSeconds % 60}s · Tap to Resume",
                                                color = Color.White.copy(alpha = 0.9f),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isWorking) Color(0xFF1E40AF) else Color.White.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF60A5FA).copy(alpha = 0.5f)),
                                    modifier = Modifier.clickable(enabled = isWorking) {
                                        showBreakOptionsDialog = true
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.FreeBreakfast,
                                            contentDescription = "Break Options",
                                            tint = if (isWorking) Color(0xFF93C5FD) else Color(0xFF94A3B8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = if (isWorking) "☕ Take Break" else "No Active Shift",
                                                color = if (isWorking) Color.White else Color(0xFFCBD5E1),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = if (isWorking) "Tea / Lunch / Custom" else "Check in first",
                                                color = Color(0xFF93C5FD),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color(0xFF3B82F6).copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Apartment,
                                    contentDescription = null,
                                    tint = Color(0xFF93C5FD),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Today's Time in Office", color = Color(0xFF93C5FD), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isWorking) formatLiveSeconds(liveSeconds) else if ((attendance?.durationMinutes ?: 0) > 0) "${(attendance?.durationMinutes ?: 0) / 60}h ${(attendance?.durationMinutes ?: 0) % 60}m" else "00:00:00",
                                color = Color(0xFFFDE047), // Rich Yellow Gold Accent
                                fontWeight = FontWeight.Black,
                                fontSize = 26.sp
                            )
                            Text(
                                text = if (isWorking) "${liveSeconds / 3600}h ${(liveSeconds % 3600) / 60}m logged today" else "Total office hours logged",
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp
                            )
                        }

                        if (isWorking) {
                            Button(
                                onClick = { viewModel.checkOutUser() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEF4444),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("check_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Check Out",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                        } else {
                            Button(
                                onClick = { viewModel.checkInUser() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF59E0B), // Vibrant Gold
                                    contentColor = Color(0xFF0F172A)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("check_in_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Login,
                                    contentDescription = null,
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Check In",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }

                    // Live Sync Pill Inside Card
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isFirebaseConnected) Color(0xFF10B981) else Color(0xFFF59E0B))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        firebaseSyncStatus,
                                        fontSize = 11.sp,
                                        color = Color(0xFF0F172A),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    "Live Sync",
                                    fontSize = 10.sp,
                                    color = Color(0xFF059669),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Actions Section (Full 8-Action Suite)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, BorderLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Quick Actions & Tools",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEFF6FF)
                        ) {
                            Text(
                                "8 Tools",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Row 1: Core Operations
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QuickActionItem(
                            drawableRes = com.example.R.drawable.ic_quick_lead,
                            label = "Leads",
                            bgColor = Color(0xFFF0F9FF),
                            onClick = onNavigateToLeads
                        )
                        QuickActionItem(
                            drawableRes = com.example.R.drawable.ic_quick_task,
                            label = "Tasks",
                            bgColor = Color(0xFFFEF3C7),
                            onClick = onNavigateToTasks
                        )
                        QuickActionItem(
                            drawableRes = com.example.R.drawable.ic_quick_call,
                            label = "Calls",
                            bgColor = Color(0xFFEFF6FF),
                            onClick = onNavigateToCalls
                        )
                        QuickActionItem(
                            drawableRes = com.example.R.drawable.ic_quick_leave,
                            label = "Leave",
                            bgColor = Color(0xFFFEF2F2),
                            onClick = onNavigateToHolidays
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Row 2: Invoices, Brochure, Reviews QR & Voice Recorder
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QuickActionItem(
                            drawableRes = com.example.R.drawable.ic_quick_invoice,
                            label = "Invoices",
                            bgColor = Color(0xFFF5F3FF),
                            onClick = { onNavigateToInvoices() }
                        )
                        QuickActionItem(
                            drawableRes = com.example.R.drawable.ic_quick_brochure,
                            label = "Brochure",
                            bgColor = Color(0xFFF0FDF4),
                            onClick = { showBrochureDialog = true }
                        )
                        QuickActionItem(
                            drawableRes = com.example.R.drawable.ic_quick_qrcode,
                            label = "Review QR",
                            bgColor = Color(0xFFFFF7ED),
                            onClick = { showReviewsQrDialog = true }
                        )
                        QuickActionItem(
                            drawableRes = com.example.R.drawable.ic_quick_recorder,
                            label = "Recorder",
                            bgColor = Color(0xFFFDF2F8),
                            onClick = { showCallRecorderDialog = true }
                        )
                    }
                }
            }
        }

        // Daily Task Management Component for Homepage
        item {
            DailyTaskManagementSection(
                viewModel = viewModel,
                onNavigateToTasks = onNavigateToTasks
            )
        }

        // Active Projects Card preview
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToProjects() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Active Projects", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        Text("Website Revamp · 82% Complete", color = TextSecondary, fontSize = 13.sp)
                    }

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "View Projects",
                        tint = TextMuted
                    )
                }
            }
        }
    }
}

    // 📄 Company Profile PDF Auto-Send & Preview Dialog
    if (showBrochureDialog) {
        CompanyProfileBrochureDialog(
            viewModel = viewModel,
            onDismiss = { showBrochureDialog = false }
        )
    }

    // 📱 Social Media Reviews & QR Code Hub Dialog
    if (showReviewsQrDialog) {
        SocialReviewsAndQrDialog(
            viewModel = viewModel,
            onDismiss = { showReviewsQrDialog = false }
        )
    }

    // 🎙️ Call Audio Recording & Voice Notes Dialog
    if (showCallRecorderDialog) {
        CallRecorderAudioDialog(
            viewModel = viewModel,
            onDismiss = { showCallRecorderDialog = false }
        )
    }
}

@Composable
fun QuickActionItem(
    drawableRes: Int? = null,
    icon: ImageVector? = null,
    label: String,
    bgColor: Color,
    tintColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = bgColor,
            modifier = Modifier.size(58.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (drawableRes != null) {
                    Image(
                        painter = painterResource(id = drawableRes),
                        contentDescription = label,
                        modifier = Modifier.size(34.dp)
                    )
                } else if (icon != null) {
                    Icon(icon, contentDescription = label, tint = tintColor, modifier = Modifier.size(26.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

@Composable
fun FirstTimeCheckInDialog(
    initialName: String,
    initialRole: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, role: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var role by remember { mutableStateOf(initialRole) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = ElectricBlueBg,
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Badge,
                            contentDescription = "Employee Check-In",
                            tint = ElectricBlue,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Welcome to MB Traker!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Please enter your details to start today's attendance check-in.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Employee Name") },
                    placeholder = { Text("e.g. Rahul Sharma") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = ElectricBlue)
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp),
                    colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Role / Designation") },
                    placeholder = { Text("e.g. Senior Developer") },
                    leadingIcon = {
                        Icon(Icons.Default.Work, contentDescription = null, tint = ElectricBlue)
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp),
                    colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        onConfirm(name, role)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBlue,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Login, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Check In Now",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Skip for Now", color = TextSecondary, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun BreakOptionsDialog(
    onDismiss: () -> Unit,
    onSelectBreak: (String) -> Unit
) {
    var customBreakName by remember { mutableStateOf("") }
    var isCustomSelected by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFEF3C7),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.FreeBreakfast,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Take a Break",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextPrimary
                            )
                            Text(
                                "Pause work timer & log downtime",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                val presetBreaks = listOf(
                    Triple("Tea Break", "15 Mins · Quick refresh", Icons.Default.Coffee),
                    Triple("Lunch Break", "45 Mins · Midday meal", Icons.Default.Restaurant),
                    Triple("Short Break", "10 Mins · Walk & stretch", Icons.Default.DirectionsWalk),
                    Triple("Official Meeting Break", "30 Mins · Standup / Review", Icons.Default.Groups)
                )

                presetBreaks.forEach { (name, desc, icon) ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelectBreak(name) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                    Text(desc, fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (!isCustomSelected) {
                    OutlinedButton(
                        onClick = { isCustomSelected = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Custom Reason Break", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    OutlinedTextField(
                        value = customBreakName,
                        onValueChange = { customBreakName = it },
                        label = { Text("Custom Break Reason") },
                        placeholder = { Text("e.g. Doctor appointment, Client commute") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (customBreakName.isNotBlank()) {
                                onSelectBreak(customBreakName.trim())
                            }
                        },
                        enabled = customBreakName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                    ) {
                        Text("Start Custom Break", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. Company Profile PDF Brochure Dialog
// ==========================================
@Composable
fun CompanyProfileBrochureDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val rawBrochureConfig by viewModel.autoBrochureConfig.collectAsState()
    val brochureConfig = rawBrochureConfig ?: AutoBrochureConfigEntity()
    var isAutoSend by remember(brochureConfig) { mutableStateOf(brochureConfig.isAutoSendEnabled) }
    var sendWhatsApp by remember(brochureConfig) { mutableStateOf(brochureConfig.sendViaWhatsApp) }
    var sendEmail by remember(brochureConfig) { mutableStateOf(brochureConfig.sendViaEmail) }
    var showPreviewSheet by remember { mutableStateOf(false) }
    var testRecipientName by remember { mutableStateOf("") }
    var testRecipientPhone by remember { mutableStateOf("") }
    var isSendingTest by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF0FDF4),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(id = com.example.R.drawable.ic_quick_brochure),
                                    contentDescription = null,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Company Profile PDF",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextPrimary
                            )
                            Text(
                                "Automated brochure delivery on lead capture",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // PDF Document Card Preview
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFDC2626),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                "PDF",
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            brochureConfig.brochureFileName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "${brochureConfig.brochureFileSize} · 24 Pages · Verified 2026 Edition",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showPreviewSheet = !showPreviewSheet },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        if (showPreviewSheet) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (showPreviewSheet) "Hide View" else "Preview PDF", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        viewModel.testSendBrochureManual("Client Prospect", "+91 98765 43210", "prospect@example.com")
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Send Now", fontSize = 12.sp, color = Color.White)
                                }
                            }

                            if (showPreviewSheet) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            "MAKING BRANDS — CORPORATE PROFILE",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp,
                                            color = BrandBlue
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            "• End-to-End Digital Transformation & Mobile App Engineering\n• Enterprise CRM & Lead Generation Pipeline\n• Real-Time Cloud Infrastructure & Brand Advisory",
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Automation Settings
                    Text("Auto-Dispatch Settings", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, BorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Auto Send on Lead Capture", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                    Text("Sends company profile immediately when a new lead is added", fontSize = 11.sp, color = TextSecondary)
                                }
                                Switch(
                                    checked = isAutoSend,
                                    onCheckedChange = {
                                        isAutoSend = it
                                        viewModel.updateAutoBrochureConfig(brochureConfig.copy(isAutoSendEnabled = it))
                                    }
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = BorderLight)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Send via WhatsApp", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                                Switch(
                                    checked = sendWhatsApp,
                                    onCheckedChange = {
                                        sendWhatsApp = it
                                        viewModel.updateAutoBrochureConfig(brochureConfig.copy(sendViaWhatsApp = it))
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Send via Email", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                                Switch(
                                    checked = sendEmail,
                                    onCheckedChange = {
                                        sendEmail = it
                                        viewModel.updateAutoBrochureConfig(brochureConfig.copy(sendViaEmail = it))
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stats Summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Total Delivered", fontSize = 11.sp, color = Color(0xFF166534))
                                Text("${brochureConfig.totalSentCount}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Last Dispatched", fontSize = 11.sp, color = BrandBlue)
                                Text(brochureConfig.lastSentTimestamp, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Save & Close", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// 2. Social Media Reviews & QR Code Hub Dialog
// ==========================================
@Composable
fun SocialReviewsAndQrDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val reviewConfigs by viewModel.socialReviews.collectAsState()
    var selectedIndex by remember { mutableStateOf(0) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var copiedMessage by remember { mutableStateOf<String?>(null) }

    val activeReview = reviewConfigs.getOrNull(selectedIndex) ?: SocialReviewConfigEntity(
        platformId = "google",
        platformName = "Google Reviews",
        reviewUrl = "https://g.page/r/makingbrands/review",
        qrCodePayload = "https://g.page/r/makingbrands/review",
        ratingText = "4.9 ★",
        totalReviewsCount = 184,
        isPrimary = true
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFFF7ED),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(id = com.example.R.drawable.ic_quick_qrcode),
                                    contentDescription = null,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Social Review & QR Hub",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextPrimary
                            )
                            Text(
                                "Send instant review links & QR codes to clients",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Platform Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(reviewConfigs.indices.toList()) { index ->
                        val item = reviewConfigs[index]
                        val isSelected = index == selectedIndex
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) BrandBlue else Color(0xFFF1F5F9),
                            modifier = Modifier.clickable { selectedIndex = index }
                        ) {
                            Text(
                                item.platformName,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextSecondary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // QR Code Card
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                activeReview.platformName.uppercase(),
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = BrandBlue,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${activeReview.ratingText} (${activeReview.totalReviewsCount} Client Reviews)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFD97706)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Simulated high-fidelity QR Pattern
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(2.dp, Color(0xFF0F172A)),
                                modifier = Modifier.size(170.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        repeat(6) { row ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                repeat(6) { col ->
                                                    val isDark = ((row + col * 3) % 2 == 0) || (row in 0..1 && col in 0..1) || (row in 0..1 && col in 4..5) || (row in 4..5 && col in 0..1)
                                                    Box(
                                                        modifier = Modifier
                                                            .size(18.dp)
                                                            .background(
                                                                if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                                                                shape = RoundedCornerShape(3.dp)
                                                            )
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Center Brand Logo Pill
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = BrandBlue,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("MB", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                "Scan with any phone camera to leave a 5★ review",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Review URL Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                activeReview.reviewUrl,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(activeReview.reviewUrl))
                                    copiedMessage = "Review link copied!"
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = BrandBlue, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    if (copiedMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            copiedMessage ?: "",
                            fontSize = 11.sp,
                            color = Color(0xFF059669),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, "${activeReview.customInviteText}\n\nReview Link: ${activeReview.reviewUrl}")
                                }
                                context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Review Link"))
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share via WhatsApp", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// 3. Call Audio Recording & Voice Notes Dialog
// ==========================================
@Composable
fun CallRecorderAudioDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val recordings by viewModel.callRecordings.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") } // "All", "SIM Call", "WhatsApp Call"
    var playingRecordingId by remember { mutableStateOf<Long?>(null) }
    var isSimulatingRecording by remember { mutableStateOf(false) }

    val filteredList = remember(recordings, selectedFilter) {
        when (selectedFilter) {
            "SIM Call" -> recordings.filter { it.callMedium == "SIM Call" }
            "WhatsApp Call" -> recordings.filter { it.callMedium == "WhatsApp Call" }
            else -> recordings
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFDF2F8),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(id = com.example.R.drawable.ic_quick_recorder),
                                    contentDescription = null,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Call Audio Recorder",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextPrimary
                            )
                            Text(
                                "SIM & WhatsApp in-call audio recordings",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "SIM Call", "WhatsApp Call").forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) BrandBlue else Color(0xFFF1F5F9),
                            modifier = Modifier.clickable { selectedFilter = filter }
                        ) {
                            Text(
                                filter,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // List of Recordings
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.MicOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No recordings found in this category", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredList) { rec ->
                            val isPlaying = playingRecordingId == rec.id
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isPlaying) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isPlaying) BrandBlue else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    playingRecordingId = if (isPlaying) null else rec.id
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = if (isPlaying) BrandBlue else Color(0xFFE2E8F0),
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                            contentDescription = "Play",
                                                            tint = if (isPlaying) Color.White else TextPrimary,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column {
                                                Text(rec.contactName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                                Text("${rec.phoneNumber} · ${rec.callMedium}", fontSize = 11.sp, color = TextSecondary)
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(rec.durationText, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrandBlue)
                                            Text(rec.recordedAt, fontSize = 10.sp, color = TextMuted)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Waveform & Transcript
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        repeat(16) { barIndex ->
                                            val barHeight = ((barIndex * 7 + 15) % 22 + 6).dp
                                            Box(
                                                modifier = Modifier
                                                    .width(4.dp)
                                                    .height(if (isPlaying) barHeight else 10.dp)
                                                    .background(
                                                        if (isPlaying) BrandBlue else Color(0xFFCBD5E1),
                                                        shape = RoundedCornerShape(2.dp)
                                                    )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        "“${rec.transcriptionSnippet}”",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // New Record Simulation Button
                Button(
                    onClick = {
                        viewModel.addCallRecording(
                            contactName = "New Client Interaction",
                            phoneNumber = "+91 91234 56789",
                            callMedium = if (selectedFilter == "WhatsApp Call") "WhatsApp Call" else "SIM Call",
                            callType = "Outgoing",
                            durationText = "03:12",
                            durationSeconds = 192,
                            transcriptionSnippet = "Project quotation review and deliverable milestone alignment finalized."
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Record & Save New Call Audio", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * ⚡ Live Inline Team Chat & Messenger Card
 * Displayed directly below the employee name in place of the time display.
 * Shows only the present chat message in a space-efficient view with horizontal slide controls to browse previous messages.
 */
@Composable
fun InlineLiveTeamChatCard(
    viewModel: MainViewModel,
    onNavigateToChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chatMessages by viewModel.chatMessages.collectAsState()
    val currentChannel by viewModel.currentChannel.collectAsState()
    val unreadCount by viewModel.unreadChatCount.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var inputMessage by remember { mutableStateOf("") }

    val channelList = listOf(
        "dev-team" to "Dev Team",
        "sales-hq" to "Sales & CRM",
        "general-crm" to "HQ General",
        "announcements" to "Notices"
    )

    // Filter messages for currently selected channel
    val channelMessages = remember(chatMessages, currentChannel) {
        val list = chatMessages.filter { it.channelId == currentChannel }
        if (list.isEmpty()) {
            chatMessages.filter { it.channelId.replace("-", "_") == currentChannel.replace("-", "_") }
        } else {
            list
        }
    }

    val pageCount = if (channelMessages.isEmpty()) 1 else channelMessages.size
    val pagerState = rememberPagerState(
        initialPage = if (channelMessages.isNotEmpty()) channelMessages.size - 1 else 0,
        pageCount = { pageCount }
    )

    // Auto slide to the newest present message when a new message is received or sent
    LaunchedEffect(channelMessages.size) {
        if (channelMessages.isNotEmpty()) {
            pagerState.animateScrollToPage(channelMessages.size - 1)
        }
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Live Feed Badge + Channel Tag + Direct Team Chat Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF25D366),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Chat,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Team Live Chat",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Live pulse indicator
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Live",
                                fontSize = 9.sp,
                                color = Color(0xFF16A34A),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Instant dynamic team feed",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Leads directly to Team Chat Screen
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEFF6FF),
                    modifier = Modifier.clickable { onNavigateToChat() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Team Chat",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = "Open Team Chat",
                            tint = BrandBlue,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Channel Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(channelList) { (chId, label) ->
                    val isSelected = currentChannel.replace("-", "_") == chId.replace("-", "_")
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) BrandDarkBlue else Color(0xFFF1F5F9),
                        modifier = Modifier.clickable { viewModel.selectChatChannel(chId) }
                    ) {
                        Text(
                            text = "# $label",
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Slide Control Bar & Single Present Chat Message Box (Space-Saving Layout)
            if (channelMessages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No messages yet. Send a quick update below!",
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            } else {
                val isAtLatest = pagerState.currentPage == channelMessages.size - 1

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    // Slide header & controls row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isAtLatest) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFDCFCE7)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF16A34A))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Present Chat",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D)
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFEF3C7)
                                ) {
                                    Text(
                                        text = "◄ Past (${pagerState.currentPage + 1}/${channelMessages.size})",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Jump to Present ▶",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue,
                                    modifier = Modifier.clickable {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(channelMessages.size - 1)
                                        }
                                    }
                                )
                            }
                        }

                        // Slide Arrow Buttons & Slide indicator
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Slide ◄ ►",
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            // Prev Slide Arrow
                            Surface(
                                shape = CircleShape,
                                color = if (pagerState.currentPage > 0) Color(0xFFE2E8F0) else Color(0xFFF1F5F9),
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable(enabled = pagerState.currentPage > 0) {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.ChevronLeft,
                                        contentDescription = "Previous Message",
                                        tint = if (pagerState.currentPage > 0) TextPrimary else TextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            // Next Slide Arrow
                            Surface(
                                shape = CircleShape,
                                color = if (pagerState.currentPage < channelMessages.size - 1) Color(0xFFE2E8F0) else Color(0xFFF1F5F9),
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable(enabled = pagerState.currentPage < channelMessages.size - 1) {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "Next Message",
                                        tint = if (pagerState.currentPage < channelMessages.size - 1) TextPrimary else TextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Horizontal Pager: Shows exactly ONE message at a time, allowing user to swipe/slide left to see previous messages
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        if (page in channelMessages.indices) {
                            val msg = channelMessages[page]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (msg.isMe) Arrangement.End else Arrangement.Start,
                                verticalAlignment = Alignment.Top
                            ) {
                                if (!msg.isMe) {
                                    Surface(
                                        shape = CircleShape,
                                        color = when (msg.senderName.take(1)) {
                                            "A" -> Color(0xFF8B5CF6)
                                            "P" -> Color(0xFFEC4899)
                                            "V" -> Color(0xFF06B6D4)
                                            else -> Color(0xFF3B82F6)
                                        },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = msg.senderName.take(1),
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                Column(horizontalAlignment = if (msg.isMe) Alignment.End else Alignment.Start) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (msg.isMe) "You" else msg.senderName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (msg.isMe) BrandBlue else TextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "· ${msg.senderRole}",
                                            fontSize = 9.sp,
                                            color = TextSecondary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = msg.timestampText,
                                            fontSize = 9.sp,
                                            color = TextMuted
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Surface(
                                        shape = RoundedCornerShape(
                                            topStart = 8.dp,
                                            topEnd = 8.dp,
                                            bottomStart = if (msg.isMe) 8.dp else 2.dp,
                                            bottomEnd = if (msg.isMe) 2.dp else 8.dp
                                        ),
                                        color = if (msg.isMe) Color(0xFFDCF8C6) else Color.White,
                                        shadowElevation = 1.dp
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                                            Text(
                                                text = msg.messageText,
                                                fontSize = 12.sp,
                                                color = TextPrimary
                                            )
                                            if (!msg.attachmentFileName.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.AttachFile,
                                                        contentDescription = null,
                                                        tint = BrandBlue,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(
                                                        text = msg.attachmentFileName,
                                                        fontSize = 10.sp,
                                                        color = BrandBlue,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick 1-Tap Reaction / Action Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val quickReplies = listOf("👍 Ack", "🚀 On it", "✅ Done", "☕ On Break", "📍 In Meeting")
                items(quickReplies) { reply ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.clickable {
                            viewModel.sendChatMessage(reply)
                        }
                    ) {
                        Text(
                            text = reply,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Instant Inline Message Input Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = inputMessage,
                    onValueChange = { inputMessage = it },
                    placeholder = { Text("Type instant reply...", fontSize = 11.sp, color = TextMuted) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    singleLine = true
                )

                Surface(
                    shape = CircleShape,
                    color = if (inputMessage.isNotBlank()) Color(0xFF25D366) else Color(0xFFCBD5E1),
                    modifier = Modifier
                        .size(38.dp)
                        .clickable(enabled = inputMessage.isNotBlank()) {
                            viewModel.sendChatMessage(inputMessage.trim())
                            inputMessage = ""
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send Message",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}


