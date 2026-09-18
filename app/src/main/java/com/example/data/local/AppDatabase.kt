package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

@Database(
    entities = [
        EmployeeEntity::class,
        AttendanceRecord::class,
        ProjectEntity::class,
        TaskEntity::class,
        LeadEntity::class,
        FollowUpEntity::class,
        CallLogEntity::class,
        ChatMessageEntity::class,
        NotificationEntity::class,
        UserProfileEntity::class,
        LeaveApplicationEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun employeeDao(): EmployeeDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun projectDao(): ProjectDao
    abstract fun taskDao(): TaskDao
    abstract fun leadDao(): LeadDao
    abstract fun followUpDao(): FollowUpDao
    abstract fun callLogDao(): CallLogDao
    abstract fun chatDao(): ChatDao
    abstract fun notificationDao(): NotificationDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun leaveDao(): LeaveDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mb_traker_database"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }

            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                super.onDestructiveMigration(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(db: AppDatabase) {
            // Employees
            db.employeeDao().insertAll(
                listOf(
                    EmployeeEntity(
                        name = "Rahul Sharma",
                        email = "rahul.sharma@makingbrands.in",
                        phone = "+91 98765 43210",
                        designation = "Senior Android Developer",
                        department = Department.ENGINEERING,
                        role = EmployeeRole.DEVELOPER,
                        status = EmployeeStatus.ACTIVE,
                        presenceStatus = PresenceStatus.ONLINE,
                        joiningDate = Date(1672531199000L), // Jan 2023
                        skills = listOf("Kotlin", "Jetpack Compose", "Room", "Coroutines", "MVVM"),
                        assignedProjectIds = listOf(1L, 2L),
                        salary = 95000.0,
                        emergencyContact = "+91 98765 00001"
                    ),
                    EmployeeEntity(
                        name = "Priya Singh",
                        email = "priya.singh@makingbrands.in",
                        phone = "+91 98111 22334",
                        designation = "Lead QA Engineer",
                        department = Department.ENGINEERING,
                        role = EmployeeRole.QA_ENGINEER,
                        status = EmployeeStatus.ACTIVE,
                        presenceStatus = PresenceStatus.IN_MEETING,
                        joiningDate = Date(1685577600000L), // June 2023
                        skills = listOf("Manual Testing", "Espresso", "Robolectric", "API Testing"),
                        assignedProjectIds = listOf(1L, 2L),
                        salary = 85000.0,
                        emergencyContact = "+91 98111 00002"
                    ),
                    EmployeeEntity(
                        name = "Arjun Mehta",
                        email = "arjun.mehta@makingbrands.in",
                        phone = "+91 99222 33445",
                        designation = "Engineering Manager",
                        department = Department.MANAGEMENT,
                        role = EmployeeRole.MANAGER,
                        status = EmployeeStatus.ACTIVE,
                        presenceStatus = PresenceStatus.ONLINE,
                        joiningDate = Date(1640995200000L), // Jan 2022
                        skills = listOf("Team Leadership", "Agile/Scrum", "System Design", "Sprint Planning"),
                        assignedProjectIds = listOf(1L, 2L, 3L),
                        salary = 145000.0,
                        emergencyContact = "+91 99222 00003"
                    ),
                    EmployeeEntity(
                        name = "Neha Gupta",
                        email = "neha.gupta@makingbrands.in",
                        phone = "+91 99333 44556",
                        designation = "Senior Product Designer",
                        department = Department.DESIGN,
                        role = EmployeeRole.DESIGNER,
                        status = EmployeeStatus.ACTIVE,
                        presenceStatus = PresenceStatus.OFFLINE,
                        joiningDate = Date(1693526400000L), // Sep 2023
                        skills = listOf("Figma", "Design Systems", "Prototyping", "User Research"),
                        assignedProjectIds = listOf(1L),
                        salary = 90000.0,
                        emergencyContact = "+91 99333 00004"
                    ),
                    EmployeeEntity(
                        name = "Suresh Patel",
                        email = "suresh.patel@makingbrands.in",
                        phone = "+91 99444 55667",
                        designation = "Business Development Lead",
                        department = Department.SALES,
                        role = EmployeeRole.SALES_EXECUTIVE,
                        status = EmployeeStatus.ACTIVE,
                        presenceStatus = PresenceStatus.IN_MEETING,
                        joiningDate = Date(1677628800000L), // Mar 2023
                        skills = listOf("CRM", "Client Relations", "B2B Negotiations", "Lead Generation"),
                        assignedProjectIds = listOf(3L),
                        salary = 80000.0,
                        emergencyContact = "+91 99444 00005"
                    )
                )
            )

            // Initial attendance history (previous day completed record so today starts fresh with Check In)
            db.attendanceDao().insert(
                AttendanceRecord(
                    date = "2025-09-15",
                    checkInTime = "09:05 AM",
                    checkOutTime = "06:12 PM",
                    durationMinutes = 547, // 09h 07m
                    isWorking = false,
                    status = "Present",
                    overtimeMinutes = 35
                )
            )

            // Projects
            db.projectDao().insertAll(
                listOf(
                    ProjectEntity(
                        name = "Website Revamp",
                        clientName = "ABC Ltd",
                        totalTasks = 12,
                        completedTasks = 10,
                        progressPercent = 82,
                        status = "Active",
                        priority = "High",
                        startDate = "01 Sep 2025",
                        deadline = "28 Sep 2025",
                        managerName = "Rahul Sharma",
                        teamSize = 5,
                        tags = listOf("Web", "Frontend", "Next.js", "Redesign"),
                        assignedEmployeeIds = listOf(1L, 2L, 3L, 4L),
                        budget = 350000.0
                    ),
                    ProjectEntity(
                        name = "Mobile App Development",
                        clientName = "XYZ Pvt Ltd",
                        totalTasks = 8,
                        completedTasks = 4,
                        progressPercent = 45,
                        status = "Active",
                        priority = "High",
                        startDate = "15 Aug 2025",
                        deadline = "15 Oct 2025",
                        managerName = "Priya Singh",
                        teamSize = 6,
                        tags = listOf("Mobile", "Android", "Kotlin", "Compose"),
                        assignedEmployeeIds = listOf(1L, 2L, 3L),
                        budget = 500000.0
                    ),
                    ProjectEntity(
                        name = "SEO Optimization",
                        clientName = "Digital Media",
                        totalTasks = 6,
                        completedTasks = 6,
                        progressPercent = 100,
                        status = "Completed",
                        priority = "Medium",
                        startDate = "01 Aug 2025",
                        deadline = "10 Sep 2025",
                        managerName = "Rahul Sharma",
                        teamSize = 3,
                        tags = listOf("Marketing", "SEO", "Analytics"),
                        assignedEmployeeIds = listOf(3L, 5L),
                        budget = 120000.0
                    )
                )
            )

            // Tasks
            db.taskDao().insertAll(
                listOf(
                    TaskEntity(
                        title = "Homepage Design",
                        projectName = "Website Revamp",
                        dueDate = "20 Sep 2025",
                        priority = "High",
                        status = "In Progress",
                        isCompleted = false
                    ),
                    TaskEntity(
                        title = "API Integration",
                        projectName = "Mobile App Dev",
                        dueDate = "22 Sep 2025",
                        priority = "Medium",
                        status = "In Progress",
                        isCompleted = false
                    ),
                    TaskEntity(
                        title = "Content Writing",
                        projectName = "SEO Optimization",
                        dueDate = "26 Sep 2025",
                        priority = "Low",
                        status = "Completed",
                        isCompleted = true
                    ),
                    TaskEntity(
                        title = "Testing & QA",
                        projectName = "Website Revamp",
                        dueDate = "28 Sep 2025",
                        priority = "High",
                        status = "Backlog",
                        isCompleted = false
                    ),
                    TaskEntity(
                        title = "Database Migration",
                        projectName = "Website Revamp",
                        dueDate = "18 Sep 2025",
                        priority = "High",
                        status = "Completed",
                        isCompleted = true
                    )
                )
            )

            // Leads
            db.leadDao().insertAll(
                listOf(
                    LeadEntity(
                        name = "Ramesh Kumar",
                        company = "ABC Industries",
                        phone = "+91 98765 43210",
                        email = "ramesh@abc.com",
                        leadScore = 85,
                        requirement = "Website + App Development",
                        potentialValue = "₹ 2,50,000",
                        stage = "Interested",
                        assignedTo = "Arjun Mehta",
                        nextFollowUp = "Today, 4:30 PM"
                    ),
                    LeadEntity(
                        name = "Suresh Patel",
                        company = "XYZ Traders",
                        phone = "+91 98111 22334",
                        email = "suresh@xyz.com",
                        leadScore = 72,
                        requirement = "ERP Customization",
                        potentialValue = "₹ 1,80,000",
                        stage = "Contacted",
                        assignedTo = "Rahul Sharma",
                        nextFollowUp = "Tomorrow, 11:00 AM"
                    ),
                    LeadEntity(
                        name = "Neha Gupta",
                        company = "Digital Solutions",
                        phone = "+91 99222 33445",
                        email = "neha@digitalsol.com",
                        leadScore = 92,
                        requirement = "Cloud Migration & SEO",
                        potentialValue = "₹ 4,20,000",
                        stage = "Proposal",
                        assignedTo = "Arjun Mehta",
                        nextFollowUp = "22 Sep, 2:00 PM"
                    )
                )
            )

            // Follow-ups
            db.followUpDao().insertAll(
                listOf(
                    FollowUpEntity(
                        leadId = 1,
                        clientName = "ABC Industries",
                        taskDescription = "Call customer regarding final proposal scope",
                        scheduledTime = "10:30 AM",
                        scheduledDateCategory = "Today",
                        actionType = "Call",
                        isCompleted = false
                    ),
                    FollowUpEntity(
                        leadId = 2,
                        clientName = "XYZ Traders",
                        taskDescription = "Send quotation & tech stack breakdown",
                        scheduledTime = "11:45 AM",
                        scheduledDateCategory = "Today",
                        actionType = "Open",
                        isCompleted = false
                    ),
                    FollowUpEntity(
                        leadId = 1,
                        clientName = "Client Meeting",
                        taskDescription = "Follow-up video call with technical leads",
                        scheduledTime = "03:00 PM",
                        scheduledDateCategory = "Today",
                        actionType = "Call",
                        isCompleted = false
                    ),
                    FollowUpEntity(
                        leadId = 3,
                        clientName = "Digital Solutions",
                        taskDescription = "Send formal commercial proposal contract",
                        scheduledTime = "04:30 PM",
                        scheduledDateCategory = "Today",
                        actionType = "Open",
                        isCompleted = false
                    )
                )
            )

            // Call Logs
            db.callLogDao().insertAll(
                listOf(
                    CallLogEntity(
                        contactName = "Ramesh Kumar",
                        callType = "Incoming",
                        status = "Connected",
                        timestampText = "Today, 09:32 AM",
                        durationText = "4m 12s",
                        phoneNumber = "+91 98765 43210"
                    ),
                    CallLogEntity(
                        contactName = "XYZ Traders",
                        callType = "Outgoing",
                        status = "Connected",
                        timestampText = "Today, 11:15 AM",
                        durationText = "2m 41s",
                        phoneNumber = "+91 98111 22334"
                    ),
                    CallLogEntity(
                        contactName = "ABC Industries",
                        callType = "Outgoing",
                        status = "No Answer",
                        timestampText = "Today, 01:22 PM",
                        durationText = "5m 03s",
                        phoneNumber = "+91 98765 43210"
                    ),
                    CallLogEntity(
                        contactName = "Digital Solutions",
                        callType = "Incoming",
                        status = "Connected",
                        timestampText = "Yesterday, 04:22 PM",
                        durationText = "3m 03s",
                        phoneNumber = "+91 99222 33445"
                    )
                )
            )

            // Chat Messages
            db.chatDao().insertAll(
                listOf(
                    ChatMessageEntity(
                        channelId = "dev_team",
                        senderName = "Rahul Sharma",
                        senderRole = "Senior Developer",
                        messageText = "The new build is ready for testing.",
                        timestampText = "10:04 AM",
                        isMe = true
                    ),
                    ChatMessageEntity(
                        channelId = "dev_team",
                        senderName = "Priya Singh",
                        senderRole = "QA Engineer",
                        messageText = "Great! I'll check and update the release checklist.",
                        timestampText = "10:32 AM",
                        isMe = false
                    ),
                    ChatMessageEntity(
                        channelId = "dev_team",
                        senderName = "Priya Singh",
                        senderRole = "QA Engineer",
                        messageText = "Attached the verified release APK package for review.",
                        timestampText = "10:33 AM",
                        isMe = false,
                        attachmentFileName = "app-release-v1.2.0.apk",
                        attachmentFileSize = "2.4 MB"
                    ),
                    ChatMessageEntity(
                        channelId = "dev_team",
                        senderName = "Arjun Mehta",
                        senderRole = "Team Lead",
                        messageText = "Looks good. Deploying today after client approval.",
                        timestampText = "11:05 AM",
                        isMe = false
                    )
                )
            )

            // Notifications
            db.notificationDao().insertAll(
                listOf(
                    NotificationEntity(
                        title = "Follow-up due",
                        subtitle = "ABC Industries · 10 minutes ago",
                        timeAgo = "10m ago",
                        category = "followup"
                    ),
                    NotificationEntity(
                        title = "Task assigned",
                        subtitle = "Homepage redesign · 1 hour ago",
                        timeAgo = "1h ago",
                        category = "task"
                    ),
                    NotificationEntity(
                        title = "New message",
                        subtitle = "Rahul · 2 hours ago",
                        timeAgo = "2h ago",
                        category = "message"
                    ),
                    NotificationEntity(
                        title = "Attendance reminder",
                        subtitle = "You haven't checked out · 3 hours ago",
                        timeAgo = "3h ago",
                        category = "attendance"
                    ),
                    NotificationEntity(
                        title = "Leave approved",
                        subtitle = "Project update · 5 hours ago",
                        timeAgo = "5h ago",
                        category = "leave"
                    ),
                    NotificationEntity(
                        title = "Project update",
                        subtitle = "Website Revamp · 6 hours ago",
                        timeAgo = "6h ago",
                        category = "project"
                    )
                )
            )

            val leaveDao = db.leaveDao()
            leaveDao.insert(
                LeaveApplicationEntity(
                    username = "Rahul Sharma",
                    leaveType = "Casual Leave",
                    startDate = "22 Sep 2025",
                    endDate = "23 Sep 2025",
                    totalDays = 2,
                    reason = "Family function in hometown",
                    status = "Approved",
                    appliedDate = "14 Sep 2025"
                )
            )
            leaveDao.insert(
                LeaveApplicationEntity(
                    username = "Rahul Sharma",
                    leaveType = "Sick Leave",
                    startDate = "05 Sep 2025",
                    endDate = "05 Sep 2025",
                    totalDays = 1,
                    reason = "Viral fever and doctor consultation",
                    status = "Approved",
                    appliedDate = "05 Sep 2025"
                )
            )
        }
    }
}
