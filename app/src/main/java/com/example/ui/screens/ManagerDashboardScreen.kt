package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.data.model.Department
import com.example.data.model.EmployeeEntity
import com.example.data.model.EmployeeStatus
import com.example.data.model.PresenceStatus
import com.example.data.model.CompanyProfile
import com.example.data.model.HolidayItem
import com.example.data.model.LeaveApplicationEntity
import com.example.data.model.MiloKnowledgeItem
import com.example.data.model.AuditLogEntity
import com.example.presentation.components.banner.AppOfferBanner
import com.example.presentation.components.banner.OfferBannerCard
import com.example.presentation.components.banner.OfferBannerSlider
import com.example.ui.components.AppHeader
import com.example.ui.components.MetricBadge
import com.example.ui.components.StatusIndicatorBadge
import com.example.ui.components.PulsingStatusDot
import com.example.ui.components.TeamWorkloadChartCard
import com.example.ui.components.PerformanceTrendsDashboardWidget
import com.example.ui.components.ScrollableActivityFeedWidget
import com.example.ui.components.DailyActivitySummaryWidget
import com.example.ui.components.MiloAdminCopilotWidget
import com.example.ui.components.SmartAttendanceGpsCard
import com.example.ui.components.AttendanceHeatmapWidget
import com.example.ui.components.OrganizationMasterOverviewCard
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
    onNavigateToVault: () -> Unit = {},
    onNavigateToCalls: () -> Unit = {},
    onNavigateToBannersAdmin: () -> Unit = {},
    onNavigateToBroadcastAdmin: () -> Unit = {},
    onNavigateToMiloAdmin: () -> Unit = {},
    onNavigateToClientWishes: () -> Unit = {}
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
    val userRole by viewModel.userRole.collectAsState()

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
    var selectedTab by remember { mutableStateOf("All") }

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

    // Admin Banners, Push Notifications & Milo MP4 States
    val coroutineScope = rememberCoroutineScope()
    var showAddBannerDialog by remember { mutableStateOf(false) }
    var showManageBannersDialog by remember { mutableStateOf(false) }
    var showPushNotificationDialog by remember { mutableStateOf(false) }
    var showMiloVideoConfigDialog by remember { mutableStateOf(false) }

    val allBanners by viewModel.banners.collectAsState()
    val activeBanners by viewModel.activeBanners.collectAsState()
    val isFirestoreBannersConnected by viewModel.isFirestoreBannersConnected.collectAsState()
    val adminBroadcasts by viewModel.adminBroadcasts.collectAsState()
    val holidays by viewModel.holidays.collectAsState()
    val miloKnowledgeList by viewModel.miloKnowledgeList.collectAsState()
    val companyProfile by viewModel.companyProfile.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState(initial = emptyList())

    var showLeaveManagementDialog by remember { mutableStateOf(false) }
    var showAuditLogDialog by remember { mutableStateOf(false) }
    var showHolidaysDialog by remember { mutableStateOf(false) }
    var showAddHolidayDialog by remember { mutableStateOf(false) }
    var showCompanyProfileDialog by remember { mutableStateOf(false) }
    var showMiloKnowledgeDialog by remember { mutableStateOf(false) }
    var showAddMiloKnowledgeDialog by remember { mutableStateOf(false) }
    var showMbEmConnectionHubDialog by remember { mutableStateOf(false) }
    var isPingingMbEm by remember { mutableStateOf(false) }

    val mbEmSyncState by viewModel.mbEmSyncState.collectAsState()
    val mbEmLatestEvent by viewModel.mbEmLatestSyncEvent.collectAsState()

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

    // 0C. Dialog: Upload & Add App Home Banner (Admin - Firebase Storage & Firestore)
    if (showAddBannerDialog) {
        var headline by remember { mutableStateOf("") }
        var subtext by remember { mutableStateOf("") }
        var ctaText by remember { mutableStateOf("Explore Now") }
        var badge by remember { mutableStateOf("SPECIAL OFFER") }
        var selectedRoute by remember { mutableStateOf("leads") }
        var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
        var savedImagePath by remember { mutableStateOf<String?>(null) }
        var isUploadingToFirebaseStorage by remember { mutableStateOf(false) }
        var storageUploadProgress by remember { mutableFloatStateOf(0f) }
        var isBannerActive by remember { mutableStateOf(true) }

        // Color theme palettes
        val colorPalettes = listOf(
            Triple("Teal Emerald", listOf(Color(0xFF0D9488), Color(0xFF10B981)), Color(0xFF047857)),
            Triple("Electric Blue", listOf(Color(0xFF1D4ED8), Color(0xFF3B82F6)), Color(0xFF1E40AF)),
            Triple("Sunset Orange", listOf(Color(0xFFEA580C), Color(0xFFF97316)), Color(0xFFC2410C)),
            Triple("Royal Purple", listOf(Color(0xFF7C3AED), Color(0xFF8B5CF6)), Color(0xFF6D28D9)),
            Triple("Dark Obsidian", listOf(Color(0xFF0F172A), Color(0xFF1E293B)), Color(0xFF2563EB))
        )
        var selectedPaletteIndex by remember { mutableStateOf(0) }

        val photoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                selectedImageUri = uri
                isUploadingToFirebaseStorage = true
                storageUploadProgress = 0.1f
                coroutineScope.launch {
                    val result = viewModel.uploadBannerImageToStorage(uri) { progress ->
                        storageUploadProgress = progress.coerceIn(0.1f, 1f)
                    }
                    if (result.isSuccess) {
                        savedImagePath = result.getOrNull()
                        isUploadingToFirebaseStorage = false
                        Toast.makeText(context, "Image uploaded to Firebase Storage!", Toast.LENGTH_SHORT).show()
                    } else {
                        savedImagePath = viewModel.saveBannerImage(uri)
                        isUploadingToFirebaseStorage = false
                        Toast.makeText(context, "Saved banner image locally (offline mode)", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showAddBannerDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .fillMaxHeight(0.88f)
                    .padding(vertical = 16.dp)
                    .clip(RoundedCornerShape(24.dp)),
                color = SurfaceBg,
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Surface(color = Color.White, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFEFF6FF), modifier = Modifier.size(38.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Add App Home Banner", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Upload promotional banner for employee dashboard", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                            IconButton(onClick = { showAddBannerDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                            }
                        }
                    }

                    // Body
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Image Upload Section
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Banner Graphic / Image", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                Text("Upload a high-resolution banner image from gallery or use styled card presets.", fontSize = 12.sp, color = TextSecondary)

                                if (isUploadingToFirebaseStorage) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFF0FDF4))
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF059669))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Uploading banner to Firebase Storage: ${(storageUploadProgress * 100).toInt()}%",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF065F46)
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { storageUploadProgress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = Color(0xFF059669),
                                            trackColor = Color(0xFFD1FAE5)
                                        )
                                    }
                                }

                                if (savedImagePath != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (savedImagePath?.startsWith("http") == true) Color(0xFFEFF6FF) else Color(0xFFF1F5F9)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                if (savedImagePath?.startsWith("http") == true) Icons.Default.CloudDone else Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = if (savedImagePath?.startsWith("http") == true) BrandBlue else Color(0xFF059669),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                if (savedImagePath?.startsWith("http") == true) "Uploaded to Firebase Storage (Cloud)" else "Saved to Local App Storage",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (savedImagePath?.startsWith("http") == true) BrandBlue else Color(0xFF059669)
                                            )
                                        }
                                    }
                                }

                                if (selectedImageUri != null || savedImagePath != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    ) {
                                        AsyncImage(
                                            model = savedImagePath ?: selectedImageUri,
                                            contentDescription = "Selected Banner Image",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.Black.copy(alpha = 0.65f),
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                                .clickable {
                                                    selectedImageUri = null
                                                    savedImagePath = null
                                                }
                                        ) {
                                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Remove", color = Color.White, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (selectedImageUri != null) Color(0xFF0F172A) else BrandBlue),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(42.dp)
                                ) {
                                    Icon(Icons.Default.Upload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (selectedImageUri != null) "Change Banner Image" else "Upload Image to Firebase Storage", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        // Banner Text & Action Section
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Banner Details & Actions", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)

                                OutlinedTextField(
                                    value = headline,
                                    onValueChange = { headline = it },
                                    label = { Text("Headline / Title *") },
                                    placeholder = { Text("e.g. Instant Lead Sync & Auto Follow-up") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                OutlinedTextField(
                                    value = subtext,
                                    onValueChange = { subtext = it },
                                    label = { Text("Subtext / Description") },
                                    placeholder = { Text("e.g. Get real-time Meta & Google leads in 15 seconds") },
                                    maxLines = 2,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = badge,
                                        onValueChange = { badge = it },
                                        label = { Text("Badge Label") },
                                        placeholder = { Text("OFFER") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    OutlinedTextField(
                                        value = ctaText,
                                        onValueChange = { ctaText = it },
                                        label = { Text("Button Text") },
                                        placeholder = { Text("Explore Now") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }

                                Text("CTA Destination Screen:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val routes = listOf(
                                        "leads" to "Leads / CRM",
                                        "tasks" to "Tasks",
                                        "invoices" to "Invoices",
                                        "chat" to "Team Chat",
                                        "milo_ai" to "Ask Milo AI",
                                        "attendance" to "Attendance"
                                    )
                                    items(routes) { (routeKey, routeLabel) ->
                                        FilterChip(
                                            selected = selectedRoute == routeKey,
                                            onClick = { selectedRoute = routeKey },
                                            label = { Text(routeLabel, fontSize = 12.sp) }
                                        )
                                    }
                                }

                                Text("Color Theme Palette:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(colorPalettes.indices.toList()) { index ->
                                        val palette = colorPalettes[index]
                                        FilterChip(
                                            selected = selectedPaletteIndex == index,
                                            onClick = { selectedPaletteIndex = index },
                                            label = { Text(palette.first, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }

                        // Visibility on Home Screen Toggle Card
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Show on Home Screen (Pager)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                    Text(
                                        if (isBannerActive) "Active: Will appear in home screen Pager slider" else "Paused: Hidden from home screen Pager",
                                        fontSize = 11.sp,
                                        color = if (isBannerActive) StatusGreen else StatusOrange
                                    )
                                }
                                Switch(
                                    checked = isBannerActive,
                                    onCheckedChange = { isBannerActive = it }
                                )
                            }
                        }

                        // Live Preview Card
                        Text("Live Preview on Home Screen:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        val currentPalette = colorPalettes[selectedPaletteIndex]
                        val previewBanner = AppOfferBanner(
                            id = "preview",
                            headline = headline.ifBlank { "Instant Lead Sync & Auto Follow-up" },
                            subtext = subtext.ifBlank { "Get real-time Meta & Google leads in 15 seconds" },
                            ctaText = ctaText.ifBlank { "Explore Now" },
                            badge = badge.ifBlank { "SPECIAL OFFER" },
                            bgGradientColors = currentPalette.second,
                            ctaButtonColor = currentPalette.third,
                            ctaTextColor = Color.White,
                            routeAction = selectedRoute,
                            imageUri = savedImagePath ?: selectedImageUri?.toString(),
                            isActive = isBannerActive
                        )
                        OfferBannerCard(
                            banner = previewBanner,
                            onClick = {}
                        )
                    }

                    // Footer
                    Surface(color = Color.White, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { showAddBannerDialog = false },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancel", color = TextSecondary, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    if (headline.isNotBlank()) {
                                        val palette = colorPalettes[selectedPaletteIndex]
                                        viewModel.addBanner(
                                            headline = headline,
                                            subtext = subtext,
                                            ctaText = ctaText,
                                            badge = badge,
                                            routeAction = selectedRoute,
                                            imageUri = savedImagePath ?: selectedImageUri?.toString(),
                                            bgGradientColors = palette.second,
                                            ctaButtonColor = palette.third,
                                            isActive = isBannerActive
                                        )
                                        showAddBannerDialog = false
                                        Toast.makeText(context, "Banner saved to Firestore 'Banners' & synced to Home Screen!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = headline.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                modifier = Modifier.weight(1.3f).height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Publish to Firestore", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    // 0D. Dialog: Manage Existing App Banners (Admin)
    if (showManageBannersDialog) {
        AlertDialog(
            onDismissRequest = { showManageBannersDialog = false },
            icon = { Icon(Icons.Default.ViewCarousel, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(36.dp)) },
            title = { Text("Manage App Home Banners", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${allBanners.size} Total Banners (${activeBanners.size} Active)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                        Button(
                            onClick = {
                                showManageBannersDialog = false
                                showAddBannerDialog = true
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("+ Add New", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    allBanners.forEach { banner ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    if (!banner.imageUri.isNullOrBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        ) {
                                            AsyncImage(
                                                model = banner.imageUri,
                                                contentDescription = banner.headline,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = banner.ctaButtonColor.copy(alpha = 0.15f),
                                            modifier = Modifier.size(46.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Image, contentDescription = null, tint = banner.ctaButtonColor, modifier = Modifier.size(22.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(banner.headline, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary, maxLines = 1)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(shape = RoundedCornerShape(4.dp), color = banner.ctaButtonColor.copy(alpha = 0.12f)) {
                                                Text(banner.badge ?: "OFFER", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = banner.ctaButtonColor, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                if (banner.imageUri?.startsWith("http") == true) "Firebase Storage" else "Local / Preset",
                                                fontSize = 9.sp,
                                                color = if (banner.imageUri?.startsWith("http") == true) BrandBlue else TextSecondary
                                            )
                                        }
                                        Text(
                                            if (banner.isActive) "Active in Home Pager" else "Hidden / Paused",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (banner.isActive) StatusGreen else StatusOrange
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = banner.isActive,
                                        onCheckedChange = { isChecked ->
                                            viewModel.toggleBannerStatus(banner.id, isChecked)
                                            val statusMsg = if (isChecked) "Banner active in Home Screen Pager" else "Banner hidden from Home Screen Pager"
                                            Toast.makeText(context, statusMsg, Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.scale(0.8f)
                                    )
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteBanner(banner.id)
                                            Toast.makeText(context, "Banner removed from Firestore 'Banners'", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Banner", tint = StatusRed, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showManageBannersDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 0D-2. Dialog: Admin Audit & Governance Log
    if (showAuditLogDialog) {
        var selectedFilter by remember { mutableStateOf("ALL") }
        var searchFilter by remember { mutableStateOf("") }
        val filteredLogs = remember(auditLogs, selectedFilter, searchFilter) {
            auditLogs.filter { log ->
                val matchesFilter = when (selectedFilter) {
                    "TASKS" -> log.action == "TASK_REASSIGN"
                    "ATTENDANCE" -> log.action == "ATTENDANCE_CHANGE"
                    "LEADS" -> log.action == "LEAD_STATUS_UPDATE"
                    else -> true
                }
                val matchesSearch = if (searchFilter.isBlank()) true else {
                    log.description.contains(searchFilter, ignoreCase = true) ||
                    log.performedBy.contains(searchFilter, ignoreCase = true) ||
                    log.action.contains(searchFilter, ignoreCase = true)
                }
                matchesFilter && matchesSearch
            }
        }
        val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

        AlertDialog(
            onDismissRequest = { showAuditLogDialog = false },
            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(36.dp)) },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Admin Audit & Governance Trail", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("${auditLogs.size} Total System Records • Immutable Trail", fontSize = 11.sp, color = TextSecondary)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchFilter,
                        onValueChange = { searchFilter = it },
                        placeholder = { Text("Search logs by user, action, note...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Filter chips row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" },
                            label = { Text("All (${auditLogs.size})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedFilter == "TASKS",
                            onClick = { selectedFilter = "TASKS" },
                            label = { Text("Tasks", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedFilter == "ATTENDANCE",
                            onClick = { selectedFilter = "ATTENDANCE" },
                            label = { Text("Attendance", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedFilter == "LEADS",
                            onClick = { selectedFilter = "LEADS" },
                            label = { Text("Leads", fontSize = 11.sp) }
                        )
                    }

                    if (filteredLogs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.FactCheck, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No audit logs match criteria", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Actions will be logged automatically as admins update tasks, attendance, or leads.", fontSize = 11.sp, color = TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredLogs, key = { it.id }) { log ->
                                val (icon, iconTint, bgTint) = when (log.action) {
                                    "TASK_REASSIGN" -> Triple(Icons.Default.AssignmentInd, Color(0xFF4F46E5), Color(0xFFEEF2FF))
                                    "ATTENDANCE_CHANGE" -> Triple(Icons.Default.Timer, Color(0xFFD97706), Color(0xFFFEF3C7))
                                    "LEAD_STATUS_UPDATE" -> Triple(Icons.Default.TrendingUp, Color(0xFF059669), Color(0xFFD1FAE5))
                                    else -> Triple(Icons.Default.Security, BrandBlue, Color(0xFFEFF6FF))
                                }
                                val actionLabel = when (log.action) {
                                    "TASK_REASSIGN" -> "Task Update"
                                    "ATTENDANCE_CHANGE" -> "Attendance"
                                    "LEAD_STATUS_UPDATE" -> "Lead Status"
                                    else -> log.action
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = bgTint,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(shape = RoundedCornerShape(4.dp), color = bgTint) {
                                                    Text(actionLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = iconTint, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                                Text(dateFormat.format(Date(log.timestamp)), fontSize = 10.sp, color = TextMuted)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(log.description, fontSize = 12.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Medium)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("By: ${log.performedBy}", fontSize = 11.sp, color = TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showAuditLogDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)) {
                    Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 0E. Dialog: Broadcast Push Notification (Admin)
    if (showPushNotificationDialog) {
        var notifTitle by remember { mutableStateOf("") }
        var notifMessage by remember { mutableStateOf("") }
        var selectedAudience by remember { mutableStateOf("All Employees") }
        var selectedPriority by remember { mutableStateOf("High") }
        var selectedCategory by remember { mutableStateOf("announcement") }

        AlertDialog(
            onDismissRequest = { showPushNotificationDialog = false },
            icon = { Icon(Icons.Default.Campaign, contentDescription = null, tint = StatusOrange, modifier = Modifier.size(36.dp)) },
            title = { Text("Broadcast Push Notification", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Broadcast an instant push alert to all registered employees. This triggers a real device system notification and adds to all in-app notification centers.",
                        fontSize = 12.sp,
                        color = Color(0xFF475569)
                    )

                    // Quick Templates
                    Text("Quick Templates:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val templates = listOf(
                            "📢 All-Hands Meeting" to "Urgent: Team All-Hands meeting starting in 15 minutes on Google Meet.",
                            "🎯 Target Announcement" to "Great job team! Q3 sales targets have been refreshed with bonus rewards.",
                            "⚡ Urgent CRM Review" to "Attention Sales: 18 incoming leads require immediate WhatsApp follow-up.",
                            "🎉 Company Notice" to "Company holiday scheduled for upcoming festival. Check holiday calendar."
                        )
                        items(templates) { (tTitle, tMsg) ->
                            FilterChip(
                                selected = notifTitle == tTitle,
                                onClick = {
                                    notifTitle = tTitle
                                    notifMessage = tMsg
                                },
                                label = { Text(tTitle, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = notifTitle,
                        onValueChange = { notifTitle = it },
                        label = { Text("Notification Title *") },
                        placeholder = { Text("e.g. Urgent All-Hands Call") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = notifMessage,
                        onValueChange = { notifMessage = it },
                        label = { Text("Notification Body / Message *") },
                        placeholder = { Text("Enter detailed broadcast announcement...") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Text("Target Audience:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val audiences = listOf("All Employees", "Sales & CRM", "Engineering", "Design", "Management")
                        items(audiences) { aud ->
                            FilterChip(
                                selected = selectedAudience == aud,
                                onClick = { selectedAudience = aud },
                                label = { Text(aud, fontSize = 11.sp) }
                            )
                        }
                    }

                    Text("Priority Level:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("High", "Urgent", "Normal").forEach { pr ->
                            FilterChip(
                                selected = selectedPriority == pr,
                                onClick = { selectedPriority = pr },
                                label = { Text(pr, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Live Preview Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEFF6FF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    notifTitle.ifBlank { "📢 Notification Title" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = BrandBlue
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Surface(shape = RoundedCornerShape(4.dp), color = BrandBlue) {
                                    Text(selectedAudience, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                notifMessage.ifBlank { "Live preview of the broadcast message that all employees will receive." },
                                fontSize = 11.sp,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (notifTitle.isNotBlank() && notifMessage.isNotBlank()) {
                            val targetTopic = when (selectedAudience) {
                                "Sales & CRM" -> "sales_team"
                                "Engineering" -> "dev_team"
                                "Design" -> "design_team"
                                "Management" -> "management"
                                else -> "all_users"
                            }
                            viewModel.sendAdminBroadcastPushNotification(
                                title = notifTitle,
                                message = notifMessage,
                                audience = selectedAudience,
                                priority = selectedPriority,
                                category = selectedCategory,
                                topic = targetTopic,
                                actionRoute = "notifications",
                                context = context
                            )
                            showPushNotificationDialog = false
                            Toast.makeText(context, "FCM Push Broadcast dispatched to $selectedAudience!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusOrange),
                    enabled = notifTitle.isNotBlank() && notifMessage.isNotBlank()
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Broadcast Now", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPushNotificationDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // 0F. Dialog: Milo MP4 Video Status Configuration
    if (showMiloVideoConfigDialog) {
        val videoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                val saved = MiloVideoHelper.saveMiloVideo(context, uri, "milo_status.mp4")
                if (saved) {
                    Toast.makeText(context, "Milo MP4 Video uploaded & active!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to save video file", Toast.LENGTH_SHORT).show()
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showMiloVideoConfigDialog = false },
            icon = { Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(36.dp)) },
            title = { Text("Real Milo Status & MP4 Video Setup", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MiloRealStatusView(
                            state = MiloState.WORKING,
                            size = 60.dp,
                            showStateBadge = true
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Real Milo Mascot Active", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                            Text("ExoPlayer CacheDataSource Active • Smooth MP4 Streaming", fontSize = 11.sp, color = StatusGreen)
                        }
                    }

                    Text(
                        "ExoPlayer streams MP4 files efficiently using a local cache data source (100MB LRU disk cache). MP4 animations are cached locally after first stream for instant zero-latency playback.",
                        fontSize = 12.sp,
                        color = Color(0xFF475569)
                    )

                    var customVideoUrlInput by remember { mutableStateOf(MiloVideoHelper.getMiloVideoUrl(context) ?: "") }

                    OutlinedTextField(
                        value = customVideoUrlInput,
                        onValueChange = { customVideoUrlInput = it },
                        label = { Text("Remote MP4 Stream URL", fontSize = 11.sp) },
                        placeholder = { Text("https://example.com/milo_anim.mp4", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                if (customVideoUrlInput.isNotBlank()) {
                                    MiloVideoHelper.saveMiloVideoUrl(context, customVideoUrlInput.trim())
                                    Toast.makeText(context, "Streaming URL saved! ExoPlayer caching enabled.", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.Check, contentDescription = "Save URL", tint = BrandBlue)
                            }
                        }
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Supported Video Sources (res/raw/ or Remote URL):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A))
                            Text("• milo_idle.mp4 (Default looping state)", fontSize = 11.sp, color = TextSecondary)
                            Text("• milo_thinking.mp4 (When AI query is processing)", fontSize = 11.sp, color = TextSecondary)
                            Text("• milo_working.mp4 (Active working status)", fontSize = 11.sp, color = TextSecondary)
                            Text("• milo_welcome.mp4 (Morning greetings)", fontSize = 11.sp, color = TextSecondary)
                            Text("• milo_splash.mp4 (App launch splash intro)", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    Button(
                        onClick = { videoPickerLauncher.launch("video/mp4") },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload MP4 Video File from Device", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showMiloVideoConfigDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))) {
                    Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
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
                    .fillMaxWidth(0.90f)
                    .fillMaxHeight(0.88f)
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(24.dp),
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

    // 🌟 Leave Applications & Admin Approval Management Dialog
    if (showLeaveManagementDialog) {
        var leaveFilter by remember { mutableStateOf("All") }
        val displayedLeaves = remember(leavesList, leaveFilter) {
            when (leaveFilter) {
                "Pending" -> leavesList.filter { it.status.equals("Pending", ignoreCase = true) }
                "Approved" -> leavesList.filter { it.status.equals("Approved", ignoreCase = true) }
                "Rejected" -> leavesList.filter { it.status.equals("Rejected", ignoreCase = true) }
                else -> leavesList
            }
        }

        AlertDialog(
            onDismissRequest = { showLeaveManagementDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Leave Applications", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFEFF6FF)) {
                        Text(
                            "${leavesList.size} Total",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)) {
                    // Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("All", "Pending", "Approved", "Rejected").forEach { filter ->
                            val isSel = leaveFilter == filter
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) BrandBlue else Color(0xFFF1F5F9),
                                modifier = Modifier.clickable { leaveFilter = filter }
                            ) {
                                Text(
                                    text = filter,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    if (displayedLeaves.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.EventAvailable, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("No $leaveFilter leave applications found", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(displayedLeaves, key = { it.id }) { item ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(item.username, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                                Text(item.leaveType, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = BrandBlue)
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = when (item.status) {
                                                    "Approved" -> Color(0xFFDCFCE7)
                                                    "Rejected" -> Color(0xFFFEE2E2)
                                                    else -> Color(0xFFFEF3C7)
                                                }
                                            ) {
                                                Text(
                                                    text = item.status,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (item.status) {
                                                        "Approved" -> Color(0xFF15803D)
                                                        "Rejected" -> Color(0xFFB91C1C)
                                                        else -> Color(0xFFB45309)
                                                    },
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            "📅 ${item.startDate} to ${item.endDate}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF475569)
                                        )
                                        if (item.reason.isNotBlank()) {
                                            Text(
                                                "Reason: ${item.reason}",
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (item.status != "Approved") {
                                                Button(
                                                    onClick = {
                                                        viewModel.updateLeaveStatus(item.id, "Approved")
                                                        Toast.makeText(context, "Leave approved for ${item.username}", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f).height(34.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Approve", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }

                                            if (item.status != "Rejected") {
                                                Button(
                                                    onClick = {
                                                        viewModel.updateLeaveStatus(item.id, "Rejected")
                                                        Toast.makeText(context, "Leave rejected for ${item.username}", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f).height(34.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Reject", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.deleteLeave(item)
                                                    Toast.makeText(context, "Leave record deleted", Toast.LENGTH_SHORT).show()
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(34.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StatusRed, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLeaveManagementDialog = false }) {
                    Text("Close", fontWeight = FontWeight.Bold, color = BrandBlue)
                }
            }
        )
    }

    // 🗓️ Company Holidays Management Dialog
    if (showHolidaysDialog) {
        AlertDialog(
            onDismissRequest = { showHolidaysDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Celebration, contentDescription = null, tint = StatusOrange, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Company Holidays", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    FilledTonalButton(
                        onClick = { showAddHolidayDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFEFF6FF), contentColor = BrandBlue),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    Text(
                        "Official company holiday calendar updated by Admin for all staff and Milo AI.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(holidays, key = { it.id }) { holiday ->
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
                                        Text(holiday.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                        Text("${holiday.date} (${holiday.day}) • ${holiday.type}", fontSize = 11.sp, color = BrandBlue)
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteHoliday(holiday.id)
                                            Toast.makeText(context, "Holiday removed", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StatusRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHolidaysDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        )
    }

    // ➕ Add New Holiday Dialog
    if (showAddHolidayDialog) {
        var holidayTitle by remember { mutableStateOf("") }
        var holidayDate by remember { mutableStateOf("") }
        var holidayDay by remember { mutableStateOf("Monday") }
        var holidayType by remember { mutableStateOf("Gazetted Holiday") }

        AlertDialog(
            onDismissRequest = { showAddHolidayDialog = false },
            title = { Text("Add Company Holiday", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = holidayTitle,
                        onValueChange = { holidayTitle = it },
                        label = { Text("Holiday Name / Occasion") },
                        placeholder = { Text("e.g. Diwali / Republic Day") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = holidayDate,
                        onValueChange = { holidayDate = it },
                        label = { Text("Date") },
                        placeholder = { Text("e.g. 15 Aug 2026") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = holidayDay,
                        onValueChange = { holidayDay = it },
                        label = { Text("Day of Week") },
                        placeholder = { Text("e.g. Saturday / Monday") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = holidayType,
                        onValueChange = { holidayType = it },
                        label = { Text("Holiday Type") },
                        placeholder = { Text("e.g. Gazetted Holiday / National Holiday") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (holidayTitle.isNotBlank() && holidayDate.isNotBlank()) {
                            viewModel.addHoliday(holidayTitle.trim(), holidayDate.trim(), holidayDay.trim(), holidayType.trim())
                            Toast.makeText(context, "Holiday '$holidayTitle' added", Toast.LENGTH_SHORT).show()
                            showAddHolidayDialog = false
                        } else {
                            Toast.makeText(context, "Please enter holiday title and date", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Save Holiday", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddHolidayDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // 🏢 Company Profile Editor Dialog
    if (showCompanyProfileDialog) {
        var compName by remember { mutableStateOf(companyProfile.companyName) }
        var compTagline by remember { mutableStateOf(companyProfile.tagline) }
        var compIndustry by remember { mutableStateOf(companyProfile.industry) }
        var compEmail by remember { mutableStateOf(companyProfile.email) }
        var compPhone by remember { mutableStateOf(companyProfile.phone) }
        var compWebsite by remember { mutableStateOf(companyProfile.website) }
        var compAddress by remember { mutableStateOf(companyProfile.address) }
        var compGst by remember { mutableStateOf(companyProfile.gstNumber) }
        var compBrochure by remember { mutableStateOf(companyProfile.brochureUrl) }
        var compOverview by remember { mutableStateOf(companyProfile.overview) }

        AlertDialog(
            onDismissRequest = { showCompanyProfileDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Business, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Company Profile & Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(value = compName, onValueChange = { compName = it }, label = { Text("Company Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = compTagline, onValueChange = { compTagline = it }, label = { Text("Tagline / Motto") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = compIndustry, onValueChange = { compIndustry = it }, label = { Text("Industry / Sector") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = compEmail, onValueChange = { compEmail = it }, label = { Text("Official Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = compPhone, onValueChange = { compPhone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = compWebsite, onValueChange = { compWebsite = it }, label = { Text("Official Website") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = compAddress, onValueChange = { compAddress = it }, label = { Text("Corporate Address") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = compGst, onValueChange = { compGst = it }, label = { Text("GSTIN / Tax ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = compBrochure, onValueChange = { compBrochure = it }, label = { Text("PDF Brochure / Profile Link") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = compOverview, onValueChange = { compOverview = it }, label = { Text("Company Overview & Bio") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = CompanyProfile(
                            companyName = compName.trim().ifBlank { "Making Brands Pvt Ltd" },
                            tagline = compTagline.trim(),
                            industry = compIndustry.trim(),
                            email = compEmail.trim(),
                            phone = compPhone.trim(),
                            website = compWebsite.trim(),
                            address = compAddress.trim(),
                            gstNumber = compGst.trim(),
                            brochureUrl = compBrochure.trim(),
                            overview = compOverview.trim()
                        )
                        viewModel.updateCompanyProfile(updated)
                        Toast.makeText(context, "Company profile updated successfully!", Toast.LENGTH_SHORT).show()
                        showCompanyProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompanyProfileDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // 🧠 Milo AI Knowledge Base & Brain Training Dialog
    if (showMiloKnowledgeDialog) {
        var knowledgeQuery by remember { mutableStateOf("") }
        val filteredKnowledge = remember(miloKnowledgeList, knowledgeQuery) {
            if (knowledgeQuery.isBlank()) miloKnowledgeList
            else miloKnowledgeList.filter {
                it.title.contains(knowledgeQuery, ignoreCase = true) ||
                it.category.contains(knowledgeQuery, ignoreCase = true) ||
                it.content.contains(knowledgeQuery, ignoreCase = true)
            }
        }

        AlertDialog(
            onDismissRequest = { showMiloKnowledgeDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Milo Knowledge Option", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    FilledTonalButton(
                        onClick = { showAddMiloKnowledgeDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFEFF6FF), contentColor = BrandBlue),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Add Topic", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)) {
                    Text(
                        "Configure company knowledge, policies, and FAQs that Milo uses to intelligently answer employee questions.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = knowledgeQuery,
                        onValueChange = { knowledgeQuery = it },
                        placeholder = { Text("Search knowledge articles...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredKnowledge, key = { it.id }) { item ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFEFF6FF),
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                Text(
                                                    item.category,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BrandBlue,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteMiloKnowledge(item.id)
                                                Toast.makeText(context, "Knowledge article deleted", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StatusRed, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        item.content,
                                        fontSize = 11.sp,
                                        color = Color(0xFF475569),
                                        lineHeight = 16.sp
                                    )

                                    if (item.tags.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            item.tags.forEach { tag ->
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFF1F5F9)
                                                ) {
                                                    Text(
                                                        "#$tag",
                                                        fontSize = 9.sp,
                                                        color = Color(0xFF64748B),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
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
            },
            confirmButton = {
                Button(
                    onClick = { showMiloKnowledgeDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        )
    }

    // ➕ Add Milo Knowledge Topic Dialog
    if (showAddMiloKnowledgeDialog) {
        var kTitle by remember { mutableStateOf("") }
        var kCategory by remember { mutableStateOf("HR Policies") }
        var kContent by remember { mutableStateOf("") }
        var kTags by remember { mutableStateOf("") }

        val categories = listOf("Company Profile", "HR Policies", "Sales & CRM", "Finance & Claims", "Tech SOPs", "Customer Support")

        AlertDialog(
            onDismissRequest = { showAddMiloKnowledgeDialog = false },
            title = { Text("Add Milo Knowledge Topic", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = kTitle,
                        onValueChange = { kTitle = it },
                        label = { Text("Topic Title / Question") },
                        placeholder = { Text("e.g. Leave Guidelines / Office Timings") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("Category:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        items(categories) { cat ->
                            val isSel = kCategory == cat
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) BrandBlue else Color(0xFFF1F5F9),
                                modifier = Modifier.clickable { kCategory = cat }
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = kContent,
                        onValueChange = { kContent = it },
                        label = { Text("Knowledge Information / Answer") },
                        placeholder = { Text("Enter the policy, rule, FAQ answer, or company information...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    OutlinedTextField(
                        value = kTags,
                        onValueChange = { kTags = it },
                        label = { Text("Tags (comma separated)") },
                        placeholder = { Text("e.g. Leave, Vacation, Policy") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (kTitle.isNotBlank() && kContent.isNotBlank()) {
                            val tagList = kTags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            viewModel.addMiloKnowledge(kTitle.trim(), kCategory, kContent.trim(), tagList)
                            Toast.makeText(context, "Knowledge topic added for Milo!", Toast.LENGTH_SHORT).show()
                            showAddMiloKnowledgeDialog = false
                        } else {
                            Toast.makeText(context, "Please fill in title and knowledge content", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Save Knowledge", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMiloKnowledgeDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // 🔗 MB EM Employee App Realtime Cloud Connection Hub & Credentials Dialog
    if (showMbEmConnectionHubDialog) {
        AlertDialog(
            onDismissRequest = { showMbEmConnectionHubDialog = false },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFDCFCE7),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("MB EM App Connection Hub", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("Real-Time Synchronization with Employee Terminal", fontSize = 11.sp, color = TextSecondary)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Status Badge Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0FDF4),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PulsingStatusDot(
                                color = Color(0xFF16A34A),
                                dotSize = 10.dp,
                                haloExpansion = 6.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Connected & Live",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF166534)
                                )
                                Text(
                                    mbEmLatestEvent.ifBlank { "All snapshot listeners actively streaming." },
                                    fontSize = 11.sp,
                                    color = Color(0xFF15803D),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    // Shared Cloud Architecture Info
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Cloud Firestore Architecture:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                            Text("• Database: Dual Sync (Custom Project DB & Default Instance)", fontSize = 11.sp, color = TextSecondary)
                            Text("• Protocol: Low-latency WebSockets & gRPC Snapshots", fontSize = 11.sp, color = TextSecondary)
                            Text("• Target Platform: 'MB EM' Employee Android App", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    // Synchronized Collections Checklist
                    Text("Synchronized Real-Time Channels:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF334155))
                    listOf(
                        "📋 Tasks & Task Records (Instant Assignment & Progress)" to "tasks, task_records",
                        "⏰ Attendance Roster (Live Clock-In/Out & Geotags)" to "attendance_records, attendance",
                        "👥 Employees & Presence (Active, On Break, Offline)" to "employees, users",
                        "🏖️ Leave Approvals (Realtime Decision Sync)" to "leave_applications, leaves",
                        "💬 Team & Channel Chat (Live Messaging & Audio)" to "chat_messages, messages",
                        "📢 Push Broadcasts & System Alerts" to "notifications"
                    ).forEach { (label, collections) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text("Firestore: $collections", fontSize = 9.sp, color = BrandBlue)
                                }
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Ping Test Action
                    Button(
                        onClick = {
                            isPingingMbEm = true
                            viewModel.sendMbEmSyncPing { success, msg ->
                                isPingingMbEm = false
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isPingingMbEm,
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        if (isPingingMbEm) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Broadcasting Ping...", color = Color.White, fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.WifiTethering, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Live Sync Ping with MB EM", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showMbEmConnectionHubDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.reconnectMbEmSync()
                        Toast.makeText(context, "Re-attaching all active MB EM listeners...", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Force Re-Sync", color = BrandBlue, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "MB Admin",
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
            // 🏷️ HORIZONTAL MENU FOR EASY NAVIGATION JUMPS
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val tabs = listOf(
                        "All" to Icons.Default.Dashboard,
                        "Milo AI" to Icons.Default.Psychology,
                        "Offers" to Icons.Default.LocalOffer,
                        "Control Panel" to Icons.Default.Tune,
                        "AI Copilot" to Icons.Default.AutoAwesome,
                        "GPS Map" to Icons.Default.LocationOn,
                        "Sync Hub" to Icons.Default.CloudSync,
                        "Approvals" to Icons.Default.VerifiedUser,
                        "Heatmap" to Icons.Default.GridOn
                    )
                    items(tabs) { (tab, icon) ->
                        FilterChip(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            leadingIcon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedTab == tab) Color.White else BrandBlue
                                )
                            },
                            label = { Text(tab, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandBlue,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = TextSecondary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // 🦁 MILO PROACTIVE TIME-OF-DAY GREETING & SMART ASSISTANT CARD
            if (selectedTab == "All" || selectedTab == "Milo AI") {
                item {
                    MiloGreetingCard(
                        viewModel = viewModel,
                        onOpenAskMilo = { showMiloAssistant = true },
                        onNavigateToTasks = onNavigateToTasks,
                        onNavigateToLeads = onNavigateToLeads
                    )
                }
            }

            // 🎁 EXCLUSIVE APP OFFERS & PROMOTIONS SLIDER (Directly after Milo Greeting Card)
            if (selectedTab == "All" || selectedTab == "Offers") {
                item {
                    val liveBanners by viewModel.activeBanners.collectAsState()
                    OfferBannerSlider(
                        banners = liveBanners,
                        onBannerClick = { banner ->
                            when (banner.routeAction) {
                                "leads" -> onNavigateToLeads()
                                "tasks" -> onNavigateToTasks()
                                "projects" -> onNavigateToProjects()
                                "chat" -> onNavigateToChat()
                                "calls" -> onNavigateToCalls()
                                "milo_ai" -> {
                                    showMiloAssistant = true
                                    viewModel.miloViewModel.handleEvent(MiloEvent.Thinking("Special Offers & CRM Deals"))
                                }
                                else -> onNavigateToLeads()
                            }
                        }
                    )
                }
            }

            // 🔒 SECURE ADMIN CRUD EMPLOYEE APPROVALS PORTAL
            if ((selectedTab == "All" || selectedTab == "Approvals") && userRole == "MB Admin") {
                item {
                    EmployeeApprovalsManagerCard(
                        viewModel = viewModel,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            // 🏢 Organization Master Control & Overview (Workforce, Leads, Drafts, Festivals & Wishes)
            if (selectedTab == "All" || selectedTab == "Control Panel") {
                item {
                    OrganizationMasterOverviewCard(
                        viewModel = viewModel,
                        onNavigateToClientWishes = onNavigateToClientWishes,
                        onNavigateToLeads = onNavigateToLeads,
                        onNavigateToTasks = onNavigateToTasks,
                        onNavigateToAttendance = onNavigateToTimesheets,
                        onNavigateToMilo = onNavigateToMiloAdmin
                    )
                }
            }

            // 🦁 Milo Executive AI Copilot Suite (Autonomous Admin Actions & Intelligence)
            if (selectedTab == "All" || selectedTab == "AI Copilot") {
                item {
                    MiloAdminCopilotWidget(
                        viewModel = viewModel,
                        onNavigateToTasks = onNavigateToTasks,
                        onNavigateToAttendance = onNavigateToTimesheets,
                        onNavigateToLeads = onNavigateToLeads,
                        onNavigateToInvoices = { /* Invoices */ }
                    )
                }
            }

            // 📍 Smart Attendance Realtime GPS Location & Firebase Sync Card
            if (selectedTab == "All" || selectedTab == "GPS Map") {
                item {
                    SmartAttendanceGpsCard(
                        viewModel = viewModel,
                        onNavigateToAttendance = onNavigateToTimesheets
                    )
                }
            }

            // 📊 D3 Attendance Weekly Heatmap
            if (selectedTab == "All" || selectedTab == "Heatmap") {
                item {
                    AttendanceHeatmapWidget(
                        viewModel = viewModel,
                        onNavigateToAttendance = onNavigateToTimesheets
                    )
                }
            }

            // 🔗 MB EM Employee App Realtime Cloud Connection Terminal
            if (selectedTab == "All" || selectedTab == "Sync Hub") {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
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
                                        shape = CircleShape,
                                        color = Color(0xFFDCFCE7),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.CloudSync,
                                                contentDescription = null,
                                                tint = Color(0xFF16A34A),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            "MB EM Employee App Sync",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            "Real-time Cloud Terminal & Live Firestore Listeners",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFDCFCE7)) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PulsingStatusDot(
                                            color = Color(0xFF16A34A),
                                            dotSize = 6.dp,
                                            haloExpansion = 4.dp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "CONNECTED",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF166534)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Live Sync Metrics Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val activeEmpCount = employees.count { it.status == EmployeeStatus.ACTIVE }
                                val onlineEmpCount = employees.count { it.presenceStatus != PresenceStatus.OFFLINE }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Online Staff", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("$onlineEmpCount", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                                            Text("/$activeEmpCount", fontSize = 11.sp, color = TextSecondary)
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Synced Tasks", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                                        Text("${tasks.size}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Streams Active", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                                        Text("${mbEmSyncState.activeListenersCount.coerceAtLeast(1)} Live", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Live Event Banner
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF0FDF4),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("⚡", fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = mbEmLatestEvent.ifBlank { "Synchronized with 'MB EM' mobile application." },
                                        fontSize = 11.sp,
                                        color = Color(0xFF166534),
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        isPingingMbEm = true
                                        viewModel.sendMbEmSyncPing { _, msg ->
                                            isPingingMbEm = false
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = !isPingingMbEm,
                                    modifier = Modifier.weight(1f).height(42.dp)
                                ) {
                                    if (isPingingMbEm) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.WifiTethering, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Test Ping", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                    }
                                }

                                FilledTonalButton(
                                    onClick = { showMbEmConnectionHubDialog = true },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Color(0xFFEFF6FF),
                                        contentColor = BrandBlue
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1.3f).height(42.dp)
                                ) {
                                    Icon(Icons.Default.SettingsEthernet, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sync Hub & Info", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = BrandBlue)
                                }
                            }
                        }
                    }
                }
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

            // ⚡ Recharts / D3 Performance Trends Widget (Average Task Completion Time for All Employees)
            item {
                PerformanceTrendsDashboardWidget(
                    viewModel = viewModel,
                    onNavigateToTasks = onNavigateToTasks
                )
            }

            // 📋 Scrollable Unified Employee Activity Feed (Real-time Chronological Stream of Actions)
            item {
                ScrollableActivityFeedWidget(
                    viewModel = viewModel,
                    onNavigateToTasks = onNavigateToTasks,
                    onNavigateToAttendance = onNavigateToTimesheets,
                    onNavigateToChat = onNavigateToChat
                )
            }

            // 📊 Daily Activity Summary (Aggregated Attendance, Completed Tasks & CRM Interactions)
            item {
                DailyActivitySummaryWidget(
                    viewModel = viewModel,
                    onNavigateToAttendance = onNavigateToTimesheets,
                    onNavigateToTasks = onNavigateToTasks,
                    onNavigateToCRM = onNavigateToLeads
                )
            }

            // 📢 1. App Home Screen Banners Management Card (Admin Panel - Firebase Firestore & Storage)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                                Surface(shape = CircleShape, color = Color(0xFFEFF6FF), modifier = Modifier.size(38.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.ViewCarousel, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(22.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Home Screen Banners", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color(0xFF0F172A))
                                    Text("Firestore 'Banners' Collection & Storage", fontSize = 11.sp, color = TextSecondary)
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isFirestoreBannersConnected) Color(0xFFECFDF5) else Color(0xFFFFFBEB)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isFirestoreBannersConnected) {
                                            PulsingStatusDot(
                                                color = StatusGreen,
                                                dotSize = 6.dp,
                                                haloExpansion = 4.dp
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(StatusOrange)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            if (isFirestoreBannersConnected) "Firestore Live" else "Cached Mode",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isFirestoreBannersConnected) Color(0xFF065F46) else Color(0xFF92400E)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFEFF6FF)) {
                                    Text(
                                        "${activeBanners.size} Active in Pager",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandBlue,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Inline quick toggle list of banners with display order & visibility
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Banners Order & Home Visibility:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            TextButton(
                                onClick = onNavigateToBannersAdmin,
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("Open Full Panel", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp), tint = BrandBlue)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            allBanners.take(5).forEachIndexed { index, banner ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                            // Order badge & reorder buttons
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (banner.isActive) BrandBlue else Color(0xFF64748B),
                                                modifier = Modifier.padding(end = 4.dp)
                                            ) {
                                                Text(
                                                    text = "#${banner.displayOrder + 1}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }

                                            // Reorder buttons
                                            Column {
                                                IconButton(
                                                    onClick = { viewModel.moveBannerUp(banner.id) },
                                                    enabled = index > 0,
                                                    modifier = Modifier.size(18.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.ArrowDropUp,
                                                        contentDescription = "Move Up",
                                                        tint = if (index > 0) BrandBlue else Color(0xFFCBD5E1),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { viewModel.moveBannerDown(banner.id) },
                                                    enabled = index < allBanners.size - 1,
                                                    modifier = Modifier.size(18.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.ArrowDropDown,
                                                        contentDescription = "Move Down",
                                                        tint = if (index < allBanners.size - 1) BrandBlue else Color(0xFFCBD5E1),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(6.dp))

                                            if (!banner.imageUri.isNullOrBlank()) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                ) {
                                                    AsyncImage(
                                                        model = banner.imageUri,
                                                        contentDescription = banner.headline,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                            } else {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = banner.ctaButtonColor.copy(alpha = 0.15f),
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(Icons.Default.Image, contentDescription = null, tint = banner.ctaButtonColor, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(banner.headline, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextPrimary, maxLines = 1)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(banner.badge ?: "OFFER", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = banner.ctaButtonColor)
                                                    Text(" • ", fontSize = 8.sp, color = TextSecondary)
                                                    Text(
                                                        if (banner.imageUri?.startsWith("http") == true) "Firebase Storage" else "Preset",
                                                        fontSize = 8.sp,
                                                        color = if (banner.imageUri?.startsWith("http") == true) BrandBlue else TextSecondary
                                                    )
                                                    Text(" • ", fontSize = 8.sp, color = TextSecondary)
                                                    Text(
                                                        if (banner.isActive) "Visible" else "Hidden",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (banner.isActive) StatusGreen else StatusOrange
                                                    )
                                                }
                                            }
                                        }

                                        Switch(
                                            checked = banner.isActive,
                                            onCheckedChange = { isChecked ->
                                                viewModel.toggleBannerStatus(banner.id, isChecked)
                                                val msg = if (isChecked) "Banner activated in Home Screen Pager" else "Banner hidden from Home Screen Pager"
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.scale(0.75f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showAddBannerDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(42.dp)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Upload", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                            }

                            FilledTonalButton(
                                onClick = onNavigateToBannersAdmin,
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFEFF6FF), contentColor = BrandBlue),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.3f).height(42.dp)
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Admin Panel (${allBanners.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = BrandBlue)
                            }

                            OutlinedButton(
                                onClick = { showManageBannersDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(0.9f).height(42.dp)
                            ) {
                                Text("Quick List", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextPrimary)
                            }
                        }
                    }
                }
            }

            // 📢 2. Enterprise Push Notifications Broadcast Card (Admin Panel - FCM & Cloud Alerts)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFED7AA)),
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
                                Surface(shape = CircleShape, color = Color(0xFFFFF7ED), modifier = Modifier.size(36.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Campaign, contentDescription = null, tint = StatusOrange, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Broadcast Push Notification", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color(0xFF0F172A))
                                    Text("FCM Cloud Messaging to all devices & topics", fontSize = 11.sp, color = TextSecondary)
                                }
                            }

                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFDCFCE7)) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PulsingStatusDot(
                                        color = StatusGreen,
                                        dotSize = 6.dp,
                                        haloExpansion = 4.dp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "FCM LIVE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF166534)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Dispatches high-priority system alerts via Firebase Cloud Messaging (FCM) to all registered employees with custom audience filtering.",
                            fontSize = 12.sp,
                            color = Color(0xFF475569)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { showPushNotificationDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = StatusOrange),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Quick Send", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                            }

                            FilledTonalButton(
                                onClick = onNavigateToBroadcastAdmin,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFFFFF7ED),
                                    contentColor = StatusOrange
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.2f).height(44.dp)
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = StatusOrange, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Full FCM Panel", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StatusOrange)
                            }
                        }

                        if (adminBroadcasts.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Recent Broadcasts (${adminBroadcasts.size}):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            adminBroadcasts.take(2).forEach { b ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF8FAFC),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                ) {
                                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("📢", fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(b.title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextPrimary)
                                            Text("${b.audience} • ${b.priority} priority", fontSize = 10.sp, color = TextSecondary)
                                        }
                                        Text("FCM Sent", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 🦁 3. Real Milo Mascot & MP4 Video Status Manager Card (Admin Panel)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MiloRealStatusView(
                                    state = MiloState.WELCOME,
                                    size = 46.dp,
                                    showStateBadge = true
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Real Milo Mascot & Video", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color(0xFF0F172A))
                                    Text("Vector graphics removed • MP4 loops ready", fontSize = 11.sp, color = StatusGreen)
                                }
                            }

                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFDCFCE7)) {
                                Text(
                                    "MP4 READY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Milo status renders clean high-fidelity photography and automatically plays looping MP4 video files when placed into res/raw/ or uploaded via device.",
                            fontSize = 12.sp,
                            color = Color(0xFF475569)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onNavigateToMiloAdmin,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Milo AI & MP4 Video Admin Panel", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }

            // 📋 4. Employee Leave Applications & Approvals Card (Admin Panel)
            item {
                val pendingLeavesCount = leavesList.count { it.status.equals("Pending", ignoreCase = true) }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (pendingLeavesCount > 0) Color(0xFFFDE68A) else Color(0xFFE2E8F0)),
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
                                Surface(shape = CircleShape, color = Color(0xFFFEF3C7), modifier = Modifier.size(36.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.EventBusy, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Employee Leave Approvals", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color(0xFF0F172A))
                                    Text("Review & approve staff leave applications", fontSize = 11.sp, color = TextSecondary)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (pendingLeavesCount > 0) Color(0xFFFEF3C7) else Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    if (pendingLeavesCount > 0) "$pendingLeavesCount PENDING" else "ALL CLEAR",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pendingLeavesCount > 0) Color(0xFFB45309) else Color(0xFF15803D),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (leavesList.isEmpty()) {
                            Text("No employee leave applications currently submitted.", fontSize = 12.sp, color = TextSecondary)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                leavesList.take(3).forEach { leaveItem ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(leaveItem.username, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                                    Text("${leaveItem.leaveType} • ${leaveItem.startDate} to ${leaveItem.endDate}", fontSize = 11.sp, color = BrandBlue)
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = when (leaveItem.status) {
                                                        "Approved" -> Color(0xFFDCFCE7)
                                                        "Rejected" -> Color(0xFFFEE2E2)
                                                        else -> Color(0xFFFEF3C7)
                                                    }
                                                ) {
                                                    Text(
                                                        leaveItem.status,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = when (leaveItem.status) {
                                                            "Approved" -> Color(0xFF15803D)
                                                            "Rejected" -> Color(0xFFB91C1C)
                                                            else -> Color(0xFFB45309)
                                                        },
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            if (leaveItem.reason.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Reason: ${leaveItem.reason}", fontSize = 11.sp, color = Color(0xFF64748B))
                                            }

                                            if (leaveItem.status.equals("Pending", ignoreCase = true)) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                                    Button(
                                                        onClick = {
                                                            viewModel.updateLeaveStatus(leaveItem.id, "Approved")
                                                            Toast.makeText(context, "Leave approved for ${leaveItem.username}", Toast.LENGTH_SHORT).show()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1f).height(32.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Approve", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    }

                                                    OutlinedButton(
                                                        onClick = {
                                                            viewModel.updateLeaveStatus(leaveItem.id, "Rejected")
                                                            Toast.makeText(context, "Leave rejected for ${leaveItem.username}", Toast.LENGTH_SHORT).show()
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1f).height(32.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Icon(Icons.Default.Close, contentDescription = null, tint = StatusRed, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Reject", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusRed)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { showLeaveManagementDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Icon(Icons.Default.FactCheck, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Manage & Review All Leaves (${leavesList.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }

            // 🛡️ 4B. Enterprise Audit Trail & Governance (Admin Audit Logs)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC7D2FE)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = CircleShape, color = Color(0xFFEEF2FF), modifier = Modifier.size(36.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Audit Trail & Governance", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color(0xFF0F172A))
                                    Text("Task reassignments, attendance, lead status logs", fontSize = 11.sp, color = TextSecondary)
                                }
                            }

                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFEEF2FF)) {
                                Text(
                                    "${auditLogs.size} EVENTS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4F46E5),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Every critical administrative mutation is permanently tracked with timestamps, actor names, and previous/new states to maintain full accountability.",
                            fontSize = 12.sp,
                            color = Color(0xFF475569)
                        )

                        if (auditLogs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Recent System Activity:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            auditLogs.take(2).forEach { log ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF8FAFC),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                ) {
                                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("🛡️", fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(log.description, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextPrimary)
                                            Text("By ${log.performedBy} • ${SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(log.timestamp))}", fontSize = 10.sp, color = TextSecondary)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { showAuditLogDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Audit & Compliance Log (${auditLogs.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }

            // 🗓️ 5. Company Holidays Update Card (Admin Panel)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                                Surface(shape = CircleShape, color = Color(0xFFFFF7ED), modifier = Modifier.size(36.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Celebration, contentDescription = null, tint = StatusOrange, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Company Holidays Update", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color(0xFF0F172A))
                                    Text("${holidays.size} Official Holidays Configured", fontSize = 11.sp, color = TextSecondary)
                                }
                            }

                            FilledTonalButton(
                                onClick = { showAddHolidayDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFEFF6FF), contentColor = BrandBlue),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Add Holiday", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Next 3 Holidays Preview
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            holidays.take(3).forEach { h ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(h.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                            Text("${h.date} (${h.day}) • ${h.type}", fontSize = 10.sp, color = TextSecondary)
                                        }
                                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFDCFCE7)) {
                                            Text(
                                                "OFFICIAL",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF15803D),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { showHolidaysDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusOrange),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Update & Manage Holidays Calendar", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }

            // 🧠 6. Knowledge Option for MILO AI Card (Admin Panel)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDD6FE)),
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
                                Surface(shape = CircleShape, color = Color(0xFFEDE9FE), modifier = Modifier.size(36.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Psychology, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Knowledge Option for MILO", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color(0xFF0F172A))
                                    Text("${miloKnowledgeList.size} AI Knowledge & FAQ entries", fontSize = 11.sp, color = TextSecondary)
                                }
                            }

                            FilledTonalButton(
                                onClick = { showAddMiloKnowledgeDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFEDE9FE), contentColor = Color(0xFF7C3AED)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Add Topic", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Train Milo AI with company policies, SLAs, FAQs, and SOPs so it can intelligently guide your team members in Ask Milo.",
                            fontSize = 12.sp,
                            color = Color(0xFF475569)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Preview of recent 2 Knowledge entries
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            miloKnowledgeList.take(2).forEach { k ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(k.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                            Text(k.category, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
                                        }
                                        Text(k.content, fontSize = 10.sp, color = TextSecondary, maxLines = 1)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { showMiloKnowledgeDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Icon(Icons.Default.AutoStories, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Milo AI Knowledge Base", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }

            // 🏢 7. Company Profile Card (Admin Panel)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
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
                                Surface(shape = CircleShape, color = Color(0xFFEFF6FF), modifier = Modifier.size(36.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Business, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Company Profile & Information", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color(0xFF0F172A))
                                    Text("Enterprise corporate details & brochure", fontSize = 11.sp, color = TextSecondary)
                                }
                            }

                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFDCFCE7)) {
                                Text(
                                    "ACTIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(companyProfile.companyName, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = BrandBlue)
                                if (companyProfile.tagline.isNotBlank()) {
                                    Text("\"${companyProfile.tagline}\"", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
                                }
                                Text("🏢 ${companyProfile.industry}", fontSize = 11.sp, color = TextPrimary)
                                Text("📞 ${companyProfile.phone}  •  ✉️ ${companyProfile.email}", fontSize = 11.sp, color = TextSecondary)
                                Text("🌐 ${companyProfile.website}", fontSize = 11.sp, color = BrandBlue)
                                if (companyProfile.address.isNotBlank()) {
                                    Text("📍 ${companyProfile.address}", fontSize = 10.sp, color = Color(0xFF64748B))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { showCompanyProfileDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit & Update Company Profile", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }

            // 🎨 8. App Logo & Launcher Icon Customizer Card (Admin Panel)
            item {
                AdminAppLogoManagerCard()
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
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Live Field Map", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    item {
                        Button(
                            onClick = onNavigateToTimesheets,
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonSecondary),
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
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonSecondary),
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
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Document Vault", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    item {
                        Button(
                            onClick = onNavigateToCalls,
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonSecondary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PhoneCallback, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Call Logs", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
        MiloSmartAssistantSheet(
            viewModel = viewModel,
            onDismiss = { showMiloAssistant = false },
            onNavigateToLeads = onNavigateToLeads,
            onNavigateToTasks = onNavigateToTasks,
            onNavigateToCalls = onNavigateToCalls
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

@Composable
fun AdminAppLogoManagerCard() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var refreshKey by remember { mutableStateOf(0) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var currentLogoUri by remember(refreshKey) { mutableStateOf(com.example.util.AppPreferences.getCustomAppLogoUri(context)) }
    var currentBgHex by remember(refreshKey) { mutableStateOf(com.example.util.AppPreferences.getAppIconBgColor(context)) }
    var currentPreset by remember(refreshKey) { mutableStateOf(com.example.util.AppPreferences.getAppLogoPreset(context)) }

    // Secure Document/Content File Picker for PNG/Image files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isUploading = true
            uploadProgress = 0.1f
            Toast.makeText(context, "Uploading PNG branding image to Firebase Storage...", Toast.LENGTH_SHORT).show()

            coroutineScope.launch {
                val result = com.example.data.firebase.FirebaseStorageManager.uploadAppLogoImage(
                    context = context,
                    imageUri = uri,
                    bgHexColor = currentBgHex,
                    onProgress = { p -> uploadProgress = p }
                )

                isUploading = false
                if (result.isSuccess) {
                    refreshKey++
                    Toast.makeText(context, "✅ PNG Logo uploaded to Firebase Storage & persisted in Firestore!", Toast.LENGTH_LONG).show()
                } else {
                    val fallbackPath = com.example.util.AppIconHelper.saveCustomLogoFromUri(context, uri)
                    if (fallbackPath != null) {
                        refreshKey++
                        Toast.makeText(context, "✅ PNG Logo saved to App Preferences!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to upload or save PNG logo image.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Android Visual Media Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isUploading = true
            uploadProgress = 0.1f
            Toast.makeText(context, "Uploading custom logo to Firebase Storage...", Toast.LENGTH_SHORT).show()

            coroutineScope.launch {
                val result = com.example.data.firebase.FirebaseStorageManager.uploadAppLogoImage(
                    context = context,
                    imageUri = uri,
                    bgHexColor = currentBgHex,
                    onProgress = { p -> uploadProgress = p }
                )

                isUploading = false
                if (result.isSuccess) {
                    refreshKey++
                    Toast.makeText(context, "✅ App Logo uploaded to Firebase Storage & saved to Firestore!", Toast.LENGTH_LONG).show()
                } else {
                    val fallbackPath = com.example.util.AppIconHelper.saveCustomLogoFromUri(context, uri)
                    if (fallbackPath != null) {
                        refreshKey++
                        Toast.makeText(context, "✅ Logo saved locally to App Preferences!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to upload or save logo image.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEFF6FF),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("App Logo & Launcher Icon", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color(0xFF0F172A))
                        Text("Upload custom app logo image or select brand emblem", fontSize = 11.sp, color = TextSecondary)
                    }
                }

                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFDBEAFE)) {
                    Text(
                        "APP ICON",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Current App Logo & Icon Live Preview Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Active App Logo & Launcher Icon", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF334155))
                            Text(
                                if (currentLogoUri != null) "Custom Image Upload Active (Firebase Storage)" else "Preset: $currentPreset",
                                fontSize = 11.sp,
                                color = BrandBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Applies to app headers, login, splash & app launcher icon", fontSize = 10.sp, color = Color(0xFF64748B))
                        }

                        // Live Logo Emblem
                        key(refreshKey) {
                            com.example.ui.components.MBAppLogo(size = 52.dp, showText = false)
                        }
                    }

                    if (isUploading) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { uploadProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = BrandBlue,
                            trackColor = Color(0xFFDBEAFE)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Uploading to Firebase Storage & updating Firestore config...",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Secure File Picker (PNG) & Photo Gallery & Reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button 1: Secure PNG Document File Picker
                Button(
                    onClick = { filePickerLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(42.dp)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select PNG File", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                }

                // Button 2: Photo Gallery Picker
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gallery", fontSize = 12.sp, color = BrandBlue, fontWeight = FontWeight.Bold)
                }

                if (currentLogoUri != null || currentPreset != "MILO_LION") {
                    OutlinedButton(
                        onClick = {
                            com.example.util.AppIconHelper.resetToDefaultLogo(context)
                            refreshKey++
                            Toast.makeText(context, "Reset to Default Mascot Logo!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // App Icon Container Background Color Tint Picker
            Text("App Icon Background Color Tint", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF334155))
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "#0F172A" to "Slate Dark",
                    "#1E3A8A" to "Royal Blue",
                    "#000000" to "Midnight Black",
                    "#059669" to "Emerald Green",
                    "#B45309" to "Amber Gold"
                ).forEach { (hex, label) ->
                    val colorObj = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.DarkGray }
                    val isSelected = currentBgHex.equals(hex, ignoreCase = true)

                    Surface(
                        shape = CircleShape,
                        color = colorObj,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.5.dp, BrandBlue) else androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                        shadowElevation = if (isSelected) 4.dp else 1.dp,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable {
                                com.example.util.AppPreferences.saveAppIconBgColor(context, hex)
                                refreshKey++
                                Toast.makeText(context, "Icon background tint: $label", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        if (isSelected) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Executive Employee Management & Registration Approvals Card.
 * Allows the authenticated administrator user to view, approve, or delete newly registered employees.
 */
@Composable
fun EmployeeApprovalsManagerCard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val employees by viewModel.employees.collectAsState(initial = emptyList())
    val context = LocalContext.current

    val unapprovedEmployees = remember(employees) {
        employees.filter { it.status == com.example.data.model.EmployeeStatus.INACTIVE }
    }
    val approvedEmployees = remember(employees) {
        employees.filter { it.status != com.example.data.model.EmployeeStatus.INACTIVE }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
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
                        color = Color(0xFFEFF6FF),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = BrandBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "Employee Approvals Portal",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            "Authorize registration & directory",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (unapprovedEmployees.isNotEmpty()) Color(0xFFFEE2E2) else Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = "${unapprovedEmployees.size} PENDING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (unapprovedEmployees.isNotEmpty()) StatusRed else TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (unapprovedEmployees.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "All employees are verified",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            "New signups will appear here for approval.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                Text(
                    "Newly Registered (Pending Approval):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    unapprovedEmployees.forEach { emp ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFFBEB),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        emp.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        "${emp.designation} · ${emp.email}",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.updateEmployee(emp.copy(status = com.example.data.model.EmployeeStatus.ACTIVE))
                                            viewModel.recordAndBroadcastNotification(
                                                title = "✅ Employee Approved: ${emp.name}",
                                                subtitle = "Employee account has been approved by Admin.",
                                                category = "changes"
                                            )
                                            Toast.makeText(context, "Approved ${emp.name} successfully!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Approve", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.deleteEmployee(emp)
                                            Toast.makeText(context, "Deleted ${emp.name} successfully!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = StatusRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            // Show general employee directory shortcut and CRUD controls
            Text(
                "Active Corporate Directory: ${approvedEmployees.size} Members",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF475569),
                modifier = Modifier.padding(bottom = 6.dp)
            )

            if (approvedEmployees.isEmpty()) {
                Text("No active employees currently enrolled.", fontSize = 11.sp, color = TextSecondary)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    approvedEmployees.forEach { emp ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        emp.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        "${emp.designation} · ${emp.email}",
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteEmployee(emp)
                                            Toast.makeText(context, "Deleted active employee: ${emp.name}", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete Active Employee",
                                            tint = StatusRed,
                                            modifier = Modifier.size(16.dp)
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
