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

    const val CHANNEL_TEAM_CHAT = "team_chat_channel"
    const val CHANNEL_TASK_UPDATES = "task_updates_channel"
    const val CHANNEL_LEAD_ALERTS = "lead_alerts_channel"
    const val CHANNEL_AUTH_ALERTS = "auth_security_channel"
    const val CHANNEL_BROADCAST = "admin_broadcast_channel"
    const val CHANNEL_CALL_LOGS = "call_logs_channel"

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

            notificationManager.createNotificationChannel(broadcastChannel)
            notificationManager.createNotificationChannel(chatChannel)
            notificationManager.createNotificationChannel(taskChannel)
            notificationManager.createNotificationChannel(leadChannel)
            notificationManager.createNotificationChannel(authChannel)
            notificationManager.createNotificationChannel(callChannel)
        }
    }

    fun showBroadcastAlert(
        context: Context,
        title: String,
        messageText: String,
        audience: String = "All Users",
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

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_BROADCAST)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
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
            .setSmallIcon(android.R.drawable.ic_dialog_email)
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
            .setSmallIcon(android.R.drawable.ic_dialog_info)
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
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
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
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
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
            .setSmallIcon(android.R.drawable.sym_call_incoming)
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

    fun showNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = 1001
    ) {
        showBroadcastAlert(
            context = context,
            title = title,
            messageText = message,
            audience = "Milo Assistant",
            priority = "High"
        )
    }
}
