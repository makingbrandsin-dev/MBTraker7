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
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LeadEntity
import com.example.ui.components.AppHeader
import com.example.ui.components.CrmTasksAttendanceSwitcher
import com.example.ui.components.MetricBadge
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onLeadClick: (Long) -> Unit,
    onNavigateToTasks: () -> Unit = {},
    onNavigateToAttendance: () -> Unit = {},
    onNavigateToProfile: (() -> Unit)? = null
) {
    val leads by viewModel.leads.collectAsState()
    var showAddLeadBottomSheet by remember { mutableStateOf(false) }

    val pipelineStages = listOf(
        "New" to 38,
        "Contacted" to 32,
        "Interested" to 21,
        "Follow-up" to 15,
        "Proposal" to 8,
        "Negotiation" to 5,
        "Won" to 4
    )

    Scaffold(
        topBar = {
            AppHeader(
                title = "CRM & Leads",
                onBack = onBack,
                onNavigateToProfile = onNavigateToProfile,
                actions = {
                    IconButton(onClick = { showAddLeadBottomSheet = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Lead", tint = TextPrimary)
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = TextPrimary)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddLeadBottomSheet = true },
                containerColor = ElectricBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Lead", tint = Color.White) },
                text = { Text("Add Lead", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                modifier = Modifier
                    .testTag("add_lead_fab")
                    .padding(bottom = 8.dp)
            )
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CRM - Tasks - Attendance Hub Switcher
            item {
                CrmTasksAttendanceSwitcher(
                    selectedTab = "crm",
                    onNavigateToCrm = { },
                    onNavigateToTasks = onNavigateToTasks,
                    onNavigateToAttendance = onNavigateToAttendance,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            // Metrics Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBadge(
                        label = "Total",
                        value = "124",
                        backgroundColor = Color(0xFFE0E7FF),
                        textColor = Color(0xFF3730A3),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadge(
                        label = "New",
                        value = "38",
                        backgroundColor = Color(0xFFDCFCE7),
                        textColor = Color(0xFF166534),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadge(
                        label = "Follow-ups",
                        value = "24",
                        backgroundColor = Color(0xFFFEF3C7),
                        textColor = Color(0xFF92400E),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadge(
                        label = "Converted",
                        value = "9",
                        backgroundColor = Color(0xFFF3E8FF),
                        textColor = Color(0xFF6B21A8),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Lead Pipeline Funnel
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Lead Pipeline",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        pipelineStages.forEach { (stage, count) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (stage) {
                                                    "Won" -> StatusGreen
                                                    "New" -> BrandBlue
                                                    else -> StatusOrange
                                                }
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(stage, fontSize = 13.sp, color = TextPrimary)
                                }
                                Text(
                                    "$count",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue
                                )
                            }
                        }
                    }
                }
            }

            // Realtime Leads List from Room Database
            item {
                Text(
                    "Recent Leads",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
            }

            items(leads) { lead ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLeadClick(lead.id) }
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
                                color = BrandBlue.copy(alpha = 0.1f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = BrandBlue)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(lead.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                Text("${lead.company} · Score ${lead.leadScore}%", fontSize = 12.sp, color = TextSecondary)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StatusGreenBg
                        ) {
                            Text(
                                lead.stage,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddLeadBottomSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var name by remember { mutableStateOf("") }
        var company by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var requirement by remember { mutableStateOf("") }
        var value by remember { mutableStateOf("₹ 2,50,000") }
        var selectedStage by remember { mutableStateOf("New") }
        var leadScore by remember { mutableFloatStateOf(85f) }
        var isSaving by remember { mutableStateOf(false) }
        var nameError by remember { mutableStateOf(false) }
        var phoneError by remember { mutableStateOf(false) }

        val stages = listOf("New", "Contacted", "Interested", "Follow-up", "Proposal", "Negotiation", "Won")

        ModalBottomSheet(
            onDismissRequest = { showAddLeadBottomSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = Color(0xFFCBD5E1),
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
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
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Add New Lead",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                "Syncs to Cloud Firestore & Local CRM",
                                fontSize = 12.sp,
                                color = ElectricBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.size(36.dp)
                    ) {
                        IconButton(onClick = { showAddLeadBottomSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Customer / Lead Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = false
                    },
                    label = { Text("Lead / Customer Name *") },
                    isError = nameError,
                    supportingText = if (nameError) { { Text("Name is required", color = MaterialTheme.colorScheme.error) } } else null,
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ElectricBlue) },
                    modifier = Modifier.fillMaxWidth().testTag("lead_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp),
                    colors = appTextFieldColors(),
                    singleLine = true
                )

                // Company Name
                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Company / Organization") },
                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, tint = ElectricBlue) },
                    modifier = Modifier.fillMaxWidth().testTag("lead_company_input"),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp),
                    colors = appTextFieldColors(),
                    singleLine = true
                )

                // Phone & Value Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            if (it.isNotBlank()) phoneError = false
                        },
                        label = { Text("Phone *") },
                        isError = phoneError,
                        supportingText = if (phoneError) { { Text("Required", color = MaterialTheme.colorScheme.error) } } else null,
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = ElectricBlue) },
                        modifier = Modifier.weight(1f).testTag("lead_phone_input"),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp),
                        colors = appTextFieldColors(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text("Deal Value") },
                        leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = ElectricBlue) },
                        modifier = Modifier.weight(1f).testTag("lead_value_input"),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp),
                        colors = appTextFieldColors(),
                        singleLine = true
                    )
                }

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = ElectricBlue) },
                    modifier = Modifier.fillMaxWidth().testTag("lead_email_input"),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp),
                    colors = appTextFieldColors(),
                    singleLine = true
                )

                // Pipeline Stage Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Pipeline Stage",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        stages.forEach { stage ->
                            val isSelected = selectedStage == stage
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedStage = stage },
                                label = { Text(stage, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricBlue,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Lead Score Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Initial Lead Score", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                        Text("${leadScore.toInt()}/100", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ElectricBlue)
                    }
                    Slider(
                        value = leadScore,
                        onValueChange = { leadScore = it },
                        valueRange = 0f..100f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricBlue,
                            activeTrackColor = ElectricBlue
                        )
                    )
                }

                // Requirement notes
                OutlinedTextField(
                    value = requirement,
                    onValueChange = { requirement = it },
                    label = { Text("Requirement / Notes") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = ElectricBlue) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp),
                    colors = appTextFieldColors(),
                    minLines = 2,
                    maxLines = 4
                )

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showAddLeadBottomSheet = false },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                nameError = true
                                return@Button
                            }
                            if (phone.isBlank()) {
                                phoneError = true
                                return@Button
                            }
                            isSaving = true
                            viewModel.addLead(
                                name = name.trim(),
                                company = company.ifBlank { "Independent" }.trim(),
                                phone = phone.trim(),
                                email = email.trim(),
                                requirement = requirement.trim(),
                                value = value.trim(),
                                stage = selectedStage,
                                score = leadScore.toInt()
                            )
                            isSaving = false
                            showAddLeadBottomSheet = false
                        },
                        modifier = Modifier.weight(1.5f).height(48.dp).testTag("save_lead_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save to Firestore", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
