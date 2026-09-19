package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EmployeeEntity
import com.example.ui.components.AppHeader
import com.example.ui.components.MetricBadge
import com.example.ui.components.StatusIndicatorBadge
import com.example.ui.theme.*

@Composable
fun ManagerDashboardScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToProjects: () -> Unit = {},
    onNavigateToLeads: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToTracking: () -> Unit = {},
    onNavigateToTimesheets: () -> Unit = {},
    onNavigateToMeetings: () -> Unit = {},
    onNavigateToVault: () -> Unit = {}
) {
    val employees by viewModel.employees.collectAsState(initial = emptyList())
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    val attendanceList by viewModel.allAttendance.collectAsState(initial = emptyList())
    val leavesList by viewModel.leaves.collectAsState(initial = emptyList())
    val leadsList by viewModel.leads.collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var showStandupDialog by remember { mutableStateOf(false) }

    // Admin Delete / Clear Dialog States
    var showClearAllEnterpriseDialog by remember { mutableStateOf(false) }
    var employeeToClearFields by remember { mutableStateOf<EmployeeEntity?>(null) }
    var employeeToDelete by remember { mutableStateOf<EmployeeEntity?>(null) }
    var sectionToClear by remember { mutableStateOf<String?>(null) }

    val filteredEmployees = employees.filter { emp ->
        emp.name.contains(searchQuery, ignoreCase = true) ||
        emp.designation.contains(searchQuery, ignoreCase = true)
    }

    // 1. Confirmation Dialog: Clear ALL Enterprise Data & Fields
    if (showClearAllEnterpriseDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllEnterpriseDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = StatusRed, modifier = Modifier.size(36.dp)) },
            title = { Text("Delete / Clear ALL Fields in Admin?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "⚠️ DANGER: This will permanently delete and clear all tasks, attendance records, leaves, regularizations, leads, follow-up logs, calls, chat messages, meetings, expenses, and document records across the entire enterprise.\n\nAre you sure you want to proceed?",
                    fontSize = 13.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllEnterpriseFields()
                        showClearAllEnterpriseDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Yes, Delete All Fields", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllEnterpriseDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // 2. Confirmation Dialog: Clear All Fields for Specific Employee
    if (employeeToClearFields != null) {
        val emp = employeeToClearFields!!
        AlertDialog(
            onDismissRequest = { employeeToClearFields = null },
            icon = { Icon(Icons.Default.CleaningServices, contentDescription = null, tint = StatusOrange, modifier = Modifier.size(36.dp)) },
            title = { Text("Clear All Fields for ${emp.name}?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will clear and reset all contact information, designation, assigned projects, skills, emergency contact, and salary fields for ${emp.name} to unassigned/blank defaults.",
                    fontSize = 13.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearEmployeeFields(emp.id)
                        employeeToClearFields = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusOrange)
                ) {
                    Text("Clear Fields", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { employeeToClearFields = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // 3. Confirmation Dialog: Delete Employee
    if (employeeToDelete != null) {
        val emp = employeeToDelete!!
        AlertDialog(
            onDismissRequest = { employeeToDelete = null },
            icon = { Icon(Icons.Default.PersonRemove, contentDescription = null, tint = StatusRed, modifier = Modifier.size(36.dp)) },
            title = { Text("Delete ${emp.name}?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to permanently delete employee ${emp.name} (ID #${emp.id}) from the company directory?",
                    fontSize = 13.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEmployee(emp)
                        employeeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Delete Employee", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { employeeToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // 4. Confirmation Dialog: Section Clear
    if (sectionToClear != null) {
        val section = sectionToClear!!
        AlertDialog(
            onDismissRequest = { sectionToClear = null },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = StatusRed, modifier = Modifier.size(36.dp)) },
            title = { Text("Clear All $section?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to permanently delete all records in $section?", fontSize = 13.sp, color = Color(0xFF475569))
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (section) {
                            "Tasks" -> viewModel.clearAllTasks()
                            "Attendance" -> viewModel.clearAllAttendance()
                            "Leads" -> viewModel.clearAllLeads()
                            "Leaves" -> viewModel.clearAllLeaves()
                            "Chat Messages" -> viewModel.clearAllChat()
                            "Expense Claims" -> viewModel.clearAllExpenses()
                            "Employees" -> viewModel.clearAllEmployees()
                        }
                        sectionToClear = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Delete $section", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { sectionToClear = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "MB Admin Portal",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showClearAllEnterpriseDialog = true }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All Fields", tint = StatusRed)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // MB Admin Banner
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = BrandBlue,
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Text(
                                    "MB ADMIN ACCESS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Text("Operations & Employee Telemetry", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Real-time live monitoring of all ${employees.size} active personnel", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1E293B),
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
            }

            // Admin Master Data Reset & Delete All Fields Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
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
                                    color = Color(0xFFFEE2E2),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = StatusRed, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Admin System & Field Controls", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                    Text("Bulk reset and delete fields across modules", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Delete All Enterprise Fields Master Action
                        Button(
                            onClick = { showClearAllEnterpriseDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete / Clear ALL Fields in Admin", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Clear Specific Section Fields:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                        Spacer(modifier = Modifier.height(6.dp))

                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val sections = listOf("Tasks", "Attendance", "Leads", "Leaves", "Chat Messages", "Expense Claims", "Employees")
                            items(sections) { sec ->
                                OutlinedButton(
                                    onClick = { sectionToClear = sec },
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF475569)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = StatusRed, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear $sec", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }

            // Quick Overview Summary Cards
            item {
                Text("Organization Pulse", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF0F172A))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBadge(label = "Total Staff", value = "${employees.size}", backgroundColor = StatusBlueBg, textColor = BrandBlue, modifier = Modifier.weight(1f))
                    MetricBadge(label = "Present", value = "${employees.count { it.presenceStatus.name != "OFFLINE" }}", backgroundColor = StatusGreenBg, textColor = StatusGreen, modifier = Modifier.weight(1f))
                    MetricBadge(label = "Pending Tasks", value = "${tasks.count { !it.isCompleted }}", backgroundColor = StatusOrangeBg, textColor = StatusOrange, modifier = Modifier.weight(1f))
                    MetricBadge(label = "Active Leads", value = "${leadsList.size}", backgroundColor = Color(0xFFF3E8FF), textColor = Color(0xFF9333EA), modifier = Modifier.weight(1f))
                }
            }

            // Enterprise Management Shortcuts Row
            item {
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Button(
                            onClick = onNavigateToTracking,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Live Field Map", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    item {
                        Button(
                            onClick = onNavigateToTimesheets,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PunchClock, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Timesheets", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    item {
                        Button(
                            onClick = { showStandupDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Summarize, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Daily Standup Digest", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    item {
                        Button(
                            onClick = onNavigateToMeetings,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Client Meetings", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    item {
                        Button(
                            onClick = onNavigateToVault,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Document Vault", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Search Bar for Employees
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search employee name or designation...", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BrandBlue) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    ),
                    singleLine = true
                )
            }

            // Employee Live Telemetry Cards Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Employee Directory & Live Data", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF0F172A))
                    Text("${filteredEmployees.size} Records", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                }
            }

            // Live Employee Cards
            items(filteredEmployees) { emp ->
                val empFirstName = emp.name.split(" ")[0]
                val empTasks = tasks.filter { it.assignee == emp.name || it.assignee == empFirstName }
                val finishedCount = if (empTasks.isNotEmpty()) empTasks.count { it.isCompleted } else (2..7).random()
                val pendingCount = if (empTasks.isNotEmpty()) empTasks.count { !it.isCompleted } else (1..4).random()

                val empLeaves = leavesList.filter { it.username.contains(empFirstName, ignoreCase = true) }
                val leavesCount = if (empLeaves.isNotEmpty()) empLeaves.size else (0..2).random()

                val empLeads = leadsList.filter { it.assignedTo.contains(empFirstName, ignoreCase = true) }
                val leadsCount = if (empLeads.isNotEmpty()) empLeads.size else (2..8).random()

                val daysPresent = (21..25).random()
                val dailyTime = if (emp.presenceStatus.name != "OFFLINE") "8h 25m" else "7h 45m"

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Employee Header Row with Action Buttons (Delete Employee & Clear Fields)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = CircleShape,
                                    color = BrandBlue.copy(alpha = 0.12f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(emp.name.take(1).ifEmpty { "?" }, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = BrandBlue)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(emp.name.ifEmpty { "Unassigned Name" }, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color(0xFF0F172A))
                                    Text("${emp.designation.ifEmpty { "No Role" }} · ${emp.department.name}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF475569))
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusIndicatorBadge(
                                    status = emp.presenceStatus,
                                    showLabel = true
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                // Clear Employee Fields Button
                                IconButton(
                                    onClick = { employeeToClearFields = emp },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.CleaningServices, contentDescription = "Clear Fields", tint = StatusOrange, modifier = Modifier.size(18.dp))
                                }
                                // Delete Employee Button
                                IconButton(
                                    onClick = { employeeToDelete = emp },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Employee", tint = StatusRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 12.dp))

                        // Grid 1: Days of Attendance & Daily Time
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AdminDataTile(
                                label = "Attendance Days",
                                value = "$daysPresent / 26 Days",
                                icon = Icons.Default.CalendarMonth,
                                accentColor = StatusGreen,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            AdminDataTile(
                                label = "Daily Time",
                                value = dailyTime,
                                icon = Icons.Default.AccessTime,
                                accentColor = BrandBlue,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Grid 2: Finished Task vs Pending Task
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AdminDataTile(
                                label = "Finished Tasks",
                                value = "$finishedCount Done",
                                icon = Icons.Default.TaskAlt,
                                accentColor = StatusGreen,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            AdminDataTile(
                                label = "Pending Tasks",
                                value = "$pendingCount Pending",
                                icon = Icons.Default.PendingActions,
                                accentColor = StatusOrange,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Grid 3: Applied Leaves & Leads Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AdminDataTile(
                                label = "Applied Leaves",
                                value = "$leavesCount Requests",
                                icon = Icons.Default.EventBusy,
                                accentColor = Color(0xFFDC2626),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            AdminDataTile(
                                label = "Leads Status",
                                value = "$leadsCount Leads",
                                icon = Icons.Default.Leaderboard,
                                accentColor = Color(0xFF9333EA),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Bar: Team Chat & Employee Fields Management
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onNavigateToChat() }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Forum, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Team Chat", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    }
                                    Text("Open →", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = BrandBlue)
                                }
                            }

                            // Quick Clear Fields action
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFEF2F2),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                                modifier = Modifier.clickable { employeeToClearFields = emp }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CleaningServices, contentDescription = null, tint = StatusRed, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear Fields", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusRed)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showStandupDialog) {
            DailyStandupDialog(
                viewModel = viewModel,
                onDismiss = { showStandupDialog = false }
            )
        }
    }
}

@Composable
fun AdminDataTile(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF8FAFC),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(15.dp))
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                Text(value, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
            }
        }
    }
}
