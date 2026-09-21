package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LeadEntity
import com.example.ui.components.AppHeader
import com.example.ui.components.StandardScreenHeader
import com.example.ui.theme.*
import com.example.util.WhatsAppHelper

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
    val tabs = listOf("Overview", "Activity", "Notes", "Files")
    var newNoteText by remember { mutableStateOf("") }
    var isAddingNote by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            StandardScreenHeader(
                viewModel = viewModel,
                subMenuTitle = lead?.name ?: "Lead Details",
                subMenuSubtitle = "${lead?.company ?: "Client"} · ${lead?.stage ?: "New"}",
                onBack = onBack,
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.size(38.dp)
                    ) {
                        IconButton(onClick = {
                            if (lead != null) {
                                viewModel.sendCompanyProfileBrochure(
                                    context = context,
                                    recipientName = lead.name,
                                    recipientPhone = lead.phone,
                                    companyName = lead.company
                                )
                            }
                        }) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Share Profile",
                                tint = ElectricBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            )
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        if (lead != null) {
            val (avatarBg, avatarText) = getAvatarColor(lead.name)
            val (stageBg, stageText) = getStageBadgeColors(lead.stage)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Card 1: Lead Hero Header ─────────────────────────
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Avatar + Name + Stage Pill
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = avatarBg,
                                        modifier = Modifier.size(54.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = getInitials(lead.name),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp,
                                                color = avatarText
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = lead.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = lead.company.ifBlank { "Independent Prospect" },
                                            fontSize = 13.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = stageBg,
                                    border = BorderStroke(1.dp, stageText.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = lead.stage,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = stageText,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            // Contact Channels with Direct Tap Affordances
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Phone Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFF8FAFC))
                                        .clickable {
                                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${lead.phone.replace(" ", "")}"))
                                            try { context.startActivity(dialIntent) } catch (_: Exception) {}
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Phone, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(lead.phone, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                                    }
                                    Text("Tap to call", fontSize = 11.sp, color = ElectricBlue, fontWeight = FontWeight.Medium)
                                }

                                // Email Row (if available)
                                if (lead.email.isNotBlank()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFF8FAFC))
                                            .clickable {
                                                val mailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${lead.email}"))
                                                try { context.startActivity(mailIntent) } catch (_: Exception) {}
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Email, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(lead.email, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                                        }
                                        Text("Tap to email", fontSize = 11.sp, color = ElectricBlue, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Card 2: Quick Action Grid (Call, WhatsApp, Email, Brochure) ───
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Call
                        LeadDetailActionButton(
                            icon = Icons.Default.Phone,
                            label = "Direct Call",
                            bgColor = Color(0xFFDCFCE7),
                            tintColor = Color(0xFF15803D),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${lead.phone.replace(" ", "")}"))
                                try { context.startActivity(intent) } catch (_: Exception) {}
                                viewModel.addCallLog(lead.name, lead.phone, "Outgoing", "Initiated")
                            }
                        )
                        // WhatsApp
                        LeadDetailActionButton(
                            icon = Icons.Default.Chat,
                            label = "WhatsApp",
                            bgColor = Color(0xFFD1FAE5),
                            tintColor = Color(0xFF059669),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                WhatsAppHelper.sendWhatsAppMessage(
                                    context = context,
                                    phoneNumber = lead.phone,
                                    message = "Hello ${lead.name}, connecting with you from Making Brands.",
                                    showSuccessToast = true
                                )
                            }
                        )
                        // Send Brochure
                        LeadDetailActionButton(
                            icon = Icons.Default.Share,
                            label = "Send Profile",
                            bgColor = Color(0xFFEFF6FF),
                            tintColor = Color(0xFF2563EB),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.sendCompanyProfileBrochure(
                                    context = context,
                                    recipientName = lead.name,
                                    recipientPhone = lead.phone,
                                    companyName = lead.company
                                )
                            }
                        )
                    }
                }

                // ── Card 3: Deal Metrics & Pipeline Health ─────────────
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Deal Financials & Pipeline Scope", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Potential Value
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Deal Value", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(lead.potentialValue, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                                    }
                                }

                                // Lead Score
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Quality Score", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                if (lead.leadScore >= 80) Icons.Default.LocalFireDepartment else Icons.Default.TrendingUp,
                                                contentDescription = null,
                                                tint = if (lead.leadScore >= 80) Color(0xFFEA580C) else ElectricBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("${lead.leadScore}%", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                                        }
                                    }
                                }
                            }

                            // Requirement Scope Box
                            if (lead.requirement.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Client Requirement", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(lead.requirement, fontSize = 13.sp, color = Color(0xFF334155), lineHeight = 18.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Subview Tabs & Content ─────────────────────────────
                item {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.White,
                        contentColor = ElectricBlue,
                        modifier = Modifier.clip(RoundedCornerShape(14.dp))
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        title,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                }
                            )
                        }
                    }
                }

                // Tab Content Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            when (selectedTab) {
                                0 -> { // Overview
                                    Text("Lead Origin & Assignment", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Source Platform", fontSize = 13.sp, color = Color(0xFF64748B))
                                        Text(lead.source, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    }
                                    HorizontalDivider(color = Color(0xFFF1F5F9))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Assigned Representative", fontSize = 13.sp, color = Color(0xFF64748B))
                                        Text(lead.assignedTo.ifBlank { "Unassigned" }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    }
                                    HorizontalDivider(color = Color(0xFFF1F5F9))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Lead Record ID", fontSize = 13.sp, color = Color(0xFF64748B))
                                        Text("#LEAD-${lead.id}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ElectricBlue)
                                    }
                                }
                                1 -> { // Activity
                                    Text("Recent Engagement Activity", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(shape = CircleShape, color = Color(0xFFDCFCE7), modifier = Modifier.size(24.dp)) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.PhoneCallback, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(13.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text("Lead Created in CRM", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                                Text("Source: ${lead.source} · Stage: ${lead.stage}", fontSize = 11.sp, color = Color(0xFF64748B))
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(shape = CircleShape, color = Color(0xFFEFF6FF), modifier = Modifier.size(24.dp)) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(13.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text("Company Profile Dispatch Ready", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                                Text("Automated brochure available for WhatsApp delivery", fontSize = 11.sp, color = Color(0xFF64748B))
                                            }
                                        }
                                    }
                                }
                                2 -> { // Notes
                                    Text("Client Notes & Records", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                    Text(
                                        lead.notes.ifBlank { "No notes added yet for this client." },
                                        fontSize = 13.sp,
                                        color = Color(0xFF475569),
                                        lineHeight = 18.sp
                                    )
                                }
                                3 -> { // Files
                                    Text("Attached Documents & Media", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            viewModel.sendCompanyProfileBrochure(
                                                context = context,
                                                recipientName = lead.name,
                                                recipientPhone = lead.phone,
                                                companyName = lead.company
                                            )
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(24.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Making_Brands_Company_Profile.pdf", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                                Text("2.4 MB · Ready to share on WhatsApp", fontSize = 11.sp, color = Color(0xFF64748B))
                                            }
                                            Icon(Icons.Default.Share, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                                        }
                                    }
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
fun LeadDetailActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    bgColor: Color,
    tintColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(1.dp, tintColor.copy(alpha = 0.3f)),
        modifier = modifier
            .height(64.dp)
            .clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(icon, contentDescription = label, tint = tintColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = tintColor)
        }
    }
}
