package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.screens.EmployeeActionFeedItem
import com.example.ui.screens.EmployeeActionType
import com.example.ui.screens.MainViewModel
import com.example.ui.theme.*
import com.example.util.MiloHaptics
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

/**
 * Filter Categories for the Chronological Activity Feed
 */
enum class ActivityFeedFilter(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    ALL("All Actions", Icons.Default.AllInclusive),
    PUNCH_IN("Punch-Ins", Icons.Default.Fingerprint),
    PUNCH_OUT("Punch-Outs", Icons.Default.Logout),
    TASK_COMPLETED("Tasks Done", Icons.Default.CheckCircle),
    TASK_ASSIGNED("Assigned", Icons.Default.Assignment),
    CHAT("Chats", Icons.AutoMirrored.Filled.Chat),
    LEAVE("Leaves", Icons.Default.DateRange)
}

/**
 * Generates deterministic, vibrant color palettes for employee avatars based on their name.
 */
fun getEmployeeAvatarGradient(name: String): Brush {
    val cleanName = name.trim().lowercase(Locale.ROOT)
    val hash = abs(cleanName.hashCode())
    val palettes = listOf(
        listOf(Color(0xFF2563EB), Color(0xFF38BDF8)), // Blue to Sky
        listOf(Color(0xFF059669), Color(0xFF34D399)), // Emerald to Mint
        listOf(Color(0xFF7C3AED), Color(0xFFA78BFA)), // Purple to Violet
        listOf(Color(0xFFEA580C), Color(0xFFFBBF24)), // Orange to Amber
        listOf(Color(0xFFDB2777), Color(0xFFF472B6)), // Pink to Rose
        listOf(Color(0xFF0D9488), Color(0xFF2DD4BF)), // Teal to Cyan
        listOf(Color(0xFF4F46E5), Color(0xFF818CF8)), // Indigo to Periwinkle
        listOf(Color(0xFFDC2626), Color(0xFFFB7185))  // Crimson to Coral
    )
    val selected = palettes[hash % palettes.size]
    return Brush.linearGradient(selected)
}

/**
 * Extracts 1-2 letter initials from an employee's name.
 */
fun getEmployeeInitials(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isBlank()) return "EM"
    val parts = trimmed.split(" ").filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].take(1)}${parts[1].take(1)}".uppercase(Locale.ROOT)
        parts.size == 1 -> parts[0].take(2).uppercase(Locale.ROOT)
        else -> "EM"
    }
}

/**
 * Scrollable Activity Feed Dashboard Card:
 * Real-time chronological audit and activity stream of all employee operations
 * (punch-ins, punch-outs, task completions, new assignments, team chats, and leaves)
 * equipped with user-specific avatars, instant category filters, search capabilities,
 * and live Firestore synchronization status.
 */
