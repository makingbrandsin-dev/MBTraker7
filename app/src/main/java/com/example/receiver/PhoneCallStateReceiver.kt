package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.CallLogEntity
import com.example.data.model.NotificationEntity
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * BroadcastReceiver that monitors incoming, outgoing, and missed telephony call states.
 * Automatically resolves contact names from CRM Leads and Organization Employees,
 * persists call details into the Room Call Log database, and fires system alerts.
 */
class PhoneCallStateReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        try {
            val action = intent.action ?: return

            when (action) {
                Intent.ACTION_NEW_OUTGOING_CALL -> {
                    val outgoingNumber = try {
                        intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
                            ?: intent.getStringExtra("android.intent.extra.PHONE_NUMBER")
                            ?: ""
                    } catch (se: SecurityException) {
                        Log.w(TAG, "SecurityException reading outgoing number: ${se.message}")
                        ""
                    }
                    handleOutgoingCallInitiated(outgoingNumber)
                }
                TelephonyManager.ACTION_PHONE_STATE_CHANGED,
                "android.intent.action.PHONE_STATE" -> {
                    val stateStr = try {
                        intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                    } catch (se: SecurityException) {
                        Log.w(TAG, "SecurityException reading phone state: ${se.message}")
                        null
                    } ?: return

                    val incomingNumber = try {
                        intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                    } catch (se: SecurityException) {
                        Log.w(TAG, "SecurityException reading incoming number: ${se.message}")
                        null
                    }
                    handlePhoneStateChanged(context, stateStr, incomingNumber)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unhandled error in PhoneCallStateReceiver: ${e.message}", e)
        }
    }

    private fun handleOutgoingCallInitiated(number: String) {
        if (number.isNotBlank()) {
            savedPhoneNumber = sanitizePhoneNumber(number)
        }
        isIncomingCall = false
        isCallAnswered = false
        callStartTimestamp = System.currentTimeMillis()
        currentCallState = TelephonyManager.CALL_STATE_OFFHOOK
        Log.d(TAG, "Outgoing call initiated to: $savedPhoneNumber")
    }

