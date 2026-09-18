package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    onNavigateToProfile: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
            }
        },
        actions = {
            actions()
            if (onNavigateToProfile != null) {
                IconButton(onClick = onNavigateToProfile) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(com.example.R.drawable.ic_nav_profile),
                        contentDescription = "Profile",
                        tint = BrandBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
fun MetricBadge(
    label: String,
    value: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = textColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun PriorityBadge(priority: String) {
    val (bg, fg) = when (priority.lowercase()) {
        "high" -> StatusRedBg to StatusRed
        "medium" -> StatusOrangeBg to StatusOrange
        else -> StatusGreenBg to StatusGreen
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg
    ) {
        Text(
            text = priority,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun StatusTag(text: String, isGreen: Boolean = true) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isGreen) StatusGreenBg else StatusRedBg
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isGreen) StatusGreen else StatusRed)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = if (isGreen) StatusGreen else StatusRed,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

fun formatLiveSeconds(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return String.format("%02dh %02dm %02ds", hrs, mins, secs)
}

@Composable
fun CrmTasksAttendanceSwitcher(
    selectedTab: String,
    onNavigateToCrm: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToAttendance: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF1F5F9),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val tabs = listOf(
                Triple("crm", "CRM", Icons.Default.Groups),
                Triple("tasks", "Tasks", Icons.Default.Assignment),
                Triple("attendance", "Attendance", Icons.Default.AccessTime)
            )

            tabs.forEach { (id, title, icon) ->
                val isSelected = selectedTab == id
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Color(0xFF0F172A) else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (!isSelected) {
                                when (id) {
                                    "crm" -> onNavigateToCrm()
                                    "tasks" -> onNavigateToTasks()
                                    "attendance" -> onNavigateToAttendance()
                                }
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            modifier = Modifier.size(15.dp),
                            tint = if (isSelected) Color.White else Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFF0F172A)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusIndicatorBadge(
    status: com.example.data.model.PresenceStatus,
    modifier: Modifier = Modifier,
    showLabel: Boolean = false,
    dotSize: androidx.compose.ui.unit.Dp = 8.dp
) {
    val (dotColor, labelText, bgContainer) = when (status) {
        com.example.data.model.PresenceStatus.ONLINE -> Triple(Color(0xFF22C55E), "Online", Color(0xFFDCFCE7))
        com.example.data.model.PresenceStatus.IN_MEETING -> Triple(Color(0xFFF59E0B), "In Meeting", Color(0xFFFEF3C7))
        com.example.data.model.PresenceStatus.OFFLINE -> Triple(Color(0xFF94A3B8), "Offline", Color(0xFFF1F5F9))
    }

    if (showLabel) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = bgContainer,
            modifier = modifier
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = labelText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = dotColor
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .size(dotSize)
                .clip(CircleShape)
                .background(dotColor)
                .border(1.5.dp, Color.White, CircleShape)
        )
    }
}

