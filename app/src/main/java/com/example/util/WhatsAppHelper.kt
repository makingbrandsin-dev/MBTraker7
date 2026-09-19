package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.AutoBrochureConfigEntity
import java.net.URLEncoder

object WhatsAppHelper {

    /**
     * Sanitizes raw phone numbers for WhatsApp API.
     * Removes spaces, dashes, parentheses, plus signs.
     * If 10 digits (standard Indian mobile format without country code), prepends 91.
     */
    fun sanitizePhoneNumber(rawPhone: String): String {
        val digitsOnly = rawPhone.replace(Regex("[^0-9]"), "")
        return when {
            digitsOnly.length == 10 -> "91$digitsOnly"
            digitsOnly.startsWith("0") && digitsOnly.length == 11 -> "91" + digitsOnly.substring(1)
            else -> digitsOnly
        }
    }

    /**
     * Builds the personalized Company Profile & Portfolio WhatsApp message.
     */
    fun buildCompanyProfileMessage(
        recipientName: String,
        companyName: String = "",
        brochureConfig: AutoBrochureConfigEntity? = null
    ): String {
        val fileName = brochureConfig?.brochureFileName ?: "MakingBrands_Company_Profile_2026.pdf"
        val fileSize = brochureConfig?.brochureFileSize ?: "3.4 MB"
        val customMsg = brochureConfig?.customMessage?.replace("{NAME}", recipientName.trim()) 
            ?: "Hello ${recipientName.trim()}, thank you for connecting with Making Brands!"

        val companySuffix = if (companyName.isNotBlank() && companyName != "Independent") " (${companyName.trim()})" else ""

        return """
👋 $customMsg$companySuffix

We are delighted to share our official *Company Profile & Solutions Portfolio*:
📄 *File:* $fileName ($fileSize)
🌐 *View & Download PDF Brochure:* https://makingbrands.in/brochure/MakingBrands_Company_Profile.pdf

🚀 *What We Do at Making Brands:*
• Custom Web & Mobile Application Development
• Enterprise CRM, ERP & Workflow Automation
• Digital Marketing, SEO & High-Converting Lead Generation
• UI/UX Strategy & Brand Identity Design

Feel free to reply directly to this chat or call us if you would like to schedule a free project consultation!

Best regards,
*Making Brands Team*
🌐 makingbrands.in | 📞 +91 98765 43210
""".trimIndent()
    }

