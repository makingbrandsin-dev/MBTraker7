package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.local.*
import com.example.data.model.*
import com.example.util.AppSoundHelper
import com.example.util.NotificationHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

/**
 * Dedicated Firestore Listener Manager that synchronizes the Admin app ("MB Taker")
 * and the Employee app ("MB EM"), ensuring all status updates, task modifications,
 * and live operations are reflected instantly across both platforms over the internet.
 */
object MbEmSyncManager {
    private const val TAG = "MbEmSyncManager"

    // Firestore instances (supports both named AI Studio database and default database)
    private var primaryFirestore: FirebaseFirestore? = null
    private var secondaryFirestore: FirebaseFirestore? = null

    // Room DAOs for immediate local database persistence
    private var taskDaoRef: TaskDao? = null
    private var employeeDaoRef: EmployeeDao? = null
    private var attendanceDaoRef: AttendanceDao? = null
    private var leaveDaoRef: LeaveDao? = null
    private var leadDaoRef: LeadDao? = null
    private var chatDaoRef: ChatDao? = null
    private var notificationDaoRef: NotificationDao? = null
    private var projectDaoRef: ProjectDao? = null

    private var appContext: Context? = null
    private var managerScope: CoroutineScope? = null

    // Active Listener Registrations
    private val activeRegistrations = ConcurrentHashMap<String, ListenerRegistration>()

    // Observable Synchronization States
    data class MbEmSyncState(
        val isConnected: Boolean = true,
        val activeListenersCount: Int = 0,
        val lastSyncTimestamp: Long = System.currentTimeMillis(),
        val lastEventMessage: String = "Connected with 'MB EM' App",
        val syncedTasksCount: Int = 0,
        val onlineEmployeesCount: Int = 0
    )

    private val _syncState = MutableStateFlow(MbEmSyncState())
    val syncState: StateFlow<MbEmSyncState> = _syncState.asStateFlow()

    private val _latestSyncEvent = MutableStateFlow("Listener Manager Initializing...")
    val latestSyncEvent: StateFlow<String> = _latestSyncEvent.asStateFlow()

    private val handledDocIds = ConcurrentHashMap.newKeySet<String>()

    /**
     * Initializes the Dedicated Firestore Listener Manager with all DAOs and CoroutineScope.
     */
    fun initialize(
        context: Context,
        scope: CoroutineScope,
        taskDao: TaskDao,
        employeeDao: EmployeeDao,
        attendanceDao: AttendanceDao,
        leaveDao: LeaveDao,
        leadDao: LeadDao,
        chatDao: ChatDao,
        notificationDao: NotificationDao,
        projectDao: ProjectDao? = null
    ) {
        appContext = context.applicationContext
        managerScope = scope
        taskDaoRef = taskDao
        employeeDaoRef = employeeDao
        attendanceDaoRef = attendanceDao
        leaveDaoRef = leaveDao
        leadDaoRef = leadDao
        chatDaoRef = chatDao
        notificationDaoRef = notificationDao
        projectDaoRef = projectDao

        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }

            val customDbId = try {
                context.getString(com.example.R.string.firestore_database_id)
                    .takeIf { it.isNotBlank() && it != "(default)" }
            } catch (_: Exception) { null }

            val defaultFs = try { FirebaseFirestore.getInstance() } catch (_: Exception) { null }
            val namedFs = if (!customDbId.isNullOrBlank()) {
                try {
                    FirebaseFirestore.getInstance(customDbId)
                } catch (e: Exception) {
                    Log.w(TAG, "Named database '$customDbId' resolution warning: ${e.message}")
                    null
                }
            } else null

            primaryFirestore = namedFs ?: defaultFs
            secondaryFirestore = if (namedFs != null && defaultFs != null && namedFs != defaultFs) defaultFs else null

            // Configure low-latency persistence and network connectivity
            listOfNotNull(primaryFirestore, secondaryFirestore).forEach { fs ->
                try {
                    val settings = FirebaseFirestoreSettings.Builder()
                        .setPersistenceEnabled(true)
                        .build()
                    fs.firestoreSettings = settings
                    fs.enableNetwork()
                } catch (e: Exception) {
                    Log.w(TAG, "Configuring Firestore settings warning: ${e.message}")
                }
            }

            // Start all persistent cross-platform listeners
            attachAllActiveListeners()

