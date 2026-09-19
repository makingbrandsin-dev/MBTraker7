package com.example.data.firebase

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.data.local.AttendanceDao
import com.example.data.local.LeadDao
import com.example.data.local.TaskDao
import com.example.data.local.UserProfileDao
import com.example.data.model.AttendanceRecord
import com.example.data.model.LeadEntity
import com.example.data.model.TaskEntity
import com.example.data.model.UserProfileEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

enum class NetworkSyncStatus {
    SYNCED,
    SYNCING,
    OFFLINE,
    ERROR
}

data class SyncQueueSummary(
    val pendingLeads: Int = 0,
    val pendingTasks: Int = 0,
    val pendingAttendance: Int = 0
) {
    val totalPending: Int get() = pendingLeads + pendingTasks + pendingAttendance
}

data class SyncState(
    val status: NetworkSyncStatus = NetworkSyncStatus.SYNCED,
    val isOnline: Boolean = true,
    val isSyncing: Boolean = false,
    val isSimulatedOffline: Boolean = false,
    val pendingSummary: SyncQueueSummary = SyncQueueSummary(),
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val statusMessage: String = "Firebase Realtime: Live & Synced"
) {
    val formattedLastSyncTime: String
        get() {
            val now = System.currentTimeMillis()
            val diffSec = (now - lastSyncTimestamp) / 1000
            return when {
                diffSec < 15 -> "Just now"
                diffSec < 60 -> "${diffSec}s ago"
                diffSec < 3600 -> "${diffSec / 60}m ago"
                else -> SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(lastSyncTimestamp))
            }
        }
}

object FirebaseRealtimeManager {
    private const val TAG = "FirebaseRealtimeManager"
    private var firestore: FirebaseFirestore? = null
    private var attendanceListener: ListenerRegistration? = null
    private var taskListener: ListenerRegistration? = null
    private var profileListener: ListenerRegistration? = null
    private var isInitialized = false
    private var appScope: CoroutineScope? = null

    // Pending Sync Queues for offline mutations
    private val pendingLeadsQueue = ConcurrentLinkedQueue<LeadEntity>()
    private val pendingTasksQueue = ConcurrentLinkedQueue<TaskEntity>()
    private val pendingAttendanceQueue = ConcurrentLinkedQueue<AttendanceRecord>()

    private var isRealNetworkConnected = true
    private var isSimulatedOffline = false

    private val _syncState = MutableStateFlow(
        SyncState(
            status = NetworkSyncStatus.SYNCED,
            isOnline = true,
            isSyncing = false,
            lastSyncTimestamp = System.currentTimeMillis(),
            statusMessage = "Firebase Realtime: Live & Synced"
        )
    )
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // Backward compatibility flows
    val isRealtimeConnected: StateFlow<Boolean>
        get() = MutableStateFlow(_syncState.value.isOnline && _syncState.value.status != NetworkSyncStatus.OFFLINE)
    val lastSyncTimestamp: StateFlow<Long>
        get() = MutableStateFlow(_syncState.value.lastSyncTimestamp)
    val syncStatus: StateFlow<String>
        get() = MutableStateFlow(_syncState.value.statusMessage)

    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    fun initialize(
        context: Context,
        attendanceDao: AttendanceDao,
        userProfileDao: UserProfileDao,
        taskDao: TaskDao? = null,
        leadDao: LeadDao? = null,
        scope: CoroutineScope
    ) {
        appScope = scope
        registerNetworkCallback(context)
        NotificationHelper.createNotificationChannels(context)

        if (isInitialized && firestore != null) {
            fetchAndRegisterFcmToken(context)
            return
        }

        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:371778881998:android:com_aistudio_mbtraker_hqxk")
                    .setProjectId("mbtraker-hrms-realtime")
                    .setApiKey("AIzaSyRealtimeEvaluationKeyForMBTracker2026")
                    .build()
                FirebaseApp.initializeApp(context, options)
                Log.d(TAG, "Initialized default FirebaseApp with options")
            }

            firestore = FirebaseFirestore.getInstance()
            isInitialized = true
            updateSyncState(
                status = NetworkSyncStatus.SYNCED,
                isOnline = isEffectiveOnline(),
                statusMessage = "Firebase Realtime: Connected"
            )

            // Register and fetch FCM token
            fetchAndRegisterFcmToken(context)

