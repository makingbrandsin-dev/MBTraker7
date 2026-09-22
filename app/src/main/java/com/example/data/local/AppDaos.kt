package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records ORDER BY id DESC LIMIT 1")
    fun getLatestAttendance(): Flow<AttendanceRecord?>

    @Query("SELECT * FROM attendance_records ORDER BY id DESC")
    fun getAllAttendance(): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: AttendanceRecord): Long

    @Update
    suspend fun update(record: AttendanceRecord)

    @Delete
    suspend fun delete(record: AttendanceRecord)

    @Query("DELETE FROM attendance_records")
    suspend fun clearAll()
}

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees ORDER BY id ASC")
    fun getAllEmployees(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE id = :id")
    fun getEmployeeById(id: Long): Flow<EmployeeEntity?>

    @Query("SELECT * FROM employees WHERE department = :department ORDER BY name ASC")
    fun getEmployeesByDepartment(department: Department): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE status = :status ORDER BY name ASC")
    fun getEmployeesByStatus(status: EmployeeStatus): Flow<List<EmployeeEntity>>

    @Query("SELECT COUNT(*) FROM employees")
    fun getEmployeeCount(): Flow<Int>

    @Query("SELECT * FROM employees WHERE id IN (:ids)")
    fun getEmployeesByIds(ids: List<Long>): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getEmployeeByEmail(email: String): EmployeeEntity?

    @Query("SELECT * FROM employees WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    fun getEmployeeByEmailFlow(email: String): Flow<EmployeeEntity?>

    @Query("SELECT COUNT(*) FROM employees")
    suspend fun getEmployeeCountDirect(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(employee: EmployeeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(employees: List<EmployeeEntity>)

    @Update
    suspend fun update(employee: EmployeeEntity)

    @Delete
    suspend fun delete(employee: EmployeeEntity)

    @Query("DELETE FROM employees WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM employees")
    suspend fun clearAll()
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY id ASC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectById(id: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE status = :status ORDER BY id ASC")
    fun getProjectsByStatus(status: String): Flow<List<ProjectEntity>>

    @Query("SELECT COUNT(*) FROM projects")
    fun getProjectCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(projects: List<ProjectEntity>)

    @Update
    suspend fun update(project: ProjectEntity)

    @Delete
    suspend fun delete(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM projects")
    suspend fun clearAll()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY id DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY id DESC")
    fun getTasksByProject(projectId: Long): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1")
    fun getCompletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTaskCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTaskById(id: Long): Flow<TaskEntity?>

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("DELETE FROM tasks")
    suspend fun clearAll()
}

@Dao
interface LeadDao {
    @Query("SELECT * FROM leads ORDER BY id DESC, createdAt DESC")
    fun getAllLeads(): Flow<List<LeadEntity>>

    @Query("SELECT * FROM leads WHERE id = :id")
    fun getLeadById(id: Long): Flow<LeadEntity?>

    @Query("SELECT * FROM leads WHERE source = :source ORDER BY id DESC, createdAt DESC")
    fun getLeadsBySource(source: String): Flow<List<LeadEntity>>

    @Query("SELECT COUNT(*) FROM leads")
    suspend fun getLeadCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lead: LeadEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(leads: List<LeadEntity>)

    @Update
    suspend fun update(lead: LeadEntity)

    @Delete
    suspend fun delete(lead: LeadEntity)

    @Query("DELETE FROM leads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM leads")
    suspend fun clearAll()
}

@Dao
interface LeadSourceDao {
    @Query("SELECT * FROM lead_source_configs ORDER BY displayName ASC")
    fun getAllSourceConfigs(): Flow<List<LeadSourceConfigEntity>>

    @Query("SELECT * FROM lead_source_configs WHERE sourceId = :sourceId LIMIT 1")
    fun getSourceConfigById(sourceId: String): Flow<LeadSourceConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: LeadSourceConfigEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(configs: List<LeadSourceConfigEntity>)

    @Update
    suspend fun update(config: LeadSourceConfigEntity)

    @Delete
    suspend fun delete(config: LeadSourceConfigEntity)
}

@Dao
interface FollowUpDao {
    @Query("SELECT * FROM follow_ups ORDER BY id DESC")
    fun getAllFollowUps(): Flow<List<FollowUpEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(followUp: FollowUpEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(followUps: List<FollowUpEntity>)

    @Update
    suspend fun update(followUp: FollowUpEntity)

    @Query("DELETE FROM follow_ups")
    suspend fun clearAll()
}

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs ORDER BY id DESC")
    fun getAllCallLogs(): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: CallLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<CallLogEntity>)

    @Query("DELETE FROM call_logs")
    suspend fun clearAll()
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE channelId = :channelId ORDER BY id ASC")
    fun getMessagesForChannel(channelId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY id DESC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT COUNT(*) FROM chat_messages")
    suspend fun getMessageCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<ChatMessageEntity>)

    @Delete
    suspend fun delete(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY id DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun getNotificationCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllAsRead()

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("DELETE FROM notifications")
    suspend fun clearAll()
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)

    @Query("SELECT COUNT(*) FROM user_profile WHERE id = 1 AND isOnboarded = 1")
    suspend fun isOnboarded(): Int

    @Query("UPDATE user_profile SET name = :name, role = :role, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateNameAndRole(name: String, role: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE user_profile SET name = '', role = '', email = '', phone = '', department = '', emergencyContact = '', address = '', skills = '', bio = '', updatedAt = :updatedAt WHERE id = 1")
    suspend fun clearProfileFields(updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM user_profile")
    suspend fun clearProfile()
}

@Dao
interface LeaveDao {
    @Query("SELECT * FROM leave_applications ORDER BY id DESC")
    fun getAllLeaves(): Flow<List<LeaveApplicationEntity>>

    @Query("SELECT * FROM leave_applications WHERE username = :username ORDER BY id DESC")
    fun getLeavesForUser(username: String): Flow<List<LeaveApplicationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(leave: LeaveApplicationEntity): Long

    @Update
    suspend fun update(leave: LeaveApplicationEntity)

    @Delete
    suspend fun delete(leave: LeaveApplicationEntity)

    @Query("DELETE FROM leave_applications")
    suspend fun clearAll()
}

@Dao
interface CallRecordingDao {
    @Query("SELECT * FROM call_recordings ORDER BY id DESC")
    fun getAllRecordings(): Flow<List<CallRecordingEntity>>

    @Query("SELECT * FROM call_recordings WHERE callMedium = :medium ORDER BY id DESC")
    fun getRecordingsByMedium(medium: String): Flow<List<CallRecordingEntity>>

    @Query("SELECT COUNT(*) FROM call_recordings")
    fun getRecordingsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recording: CallRecordingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recordings: List<CallRecordingEntity>)

    @Update
    suspend fun update(recording: CallRecordingEntity)

    @Delete
    suspend fun delete(recording: CallRecordingEntity)

    @Query("DELETE FROM call_recordings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM call_recordings")
    suspend fun clearAll()
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY id DESC")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE status = :status ORDER BY id DESC")
    fun getInvoicesByStatus(status: String): Flow<List<InvoiceEntity>>

    @Query("SELECT COUNT(*) FROM invoices")
    fun getInvoiceCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invoice: InvoiceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(invoices: List<InvoiceEntity>)

    @Update
    suspend fun update(invoice: InvoiceEntity)

    @Delete
    suspend fun delete(invoice: InvoiceEntity)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM invoices")
    suspend fun clearAll()
}

@Dao
interface QuotationDao {
    @Query("SELECT * FROM quotations ORDER BY id DESC")
    fun getAllQuotations(): Flow<List<QuotationEntity>>

    @Query("SELECT * FROM quotations WHERE status = :status ORDER BY id DESC")
    fun getQuotationsByStatus(status: String): Flow<List<QuotationEntity>>

    @Query("SELECT COUNT(*) FROM quotations")
    fun getQuotationCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(quotation: QuotationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(quotations: List<QuotationEntity>)

    @Update
    suspend fun update(quotation: QuotationEntity)

    @Delete
    suspend fun delete(quotation: QuotationEntity)

    @Query("DELETE FROM quotations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM quotations")
    suspend fun clearAll()
}

@Dao
interface AutoBrochureDao {
    @Query("SELECT * FROM auto_brochure_configs WHERE id = 1 LIMIT 1")
    fun getAutoBrochureConfig(): Flow<AutoBrochureConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: AutoBrochureConfigEntity)

    @Query("UPDATE auto_brochure_configs SET totalSentCount = totalSentCount + 1, lastSentTimestamp = :lastSent WHERE id = 1")
    suspend fun incrementSentCount(lastSent: String)
}

@Dao
interface SocialReviewDao {
    @Query("SELECT * FROM social_review_configs ORDER BY platformName ASC")
    fun getAllReviewConfigs(): Flow<List<SocialReviewConfigEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: SocialReviewConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(configs: List<SocialReviewConfigEntity>)

    @Update
    suspend fun update(config: SocialReviewConfigEntity)

    @Delete
    suspend fun delete(config: SocialReviewConfigEntity)
}

@Dao
interface ClientMeetingDao {
    @Query("SELECT * FROM client_meetings ORDER BY id DESC")
    fun getAllMeetings(): Flow<List<ClientMeetingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meeting: ClientMeetingEntity): Long

    @Update
    suspend fun update(meeting: ClientMeetingEntity)

    @Query("DELETE FROM client_meetings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM client_meetings")
    suspend fun clearAll()
}

@Dao
interface ExpenseClaimDao {
    @Query("SELECT * FROM expense_claims ORDER BY id DESC")
    fun getAllExpenses(): Flow<List<ExpenseClaimEntity>>

    @Query("SELECT * FROM expense_claims WHERE status = :status ORDER BY id DESC")
    fun getExpensesByStatus(status: String): Flow<List<ExpenseClaimEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseClaimEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<ExpenseClaimEntity>)

    @Update
    suspend fun update(expense: ExpenseClaimEntity)

    @Query("UPDATE expense_claims SET status = :status, reviewedBy = :reviewedBy WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, reviewedBy: String)

    @Query("DELETE FROM expense_claims WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM expense_claims")
    suspend fun clearAll()
}

@Dao
interface ProjectMilestoneDao {
    @Query("SELECT * FROM project_milestones ORDER BY id ASC")
    fun getAllMilestones(): Flow<List<ProjectMilestoneEntity>>

    @Query("SELECT * FROM project_milestones WHERE projectId = :projectId ORDER BY id ASC")
    fun getMilestonesByProject(projectId: Long): Flow<List<ProjectMilestoneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(milestone: ProjectMilestoneEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(milestones: List<ProjectMilestoneEntity>)

    @Update
    suspend fun update(milestone: ProjectMilestoneEntity)

    @Query("UPDATE project_milestones SET completionPercent = :percent, isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateProgress(id: Long, percent: Int, isCompleted: Boolean)

    @Query("DELETE FROM project_milestones WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM project_milestones")
    suspend fun clearAll()
}

@Dao
interface VaultDocumentDao {
    @Query("SELECT * FROM vault_documents ORDER BY id DESC")
    fun getAllDocuments(): Flow<List<VaultDocumentEntity>>

    @Query("SELECT * FROM vault_documents WHERE category = :category ORDER BY id DESC")
    fun getDocumentsByCategory(category: String): Flow<List<VaultDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: VaultDocumentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(documents: List<VaultDocumentEntity>)

    @Delete
    suspend fun delete(document: VaultDocumentEntity)

    @Query("DELETE FROM vault_documents WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM vault_documents")
    suspend fun clearAll()
}

@Dao
interface AttendanceRegularizationDao {
    @Query("SELECT * FROM attendance_regularizations ORDER BY id DESC")
    fun getAllRegularizations(): Flow<List<AttendanceRegularizationEntity>>

    @Query("SELECT * FROM attendance_regularizations WHERE employeeName = :empName ORDER BY id DESC")
    fun getRegularizationsForEmployee(empName: String): Flow<List<AttendanceRegularizationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(regularization: AttendanceRegularizationEntity): Long

    @Update
    suspend fun update(regularization: AttendanceRegularizationEntity)

    @Query("UPDATE attendance_regularizations SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM attendance_regularizations")
    suspend fun clearAll()
}



