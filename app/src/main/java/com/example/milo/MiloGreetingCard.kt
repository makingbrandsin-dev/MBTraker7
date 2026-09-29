package com.example.milo

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.milo.MiloState
import com.example.ui.components.liftOnPress
import com.example.ui.screens.MainViewModel
import com.example.ui.theme.*
import com.example.util.MiloHaptics

/**
 * Proactive Milo Smart Assistant Greeting Card.
 * Shown prominently at the top of the Employee and Manager Dashboards whenever the user opens the app.
 * Features:
 * - Dynamic time-of-day greeting ("Good Morning", "Good Afternoon", "Good Evening")
 * - Proactive alerts on pending works & missed leads
 * - 1-tap quick actions: Ask Milo, Auto-Align Leads, View Tasks, View Funnel
 * - Interactive hover lift-up animations on all buttons
 */
@Composable
fun MiloGreetingCard(
    viewModel: MainViewModel,
    onOpenAskMilo: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToLeads: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val employeeName by viewModel.currentEmployeeName.collectAsState()
    val allLeads by viewModel.leads.collectAsState()
    val allTasks by viewModel.tasks.collectAsState()
    val miloState by viewModel.miloViewModel.state.collectAsState()

    val (greetingTitle, greetingSubtitle) = remember(employeeName) {
        getTimeBasedGreeting(employeeName)
    }

    val pendingCount = remember(allTasks) { allTasks.count { !it.isCompleted } }
    val missedLeadsCount = remember(allLeads) {
        allLeads.count {
            !it.stage.equals("Won", true) && !it.stage.equals("Lost", true) &&
                    (it.leadScore < 60 || it.nextFollowUp.contains("Today") || it.stage.equals("New", true))
        }
    }
    val activeLeadsCount = remember(allLeads) { allLeads.size }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFEFF6FF),
                            Color.White
                        )
                    )
                )
                .padding(16.dp)
        ) {
            // Top Row: Mascot Avatar + Greeting Speech Bubble
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Milo Character
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(2.dp, BrandBlue),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .size(58.dp)
                        .liftOnPress(elevationLift = 8.dp, translateY = (-4).dp)
                        .clickable {
                            MiloHaptics.performButtonTap(context)
                            onOpenAskMilo()
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        MiloCharacter(
                            state = miloState,
                            size = 50.dp,
                            showStateBadge = false
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Speech & Dynamic Greeting
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = greetingTitle,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = greetingSubtitle,
                        fontSize = 12.sp,
                        color = BrandBlue,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Proactive Snapshot Badge: Pending Works & Missed Leads
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (missedLeadsCount > 0) StatusRed else StatusGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (missedLeadsCount > 0) "🚨 $missedLeadsCount Missed Leads • $pendingCount Pending Works"
                            else "✨ $pendingCount Pending Works • $activeLeadsCount Funnel Deals",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (missedLeadsCount > 0) Color(0xFF991B1B) else Color(0xFF1E3A8A)
                        )
                    }

                    Text(
                        text = "Live Sync",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Interactive Action Buttons with Lift-Up Effect
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Ask Milo Button
                Button(
                    onClick = {
                        MiloHaptics.performButtonTap(context)
                        onOpenAskMilo()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .liftOnPress()
                ) {
                    Icon(Icons.Default.ChatBubble, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ask Milo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // 2. Auto-Align Button
                OutlinedButton(
                    onClick = {
                        MiloHaptics.performButtonTap(context)
                        onOpenAskMilo()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0284C7)),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .liftOnPress()
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF0284C7))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto-Align", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // 3. Pending Tasks Button
                FilledTonalButton(
                    onClick = {
                        MiloHaptics.performButtonTap(context)
                        onNavigateToTasks()
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFFEDE9FE),
                        contentColor = Color(0xFF7C3AED)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .liftOnPress()
                ) {
                    Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tasks ($pendingCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
