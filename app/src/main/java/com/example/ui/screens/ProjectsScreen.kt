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
import com.example.data.model.ProjectEntity
import com.example.ui.components.AppHeader
import com.example.ui.components.StatusTag
import com.example.ui.theme.*

@Composable
fun ProjectsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onProjectClick: (Long) -> Unit
) {
    val projects by viewModel.projects.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filters = listOf("All", "Active", "Completed", "Overdue")

    val filteredProjects = projects.filter { proj ->
        val matchesFilter = when (selectedFilter) {
            "Active" -> proj.status.equals("Active", ignoreCase = true)
            "Completed" -> proj.status.equals("Completed", ignoreCase = true)
            "Overdue" -> proj.status.equals("Overdue", ignoreCase = true)
            else -> true
        }
        val matchesSearch = proj.name.contains(searchQuery, ignoreCase = true) ||
                proj.clientName.contains(searchQuery, ignoreCase = true)
        matchesFilter && matchesSearch
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Projects",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { /* Calendar view */ }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar", tint = TextPrimary)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF0F172A),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Project")
            }
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search projects...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        unfocusedBorderColor = BorderLight
                    )
                )
            }

            // Filter Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filters) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Projects List
            if (filteredProjects.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No Projects Found", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                            Text("Create a new project using the + button below", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
            } else {
                items(filteredProjects) { project ->
                    ProjectCardItem(
                        project = project,
                        onClick = { onProjectClick(project.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        var newName by remember { mutableStateOf("") }
        var newClient by remember { mutableStateOf("") }
        var newDeadline by remember { mutableStateOf("30 Oct 2026") }
        var newPriority by remember { mutableStateOf("High") }
        var newTeamSize by remember { mutableIntStateOf(4) }
        var newTotalTasks by remember { mutableIntStateOf(10) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEFF6FF),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AddBusiness, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Create New Project", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Project Name *") },
                        placeholder = { Text("e.g. Enterprise Mobile App") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newClient,
                        onValueChange = { newClient = it },
                        label = { Text("Client Name *") },
                        placeholder = { Text("e.g. Making Brands Tech") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDeadline,
                        onValueChange = { newDeadline = it },
                        label = { Text("Deadline Date") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextMuted) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newPriority,
                            onValueChange = { newPriority = it },
                            label = { Text("Priority") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newTeamSize.toString(),
                            onValueChange = { newTeamSize = it.toIntOrNull() ?: 1 },
                            label = { Text("Team Size") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank() && newClient.isNotBlank()) {
                            viewModel.addProject(
                                name = newName.trim(),
                                clientName = newClient.trim(),
                                deadline = newDeadline,
                                priority = newPriority,
                                teamSize = newTeamSize,
                                totalTasks = newTotalTasks
                            )
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Create Project", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun ProjectCardItem(
    project: ProjectEntity,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(project.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                    Text("${project.clientName} · ${project.totalTasks} Tasks", fontSize = 12.sp, color = TextSecondary)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (project.status == "Completed") StatusGreenBg else StatusBlueBg
                ) {
                    Text(
                        project.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (project.status == "Completed") StatusGreen else BrandBlue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { project.progressPercent / 100f },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (project.progressPercent == 100) StatusGreen else BrandLightBlue,
                    trackColor = Color(0xFFE2E8F0)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "${project.progressPercent}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = StatusRed, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Due: ${project.deadline}", fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}
