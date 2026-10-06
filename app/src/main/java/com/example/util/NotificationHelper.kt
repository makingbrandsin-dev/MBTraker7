package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {

    /**
     * Resolves the proper custom notification icon resource ID dynamically based on the category.
     * Prevents using generic defaults and ensures clear visual cues for each notification type.
     */
    fun resolveIconForCategory(category: String): Int {
        return when (category.lowercase().trim()) {
            "task", "sprint" -> com.example.R.drawable.ic_notif_task
            "lead", "crm", "followup", "client" -> com.example.R.drawable.ic_notif_lead
            "attendance", "clock", "punch" -> com.example.R.drawable.ic_notif_attendance
            "chat", "message", "team_chat" -> com.example.R.drawable.ic_notif_chat
            "leave", "holiday" -> com.example.R.drawable.ic_notif_leave
            "call", "telephony" -> com.example.R.drawable.ic_notif_call
            "changes", "admin", "system", "policy" -> com.example.R.drawable.ic_notif_changes
            else -> com.example.R.drawable.ic_notif_changes
        }
    }

    const val CHANNEL_TEAM_CHAT = "team_chat_channel"
    const val CHANNEL_TASK_UPDATES = "task_updates_channel"
    const val CHANNEL_LEAD_ALERTS = "lead_alerts_channel"
    const val CHANNEL_AUTH_ALERTS = "auth_security_channel"
    const val CHANNEL_BROADCAST = "admin_broadcast_channel"
    const val CHANNEL_CALL_LOGS = "call_logs_channel"
    const val CHANNEL_ATTENDANCE = "attendance_clock_channel"
    const val CHANNEL_CHANGES = "admin_changes_channel"
    const val CHANNEL_LEAVES = "leaves_channel"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // 0. Admin Broadcast & Push Announcements Channel
            val broadcastChannel = NotificationChannel(
                CHANNEL_BROADCAST,
                "Admin Push Announcements",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Company-wide announcements, admin broadcast alerts, and urgent notices"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            // 1. Team Chat & Mentions Channel
            val chatChannel = NotificationChannel(
                CHANNEL_TEAM_CHAT,
                "Team Chat & Mentions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming team messages, mentions, and urgent project discussions"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            // 2. Task & Sprint Updates Channel
            val taskChannel = NotificationChannel(
                CHANNEL_TASK_UPDATES,
                "Task & Sprint Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Task assignments, status changes, and sprint deadline alerts"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            // 3. Lead & CRM Alerts Channel
            val leadChannel = NotificationChannel(
                CHANNEL_LEAD_ALERTS,
                "Lead & CRM Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "New lead assignments, WhatsApp brochures, and scheduled follow-ups"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            // 4. Security & OTP Channel
            val authChannel = NotificationChannel(
                CHANNEL_AUTH_ALERTS,
                "Security & WhatsApp OTP",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "One-time passcodes and login security verification alerts"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            // 5. Call Logging & Telephony Channel
            val callChannel = NotificationChannel(
                CHANNEL_CALL_LOGS,
                "Call Logs & Recording Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Auto-logged incoming & outgoing phone calls and CRM client sync"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            // 6. Attendance & Punch Timers Channel
            val attendanceChannel = NotificationChannel(
                CHANNEL_ATTENDANCE,
                "Attendance & Clock In/Out",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Clock-in reminders, geofenced punch-in verification, and attendance regularization"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            // 7. Admin Changes & Policy Updates Channel
            val changesChannel = NotificationChannel(
                CHANNEL_CHANGES,
                "Admin Changes & Work Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time updates made by Admin affecting projects, assignments, and organization policies"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            // 8. Leave Applications & Approvals Channel
            val leavesChannel = NotificationChannel(
                CHANNEL_LEAVES,
                "Leave Requests & Approvals",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Leave applications submitted, status updates, and holiday calendar alerts"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(broadcastChannel)
            notificationManager.createNotificationChannel(chatChannel)
            notificationManager.createNotificationChannel(taskChannel)
            notificationManager.createNotificationChannel(leadChannel)
            notificationManager.createNotificationChannel(authChannel)
            notificationManager.createNotificationChannel(callChannel)
            notificationManager.createNotificationChannel(attendanceChannel)
            notificationManager.createNotificationChannel(changesChannel)
            notificationManager.createNotificationChannel(leavesChannel)
        }
    }

    fun showBroadcastAlert(
        context: Context,
        title: String,
        messageText: String,
        audience: String = "All Users",
        priority: String = "High",
        category: String = "changes"
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "notifications")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_BROADCAST)
            .setSmallIcon(resolveIconForCategory(category))
            .setContentTitle("📢 $title")
            .setContentText(messageText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("[$audience] $messageText"))
            .setAutoCancel(true)
            .setSound(defaultSound)
            .setPriority(if (priority.equals("urgent", ignoreCase = true) || priority.equals("critical", ignoreCase = true)) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = (System.currentTimeMillis() % 10000).toInt() + 5000
            notificationManager.notify(notificationId, notificationBuilder.build())
        } catch (_: SecurityException) {
        }
    }

    fun showChatAlert(
        context: Context,
        senderName: String,
        messageText: String,
        channelTitle: String = "Team Chat",
        channelId: String = "company_chat"
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "chat")
            putExtra("channelId", channelId)
            putExtra("channelTitle", channelTitle)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_TEAM_CHAT)
            .setSmallIcon(com.example.R.drawable.ic_notif_chat)
            .setContentTitle("💬 $senderName ($channelTitle)")
            .setContentText(messageText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageText))
            .setAutoCancel(true)
            .setSound(defaultSound)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = (System.currentTimeMillis() % 10000).toInt() + 1000
            notificationManager.notify(notificationId, notificationBuilder.build())
            AppSoundHelper.playChatNotificationSound(context)
        } catch (_: SecurityException) {
        }
    }

    fun showTaskAlert(
        context: Context,
        title: String,
        messageText: String,
        taskId: Long = 0L
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "tasks")
            putExtra("taskId", taskId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_TASK_UPDATES)
            .setSmallIcon(com.example.R.drawable.ic_notif_task)
            .setContentTitle("⚡ $title")
            .setContentText(messageText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageText))
            .setAutoCancel(true)
            .setSound(defaultSound)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = (System.currentTimeMillis() % 10000).toInt() + 2000
            notificationManager.notify(notificationId, notificationBuilder.build())
        } catch (_: SecurityException) {
        }
    }

    fun showLeadAlert(
        context: Context,
        leadName: String,
        company: String,
        requirement: String
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "crm")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_LEAD_ALERTS)
            .setSmallIcon(com.example.R.drawable.ic_notif_lead)
            .setContentTitle("🎯 New Lead: $leadName ($company)")
            .setContentText("Requirement: $requirement")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Requirement: $requirement\nAuto-brochure ready for dispatch."))
            .setAutoCancel(true)
            .setSound(defaultSound)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = (System.currentTimeMillis() % 10000).toInt() + 3000
            notificationManager.notify(notificationId, notificationBuilder.build())
        } catch (_: SecurityException) {
        }
    }

    fun showAttendanceAlert(
        context: Context,
        title: String,
        messageText: String
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "attendance")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ATTENDANCE)
            .setSmallIcon(com.example.R.drawable.ic_notif_attendance)
            .setContentTitle("⏱ $title")
            .setContentText(messageText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageText))
            .setAutoCancel(true)
            .setSound(defaultSound)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = (System.currentTimeMillis() % 10000).toInt() + 6000
            notificationManager.notify(notificationId, notificationBuilder.build())
        } catch (_: SecurityException) {
        }
    }

    fun showChangesAlert(
        context: Context,
        title: String,
        messageText: String,
        priority: String = "High"
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "notifications")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_CHANGES)
            .setSmallIcon(com.example.R.drawable.ic_notif_changes)
            .setContentTitle("🛠️ $title")
            .setContentText(messageText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageText))
            .setAutoCancel(true)
            .setSound(defaultSound)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = (System.currentTimeMillis() % 10000).toInt() + 7000
            notificationManager.notify(notificationId, notificationBuilder.build())
        } catch (_: SecurityException) {
        }
    }

    fun showLeaveAlert(
        context: Context,
        title: String,
        messageText: String
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "holidays_leaves")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_LEAVES)
            .setSmallIcon(com.example.R.drawable.ic_notif_leave)
            .setContentTitle("🌴 $title")
            .setContentText(messageText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageText))
            .setAutoCancel(true)
            .setSound(defaultSound)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = (System.currentTimeMillis() % 10000).toInt() + 8000
            notificationManager.notify(notificationId, notificationBuilder.build())
        } catch (_: SecurityException) {
        }
    }

    fun showOtpAlert(
        context: Context,
        otpCode: String,
        phone: String
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "otp")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_AUTH_ALERTS)
            .setSmallIcon(com.example.R.drawable.ic_notif_changes)
            .setContentTitle("🔐 MB Traker OTP: $otpCode")
            .setContentText("Your WhatsApp login code is $otpCode for $phone.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("One-Time Passcode for MB Traker sign-in is $otpCode. Valid for 10 minutes."))
            .setAutoCancel(true)
            .setSound(defaultSound)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = 9999
            notificationManager.notify(notificationId, notificationBuilder.build())
        } catch (_: SecurityException) {
        }
    }

    fun showCallAlert(
        context: Context,
        contactName: String,
        callType: String,
        durationText: String,
        phoneNumber: String
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "call_tracker")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val title = when (callType.lowercase()) {
            "missed" -> "🔴 Missed Call: $contactName"
            "incoming" -> "📲 Incoming Call Logged: $contactName"
            else -> "📞 Outgoing Call Logged: $contactName"
        }

        val subtitle = "$callType Call • $durationText • $phoneNumber"

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_CALL_LOGS)
            .setSmallIcon(com.example.R.drawable.ic_notif_call)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Contact: $contactName\nNumber: $phoneNumber\nType: $callType\nDuration: $durationText\nSaved to CRM Call Tracker."))
            .setAutoCancel(true)
            .setSound(defaultSound)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = (System.currentTimeMillis() % 10000).toInt() + 4000
            notificationManager.notify(notificationId, notificationBuilder.build())
        } catch (_: SecurityException) {
        }
    }

    fun showCategoryAlert(
        context: Context,
        title: String,
        messageText: String,
        category: String
    ) {
        when (category.lowercase()) {
            "task" -> showTaskAlert(context, title, messageText)
            "lead", "crm", "followup" -> showLeadAlert(context, title, "CRM Lead Update", messageText)
            "attendance", "clock" -> showAttendanceAlert(context, title, messageText)
            "chat", "message" -> showChatAlert(context, "Team Chat", messageText)
            "leave", "holiday" -> showLeaveAlert(context, title, messageText)
            "changes", "admin", "system" -> showChangesAlert(context, title, messageText)
            else -> showBroadcastAlert(context, title, messageText)
        }
    }

    fun showNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = 1001,
        category: String = "changes"
    ) {
        showBroadcastAlert(
            context = context,
            title = title,
            messageText = message,
            audience = "Milo Assistant",
            priority = "High",
            category = category
        )
    }

    /**
     * Programmatically clears active push notifications from the status bar for a given category.
     * Keeps notifications synchronized and prevents status bar clutter once user acts on them.
     */
    fun dismissNotificationsByCategory(context: Context, category: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNotifications = notificationManager.activeNotifications
                for (notif in activeNotifications) {
                    val channelId = notif.notification.channelId
                    val matches = when (category.lowercase().trim()) {
                        "task", "sprint" -> channelId == CHANNEL_TASK_UPDATES
                        "lead", "crm", "followup", "client" -> channelId == CHANNEL_LEAD_ALERTS
                        "chat", "message", "team_chat" -> channelId == CHANNEL_TEAM_CHAT
                        "attendance", "clock", "punch" -> channelId == CHANNEL_ATTENDANCE
                        "leave", "holiday" -> channelId == CHANNEL_LEAVES
                        "changes", "admin", "system" -> channelId == CHANNEL_CHANGES
                        "call" -> channelId == CHANNEL_CALL_LOGS
                        else -> false
                    }
                    if (matches) {
                        notificationManager.cancel(notif.id)
                    }
                }
            } else {
                // Pre-M compatibility: cancel category specific notification ID pools
                when (category.lowercase().trim()) {
                    "chat", "message", "team_chat" -> {
                        (1000..1999).forEach { notificationManager.cancel(it) }
                    }
                    "task", "sprint" -> {
                        (2000..2999).forEach { notificationManager.cancel(it) }
                    }
                    "lead", "crm", "followup", "client" -> {
                        (3000..3999).forEach { notificationManager.cancel(it) }
                    }
                    "attendance", "clock", "punch" -> {
                        (6000..6999).forEach { notificationManager.cancel(it) }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("NotificationHelper", "Failed to cancel category $category notifications: ${e.message}")
        }
    }

    /**
     * Scans active tasks for approaching deadlines (due today or tomorrow)
     * and triggers a local system-level alert.
     */
    fun checkApproachingTaskDeadlines(context: Context, tasks: List<com.example.data.model.TaskEntity>) {
        val sdf1 = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val sdf2 = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        
        val today = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        
        val uncompletedTasks = tasks.filter { !it.isCompleted }
        for (task in uncompletedTasks) {
            val parsedDate = try { sdf1.parse(task.dueDate) } catch (e: Exception) {
                try { sdf2.parse(task.dueDate) } catch (e: Exception) { null }
            }
            
            if (parsedDate != null) {
                val taskCal = java.util.Calendar.getInstance().apply { time = parsedDate }
                val diffMs = taskCal.timeInMillis - today.timeInMillis
                val diffDays = (diffMs / (24 * 60 * 60 * 1000)).toInt()
                
                // If due today (0 days) or due tomorrow (1 day)
                if (diffDays == 0 || diffDays == 1) {
                    val alertKey = "deadline_alert_fired_${task.id}"
                    val prefs = context.getSharedPreferences("deadline_alerts_prefs", Context.MODE_PRIVATE)
                    val alreadyFired = prefs.getBoolean(alertKey, false)
                    
                    if (!alreadyFired) {
                        val alertTitle = "⏰ Task Deadline Approaching!"
                        val alertMessage = when (diffDays) {
                            0 -> "Task '${task.title}' is due TODAY (${task.dueDate})."
                            else -> "Task '${task.title}' is due TOMORROW (${task.dueDate})."
                        }
                        
                        showTaskAlert(context, alertTitle, alertMessage, task.id)
                        AppSoundHelper.playCategorySound(context, "task")
                        
                        // Mark as fired
                        prefs.edit().putBoolean(alertKey, true).apply()
                    }
                }
            }
        }
    }
}
