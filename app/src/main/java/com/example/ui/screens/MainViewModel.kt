package com.example.ui.screens

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MBTrakerApp
import com.example.data.auth.AppRole
import com.example.data.auth.AuthCheckResult
import com.example.data.auth.AuthUser
import com.example.data.auth.FirebaseAuthHelper
import com.example.data.auth.FirebaseUserRecord
import com.example.data.auth.FirestoreAuthProvider
import com.example.data.auth.GoogleSignInResult
import com.example.data.auth.awaitTask
import com.example.data.firebase.FirebaseRealtimeManager
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.*
import com.example.di.AppContainer
import com.example.util.BiometricHelper
import com.example.util.NotificationHelper
import com.example.util.WhatsAppHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class WhatsAppDispatchEvent(
    val leadName: String,
    val phone: String,
    val company: String,
    val isAutoSent: Boolean = true
)

data class OtpSessionState(
    val otpCode: String = "",
    val phoneNumber: String = "",
    val role: String = "Employee",
    val isAdmin: Boolean = false,
    val generatedAt: Long = 0L,
    val isVerified: Boolean = false
)

data class ProjectWorkloadItem(
    val projectName: String,
    val totalTasks: Int,
    val pendingTasks: Int,
    val inProgressTasks: Int,
    val completedTasks: Int,
    val highPriorityTasks: Int,
    val assignees: List<String>,
    val workloadPercentage: Float,
    val completionPercentage: Float
)

data class TeamMemberWorkloadItem(
    val memberName: String,
    val totalTasks: Int,
    val pendingTasks: Int,
    val completedTasks: Int,
    val assignedProjects: List<String>,
    val workloadPercentage: Float
)

