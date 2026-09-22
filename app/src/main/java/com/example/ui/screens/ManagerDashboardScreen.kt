package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Department
import com.example.data.model.EmployeeEntity
import com.example.data.model.EmployeeStatus
import com.example.data.model.PresenceStatus
import com.example.ui.components.AppHeader
import com.example.ui.components.MetricBadge
import com.example.ui.components.StatusIndicatorBadge
import com.example.ui.components.TeamWorkloadChartCard
import com.example.domain.milo.*
import com.example.milo.*
import com.example.ui.theme.*

@Composable
fun ManagerDashboardScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit = {},
    onNavigateToProjects: () -> Unit = {},
    onNavigateToLeads: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToTracking: () -> Unit = {},
    onNavigateToTimesheets: () -> Unit = {},
    onNavigateToMeetings: () -> Unit = {},
    onNavigateToVault: () -> Unit = {}
) {
    BackHandler {
        onBack()
    }

    val employees by viewModel.employees.collectAsState(initial = emptyList())
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    val attendanceList by viewModel.allAttendance.collectAsState(initial = emptyList())
    val leavesList by viewModel.leaves.collectAsState(initial = emptyList())
    val leadsList by viewModel.leads.collectAsState(initial = emptyList())
    val chatMessages by viewModel.allChatMessages.collectAsState(initial = emptyList())

    val context = LocalContext.current
    val installDate = remember { com.example.util.AppPreferences.getAppInstallDate(context) }
    val filteredAttendance = remember(attendanceList, installDate) {
        val installDateObj = try {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(installDate)
        } catch (_: Exception) { null }
        val installDateStartMs = installDateObj?.time ?: 0L

        attendanceList.filter { record ->
            if (record.timestamp > 0 && installDateStartMs > 0) {
                record.timestamp >= installDateStartMs
            } else {
                record.date >= installDate
            }
        }
    }

    var searchQuery by remember { mutableStateOf("") }

    // Employee Creation & Task Assignment Dialog States
    var showCreateEmployeeDialog by remember { mutableStateOf(false) }
    var showAssignTaskDialog by remember { mutableStateOf(false) }
    var preselectedAssignee by remember { mutableStateOf<String?>(null) }

    // Admin Delete / Clear Dialog States
    var showClearAllEnterpriseDialog by remember { mutableStateOf(false) }
    var showMiloAssistant by remember { mutableStateOf(false) }
    var showLogoutConfirmationDialog by remember { mutableStateOf(false) }
    var employeeToClearFields by remember { mutableStateOf<EmployeeEntity?>(null) }
    var employeeToDelete by remember { mutableStateOf<EmployeeEntity?>(null) }
    var sectionToClear by remember { mutableStateOf<String?>(null) }

    val filteredEmployees = employees.filter { emp ->
        emp.name.contains(searchQuery, ignoreCase = true) ||
        emp.designation.contains(searchQuery, ignoreCase = true)
    }

    // 0A. Dialog: Create Employee with Email & Password (Admin Only)
    if (showCreateEmployeeDialog) {
        var empName by remember { mutableStateOf("") }
        var empEmail by remember { mutableStateOf("") }
        var empPassword by remember { mutableStateOf("") }
        var empPhone by remember { mutableStateOf("+91 ") }
        var empDesignation by remember { mutableStateOf("") }
        var empDept by remember { mutableStateOf(Department.ENGINEERING) }
        var passwordVisible by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var isCreating by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isCreating) showCreateEmployeeDialog = false },
            icon = { Icon(Icons.Default.PersonAdd, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(36.dp)) },
            title = { Text("Create & Authorize Employee", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Set up employee credentials. Only this employee will be authorized to sign in with this email and password.",
                        fontSize = 12.sp,
                        color = Color(0xFF475569)
                    )

                    if (errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEE2E2),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFDC2626),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = empName,
                        onValueChange = { empName = it },
                        label = { Text("Employee Full Name") },
                        placeholder = { Text("e.g. Rahul Sharma") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = empEmail,
                        onValueChange = { empEmail = it; errorMessage = null },
                        label = { Text("Employee Email Address") },
                        placeholder = { Text("e.g. employee@makingbrands.in") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = empPassword,
                        onValueChange = { empPassword = it; errorMessage = null },
                        label = { Text("Set Employee Password") },
                        placeholder = { Text("Minimum 4 characters") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = empPhone,
                        onValueChange = { empPhone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = empDesignation,
                        onValueChange = { empDesignation = it },
                        label = { Text("Designation") },
                        placeholder = { Text("e.g. Senior Android Engineer") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Text("Department", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(Department.ENGINEERING, Department.DESIGN, Department.MARKETING, Department.SALES).forEach { dept ->
                            val isSel = empDept == dept
                            FilterChip(
                                selected = isSel,
                                onClick = { empDept = dept },
                                label = { Text(dept.name.take(4), fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isCreating = true
                        viewModel.createEmployeeByAdmin(
                            name = empName,
                            email = empEmail,
                            password = empPassword,
                            phone = empPhone,
                            designation = empDesignation,
                            department = empDept,
                            onSuccess = {
                                isCreating = false
                                showCreateEmployeeDialog = false
                                Toast.makeText(context, "Employee $empName created & authorized!", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                isCreating = false
                                errorMessage = err
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    enabled = !isCreating
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Create Employee", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateEmployeeDialog = false }, enabled = !isCreating) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // 0B. Dialog: Assign Task to Available Employees
    if (showAssignTaskDialog) {
        var taskTitle by remember { mutableStateOf("") }
        var projectName by remember { mutableStateOf("Enterprise App") }
        var priority by remember { mutableStateOf("High") }
        var dueDate by remember { mutableStateOf("Today, 6:00 PM") }
        val availableEmployees = employees.filter { it.status == EmployeeStatus.ACTIVE }
        var selectedAssignee by remember {
            mutableStateOf(preselectedAssignee ?: availableEmployees.firstOrNull()?.name ?: "Rahul Sharma")
        }

        AlertDialog(
            onDismissRequest = { showAssignTaskDialog = false },
            icon = { Icon(Icons.Default.AssignmentInd, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(36.dp)) },
            title = { Text("Assign Task to Available Employee", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Assign an enterprise task to an active team member.",
                        fontSize = 12.sp,
                        color = Color(0xFF475569)
                    )

                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("Task Title") },
                        placeholder = { Text("e.g. Implement Realtime Sync") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { projectName = it },
                        label = { Text("Project Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Text("Assign to Available Employee:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        availableEmployees.forEach { emp ->
                            val isSel = selectedAssignee == emp.name
                            val isOnline = emp.presenceStatus != PresenceStatus.OFFLINE
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) BrandBlue.copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSel) BrandBlue else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedAssignee = emp.name }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = isSel,
                                            onClick = { selectedAssignee = emp.name },
                                            colors = RadioButtonDefaults.colors(selectedColor = BrandBlue)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(emp.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("${emp.designation} · ${emp.email}", fontSize = 10.sp, color = TextSecondary)
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isOnline) Color(0xFFDEF7EC) else Color(0xFFF1F5F9)
                                    ) {
                                        Text(
                                            text = if (isOnline) "🟢 Available" else "⚪ Offline",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isOnline) Color(0xFF03543F) else Color(0xFF64748B),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text("Priority", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("High", "Medium", "Low").forEach { p ->
                            val isSel = priority == p
                            FilterChip(
                                selected = isSel,
                                onClick = { priority = p },
                                label = { Text(p, fontSize = 12.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("Due Date / Deadline") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (taskTitle.isNotBlank()) {
                            viewModel.addTask(
                                title = taskTitle,
                                projectName = projectName,
                                priority = priority,
                                dueDate = dueDate,
                                assignee = selectedAssignee
                            )
                            showAssignTaskDialog = false
                            Toast.makeText(context, "Task assigned to $selectedAssignee!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    enabled = taskTitle.isNotBlank()
                ) {
                    Text("Assign Task", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAssignTaskDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // 1. Comprehensive Dialog: Show & Delete All Entities and Fields
    if (showClearAllEnterpriseDialog) {
        var selectedDeleteTab by remember { mutableStateOf(0) }
        var showConfirmDeleteEverything by remember { mutableStateOf(false) }

        if (showConfirmDeleteEverything) {
            AlertDialog(
                onDismissRequest = { showConfirmDeleteEverything = false },
                icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = StatusRed, modifier = Modifier.size(36.dp)) },
                title = { Text("Delete Everything?", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Are you sure you want to delete all employees, tasks, chats, attendance, leads, leaves, and other fields permanently?",
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearAllEnterpriseFields()
                            showConfirmDeleteEverything = false
                            showClearAllEnterpriseDialog = false
                            Toast.makeText(context, "All enterprise fields and records permanently deleted!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                    ) {
                        Text("Yes, Delete Everything", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDeleteEverything = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showClearAllEnterpriseDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 20.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = StatusRed, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete All Enterprise Fields", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                        }
                        IconButton(onClick = { showClearAllEnterpriseDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Master Action Card: Delete All Everything
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Master Enterprise Reset", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StatusRed)
                            Text("Delete all employees, tasks, chats, attendance, leads, leaves, and other records across the system in one tap.", fontSize = 11.sp, color = Color(0xFF64748B))
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showConfirmDeleteEverything = true },
                                colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(38.dp)
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("DELETE ALL FIELDS (ENTIRE ENTERPRISE)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tabs: Employees, Tasks, Chats, Attendance, Other Fields
                    val tabTitles = listOf(
                        "Employees (${employees.size})",
                        "Tasks (${tasks.size})",
                        "Chats (${chatMessages.size})",
                        "Attendance (${filteredAttendance.size})",
                        "Other Fields"
                    )
                    ScrollableTabRow(
                        selectedTabIndex = selectedDeleteTab,
                        edgePadding = 0.dp,
                        containerColor = Color(0xFFF1F5F9),
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                    ) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedDeleteTab == index,
                                onClick = { selectedDeleteTab = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontSize = 11.sp,
                                        fontWeight = if (selectedDeleteTab == index) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedDeleteTab == index) BrandBlue else Color(0xFF475569)
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tab Contents
                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedDeleteTab) {
                            0 -> {
                                // All Employees
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("All Employees (${employees.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                        if (employees.isNotEmpty()) {
                                            TextButton(
                                                onClick = {
                                                    viewModel.clearAllEmployees()
                                                    Toast.makeText(context, "All employees cleared", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Text("Clear All Employees", color = StatusRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    if (employees.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("No employees found", color = Color(0xFF94A3B8), fontSize = 13.sp)
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(employees, key = { it.id }) { emp ->
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = Color(0xFFF8FAFC),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(10.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(emp.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                                            Text("${emp.designation} • ${emp.department.name}", fontSize = 11.sp, color = Color(0xFF64748B))
                                                            Text(emp.email, fontSize = 10.sp, color = Color(0xFF94A3B8))
                                                        }
                                                        IconButton(onClick = { viewModel.deleteEmployee(emp) }) {
                                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Employee", tint = StatusRed, modifier = Modifier.size(20.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            1 -> {
                                // All Tasks
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("All Tasks (${tasks.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                        if (tasks.isNotEmpty()) {
                                            TextButton(
                                                onClick = {
                                                    viewModel.clearAllTasks()
                                                    Toast.makeText(context, "All tasks cleared", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Text("Clear All Tasks", color = StatusRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    if (tasks.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("No tasks found", color = Color(0xFF94A3B8), fontSize = 13.sp)
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(tasks, key = { it.id }) { task ->
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = Color(0xFFF8FAFC),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(10.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(task.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                                            Text("Assignee: ${task.assignee} • Priority: ${task.priority}", fontSize = 11.sp, color = Color(0xFF64748B))
                                                            Text(if (task.isCompleted) "Status: Completed" else "Status: Pending", fontSize = 10.sp, color = if (task.isCompleted) StatusGreen else StatusOrange)
                                                        }
                                                        IconButton(onClick = { viewModel.deleteTask(task) }) {
                                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Task", tint = StatusRed, modifier = Modifier.size(20.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            2 -> {
                                // All Chats
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("All Chats (${chatMessages.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                        if (chatMessages.isNotEmpty()) {
                                            TextButton(
                                                onClick = {
                                                    viewModel.clearAllChat()
                                                    Toast.makeText(context, "All chat messages cleared", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Text("Clear All Chats", color = StatusRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    if (chatMessages.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("No chat messages found", color = Color(0xFF94A3B8), fontSize = 13.sp)
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(chatMessages, key = { it.id }) { msg ->
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = Color(0xFFF8FAFC),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(10.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(msg.senderName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                                                            Text(msg.messageText.ifBlank { msg.attachmentFileName ?: "Attachment" }, fontSize = 11.sp, color = Color(0xFF475569))
                                                            Text("Channel: ${msg.channelId} • ${msg.timestampText}", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                                        }
                                                        IconButton(onClick = { viewModel.deleteChatMessage(msg) }) {
                                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Chat", tint = StatusRed, modifier = Modifier.size(20.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            3 -> {
                                // All Attendance (starts strictly from app install date)
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("All Attendance (${filteredAttendance.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                            Text("Starting from install date: $installDate", fontSize = 10.sp, color = BrandBlue)
                                        }
                                        if (filteredAttendance.isNotEmpty()) {
                                            TextButton(
                                                onClick = {
                                                    viewModel.clearAllAttendance()
                                                    Toast.makeText(context, "All attendance records cleared", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Text("Clear All Attendance", color = StatusRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    if (filteredAttendance.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("No attendance records logged since $installDate", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(filteredAttendance, key = { it.id }) { att ->
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = Color(0xFFF8FAFC),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(10.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(att.employeeName.ifBlank { "Employee" }, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                                                            Text("Date: ${att.date} • Status: ${att.status}", fontSize = 11.sp, color = Color(0xFF475569))
                                                            Text("In: ${att.checkInTime} | Out: ${att.checkOutTime ?: "Active"} | ${att.durationMinutes / 60}h ${att.durationMinutes % 60}m", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                                        }
                                                        IconButton(onClick = { viewModel.deleteAttendanceRecord(att) }) {
                                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Attendance", tint = StatusRed, modifier = Modifier.size(20.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            4 -> {
                                // All Other Fields
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("Other Enterprise Modules & Fields", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))

                                    // Leads
                                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFF8FAFC), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column {
                                                Text("Leads Pipeline", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                                Text("${leadsList.size} active leads in pipeline", fontSize = 11.sp, color = Color(0xFF64748B))
                                            }
                                            Button(onClick = { viewModel.clearAllLeads(); Toast.makeText(context, "All leads cleared", Toast.LENGTH_SHORT).show() }, colors = ButtonDefaults.buttonColors(containerColor = StatusRed), shape = RoundedCornerShape(8.dp)) {
                                                Text("Clear Leads", fontSize = 11.sp, color = Color.White)
                                            }
                                        }
                                    }

                                    // Leaves
                                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFF8FAFC), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column {
                                                Text("Applied Leaves", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                                Text("${leavesList.size} leave requests", fontSize = 11.sp, color = Color(0xFF64748B))
                                            }
                                            Button(onClick = { viewModel.clearAllLeaves(); Toast.makeText(context, "All leaves cleared", Toast.LENGTH_SHORT).show() }, colors = ButtonDefaults.buttonColors(containerColor = StatusRed), shape = RoundedCornerShape(8.dp)) {
                                                Text("Clear Leaves", fontSize = 11.sp, color = Color.White)
                                            }
                                        }
                                    }

                                    // Expense Claims
                                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFF8FAFC), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column {
                                                Text("Expense Claims", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                                Text("Reimbursements & claims", fontSize = 11.sp, color = Color(0xFF64748B))
                                            }
                                            Button(onClick = { viewModel.clearAllExpenses(); Toast.makeText(context, "All expenses cleared", Toast.LENGTH_SHORT).show() }, colors = ButtonDefaults.buttonColors(containerColor = StatusRed), shape = RoundedCornerShape(8.dp)) {
                                                Text("Clear Expenses", fontSize = 11.sp, color = Color.White)
                                            }
                                        }
                                    }

                                    // Client Meetings
                                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFF8FAFC), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column {
                                                Text("Client Meetings", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                                Text("Meeting logs & schedules", fontSize = 11.sp, color = Color(0xFF64748B))
                                            }
                                            Button(onClick = { viewModel.clearAllMeetings(); Toast.makeText(context, "All meetings cleared", Toast.LENGTH_SHORT).show() }, colors = ButtonDefaults.buttonColors(containerColor = StatusRed), shape = RoundedCornerShape(8.dp)) {
                                                Text("Clear Meetings", fontSize = 11.sp, color = Color.White)
                                            }
                                        }
                                    }

                                    // Document Vault
                                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFF8FAFC), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column {
                                                Text("Document Vault", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                                Text("Vault files & policies", fontSize = 11.sp, color = Color(0xFF64748B))
                                            }
                                            Button(onClick = { viewModel.clearAllDocuments(); Toast.makeText(context, "All documents cleared", Toast.LENGTH_SHORT).show() }, colors = ButtonDefaults.buttonColors(containerColor = StatusRed), shape = RoundedCornerShape(8.dp)) {
                                                Text("Clear Vault", fontSize = 11.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showClearAllEnterpriseDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                    }
                }
            }
        }
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

    // Logout Confirmation Dialog
    if (showLogoutConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmationDialog = false },
            icon = { Icon(Icons.Default.Logout, contentDescription = null, tint = StatusRed, modifier = Modifier.size(36.dp)) },
            title = { Text("Log Out from Admin Portal?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to log out of the MB Admin session? You will be returned to the Login screen.",
                    fontSize = 13.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmationDialog = false
                        viewModel.logout()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Log Out", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmationDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "MB Admin Portal",
                subtitle = "Administrator Session",
                onBack = onBack,
                actions = {
                    FilledTonalButton(
                        onClick = { showLogoutConfirmationDialog = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFFEE2E2),
                            contentColor = StatusRed
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .padding(end = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Log Out Admin",
                            tint = StatusRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Logout",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusRed
                        )
                    }
                    IconButton(onClick = { showClearAllEnterpriseDialog = true }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All Fields", tint = Color(0xFF64748B))
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
            // MB Admin Banner with Logout Button
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
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
                            }
                            Text("Operations & Employee Telemetry", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Real-time live monitoring of all ${employees.size} active personnel", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Quick Logout Action Button inside Banner
                            IconButton(
                                onClick = { showLogoutConfirmationDialog = true },
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(40.dp)
                                    .background(Color(0xFF1E293B), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Logout,
                                    contentDescription = "Admin Logout",
                                    tint = StatusRed,
                                    modifier = Modifier.size(20.dp)
                                )
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
            }

            // 🦁 MILO LIVE ASSISTANT COMPONENT FOR ADMIN
            item {
                MiloDashboardWidget(
                    miloViewModel = viewModel.miloViewModel,
                    newLeadsCount = 12,
                    followUpsCount = 8,
                    onNavigateToLeads = onNavigateToLeads,
                    onNavigateToFollowUps = {
                        viewModel.miloViewModel.handleEvent(MiloEvent.FollowUpDue(8))
                        onNavigateToLeads()
                    },
                    onOpenAiAssistant = { showMiloAssistant = true }
                )
            }
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Admin Enterprise Actions",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Provision employee accounts and dispatch tasks to active staff",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showCreateEmployeeDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create Employee", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                            }

                            FilledTonalButton(
                                onClick = {
                                    preselectedAssignee = null
                                    showAssignTaskDialog = true
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFFEFF6FF),
                                    contentColor = BrandBlue
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Icon(Icons.Default.AssignmentInd, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Assign Task", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrandBlue)
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

            // 📊 Team Workload by Project Visualizer (Recharts / D3 Logic)
            item {
                TeamWorkloadChartCard(
                    viewModel = viewModel,
                    onNavigateToTasks = { /* Tasks list in dialog/nav */ },
                    onNavigateToProjects = onNavigateToProjects
                )
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
            items(filteredEmployees, key = { it.id }) { emp ->
                val empFirstName = emp.name.split(" ").firstOrNull() ?: emp.name
                val empTasks = tasks.filter { it.assignee.equals(emp.name, ignoreCase = true) || it.assignee.contains(empFirstName, ignoreCase = true) }
                val finishedCount = empTasks.count { it.isCompleted }
                val pendingCount = empTasks.count { !it.isCompleted }

                val empLeaves = leavesList.filter { it.username.contains(empFirstName, ignoreCase = true) || it.username.contains(emp.name, ignoreCase = true) }
                val leavesCount = empLeaves.size

                val empLeads = leadsList.filter { it.assignedTo.contains(empFirstName, ignoreCase = true) || it.assignedTo.contains(emp.name, ignoreCase = true) }
                val leadsCount = empLeads.size

                val daysPresent = filteredAttendance.count {
                    (it.employeeName.contains(empFirstName, ignoreCase = true) || it.employeeName.contains(emp.name, ignoreCase = true)) && it.date >= installDate
                }
                val todayAtt = filteredAttendance.firstOrNull {
                    (it.employeeName.contains(empFirstName, ignoreCase = true) || it.employeeName.contains(emp.name, ignoreCase = true)) && it.date == com.example.util.AppPreferences.getCurrentDateString()
                }
                val dailyTime = if (todayAtt != null) {
                    "${todayAtt.durationMinutes / 60}h ${todayAtt.durationMinutes % 60}m"
                } else if (emp.presenceStatus.name != "OFFLINE") {
                    "Checked In"
                } else {
                    "0h 0m"
                }

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
                                    if (emp.email.isNotBlank()) {
                                        Text(emp.email, fontSize = 11.sp, color = BrandBlue, fontWeight = FontWeight.Medium)
                                    }
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

                            // Quick Assign Task to this employee
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = BrandBlue.copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BrandBlue.copy(alpha = 0.3f)),
                                modifier = Modifier.clickable {
                                    preselectedAssignee = emp.name
                                    showAssignTaskDialog = true
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AssignmentInd, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Assign Task", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
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
    }

    if (showMiloAssistant) {
        MiloAiAssistantSheet(
            miloViewModel = viewModel.miloViewModel,
            onDismiss = { showMiloAssistant = false },
            onNavigateToLeads = onNavigateToLeads,
            onNavigateToTasks = onNavigateToTasks
        )
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
