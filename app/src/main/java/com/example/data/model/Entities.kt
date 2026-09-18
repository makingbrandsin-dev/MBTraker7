package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

enum class Department {
    ENGINEERING,
    DESIGN,
    MARKETING,
    SALES,
    OPERATIONS,
    MANAGEMENT,
    HR
}

enum class EmployeeRole {
    ADMIN,
    MANAGER,
    TEAM_LEAD,
    DEVELOPER,
    DESIGNER,
    QA_ENGINEER,
    HR_SPECIALIST,
    SALES_EXECUTIVE
}

enum class EmployeeStatus {
    ACTIVE,
    ON_LEAVE,
    PROBATION,
    INACTIVE
}

enum class PresenceStatus {
    ONLINE,
    IN_MEETING,
    OFFLINE
}

enum class ProjectPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class ProjectStatus {
    PLANNING,
    ACTIVE,
    ON_HOLD,
    COMPLETED,
    OVERDUE
}

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val phone: String,
    val designation: String,
    val department: Department = Department.ENGINEERING,
    val role: EmployeeRole = EmployeeRole.DEVELOPER,
    val status: EmployeeStatus = EmployeeStatus.ACTIVE,
    val presenceStatus: PresenceStatus = PresenceStatus.ONLINE,
    val joiningDate: Date = Date(),
    val skills: List<String> = emptyList(),
    val assignedProjectIds: List<Long> = emptyList(),
    val avatarUrl: String? = null,
    val salary: Double? = null,
    val emergencyContact: String? = null
)

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val checkInTime: String, // HH:mm:ss or HH:mm
    val checkOutTime: String? = null,
    val durationMinutes: Long = 0,
    val isWorking: Boolean = false,
    val status: String = "Present", // Present, Late, Half Day
    val overtimeMinutes: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val employeeName: String = "Rahul Sharma"
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val clientName: String,
    val totalTasks: Int,
    val completedTasks: Int,
    val progressPercent: Int,
    val status: String, // Active, Completed, Overdue
    val priority: String = "High", // High, Medium, Low
    val startDate: String = "01 Sep 2025",
    val deadline: String = "28 Sep 2025",
    val managerName: String = "Rahul Sharma",
    val teamSize: Int = 5,
    val description: String = "Enterprise mobile & web portal revamp with seamless user experience.",
    val tags: List<String> = emptyList(),
    val assignedEmployeeIds: List<Long> = emptyList(),
    val budget: Double? = 250000.0,
    val createdAt: Date = Date()
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long = 1,
    val projectName: String = "Website Revamp",
    val title: String,
    val dueDate: String,
    val priority: String, // High, Medium, Low
    val status: String, // Backlog, In Progress, Completed
    val isCompleted: Boolean = false,
    val assignee: String = "Rahul Sharma"
)

@Entity(tableName = "leads")
data class LeadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val company: String,
    val phone: String,
    val email: String,
    val leadScore: Int, // e.g. 85
    val requirement: String, // e.g. Website + App Development
    val potentialValue: String, // e.g. ₹ 2,50,000
    val stage: String, // New, Contacted, Interested, Follow-up, Proposal, Negotiation, Won
    val assignedTo: String = "Arjun Mehta",
    val nextFollowUp: String = "Today, 4:30 PM",
    val notes: String = "Client requested detailed quote with milestone breakdowns.",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "follow_ups")
data class FollowUpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val leadId: Long,
    val clientName: String,
    val taskDescription: String,
    val scheduledTime: String, // e.g. 10:30 AM
    val scheduledDateCategory: String, // Today, Upcoming, Completed
    val actionType: String = "Call", // Call, Open, Send proposal
    val isCompleted: Boolean = false
)

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactName: String,
    val callType: String, // Incoming, Outgoing, Missed
    val status: String, // Connected, No Answer, Busy
    val timestampText: String, // Today, 09:32 AM
    val durationText: String, // 4m 12s
    val phoneNumber: String = "+91 98765 43210"
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: String, // general, dev_team, sales_team, marketing
    val senderName: String,
    val senderRole: String = "Team Member",
    val messageText: String,
    val timestampText: String,
    val isMe: Boolean = false,
    val attachmentFileName: String? = null,
    val attachmentFileSize: String? = null
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subtitle: String,
    val timeAgo: String,
    val category: String, // followup, task, message, attendance, leave, project
    val isRead: Boolean = false
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Long = 1L,
    val name: String,
    val role: String,
    val isOnboarded: Boolean = true,
    val email: String = "makingbrands.in@gmail.com",
    val phone: String = "+91 98765 43210",
    val department: String = "Engineering",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "leave_applications")
data class LeaveApplicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val leaveType: String = "Casual Leave", // Casual Leave, Sick Leave, Paid Leave, Work From Home
    val startDate: String, // e.g. "16 Sep 2025"
    val endDate: String,   // e.g. "17 Sep 2025"
    val totalDays: Int = 1,
    val reason: String = "",
    val status: String = "Pending", // Pending, Approved, Rejected
    val appliedDate: String = "18 Sep 2025",
    val createdAt: Long = System.currentTimeMillis()
)