@Composable
fun ScrollableActivityFeedWidget(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onNavigateToTasks: () -> Unit = {},
    onNavigateToAttendance: () -> Unit = {},
    onNavigateToChat: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val rawFeedItems by viewModel.unifiedEmployeeActivityFeed.collectAsState()
    val isConnected by viewModel.isFirebaseConnected.collectAsState()

    var selectedFilter by remember { mutableStateOf(ActivityFeedFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var selectedItemForDetail by remember { mutableStateOf<EmployeeActionFeedItem?>(null) }

    // Filter items based on active category & search query
    val filteredItems = remember(rawFeedItems, selectedFilter, searchQuery) {
        rawFeedItems.filter { item ->
            val matchesFilter = when (selectedFilter) {
                ActivityFeedFilter.ALL -> true
                ActivityFeedFilter.PUNCH_IN -> item.actionType == EmployeeActionType.PUNCH_IN
                ActivityFeedFilter.PUNCH_OUT -> item.actionType == EmployeeActionType.PUNCH_OUT
                ActivityFeedFilter.TASK_COMPLETED -> item.actionType == EmployeeActionType.TASK_COMPLETED
                ActivityFeedFilter.TASK_ASSIGNED -> item.actionType == EmployeeActionType.TASK_ASSIGNED
                ActivityFeedFilter.CHAT -> item.actionType == EmployeeActionType.CHAT_MESSAGE
                ActivityFeedFilter.LEAVE -> item.actionType == EmployeeActionType.LEAVE_APPLIED
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                item.employeeName.contains(searchQuery, ignoreCase = true) ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true) ||
                item.department.contains(searchQuery, ignoreCase = true) ||
                (item.metadataTag?.contains(searchQuery, ignoreCase = true) ?: false)
            }
            matchesFilter && matchesSearch
        }
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("scrollable_activity_feed_widget")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 🏷️ Header Bar with Live Indicator & Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Activity Feed",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Employee Activity Feed",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFEFF6FF)
                            ) {
                                Text(
                                    text = "${filteredItems.size}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isConnected) Color(0xFF10B981) else Color(0xFFF59E0B))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isConnected) "Live Chronological Stream • Firestore Sync" else "Cached Audit Stream",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isConnected) Color(0xFF059669) else Color(0xFFD97706)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Search toggle
                    IconButton(
                        onClick = {
                            MiloHaptics.performReactionTick(context, haptic)
                            isSearchExpanded = !isSearchExpanded
                            if (!isSearchExpanded) searchQuery = ""
                        },
                        modifier = Modifier.size(34.dp).testTag("activity_search_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search feed",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Refresh Button
                    IconButton(
                        onClick = {
                            MiloHaptics.performReactionTick(context, haptic)
                            viewModel.triggerManualSync()
                        },
                        modifier = Modifier.size(34.dp).testTag("activity_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh feed",
                            tint = BrandBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 🔍 Expandable Search Bar
            AnimatedVisibility(
                visible = isSearchExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by employee, task, project, channel...", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("activity_search_input")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🏷️ Action Filter Horizontal Tabs
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ActivityFeedFilter.values()) { filter ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) BrandBlue else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (isSelected) BrandBlue else Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .clickable {
                                MiloHaptics.performReactionTick(context, haptic)
                                selectedFilter = filter
                            }
                            .testTag("activity_filter_${filter.name.lowercase(Locale.ROOT)}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = filter.icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else Color(0xFF64748B),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = filter.label,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF334155)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 📜 Scrollable Feed List (Inner scrollable list with fixed max height)
            if (filteredItems.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventBusy,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No activity matching \"$searchQuery\"" else "No activities recorded in this category yet.",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick = {
                                selectedFilter = ActivityFeedFilter.ALL
                                searchQuery = ""
                            }
                        ) {
                            Text("Reset Filters", fontSize = 12.sp, color = BrandBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Scrollable Box with dynamic height
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                ) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("activity_feed_scroll_list")
                    ) {
                        items(filteredItems, key = { it.id }) { item ->
                            ActivityFeedActionRow(
                                item = item,
                                onClick = {
                                    MiloHaptics.performReactionTick(context, haptic)
                                    selectedItemForDetail = item
                                }
                            )
                        }
                    }
                }
            }

            // Quick Jump Footer Bar
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Showing latest ${filteredItems.size} operations",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = {
                            MiloHaptics.performReactionTick(context, haptic)
                            onNavigateToAttendance()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Attendance Hub", fontSize = 11.sp, color = BrandBlue, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(
                        onClick = {
                            MiloHaptics.performReactionTick(context, haptic)
                            onNavigateToTasks()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Tasks Hub", fontSize = 11.sp, color = BrandBlue, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // 📋 Activity Detail Popup Dialog
    selectedItemForDetail?.let { item ->
        ActivityDetailDialog(
            item = item,
            onDismiss = { selectedItemForDetail = null },
            onNavigateToTasks = {
                selectedItemForDetail = null
                onNavigateToTasks()
            },
            onNavigateToAttendance = {
                selectedItemForDetail = null
                onNavigateToAttendance()
            },
            onNavigateToChat = {
                selectedItemForDetail = null
                onNavigateToChat()
            }
        )
    }
}

/**
 * Single Activity Item Row with User-Specific Avatar & Action Badge
 */
@Composable
fun ActivityFeedActionRow(
    item: EmployeeActionFeedItem,
    onClick: () -> Unit
) {
    val (actionBadgeIcon, badgeBgColor, badgeTextColor, actionLabel) = when (item.actionType) {
        EmployeeActionType.PUNCH_IN -> Tuple4(
            Icons.Default.Fingerprint,
            Color(0xFFECFDF5),
            Color(0xFF059669),
            "Punch-In"
        )
        EmployeeActionType.PUNCH_OUT -> Tuple4(
            Icons.Default.Logout,
            Color(0xFFFEF2F2),
            Color(0xFFDC2626),
            "Punch-Out"
        )
        EmployeeActionType.TASK_COMPLETED -> Tuple4(
            Icons.Default.CheckCircle,
            Color(0xFFEFF6FF),
            Color(0xFF2563EB),
            "Task Done"
        )
        EmployeeActionType.TASK_ASSIGNED -> Tuple4(
            Icons.Default.Assignment,
            Color(0xFFF5F3FF),
            Color(0xFF7C3AED),
            "Assigned"
        )
        EmployeeActionType.CHAT_MESSAGE -> Tuple4(
            Icons.AutoMirrored.Filled.Chat,
            Color(0xFFF0FDF4),
            Color(0xFF16A34A),
            "Chat"
        )
        EmployeeActionType.LEAVE_APPLIED -> Tuple4(
            Icons.Default.DateRange,
            Color(0xFFFFFBEB),
            Color(0xFFD97706),
            "Leave"
        )
        EmployeeActionType.CUSTOM_POST -> Tuple4(
            Icons.Default.Campaign,
            Color(0xFFFAF5FF),
            Color(0xFF9333EA),
            "Post"
        )
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("activity_row_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 👤 User-Specific Avatar with Overlay Action Icon Badge
            Box(
                modifier = Modifier.size(42.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Gradient Avatar
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(getEmployeeAvatarGradient(item.employeeName)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getEmployeeInitials(item.employeeName),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                // Overlay Action Badge in Bottom-End Corner
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(17.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(badgeTextColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = actionBadgeIcon,
                            contentDescription = actionLabel,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // 📝 Body Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Name, Role & Relative Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = item.employeeName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.department.isNotBlank()) {
                            Text(
                                text = " • ${item.department}",
                                fontSize = 10.5.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Text(
                        text = item.timeAgo,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Action Description
                Text(
                    text = item.description,
                    fontSize = 12.sp,
                    color = Color(0xFF334155),
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Metadata Tag Pill (if present)
                if (!item.metadataTag.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeBgColor
                        ) {
                            Text(
                                text = "${item.title}: ${item.metadataTag}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = badgeTextColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFCBD5E1),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Detailed Information Dialog for an Activity Item
 */
@Composable
fun ActivityDetailDialog(
    item: EmployeeActionFeedItem,
    onDismiss: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToAttendance: () -> Unit,
    onNavigateToChat: () -> Unit
) {
    val formattedDate = remember(item.timestamp) {
        SimpleDateFormat("EEEE, dd MMMM yyyy 'at' hh:mm:ss a", Locale.getDefault()).format(Date(item.timestamp))
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Big User Avatar
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(getEmployeeAvatarGradient(item.employeeName)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getEmployeeInitials(item.employeeName),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = item.employeeName,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "${item.employeeRole} • ${item.department}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Action Type", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Text(item.actionType.name.replace("_", " "), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE2E8F0))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Recorded Time", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Text(item.timeAgo, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE2E8F0))
                        Text("Timestamp", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(formattedDate, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Description Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = item.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = BrandBlue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.description,
                            fontSize = 12.sp,
                            color = Color(0xFF1E3A8A),
                            lineHeight = 17.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close", color = Color(0xFF64748B))
                    }

                    Button(
                        onClick = {
                            when (item.actionType) {
                                EmployeeActionType.PUNCH_IN, EmployeeActionType.PUNCH_OUT -> onNavigateToAttendance()
                                EmployeeActionType.TASK_COMPLETED, EmployeeActionType.TASK_ASSIGNED -> onNavigateToTasks()
                                EmployeeActionType.CHAT_MESSAGE -> onNavigateToChat()
                                else -> onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = when (item.actionType) {
                                EmployeeActionType.PUNCH_IN, EmployeeActionType.PUNCH_OUT -> "Attendance"
                                EmployeeActionType.TASK_COMPLETED, EmployeeActionType.TASK_ASSIGNED -> "View Tasks"
                                EmployeeActionType.CHAT_MESSAGE -> "Open Chat"
                                else -> "OK"
                            },
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
