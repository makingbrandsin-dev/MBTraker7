package com.example.ui.screens

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskEntity
import com.example.ui.components.AppHeader
import com.example.ui.components.CrmTasksAttendanceSwitcher
import com.example.ui.components.PriorityBadge
import com.example.ui.theme.*

@Composable
fun TasksScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToCrm: () -> Unit = {},
    onNavigateToAttendance: () -> Unit = {},
    onNavigateToProfile: (() -> Unit)? = null
) {
    val tasks by viewModel.tasks.collectAsState()
    var selectedScopeTab by remember { mutableIntStateOf(0) } // 0: All, 1: My Tasks
    var selectedStatusFilter by remember { mutableStateOf("All") }
    var showAddTaskDialog by remember { mutableStateOf(false) }

    val statusFilters = listOf("All", "Backlog", "In Progress", "Completed")

    val filteredTasks = tasks.filter { task ->
        val matchesStatus = when (selectedStatusFilter) {
            "Backlog" -> task.status.equals("Backlog", ignoreCase = true)
            "In Progress" -> task.status.equals("In Progress", ignoreCase = true)
            "Completed" -> task.isCompleted || task.status.equals("Completed", ignoreCase = true)
            else -> true
        }
        val matchesScope = if (selectedScopeTab == 1) task.assignee == "Rahul Sharma" else true
        matchesStatus && matchesScope
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Tasks",
                onBack = onBack,
                onNavigateToProfile = onNavigateToProfile
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = ElectricBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // CRM - Tasks - Attendance Hub Switcher
            item {
                CrmTasksAttendanceSwitcher(
                    selectedTab = "tasks",
                    onNavigateToCrm = onNavigateToCrm,
                    onNavigateToTasks = { },
                    onNavigateToAttendance = onNavigateToAttendance,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            // Scope Tabs (All vs My Tasks)
            item {
                TabRow(
                    selectedTabIndex = selectedScopeTab,
                    containerColor = Color.White,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedScopeTab == 0,
                        onClick = { selectedScopeTab = 0 },
                        text = { Text("All", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedScopeTab == 1,
                        onClick = { selectedScopeTab = 1 },
                        text = { Text("My Tasks", fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // Status Filter Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(statusFilters) { filter ->
                        val isSel = selectedStatusFilter == filter
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedStatusFilter = filter },
                            label = {
                                Text(
                                    filter,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSel) Color.White else Color(0xFF0F172A)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandBlue,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFE2E8F0),
                                labelColor = Color(0xFF0F172A)
                            )
                        )
                    }
                }
            }

            // Tasks List
            items(filteredTasks) { task ->
                TaskCardItem(
                    task = task,
                    onToggle = { viewModel.toggleTaskCompletion(task) },
                    onStatusChange = { newStatus -> viewModel.updateTaskStatus(task, newStatus) },
                    onDelete = { viewModel.deleteTask(task) }
                )
            }
        }
    }

    if (showAddTaskDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newProject by remember { mutableStateOf("Website Revamp") }
        var newPriority by remember { mutableStateOf("High") }

        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ElectricBlueBg,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Create New Task", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Task Title *") },
                        textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp),
                        colors = appTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newProject,
                        onValueChange = { newProject = it },
                        label = { Text("Project Name") },
                        textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp),
                        colors = appTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            viewModel.addTask(newTitle, newProject, newPriority, "30 Sep 2025")
                            showAddTaskDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Add Task", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }
}

@Composable
fun TaskCardItem(
    task: TaskEntity,
    onToggle: () -> Unit,
    onStatusChange: (String) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var showStatusMenu by remember { mutableStateOf(false) }
    val availableStatuses = listOf("Backlog", "In Progress", "In Review", "Completed")

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { onToggle() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = BrandBlue,
                            uncheckedColor = TextMuted
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = task.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (task.isCompleted) TextMuted else Color(0xFF0F172A),
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${task.projectName} · Due: ${task.dueDate}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    PriorityBadge(priority = task.priority)

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete task",
                            tint = StatusRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showStatusMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Task options",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showStatusMenu,
                            onDismissRequest = { showStatusMenu = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            Text(
                                "Update Status:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                            availableStatuses.forEach { statusOption ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            statusOption,
                                            fontWeight = if (task.status.equals(statusOption, ignoreCase = true)) FontWeight.Bold else FontWeight.Normal,
                                            color = if (task.status.equals(statusOption, ignoreCase = true)) BrandBlue else Color(0xFF0F172A)
                                        )
                                    },
                                    onClick = {
                                        onStatusChange(statusOption)
                                        showStatusMenu = false
                                    },
                                    leadingIcon = {
                                        val iconVector = when (statusOption) {
                                            "Completed" -> Icons.Default.CheckCircle
                                            "In Progress" -> Icons.Default.Pending
                                            "In Review" -> Icons.Default.FindInPage
                                            else -> Icons.Default.Inventory
                                        }
                                        Icon(
                                            iconVector,
                                            contentDescription = null,
                                            tint = if (task.status.equals(statusOption, ignoreCase = true)) BrandBlue else Color(0xFF64748B),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                            }
                            Divider(color = BorderLight, thickness = 1.dp)
                            DropdownMenuItem(
                                text = {
                                    Text("Delete Task", fontWeight = FontWeight.Bold, color = StatusRed)
                                },
                                onClick = {
                                    onDelete()
                                    showStatusMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Task",
                                        tint = StatusRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Status Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Status:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                availableStatuses.forEach { statusOption ->
                    val isSelected = task.status.equals(statusOption, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) BrandBlue else Color(0xFFE2E8F0),
                        modifier = Modifier.clickable { onStatusChange(statusOption) }
                    ) {
                        Text(
                            text = statusOption,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) Color.White else Color(0xFF0F172A),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
