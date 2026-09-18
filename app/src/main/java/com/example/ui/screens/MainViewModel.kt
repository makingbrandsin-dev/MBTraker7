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

    // Leaves
    val leaves = leaveDao.getAllLeaves()
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
    val isFirebaseConnected = FirebaseRealtimeManager.isRealtimeConnected
    val firebaseSyncStatus = FirebaseRealtimeManager.syncStatus

    private val _liveActiveDurationSeconds = MutableStateFlow(0L) // Default 0 when starting day fresh
    val liveActiveDurationSeconds: StateFlow<Long> = _liveActiveDurationSeconds.asStateFlow()

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
                val currentRec = latestAttendance.value
                if (currentRec != null && currentRec.isWorking) {
                    _liveActiveDurationSeconds.value += 1
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
            _attendanceSnackbarMessage.value = "Checked In at $nowTimeStr (Timestamp: $currentTimestamp, ID: #$recordId)"
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
                _attendanceSnackbarMessage.value = "Checked Out at $nowTimeStr"
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

    fun addTask(title: String, projectName: String, priority: String, dueDate: String) {
        viewModelScope.launch {
            taskDao.insert(
                TaskEntity(
                    title = title,
                    projectName = projectName,
                    priority = priority,
                    dueDate = dueDate,
                    status = "Backlog",
                    isCompleted = false
                )
            )
            notificationDao.insert(
                NotificationEntity(
                    title = "New Task Assigned",
                    subtitle = "Task '$title' created for $projectName",
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
        score: Int = (70..95).random()
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
                nextFollowUp = "Tomorrow, 10:00 AM"
            )
            val id = leadDao.insert(newLead)
            FirebaseRealtimeManager.syncLeadToFirebase(newLead.copy(id = id))
            notificationDao.insert(
                NotificationEntity(
                    title = "New Lead Added",
                    subtitle = "Lead '$name' ($company) added to CRM pipeline",
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
            val message = ChatMessageEntity(
                channelId = _currentChannel.value,
                senderName = "Rahul Sharma",
                senderRole = "Senior Developer",
                messageText = text,
                timestampText = timeFormat.format(Date()),
                isMe = true,
                attachmentFileName = fileName,
                attachmentFileSize = fileSize
            )
            chatDao.insert(message)

            // Simulate quick auto-reply from team member after 2 seconds for dynamic feel
            delay(2000)
            chatDao.insert(
                ChatMessageEntity(
                    channelId = _currentChannel.value,
                    senderName = "Arjun Mehta",
                    senderRole = "Team Lead",
                    messageText = "Acknowledged: \"$text\". Proceeding with team schedule!",
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
}

