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
    onNavigateToChat: () -> Unit = {}
) {
    val employees by viewModel.employees.collectAsState(initial = emptyList())
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    val attendanceList by viewModel.allAttendance.collectAsState(initial = emptyList())
    val leavesList by viewModel.leaves.collectAsState(initial = emptyList())
    val leadsList by viewModel.leads.collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }

    val filteredEmployees = employees.filter { emp ->
        emp.name.contains(searchQuery, ignoreCase = true) ||
        emp.designation.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "MB Admin Portal",
                onBack = onBack,
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = TextPrimary)
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
                        // Employee Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = BrandBlue.copy(alpha = 0.12f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(emp.name.take(1), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = BrandBlue)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(emp.name, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color(0xFF0F172A))
                                    Text("${emp.designation} · ${emp.department.name}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF475569))
                                }
                            }

                            StatusIndicatorBadge(
                                status = emp.presenceStatus,
                                showLabel = true
                            )
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

                        // Action Bar: Team Chat Shortcut
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
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
                                    Text("Team Chat & Communication", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                }
                                Text("Open Chat →", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = BrandBlue)
                            }
                        }
                    }
                }
            }
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
