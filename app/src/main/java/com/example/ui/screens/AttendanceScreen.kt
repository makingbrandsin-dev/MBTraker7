package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppHeader
import com.example.ui.components.CrmTasksAttendanceSwitcher
import com.example.ui.components.formatLiveSeconds
import com.example.ui.theme.*

@Composable
fun AttendanceScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToCrm: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToProfile: (() -> Unit)? = null
) {
    val attendance by viewModel.latestAttendance.collectAsState()
    val liveSeconds by viewModel.liveActiveDurationSeconds.collectAsState()
    val isWorking = attendance?.isWorking ?: false

    // Pulsing circle animation when working
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseBorder by infiniteTransition.animateFloat(
        initialValue = 2f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseBorder"
    )

    Scaffold(
        topBar = {
            AppHeader(
                title = "Attendance",
                onBack = onBack,
                onNavigateToProfile = onNavigateToProfile
            )
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CRM - Tasks - Attendance Hub Switcher
            item {
                CrmTasksAttendanceSwitcher(
                    selectedTab = "attendance",
                    onNavigateToCrm = onNavigateToCrm,
                    onNavigateToTasks = onNavigateToTasks,
                    onNavigateToAttendance = { },
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Big Circular Gauge
            item {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(
                            width = if (isWorking) pulseBorder.dp else 4.dp,
                            color = if (isWorking) StatusGreen else StatusOrange,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = if (isWorking) StatusGreenBg else StatusOrangeBg,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isWorking) StatusGreen else StatusOrange)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (isWorking) "Working" else "Checked Out",
                                    color = if (isWorking) StatusGreen else StatusOrange,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Text(
                            text = if (isWorking) (attendance?.checkInTime ?: "--:--") else (attendance?.checkOutTime ?: "--:--"),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = attendance?.date ?: java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date()),
                            fontSize = 13.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isWorking) formatLiveSeconds(liveSeconds) else "00:00:00",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandBlue
                        )
                    }
                }
            }

            // Punch CTA Button
            item {
                Button(
                    onClick = { viewModel.toggleCheckInCheckOut() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F172A),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        if (isWorking) Icons.Default.Logout else Icons.Default.Login,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isWorking) "Check Out" else "Check In Now",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Attendance Timeline Breakdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Today's Attendance",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        AttendanceTimelineRow(
                            icon = Icons.Default.Login,
                            label = "Check In",
                            time = attendance?.checkInTime ?: "-- : --",
                            iconBg = StatusGreenBg,
                            iconTint = StatusGreen
                        )
                        Divider(modifier = Modifier.padding(start = 36.dp, top = 8.dp, bottom = 8.dp), color = BorderLight)

                        AttendanceTimelineRow(
                            icon = Icons.Default.FreeBreakfast,
                            label = "Break Start",
                            time = if (isWorking) "01:00 PM" else "-- : --",
                            iconBg = StatusOrangeBg,
                            iconTint = StatusOrange
                        )
                        Divider(modifier = Modifier.padding(start = 36.dp, top = 8.dp, bottom = 8.dp), color = BorderLight)

                        AttendanceTimelineRow(
                            icon = Icons.Default.Work,
                            label = "Break End",
                            time = if (isWorking) "01:30 PM" else "-- : --",
                            iconBg = StatusBlueBg,
                            iconTint = StatusBlue
                        )
                        Divider(modifier = Modifier.padding(start = 36.dp, top = 8.dp, bottom = 8.dp), color = BorderLight)

                        AttendanceTimelineRow(
                            icon = Icons.Default.Logout,
                            label = "Check Out",
                            time = attendance?.checkOutTime ?: "-- : --",
                            iconBg = StatusRedBg,
                            iconTint = StatusRed
                        )
                    }
                }
            }

            // Stat Metrics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Total Working Time", fontSize = 12.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${liveSeconds / 3600}h ${(liveSeconds % 3600) / 60}m",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Overtime", fontSize = 12.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "0h 00m",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceTimelineRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    time: String,
    iconBg: Color,
    iconTint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = iconBg,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(label, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
        }
        Text(time, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
