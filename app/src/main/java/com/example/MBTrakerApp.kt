package com.example

import android.app.Application
import com.example.di.AppContainer
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Application class managing application-level singleton dependencies and DI container.
 */
class MBTrakerApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)

        // Initialize Firebase Realtime Manager active listeners immediately on application start
        val db = com.example.data.local.AppDatabase.getDatabase(this)
        val appScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
        com.example.data.firebase.FirebaseRealtimeManager.initialize(
            context = this,
            attendanceDao = db.attendanceDao(),
            userProfileDao = db.userProfileDao(),
            taskDao = db.taskDao(),
            leadDao = db.leadDao(),
            chatDao = db.chatDao(),
            activityFeedDao = db.activityFeedDao(),
            scope = appScope,
            notificationDao = db.notificationDao(),
            projectDao = db.projectDao(),
            leaveDao = db.leaveDao(),
            employeeDao = db.employeeDao()
        )
        
        // Schedule periodic Lead Intent monitoring with battery and network constraints
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<com.example.workers.LeadIntentWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        try {
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "LeadIntentWorker",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        } catch (e: Exception) {
            android.util.Log.w("MBTrakerApp", "WorkManager unavailable or skipped in current environment: ${e.message}")
        }

        // Schedule periodic high-security database sync when connection is restored
        val syncConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = PeriodicWorkRequestBuilder<com.example.workers.BackgroundSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(syncConstraints)
            .build()
        try {
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "BackgroundSyncWorker",
                ExistingPeriodicWorkPolicy.KEEP,
                syncWorkRequest
            )
        } catch (e: Exception) {
            android.util.Log.w("MBTrakerApp", "WorkManager background sync registration warning: ${e.message}")
        }
    }

    companion object {
        lateinit var instance: MBTrakerApp
            private set
    }
}
