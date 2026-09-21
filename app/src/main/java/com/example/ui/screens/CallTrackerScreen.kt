package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallLogEntity
import com.example.ui.components.AppHeader
import com.example.ui.components.StandardScreenHeader
import com.example.ui.theme.*

@Composable
fun CallTrackerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val callLogs by viewModel.callLogs.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Incoming", "Outgoing", "Missed")

    var showQuickLogDialog by remember { mutableStateOf(false) }

    val filteredLogs = callLogs.filter { log ->
        when (selectedFilter) {
            "Incoming" -> log.callType.equals("Incoming", ignoreCase = true)
            "Outgoing" -> log.callType.equals("Outgoing", ignoreCase = true)
            "Missed" -> log.status.equals("No Answer", ignoreCase = true)
            else -> true
        }
    }.sortedByDescending { it.id }

    Scaffold(
        topBar = {
            StandardScreenHeader(
                viewModel = viewModel,
                subMenuTitle = "Call Tracker & Logs",
                subMenuSubtitle = "${filteredLogs.size} Calls Recorded",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showQuickLogDialog = true }) {
                        Icon(Icons.Default.AddIcCall, contentDescription = "Log Call", tint = BrandBlue)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showQuickLogDialog = true },
                containerColor = BrandBlue,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Log Call")
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
            // Filter Pills
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

            // Call Log Cards
            items(filteredLogs) { log ->
                CallLogCardItem(log)
            }
        }
    }

    if (showQuickLogDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var duration by remember { mutableStateOf("3m 15s") }
        var type by remember { mutableStateOf("Outgoing") }

        AlertDialog(
            onDismissRequest = { showQuickLogDialog = false },
            title = { Text("Log Quick Call", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Contact Name") },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp),
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp),
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = { Text("Duration") },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp),
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addCallLog(name, phone, type, duration)
                            showQuickLogDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    Text("Save Log", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickLogDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun CallLogCardItem(log: CallLogEntity) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (log.status == "Connected") StatusGreenBg else StatusRedBg,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (log.callType == "Incoming") Icons.AutoMirrored.Filled.CallReceived else Icons.AutoMirrored.Filled.CallMade,
                            contentDescription = null,
                            tint = if (log.status == "Connected") StatusGreen else StatusRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(log.contactName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "${log.callType} · ${log.durationText}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(log.timestampText, fontSize = 11.sp, color = TextMuted)
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (log.status == "Connected") StatusGreenBg else StatusRedBg
            ) {
                Text(
                    log.status,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (log.status == "Connected") StatusGreen else StatusRed,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
