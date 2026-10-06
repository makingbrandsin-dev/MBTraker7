package com.example.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.firebase.FirebaseRealtimeManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * High-Security Background Synchronization Worker using Android Jetpack WorkManager.
 * Automatically triggered by the OS as soon as network connectivity is restored.
 * Synchronizes all pending local SQLite Room database modifications (such as newly
 * created tasks or CRM leads) to the remote dual-instance Firebase Firestore database.
 */
class BackgroundSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("BackgroundSyncWorker", "Executing scheduled background database synchronization...")
        return try {
            if (FirebaseRealtimeManager.isEffectiveOnline()) {
                // Trigger full sync of cached database elements
                FirebaseRealtimeManager.syncNow(CoroutineScope(Dispatchers.IO))
                Log.d("BackgroundSyncWorker", "Background synchronization successfully dispatched.")
                Result.success()
            } else {
                Log.w("BackgroundSyncWorker", "Device is offline. Retrying background synchronization once connection is restored.")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("BackgroundSyncWorker", "Background synchronization failed with exception: ${e.message}", e)
            Result.retry()
        }
    }
}