    /**
     * Opens WhatsApp or browser to send the message directly to the target recipient.
     */
    fun sendWhatsAppMessage(
        context: Context,
        phoneNumber: String,
        message: String,
        showSuccessToast: Boolean = true
    ): Boolean {
        val cleanPhone = sanitizePhoneNumber(phoneNumber)
        if (cleanPhone.isBlank()) {
            Toast.makeText(context, "Please provide a valid phone number", Toast.LENGTH_SHORT).show()
            return false
        }

        val encodedMessage = try {
            URLEncoder.encode(message, "UTF-8")
        } catch (e: Exception) {
            Uri.encode(message)
        }

        // Try direct whatsapp:// protocol first (best compatibility with native app)
        val waUri = Uri.parse("whatsapp://send?phone=$cleanPhone&text=$encodedMessage")
        val directIntent = Intent(Intent.ACTION_VIEW, waUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // Check if official WhatsApp or Business is installed
        val packageManager = context.packageManager
        val waPackages = listOf("com.whatsapp", "com.whatsapp.w4b")
        for (pkg in waPackages) {
            try {
                packageManager.getPackageInfo(pkg, 0)
                directIntent.setPackage(pkg)
                context.startActivity(directIntent)
                if (showSuccessToast) {
                    Toast.makeText(context, "Opening WhatsApp for +$cleanPhone...", Toast.LENGTH_SHORT).show()
                }
                return true
            } catch (_: Exception) {
                // Continue to next package or web fallback
            }
        }

        // Fallback to https://api.whatsapp.com web intent
        try {
            val webUrl = "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage"
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
            if (showSuccessToast) {
                Toast.makeText(context, "Opening WhatsApp web dispatch for +$cleanPhone...", Toast.LENGTH_SHORT).show()
            }
            return true
        } catch (e: Exception) {
            // Ultimate fallback to wa.me URL
            try {
                val waMeUrl = "https://wa.me/$cleanPhone?text=$encodedMessage"
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(waMeUrl)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
                return true
            } catch (ex: Exception) {
                Toast.makeText(context, "Unable to open WhatsApp: ${ex.localizedMessage}", Toast.LENGTH_SHORT).show()
                return false
            }
        }
    }

    /**
     * Helper to send company profile directly to a lead.
     */
    fun sendCompanyProfileToLead(
        context: Context,
        leadName: String,
        leadPhone: String,
        companyName: String = "",
        brochureConfig: AutoBrochureConfigEntity? = null
    ): Boolean {
        val message = buildCompanyProfileMessage(leadName, companyName, brochureConfig)
        return sendWhatsAppMessage(context, leadPhone, message, showSuccessToast = true)
    }

    /**
     * Automated WhatsApp follow-up tailored to the lead's current stage.
     */
    fun sendLeadFollowUp(
        context: Context,
        phoneNumber: String,
        clientName: String,
        stage: String,
        requirement: String,
        agentName: String = "Making Brands Sales Team"
    ): Boolean {
        val stageHeader = when (stage.lowercase()) {
            "new" -> "Thank you for reaching out to Making Brands regarding your *${requirement.trim()}* requirement."
            "contacted" -> "Following up on our recent discussion regarding *${requirement.trim()}*."
            "interested" -> "We are excited about collaborating on your *${requirement.trim()}* project!"
            "proposal" -> "Have you had a chance to review our detailed project proposal for *${requirement.trim()}*?"
            "negotiation" -> "Checking in on the final contract terms for *${requirement.trim()}*."
            "won" -> "Welcome onboard to Making Brands! We have kicked off your *${requirement.trim()}* milestones."
            else -> "Quick check-in regarding your *${requirement.trim()}* requirements."
        }

        val message = """
            👋 Hi ${clientName.trim()},

            $stageHeader

            We would love to answer any questions or hop on a quick 10-minute call to finalize the next steps.

            🗓️ *Project Requirement:* ${requirement.trim()}
            💼 *Representative:* $agentName
            🌐 *Website:* makingbrands.in

            Feel free to reply directly to this chat!
        """.trimIndent()

        return sendWhatsAppMessage(context, phoneNumber, message, showSuccessToast = true)
    }

    /**
     * Dispatches quotation/estimate details directly over WhatsApp.
     */
    fun sendQuotationEstimate(
        context: Context,
        phoneNumber: String,
        clientName: String,
        quotationNumber: String,
        totalAmount: Double,
        currency: String = "₹",
        scopeOfWork: String = "Mobile & Web Development"
    ): Boolean {
        val formattedAmount = String.format(java.util.Locale.US, "%,.2f", totalAmount)
        val message = """
            📄 *OFFICIAL ESTIMATE & QUOTATION*
            ━━━━━━━━━━━━━━━━━━━━━
            Dear ${clientName.trim()},

            Here is the formal quotation from *Making Brands*:
            • *Quotation No:* $quotationNumber
            • *Scope:* $scopeOfWork
            • *Total Investment:* $currency $formattedAmount (incl. GST)
            • *Terms:* 50% Advance on kickoff, 50% on final UAT milestone

            You can review the full project breakdown and sign off directly with our management team.

            Best regards,
            *Making Brands Accounts*
            📞 +91 98765 43210 | 🌐 makingbrands.in
        """.trimIndent()

        return sendWhatsAppMessage(context, phoneNumber, message, showSuccessToast = true)
    }

    /**
     * Sends automated Daily Standup Digest to Admin / Management.
     */
    fun sendDailyStandupDigest(
        context: Context,
        adminPhone: String,
        date: String,
        presentHeadcount: Int,
        totalEmployees: Int,
        completedTasksToday: Int,
        pendingTasks: Int,
        newLeadsToday: Int,
        totalActiveProjects: Int
    ): Boolean {
        val message = """
            📊 *DAILY STANDUP DIGEST — MAKING BRANDS*
            🗓️ *Date:* $date
            ━━━━━━━━━━━━━━━━━━━━━
            👥 *Attendance:* $presentHeadcount / $totalEmployees On-Duty (${(presentHeadcount * 100 / totalEmployees.coerceAtLeast(1))}% presence)
            ✅ *Tasks Closed Today:* $completedTasksToday ($pendingTasks in progress)
            🎯 *Active Client Projects:* $totalActiveProjects
            📈 *New Leads Pipeline:* $newLeadsToday incoming leads today

            _Generated automatically via MB Traker Enterprise OS._
        """.trimIndent()

        return sendWhatsAppMessage(context, adminPhone, message, showSuccessToast = true)
    }

    /**
     * Shares exportable timesheet summary via WhatsApp.
     */
    fun shareTimesheetReport(
        context: Context,
        recipientPhone: String,
        employeeName: String,
        month: String,
        totalHours: Double,
        overtimeHours: Double,
        daysPresent: Int
    ): Boolean {
        val message = """
            ⏱️ *EMPLOYEE MONTHLY TIMESHEET SUMMARY*
            ━━━━━━━━━━━━━━━━━━━━━
            • *Employee:* $employeeName
            • *Billing Period:* $month
            • *Days Logged:* $daysPresent Days
            • *Total Regular Hours:* ${String.format(java.util.Locale.US, "%.1f", totalHours)} hrs
            • *Approved Overtime:* ${String.format(java.util.Locale.US, "%.1f", overtimeHours)} hrs
            • *Status:* Verified by Geofenced Attendance System

            _Making Brands Human Resources & Payroll_
        """.trimIndent()

        return sendWhatsAppMessage(context, recipientPhone, message, showSuccessToast = true)
    }
}