    private fun handlePhoneStateChanged(context: Context, stateStr: String, rawNumber: String?) {
        val number = if (!rawNumber.isNullOrBlank()) sanitizePhoneNumber(rawNumber) else savedPhoneNumber

        when (stateStr) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                currentCallState = TelephonyManager.CALL_STATE_RINGING
                isIncomingCall = true
                isCallAnswered = false
                callStartTimestamp = System.currentTimeMillis()
                if (!number.isNullOrBlank()) {
                    savedPhoneNumber = number
                }
                Log.d(TAG, "Incoming call ringing from: $savedPhoneNumber")
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                val previousState = currentCallState
                currentCallState = TelephonyManager.CALL_STATE_OFFHOOK
                isCallAnswered = true
                callStartTimestamp = System.currentTimeMillis()

                if (previousState == TelephonyManager.CALL_STATE_RINGING) {
                    isIncomingCall = true
                    Log.d(TAG, "Incoming call answered from: $savedPhoneNumber")
                } else {
                    if (!isIncomingCall) {
                        Log.d(TAG, "Outgoing call connected with: $savedPhoneNumber")
                    }
                }
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                val previousState = currentCallState
                currentCallState = TelephonyManager.CALL_STATE_IDLE

                val phoneToLog = savedPhoneNumber?.ifBlank { null } ?: number?.ifBlank { null } ?: "+91 98765 43210"

                if (previousState == TelephonyManager.CALL_STATE_RINGING && !isCallAnswered) {
                    // Missed / Rejected Call
                    val durationText = "0s"
                    val callType = "Missed"
                    val callStatus = "Missed"
                    Log.d(TAG, "Call missed from: $phoneToLog")
                    recordCallLogToDatabase(context, phoneToLog, callType, callStatus, durationText, 0L)
                } else if (isCallAnswered) {
                    // Connected & Answered Call Completed
                    val durationMs = (System.currentTimeMillis() - callStartTimestamp).coerceAtLeast(1000L)
                    val durationSec = durationMs / 1000L
                    val durationText = formatDuration(durationSec)
                    val callType = if (isIncomingCall) "Incoming" else "Outgoing"
                    val callStatus = "Connected"
                    Log.d(TAG, "Call ended: $callType ($durationText) with: $phoneToLog")
                    recordCallLogToDatabase(context, phoneToLog, callType, callStatus, durationText, durationSec)
                } else if (!isIncomingCall && previousState == TelephonyManager.CALL_STATE_OFFHOOK) {
                    // Outgoing call ended without connection or cancelled
                    val durationText = "0s"
                    val callType = "Outgoing"
                    val callStatus = "No Answer"
                    Log.d(TAG, "Outgoing call ended (No answer) with: $phoneToLog")
                    recordCallLogToDatabase(context, phoneToLog, callType, callStatus, durationText, 0L)
                }

                // Reset state variables
                isIncomingCall = false
                isCallAnswered = false
                callStartTimestamp = 0L
                savedPhoneNumber = null
            }
        }
    }

    private fun recordCallLogToDatabase(
        context: Context,
        phoneNumber: String,
        callType: String,
        callStatus: String,
        durationText: String,
        durationSeconds: Long
    ) {
        val appContext = context.applicationContext
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(appContext)

                // 1. Resolve contact name from Leads or Employees
                val resolvedName = resolveContactName(db, phoneNumber)

                // 2. Build CallLogEntity
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val timestampText = "Today, " + timeFormat.format(Date())

                val callLog = CallLogEntity(
                    contactName = resolvedName,
                    callType = callType,
                    status = callStatus,
                    timestampText = timestampText,
                    durationText = durationText,
                    phoneNumber = phoneNumber
                )

                // 3. Insert into Room Database
                val insertedId = db.callLogDao().insert(callLog)
                Log.d(TAG, "Call Log inserted in database with ID: $insertedId for $resolvedName")

                // 4. Record Notification Alert
                val notifTitle = when (callType.lowercase()) {
                    "missed" -> "🔴 Missed Call: $resolvedName"
                    "incoming" -> "📲 Incoming Call Logged: $resolvedName"
                    else -> "📞 Outgoing Call Logged: $resolvedName"
                }

                val notifSubtitle = "$callType Call ($durationText) • $phoneNumber • Status: $callStatus"

                db.notificationDao().insert(
                    NotificationEntity(
                        title = notifTitle,
                        subtitle = notifSubtitle,
                        timeAgo = "Just now",
                        category = "followup",
                        isRead = false
                    )
                )

                // 5. Fire system notification alert
                NotificationHelper.showCallAlert(
                    context = appContext,
                    contactName = resolvedName,
                    callType = callType,
                    durationText = durationText,
                    phoneNumber = phoneNumber
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error recording call log: ${e.message}", e)
            }
        }
    }

    private suspend fun resolveContactName(db: AppDatabase, phoneNumber: String): String {
        val cleanTarget = cleanDigits(phoneNumber)

        // Check in CRM Leads
        try {
            val leads = db.leadDao().getAllLeadsDirectly()
            for (lead in leads) {
                if (cleanDigits(lead.phone) == cleanTarget || (cleanTarget.length >= 7 && cleanDigits(lead.phone).endsWith(cleanTarget))) {
                    return "${lead.name} (${lead.company.ifBlank { "Client" }})"
                }
            }
        } catch (_: Exception) {}

        // Check in Organization Employees
        try {
            val employees = db.employeeDao().getAllEmployeesDirectly()
            for (emp in employees) {
                if (cleanDigits(emp.phone) == cleanTarget || (cleanTarget.length >= 7 && cleanDigits(emp.phone).endsWith(cleanTarget))) {
                    return "${emp.name} (${emp.designation})"
                }
            }
        } catch (_: Exception) {}

        // Fallback formatted identifier
        return if (phoneNumber.isNotBlank()) {
            "Client ($phoneNumber)"
        } else {
            "Unknown Caller"
        }
    }

    private fun formatDuration(seconds: Long): String {
        return if (seconds <= 0L) {
            "0s"
        } else if (seconds < 60L) {
            "${seconds}s"
        } else {
            val mins = seconds / 60
            val secs = seconds % 60
            String.format(Locale.getDefault(), "%02dm %02ds", mins, secs)
        }
    }

    private fun sanitizePhoneNumber(raw: String): String {
        return raw.trim()
    }

    private fun cleanDigits(phone: String): String {
        return phone.filter { it.isDigit() }.takeLast(10)
    }

    companion object {
        private const val TAG = "PhoneCallStateReceiver"

        @Volatile
        private var currentCallState: Int = TelephonyManager.CALL_STATE_IDLE

        @Volatile
        private var callStartTimestamp: Long = 0L

        @Volatile
        private var isIncomingCall: Boolean = false

        @Volatile
        private var isCallAnswered: Boolean = false

        @Volatile
        private var savedPhoneNumber: String? = null

        /**
         * Test trigger to simulate a telephony event for automated verification or UI preview.
         */
        fun simulateTestCallEvent(
            context: Context,
            contactName: String,
            phoneNumber: String,
            callType: String = "Incoming",
            durationSeconds: Long = 125
        ) {
            val receiver = PhoneCallStateReceiver()
            val mins = durationSeconds / 60
            val secs = durationSeconds % 60
            val durText = if (durationSeconds > 0) String.format(Locale.getDefault(), "%02dm %02ds", mins, secs) else "0s"
            val status = if (callType.equals("Missed", ignoreCase = true)) "Missed" else "Connected"
            receiver.recordCallLogToDatabase(context, phoneNumber, callType, status, durText, durationSeconds)
        }
    }
}