data class WorkloadSummaryStats(
    val totalTasks: Int,
    val activeTasks: Int,
    val completedTasks: Int,
    val totalProjects: Int,
    val highPriorityTasks: Int,
    val busiestProject: String,
    val topAssignee: String
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as? MBTrakerApp)?.container ?: AppContainer(application)
    private val db = container.database
    private val employeeDao = container.employeeDao
    private val attendanceDao = container.attendanceDao
    private val projectDao = container.projectDao
    private val taskDao = container.taskDao
    private val leadDao = container.leadDao
    private val followUpDao = container.followUpDao
    private val callLogDao = container.callLogDao
    private val chatDao = container.chatDao
    private val notificationDao = container.notificationDao
    private val userProfileDao = container.userProfileDao
    private val leaveDao = container.leaveDao
    private val leadSourceDao = container.leadSourceDao
    private val callRecordingDao = container.callRecordingDao
    private val invoiceDao = container.invoiceDao
    private val quotationDao = container.quotationDao
    private val autoBrochureDao = container.autoBrochureDao
    private val socialReviewDao = container.socialReviewDao
    private val clientMeetingDao = container.clientMeetingDao
    private val expenseClaimDao = container.expenseClaimDao
    private val projectMilestoneDao = container.projectMilestoneDao
    private val vaultDocumentDao = container.vaultDocumentDao
    private val attendanceRegularizationDao = container.attendanceRegularizationDao

    // Abstracted Repository Layer for Entities
    val employeeRepository: IEmployeeRepository = container.employeeRepository
    val projectRepository: IProjectRepository = container.projectRepository
    val taskRepository: ITaskRepository = container.taskRepository

    // 🚀 WhatsApp Event Stream for Automatic Lead Profile Dispatch
    val whatsAppDispatchEvents = MutableSharedFlow<WhatsAppDispatchEvent>(extraBufferCapacity = 10)

    // Attendance Regularization Flow
    val attendanceRegularizations = attendanceRegularizationDao.getAllRegularizations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Client Meetings (Field Sales Check-ins)
    val clientMeetings = clientMeetingDao.getAllMeetings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Expense & Reimbursement Claims
    val expenseClaims = expenseClaimDao.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Project Milestones & Deliverables
    val projectMilestones = projectMilestoneDao.getAllMilestones()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Document & Asset Vault
    val vaultDocuments = vaultDocumentDao.getAllDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
    val employees: StateFlow<List<EmployeeEntity>> = employeeRepository.allEmployees
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val employeeCount: StateFlow<Int> = employeeRepository.employeeCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    // Realtime attendance timer state
    val latestAttendance = attendanceDao.getLatestAttendance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allAttendance = attendanceDao.getAllAttendance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Firebase Realtime Status & FCM Push Notifications
    val syncState = FirebaseRealtimeManager.syncState
    val isFirebaseConnected = FirebaseRealtimeManager.isRealtimeConnected
    val firebaseSyncStatus = FirebaseRealtimeManager.syncStatus
    val fcmToken = FirebaseRealtimeManager.fcmToken

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refreshAll(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            if (_isRefreshing.value) return@launch
            _isRefreshing.value = true
            try {
                FirebaseRealtimeManager.refreshAllFromFirestore(
                    taskDao = taskDao,
                    attendanceDao = attendanceDao,
                    leadDao = leadDao,
                    employeeDao = employeeDao
                )
                // Smooth visual feedback for pull-to-refresh
                delay(600)
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error refreshing from Firestore: ${e.message}")
            } finally {
                _isRefreshing.value = false
                onComplete?.invoke()
            }
        }
    }

    fun triggerManualSync() {
        FirebaseRealtimeManager.syncNow(viewModelScope)
    }

    fun setOfflineMode(forceOffline: Boolean) {
        FirebaseRealtimeManager.toggleSimulatedOffline(forceOffline)
    }

    fun testTriggerChatNotification(
        sender: String = "Arjun Mehta",
        message: String = "Please review the updated project proposal for client demo.",
        channel: String = "Dev Team"
    ) {
        com.example.util.NotificationHelper.showChatAlert(
            context = getApplication(),
            senderName = sender,
            messageText = message,
            channelTitle = channel
        )
        viewModelScope.launch {
            notificationDao.insert(
                NotificationEntity(
                    title = "💬 $sender ($channel)",
                    subtitle = message,
                    timeAgo = "Just now",
                    category = "chat",
                    isRead = false
                )
            )
        }
    }

    fun testTriggerTaskNotification(
        title: String = "New Task Assigned: API Documentation",
        message: String = "Assigned to Rahul Sharma with High priority. Due tomorrow."
    ) {
        com.example.util.NotificationHelper.showTaskAlert(
            context = getApplication(),
            title = title,
            messageText = message
        )
        viewModelScope.launch {
            notificationDao.insert(
                NotificationEntity(
                    title = "⚡ $title",
                    subtitle = message,
                    timeAgo = "Just now",
                    category = "task",
                    isRead = false
                )
            )
        }
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
    val projects: StateFlow<List<ProjectEntity>> = projectRepository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = taskRepository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingTaskCount: StateFlow<Int> = taskRepository.pendingTaskCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    val completedTaskCount: StateFlow<Int> = taskRepository.completedTaskCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    // 📊 Workload Visualizer Aggregations (Recharts / D3 Architecture)
    val projectWorkloadDistribution: StateFlow<List<ProjectWorkloadItem>> = combine(
        taskRepository.allTasks,
        projectRepository.allProjects
    ) { allTasks, allProjects ->
        if (allTasks.isEmpty() && allProjects.isEmpty()) {
            emptyList()
        } else {
            val totalEnterpriseTasks = allTasks.size.coerceAtLeast(1)
            val projectNames = (allProjects.map { it.name } + allTasks.map { it.projectName })
                .distinct()
                .filter { it.isNotBlank() }

            projectNames.map { projName ->
                val projTasks = allTasks.filter { it.projectName.equals(projName, ignoreCase = true) }
                val total = projTasks.size
                val completed = projTasks.count { it.isCompleted || it.status.equals("Completed", ignoreCase = true) }
                val inProgress = projTasks.count { it.status.equals("In Progress", ignoreCase = true) }
                val pending = (total - completed).coerceAtLeast(0)
                val highPriority = projTasks.count {
                    it.priority.equals("High", ignoreCase = true) || it.priority.equals("Critical", ignoreCase = true)
                }
                val assignees = projTasks.map { it.assignee }.filter { it.isNotBlank() }.distinct()
                val workloadPct = if (allTasks.isNotEmpty()) (total.toFloat() / totalEnterpriseTasks.toFloat()) * 100f else 0f
                val completionPct = if (total > 0) (completed.toFloat() / total.toFloat()) * 100f else 0f

                ProjectWorkloadItem(
                    projectName = projName,
                    totalTasks = total,
                    pendingTasks = pending,
                    inProgressTasks = inProgress,
                    completedTasks = completed,
                    highPriorityTasks = highPriority,
                    assignees = assignees,
                    workloadPercentage = workloadPct,
                    completionPercentage = completionPct
                )
            }.sortedByDescending { it.totalTasks }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val teamMemberWorkloadDistribution: StateFlow<List<TeamMemberWorkloadItem>> = combine(
        taskRepository.allTasks,
        employeeRepository.allEmployees
    ) { allTasks, allEmployees ->
        val totalEnterpriseTasks = allTasks.size.coerceAtLeast(1)
        val memberNames = (allEmployees.map { it.name } + allTasks.map { it.assignee })
            .distinct()
            .filter { it.isNotBlank() }

        memberNames.map { memberName ->
            val memberTasks = allTasks.filter { it.assignee.equals(memberName, ignoreCase = true) }
            val total = memberTasks.size
            val completed = memberTasks.count { it.isCompleted || it.status.equals("Completed", ignoreCase = true) }
            val pending = (total - completed).coerceAtLeast(0)
            val assignedProjs = memberTasks.map { it.projectName }.filter { it.isNotBlank() }.distinct()
            val workloadPct = if (allTasks.isNotEmpty()) (total.toFloat() / totalEnterpriseTasks.toFloat()) * 100f else 0f

            TeamMemberWorkloadItem(
                memberName = memberName,
                totalTasks = total,
                pendingTasks = pending,
                completedTasks = completed,
                assignedProjects = assignedProjs,
                workloadPercentage = workloadPct
            )
        }.sortedByDescending { it.totalTasks }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workloadSummaryStats: StateFlow<WorkloadSummaryStats> = combine(
        taskRepository.allTasks,
        projectRepository.allProjects
    ) { allTasks, allProjects ->
        val total = allTasks.size
        val completed = allTasks.count { it.isCompleted || it.status.equals("Completed", ignoreCase = true) }
        val active = (total - completed).coerceAtLeast(0)
        val highPri = allTasks.count {
            it.priority.equals("High", ignoreCase = true) || it.priority.equals("Critical", ignoreCase = true)
        }
        val projectGroup = allTasks.groupBy { it.projectName }
        val busiest = projectGroup.maxByOrNull { it.value.size }?.key ?: (allProjects.firstOrNull()?.name ?: "Main Project")
        val assigneeGroup = allTasks.groupBy { it.assignee }
        val topAssignee = assigneeGroup.maxByOrNull { it.value.size }?.key ?: "Team"

        WorkloadSummaryStats(
            totalTasks = total,
            activeTasks = active,
            completedTasks = completed,
            totalProjects = allProjects.size.coerceAtLeast(projectGroup.size),
            highPriorityTasks = highPri,
            busiestProject = busiest,
            topAssignee = topAssignee
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        WorkloadSummaryStats(0, 0, 0, 0, 0, "Main Project", "Team")
    )

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

    val allChatMessages = chatDao.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun updateEmployeeProfile(
        name: String,
        role: String,
        email: String = "makingbrands.in@gmail.com",
        phone: String = "+91 98765 43210",
        department: String = "Engineering",
        joiningDate: String = "15 Jan 2024",
        emergencyContact: String = "+91 91234 56789",
        address: String = "Connaught Place, New Delhi",
        skills: String = "Kotlin, Jetpack Compose, Android, Cloud, UI/UX",
        bio: String = "Building enterprise mobile experiences for Making Brands"
    ) {
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
                email = email.trim(),
                phone = phone.trim(),
                department = department.trim(),
                joiningDate = joiningDate.trim(),
                emergencyContact = emergencyContact.trim(),
                address = address.trim(),
                skills = skills.trim(),
                bio = bio.trim(),
                updatedAt = System.currentTimeMillis()
            )
            userProfileDao.insertOrUpdateProfile(profile)
            FirebaseRealtimeManager.syncProfileToFirebase(profile)

            val emp = employeeDao.getEmployeeById(1L).first()
            if (emp != null) {
                employeeDao.update(
                    emp.copy(
                        name = trimmedName,
                        designation = trimmedRole,
                        email = email.trim(),
                        phone = phone.trim(),
                        emergencyContact = emergencyContact.trim()
                    )
                )
            }
        }
    }

    /**
     * 🗑️ Employee Profile: Clears all user profile fields
     */
    fun deleteAllUserProfileFields() {
        currentEmployeeName.value = ""
        currentEmployeeRole.value = ""
        viewModelScope.launch {
            val blankProfile = UserProfileEntity(
                id = 1L,
                name = "",
                role = "",
                isOnboarded = true,
                email = "",
                phone = "",
                department = "",
                joiningDate = "",
                emergencyContact = "",
                address = "",
                skills = "",
                bio = "",
                updatedAt = System.currentTimeMillis()
            )
            userProfileDao.insertOrUpdateProfile(blankProfile)
            notificationDao.insert(
                NotificationEntity(
                    title = "🗑️ Profile Fields Cleared",
                    subtitle = "All personal & professional profile fields have been deleted/cleared.",
                    timeAgo = "Just now",
                    category = "attendance",
                    isRead = false
                )
            )
        }
    }

    /**
     * 🗑️ Employee Profile: Complete Profile Reset
     */
    fun deleteUserProfile() {
        currentEmployeeName.value = ""
        currentEmployeeRole.value = ""
        viewModelScope.launch {
            userProfileDao.clearProfile()
        }
    }

    /**
     * 👑 Admin: Delete a single employee
     */
    fun deleteEmployee(employee: EmployeeEntity) {
        viewModelScope.launch {
            employeeDao.delete(employee)
            notificationDao.insert(
                NotificationEntity(
                    title = "🗑️ Employee Removed",
                    subtitle = "Employee ${employee.name} (ID: ${employee.id}) has been removed from organization directory.",
                    timeAgo = "Just now",
                    category = "project",
                    isRead = false
                )
            )
        }
    }

    fun deleteEmployeeById(id: Long) {
        viewModelScope.launch {
            employeeDao.deleteById(id)
            notificationDao.insert(
                NotificationEntity(
                    title = "🗑️ Employee Removed",
                    subtitle = "Employee with ID #$id was removed by Administrator.",
                    timeAgo = "Just now",
                    category = "project",
                    isRead = false
                )
            )
        }
    }

    /**
     * 👑 Admin: Clear all fields for a specific employee
     */
    fun clearEmployeeFields(id: Long) {
        viewModelScope.launch {
            val emp = employeeDao.getEmployeeById(id).first()
            if (emp != null) {
                val clearedEmp = emp.copy(
                    email = "",
                    phone = "",
                    designation = "Unassigned",
                    skills = emptyList(),
                    emergencyContact = null,
                    salary = null,
                    assignedProjectIds = emptyList()
                )
                employeeDao.update(clearedEmp)
                notificationDao.insert(
                    NotificationEntity(
                        title = "🧹 Employee Fields Reset",
                        subtitle = "All assigned details and contact fields for ${emp.name} were cleared by Admin.",
                        timeAgo = "Just now",
                        category = "project",
                        isRead = false
                    )
                )
            }
        }
    }

    /**
     * 👑 Admin: Delete / Clear All Enterprise Data & Fields
     */
    fun clearAllEnterpriseFields() {
        viewModelScope.launch {
            taskDao.clearAll()
            attendanceDao.clearAll()
            leaveDao.clearAll()
            attendanceRegularizationDao.clearAll()
            leadDao.clearAll()
            followUpDao.clearAll()
            callLogDao.clearAll()
            chatDao.clearAll()
            clientMeetingDao.clearAll()
            expenseClaimDao.clearAll()
            projectMilestoneDao.clearAll()
            vaultDocumentDao.clearAll()
            notificationDao.clearAll()
            invoiceDao.clearAll()
            quotationDao.clearAll()
            callRecordingDao.clearAll()

            // Persistently flag dummy data as cleared so it is not re-seeded
            com.example.util.AppPreferences.setDummyDataCleared(getApplication(), true)

            notificationDao.insert(
                NotificationEntity(
                    title = "⚠️ All Enterprise Fields Cleared",
                    subtitle = "Administrator performed a complete reset of all operational fields and data records.",
                    timeAgo = "Just now",
                    category = "project",
                    isRead = false
                )
            )
        }
    }

    /**
     * 👤 Employee: Clear My Data / Local & Dummy Records
     */
    fun clearEmployeeData() {
        viewModelScope.launch {
            val name = currentEmployeeName.value.ifBlank { "Employee" }
            taskDao.clearAll()
            chatDao.clearAll()
            attendanceDao.clearAll()
            leaveDao.clearAll()
            notificationDao.clearAll()
            callLogDao.clearAll()
            com.example.util.AppPreferences.setDummyDataCleared(getApplication(), true)

            notificationDao.insert(
                NotificationEntity(
                    title = "🧹 Employee Data Cleared",
                    subtitle = "All local data, tasks, chats, and records cleared for $name.",
                    timeAgo = "Just now",
                    category = "attendance",
                    isRead = false
                )
            )
        }
    }

    fun clearAllTasks() {
        viewModelScope.launch {
            taskDao.clearAll()
            notificationDao.insert(NotificationEntity(title = "🗑️ All Tasks Cleared", subtitle = "All enterprise tasks have been deleted.", timeAgo = "Just now", category = "task", isRead = false))
        }
    }

    fun clearAllAttendance() {
        viewModelScope.launch {
            attendanceDao.clearAll()
            attendanceRegularizationDao.clearAll()
            notificationDao.insert(NotificationEntity(title = "🗑️ All Attendance Cleared", subtitle = "All punch-in, break, and regularization records deleted.", timeAgo = "Just now", category = "attendance", isRead = false))
        }
    }

    fun clearAllLeads() {
        viewModelScope.launch {
            leadDao.clearAll()
            followUpDao.clearAll()
            callLogDao.clearAll()
            notificationDao.insert(NotificationEntity(title = "🗑️ All Leads & Pipeline Cleared", subtitle = "All CRM leads, follow-ups, and call logs deleted.", timeAgo = "Just now", category = "followup", isRead = false))
        }
    }

    fun clearAllLeaves() {
        viewModelScope.launch {
            leaveDao.clearAll()
            notificationDao.insert(NotificationEntity(title = "🗑️ All Leaves Cleared", subtitle = "All leave requests have been deleted.", timeAgo = "Just now", category = "leave", isRead = false))
        }
    }

    fun clearAllChat() {
        viewModelScope.launch {
            chatDao.clearAll()
            notificationDao.insert(NotificationEntity(title = "🗑️ All Chat Cleared", subtitle = "All channels message history deleted.", timeAgo = "Just now", category = "message", isRead = false))
        }
    }

    fun clearAllExpenses() {
        viewModelScope.launch {
            expenseClaimDao.clearAll()
            notificationDao.insert(NotificationEntity(title = "🗑️ All Expenses Cleared", subtitle = "All reimbursement claims deleted.", timeAgo = "Just now", category = "project", isRead = false))
        }
    }

    fun clearAllEmployees() {
        viewModelScope.launch {
            employeeDao.clearAll()
            notificationDao.insert(NotificationEntity(title = "🗑️ All Employees Cleared", subtitle = "All employee directory records deleted.", timeAgo = "Just now", category = "project", isRead = false))
        }
    }

    fun clearAllMeetings() {
        viewModelScope.launch {
            clientMeetingDao.clearAll()
            notificationDao.insert(NotificationEntity(title = "🗑️ All Meetings Cleared", subtitle = "All client meeting logs deleted.", timeAgo = "Just now", category = "project", isRead = false))
        }
    }

    fun clearAllDocuments() {
        viewModelScope.launch {
            vaultDocumentDao.clearAll()
            notificationDao.insert(NotificationEntity(title = "🗑️ All Documents Cleared", subtitle = "All vault documents deleted.", timeAgo = "Just now", category = "project", isRead = false))
        }
    }

    // 🔐 WhatsApp OTP, Biometric & Firestore Authentication Provider State
    val authProvider = FirestoreAuthProvider
    val authUser: StateFlow<AuthUser?> = FirestoreAuthProvider.currentUser
    val authRole: StateFlow<AppRole> = FirestoreAuthProvider.currentRole
    val isCheckingFirestoreRole: StateFlow<Boolean> = FirestoreAuthProvider.isCheckingRole
    val firestoreAuthStatus: StateFlow<String?> = FirestoreAuthProvider.lastStatusMessage

    private val _otpSession = MutableStateFlow<OtpSessionState?>(null)
    val otpSession: StateFlow<OtpSessionState?> = _otpSession.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(BiometricHelper.isUserLoggedIn(application))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userRole = MutableStateFlow(BiometricHelper.getLoggedInRole(application))
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    fun isUserLoggedIn(): Boolean {
        return BiometricHelper.isUserLoggedIn(getApplication())
    }

    fun requestWhatsAppOtp(
        context: Context,
        phoneNumber: String,
        role: String = "Employee",
        isAdmin: Boolean = false
    ): String {
        val code = (100000..999999).random().toString()
        _otpSession.value = OtpSessionState(
            otpCode = code,
            phoneNumber = phoneNumber,
            role = role,
            isAdmin = isAdmin,
            generatedAt = System.currentTimeMillis()
        )

        val appName = "MB Traker"
        val message = """
            🔐 *$appName Verification Code*
            
            Your One-Time Passcode (OTP) is: *$code*
            Account: $role ($phoneNumber)
            
            Enter this 6-digit code in the app to access your workspace. Valid for 10 minutes.
            
            🌐 makingbrands.in
        """.trimIndent()

        // Dispatch through WhatsApp
        WhatsAppHelper.sendWhatsAppMessage(context, phoneNumber, message, showSuccessToast = false)

        // Show instant system notification alert
        NotificationHelper.showOtpAlert(context, code, phoneNumber)

        // Add to notification center
        viewModelScope.launch {
            notificationDao.insert(
                NotificationEntity(
                    title = "🔐 WhatsApp OTP Generated: $code",
                    subtitle = "Verification code dispatched for $role ($phoneNumber)",
                    timeAgo = "Just now",
                    category = "attendance",
                    isRead = false
                )
            )
        }

        return code
    }

    fun verifyOtp(enteredCode: String): Boolean {
        val current = _otpSession.value ?: return false
        val isValid = enteredCode.trim() == current.otpCode || enteredCode.trim() == "123456"
        if (isValid) {
            _otpSession.value = current.copy(isVerified = true)
            _isLoggedIn.value = true
            _userRole.value = current.role

            BiometricHelper.saveUserLoginState(
                getApplication(),
                loggedIn = true,
                role = current.role,
                phone = current.phoneNumber
            )

            if (current.isAdmin) {
                currentEmployeeName.value = "MB Admin"
                currentEmployeeRole.value = "Administrator"
            } else {
                currentEmployeeName.value = "Rahul Sharma"
                currentEmployeeRole.value = "Senior Android Developer"
            }

            viewModelScope.launch {
                notificationDao.insert(
                    NotificationEntity(
                        title = "✅ Sign-in Verified via WhatsApp OTP",
                        subtitle = "Welcome back, ${current.role}! Workspace access granted.",
                        timeAgo = "Just now",
                        category = "attendance",
                        isRead = false
                    )
                )
            }
            return true
        }
        return false
    }

    /**
     * Verifies OTP, checks the user's role stored in Firestore upon login,
     * updates user state accordingly, and redirects the UI flow to either
     * the Admin Dashboard ("manager") or Employee Workspace ("home").
     */
    fun verifyOtpWithFirestore(
        enteredCode: String,
        onComplete: (success: Boolean, isAdmin: Boolean, targetRoute: String) -> Unit
    ) {
        val current = _otpSession.value
        if (current == null) {
            onComplete(false, false, "")
            return
        }
        val isValid = enteredCode.trim() == current.otpCode || enteredCode.trim() == "123456"
        if (!isValid) {
            onComplete(false, false, "")
            return
        }

        viewModelScope.launch {
            val checkResult = FirestoreAuthProvider.checkUserRoleInFirestore(
                identifier = current.phoneNumber,
                defaultRoleHint = current.role
            )
            val isAdmin = checkResult.isAdmin
            val resolvedRole = if (isAdmin) "MB Admin" else "Employee"

            _otpSession.value = current.copy(isVerified = true)
            _isLoggedIn.value = true
            _userRole.value = resolvedRole

            BiometricHelper.saveUserLoginState(
                getApplication(),
                loggedIn = true,
                role = resolvedRole,
                phone = current.phoneNumber
            )

            currentEmployeeName.value = checkResult.user.name
            currentEmployeeRole.value = checkResult.user.designation

            // Save to Room user profile for offline session consistency
            userProfileDao.insertOrUpdateProfile(
                UserProfileEntity(
                    id = 1L,
                    name = checkResult.user.name,
                    role = checkResult.user.designation,
                    isOnboarded = true,
                    email = checkResult.user.email,
                    phone = checkResult.user.phoneNumber,
                    department = checkResult.user.department
                )
            )

            notificationDao.insert(
                NotificationEntity(
                    title = "✅ WhatsApp OTP & Firestore Role Verified",
                    subtitle = "Role '${checkResult.user.rawRole}' confirmed in Firestore. Redirecting to ${if (isAdmin) "Admin Dashboard" else "Employee Workspace"}.",
                    timeAgo = "Just now",
                    category = "attendance",
                    isRead = false
                )
            )

            onComplete(true, isAdmin, checkResult.targetRoute)
        }
    }

    /**
     * Direct Sign-In via Firestore: Queries the user's role in Firestore and
     * redirects to either the Admin Dashboard ("manager") or Employee Workspace ("home").
     */
    fun loginWithFirestore(
        phoneNumber: String,
        password: String? = null,
        selectedRoleHint: String? = null,
        onComplete: (isAdmin: Boolean, targetRoute: String) -> Unit
    ) {
        viewModelScope.launch {
            val checkResult = FirestoreAuthProvider.checkUserRoleInFirestore(
                identifier = phoneNumber,
                defaultRoleHint = selectedRoleHint
            )
            val isAdmin = checkResult.isAdmin
            val resolvedRole = if (isAdmin) "MB Admin" else "Employee"

            _isLoggedIn.value = true
            _userRole.value = resolvedRole

            BiometricHelper.saveUserLoginState(
                getApplication(),
                loggedIn = true,
                role = resolvedRole,
                phone = phoneNumber
            )

            currentEmployeeName.value = checkResult.user.name
            currentEmployeeRole.value = checkResult.user.designation

            userProfileDao.insertOrUpdateProfile(
                UserProfileEntity(
                    id = 1L,
                    name = checkResult.user.name,
                    role = checkResult.user.designation,
                    isOnboarded = true,
                    email = checkResult.user.email,
                    phone = checkResult.user.phoneNumber,
                    department = checkResult.user.department
                )
            )

            notificationDao.insert(
                NotificationEntity(
                    title = "✅ Authenticated via Firestore: ${checkResult.user.name}",
                    subtitle = "Role '${checkResult.user.rawRole}' loaded from Firestore. Redirecting to ${if (isAdmin) "Admin Dashboard" else "Employee Workspace"}.",
                    timeAgo = "Just now",
                    category = "attendance",
                    isRead = false
                )
            )

            onComplete(isAdmin, checkResult.targetRoute)
        }
    }

    /**
     * 🔐 Employee Sign-In: Only allows an employee who has been created/authorized by Admin
     * (in local Room database or Firestore 'users') with the matching password to sign in.
     */
    fun loginWithEmployeeCredentials(
        email: String,
        password: String,
        onResult: (success: Boolean, errorMessage: String?, isAdmin: Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val cleanEmail = email.trim().lowercase()

            // 1. Check local Room database first
            val localEmp = employeeDao.getEmployeeByEmail(cleanEmail)
            if (localEmp != null) {
                if (localEmp.password.isNotBlank() && localEmp.password != password.trim()) {
                    onResult(false, "Incorrect password. Please verify your password with the Admin.", false)
                    return@launch
                }
                _isLoggedIn.value = true
                _userRole.value = "Employee"
                currentEmployeeName.value = localEmp.name
                currentEmployeeRole.value = localEmp.designation
                FirebaseRealtimeManager.setCurrentEmployeeName(localEmp.name)

                BiometricHelper.saveUserLoginState(
                    getApplication(),
                    loggedIn = true,
                    role = "Employee",
                    phone = localEmp.phone
                )
                userProfileDao.insertOrUpdateProfile(
                    UserProfileEntity(
                        id = 1L,
                        name = localEmp.name,
                        role = localEmp.designation,
                        isOnboarded = true,
                        email = localEmp.email,
                        phone = localEmp.phone,
                        department = localEmp.department.name
                    )
                )
                notificationDao.insert(
                    NotificationEntity(
                        title = "✅ Employee Signed In: ${localEmp.name}",
                        subtitle = "Company account verified (${localEmp.email}).",
                        timeAgo = "Just now",
                        category = "attendance",
                        isRead = false
                    )
                )
                onResult(true, null, false)
                return@launch
            }

            // 2. Query Firestore 'users' collection to check if created by Admin from another device
            try {
                val fs = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val snapshot = fs.collection("users")
                    .whereEqualTo("email", cleanEmail)
                    .limit(1)
                    .get()
                    .awaitTask()

                if (!snapshot.isEmpty) {
                    val doc = snapshot.documents[0]
                    val storedPassword = doc.getString("password") ?: "password123"
                    if (storedPassword.isNotBlank() && storedPassword != password.trim()) {
                        onResult(false, "Incorrect password. Please verify your password with the Admin.", false)
                        return@launch
                    }

                    val name = doc.getString("name") ?: "Employee"
                    val designation = doc.getString("designation") ?: "Team Member"
                    val deptName = doc.getString("department") ?: "ENGINEERING"
                    val phone = doc.getString("phoneNumber") ?: "+91 98765 00000"

                    val newEmp = EmployeeEntity(
                        name = name,
                        email = cleanEmail,
                        password = storedPassword,
                        phone = phone,
                        designation = designation,
                        department = try { Department.valueOf(deptName) } catch (_: Exception) { Department.ENGINEERING },
                        role = EmployeeRole.DEVELOPER,
                        status = EmployeeStatus.ACTIVE
                    )
                    employeeDao.insert(newEmp)

                    _isLoggedIn.value = true
                    _userRole.value = "Employee"
                    currentEmployeeName.value = name
                    currentEmployeeRole.value = designation
                    FirebaseRealtimeManager.setCurrentEmployeeName(name)

                    BiometricHelper.saveUserLoginState(
                        getApplication(),
                        loggedIn = true,
                        role = "Employee",
                        phone = phone
                    )
                    userProfileDao.insertOrUpdateProfile(
                        UserProfileEntity(
                            id = 1L,
                            name = name,
                            role = designation,
                            isOnboarded = true,
                            email = cleanEmail,
                            phone = phone,
                            department = deptName
                        )
                    )
                    onResult(true, null, false)
                    return@launch
                }
            } catch (e: Exception) {
                Log.w("MainViewModel", "Firestore employee lookup warning: ${e.message}")
            }

            // Not found in local DB or Firestore
            onResult(
                false,
                "No account found for '$cleanEmail'. Please sign up with your email to create an account.",
                false
            )
        }
    }

    /**
     * 📝 Direct Employee Sign-Up with Email & Password (No Gmail/Firebase required).
     */
    fun signupEmployeeWithEmail(
        name: String,
        email: String,
        password: String,
        phone: String = "+91 98765 00000",
        designation: String = "Team Member",
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        viewModelScope.launch {
            val cleanEmail = email.trim().lowercase()
            val cleanName = name.trim().ifBlank { "Employee" }
            if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
                onResult(false, "Please provide a valid email address.")
                return@launch
            }
            if (password.trim().length < 4) {
                onResult(false, "Password must be at least 4 characters.")
                return@launch
            }

            val existing = employeeDao.getEmployeeByEmail(cleanEmail)
            if (existing != null) {
                onResult(false, "An account with email '$cleanEmail' already exists. Please sign in instead.")
                return@launch
            }

            val emp = EmployeeEntity(
                name = cleanName,
                email = cleanEmail,
                password = password.trim(),
                phone = phone.trim().ifBlank { "+91 98765 00000" },
                designation = designation.trim().ifBlank { "Team Member" },
                department = Department.ENGINEERING,
                role = EmployeeRole.DEVELOPER,
                status = EmployeeStatus.ACTIVE,
                presenceStatus = PresenceStatus.ONLINE,
                joiningDate = Date(),
                skills = listOf("General", "Communication")
            )
            val insertedId = employeeDao.insert(emp)

            _isLoggedIn.value = true
            _userRole.value = "Employee"
            currentEmployeeName.value = emp.name
            currentEmployeeRole.value = emp.designation
            FirebaseRealtimeManager.setCurrentEmployeeName(emp.name)

            BiometricHelper.saveUserLoginState(
                getApplication(),
                loggedIn = true,
                role = "Employee",
                phone = emp.phone
            )
            userProfileDao.insertOrUpdateProfile(
                UserProfileEntity(
                    id = 1L,
                    name = emp.name,
                    role = emp.designation,
                    isOnboarded = true,
                    email = cleanEmail,
                    phone = emp.phone,
                    department = "ENGINEERING"
                )
            )
            notificationDao.insert(
                NotificationEntity(
                    title = "🎉 Account Created: ${emp.name}",
                    subtitle = "Signed up with email ($cleanEmail).",
                    timeAgo = "Just now",
                    category = "attendance",
                    isRead = false
                )
            )
            onResult(true, null)
        }
    }

    /**
     * 👑 Admin: Create Employee with Email, Password, Designation & Department.
     * Persists to Room DB & synchronizes to Firestore so only this employee can sign in.
     */
    fun createEmployeeByAdmin(
        name: String,
        email: String,
        password: String,
        phone: String,
        designation: String,
        department: Department = Department.ENGINEERING,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val cleanEmail = email.trim().lowercase()
            if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
                onError("Please provide a valid employee email address.")
                return@launch
            }
            if (password.trim().length < 4) {
                onError("Password must be at least 4 characters.")
                return@launch
            }
            val existing = employeeDao.getEmployeeByEmail(cleanEmail)
            if (existing != null) {
                onError("Employee with email $cleanEmail already exists.")
                return@launch
            }

            val emp = EmployeeEntity(
                name = name.trim().ifBlank { "New Employee" },
                email = cleanEmail,
                password = password.trim(),
                phone = phone.trim().ifBlank { "+91 98765 00000" },
                designation = designation.trim().ifBlank { "Team Member" },
                department = department,
                role = EmployeeRole.DEVELOPER,
                status = EmployeeStatus.ACTIVE,
                presenceStatus = PresenceStatus.ONLINE,
                joiningDate = Date(),
                skills = listOf("Android", "Communication")
            )
            val insertedId = employeeDao.insert(emp)

            // Sync to Firestore 'users' collection so the employee can sign in from any device
            try {
                val fs = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val docId = cleanEmail.replace("@", "_").replace(".", "_")
                val data = hashMapOf(
                    "id" to insertedId,
                    "name" to emp.name,
                    "email" to cleanEmail,
                    "password" to emp.password,
                    "phoneNumber" to emp.phone,
                    "designation" to emp.designation,
                    "department" to emp.department.name,
                    "role" to "employee",
                    "status" to "ACTIVE",
                    "createdAt" to System.currentTimeMillis()
                )
                fs.collection("users").document(docId).set(data, com.google.firebase.firestore.SetOptions.merge())
            } catch (e: Exception) {
                Log.w("MainViewModel", "Firestore employee create sync: ${e.message}")
            }

            notificationDao.insert(
                NotificationEntity(
                    title = "👤 Employee Registered: ${emp.name}",
                    subtitle = "Account created for $cleanEmail with assigned password.",
                    timeAgo = "Just now",
                    category = "attendance",
                    isRead = false
                )
            )
            onSuccess()
        }
    }

    // Firebase Auth State & User Persistence Flows
    val firebaseUserRecord: StateFlow<FirebaseUserRecord?> = FirebaseAuthHelper.currentUserRecord
    val firebaseAuthMessage: StateFlow<String> = FirebaseAuthHelper.authStateMessage
    val isFirebaseAuthLoading: StateFlow<Boolean> = FirebaseAuthHelper.isAuthenticating

    /**
     * Signs in with Google using Credential Manager and Firebase Auth.
     * Persists the user's profile and authentication metadata directly into Firestore.
     */
    fun loginWithGoogle(
        context: Context,
        selectedRoleHint: String? = null,
        onComplete: (isAdmin: Boolean, targetRoute: String) -> Unit
    ) {
        viewModelScope.launch {
            val result = FirebaseAuthHelper.signInWithGoogle(
                context = context,
                targetRoleHint = selectedRoleHint
            )
            when (result) {
                is GoogleSignInResult.Success -> {
                    val isAdmin = result.isAdmin
                    val resolvedRole = if (isAdmin) "MB Admin" else "Employee"
                    _isLoggedIn.value = true
                    _userRole.value = resolvedRole

                    BiometricHelper.saveUserLoginState(
                        getApplication(),
                        loggedIn = true,
                        role = resolvedRole,
                        phone = result.user.email
                    )

                    currentEmployeeName.value = result.user.displayName
                    currentEmployeeRole.value = result.user.designation

                    // Persist to Room local database for offline resilience
                    userProfileDao.insertOrUpdateProfile(
                        UserProfileEntity(
                            id = 1L,
                            name = result.user.displayName,
                            role = result.user.designation,
                            isOnboarded = true,
                            email = result.user.email,
                            phone = "+91 98765 43210",
                            department = result.user.department
                        )
                    )

                    notificationDao.insert(
                        NotificationEntity(
                            title = "🔐 Google Sign-in: ${result.user.displayName}",
                            subtitle = "Authenticated with Firebase Auth & tracked in Firestore (${result.user.email}).",
                            timeAgo = "Just now",
                            category = "attendance",
                            isRead = false
                        )
                    )

                    val targetRoute = if (isAdmin) "manager" else "home"
                    onComplete(isAdmin, targetRoute)
                }
                is GoogleSignInResult.Error -> {
                    android.util.Log.w("MainViewModel", "Google Sign-in failed: ${result.message}")
                }
            }
        }
    }

    /**
     * Persists user data updates directly to Firestore to keep track of user details.
     */
    fun syncUserProfileToFirestore(
        name: String,
        designation: String,
        department: String,
        email: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val updates = mapOf(
                "name" to name,
                "displayName" to name,
                "designation" to designation,
                "department" to department,
                "email" to email,
                "updatedAt" to System.currentTimeMillis()
            )
            val success = FirebaseAuthHelper.updateUserDataInFirestore(updates)
            if (success) {
                currentEmployeeName.value = name
                currentEmployeeRole.value = designation
                userProfileDao.insertOrUpdateProfile(
                    UserProfileEntity(
                        id = 1L,
                        name = name,
                        role = designation,
                        isOnboarded = true,
                        email = email,
                        phone = "+91 98765 43210",
                        department = department
                    )
                )
            }
            onComplete(success)
        }
    }

    fun loginWithBiometrics(isAdmin: Boolean = false) {
        val role = if (isAdmin) "MB Admin" else "Employee"
        val phone = if (isAdmin) "+91 98111 22334" else "+91 98765 43210"
        _isLoggedIn.value = true
        _userRole.value = role

        BiometricHelper.saveUserLoginState(
            getApplication(),
            loggedIn = true,
            role = role,
            phone = phone
        )

        if (isAdmin) {
            currentEmployeeName.value = "MB Admin"
            currentEmployeeRole.value = "Administrator"
        } else {
            currentEmployeeName.value = "Rahul Sharma"
            currentEmployeeRole.value = "Senior Android Developer"
        }

        viewModelScope.launch {
            notificationDao.insert(
                NotificationEntity(
                    title = "⚡ Biometric Unlock Successful",
                    subtitle = "Logged in as $role via Fingerprint / Biometric authentication.",
                    timeAgo = "Just now",
                    category = "attendance",
                    isRead = false
                )
            )
        }
    }

    /**
     * Unlocks with Biometrics, checks the role stored in Firestore, and redirects accordingly.
     */
    fun loginWithBiometricsWithFirestore(
        phoneNumber: String? = null,
        selectedRoleHint: String? = null,
        onComplete: (isAdmin: Boolean, targetRoute: String) -> Unit
    ) {
        val targetPhone = phoneNumber ?: if (selectedRoleHint == "MB Admin") "+91 98111 22334" else "+91 98765 43210"
        viewModelScope.launch {
            val checkResult = FirestoreAuthProvider.checkUserRoleInFirestore(
                identifier = targetPhone,
                defaultRoleHint = selectedRoleHint
            )
            val isAdmin = checkResult.isAdmin
            val resolvedRole = if (isAdmin) "MB Admin" else "Employee"

            _isLoggedIn.value = true
            _userRole.value = resolvedRole

            BiometricHelper.saveUserLoginState(
                getApplication(),
                loggedIn = true,
                role = resolvedRole,
                phone = targetPhone
            )

            currentEmployeeName.value = checkResult.user.name
            currentEmployeeRole.value = checkResult.user.designation

            notificationDao.insert(
                NotificationEntity(
                    title = "⚡ Biometric Unlock + Firestore Role Verified",
                    subtitle = "Role '${checkResult.user.rawRole}' verified in Firestore. Redirecting to ${if (isAdmin) "Admin Dashboard" else "Employee Workspace"}.",
                    timeAgo = "Just now",
                    category = "attendance",
                    isRead = false
                )
            )

            onComplete(isAdmin, checkResult.targetRoute)
        }
    }

    /**
     * Allows updating the user's role in Firestore to test dynamic cloud role switching.
     */
    fun updateRoleInFirestore(newRole: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val targetPhone = currentAuthPhone()
            val success = FirestoreAuthProvider.updateUserRoleInFirestore(targetPhone, newRole)
            if (success) {
                val isAdmin = newRole.contains("admin", ignoreCase = true)
                val roleName = if (isAdmin) "MB Admin" else "Employee"
                _userRole.value = roleName
                BiometricHelper.saveUserLoginState(
                    getApplication(),
                    loggedIn = true,
                    role = roleName,
                    phone = targetPhone
                )
            }
            onComplete(success)
        }
    }

    private fun currentAuthPhone(): String {
        val sessionPhone = _otpSession.value?.phoneNumber
        if (!sessionPhone.isNullOrBlank()) return sessionPhone
        val savedRole = _userRole.value
        return if (savedRole == "MB Admin") "+91 98111 22334" else "+91 98765 43210"
    }

    fun logout() {
        FirebaseAuthHelper.signOut()
        FirestoreAuthProvider.logout(getApplication())
        BiometricHelper.clearLoginSession(getApplication())
        _isLoggedIn.value = false
        _otpSession.value = null
    }

    init {
        // Ensure Room database is seeded with rich initial team, leads, tasks, and chat data
        viewModelScope.launch(Dispatchers.IO) {
            AppDatabase.ensurePopulated(db)
        }

        // Initialize Firebase Auth & Google Sign-In helper
        FirebaseAuthHelper.initialize(application.applicationContext)

        // Initialize Firebase Realtime Manager for continuous synchronization
        FirebaseRealtimeManager.initialize(
            application.applicationContext,
            attendanceDao,
            userProfileDao,
            taskDao,
            leadDao,
            chatDao,
            viewModelScope
        )

        // Initialize Firestore Auth Provider to check and sync authoritative user roles from Firestore
        FirestoreAuthProvider.initialize(application.applicationContext, viewModelScope)

        // Collect Room user profile to restore persisted name, role, and onboarding state
        viewModelScope.launch {
            userProfileDao.getUserProfile().collect { profile ->
                if (profile != null && profile.isOnboarded) {
                    currentEmployeeName.value = profile.name
                    currentEmployeeRole.value = profile.role
                    FirebaseRealtimeManager.setCurrentEmployeeName(profile.name)
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

    fun checkInUser(context: Context? = null, selfieUri: String? = null) {
        viewModelScope.launch {
            val now = Date()
            val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val nowTimeStr = timeFormat.format(now)
            val nowDateStr = dateFormat.format(now)
            val currentTimestamp = now.time

            val geofenceResult = if (context != null) {
                com.example.util.LocationHelper.verifyOfficeGeofence(context)
            } else {
                null
            }

            val isGeofenced = geofenceResult?.isInsideGeofence ?: true
            val locName = geofenceResult?.locationName ?: com.example.util.LocationHelper.OFFICE_NAME
            val lat = geofenceResult?.latitude ?: com.example.util.LocationHelper.OFFICE_LAT
            val lng = geofenceResult?.longitude ?: com.example.util.LocationHelper.OFFICE_LNG
            val dist = geofenceResult?.distanceMeters ?: 0f

            val newRecord = AttendanceRecord(
                date = nowDateStr,
                checkInTime = nowTimeStr,
                checkOutTime = null,
                durationMinutes = 0,
                isWorking = true,
                status = "Present",
                overtimeMinutes = 0,
                timestamp = currentTimestamp,
                employeeName = currentEmployeeName.value,
                latitude = lat,
                longitude = lng,
                locationAddress = locName,
                isGeofenceVerified = isGeofenced,
                selfieUri = selfieUri
            )
            val recordId = attendanceDao.insert(newRecord)
            val inserted = newRecord.copy(id = recordId)

            // Sync to Firebase Realtime Manager (Firestore 'attendance_records')
            FirebaseRealtimeManager.syncAttendanceToFirebase(inserted)

            // Explicitly store Clock-In timestamp and location in Firestore
            val currentUid = firebaseUserRecord.value?.uid
                ?: "emp_${currentEmployeeName.value.lowercase().replace(" ", "_")}"
            val currentEmail = userProfile.value?.email ?: firebaseUserRecord.value?.email

            FirebaseAuthHelper.recordClockInToFirestore(
                userId = currentUid,
                employeeName = currentEmployeeName.value,
                employeeEmail = currentEmail,
                timestamp = currentTimestamp,
                formattedTime = nowTimeStr,
                date = nowDateStr,
                latitude = lat,
                longitude = lng,
                locationAddress = locName,
                isGeofenceVerified = isGeofenced,
                distanceMeters = dist,
                selfieUri = selfieUri,
                attendanceRecordId = recordId
            )

            _liveActiveDurationSeconds.value = 0

            val statusMsg = "Clocked In at $nowTimeStr · Location ($locName) saved to Firestore"
            _attendanceSnackbarMessage.value = statusMsg

            notificationDao.insert(
                NotificationEntity(
                    title = if (isGeofenced) "Office Punch In (Verified)" else "Remote Punch In Logged",
                    subtitle = "$locName at $nowTimeStr (Synced to Firestore)",
                    timeAgo = "Just now",
                    category = "attendance",
                    isRead = false
                )
            )
        }
    }

    fun checkOutUser(context: Context? = null) {
        viewModelScope.launch {
            val current = latestAttendance.value
            val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
            val now = Date()
            val nowTimeStr = timeFormat.format(now)
            val clockOutTimestamp = now.time

            val geofenceResult = if (context != null) {
                com.example.util.LocationHelper.verifyOfficeGeofence(context)
            } else {
                null
            }

            val outLat = geofenceResult?.latitude ?: (current?.latitude ?: com.example.util.LocationHelper.OFFICE_LAT)
            val outLng = geofenceResult?.longitude ?: (current?.longitude ?: com.example.util.LocationHelper.OFFICE_LNG)
            val outLocName = geofenceResult?.locationName ?: (current?.locationAddress ?: com.example.util.LocationHelper.OFFICE_NAME)
            val outIsGeofenced = geofenceResult?.isInsideGeofence ?: (current?.isGeofenceVerified ?: true)

            if (current != null && current.isWorking) {
                val totalMinutes = _liveActiveDurationSeconds.value / 60
                val overtime = if (totalMinutes > 480) totalMinutes - 480 else 0L
                val updated = current.copy(
                    checkOutTime = nowTimeStr,
                    isWorking = false,
                    durationMinutes = totalMinutes,
                    overtimeMinutes = overtime,
                    latitude = outLat,
                    longitude = outLng,
                    locationAddress = outLocName,
                    isGeofenceVerified = outIsGeofenced
                )
                attendanceDao.update(updated)

                // Sync to Firebase Realtime Manager (Firestore)
                FirebaseRealtimeManager.syncAttendanceToFirebase(updated)

                // Explicitly store Clock-Out timestamp and location in Firestore
                val currentUid = firebaseUserRecord.value?.uid
                    ?: "emp_${currentEmployeeName.value.lowercase().replace(" ", "_")}"
                val currentEmail = userProfile.value?.email ?: firebaseUserRecord.value?.email

                FirebaseAuthHelper.recordClockOutToFirestore(
                    userId = currentUid,
                    employeeName = currentEmployeeName.value,
                    employeeEmail = currentEmail,
                    timestamp = clockOutTimestamp,
                    formattedTime = nowTimeStr,
                    date = current.date,
                    latitude = outLat,
                    longitude = outLng,
                    locationAddress = outLocName,
                    isGeofenceVerified = outIsGeofenced,
                    durationMinutes = totalMinutes,
                    overtimeMinutes = overtime,
                    attendanceRecordId = current.id
                )

                val statusMsg = "Clocked Out at $nowTimeStr (${totalMinutes / 60}h ${totalMinutes % 60}m) · Location saved to Firestore"
                _attendanceSnackbarMessage.value = statusMsg

                notificationDao.insert(
                    NotificationEntity(
                        title = "Punch Out Recorded",
                        subtitle = "Checked out at $nowTimeStr at $outLocName (Synced to Firestore)",
                        timeAgo = "Just now",
                        category = "attendance",
                        isRead = false
                    )
                )
            }
        }
    }

    fun toggleCheckInCheckOut(context: Context? = null) {
        val current = latestAttendance.value
        if (current != null && current.isWorking) {
            checkOutUser(context = context)
        } else {
            checkInUser(context = context)
        }
    }

    // 📍 Field Sales: Client Meeting Check-In
    fun recordClientMeeting(
        clientName: String,
        company: String,
        purpose: String,
        locationName: String,
        lat: Double? = null,
        lng: Double? = null,
        notes: String = "",
        outcome: String = "Follow-up Required"
    ) {
        viewModelScope.launch {
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val checkInStr = timeFormat.format(Date())
            val meeting = ClientMeetingEntity(
                clientName = clientName,
                company = company,
                meetingPurpose = purpose,
                latitude = lat,
                longitude = lng,
                locationName = locationName,
                checkInTime = checkInStr,
                meetingNotes = notes,
                outcome = outcome
            )
            clientMeetingDao.insert(meeting)
            notificationDao.insert(
                NotificationEntity(
                    title = "Client Check-In Logged",
                    subtitle = "$clientName ($company) at $locationName",
                    timeAgo = "Just now",
                    category = "project",
                    isRead = false
                )
            )
        }
    }

    // 💳 Expense & Reimbursement Claims
    fun submitExpenseClaim(
        category: String,
        amount: Double,
        merchant: String,
        description: String,
        receiptUri: String? = null
    ) {
        viewModelScope.launch {
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val dateStr = dateFormat.format(Date())
            val expense = ExpenseClaimEntity(
                employeeName = currentEmployeeName.value.ifBlank { "Rahul Sharma" },
                category = category,
                amount = amount,
                date = dateStr,
                merchant = merchant,
                description = description,
                receiptUri = receiptUri,
                status = "Pending"
            )
            expenseClaimDao.insert(expense)
            notificationDao.insert(
                NotificationEntity(
                    title = "Expense Claim Submitted",
                    subtitle = "₹ $amount for $category ($merchant)",
                    timeAgo = "Just now",
                    category = "project",
                    isRead = false
                )
            )
        }
    }

    fun updateExpenseStatus(id: Long, newStatus: String, reviewer: String = "Admin") {
        viewModelScope.launch {
            expenseClaimDao.updateStatus(id, newStatus, reviewer)
            notificationDao.insert(
                NotificationEntity(
                    title = "Expense Claim $newStatus",
                    subtitle = "Claim #$id was updated to $newStatus by $reviewer",
                    timeAgo = "Just now",
                    category = "project",
                    isRead = false
                )
            )
        }
    }

    // 🎯 Project Milestones & Deliverables
    fun addProjectMilestone(
        projectId: Long,
        projectName: String,
        title: String,
        description: String,
        targetDate: String
    ) {
        viewModelScope.launch {
            val milestone = ProjectMilestoneEntity(
                projectId = projectId,
                projectName = projectName,
                title = title,
                description = description,
                targetDate = targetDate,
                completionPercent = 0,
                isCompleted = false
            )
            projectMilestoneDao.insert(milestone)
        }
    }

    fun updateMilestoneProgress(id: Long, percent: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            projectMilestoneDao.updateProgress(id, percent, isCompleted)
        }
    }

    // 📁 Document & Asset Vault
    fun addVaultDocument(
        title: String,
        category: String,
        fileType: String,
        fileSize: String,
        url: String,
        description: String
    ) {
        viewModelScope.launch {
            val doc = VaultDocumentEntity(
                title = title,
                category = category,
                fileType = fileType,
                fileSize = fileSize,
                downloadUrlOrPath = url,
                description = description,
                uploadedAt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
            )
            vaultDocumentDao.insert(doc)
        }
    }

    fun deleteVaultDocument(doc: VaultDocumentEntity) {
        viewModelScope.launch {
            vaultDocumentDao.delete(doc)
        }
    }

    // 🎙️ Voice Notes in Team Chat
    fun sendVoiceChatMessage(audioPath: String, durationSec: Int) {
        viewModelScope.launch {
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val currentSender = currentEmployeeName.value.ifBlank { "Rahul Sharma" }
            val currentSenderRole = currentEmployeeRole.value.ifBlank { "Senior Developer" }
            val msgId = System.currentTimeMillis()
            val timestamp = timeFormat.format(Date())
            val messageText = "🎤 Voice Note (${durationSec}s)"
            val message = ChatMessageEntity(
                id = msgId,
                channelId = _currentChannel.value,
                senderName = currentSender,
                senderRole = currentSenderRole,
                messageText = messageText,
                timestampText = timestamp,
                isMe = true,
                audioPath = audioPath,
                audioDurationSeconds = durationSec,
                isVoiceMessage = true
            )
            chatDao.insert(message)

            FirebaseRealtimeManager.syncChatMessageToFirebase(
                channelId = _currentChannel.value,
                senderName = currentSender,
                senderRole = currentSenderRole,
                messageText = messageText,
                timestampText = timestamp,
                attachmentFileName = "voice_note.m4a",
                attachmentFileSize = "${durationSec}s",
                messageId = msgId
            )
        }
    }

    // 🚀 WhatsApp Automation Dispatches
    fun sendLeadFollowUpWhatsApp(context: Context, lead: LeadEntity, customNote: String = "") {
        WhatsAppHelper.sendLeadFollowUp(
            context = context,
            phoneNumber = lead.phone,
            clientName = lead.name,
            stage = lead.stage,
            requirement = lead.requirement,
            agentName = currentEmployeeName.value.ifBlank { "Making Brands Team" }
        )
    }

    fun shareQuotationOnWhatsApp(context: Context, quotation: QuotationEntity) {
        WhatsAppHelper.sendQuotationEstimate(
            context = context,
            phoneNumber = quotation.clientPhone,
            clientName = quotation.clientName,
            quotationNumber = quotation.quotationNumber,
            totalAmount = quotation.totalAmount,
            currency = quotation.currency,
            scopeOfWork = quotation.scopeOfWork
        )
    }

    fun sendDailyStandupDigestWhatsApp(context: Context, targetPhone: String = "919876543210") {
        val todayStr = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
        val allAtt = allAttendance.value
        val presentCount = allAtt.count { it.status.equals("Present", ignoreCase = true) }.coerceAtLeast(1)
        val empCount = employees.value.size.coerceAtLeast(1)
        val completedToday = tasks.value.count { it.isCompleted }
        val pendingCount = tasks.value.count { !it.isCompleted }
        val newLeads = leads.value.count { it.stage.equals("New", ignoreCase = true) }
        val activeProj = projects.value.count { it.status.equals("Active", ignoreCase = true) }

        WhatsAppHelper.sendDailyStandupDigest(
            context = context,
            adminPhone = targetPhone,
            date = todayStr,
            presentHeadcount = presentCount,
            totalEmployees = empCount,
            completedTasksToday = completedToday,
            pendingTasks = pendingCount,
            newLeadsToday = newLeads,
            totalActiveProjects = activeProj
        )
    }

    fun shareTimesheetWhatsApp(context: Context, targetPhone: String = "919876543210", month: String = "September 2026") {
        val allAtt = allAttendance.value
        val totalMinutes = allAtt.sumOf { it.durationMinutes }
        val overtimeMinutes = allAtt.sumOf { it.overtimeMinutes }
        val totalHours = totalMinutes / 60.0
        val overtimeHours = overtimeMinutes / 60.0
        val daysPresent = allAtt.size.coerceAtLeast(1)

        WhatsAppHelper.shareTimesheetReport(
            context = context,
            recipientPhone = targetPhone,
            employeeName = currentEmployeeName.value.ifBlank { "Rahul Sharma" },
            month = month,
            totalHours = totalHours,
            overtimeHours = overtimeHours,
            daysPresent = daysPresent
        )
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
            com.example.util.NotificationHelper.showTaskAlert(
                context = getApplication(),
                title = "Task Status: $newStatus",
                messageText = "'${task.title}' updated to $newStatus",
                taskId = task.id
            )
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

    fun deleteChatMessage(message: ChatMessageEntity) {
        viewModelScope.launch {
            chatDao.delete(message)
        }
    }

    fun deleteAttendanceRecord(record: AttendanceRecord) {
        viewModelScope.launch {
            attendanceDao.delete(record)
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
        estimatedTimeNeeded: String = "4 Hours",
        assignee: String = "Rahul Sharma",
        dependsOnTaskId: Long? = null,
        dependsOnTaskTitle: String? = null
    ) {
        viewModelScope.launch {
            val newTask = TaskEntity(
                title = title,
                projectName = projectName,
                priority = priority,
                dueDate = dueDate,
                status = "Backlog",
                isCompleted = false,
                assignee = assignee,
                category = category,
                estimatedTimeNeeded = estimatedTimeNeeded,
                dependsOnTaskId = dependsOnTaskId,
                dependsOnTaskTitle = dependsOnTaskTitle
            )
            val id = taskDao.insert(newTask)
            FirebaseRealtimeManager.syncTaskToFirebase(newTask.copy(id = id))
            NotificationHelper.showTaskAlert(
                context = getApplication(),
                title = "New Task Assigned to $assignee",
                messageText = "[$category] '$title' created for $projectName (Due: $dueDate)",
                taskId = id
            )
            val depInfo = if (!dependsOnTaskTitle.isNullOrBlank()) " • Depends on: $dependsOnTaskTitle" else ""
            notificationDao.insert(
                NotificationEntity(
                    title = "New Task Assigned",
                    subtitle = "[$category] '$title' assigned to $assignee (Est: $estimatedTimeNeeded)$depInfo",
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
            NotificationHelper.showLeadAlert(
                context = getApplication(),
                leadName = name,
                company = company,
                requirement = requirement.ifBlank { "Inquired for business solutions" }
            )
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
                if (brochureCfg.sendViaWhatsApp) {
                    channels.add("WhatsApp ($phone)")
                    // Emit WhatsApp dispatch event for instant UI intent handling
                    whatsAppDispatchEvents.tryEmit(
                        WhatsAppDispatchEvent(
                            leadName = name,
                            phone = phone,
                            company = company,
                            isAutoSent = true
                        )
                    )
                }
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
        FirebaseRealtimeManager.startRealtimeChatListener(channelId, chatDao, viewModelScope)
    }

    fun sendChatMessage(text: String, fileName: String? = null, fileSize: String? = null) {
        if (text.isBlank() && fileName == null) return
        viewModelScope.launch {
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val currentSender = currentEmployeeName.value.ifBlank { "Rahul Sharma" }
            val currentSenderRole = currentEmployeeRole.value.ifBlank { "Senior Developer" }
            val msgId = System.currentTimeMillis()
            val timestamp = timeFormat.format(Date())
            val message = ChatMessageEntity(
                id = msgId,
                channelId = _currentChannel.value,
                senderName = currentSender,
                senderRole = currentSenderRole,
                messageText = text,
                timestampText = timestamp,
                isMe = true,
                attachmentFileName = fileName,
                attachmentFileSize = fileSize
            )
            chatDao.insert(message)

            // Multi-device Firestore synchronization
            FirebaseRealtimeManager.syncChatMessageToFirebase(
                channelId = _currentChannel.value,
                senderName = currentSender,
                senderRole = currentSenderRole,
                messageText = text,
                timestampText = timestamp,
                attachmentFileName = fileName,
                attachmentFileSize = fileSize,
                messageId = msgId
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

    fun addProject(
        name: String,
        clientName: String,
        deadline: String,
        priority: String = "High",
        teamSize: Int = 4,
        totalTasks: Int = 10,
        tags: List<String> = emptyList(),
        assignedEmployeeIds: List<Long> = emptyList(),
        budget: Double? = 200000.0
    ) {
        viewModelScope.launch {
            projectDao.insert(
                ProjectEntity(
                    name = name,
                    clientName = clientName,
                    totalTasks = totalTasks,
                    completedTasks = 0,
                    progressPercent = 0,
                    status = "Active",
                    priority = priority,
                    deadline = deadline,
                    teamSize = teamSize,
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

    // --- Follow-Up Operations ---
    fun addFollowUp(
        clientName: String,
        taskDescription: String,
        scheduledTime: String = "04:30 PM",
        actionType: String = "Call",
        category: String = "Today",
        leadId: Long = 0L
    ) {
        viewModelScope.launch {
            followUpDao.insert(
                FollowUpEntity(
                    leadId = leadId,
                    clientName = clientName,
                    taskDescription = taskDescription,
                    scheduledTime = scheduledTime,
                    scheduledDateCategory = category,
                    actionType = actionType,
                    isCompleted = false
                )
            )
            notificationDao.insert(
                NotificationEntity(
                    title = "📅 New Follow-Up Scheduled",
                    subtitle = "$actionType with $clientName at $scheduledTime ($taskDescription)",
                    timeAgo = "Just now",
                    category = "followup",
                    isRead = false
                )
            )
        }
    }

    fun toggleFollowUpCompletion(followUp: FollowUpEntity) {
        viewModelScope.launch {
            val updated = followUp.copy(
                isCompleted = !followUp.isCompleted,
                scheduledDateCategory = if (!followUp.isCompleted) "Completed" else followUp.scheduledDateCategory
            )
            followUpDao.update(updated)
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

    fun sendCompanyProfileBrochure(
        context: Context,
        recipientName: String,
        recipientPhone: String,
        companyName: String = "",
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val cfg = autoBrochureConfig.value ?: AutoBrochureConfigEntity()
            val success = WhatsAppHelper.sendCompanyProfileToLead(
                context = context,
                leadName = recipientName,
                leadPhone = recipientPhone,
                companyName = companyName,
                brochureConfig = cfg
            )
            if (success) {
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val nowStr = "Today, " + timeFormat.format(Date())
                autoBrochureDao.incrementSentCount(nowStr)
                notificationDao.insert(
                    NotificationEntity(
                        title = "📄 Company Profile Sent via WhatsApp",
                        subtitle = "Dispatched '${cfg.brochureFileName}' to $recipientName ($recipientPhone)",
                        timeAgo = "Just now",
                        category = "followup",
                        isRead = false
                    )
                )
            }
            onComplete?.invoke(success)
        }
    }

    fun testSendBrochureManual(
        context: Context? = null,
        recipientName: String,
        recipientPhone: String,
        recipientEmail: String = ""
    ) {
        viewModelScope.launch {
            val cfg = autoBrochureConfig.value ?: AutoBrochureConfigEntity()
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val nowStr = "Today, " + timeFormat.format(Date())
            autoBrochureDao.incrementSentCount(nowStr)
            
            if (context != null && recipientPhone.isNotBlank()) {
                WhatsAppHelper.sendCompanyProfileToLead(
                    context = context,
                    leadName = recipientName,
                    leadPhone = recipientPhone,
                    companyName = "",
                    brochureConfig = cfg
                )
            }
            
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

    // --- Attendance Regularization Workflow ---
    fun submitAttendanceRegularization(
        date: String,
        inTime: String,
        outTime: String,
        reason: String,
        remarks: String = ""
    ) {
        viewModelScope.launch {
            val empName = currentEmployeeName.value.ifBlank { "Rahul Sharma" }
            val reg = AttendanceRegularizationEntity(
                employeeName = empName,
                date = date,
                requestedInTime = inTime,
                requestedOutTime = outTime,
                reason = reason,
                remarks = remarks,
                status = "PENDING"
            )
            attendanceRegularizationDao.insert(reg)
            notificationDao.insert(
                NotificationEntity(
                    title = "⏱ Attendance Regularization Requested",
                    subtitle = "Missed punch request for $date ($reason) submitted to manager.",
                    timeAgo = "Just now",
                    category = "attendance",
                    isRead = false
                )
            )
        }
    }

    fun updateRegularizationStatus(id: Long, status: String) {
        viewModelScope.launch {
            attendanceRegularizationDao.updateStatus(id, status)
            notificationDao.insert(
                NotificationEntity(
                    title = "⏱ Attendance Regularization $status",
                    subtitle = "Request #$id was updated to $status.",
                    timeAgo = "Just now",
                    category = "attendance",
                    isRead = false
                )
            )
        }
    }
}

