package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.data.model.NotificationEntity
import com.example.ui.components.AppHeader
import com.example.ui.components.MetricBadge
import com.example.ui.theme.*

// ---------------- Screen 15: Profile Screen ----------------
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    var showEditProfileDialog by remember { mutableStateOf(false) }
    val empName by viewModel.currentEmployeeName.collectAsState()
    val empRole by viewModel.currentEmployeeRole.collectAsState()

    if (showEditProfileDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showEditProfileDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                var editName by remember { mutableStateOf(empName) }
                var editRole by remember { mutableStateOf(empRole) }

                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Persists directly to Room database", fontSize = 12.sp, color = ElectricBlue, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Employee Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ElectricBlue) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp),
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editRole,
                        onValueChange = { editRole = it },
                        label = { Text("Role / Designation") },
                        leadingIcon = { Icon(Icons.Default.Work, contentDescription = null, tint = ElectricBlue) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp),
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showEditProfileDialog = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.updateEmployeeProfile(editName, editRole)
                                showEditProfileDialog = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricBlue,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Save Changes", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Profile",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showEditProfileDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = TextPrimary)
                    }
                }
            )
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Surface(
                    shape = CircleShape,
                    color = BrandBlue.copy(alpha = 0.15f),
                    modifier = Modifier.size(90.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(50.dp))
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(empName, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = TextPrimary)
                Text(empRole, fontSize = 13.sp, color = TextSecondary)
            }

            // Summary Pills (3 Projects, 27 Tasks, 184 Completed)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricBadge(
                        label = "Projects",
                        value = "3",
                        backgroundColor = Color(0xFFE0F2FE),
                        textColor = Color(0xFF0369A1),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadge(
                        label = "Tasks",
                        value = "27",
                        backgroundColor = Color(0xFFFEF3C7),
                        textColor = Color(0xFFB45309),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadge(
                        label = "Completed",
                        value = "184",
                        backgroundColor = Color(0xFFDCFCE7),
                        textColor = Color(0xFF15803D),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Settings Navigation Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        ProfileNavRow(Icons.Default.Person, "My Profile", onClick = { showEditProfileDialog = true })
                        Divider(color = BorderLight)
                        ProfileNavRow(Icons.Default.Settings, "Settings")
                        Divider(color = BorderLight)
                        ProfileNavRow(Icons.Default.HelpOutline, "Help & Support")
                        Divider(color = BorderLight)
                        ProfileNavRow(Icons.Default.Logout, "Logout", textColor = StatusRed, iconTint = StatusRed, onClick = onLogout)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileNavRow(
    icon: ImageVector,
    label: String,
    textColor: Color = TextPrimary,
    iconTint: Color = BrandBlue,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
    }
}

// ---------------- Screen 16: Notifications Screen ----------------
@Composable
fun NotificationsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val notifications by viewModel.notifications.collectAsState()

    Scaffold(
        topBar = {
            AppHeader(
                title = "Notifications",
                onBack = onBack,
                actions = {
                    if (notifications.isNotEmpty()) {
                        IconButton(onClick = { viewModel.markAllNotificationsAsRead() }) {
                            Icon(Icons.Default.DoneAll, contentDescription = "Mark all read", tint = ElectricBlue)
                        }
                        IconButton(onClick = { viewModel.clearAllNotifications() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear all", tint = StatusRed)
                        }
                    }
                }
            )
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = ElectricBlueBg,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No notifications yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "You're all caught up!",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notifications) { item ->
                    NotificationCardItem(
                        item = item,
                        onClick = { viewModel.markNotificationAsRead(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationCardItem(
    item: NotificationEntity,
    onClick: () -> Unit
) {
    val (icon, bgColor, tintColor) = when (item.category) {
        "followup" -> Triple(Icons.Default.PhoneCallback, Color(0xFFFEE2E2), Color(0xFFDC2626))
        "task" -> Triple(Icons.Default.Task, Color(0xFFE0E7FF), Color(0xFF4F46E5))
        "message" -> Triple(Icons.Default.Chat, Color(0xFFE0F2FE), Color(0xFF0284C7))
        "attendance" -> Triple(Icons.Default.Timer, Color(0xFFDCFCE7), Color(0xFF15803D))
        "leave" -> Triple(Icons.Default.DateRange, Color(0xFFFEF3C7), Color(0xFFB45309))
        else -> Triple(Icons.Default.Folder, Color(0xFFEDE9FE), Color(0xFF7C3AED))
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!item.isRead) ElectricBlueBg.copy(alpha = 0.5f) else Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = bgColor,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = tintColor, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.title,
                        fontWeight = if (!item.isRead) FontWeight.ExtraBold else FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (!item.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ElectricBlue)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(item.timeAgo, fontSize = 11.sp, color = TextMuted)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(item.subtitle, fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

// ---------------- Screen 17: Holidays & Leave Screen ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolidaysLeaveScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Calendar & Apply", "My Leaves (${viewModel.leaves.collectAsState().value.size})", "Holidays")

    val empName by viewModel.currentEmployeeName.collectAsState()
    val leaves by viewModel.leaves.collectAsState()

    val currentNow = remember { Calendar.getInstance() }
    var displayedCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
        })
    }
    var selectedDay by remember {
        mutableIntStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
    }
    var showApplyLeaveModal by remember { mutableStateOf(false) }
    var applicationSuccessMsg by remember { mutableStateOf<String?>(null) }

    // Dynamic month/year data
    val monthYearTitle = remember(displayedCalendar) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(displayedCalendar.time)
    }
    val daysInMonth = remember(displayedCalendar) {
        displayedCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    val firstDayOfWeek = remember(displayedCalendar) {
        displayedCalendar.get(Calendar.DAY_OF_WEEK) - 1 // 0 for Sunday
    }
    val totalSlots = remember(daysInMonth, firstDayOfWeek) {
        firstDayOfWeek + daysInMonth
    }
    val selectedDateFormatted = remember(displayedCalendar, selectedDay) {
        val cal = (displayedCalendar.clone() as Calendar).apply {
            val validDay = selectedDay.coerceIn(1, displayedCalendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.DAY_OF_MONTH, validDay)
        }
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.time)
    }

    // State for the leave application form
    var selectedLeaveType by remember { mutableStateOf("Casual Leave (CL)") }
    var leaveReason by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val leaveTypes = listOf("Casual Leave (CL)", "Sick Leave (SL)", "Earned Leave (EL)", "Half Day")

    Scaffold(
        topBar = {
            AppHeader(
                title = "Holidays & Leave",
                onBack = onBack
            )
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tab Selector
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = ElectricBlue
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == index) ElectricBlue else Color(0xFF64748B),
                                    fontSize = 13.sp
                                )
                            }
                        )
                    }
                }
            }

            // Success Confirmation Banner if applied
            if (applicationSuccessMsg != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF34D399)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                applicationSuccessMsg ?: "",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF065F46),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { applicationSuccessMsg = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    // TAB 0: Interactive Calendar & Immediate Apply Action
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                                            shape = RoundedCornerShape(8.dp),
                                            color = ElectricBlueBg,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(monthYearTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                    }
                                    Row {
                                        IconButton(onClick = {
                                            displayedCalendar = (displayedCalendar.clone() as Calendar).apply {
                                                add(Calendar.MONTH, -1)
                                            }
                                        }) { Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = Color(0xFF64748B)) }
                                        IconButton(onClick = {
                                            displayedCalendar = (displayedCalendar.clone() as Calendar).apply {
                                                add(Calendar.MONTH, 1)
                                            }
                                        }) { Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = Color(0xFF64748B)) }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Day Labels
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { day ->
                                        Text(day, fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Days Grid dynamically rendering current month & year
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(7),
                                    modifier = Modifier.height(210.dp)
                                ) {
                                    items(totalSlots) { slot ->
                                        if (slot < firstDayOfWeek) {
                                            Box(modifier = Modifier.padding(3.dp).aspectRatio(1f))
                                        } else {
                                            val dayNum = slot - firstDayOfWeek + 1
                                            val isSelected = dayNum == selectedDay
                                            val isToday = displayedCalendar.get(Calendar.YEAR) == currentNow.get(Calendar.YEAR) &&
                                                    displayedCalendar.get(Calendar.MONTH) == currentNow.get(Calendar.MONTH) &&
                                                    dayNum == currentNow.get(Calendar.DAY_OF_MONTH)
                                            val isSunday = (slot % 7) == 0

                                            Box(
                                                modifier = Modifier
                                                    .padding(3.dp)
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        when {
                                                            isSelected -> ElectricBlue
                                                            isToday -> ElectricBlueBg
                                                            isSunday -> Color(0xFFFEE2E2)
                                                            else -> Color(0xFFF8FAFC)
                                                        }
                                                    )
                                                    .then(
                                                        if (isToday && !isSelected) {
                                                            Modifier.border(1.dp, ElectricBlue, RoundedCornerShape(10.dp))
                                                        } else {
                                                            Modifier
                                                        }
                                                    )
                                                    .clickable {
                                                        selectedDay = dayNum
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        "$dayNum",
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isSelected || isToday || isSunday) FontWeight.Bold else FontWeight.Medium,
                                                        color = when {
                                                            isSelected -> Color.White
                                                            isToday -> ElectricBlue
                                                            isSunday -> StatusRed
                                                            else -> TextPrimary
                                                        }
                                                    )
                                                    if (isToday && !isSelected) {
                                                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(ElectricBlue))
                                                    } else if (isSunday && !isSelected) {
                                                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(StatusRed))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Dynamic Apply Action Card shown upon selecting a date
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, ElectricBlue.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "Selected Date",
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            selectedDateFormatted,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ElectricBlue
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF1F5F9),
                                        modifier = Modifier.padding(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                empName,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF334155)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    "Ready to request leave for this date? Click Apply to submit and record directly in your account database.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    lineHeight = 17.sp
                                )

                                // Direct Apply Button
                                Button(
                                    onClick = { showApplyLeaveModal = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ElectricBlue,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.PostAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Apply for ${if (selectedDay < 10) "0$selectedDay" else "$selectedDay"} Sep 2025",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    // Summary of Leave Balance
                    item {
                        Text("Leave Balance Breakdown", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricBadge("Casual (CL)", "8 Days", Color(0xFFDBEAFE), Color(0xFF1D4ED8), Modifier.weight(1f))
                            MetricBadge("Sick (SL)", "5 Days", Color(0xFFDCFCE7), Color(0xFF15803D), Modifier.weight(1f))
                            MetricBadge("Earned (EL)", "12 Days", Color(0xFFEDE9FE), Color(0xFF6D28D9), Modifier.weight(1f))
                        }
                    }
                }

                1 -> {
                    // TAB 1: My Leaves History (from Room database)
                    if (leaves.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.EventBusy, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("No Leave Applications Found", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF334155))
                                    Text("Select a date from the calendar to submit your first leave application.", fontSize = 13.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center)
                                }
                            }
                        }
                    } else {
                        item {
                            Text("Your Applied Leaves (${leaves.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                            Text("Stored persistently in database under username: $empName", fontSize = 12.sp, color = ElectricBlue)
                        }

                        items(leaves) { leaveItem ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = ElectricBlueBg,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.DateRange, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(leaveItem.startDate, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                                Text("Applicant: ${leaveItem.username}", fontSize = 12.sp, color = Color(0xFF64748B))
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (leaveItem.status == "Approved") Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                                        ) {
                                            Text(
                                                leaveItem.status,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (leaveItem.status == "Approved") Color(0xFF15803D) else Color(0xFFB45309),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFF8FAFC),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("Type: ${leaveItem.leaveType}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ElectricBlue)
                                            if (leaveItem.reason.isNotBlank()) {
                                                Text("Reason: ${leaveItem.reason}", fontSize = 12.sp, color = Color(0xFF475569))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = { viewModel.deleteLeave(leaveItem) }
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Cancel Leave", tint = StatusRed, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Cancel Request", color = StatusRed, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: Upcoming Official Holidays
                    item {
                        Text("Official Holidays (2025)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                HolidayRow("Gandhi Jayanti", "02 Oct 2025", "Thursday")
                                Divider(color = BorderLight)
                                HolidayRow("Dussehra", "12 Oct 2025", "Sunday")
                                Divider(color = BorderLight)
                                HolidayRow("Diwali", "20 Oct 2025", "Monday")
                                Divider(color = BorderLight)
                                HolidayRow("Guru Nanak Jayanti", "05 Nov 2025", "Wednesday")
                                Divider(color = BorderLight)
                                HolidayRow("Christmas", "25 Dec 2025", "Thursday")
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet / Dialog for Applying Leave
    if (showApplyLeaveModal) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showApplyLeaveModal = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with creative blue badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ElectricBlueBg,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.EventNote, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Apply for Leave",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                "Persists under username: $empName",
                                fontSize = 12.sp,
                                color = ElectricBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    IconButton(onClick = { showApplyLeaveModal = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Applicant details badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Applicant Username", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(empName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Date", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(selectedDateFormatted, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ElectricBlue)
                        }
                    }
                }

                // Leave Type Selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Leave Type", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        leaveTypes.take(2).forEach { type ->
                            val isSelected = selectedLeaveType == type
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedLeaveType = type },
                                label = { Text(type, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricBlue,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        leaveTypes.drop(2).forEach { type ->
                            val isSelected = selectedLeaveType == type
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedLeaveType = type },
                                label = { Text(type, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricBlue,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Reason for Leave
                OutlinedTextField(
                    value = leaveReason,
                    onValueChange = { leaveReason = it },
                    label = { Text("Reason for Leave *") },
                    placeholder = { Text("e.g., Medical checkup, family function, personal work") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = ElectricBlue) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp),
                    colors = appTextFieldColors(),
                    minLines = 2,
                    maxLines = 4
                )

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showApplyLeaveModal = false },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }

                    Button(
                        onClick = {
                            val dateStr = selectedDateFormatted
                            val reason = leaveReason.ifBlank { "Personal leave request" }
                            isSubmitting = true
                            viewModel.applyLeave(
                                leaveType = selectedLeaveType,
                                startDate = dateStr,
                                endDate = dateStr,
                                reason = reason
                            )
                            isSubmitting = false
                            showApplyLeaveModal = false
                            applicationSuccessMsg = "Leave application for $dateStr recorded in database for $empName!"
                            selectedTab = 1 // Switch to My Leaves tab so user sees their new entry immediately!
                        },
                        modifier = Modifier.weight(1.5f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Submit & Store", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HolidayRow(title: String, date: String, day: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
            Text(day, fontSize = 12.sp, color = TextSecondary)
        }
        Text(date, fontSize = 13.sp, color = ElectricBlue, fontWeight = FontWeight.Bold)
    }
}
