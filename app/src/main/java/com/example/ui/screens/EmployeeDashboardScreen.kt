package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.testTag
import com.example.ui.components.StatusTag
import com.example.ui.components.formatLiveSeconds
import com.example.ui.theme.*

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
    onNavigateToProfile: (() -> Unit)? = null
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

    val isWorking = attendance?.isWorking ?: false
    val snackbarMsg by viewModel.attendanceSnackbarMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SurfaceBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // App Top Bar
        item {
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = BrandBlue,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("MB", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("MB Traker", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = BrandDarkBlue)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateToNotifications) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotifications > 0) {
                                        Badge(
                                            containerColor = StatusRed,
                                            contentColor = Color.White
                                        ) {
                                            Text(if (unreadNotifications > 99) "99+" else "$unreadNotifications")
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = TextPrimary)
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { onNavigateToManager() }
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF1E1B4B),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.AdminPanelSettings,
                                        contentDescription = "MB Admin Portal",
                                        tint = AccentGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { onNavigateToProfile?.invoke() }
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = BrandBlue.copy(alpha = 0.12f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = androidx.compose.ui.res.painterResource(com.example.R.drawable.ic_nav_profile),
                                        contentDescription = "Profile",
                                        tint = BrandBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Greeting & Live Work Badge
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Good Morning,", fontSize = 13.sp, color = TextSecondary)
                    Text(employeeName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(employeeRole, fontSize = 12.sp, color = TextMuted)
                }

                StatusTag(
                    text = if (isWorking) "Working" else "Off-Clock",
                    isGreen = isWorking
                )
            }
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
                                    color = Color(0xFFF59E0B),
                                    modifier = Modifier.size(8.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Today's Attendance", color = Color(0xFF93C5FD), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (isWorking) (attendance?.checkInTime ?: "09:00 AM") else "--:--",
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

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Expected Out", color = Color(0xFF93C5FD), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("06:00 PM", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Standard 8h Shift", color = Color(0xFFCBD5E1), fontSize = 11.sp)
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
                            Text("Working Time (Realtime)", color = Color(0xFF93C5FD), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isWorking) formatLiveSeconds(liveSeconds) else "00:00:00",
                                color = Color(0xFFFDE047), // Rich Yellow Gold Accent
                                fontWeight = FontWeight.Black,
                                fontSize = 26.sp
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

                            if (attendance != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Room DB: Entry #${attendance?.id ?: 1}",
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "Epoch: ${attendance?.timestamp ?: System.currentTimeMillis()}",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Actions Row
        item {
            Text("Quick Actions", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickActionItem(
                    drawableRes = com.example.R.drawable.ic_quick_lead,
                    label = "Lead",
                    bgColor = Color(0xFFF0F9FF),
                    onClick = onNavigateToLeads
                )
                QuickActionItem(
                    drawableRes = com.example.R.drawable.ic_quick_task,
                    label = "Task",
                    bgColor = Color(0xFFFEF3C7),
                    onClick = onNavigateToTasks
                )
                QuickActionItem(
                    drawableRes = com.example.R.drawable.ic_quick_call,
                    label = "Call",
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
        }

        // Today's Tasks Summary Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToTasks() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Today's Tasks", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "$pendingTasks Pending",
                                color = StatusOrange,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text("  ·  ", color = TextMuted)
                            Text(
                                "$completedTasks Completed",
                                color = StatusGreen,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "View Tasks",
                        tint = TextMuted
                    )
                }
            }
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
