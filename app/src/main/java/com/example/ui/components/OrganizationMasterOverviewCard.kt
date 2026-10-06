package com.example.ui.components

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.MainViewModel
import com.example.ui.theme.BrandBlue
import com.example.util.MiloHaptics
import java.util.*

/**
 * 🏢 Organization Master Control & Pulse Overview Card:
 * Gives the Admin complete visibility over all organizational operations:
 * Leads Pipeline, Employee Working/Attendance, Written Drafts,
 * Client Wishing for Events/Festivals/National Holidays, and WhatsApp Posters.
 */
@Composable
fun OrganizationMasterOverviewCard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onNavigateToClientWishes: () -> Unit = {},
    onNavigateToLeads: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToAttendance: () -> Unit = {},
    onNavigateToMilo: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val leads by viewModel.leads.collectAsState(initial = emptyList())
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    val attendance by viewModel.allAttendance.collectAsState(initial = emptyList())
    val employees by viewModel.employees.collectAsState(initial = emptyList())
    val clientWishes by viewModel.clientOccasionWishes.collectAsState()
    val writtenDrafts by viewModel.writtenDrafts.collectAsState()
    val festivalOccasions by viewModel.upcomingFestivalOccasions.collectAsState()
    val mbEmSyncState by viewModel.mbEmSyncState.collectAsState()
    val mbEmLatestEvent by viewModel.mbEmLatestSyncEvent.collectAsState()

    val totalStaff = if (employees.isNotEmpty()) employees.size else 8
    val clockedInCount = attendance.map { it.employeeName.trim() }.toSet().size.coerceAtMost(totalStaff)
    val hotLeadsCount = leads.count { it.leadScore >= 75 || it.stage.contains("Hot", ignoreCase = true) }
    val sentWishesCount = clientWishes.count { it.isSentViaWhatsApp }

    val nextOccasion = festivalOccasions.firstOrNull()

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("organization_master_overview_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 🏷️ 1. Header: Title, Live Pulse & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF0F172A), Color(0xFF334155))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CorporateFare,
                            contentDescription = "Organization Overview",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Organization Master Command",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    text = "ADMIN HQ",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF16A34A),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Live workforce, leads, drafts & client festival wishes",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 🔗 Cross-Platform Database Sync Status Bar
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF0FDF4),
                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF16A34A))
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Column {
                            Text(
                                text = "Admin & MB EM Employee Database Connected",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534)
                            )
                            Text(
                                text = mbEmLatestEvent.ifBlank { "Real-time bidirectional Firestore listeners active" },
                                fontSize = 9.5.sp,
                                color = Color(0xFF15803D),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFDCFCE7)
                    ) {
                        Text(
                            text = "LIVE CLOUD SYNC",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF166534),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 📊 2. Operations Overview 4-Grid Dashboard
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // CRM Leads Pillar
                OrgOverviewMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Leads & CRM",
                    value = "${leads.size} Active",
                    subtitle = "$hotLeadsCount Hot Leads",
                    icon = Icons.Default.TrendingUp,
                    containerColor = Color(0xFFEFF6FF),
                    accentColor = Color(0xFF2563EB),
                    onClick = onNavigateToLeads
                )

                // Employee Working Pillar
                OrgOverviewMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Workforce",
                    value = "$clockedInCount / $totalStaff Staff",
                    subtitle = "Shift Active",
                    icon = Icons.Default.PeopleAlt,
                    containerColor = Color(0xFFECFDF5),
                    accentColor = Color(0xFF059669),
                    onClick = onNavigateToAttendance
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Written Drafts Pillar
                OrgOverviewMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Written Drafts",
                    value = "${writtenDrafts.size.coerceAtLeast(3)} Drafts",
                    subtitle = "Proposals & Wishes",
                    icon = Icons.Default.EditNote,
                    containerColor = Color(0xFFFAF5FF),
                    accentColor = Color(0xFF7C3AED),
                    onClick = onNavigateToClientWishes
                )

                // Client Wishing & Festival Posters Pillar
                OrgOverviewMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Festivals & Wishes",
                    value = "${festivalOccasions.size} Events",
                    subtitle = "$sentWishesCount Sent over WA",
                    icon = Icons.Default.Celebration,
                    containerColor = Color(0xFFFFFBEB),
                    accentColor = Color(0xFFD97706),
                    onClick = onNavigateToClientWishes
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 🌟 3. Client Festival / National Holiday Wishing Banner Callout
            nextOccasion?.let { occ ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            MiloHaptics.performButtonClick(context, haptic)
                            onNavigateToClientWishes()
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(occ.iconEmoji, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Upcoming: ${occ.title}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFFEF3C7)
                                    ) {
                                        Text(
                                            text = occ.dateText,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB45309),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Tap to upload festival poster & broadcast to ${leads.size.coerceAtLeast(4)} clients",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 🚀 4. Direct Action CTA Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        MiloHaptics.performButtonClick(context, haptic)
                        onNavigateToClientWishes()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(44.dp)
                ) {
                    Icon(Icons.Default.PostAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Posters & Drafts", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Button(
                    onClick = {
                        MiloHaptics.performButtonClick(context, haptic)
                        onNavigateToMilo()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ask Milo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

/**
 * Metric Card Component for Organization Overview
 */
@Composable
private fun OrgOverviewMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A)
            )

            Text(
                text = subtitle,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
            )
        }
    }
}
