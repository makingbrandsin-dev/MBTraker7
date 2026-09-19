package com.example.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirebaseRealtimeManager
import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val employeeDao = db.employeeDao()
    private val attendanceDao = db.attendanceDao()
    private val projectDao = db.projectDao()
    private val taskDao = db.taskDao()
    private val leadDao = db.leadDao()
    private val followUpDao = db.followUpDao()
    private val callLogDao = db.callLogDao()
    private val chatDao = db.chatDao()
    private val notificationDao = db.notificationDao()
    private val userProfileDao = db.userProfileDao()
    private val leaveDao = db.leaveDao()
    private val leadSourceDao = db.leadSourceDao()
    private val callRecordingDao = db.callRecordingDao()
    private val invoiceDao = db.invoiceDao()
    private val quotationDao = db.quotationDao()
    private val autoBrochureDao = db.autoBrochureDao()
    private val socialReviewDao = db.socialReviewDao()

    // Leaves
    val leaves = leaveDao.getAllLeaves()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Lead Sources
    val leadSourceConfigs = leadSourceDao.getAllSourceConfigs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Call Recordings
    val callRecordings = callRecordingDao.getAllRecordings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val callRecordingsCount = callRecordingDao.getRecordingsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Invoices & Quotations
    val invoices = invoiceDao.getAllInvoices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quotations = quotationDao.getAllQuotations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Auto Company Profile PDF Brochure Config
    val autoBrochureConfig = autoBrochureDao.getAutoBrochureConfig()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            AutoBrochureConfigEntity()
        )

    // Social Reviews and QR Configs
    val socialReviews = socialReviewDao.getAllReviewConfigs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Employees
    val employees = employeeDao.getAllEmployees()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val employeeCount = employeeDao.getEmployeeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    // Realtime attendance timer state
    val latestAttendance = attendanceDao.getLatestAttendance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allAttendance = attendanceDao.getAllAttendance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Firebase Realtime Status
    val syncState = FirebaseRealtimeManager.syncState
    val isFirebaseConnected = FirebaseRealtimeManager.isRealtimeConnected
    val firebaseSyncStatus = FirebaseRealtimeManager.syncStatus

    fun triggerManualSync() {
        FirebaseRealtimeManager.syncNow(viewModelScope)
    }

    fun setOfflineMode(forceOffline: Boolean) {
        FirebaseRealtimeManager.toggleSimulatedOffline(forceOffline)
    }

    private val _liveActiveDurationSeconds = MutableStateFlow(0L) // Default 0 when starting day fresh
    val liveActiveDurationSeconds: StateFlow<Long> = _liveActiveDurationSeconds.asStateFlow()

    // ⏰ Dynamic Live Clock state synced with device time
    private val _liveClockTimeMillis = MutableStateFlow(System.currentTimeMillis())
    val liveClockTimeMillis: StateFlow<Long> = _liveClockTimeMillis.asStateFlow()

    // ☕ Break Management State
    private val _isOnBreak = MutableStateFlow(false)
    val isOnBreak: StateFlow<Boolean> = _isOnBreak.asStateFlow()

    private val _currentBreakType = MutableStateFlow("None")
    val currentBreakType: StateFlow<String> = _currentBreakType.asStateFlow()

    private val _liveBreakDurationSeconds = MutableStateFlow(0L)
    val liveBreakDurationSeconds: StateFlow<Long> = _liveBreakDurationSeconds.asStateFlow()

    // 💬 WhatsApp Quick Incoming Chat Alert & Floating Chat Overlay State
    private val _isQuickChatOpen = MutableStateFlow(false)
    val isQuickChatOpen: StateFlow<Boolean> = _isQuickChatOpen.asStateFlow()

    private val _unreadChatCount = MutableStateFlow(3)
    val unreadChatCount: StateFlow<Int> = _unreadChatCount.asStateFlow()

    fun openQuickChat() {
        _isQuickChatOpen.value = true
        _unreadChatCount.value = 0
    }

    fun closeQuickChat() {
        _isQuickChatOpen.value = false
    }

    fun startBreak(breakType: String = "Tea Break") {
        val current = latestAttendance.value
        if (current != null && current.isWorking) {
            _isOnBreak.value = true
            _currentBreakType.value = breakType
            viewModelScope.launch {
                val updated = current.copy(
                    isOnBreak = true,
                    breakType = breakType,
                    status = "On Break ($breakType)"
                )
                attendanceDao.update(updated)
                FirebaseRealtimeManager.syncAttendanceToFirebase(updated)
                notificationDao.insert(
                    NotificationEntity(
                        title = "☕ Break Started",
                        subtitle = "$breakType started at ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())}",
                        timeAgo = "Just now",
                        category = "attendance",
                        isRead = false
                    )
                )
            }
        }
    }

    fun resumeFromBreak() {
        val current = latestAttendance.value
        if (current != null && current.isWorking) {
            val breakMins = (_liveBreakDurationSeconds.value / 60).coerceAtLeast(1)
            _isOnBreak.value = false
            val lastBreakType = _currentBreakType.value
            _currentBreakType.value = "None"
            viewModelScope.launch {
                val updated = current.copy(
                    isOnBreak = false,
                    breakMinutes = current.breakMinutes + breakMins,
                    status = "Present"
                )
                attendanceDao.update(updated)
                FirebaseRealtimeManager.syncAttendanceToFirebase(updated)
                notificationDao.insert(
                    NotificationEntity(
                        title = "▶ Work Resumed",
                        subtitle = "Returned from $lastBreakType. Total break logged: ${breakMins}m",
                        timeAgo = "Just now",
                        category = "attendance",
                        isRead = false
                    )
                )
            }
        }
    }

    fun toggleBreak(breakType: String = "Tea Break") {
        if (_isOnBreak.value) {
            resumeFromBreak()
        } else {
            startBreak(breakType)
        }
    }

    fun exportDailyPerformanceSlip(dateText: String) {
        viewModelScope.launch {
            notificationDao.insert(
                NotificationEntity(
                    title = "📄 Daily Performance Slip Generated",
                    subtitle = "Performance report for $dateText exported successfully (Score: 97%).",
                    timeAgo = "Just now",
                    category = "attendance",
                    isRead = false
                )
            )
        }
    }

    fun exportMonthlyPerformanceReport(monthText: String) {
        viewModelScope.launch {
            notificationDao.insert(
                NotificationEntity(
                    title = "📊 Monthly Performance Sheet Exported",
                    subtitle = "Comprehensive executive report for $monthText generated & ready for download.",
                    timeAgo = "Just now",
                    category = "attendance",
                    isRead = false
                )
            )
        }
    }

    private var timerJob: Job? = null

    // Projects & Tasks
    val projects = projectDao.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks = taskDao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingTaskCount = taskDao.getPendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    val completedTaskCount = taskDao.getCompletedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    // Leads & Followups & Calls
    val leads = leadDao.getAllLeads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val followUps = followUpDao.getAllFollowUps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val callLogs = callLogDao.getAllCallLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currency Preference ("INR" vs "USD") - Default INR ("₹")
    private val _currencyCode = MutableStateFlow("INR")
    val currencyCode: StateFlow<String> = _currencyCode.asStateFlow()

    val currencySymbol: StateFlow<String> = _currencyCode.map { code ->
        if (code == "USD") "$" else "₹"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "₹")

    fun setCurrency(code: String) {
        _currencyCode.value = code
    }

    // Chat
    private val _currentChannel = MutableStateFlow("dev_team")
    val currentChannel: StateFlow<String> = _currentChannel.asStateFlow()

    val chatMessages = _currentChannel.flatMapLatest { channelId ->
        chatDao.getMessagesForChannel(channelId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications
    val notifications = notificationDao.getAllNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationCount = notificationDao.getUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            notificationDao.markAllAsRead()
        }
    }

    fun markNotificationAsRead(id: Long) {
        viewModelScope.launch {
            notificationDao.markAsRead(id)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            notificationDao.clearAll()
        }
    }

    fun addNotification(title: String, subtitle: String, category: String) {
        viewModelScope.launch {
            notificationDao.insert(
                NotificationEntity(
                    title = title,
                    subtitle = subtitle,
                    timeAgo = "Just now",
                    category = category,
                    isRead = false
                )
            )
        }
    }

    // Auth state for demo login & OTP
    val isLoggedIn = MutableStateFlow(true) // Start directly into app or switch via drawer/profile
    val isOtpSent = MutableStateFlow(false)

    // Current logged-in employee profile (persisted in Room user_profile table)
    val userProfile = userProfileDao.getUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentEmployeeName = MutableStateFlow("Rahul Sharma")
    val currentEmployeeRole = MutableStateFlow("Senior Developer")

    // First time onboarding / first check-in popup state
    val showFirstTimeCheckInDialog = MutableStateFlow(false)

    fun dismissFirstTimeDialog() {
        showFirstTimeCheckInDialog.value = false
        viewModelScope.launch {
            // Persist as onboarded in Room so user is not prompted again
            val profile = UserProfileEntity(
                id = 1L,
                name = currentEmployeeName.value,
                role = currentEmployeeRole.value,
                isOnboarded = true,
                updatedAt = System.currentTimeMillis()
            )
            userProfileDao.insertOrUpdateProfile(profile)
        }
    }

    fun completeFirstTimeCheckIn(name: String, role: String) {
        val trimmedName = name.trim().ifEmpty { "Rahul Sharma" }
        val trimmedRole = role.trim().ifEmpty { "Senior Developer" }
        currentEmployeeName.value = trimmedName
        currentEmployeeRole.value = trimmedRole
        showFirstTimeCheckInDialog.value = false

        viewModelScope.launch {
            // 1. Persist to Room UserProfileDao
            val profile = UserProfileEntity(
                id = 1L,
                name = trimmedName,
                role = trimmedRole,
                isOnboarded = true,
                updatedAt = System.currentTimeMillis()
            )
            userProfileDao.insertOrUpdateProfile(profile)
            FirebaseRealtimeManager.syncProfileToFirebase(profile)

            // 2. Keep employee directory in sync if employee id = 1 exists
            val emp = employeeDao.getEmployeeById(1L).first()
            if (emp != null) {
                employeeDao.update(emp.copy(name = trimmedName, designation = trimmedRole))
            }

            // 3. Automatically check in for the employee in Room AttendanceDao
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val nowTimeStr = timeFormat.format(Date())
            val nowDateStr = dateFormat.format(Date())

            val current = latestAttendance.value
            if (current == null || !current.isWorking) {
                val newRecord = AttendanceRecord(
                    date = nowDateStr,
                    checkInTime = nowTimeStr,
                    checkOutTime = null,
                    durationMinutes = 0,
                    isWorking = true,
                    status = "Present",
                    overtimeMinutes = 0,
                    timestamp = System.currentTimeMillis(),
                    employeeName = trimmedName
                )
                val id = attendanceDao.insert(newRecord)
                FirebaseRealtimeManager.syncAttendanceToFirebase(newRecord.copy(id = id))
                _liveActiveDurationSeconds.value = 0
            }
        }
    }

    fun updateEmployeeProfile(name: String, role: String) {
        val trimmedName = name.trim().ifEmpty { currentEmployeeName.value }
        val trimmedRole = role.trim().ifEmpty { currentEmployeeRole.value }
        currentEmployeeName.value = trimmedName
        currentEmployeeRole.value = trimmedRole

        viewModelScope.launch {
            val profile = UserProfileEntity(
                id = 1L,
                name = trimmedName,
                role = trimmedRole,
                isOnboarded = true,
                updatedAt = System.currentTimeMillis()
            )
            userProfileDao.insertOrUpdateProfile(profile)
            FirebaseRealtimeManager.syncProfileToFirebase(profile)

            val emp = employeeDao.getEmployeeById(1L).first()
            if (emp != null) {
                employeeDao.update(emp.copy(name = trimmedName, designation = trimmedRole))
            }
        }
    }

    init {
        // Initialize Firebase Realtime Manager for continuous synchronization
        FirebaseRealtimeManager.initialize(
            application.applicationContext,
            attendanceDao,
            userProfileDao,
            taskDao,
            leadDao,
            viewModelScope
        )

        // Collect Room user profile to restore persisted name, role, and onboarding state
        viewModelScope.launch {
            userProfileDao.getUserProfile().collect { profile ->
                if (profile != null && profile.isOnboarded) {
                    currentEmployeeName.value = profile.name
                    currentEmployeeRole.value = profile.role
                    showFirstTimeCheckInDialog.value = false
                } else {
                    // Not onboarded yet -> show onboarding popup
                    showFirstTimeCheckInDialog.value = true
                }
            }
        }

        // Start realtime clock ticker
        startTimerTicker()
    }

    private fun startTimerTicker() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _liveClockTimeMillis.value = System.currentTimeMillis()
                val currentRec = latestAttendance.value
                if (currentRec != null && currentRec.isWorking) {
                    if (_isOnBreak.value) {
                        _liveBreakDurationSeconds.value += 1
                    } else {
                        // Dynamically adjust with elapsed timestamp if available for precise sync
                        if (currentRec.timestamp > 0) {
                            val elapsedSec = ((System.currentTimeMillis() - currentRec.timestamp) / 1000).coerceAtLeast(0)
                            val activeSec = (elapsedSec - (currentRec.breakMinutes * 60) - _liveBreakDurationSeconds.value).coerceAtLeast(0)
                            _liveActiveDurationSeconds.value = activeSec
                        } else {
                            _liveActiveDurationSeconds.value += 1
                        }
                    }
                }
            }
        }
    }

    private val _attendanceSnackbarMessage = MutableStateFlow<String?>(null)
    val attendanceSnackbarMessage: StateFlow<String?> = _attendanceSnackbarMessage.asStateFlow()

    fun clearAttendanceSnackbarMessage() {
        _attendanceSnackbarMessage.value = null
    }

    fun checkInUser() {
        viewModelScope.launch {
            val now = Date()
            val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val nowTimeStr = timeFormat.format(now)
            val nowDateStr = dateFormat.format(now)
            val currentTimestamp = now.time

            val newRecord = AttendanceRecord(
                date = nowDateStr,
                checkInTime = nowTimeStr,
                checkOutTime = null,
                durationMinutes = 0,
                isWorking = true,
                status = "Present",
                overtimeMinutes = 0,
                timestamp = currentTimestamp,
                employeeName = currentEmployeeName.value
            )
            val recordId = attendanceDao.insert(newRecord)
            val inserted = newRecord.copy(id = recordId)
            FirebaseRealtimeManager.syncAttendanceToFirebase(inserted)
            _liveActiveDurationSeconds.value = 0
            notificationDao.insert(
                NotificationEntity(
                    title = "Punch In Recorded",
                    subtitle = "Checked in at $nowTimeStr",
                    timeAgo = "Just now",
                    category = "attendance",
                    isRead = false
                )
            )
        }
    }

    fun checkOutUser() {
        viewModelScope.launch {
            val current = latestAttendance.value
            val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
            val nowTimeStr = timeFormat.format(Date())

            if (current != null && current.isWorking) {
                val updated = current.copy(
                    checkOutTime = nowTimeStr,
                    isWorking = false,
                    durationMinutes = _liveActiveDurationSeconds.value / 60
                )
                attendanceDao.update(updated)
                FirebaseRealtimeManager.syncAttendanceToFirebase(updated)
                notificationDao.insert(
                    NotificationEntity(
                        title = "Punch Out Recorded",
                        subtitle = "Checked out at $nowTimeStr",
                        timeAgo = "Just now",
                        category = "attendance",
                        isRead = false
                    )
                )
            }
        }
    }

    fun toggleCheckInCheckOut() {
        val current = latestAttendance.value
        if (current != null && current.isWorking) {
            checkOutUser()
        } else {
            checkInUser()
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted, status = if (!task.isCompleted) "Completed" else "In Progress")
            taskDao.update(updated)
            FirebaseRealtimeManager.syncTaskToFirebase(updated)
            notificationDao.insert(
                NotificationEntity(
                    title = if (updated.isCompleted) "Task Completed" else "Task Updated",
                    subtitle = "'${task.title}' marked as ${updated.status}",
                    timeAgo = "Just now",
                    category = "task",
                    isRead = false
                )
            )
        }
    }

    fun updateTaskStatus(task: TaskEntity, newStatus: String) {
        viewModelScope.launch {
            val isComp = newStatus.equals("Completed", ignoreCase = true)
            val updated = task.copy(status = newStatus, isCompleted = isComp)
            taskDao.update(updated)
            FirebaseRealtimeManager.syncTaskToFirebase(updated)
            notificationDao.insert(
                NotificationEntity(
                    title = "Task Status Updated",
                    subtitle = "'${task.title}' status changed to $newStatus",
                    timeAgo = "Just now",
                    category = "task",
                    isRead = false
                )
            )
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskDao.delete(task)
            FirebaseRealtimeManager.deleteTaskFromFirebase(task.id)
            notificationDao.insert(
                NotificationEntity(
                    title = "Task Deleted",
                    subtitle = "'${task.title}' was deleted",
                    timeAgo = "Just now",
                    category = "task",
                    isRead = false
                )
            )
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            taskDao.update(task)
            FirebaseRealtimeManager.syncTaskToFirebase(task)
            notificationDao.insert(
                NotificationEntity(
                    title = "Task Updated",
                    subtitle = "'${task.title}' was updated",
                    timeAgo = "Just now",
                    category = "task",
                    isRead = false
                )
            )
        }
    }

    fun addTask(
        title: String,
        projectName: String,
        priority: String,
        dueDate: String,
        category: String = "Work",
        estimatedTimeNeeded: String = "4 Hours"
    ) {
        viewModelScope.launch {
            val newTask = TaskEntity(
                title = title,
                projectName = projectName,
                priority = priority,
                dueDate = dueDate,
                status = "Backlog",
                isCompleted = false,
                category = category,
                estimatedTimeNeeded = estimatedTimeNeeded
            )
            val id = taskDao.insert(newTask)
            FirebaseRealtimeManager.syncTaskToFirebase(newTask.copy(id = id))
            notificationDao.insert(
                NotificationEntity(
                    title = "New Task Assigned",
                    subtitle = "[$category] '$title' created for $projectName (Est: $estimatedTimeNeeded)",
                    timeAgo = "Just now",
                    category = "task",
                    isRead = false
                )
            )
        }
    }

    fun addLead(
        name: String,
        company: String,
        phone: String,
        email: String = "",
        requirement: String = "",
        value: String = "₹ 2,00,000",
        stage: String = "New",
        score: Int = (70..95).random(),
        source: String = "Website"
    ) {
        viewModelScope.launch {
            val newLead = LeadEntity(
                name = name,
                company = company,
                phone = phone,
                email = email,
                leadScore = score,
                requirement = requirement,
                potentialValue = value,
                stage = stage,
                assignedTo = currentEmployeeName.value,
                nextFollowUp = "Tomorrow, 10:00 AM",
                source = source
            )
            val id = leadDao.insert(newLead)
            FirebaseRealtimeManager.syncLeadToFirebase(newLead.copy(id = id))
            notificationDao.insert(
                NotificationEntity(
                    title = "New Lead Added via $source",
                    subtitle = "Lead '$name' ($company) added to CRM pipeline",
                    timeAgo = "Just now",
                    category = "followup",
                    isRead = false
                )
            )

            // Automated Company Profile PDF Brochure dispatch
            val brochureCfg = autoBrochureConfig.value
            if (brochureCfg != null && brochureCfg.isAutoSendEnabled) {
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val nowStr = "Today, " + timeFormat.format(Date())
                autoBrochureDao.incrementSentCount(nowStr)

                val channels = mutableListOf<String>()
                if (brochureCfg.sendViaWhatsApp) channels.add("WhatsApp ($phone)")
                if (brochureCfg.sendViaEmail && email.isNotBlank()) channels.add("Email ($email)")
                val channelStr = if (channels.isEmpty()) "WhatsApp/Email" else channels.joinToString(" & ")

                notificationDao.insert(
                    NotificationEntity(
                        title = "📄 Company Profile PDF Auto-Sent",
                        subtitle = "Sent '${brochureCfg.brochureFileName}' to $name ($company) via $channelStr",
                        timeAgo = "Just now",
                        category = "followup",
                        isRead = false
                    )
                )
            }
        }
    }

    fun deleteLead(lead: LeadEntity) {
        viewModelScope.launch {
            leadDao.delete(lead)
        }
    }

    fun updateLeadSourceConfig(config: LeadSourceConfigEntity) {
        viewModelScope.launch {
            leadSourceDao.update(config)
            notificationDao.insert(
                NotificationEntity(
                    title = "Lead Source Updated",
                    subtitle = "${config.displayName} settings saved",
                    timeAgo = "Just now",
                    category = "project",
                    isRead = false
                )
            )
        }
    }

    fun addCustomLeadSource(displayName: String, apiKey: String, webhookUrl: String) {
        viewModelScope.launch {
            val sourceId = "custom_" + System.currentTimeMillis()
            val config = LeadSourceConfigEntity(
                sourceId = sourceId,
                displayName = displayName,
                isEnabled = true,
                apiKey = apiKey,
                webhookUrl = webhookUrl,
                defaultAssignee = currentEmployeeName.value,
                lastSyncTime = "Just now",
                totalLeadsIngested = 0
            )
            leadSourceDao.insert(config)
        }
    }

    fun simulateSyncPlatformLeads(sourceId: String) {
        viewModelScope.launch {
            val config = leadSourceConfigs.value.find { it.sourceId == sourceId } ?: return@launch
            val platformName = config.displayName.replace(" Leads API", "").replace(" Lead Ads", "").replace(" Webhook", "").replace(" Contact Form API", "").replace(" Business Ingestion", "").replace(" Cloud API", "")

            val sampleContacts = listOf(
                Pair("Sunil Mehta", "Mehta Agro Tech"),
                Pair("Pooja Nair", "Kavya Infotech"),
                Pair("Aman Singhal", "Singhal Steel Traders"),
                Pair("Ritu Sen", "Sen Healthcare Labs")
            )
            val selectedContact = sampleContacts.random()
            val randomPhone = "+91 98" + (10000000..99999999).random()
            val randomVal = listOf("₹ 1,50,000", "₹ 2,80,000", "₹ 4,50,000", "$ 3,200", "₹ 95,000").random()

            val newLead = LeadEntity(
                name = selectedContact.first,
                company = selectedContact.second,
                phone = randomPhone,
                email = selectedContact.first.lowercase().replace(" ", "") + "@example.com",
                leadScore = (75..98).random(),
                requirement = "Inquired via $platformName API Integration",
                potentialValue = randomVal,
                stage = "New",
                assignedTo = config.defaultAssignee,
                nextFollowUp = "Today, 5:30 PM",
                source = platformName
            )
            val id = leadDao.insert(newLead)
            FirebaseRealtimeManager.syncLeadToFirebase(newLead.copy(id = id))

            val updatedConfig = config.copy(
                lastSyncTime = "Just now",
                totalLeadsIngested = config.totalLeadsIngested + 1
            )
            leadSourceDao.update(updatedConfig)

            notificationDao.insert(
                NotificationEntity(
                    title = "⚡ Live Lead Ingested",
                    subtitle = "New lead from $platformName: ${selectedContact.first} (${selectedContact.second})",
                    timeAgo = "Just now",
                    category = "followup",
                    isRead = false
                )
            )
        }
    }

    fun updateLeadStage(lead: LeadEntity, newStage: String) {
        viewModelScope.launch {
            val updated = lead.copy(stage = newStage)
            leadDao.update(updated)
            FirebaseRealtimeManager.syncLeadToFirebase(updated)
        }
    }

    fun addCallLog(contactName: String, phone: String, type: String, duration: String) {
        viewModelScope.launch {
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            callLogDao.insert(
                CallLogEntity(
                    contactName = contactName,
                    callType = type,
                    status = "Connected",
                    timestampText = "Today, " + timeFormat.format(Date()),
                    durationText = duration,
                    phoneNumber = phone
                )
            )
        }
    }

    fun selectChatChannel(channelId: String) {
        _currentChannel.value = channelId
    }

    fun sendChatMessage(text: String, fileName: String? = null, fileSize: String? = null) {
        if (text.isBlank() && fileName == null) return
        viewModelScope.launch {
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val currentSender = currentEmployeeName.value.ifBlank { "Rahul Sharma" }
            val currentSenderRole = currentEmployeeRole.value.ifBlank { "Senior Developer" }
            val message = ChatMessageEntity(
                channelId = _currentChannel.value,
                senderName = currentSender,
                senderRole = currentSenderRole,
                messageText = text,
                timestampText = timeFormat.format(Date()),
                isMe = true,
                attachmentFileName = fileName,
                attachmentFileSize = fileSize
            )
            chatDao.insert(message)

            // Dynamic instant teammate auto-reply with high speed (sub-second)
            delay(900)
            val randomReplier = listOf(
                Pair("Arjun Mehta", "Team Lead"),
                Pair("Priya Singh", "Project Manager"),
                Pair("Vikram Rao", "Tech Architect"),
                Pair("Rohit Verma", "Product Lead")
            ).random()

            val replyText = when {
                text.contains("meeting", ignoreCase = true) || text.contains("📍", ignoreCase = true) -> "Got it, connecting on Google Meet now 👍"
                text.contains("break", ignoreCase = true) || text.contains("☕", ignoreCase = true) -> "Enjoy your break! We'll cover the pending review."
                text.contains("done", ignoreCase = true) || text.contains("✅", ignoreCase = true) -> "Awesome work! QA build has been deployed."
                text.contains("working", ignoreCase = true) || text.contains("🚀", ignoreCase = true) -> "Great speed, let us know if you need any API sync."
                text.contains("ack", ignoreCase = true) || text.contains("👍", ignoreCase = true) -> "Perfect. Thanks for the quick update!"
                else -> "Received: \"$text\". Updated in today's live feed!"
            }

            chatDao.insert(
                ChatMessageEntity(
                    channelId = _currentChannel.value,
                    senderName = randomReplier.first,
                    senderRole = randomReplier.second,
                    messageText = replyText,
                    timestampText = timeFormat.format(Date()),
                    isMe = false
                )
            )
        }
    }

    // --- Employee & Project Room Operations ---
    fun addEmployee(
        name: String,
        email: String,
        phone: String,
        designation: String,
        department: Department = Department.ENGINEERING,
        role: EmployeeRole = EmployeeRole.DEVELOPER,
        skills: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            employeeDao.insert(
                EmployeeEntity(
                    name = name,
                    email = email,
                    phone = phone,
                    designation = designation,
                    department = department,
                    role = role,
                    status = EmployeeStatus.ACTIVE,
                    joiningDate = Date(),
                    skills = skills
                )
            )
        }
    }

    fun updateEmployee(employee: EmployeeEntity) {
        viewModelScope.launch {
            employeeDao.update(employee)
        }
    }

    fun deleteEmployee(employee: EmployeeEntity) {
        viewModelScope.launch {
            employeeDao.delete(employee)
        }
    }

    fun addProject(
        name: String,
        clientName: String,
        deadline: String,
        priority: String = "High",
        tags: List<String> = emptyList(),
        assignedEmployeeIds: List<Long> = emptyList(),
        budget: Double? = 200000.0
    ) {
        viewModelScope.launch {
            projectDao.insert(
                ProjectEntity(
                    name = name,
                    clientName = clientName,
                    totalTasks = 0,
                    completedTasks = 0,
                    progressPercent = 0,
                    status = "Active",
                    priority = priority,
                    deadline = deadline,
                    tags = tags,
                    assignedEmployeeIds = assignedEmployeeIds,
                    budget = budget
                )
            )
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            projectDao.delete(project)
        }
    }

    fun applyLeave(
        leaveType: String,
        startDate: String,
        endDate: String,
        totalDays: Int = 1,
        reason: String = "",
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val username = currentEmployeeName.value
            val todayStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
            val leave = LeaveApplicationEntity(
                username = username,
                leaveType = leaveType,
                startDate = startDate,
                endDate = endDate,
                totalDays = totalDays,
                reason = reason,
                status = "Pending",
                appliedDate = todayStr,
                createdAt = System.currentTimeMillis()
            )
            leaveDao.insert(leave)
            notificationDao.insert(
                NotificationEntity(
                    title = "Leave Request Submitted",
                    subtitle = "$leaveType ($startDate to $endDate) submitted by $username",
                    timeAgo = "Just now",
                    category = "leave"
                )
            )
            onComplete()
        }
    }

    fun deleteLeave(leave: LeaveApplicationEntity) {
        viewModelScope.launch {
            leaveDao.delete(leave)
        }
    }

    // --- Call Recorder Operations (SIM & WhatsApp calls saved to local DB) ---
    fun addCallRecording(
        contactName: String,
        phoneNumber: String,
        callMedium: String = "SIM Call", // "SIM Call", "WhatsApp Call"
        callType: String = "Outgoing",
        durationText: String = "02:30",
        durationSeconds: Long = 150,
        transcriptionSnippet: String = "Call recorded successfully and encrypted locally."
    ) {
        viewModelScope.launch {
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val recordedAtStr = "Today, " + timeFormat.format(Date())
            val sanitized = contactName.lowercase().replace(" ", "_").replace("(", "").replace(")", "")
            val mediumPrefix = if (callMedium.contains("WhatsApp", ignoreCase = true)) "wa" else "sim"
            val filePath = "/recordings/${mediumPrefix}_rec_${sanitized}_${System.currentTimeMillis() % 10000}.m4a"
            val fileSize = String.format(Locale.US, "%.1f MB", (durationSeconds * 0.012).coerceAtLeast(0.8))

            val recording = CallRecordingEntity(
                contactName = contactName,
                phoneNumber = phoneNumber,
                callMedium = callMedium,
                callType = callType,
                durationText = durationText,
                durationSeconds = durationSeconds,
                recordedAt = recordedAtStr,
                filePath = filePath,
                fileSizeText = fileSize,
                transcriptionSnippet = transcriptionSnippet,
                isAutoSaved = true
            )
            callRecordingDao.insert(recording)

            // Also mirror in call log
            callLogDao.insert(
                CallLogEntity(
                    contactName = contactName,
                    callType = callType,
                    status = "Connected",
                    timestampText = recordedAtStr,
                    durationText = durationText,
                    phoneNumber = phoneNumber
                )
            )

            notificationDao.insert(
                NotificationEntity(
                    title = "🎙️ Call Recorded ($callMedium)",
                    subtitle = "Saved $callMedium recording for $contactName ($durationText)",
                    timeAgo = "Just now",
                    category = "followup",
                    isRead = false
                )
            )
        }
    }

    fun deleteCallRecording(id: Long) {
        viewModelScope.launch {
            callRecordingDao.deleteById(id)
        }
    }

    // --- Invoice Operations ---
    fun addInvoice(
        clientName: String,
        clientCompany: String,
        clientEmail: String,
        clientPhone: String,
        dueDate: String,
        currency: String = "₹",
        subtotal: Double,
        taxPercent: Double = 18.0,
        itemsSummary: String,
        notes: String = "Payment terms: Net 15 days."
    ) {
        viewModelScope.launch {
            val count = invoiceDao.getInvoiceCount().first()
            val invNum = "INV-2026-" + String.format(Locale.US, "%04d", count + 1)
            val todayStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
            val total = subtotal + (subtotal * taxPercent / 100.0)

            val invoice = InvoiceEntity(
                invoiceNumber = invNum,
                clientName = clientName,
                clientCompany = clientCompany,
                clientEmail = clientEmail,
                clientPhone = clientPhone,
                issueDate = todayStr,
                dueDate = dueDate,
                currency = currency,
                subtotal = subtotal,
                taxPercent = taxPercent,
                totalAmount = total,
                status = "Pending",
                itemsSummary = itemsSummary,
                notes = notes
            )
            invoiceDao.insert(invoice)
            notificationDao.insert(
                NotificationEntity(
                    title = "🧾 New Invoice Generated",
                    subtitle = "$invNum created for $clientCompany ($currency ${String.format(Locale.US, "%,.0f", total)})",
                    timeAgo = "Just now",
                    category = "project",
                    isRead = false
                )
            )
        }
    }

    fun updateInvoiceStatus(invoice: InvoiceEntity, newStatus: String) {
        viewModelScope.launch {
            invoiceDao.update(invoice.copy(status = newStatus))
            notificationDao.insert(
                NotificationEntity(
                    title = "Invoice Status Updated",
                    subtitle = "${invoice.invoiceNumber} marked as $newStatus",
                    timeAgo = "Just now",
                    category = "project",
                    isRead = false
                )
            )
        }
    }

    fun deleteInvoice(id: Long) {
        viewModelScope.launch {
            invoiceDao.deleteById(id)
        }
    }

    // --- Quotation Operations ---
    fun addQuotation(
        clientName: String,
        clientCompany: String,
        clientEmail: String,
        clientPhone: String,
        validUntil: String,
        currency: String = "₹",
        subtotal: Double,
        discountPercent: Double = 0.0,
        taxPercent: Double = 18.0,
        scopeOfWork: String,
        termsAndConditions: String = "50% Advance on project kickoff, 50% on final milestone."
    ) {
        viewModelScope.launch {
            val count = quotationDao.getQuotationCount().first()
            val qtNum = "QT-2026-" + String.format(Locale.US, "%04d", count + 1)
            val todayStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
            val afterDiscount = subtotal - (subtotal * discountPercent / 100.0)
            val total = afterDiscount + (afterDiscount * taxPercent / 100.0)

            val quotation = QuotationEntity(
                quotationNumber = qtNum,
                clientName = clientName,
                clientCompany = clientCompany,
                clientEmail = clientEmail,
                clientPhone = clientPhone,
                issueDate = todayStr,
                validUntil = validUntil,
                currency = currency,
                subtotal = subtotal,
                discountPercent = discountPercent,
                taxPercent = taxPercent,
                totalAmount = total,
                status = "Sent",
                scopeOfWork = scopeOfWork,
                termsAndConditions = termsAndConditions
            )
            quotationDao.insert(quotation)
            notificationDao.insert(
                NotificationEntity(
                    title = "📋 New Quotation Created",
                    subtitle = "$qtNum sent to $clientCompany ($currency ${String.format(Locale.US, "%,.0f", total)})",
                    timeAgo = "Just now",
                    category = "project",
                    isRead = false
                )
            )
        }
    }

    fun updateQuotationStatus(quotation: QuotationEntity, newStatus: String) {
        viewModelScope.launch {
            quotationDao.update(quotation.copy(status = newStatus))
            notificationDao.insert(
                NotificationEntity(
                    title = "Quotation Status Updated",
                    subtitle = "${quotation.quotationNumber} status changed to $newStatus",
                    timeAgo = "Just now",
                    category = "project",
                    isRead = false
                )
            )
        }
    }

    fun convertQuotationToInvoice(quotation: QuotationEntity) {
        viewModelScope.launch {
            quotationDao.update(quotation.copy(status = "Converted to Invoice"))
            val count = invoiceDao.getInvoiceCount().first()
            val invNum = "INV-2026-" + String.format(Locale.US, "%04d", count + 1)
            val todayStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())

            val invoice = InvoiceEntity(
                invoiceNumber = invNum,
                clientName = quotation.clientName,
                clientCompany = quotation.clientCompany,
                clientEmail = quotation.clientEmail,
                clientPhone = quotation.clientPhone,
                issueDate = todayStr,
                dueDate = quotation.validUntil,
                currency = quotation.currency,
                subtotal = quotation.subtotal,
                taxPercent = quotation.taxPercent,
                totalAmount = quotation.totalAmount,
                status = "Pending",
                itemsSummary = quotation.scopeOfWork,
                notes = "Converted from quotation ${quotation.quotationNumber}. " + quotation.termsAndConditions
            )
            invoiceDao.insert(invoice)
            notificationDao.insert(
                NotificationEntity(
                    title = "⚡ Quotation Converted to Invoice",
                    subtitle = "${quotation.quotationNumber} converted to $invNum for ${quotation.clientCompany}",
                    timeAgo = "Just now",
                    category = "project",
                    isRead = false
                )
            )
        }
    }

    fun deleteQuotation(id: Long) {
        viewModelScope.launch {
            quotationDao.deleteById(id)
        }
    }

    // --- Auto Company Profile PDF Brochure Operations ---
    fun updateAutoBrochureConfig(config: AutoBrochureConfigEntity) {
        viewModelScope.launch {
            autoBrochureDao.insertOrUpdate(config)
            notificationDao.insert(
                NotificationEntity(
                    title = "📄 Company Profile PDF Settings Updated",
                    subtitle = "Auto-send on lead capture is ${if (config.isAutoSendEnabled) "Enabled" else "Disabled"}",
                    timeAgo = "Just now",
                    category = "project",
                    isRead = false
                )
            )
        }
    }

    fun testSendBrochureManual(recipientName: String, recipientPhone: String, recipientEmail: String) {
        viewModelScope.launch {
            val cfg = autoBrochureConfig.value ?: AutoBrochureConfigEntity()
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val nowStr = "Today, " + timeFormat.format(Date())
            autoBrochureDao.incrementSentCount(nowStr)
            notificationDao.insert(
                NotificationEntity(
                    title = "📄 Company Profile PDF Dispatched",
                    subtitle = "Sent '${cfg.brochureFileName}' to $recipientName ($recipientPhone)",
                    timeAgo = "Just now",
                    category = "followup",
                    isRead = false
                )
            )
        }
    }

    // --- Social Reviews & QR Code Operations ---
    fun updateSocialReview(config: SocialReviewConfigEntity) {
        viewModelScope.launch {
            socialReviewDao.insertOrUpdate(config)
        }
    }
}

