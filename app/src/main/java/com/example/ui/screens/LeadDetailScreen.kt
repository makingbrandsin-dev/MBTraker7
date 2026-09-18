package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LeadEntity
import com.example.ui.components.AppHeader
import com.example.ui.theme.*

@Composable
fun LeadDetailScreen(
    leadId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val leads by viewModel.leads.collectAsState()
    val lead = leads.find { it.id == leadId } ?: leads.firstOrNull()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Activity", "Notes", "Files", "More")

    Scaffold(
        topBar = {
            AppHeader(
                title = "Lead Detail",
                onBack = onBack,
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextPrimary)
                    }
                }
            )
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        if (lead != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Customer Header Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = BrandBlue.copy(alpha = 0.15f),
                                        modifier = Modifier.size(54.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(32.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(lead.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                                        Text(lead.company, fontSize = 13.sp, color = TextSecondary)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = StatusOrange, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Lead Score ${lead.leadScore}%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StatusOrange)
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = StatusGreenBg
                                ) {
                                    Text(
                                        lead.stage,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = BorderLight)
                            Spacer(modifier = Modifier.height(12.dp))

                            // Contact info
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(lead.phone, fontSize = 13.sp, color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Email, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(lead.email, fontSize = 13.sp, color = TextPrimary)
                            }
                        }
                    }
                }

                // Lead Key Metrics Matrix
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Requirement", fontSize = 12.sp, color = TextSecondary)
                            Text(lead.requirement, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            Divider(color = BorderLight)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Potential Value", fontSize = 12.sp, color = TextSecondary)
                                    Text(lead.potentialValue, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = BrandBlue)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Assigned To", fontSize = 12.sp, color = TextSecondary)
                                    Text(lead.assignedTo, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                }
                            }
                        }
                    }
                }

                // Action Bar (Call, WhatsApp, Email, Add Note)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LeadActionButton(
                            icon = Icons.Default.Phone,
                            label = "Call",
                            bgColor = Color(0xFFDCFCE7),
                            tintColor = Color(0xFF15803D),
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${lead.phone}"))
                                try { context.startActivity(intent) } catch (_: Exception) {}
                                viewModel.addCallLog(lead.name, lead.phone, "Outgoing", "Initiated")
                            }
                        )
                        LeadActionButton(
                            icon = Icons.Default.Chat,
                            label = "WhatsApp",
                            bgColor = Color(0xFFD1FAE5),
                            tintColor = Color(0xFF059669),
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${lead.phone.replace(" ", "")}"))
                                try { context.startActivity(intent) } catch (_: Exception) {}
                            }
                        )
                        LeadActionButton(
                            icon = Icons.Default.Email,
                            label = "Email",
                            bgColor = Color(0xFFDBEAFE),
                            tintColor = Color(0xFF1D4ED8),
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${lead.email}"))
                                try { context.startActivity(intent) } catch (_: Exception) {}
                            }
                        )
                        LeadActionButton(
                            icon = Icons.Default.NoteAdd,
                            label = "Add Note",
                            bgColor = Color(0xFFFEF3C7),
                            tintColor = Color(0xFFB45309),
                            onClick = {}
                        )
                    }
                }

                // Subview Tabs
                item {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.White,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }

                // Tab Content preview
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            when (selectedTab) {
                                0 -> {
                                    Text("Recent Activity", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("• Call scheduled for 4:30 PM with Arjun Mehta", fontSize = 13.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("• Initial proposal sent via email on 15 Sep", fontSize = 13.sp, color = TextSecondary)
                                }
                                1 -> {
                                    Text("Client Notes", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(lead.notes, fontSize = 13.sp, color = TextSecondary)
                                }
                                2 -> {
                                    Text("Attached Files", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("📄 Technical_Scope_Document.pdf (1.2 MB)", fontSize = 13.sp, color = BrandBlue)
                                }
                                else -> {
                                    Text("Metadata & Source", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Source: Direct Web Inbound Lead", fontSize = 13.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LeadActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    bgColor: Color,
    tintColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = bgColor,
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = tintColor, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}
