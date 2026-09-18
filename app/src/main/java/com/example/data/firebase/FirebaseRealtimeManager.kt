package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.local.AttendanceDao
import com.example.data.local.UserProfileDao
import com.example.data.model.AttendanceRecord
import com.example.data.model.LeadEntity
import com.example.data.model.UserProfileEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object FirebaseRealtimeManager {
    private const val TAG = "FirebaseRealtimeManager"
    private var firestore: FirebaseFirestore? = null
    private var attendanceListener: ListenerRegistration? = null
    private var isInitialized = false

    private val _isRealtimeConnected = MutableStateFlow(false)
    val isRealtimeConnected: StateFlow<Boolean> = _isRealtimeConnected.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val _syncStatus = MutableStateFlow("Firebase Realtime: Ready")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    fun initialize(
        context: Context,
        attendanceDao: AttendanceDao,
        userProfileDao: UserProfileDao,
        scope: CoroutineScope
    ) {
        if (isInitialized && firestore != null) return

        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:371778881998:android:com_aistudio_mbtraker_hqxk")
                    .setProjectId("mbtraker-hrms-realtime")
                    .setApiKey("AIzaSyRealtimeEvaluationKeyForMBTracker2026")
                    .build()
                FirebaseApp.initializeApp(context, options)
                Log.d(TAG, "Initialized default FirebaseApp with fallback options")
            }

            firestore = FirebaseFirestore.getInstance()
            isInitialized = true
            _isRealtimeConnected.value = true
            _syncStatus.value = "Firebase Realtime: Connected"

            // Attach real-time snapshot listener
            startRealtimeAttendanceListener(attendanceDao, scope)
            startRealtimeProfileListener(userProfileDao, scope)
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization error: ${e.message}", e)
            _isRealtimeConnected.value = false
            _syncStatus.value = "Firebase Realtime: Standby (${e.localizedMessage ?: "Offline"})"
        }
    }

    private fun startRealtimeAttendanceListener(attendanceDao: AttendanceDao, scope: CoroutineScope) {
        try {
            attendanceListener?.remove()
            attendanceListener = firestore?.collection("attendance_records")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Realtime attendance listen error: ${error.message}")
                        _syncStatus.value = "Firebase Realtime: Retrying..."
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
                            _lastSyncTimestamp.value = System.currentTimeMillis()
                            _isRealtimeConnected.value = true
                            _syncStatus.value = "Firebase Realtime: Synced (${snapshot.size()} live entries)"
                        }
                    } else {
                        _isRealtimeConnected.value = true
                        _syncStatus.value = "Firebase Realtime: Live & Synced"
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start attendance listener: ${e.message}")
        }
    }

    private fun startRealtimeProfileListener(userProfileDao: UserProfileDao, scope: CoroutineScope) {
        try {
            firestore?.collection("user_profiles")?.document("user_1")
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

    fun syncAttendanceToFirebase(record: AttendanceRecord) {
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
                    _lastSyncTimestamp.value = System.currentTimeMillis()
                    _isRealtimeConnected.value = true
                    _syncStatus.value = "Firebase Realtime: Live & Synced"
                    Log.d(TAG, "Successfully synced attendance $docId to Firebase")
                }
                ?.addOnFailureListener { e ->
                    Log.w(TAG, "Firebase attendance sync error: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in syncAttendanceToFirebase: ${e.message}")
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

    fun syncLeadToFirebase(lead: LeadEntity) {
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
                    _lastSyncTimestamp.value = System.currentTimeMillis()
                    _isRealtimeConnected.value = true
                    _syncStatus.value = "Firebase Realtime: Live & Synced"
                    Log.d(TAG, "Successfully synced lead $docId to Firebase")
                }
                ?.addOnFailureListener { e ->
                    Log.w(TAG, "Firebase lead sync error: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in syncLeadToFirebase: ${e.message}")
        }
    }
}