            // Attach real-time snapshot listeners
            startRealtimeAttendanceListener(attendanceDao, scope)
            startRealtimeProfileListener(userProfileDao, scope)
            if (taskDao != null) {
                startRealtimeTaskListener(taskDao, scope)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization error: ${e.message}", e)
            updateSyncState(
                status = NetworkSyncStatus.OFFLINE,
                isOnline = false,
                statusMessage = "Firebase Standby: Offline Mode"
            )
        }
    }

    private fun fetchAndRegisterFcmToken(context: Context) {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d(TAG, "Fetched Firebase Cloud Messaging Token: $token")
                    updateFcmToken(token)
                } else {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseMessaging initialization skipped: ${e.message}")
        }
    }

    fun updateFcmToken(token: String?) {
        if (token.isNullOrBlank()) return
        _fcmToken.value = token
        appScope?.launch(Dispatchers.IO) {
            try {
                firestore?.collection("fcm_devices")?.document("device_default")?.set(
                    mapOf(
                        "token" to token,
                        "updatedAt" to Date(),
                        "platform" to "Android",
                        "appVersion" to "1.0.0"
                    ),
                    SetOptions.merge()
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to upload FCM token to Firestore: ${e.message}")
            }
        }
    }

    fun triggerLocalChatAlert(
        context: Context,
        senderName: String,
        messageText: String,
        channelTitle: String = "Company Chat",
        channelId: String = "company_chat"
    ) {
        NotificationHelper.showChatAlert(context, senderName, messageText, channelTitle, channelId)
    }

    fun triggerLocalTaskAlert(
        context: Context,
        title: String,
        messageText: String,
        taskId: Long = 0L
    ) {
        NotificationHelper.showTaskAlert(context, title, messageText, taskId)
    }

    private fun registerNetworkCallback(context: Context) {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (connectivityManager != null) {
                val activeNetwork = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                isRealNetworkConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

                val builder = NetworkRequest.Builder()
                connectivityManager.registerNetworkCallback(builder.build(), object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        isRealNetworkConnected = true
                        handleConnectivityChanged()
                    }

                    override fun onLost(network: Network) {
                        isRealNetworkConnected = false
                        handleConnectivityChanged()
                    }
                })
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network callback setup skipped: ${e.message}")
        }
    }

    private fun isEffectiveOnline(): Boolean {
        return isRealNetworkConnected && !isSimulatedOffline
    }

    private fun handleConnectivityChanged() {
        val online = isEffectiveOnline()
        if (online) {
            val totalPending = pendingLeadsQueue.size + pendingTasksQueue.size + pendingAttendanceQueue.size
            if (totalPending > 0) {
                appScope?.launch(Dispatchers.IO) {
                    processPendingSyncQueue()
                }
            } else {
                updateSyncState(
                    status = NetworkSyncStatus.SYNCED,
                    isOnline = true,
                    statusMessage = "Firebase Realtime: Live & Synced"
                )
            }
        } else {
            val totalPending = pendingLeadsQueue.size + pendingTasksQueue.size + pendingAttendanceQueue.size
            val msg = if (totalPending > 0) "Offline: $totalPending updates queued" else "Offline Mode"
            updateSyncState(
                status = NetworkSyncStatus.OFFLINE,
                isOnline = false,
                statusMessage = msg
            )
        }
    }

    fun toggleSimulatedOffline(forceOffline: Boolean) {
        isSimulatedOffline = forceOffline
        handleConnectivityChanged()
    }

    private fun updateSyncState(
        status: NetworkSyncStatus? = null,
        isOnline: Boolean? = null,
        isSyncing: Boolean? = null,
        statusMessage: String? = null,
        newTimestamp: Long? = null
    ) {
        val current = _syncState.value
        val summary = SyncQueueSummary(
            pendingLeads = pendingLeadsQueue.size,
            pendingTasks = pendingTasksQueue.size,
            pendingAttendance = pendingAttendanceQueue.size
        )
        _syncState.value = current.copy(
            status = status ?: current.status,
            isOnline = isOnline ?: current.isOnline,
            isSyncing = isSyncing ?: current.isSyncing,
            isSimulatedOffline = isSimulatedOffline,
            pendingSummary = summary,
            lastSyncTimestamp = newTimestamp ?: current.lastSyncTimestamp,
            statusMessage = statusMessage ?: current.statusMessage
        )
    }

    // ==========================================
    // SYNC OPERATIONS (Leads, Tasks, Attendance)
    // ==========================================

    fun syncLeadToFirebase(lead: LeadEntity) {
        if (!isEffectiveOnline()) {
            pendingLeadsQueue.removeIf { it.id == lead.id }
            pendingLeadsQueue.add(lead)
            updateSyncState(
                status = NetworkSyncStatus.OFFLINE,
                isOnline = false,
                statusMessage = "Offline: ${pendingLeadsQueue.size + pendingTasksQueue.size + pendingAttendanceQueue.size} pending"
            )
            return
        }

        updateSyncState(
            status = NetworkSyncStatus.SYNCING,
            isSyncing = true,
            statusMessage = "Syncing lead '${lead.name}'..."
        )

        try {
            val docId = if (lead.id > 0L) "lead_${lead.id}" else "lead_${System.currentTimeMillis()}"
            val data = hashMapOf(
                "id" to lead.id,
                "customerName" to lead.name,
                "company" to lead.company,
                "phone" to lead.phone,
                "email" to lead.email,
                "leadScore" to lead.leadScore,
                "requirement" to lead.requirement,
                "value" to (lead.potentialValue.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0),
                "status" to lead.stage,
                "assignedTo" to lead.assignedTo,
                "nextFollowUp" to lead.nextFollowUp,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore?.collection("leads")?.document(docId)
                ?.set(data, SetOptions.merge())
                ?.addOnSuccessListener {
                    pendingLeadsQueue.remove(lead)
                    val now = System.currentTimeMillis()
                    updateSyncState(
                        status = NetworkSyncStatus.SYNCED,
                        isOnline = true,
                        isSyncing = false,
                        statusMessage = "Firebase Realtime: Live & Synced",
                        newTimestamp = now
                    )
                    Log.d(TAG, "Successfully synced lead $docId to Firebase")
                }
                ?.addOnFailureListener { e ->
                    Log.w(TAG, "Firebase lead sync error: ${e.message}")
                    pendingLeadsQueue.add(lead)
                    updateSyncState(
                        status = NetworkSyncStatus.ERROR,
                        isSyncing = false,
                        statusMessage = "Sync failed: Retrying soon"
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in syncLeadToFirebase: ${e.message}")
            pendingLeadsQueue.add(lead)
            updateSyncState(
                status = NetworkSyncStatus.ERROR,
                isSyncing = false,
                statusMessage = "Sync failed"
            )
        }
    }

    fun syncTaskToFirebase(task: TaskEntity) {
        if (!isEffectiveOnline()) {
            pendingTasksQueue.removeIf { it.id == task.id }
            pendingTasksQueue.add(task)
            updateSyncState(
                status = NetworkSyncStatus.OFFLINE,
                isOnline = false,
                statusMessage = "Offline: ${pendingLeadsQueue.size + pendingTasksQueue.size + pendingAttendanceQueue.size} pending"
            )
            return
        }

        updateSyncState(
            status = NetworkSyncStatus.SYNCING,
            isSyncing = true,
            statusMessage = "Syncing task '${task.title}'..."
        )

        try {
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
                "updatedAt" to System.currentTimeMillis()
            )
            firestore?.collection("tasks")?.document(docId)
                ?.set(data, SetOptions.merge())
                ?.addOnSuccessListener {
                    pendingTasksQueue.remove(task)
                    val now = System.currentTimeMillis()
                    updateSyncState(
                        status = NetworkSyncStatus.SYNCED,
                        isOnline = true,
                        isSyncing = false,
                        statusMessage = "Firebase Realtime: Live & Synced",
                        newTimestamp = now
                    )
                    Log.d(TAG, "Successfully synced task $docId to Firebase")
                }
                ?.addOnFailureListener { e ->
                    Log.w(TAG, "Firebase task sync error: ${e.message}")
                    pendingTasksQueue.add(task)
                    updateSyncState(
                        status = NetworkSyncStatus.ERROR,
                        isSyncing = false,
                        statusMessage = "Sync failed: Retrying soon"
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in syncTaskToFirebase: ${e.message}")
            pendingTasksQueue.add(task)
            updateSyncState(
                status = NetworkSyncStatus.ERROR,
                isSyncing = false,
                statusMessage = "Sync failed"
            )
        }
    }

    fun deleteTaskFromFirebase(taskId: Long) {
        if (!isEffectiveOnline()) return
        try {
            val docId = "task_$taskId"
            firestore?.collection("tasks")?.document(docId)?.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting task from Firebase: ${e.message}")
        }
    }

    fun syncAttendanceToFirebase(record: AttendanceRecord) {
        if (!isEffectiveOnline()) {
            pendingAttendanceQueue.removeIf { it.id == record.id }
            pendingAttendanceQueue.add(record)
            updateSyncState(
                status = NetworkSyncStatus.OFFLINE,
                isOnline = false,
                statusMessage = "Offline: ${pendingLeadsQueue.size + pendingTasksQueue.size + pendingAttendanceQueue.size} pending"
            )
            return
        }

        updateSyncState(
            status = NetworkSyncStatus.SYNCING,
            isSyncing = true,
            statusMessage = "Syncing attendance..."
        )

        try {
            val docId = if (record.id > 0L) "att_${record.id}" else "att_${record.timestamp}"
            val data = hashMapOf(
                "id" to record.id,
                "date" to record.date,
                "checkInTime" to record.checkInTime,
                "checkOutTime" to record.checkOutTime,
                "durationMinutes" to record.durationMinutes,
                "isWorking" to record.isWorking,
                "status" to record.status,
                "overtimeMinutes" to record.overtimeMinutes,
                "timestamp" to record.timestamp,
                "employeeName" to record.employeeName,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore?.collection("attendance_records")?.document(docId)
                ?.set(data, SetOptions.merge())
                ?.addOnSuccessListener {
                    pendingAttendanceQueue.remove(record)
                    val now = System.currentTimeMillis()
                    updateSyncState(
                        status = NetworkSyncStatus.SYNCED,
                        isOnline = true,
                        isSyncing = false,
                        statusMessage = "Firebase Realtime: Live & Synced",
                        newTimestamp = now
                    )
                    Log.d(TAG, "Successfully synced attendance $docId to Firebase")
                }
                ?.addOnFailureListener { e ->
                    Log.w(TAG, "Firebase attendance sync error: ${e.message}")
                    pendingAttendanceQueue.add(record)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in syncAttendanceToFirebase: ${e.message}")
            pendingAttendanceQueue.add(record)
        }
    }

    fun syncProfileToFirebase(profile: UserProfileEntity) {
        try {
            val docId = "user_${profile.id}"
            val data = hashMapOf(
                "id" to profile.id,
                "name" to profile.name,
                "role" to profile.role,
                "isOnboarded" to profile.isOnboarded,
                "updatedAt" to profile.updatedAt
            )
            firestore?.collection("user_profiles")?.document(docId)
                ?.set(data, SetOptions.merge())
                ?.addOnSuccessListener {
                    Log.d(TAG, "Successfully synced profile $docId to Firebase")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in syncProfileToFirebase: ${e.message}")
        }
    }

    fun syncNow(scope: CoroutineScope? = appScope) {
        val targetScope = scope ?: CoroutineScope(Dispatchers.IO)
        targetScope.launch(Dispatchers.IO) {
            if (!isEffectiveOnline()) {
                updateSyncState(
                    status = NetworkSyncStatus.OFFLINE,
                    isOnline = false,
                    statusMessage = "Cannot sync while Offline"
                )
                return@launch
            }
            processPendingSyncQueue()
        }
    }

    private suspend fun processPendingSyncQueue() {
        val count = pendingLeadsQueue.size + pendingTasksQueue.size + pendingAttendanceQueue.size
        updateSyncState(
            status = NetworkSyncStatus.SYNCING,
            isSyncing = true,
            statusMessage = if (count > 0) "Syncing $count pending updates..." else "Syncing with Firestore..."
        )

        // Process leads
        val leadsToSync = pendingLeadsQueue.toList()
        for (lead in leadsToSync) {
            try {
                val docId = if (lead.id > 0L) "lead_${lead.id}" else "lead_${System.currentTimeMillis()}"
                val data = hashMapOf(
                    "id" to lead.id,
                    "customerName" to lead.name,
                    "company" to lead.company,
                    "phone" to lead.phone,
                    "email" to lead.email,
                    "leadScore" to lead.leadScore,
                    "requirement" to lead.requirement,
                    "value" to (lead.potentialValue.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0),
                    "status" to lead.stage,
                    "assignedTo" to lead.assignedTo,
                    "nextFollowUp" to lead.nextFollowUp,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestore?.collection("leads")?.document(docId)?.set(data, SetOptions.merge())
                pendingLeadsQueue.remove(lead)
            } catch (e: Exception) {
                Log.w(TAG, "Failed syncing queued lead: ${e.message}")
            }
        }

        // Process tasks
        val tasksToSync = pendingTasksQueue.toList()
        for (task in tasksToSync) {
            try {
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
                    "updatedAt" to System.currentTimeMillis()
                )
                firestore?.collection("tasks")?.document(docId)?.set(data, SetOptions.merge())
                pendingTasksQueue.remove(task)
            } catch (e: Exception) {
                Log.w(TAG, "Failed syncing queued task: ${e.message}")
            }
        }

        // Process attendance
        val attToSync = pendingAttendanceQueue.toList()
        for (att in attToSync) {
            try {
                val docId = if (att.id > 0L) "att_${att.id}" else "att_${att.timestamp}"
                val data = hashMapOf(
                    "id" to att.id,
                    "date" to att.date,
                    "checkInTime" to att.checkInTime,
                    "checkOutTime" to att.checkOutTime,
                    "durationMinutes" to att.durationMinutes,
                    "isWorking" to att.isWorking,
                    "status" to att.status,
                    "overtimeMinutes" to att.overtimeMinutes,
                    "timestamp" to att.timestamp,
                    "employeeName" to att.employeeName,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestore?.collection("attendance_records")?.document(docId)?.set(data, SetOptions.merge())
                pendingAttendanceQueue.remove(att)
            } catch (e: Exception) {
                Log.w(TAG, "Failed syncing queued attendance: ${e.message}")
            }
        }

        delay(600) // Brief graceful visual confirmation
        val now = System.currentTimeMillis()
        updateSyncState(
            status = NetworkSyncStatus.SYNCED,
            isOnline = true,
            isSyncing = false,
            statusMessage = "Firebase Realtime: Live & Synced",
            newTimestamp = now
        )
    }

    // ==========================================
    // REALTIME SNAPSHOT LISTENERS
    // ==========================================

    private fun startRealtimeAttendanceListener(attendanceDao: AttendanceDao, scope: CoroutineScope) {
        try {
            attendanceListener?.remove()
            attendanceListener = firestore?.collection("attendance_records")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Realtime attendance listen error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        scope.launch(Dispatchers.IO) {
                            for (doc in snapshot.documents) {
                                val id = doc.getLong("id")
                                    ?: doc.id.replace("att_", "").toLongOrNull()
                                    ?: 0L
                                val date = doc.getString("date") ?: ""
                                val checkInTime = doc.getString("checkInTime") ?: ""
                                val checkOutTime = doc.getString("checkOutTime")
                                val durationMinutes = doc.getLong("durationMinutes") ?: 0L
                                val isWorking = doc.getBoolean("isWorking") ?: false
                                val status = doc.getString("status") ?: "Present"
                                val overtimeMinutes = doc.getLong("overtimeMinutes") ?: 0L
                                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                val employeeName = doc.getString("employeeName") ?: "Rahul Sharma"

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
                                    attendanceDao.insert(record)
                                }
                            }
                            updateSyncState(
                                status = NetworkSyncStatus.SYNCED,
                                isOnline = true,
                                newTimestamp = System.currentTimeMillis(),
                                statusMessage = "Firebase Realtime: Synced (${snapshot.size()} live entries)"
                            )
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start attendance listener: ${e.message}")
        }
    }

    private fun startRealtimeTaskListener(taskDao: TaskDao, scope: CoroutineScope) {
        try {
            taskListener?.remove()
            taskListener = firestore?.collection("tasks")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Realtime task listen error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        scope.launch(Dispatchers.IO) {
                            for (doc in snapshot.documents) {
                                val id = doc.getLong("id")
                                    ?: doc.id.replace("task_", "").toLongOrNull()
                                    ?: 0L
                                val title = doc.getString("title") ?: ""
                                val projectName = doc.getString("projectName") ?: ""
                                val priority = doc.getString("priority") ?: "Medium"
                                val dueDate = doc.getString("dueDate") ?: ""
                                val status = doc.getString("status") ?: "In Progress"
                                val isCompleted = doc.getBoolean("isCompleted") ?: false
                                val category = doc.getString("category") ?: "Work"
                                val estimatedTimeNeeded = doc.getString("estimatedTimeNeeded") ?: "4 Hours"
                                val assignee = doc.getString("assignee") ?: "Rahul Sharma"

                                if (id > 0L && title.isNotBlank()) {
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
                                    taskDao.insert(task)
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start task listener: ${e.message}")
        }
    }

    private fun startRealtimeProfileListener(userProfileDao: UserProfileDao, scope: CoroutineScope) {
        try {
            profileListener?.remove()
            profileListener = firestore?.collection("user_profiles")?.document("user_1")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) {
                        return@addSnapshotListener
                    }
                    scope.launch(Dispatchers.IO) {
                        val name = snapshot.getString("name") ?: return@launch
                        val role = snapshot.getString("role") ?: return@launch
                        val isOnboarded = snapshot.getBoolean("isOnboarded") ?: true
                        val updatedAt = snapshot.getLong("updatedAt") ?: System.currentTimeMillis()

                        val profile = UserProfileEntity(
                            id = 1L,
                            name = name,
                            role = role,
                            isOnboarded = isOnboarded,
                            updatedAt = updatedAt
                        )
                        userProfileDao.insertOrUpdateProfile(profile)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start profile listener: ${e.message}")
        }
    }
}