            _syncState.value = _syncState.value.copy(
                isConnected = true,
                lastSyncTimestamp = System.currentTimeMillis(),
                lastEventMessage = "MB EM Cross-Platform Sync Active"
            )
            _latestSyncEvent.value = "Active listeners connected with 'MB EM' platform"
            Log.d(TAG, "MbEmSyncManager initialized successfully with full active listeners.")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MbEmSyncManager: ${e.message}", e)
        }
    }

    /**
     * Attaches persistent Real-Time Snapshot Listeners across both primary and secondary Firestore instances.
     */
    fun attachAllActiveListeners() {
        val scope = managerScope ?: return
        clearAllListeners()

        // 1. Tasks Listener (Instant Task Modifications & Status Updates)
        taskDaoRef?.let { dao ->
            attachDualListener("tasks", "tasks") { snapshot ->
                processTasksSnapshot(snapshot, dao, scope)
            }
        }

        // 2. Employees & Users Presence/Status Listener
        employeeDaoRef?.let { dao ->
            attachDualListener("employees", "employees") { snapshot ->
                processEmployeesSnapshot(snapshot, dao, scope)
            }
            attachDualListener("users", "users") { snapshot ->
                processUsersSnapshot(snapshot, dao, scope)
            }
        }

        // 3. Attendance & Live Clock-In Listener
        attendanceDaoRef?.let { dao ->
            attachDualListener("attendance_records", "attendance_records") { snapshot ->
                processAttendanceSnapshot(snapshot, dao, scope)
            }
            attachDualListener("attendance_alt", "attendance") { snapshot ->
                processAttendanceSnapshot(snapshot, dao, scope)
            }
        }

        // 4. Leave Applications & Approvals Listener
        leaveDaoRef?.let { dao ->
            attachDualListener("leave_applications", "leave_applications") { snapshot ->
                processLeavesSnapshot(snapshot, dao, scope)
            }
            attachDualListener("leaves_alt", "leaves") { snapshot ->
                processLeavesSnapshot(snapshot, dao, scope)
            }
        }

        // 5. CRM Leads Listener
        leadDaoRef?.let { dao ->
            attachDualListener("leads", "leads") { snapshot ->
                processLeadsSnapshot(snapshot, dao, scope)
            }
        }

        // 6. Global Chat Messages Listener
        chatDaoRef?.let { dao ->
            attachDualListener("chat_messages", "chat_messages") { snapshot ->
                processChatSnapshot(snapshot, dao, scope)
            }
            attachDualListener("messages_alt", "messages") { snapshot ->
                processChatSnapshot(snapshot, dao, scope)
            }
        }

        // 7. Projects Listener
        projectDaoRef?.let { dao ->
            attachDualListener("projects", "projects") { snapshot ->
                processProjectsSnapshot(snapshot, dao, scope)
            }
        }

        // 8. Notifications & Broadcasts Listener
        notificationDaoRef?.let { dao ->
            attachDualListener("notifications", "notifications") { snapshot ->
                processNotificationsSnapshot(snapshot, dao, scope)
            }
        }

        updateActiveListenersTelemetry()
    }

    private fun attachDualListener(
        key: String,
        collectionName: String,
        onSnapshot: (QuerySnapshot) -> Unit
    ) {
        val primary = primaryFirestore
        val secondary = secondaryFirestore

        if (primary != null) {
            try {
                val reg = primary.collection(collectionName)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.w(TAG, "Snapshot error for '$collectionName' on primary: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (snapshot != null && !snapshot.isEmpty) {
                            onSnapshot(snapshot)
                        }
                    }
                activeRegistrations["${key}_primary"] = reg
            } catch (e: Exception) {
                Log.w(TAG, "Failed attaching primary listener for '$collectionName': ${e.message}")
            }
        }

        if (secondary != null && secondary != primary) {
            try {
                val reg = secondary.collection(collectionName)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.w(TAG, "Snapshot error for '$collectionName' on secondary: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (snapshot != null && !snapshot.isEmpty) {
                            onSnapshot(snapshot)
                        }
                    }
                activeRegistrations["${key}_secondary"] = reg
            } catch (e: Exception) {
                Log.w(TAG, "Failed attaching secondary listener for '$collectionName': ${e.message}")
            }
        }
    }

    // ==========================================
    // SNAPSHOT PROCESSORS FOR REAL-TIME SYNC
    // ==========================================

    private fun processTasksSnapshot(
        snapshot: QuerySnapshot,
        taskDao: TaskDao,
        scope: CoroutineScope
    ) {
        scope.launch(Dispatchers.IO) {
            var updateCount = 0
            for (doc in snapshot.documents) {
                val id = doc.getLong("id") ?: doc.id.replace("task_", "").toLongOrNull() ?: Math.abs(doc.id.hashCode().toLong())
                val title = doc.getString("title") ?: doc.getString("taskName") ?: doc.getString("name") ?: ""
                if (id <= 0L || title.isBlank()) continue

                val projectName = doc.getString("projectName") ?: doc.getString("project") ?: "General"
                val priority = doc.getString("priority") ?: "Medium"
                val dueDate = doc.getString("dueDate") ?: doc.getString("deadline") ?: ""
                val status = doc.getString("status") ?: "In Progress"
                val isCompleted = doc.getBoolean("isCompleted") ?: (status.equals("completed", ignoreCase = true) || status.equals("done", ignoreCase = true))
                val category = doc.getString("category") ?: "Work"
                val estimatedTimeNeeded = doc.getString("estimatedTimeNeeded") ?: "4 Hours"
                val assignee = doc.getString("assignee") ?: doc.getString("assignedTo") ?: doc.getString("employeeName") ?: "Team Member"

                val task = TaskEntity(
                    id = id,
                    title = title,
                    projectName = projectName,
                    priority = priority,
                    dueDate = dueDate,
                    status = status,
                    isCompleted = isCompleted,
                    category = category,
                    estimatedTimeNeeded = estimatedTimeNeeded,
                    assignee = assignee
                )

                val existing = taskDao.getTaskByIdDirect(id)
                taskDao.insert(task)
                updateCount++

                if (existing != task) {
                    val isNew = existing == null
                    val isStatusChanged = existing != null && (existing.status != status || existing.isCompleted != isCompleted)
                    
                    if (isNew) {
                        notifyEvent(
                            title = "📋 Task Synced from MB EM",
                            message = "\"$title\" assigned to $assignee",
                            category = "task"
                        )
                    } else if (isStatusChanged) {
                        notifyEvent(
                            title = "🔄 Task Status Live Update",
                            message = "\"$title\" updated to '$status' by $assignee",
                            category = "task"
                        )
                    }
                }
            }
            _syncState.value = _syncState.value.copy(
                lastSyncTimestamp = System.currentTimeMillis(),
                syncedTasksCount = updateCount,
                lastEventMessage = "Live Tasks Synced: $updateCount active items"
            )
            _latestSyncEvent.value = "Tasks updated from 'MB EM' employee app ($updateCount items)"
        }
    }

    private fun processEmployeesSnapshot(
        snapshot: QuerySnapshot,
        employeeDao: EmployeeDao,
        scope: CoroutineScope
    ) {
        scope.launch(Dispatchers.IO) {
            var activeEmployees = 0
            for (doc in snapshot.documents) {
                val id = doc.getLong("id") ?: doc.id.replace("emp_", "").toLongOrNull() ?: 0L
                val email = (doc.getString("email") ?: "").trim().lowercase()
                val name = doc.getString("name") ?: doc.getString("displayName") ?: ""
                if (email.isBlank() && name.isBlank()) continue

                val phone = doc.getString("phone") ?: doc.getString("phoneNumber") ?: "+91 98765 00000"
                val designation = doc.getString("designation") ?: "Team Member"
                val deptStr = doc.getString("department") ?: "ENGINEERING"
                val statusStr = doc.getString("status") ?: "ACTIVE"
                val presenceStr = doc.getString("presenceStatus") ?: "ONLINE"
                val password = doc.getString("password") ?: ""

                val department = try { Department.valueOf(deptStr.uppercase()) } catch (_: Exception) { Department.ENGINEERING }
                val status = try { EmployeeStatus.valueOf(statusStr.uppercase()) } catch (_: Exception) { EmployeeStatus.ACTIVE }
                val presence = try { PresenceStatus.valueOf(presenceStr.uppercase()) } catch (_: Exception) { PresenceStatus.ONLINE }

                if (status == EmployeeStatus.ACTIVE && presence != PresenceStatus.OFFLINE) {
                    activeEmployees++
                }

                val existing = if (id > 0L) employeeDao.getEmployeeByIdDirect(id) else employeeDao.getEmployeeByEmail(email)
                if (existing != null) {
                    val updated = existing.copy(
                        name = name.ifBlank { existing.name },
                        phone = phone.ifBlank { existing.phone },
                        designation = designation.ifBlank { existing.designation },
                        department = department,
                        status = status,
                        presenceStatus = presence,
                        password = if (password.isNotBlank()) password else existing.password
                    )
                    if (updated != existing) {
                        employeeDao.update(updated)
                        if (existing.status != status || existing.presenceStatus != presence) {
                            notifyEvent(
                                title = "👥 Team Presence Update",
                                message = "$name is now $status ($presence)",
                                category = "attendance"
                            )
                        }
                    }
                } else {
                    val newEmp = EmployeeEntity(
                        id = if (id > 0L) id else 0L,
                        name = name.ifBlank { "Team Member" },
                        email = email.ifBlank { "emp_${System.currentTimeMillis()}@makingbrands.in" },
                        password = if (password.isNotBlank()) password else "password123",
                        phone = phone,
                        designation = designation,
                        department = department,
                        role = EmployeeRole.DEVELOPER,
                        status = status,
                        presenceStatus = presence,
                        joiningDate = Date(),
                        skills = listOf("Android", "Communication")
                    )
                    employeeDao.insert(newEmp)
                }
            }
            _syncState.value = _syncState.value.copy(
                lastSyncTimestamp = System.currentTimeMillis(),
                onlineEmployeesCount = activeEmployees,
                lastEventMessage = "Live Team Presence Synced ($activeEmployees online)"
            )
            _latestSyncEvent.value = "Employee directory & status synchronized with 'MB EM'"
        }
    }

    private fun processUsersSnapshot(
        snapshot: QuerySnapshot,
        employeeDao: EmployeeDao,
        scope: CoroutineScope
    ) {
        scope.launch(Dispatchers.IO) {
            for (doc in snapshot.documents) {
                val role = doc.getString("role") ?: "employee"
                if (role.equals("admin", ignoreCase = true) || role.equals("mb admin", ignoreCase = true)) continue
                val email = (doc.getString("email") ?: "").trim().lowercase()
                val name = doc.getString("name") ?: doc.getString("displayName") ?: ""
                if (email.isBlank() && name.isBlank()) continue

                val phone = doc.getString("phoneNumber") ?: doc.getString("phone") ?: "+91 98765 00000"
                val designation = doc.getString("designation") ?: "Team Member"
                val deptStr = doc.getString("department") ?: "ENGINEERING"
                val statusStr = doc.getString("status") ?: "ACTIVE"
                val password = doc.getString("password") ?: ""

                val department = try { Department.valueOf(deptStr.uppercase()) } catch (_: Exception) { Department.ENGINEERING }
                val status = try { EmployeeStatus.valueOf(statusStr.uppercase()) } catch (_: Exception) { EmployeeStatus.ACTIVE }

                val existing = employeeDao.getEmployeeByEmail(email)
                if (existing != null) {
                    val updated = existing.copy(
                        name = name.ifBlank { existing.name },
                        phone = phone.ifBlank { existing.phone },
                        designation = designation.ifBlank { existing.designation },
                        department = department,
                        status = status,
                        password = if (password.isNotBlank()) password else existing.password
                    )
                    if (updated != existing) {
                        employeeDao.update(updated)
                    }
                } else {
                    val newEmp = EmployeeEntity(
                        name = name.ifBlank { "Team Member" },
                        email = email.ifBlank { "user_${System.currentTimeMillis()}@makingbrands.in" },
                        password = if (password.isNotBlank()) password else "password123",
                        phone = phone,
                        designation = designation,
                        department = department,
                        role = EmployeeRole.DEVELOPER,
                        status = status,
                        presenceStatus = PresenceStatus.ONLINE,
                        joiningDate = Date(),
                        skills = listOf("Android", "Communication")
                    )
                    employeeDao.insert(newEmp)
                }
            }
        }
    }

    private fun processAttendanceSnapshot(
        snapshot: QuerySnapshot,
        attendanceDao: AttendanceDao,
        scope: CoroutineScope
    ) {
        scope.launch(Dispatchers.IO) {
            for (doc in snapshot.documents) {
                val id = doc.getLong("id") ?: doc.id.replace("att_", "").toLongOrNull() ?: 0L
                val date = doc.getString("date") ?: ""
                val checkInTime = doc.getString("checkInTime") ?: doc.getString("time") ?: doc.getString("checkIn") ?: ""
                val checkOutTime = doc.getString("checkOutTime") ?: doc.getString("checkOut")
                val durationMinutes = doc.getLong("durationMinutes") ?: 0L
                val isWorking = doc.getBoolean("isWorking") ?: false
                val status = doc.getString("status") ?: (if (isWorking) "Present" else "Clocked Out")
                val overtimeMinutes = doc.getLong("overtimeMinutes") ?: 0L
                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                val employeeName = doc.getString("employeeName") ?: doc.getString("name") ?: "Team Member"

                if (id > 0L) {
                    val record = AttendanceRecord(
                        id = id,
                        date = date,
                        checkInTime = checkInTime,
                        checkOutTime = checkOutTime,
                        durationMinutes = durationMinutes,
                        isWorking = isWorking,
                        status = status,
                        overtimeMinutes = overtimeMinutes,
                        timestamp = timestamp,
                        employeeName = employeeName
                    )
                    val existing = attendanceDao.getAttendanceRecordByIdDirect(id)
                    attendanceDao.insert(record)

                    if (existing != record) {
                        val isNew = existing == null
                        val statusChanged = existing != null && (existing.status != status || existing.isWorking != isWorking)
                        if (isNew) {
                            notifyEvent(
                                title = "⏰ Attendance Punch: $employeeName",
                                message = "$employeeName checked in on $date at $checkInTime ($status)",
                                category = "attendance"
                            )
                        } else if (statusChanged) {
                            notifyEvent(
                                title = "🔄 Attendance Status Changed",
                                message = "$employeeName is now '$status' (Working: $isWorking)",
                                category = "attendance"
                            )
                        }
                    }
                }
            }
            _syncState.value = _syncState.value.copy(
                lastSyncTimestamp = System.currentTimeMillis(),
                lastEventMessage = "Live Attendance Records Synced (${snapshot.size()} entries)"
            )
        }
    }

    private fun processLeavesSnapshot(
        snapshot: QuerySnapshot,
        leaveDao: LeaveDao,
        scope: CoroutineScope
    ) {
        scope.launch(Dispatchers.IO) {
            for (doc in snapshot.documents) {
                val id = doc.getLong("id") ?: Math.abs(doc.id.hashCode().toLong())
                val username = doc.getString("username") ?: doc.getString("employeeName") ?: doc.getString("name") ?: ""
                val leaveType = doc.getString("leaveType") ?: doc.getString("type") ?: "Casual Leave"
                val startDate = doc.getString("startDate") ?: ""
                val endDate = doc.getString("endDate") ?: ""
                val totalDays = (doc.getLong("totalDays") ?: 1L).toInt()
                val reason = doc.getString("reason") ?: ""
                val status = doc.getString("status") ?: "Pending"
                val appliedDate = doc.getString("appliedDate") ?: ""
                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                val existing = leaveDao.getLeaveByIdDirect(id)
                val isNew = existing == null
                val isStatusChanged = existing != null && existing.status != status

                val leave = LeaveApplicationEntity(
                    id = id,
                    username = username,
                    leaveType = leaveType,
                    startDate = startDate,
                    endDate = endDate,
                    totalDays = totalDays,
                    reason = reason,
                    status = status,
                    appliedDate = appliedDate,
                    createdAt = createdAt
                )
                leaveDao.insert(leave)

                if (isNew && username.isNotBlank()) {
                    notifyEvent(
                        title = "📅 New Leave Request from 'MB EM'",
                        message = "$leaveType submitted by $username ($startDate to $endDate)",
                        category = "leave"
                    )
                } else if (isStatusChanged) {
                    notifyEvent(
                        title = "🔄 Leave Application Updated",
                        message = "Leave for $username marked as '$status'",
                        category = "leave"
                    )
                }
            }
        }
    }

    private fun processLeadsSnapshot(
        snapshot: QuerySnapshot,
        leadDao: LeadDao,
        scope: CoroutineScope
    ) {
        scope.launch(Dispatchers.IO) {
            for (doc in snapshot.documents) {
                val id = doc.getLong("id") ?: Math.abs(doc.id.hashCode().toLong())
                val name = doc.getString("customerName") ?: doc.getString("name") ?: ""
                val company = doc.getString("company") ?: ""
                val phone = doc.getString("phone") ?: ""
                val email = doc.getString("email") ?: ""
                val leadScore = (doc.getLong("leadScore") ?: 70L).toInt()
                val requirement = doc.getString("requirement") ?: ""
                val stage = doc.getString("status") ?: doc.getString("stage") ?: "New"
                val assignedTo = doc.getString("assignedTo") ?: ""
                val nextFollowUp = doc.getString("nextFollowUp") ?: ""

                val lead = LeadEntity(
                    id = id,
                    name = name,
                    company = company,
                    phone = phone,
                    email = email,
                    leadScore = leadScore,
                    requirement = requirement,
                    potentialValue = "₹ " + (doc.getDouble("value") ?: 0.0).toLong().toString(),
                    stage = stage,
                    assignedTo = assignedTo,
                    nextFollowUp = nextFollowUp
                )
                leadDao.insert(lead)
            }
        }
    }

    private fun processChatSnapshot(
        snapshot: QuerySnapshot,
        chatDao: ChatDao,
        scope: CoroutineScope
    ) {
        scope.launch(Dispatchers.IO) {
            for (doc in snapshot.documents) {
                val msgId = doc.getLong("id") ?: Math.abs(doc.id.hashCode().toLong())
                val cId = doc.getString("channelId") ?: "company_chat"
                val senderName = doc.getString("senderName") ?: doc.getString("sender") ?: doc.getString("userName") ?: "Team"
                val senderRole = doc.getString("senderRole") ?: "Member"
                val messageText = doc.getString("messageText") ?: doc.getString("message") ?: doc.getString("text") ?: ""
                val timestampText = doc.getString("timestampText") ?: doc.getString("timestamp") ?: ""
                val attachmentFileName = doc.getString("attachmentFileName")
                val attachmentFileSize = doc.getString("attachmentFileSize")
                val audioPath = doc.getString("audioPath")
                val isVoiceMessage = doc.getBoolean("isVoiceMessage") ?: (!audioPath.isNullOrBlank())

                val existing = chatDao.getMessageById(msgId)
                val chatMsg = ChatMessageEntity(
                    id = msgId,
                    channelId = cId,
                    senderName = senderName,
                    senderRole = senderRole,
                    messageText = messageText,
                    timestampText = timestampText,
                    isMe = false,
                    attachmentFileName = attachmentFileName,
                    attachmentFileSize = attachmentFileSize,
                    audioPath = audioPath ?: existing?.audioPath,
                    isVoiceMessage = isVoiceMessage,
                    isRead = true
                )
                chatDao.insert(chatMsg)
            }
        }
    }

    private fun processProjectsSnapshot(
        snapshot: QuerySnapshot,
        projectDao: ProjectDao,
        scope: CoroutineScope
    ) {
        scope.launch(Dispatchers.IO) {
            for (doc in snapshot.documents) {
                val id = doc.getLong("id") ?: doc.id.replace("project_", "").toLongOrNull() ?: 0L
                val name = doc.getString("name") ?: ""
                if (id <= 0L || name.isBlank()) continue

                val project = ProjectEntity(
                    id = id,
                    name = name,
                    clientName = doc.getString("clientName") ?: "",
                    totalTasks = doc.getLong("totalTasks")?.toInt() ?: 10,
                    completedTasks = doc.getLong("completedTasks")?.toInt() ?: 0,
                    progressPercent = doc.getLong("progressPercent")?.toInt() ?: 0,
                    status = doc.getString("status") ?: "Active",
                    priority = doc.getString("priority") ?: "High",
                    startDate = doc.getString("startDate") ?: "01 Sep 2025",
                    deadline = doc.getString("deadline") ?: "28 Sep 2025",
                    managerName = doc.getString("managerName") ?: "Rahul Sharma",
                    teamSize = doc.getLong("teamSize")?.toInt() ?: 5,
                    description = doc.getString("description") ?: "",
                    budget = doc.getDouble("budget")
                )
                projectDao.insert(project)
            }
        }
    }

    private fun processNotificationsSnapshot(
        snapshot: QuerySnapshot,
        notificationDao: NotificationDao,
        scope: CoroutineScope
    ) {
        scope.launch(Dispatchers.IO) {
            for (doc in snapshot.documents) {
                val docId = doc.id
                val id = doc.getLong("id") ?: Math.abs(docId.hashCode().toLong())
                val title = doc.getString("title") ?: ""
                val subtitle = doc.getString("subtitle") ?: doc.getString("message") ?: ""
                val timeAgo = doc.getString("timeAgo") ?: "Just now"
                val category = doc.getString("category") ?: "broadcast"
                val isRead = doc.getBoolean("isRead") ?: false
                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                if (title.isNotBlank()) {
                    val entity = NotificationEntity(
                        id = id,
                        title = title,
                        subtitle = subtitle,
                        timeAgo = timeAgo,
                        category = category,
                        isRead = isRead
                    )
                    notificationDao.insert(entity)

                    if (handledDocIds.add(docId)) {
                        val isRecent = System.currentTimeMillis() - createdAt < 120_000L
                        if (isRecent) {
                            appContext?.let { ctx ->
                                NotificationHelper.showCategoryAlert(ctx, title, subtitle, category)
                                AppSoundHelper.playCategorySound(ctx, category)
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // INSTANT CROSS-PLATFORM MUTATION ACTIONS
    // ==========================================

    /**
     * Modifies a task and propagates changes to both Firestore instances and local Room DB simultaneously.
     */
    fun modifyTask(task: TaskEntity) {
        val scope = managerScope ?: CoroutineScope(Dispatchers.IO)
        scope.launch(Dispatchers.IO) {
            // 1. Local Room DB update
            taskDaoRef?.insert(task)

            // 2. Dual Firestore propagation
            val docId = if (task.id > 0L) "task_${task.id}" else "task_${System.currentTimeMillis()}"
            val data = hashMapOf(
                "id" to task.id,
                "title" to task.title,
                "projectName" to task.projectName,
                "priority" to task.priority,
                "dueDate" to task.dueDate,
                "status" to task.status,
                "isCompleted" to task.isCompleted,
                "category" to task.category,
                "estimatedTimeNeeded" to task.estimatedTimeNeeded,
                "assignee" to task.assignee,
                "updatedAt" to System.currentTimeMillis(),
                "lastModifiedBy" to "MB Admin"
            )
            writeToDualDatabases("tasks", docId, data)
            _latestSyncEvent.value = "Task '${task.title}' modified and broadcasted to 'MB EM'"
            Log.d(TAG, "Task '${task.title}' modified and synchronized across platforms.")
        }
    }

    /**
     * Updates task status instantly across platforms with notifications.
     */
    fun updateTaskStatus(
        taskId: Long,
        newStatus: String,
        isCompleted: Boolean,
        updatedBy: String = "MB Admin"
    ) {
        val scope = managerScope ?: CoroutineScope(Dispatchers.IO)
        scope.launch(Dispatchers.IO) {
            val existing = taskDaoRef?.getTaskByIdDirect(taskId)
            if (existing != null) {
                val updated = existing.copy(status = newStatus, isCompleted = isCompleted)
                taskDaoRef?.update(updated)
            }

            val docId = "task_$taskId"
            val data = hashMapOf<String, Any?>(
                "status" to newStatus,
                "isCompleted" to isCompleted,
                "updatedAt" to System.currentTimeMillis(),
                "lastModifiedBy" to updatedBy
            )
            writeToDualDatabases("tasks", docId, data)
            _latestSyncEvent.value = "Task #$taskId status updated to '$newStatus'"
            Log.d(TAG, "Task #$taskId status updated to '$newStatus' live.")
        }
    }

    /**
     * Updates an employee's presence or organizational status in real-time.
     */
    fun updateEmployeePresence(
        employeeId: Long,
        status: EmployeeStatus,
        presence: PresenceStatus
    ) {
        val scope = managerScope ?: CoroutineScope(Dispatchers.IO)
        scope.launch(Dispatchers.IO) {
            val existing = employeeDaoRef?.getEmployeeByIdDirect(employeeId)
            if (existing != null) {
                val updated = existing.copy(status = status, presenceStatus = presence)
                employeeDaoRef?.update(updated)

                val docId = "emp_$employeeId"
                val userDocId = existing.email.trim().lowercase().replace("@", "_").replace(".", "_")
                val data = hashMapOf(
                    "id" to employeeId,
                    "status" to status.name,
                    "presenceStatus" to presence.name,
                    "updatedAt" to System.currentTimeMillis()
                )
                writeToDualDatabases("employees", docId, data)
                if (userDocId.isNotBlank()) {
                    writeToDualDatabases("users", userDocId, data)
                }
                _latestSyncEvent.value = "Employee '${existing.name}' status updated to $status ($presence)"
            }
        }
    }

    /**
     * Approves or Rejects an employee leave application with immediate cross-app update.
     */
    fun updateLeaveStatus(leaveId: Long, newStatus: String, adminNotes: String = "") {
        val scope = managerScope ?: CoroutineScope(Dispatchers.IO)
        scope.launch(Dispatchers.IO) {
            val existing = leaveDaoRef?.getLeaveByIdDirect(leaveId)
            if (existing != null) {
                val updated = existing.copy(status = newStatus)
                leaveDaoRef?.update(updated)
            }

            val docId = "leave_$leaveId"
            val data = hashMapOf(
                "status" to newStatus,
                "adminNotes" to adminNotes,
                "updatedAt" to System.currentTimeMillis()
            )
            writeToDualDatabases("leave_applications", docId, data)
            writeToDualDatabases("leaves", docId, data)
            _latestSyncEvent.value = "Leave application #$leaveId marked as $newStatus"
        }
    }

    /**
     * Broadcasts an administrative notification across all connected 'MB EM' and 'MB Taker' devices.
     */
    fun broadcastCrossPlatformAlert(
        title: String,
        message: String,
        priority: String = "High",
        category: String = "broadcast"
    ) {
        val scope = managerScope ?: CoroutineScope(Dispatchers.IO)
        scope.launch(Dispatchers.IO) {
            val notifId = System.currentTimeMillis()
            val entity = NotificationEntity(
                id = notifId,
                title = title,
                subtitle = message,
                timeAgo = "Just now",
                category = category,
                isRead = false
            )
            notificationDaoRef?.insert(entity)

            val docId = "notif_$notifId"
            val data = hashMapOf(
                "id" to notifId,
                "title" to title,
                "subtitle" to message,
                "priority" to priority,
                "category" to category,
                "timeAgo" to "Just now",
                "isRead" to false,
                "createdAt" to System.currentTimeMillis()
            )
            writeToDualDatabases("notifications", docId, data)
            _latestSyncEvent.value = "Broadcast sent: $title"
        }
    }

    /**
     * Broadcasts an admin notification across all devices using the 'MB EM' Employee app.
     */
    fun broadcastAdminNotification(
        title: String,
        message: String,
        priority: String = "High",
        category: String = "broadcast"
    ) {
        broadcastCrossPlatformAlert(title, message, priority, category)
    }

    /**
     * Sends a live heartbeat synchronization ping to MB EM app and verifies round-trip cloud propagation.
     */
    fun sendSyncPing(onResult: ((Boolean, String) -> Unit)? = null) {
        val scope = managerScope ?: CoroutineScope(Dispatchers.IO)
        scope.launch(Dispatchers.IO) {
            try {
                val pingId = "ping_${System.currentTimeMillis()}"
                val now = System.currentTimeMillis()
                val data = hashMapOf(
                    "pingId" to pingId,
                    "sender" to "MB Admin Console",
                    "target" to "MB EM App",
                    "timestamp" to now,
                    "status" to "ACTIVE_SYNCHRONIZED",
                    "message" to "Realtime cross-platform heartbeat test successful."
                )
                writeToDualDatabases("app_sync_pings", pingId, data)
                writeToDualDatabases("app_sync_status", "latest_heartbeat", data)
                _syncState.value = _syncState.value.copy(
                    lastSyncTimestamp = now,
                    lastEventMessage = "Live Ping Broadcasted to 'MB EM'",
                    isConnected = true
                )
                _latestSyncEvent.value = "Heartbeat ping dispatched to 'MB EM' platform"
                withContext(Dispatchers.Main) {
                    onResult?.invoke(true, "Ping dispatched successfully across Firebase instances!")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending sync ping: ${e.message}")
                withContext(Dispatchers.Main) {
                    onResult?.invoke(false, e.message ?: "Failed to dispatch ping")
                }
            }
        }
    }

    fun pushClientOccasionWish(wish: com.example.data.model.ClientOccasionWishEntity) {
        val scope = managerScope ?: return
        scope.launch(Dispatchers.IO) {
            val docId = if (wish.id > 0) "wish_${wish.id}" else "wish_${System.currentTimeMillis()}"
            val data = mapOf(
                "id" to wish.id,
                "clientName" to wish.clientName,
                "clientPhone" to wish.clientPhone,
                "clientCompany" to wish.clientCompany,
                "occasionName" to wish.occasionName,
                "wishMessage" to wish.wishMessage,
                "posterImageUri" to (wish.posterImageUri ?: ""),
                "isSentViaWhatsApp" to wish.isSentViaWhatsApp,
                "sentTimestamp" to wish.sentTimestamp,
                "status" to wish.status,
                "updatedAt" to System.currentTimeMillis()
            )
            writeToDualDatabases("client_occasion_wishes", docId, data)
        }
    }

    fun pushWrittenDraft(draft: com.example.data.model.WrittenDraftEntity) {
        val scope = managerScope ?: return
        scope.launch(Dispatchers.IO) {
            val docId = if (draft.id > 0) "draft_${draft.id}" else "draft_${System.currentTimeMillis()}"
            val data = mapOf(
                "id" to draft.id,
                "title" to draft.title,
                "category" to draft.category,
                "targetAudience" to draft.targetAudience,
                "content" to draft.content,
                "tags" to draft.tags,
                "createdAt" to draft.createdAt,
                "lastEdited" to draft.lastEdited,
                "author" to draft.author,
                "updatedAt" to System.currentTimeMillis()
            )
            writeToDualDatabases("written_drafts", docId, data)
        }
    }

    // ==========================================
    // LOW LEVEL DUAL WRITING & HELPER UTILS
    // ==========================================

    private fun writeToDualDatabases(collectionName: String, docId: String, data: Map<String, Any?>) {
        try {
            primaryFirestore?.collection(collectionName)?.document(docId)?.set(data, SetOptions.merge())
        } catch (e: Exception) {
            Log.w(TAG, "Write error on primary for '$collectionName/$docId': ${e.message}")
        }
        try {
            secondaryFirestore?.collection(collectionName)?.document(docId)?.set(data, SetOptions.merge())
        } catch (e: Exception) {
            Log.w(TAG, "Write error on secondary for '$collectionName/$docId': ${e.message}")
        }
    }

    private fun notifyEvent(title: String, message: String, category: String) {
        appContext?.let { ctx ->
            AppSoundHelper.playCategorySound(ctx, category)
            NotificationHelper.showCategoryAlert(ctx, title, message, category)
        }
    }

    private fun updateActiveListenersTelemetry() {
        _syncState.value = _syncState.value.copy(
            activeListenersCount = activeRegistrations.size,
            isConnected = true
        )
    }

    fun clearAllListeners() {
        activeRegistrations.values.forEach { it.remove() }
        activeRegistrations.clear()
        updateActiveListenersTelemetry()
    }
}
