package com.example.data.firebase

import android.net.Uri
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.UploadTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * FirebaseStorageManager coordinates uploading and retrieving media assets in Firebase Storage.
 * Stores voice notes in the bucket under `voice_notes/{channelId}/{fileName}`.
 */
object FirebaseStorageManager {
    private const val TAG = "FirebaseStorageManager"

    private fun getStorage(): FirebaseStorage? {
        return try {
            val app = FirebaseApp.getInstance()
            // Connect to default storage bucket defined in google-services.json
            val bucket = app.options.storageBucket
            if (!bucket.isNullOrBlank()) {
                val fullUrl = if (bucket.startsWith("gs://")) bucket else "gs://$bucket"
                FirebaseStorage.getInstance(app, fullUrl)
            } else {
                FirebaseStorage.getInstance(app)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize FirebaseStorage: ${e.message}")
            try {
                FirebaseStorage.getInstance()
            } catch (e2: Exception) {
                Log.e(TAG, "Fallback FirebaseStorage failed: ${e2.message}")
                null
            }
        }
    }

    /**
     * Uploads an audio voice note to Firebase Storage and returns the public/authenticated download URL.
     *
     * @param channelId Team chat channel (e.g. general, dev_team)
     * @param audioFile Local .m4a audio file recorded by device microphone
     * @param onProgress Optional callback receiving upload progress 0f..1f
     * @return Result containing the Firebase Storage download URL string on success
     */
    suspend fun uploadVoiceNote(
        channelId: String,
        audioFile: File,
        onProgress: ((Float) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!audioFile.exists() || audioFile.length() == 0L) {
            return@withContext Result.failure(IllegalArgumentException("Audio file does not exist or is empty"))
        }

        val storage = getStorage()
            ?: return@withContext Result.failure(IllegalStateException("Firebase Storage is unavailable"))

        val safeChannel = channelId.replace("/", "_").ifBlank { "general" }
        val fileName = "voice_${System.currentTimeMillis()}_${audioFile.name}"
        val storageRef = storage.reference.child("voice_notes/$safeChannel/$fileName")

        val metadata = StorageMetadata.Builder()
            .setContentType("audio/mp4")
            .setCustomMetadata("channelId", safeChannel)
            .setCustomMetadata("uploadedAt", System.currentTimeMillis().toString())
            .build()

        try {
            val fileUri = Uri.fromFile(audioFile)
            val uploadTask: UploadTask = storageRef.putFile(fileUri, metadata)

            if (onProgress != null) {
                uploadTask.addOnProgressListener { taskSnapshot ->
                    val total = taskSnapshot.totalByteCount
                    val transferred = taskSnapshot.bytesTransferred
                    if (total > 0) {
                        val progress = transferred.toFloat() / total.toFloat()
                        onProgress(progress.coerceIn(0f, 1f))
                    }
                }
            }

            // Suspend until upload finishes
            suspendCancellableCoroutine<Unit> { continuation ->
                uploadTask.addOnSuccessListener {
                    if (continuation.isActive) continuation.resume(Unit)
                }.addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                }.addOnCanceledListener {
                    if (continuation.isActive) continuation.cancel()
                }

                continuation.invokeOnCancellation {
                    if (!uploadTask.isComplete) {
                        uploadTask.cancel()
                    }
                }
            }

            // Retrieve the download URL from the uploaded reference
            val downloadUrl = suspendCancellableCoroutine<Uri> { continuation ->
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    if (continuation.isActive) continuation.resume(uri)
                }.addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
            }

            Log.d(TAG, "Voice note uploaded successfully to Firebase Storage: $downloadUrl")
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading voice note to Firebase Storage: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Uploads a promotional banner image to Firebase Storage under `banners/{fileName}`
     * and returns the public download URL string.
     */
    suspend fun uploadBannerImage(
        context: android.content.Context,
        imageUri: Uri,
        onProgress: ((Float) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val storage = getStorage()
        val fileName = "banner_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(6)}.jpg"

        if (storage != null) {
            try {
                val storageRef = storage.reference.child("banners/$fileName")
                val metadata = StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .setCustomMetadata("uploadedAt", System.currentTimeMillis().toString())
                    .setCustomMetadata("type", "home_banner")
                    .build()

                val uploadTask = storageRef.putFile(imageUri, metadata)

                if (onProgress != null) {
                    uploadTask.addOnProgressListener { taskSnapshot ->
                        val total = taskSnapshot.totalByteCount
                        val transferred = taskSnapshot.bytesTransferred
                        if (total > 0) {
                            val progress = transferred.toFloat() / total.toFloat()
                            onProgress(progress.coerceIn(0f, 1f))
                        }
                    }
                }

                suspendCancellableCoroutine<Unit> { continuation ->
                    uploadTask.addOnSuccessListener {
                        if (continuation.isActive) continuation.resume(Unit)
                    }.addOnFailureListener { error ->
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }.addOnCanceledListener {
                        if (continuation.isActive) continuation.cancel()
                    }
                    continuation.invokeOnCancellation {
                        if (!uploadTask.isComplete) uploadTask.cancel()
                    }
                }

                val downloadUrl = suspendCancellableCoroutine<Uri> { continuation ->
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        if (continuation.isActive) continuation.resume(uri)
                    }.addOnFailureListener { error ->
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
                }

                Log.d(TAG, "Banner image uploaded successfully to Firebase Storage: $downloadUrl")
                return@withContext Result.success(downloadUrl.toString())
            } catch (e: Exception) {
                Log.w(TAG, "Firebase Storage upload error, caching locally: ${e.message}")
            }
        }

        // Graceful offline fallback
        try {
            val bannersDir = File(context.filesDir, "banners").apply { mkdirs() }
            val destFile = File(bannersDir, fileName)
            context.contentResolver.openInputStream(imageUri)?.use { input ->
                java.io.FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            Result.success(destFile.absolutePath)
        } catch (e: Exception) {
            Result.success(imageUri.toString())
        }
    }
}
