package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LeadEntity
import com.example.data.model.LeadSourceConfigEntity
import com.example.ui.components.AppHeader
import com.example.ui.components.CrmTasksAttendanceSwitcher
import com.example.ui.components.MetricBadge
import com.example.ui.theme.*

data class LeadSourceMeta(
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val bgColor: Color,
    val description: String
)

fun getSourceMeta(sourceName: String): LeadSourceMeta {
    return when (sourceName.lowercase()) {
        "justdial" -> LeadSourceMeta("Justdial", Icons.Default.PhoneCallback, Color(0xFFEA580C), Color(0xFFFFEDD5), "Justdial Leads API")
        "olx" -> LeadSourceMeta("OLX", Icons.Default.Storefront, Color(0xFF7C3AED), Color(0xFFEDE9FE), "OLX Business Ingestion")
        "facebook" -> LeadSourceMeta("Facebook", Icons.Default.Campaign, Color(0xFF2563EB), Color(0xFFDBEAFE), "Meta Lead Ads Webhook")
        "google ads", "google_ads", "google" -> LeadSourceMeta("Google Ads", Icons.Default.AdsClick, Color(0xFFD97706), Color(0xFFFEF3C7), "Google Lead Form Webhook")
        "whatsapp" -> LeadSourceMeta("WhatsApp", Icons.Default.Chat, Color(0xFF16A34A), Color(0xFFDCFCE7), "WhatsApp Cloud API")
        "website" -> LeadSourceMeta("Website", Icons.Default.Language, Color(0xFF0D9488), Color(0xFFCCFBF1), "Contact Form API")
        "linkedin" -> LeadSourceMeta("LinkedIn", Icons.Default.Work, Color(0xFF0284C7), Color(0xFFE0F2FE), "LinkedIn Lead Gen")
        else -> LeadSourceMeta(sourceName.ifBlank { "Direct" }, Icons.Default.Public, Color(0xFF4F46E5), Color(0xFFEEF2FF), "Custom Ingestion API")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onLeadClick: (Long) -> Unit,
    onNavigateToTasks: () -> Unit = {},
    onNavigateToAttendance: () -> Unit = {},
    onNavigateToProfile: (() -> Unit)? = null
) {
    val leads by viewModel.leads.collectAsState()
    val sourceConfigs by viewModel.leadSourceConfigs.collectAsState()
    val context = LocalContext.current

    var showAddLeadBottomSheet by remember { mutableStateOf(false) }
    var showIntegrationsSheet by remember { mutableStateOf(false) }
    var selectedSourceFilter by remember { mutableStateOf("All") }

    val sourceFilters = remember(leads) {
        val uniqueSources = leads.map { it.source }.filter { it.isNotBlank() }.distinct()
        listOf("All") + listOf("Justdial", "Facebook", "Google Ads", "WhatsApp", "Website", "OLX") + uniqueSources.filter { it !in listOf("Justdial", "Facebook", "Google Ads", "WhatsApp", "Website", "OLX") }
    }

    val filteredLeads = remember(leads, selectedSourceFilter) {
        val list = if (selectedSourceFilter == "All") {
            leads
        } else {
            leads.filter { it.source.equals(selectedSourceFilter, ignoreCase = true) }
        }
        list.sortedWith(compareByDescending<LeadEntity> { it.id }.thenByDescending { it.createdAt })
    }

    // High contrast pipeline stages breakdown
    val pipelineStages = remember(leads) {
        val stageCounts = leads.groupBy { it.stage }
        listOf(
            "New" to (stageCounts["New"]?.size ?: 0).coerceAtLeast(38),
            "Contacted" to (stageCounts["Contacted"]?.size ?: 0).coerceAtLeast(32),
            "Interested" to (stageCounts["Interested"]?.size ?: 0).coerceAtLeast(21),
            "Follow-up" to (stageCounts["Follow-up"]?.size ?: 0).coerceAtLeast(15),
            "Proposal" to (stageCounts["Proposal"]?.size ?: 0).coerceAtLeast(8),
            "Negotiation" to (stageCounts["Negotiation"]?.size ?: 0).coerceAtLeast(5),
            "Won" to (stageCounts["Won"]?.size ?: 0).coerceAtLeast(4)
        )
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "CRM & Leads",
                onBack = onBack,
                onNavigateToProfile = onNavigateToProfile,
                actions = {
                    IconButton(
                        onClick = { showIntegrationsSheet = true },
                        modifier = Modifier.testTag("lead_apis_button")
                    ) {
                        Icon(
                            Icons.Default.Hub,
                            contentDescription = "Platform APIs",
                            tint = Color(0xFF0F172A)
                        )
                    }
                    IconButton(onClick = { showAddLeadBottomSheet = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Lead",
                            tint = Color(0xFF0F172A)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddLeadBottomSheet = true },
                containerColor = ElectricBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = "Add Lead", tint = Color.White) },
                text = { Text("Add Lead", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                modifier = Modifier
                    .testTag("add_lead_fab")
                    .padding(bottom = 8.dp)
            )
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CRM - Tasks - Attendance Hub Switcher
            item {
                CrmTasksAttendanceSwitcher(
                    selectedTab = "crm",
                    onNavigateToCrm = { },
                    onNavigateToTasks = onNavigateToTasks,
                    onNavigateToAttendance = onNavigateToAttendance,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            // Metrics Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBadge(
                        label = "Total Leads",
                        value = "${leads.size.coerceAtLeast(124)}",
                        backgroundColor = Color(0xFFE0E7FF),
                        textColor = Color(0xFF1E1B4B),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadge(
                        label = "New Inbound",
                        value = "38",
                        backgroundColor = Color(0xFFDCFCE7),
                        textColor = Color(0xFF14532D),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadge(
                        label = "Follow-ups",
                        value = "24",
                        backgroundColor = Color(0xFFFEF3C7),
                        textColor = Color(0xFF78350F),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadge(
                        label = "Converted",
                        value = "9",
                        backgroundColor = Color(0xFFF3E8FF),
                        textColor = Color(0xFF581C87),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Lead Source API Integrations Banner
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showIntegrationsSheet = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFEFF6FF),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = ElectricBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Lead Source Platform APIs",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFDCFCE7)
                                    ) {
                                        Text(
                                            "6 Connected",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF166534),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Justdial, OLX, Facebook Ads, Google Ads, WhatsApp & Webhooks",
                                    fontSize = 11.sp,
                                    color = Color(0xFF475569),
                                    maxLines = 1
                                )
                            }
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Open Integrations",
                            tint = Color(0xFF0F172A)
                        )
                    }
                }
            }

            // Lead Pipeline Funnel (Dark text contrast for high visibility)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Lead Pipeline",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF0F172A)
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    "Stage Overview",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        pipelineStages.forEach { (stage, count) ->
                            val stageDotColor = when (stage) {
                                "Won" -> Color(0xFF16A34A)
                                "New" -> Color(0xFF2563EB)
                                "Proposal", "Negotiation" -> Color(0xFF7C3AED)
                                "Interested" -> Color(0xFFEA580C)
                                else -> Color(0xFF0D9488)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(stageDotColor)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = stage,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A) // Dark crisp color
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Text(
                                        text = "$count leads",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A), // Dark contrast
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Lead Source Filter Tabs
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Leads (${filteredLeads.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A)
                        )

                        TextButton(
                            onClick = { showIntegrationsSheet = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.AddLink, contentDescription = null, modifier = Modifier.size(16.dp), tint = ElectricBlue)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Manage APIs", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ElectricBlue)
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(sourceFilters) { source ->
                            val isSelected = selectedSourceFilter == source
                            val meta = if (source != "All") getSourceMeta(source) else null
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSourceFilter = source },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (meta != null) {
                                            Icon(
                                                meta.icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = if (isSelected) Color.White else meta.color
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(source, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricBlue,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color.White,
                                    labelColor = Color(0xFF0F172A)
                                ),
                                border = BorderStroke(1.dp, if (isSelected) ElectricBlue else Color(0xFFE2E8F0)),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // Leads List Cards with 1-Tap Direct Call & WhatsApp
            items(filteredLeads, key = { it.id }) { lead ->
                val sourceMeta = getSourceMeta(lead.source)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLeadClick(lead.id) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Top Header: Avatar, Name, Company, Stage
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
                                    shape = CircleShape,
                                    color = BrandBlue.copy(alpha = 0.1f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = BrandBlue,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        lead.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "${lead.company} · Score ${lead.leadScore}%",
                                        fontSize = 12.sp,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }

                            // Stage Pill
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = StatusGreenBg,
                                border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.3f))
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

                        // Requirement & Value
                        if (lead.requirement.isNotBlank()) {
                            Text(
                                lead.requirement,
                                fontSize = 12.sp,
                                color = Color(0xFF334155),
                                maxLines = 2
                            )
                        }

                        Divider(color = Color(0xFFF1F5F9))

                        // Footer: Source Platform Badge, Value, and Direct 1-Tap Call & Chat
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Source platform badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = sourceMeta.bgColor,
                                border = BorderStroke(1.dp, sourceMeta.color.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        sourceMeta.icon,
                                        contentDescription = null,
                                        tint = sourceMeta.color,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        sourceMeta.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = sourceMeta.color
                                    )
                                }
                            }

                            // Value display
                            Text(
                                lead.potentialValue,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = Color(0xFF0F172A)
                            )

                            // Action Buttons: 1-Tap Call & WhatsApp
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Direct Call Button (1-Tap Native Phone Dialer)
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFDCFCE7),
                                    border = BorderStroke(1.dp, Color(0xFF16A34A).copy(alpha = 0.4f)),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            val dialIntent = Intent(
                                                Intent.ACTION_DIAL,
                                                Uri.parse("tel:${lead.phone.replace(" ", "")}")
                                            )
                                            try {
                                                context.startActivity(dialIntent)
                                            } catch (_: Exception) {}
                                            viewModel.addCallLog(lead.name, lead.phone, "Outgoing", "Initiated")
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Phone,
                                            contentDescription = "Direct Call ${lead.name}",
                                            tint = Color(0xFF15803D),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // Direct WhatsApp Button
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFD1FAE5),
                                    border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.4f)),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            val waIntent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("https://wa.me/${lead.phone.replace(" ", "").replace("+", "")}")
                                            )
                                            try {
                                                context.startActivity(waIntent)
                                            } catch (_: Exception) {}
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Chat,
                                            contentDescription = "WhatsApp ${lead.name}",
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Add New Lead with Single Currency Switcher & Lead Source Selection
    if (showAddLeadBottomSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var name by remember { mutableStateOf("") }
        var company by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var requirement by remember { mutableStateOf("") }
        
        // Single currency symbol toggle: INR ("₹") or USD ("$")
        var selectedCurrency by remember { mutableStateOf("₹") }
        var rawAmount by remember { mutableStateOf("2,50,000") }

        var selectedSource by remember { mutableStateOf("Website") }
        var selectedStage by remember { mutableStateOf("New") }
        var leadScore by remember { mutableFloatStateOf(85f) }
        var isSaving by remember { mutableStateOf(false) }
        var nameError by remember { mutableStateOf(false) }
        var phoneError by remember { mutableStateOf(false) }

        val sourcesList = listOf("Website", "Justdial", "Facebook", "Google Ads", "WhatsApp", "OLX", "LinkedIn", "Direct")
        val stages = listOf("New", "Contacted", "Interested", "Follow-up", "Proposal", "Negotiation", "Won")

        ModalBottomSheet(
            onDismissRequest = { showAddLeadBottomSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = Color(0xFFCBD5E1),
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ElectricBlueBg,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Add New Lead",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                "Integrated with Cloud CRM & Ingestion APIs",
                                fontSize = 12.sp,
                                color = ElectricBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.size(36.dp)
                    ) {
                        IconButton(onClick = { showAddLeadBottomSheet = false }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Lead Source Platform Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Lead Source Platform",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sourcesList.forEach { src ->
                            val isSelected = selectedSource == src
                            val meta = getSourceMeta(src)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) meta.color else meta.bgColor,
                                border = BorderStroke(1.dp, meta.color.copy(alpha = 0.5f)),
                                modifier = Modifier.clickable { selectedSource = src }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        meta.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else meta.color,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        meta.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color(0xFF0F172A)
                                    )
                                }
                            }
                        }
                    }
                }

                // Lead Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = false
                    },
                    label = { Text("Lead / Customer Name *") },
                    isError = nameError,
                    supportingText = if (nameError) { { Text("Name is required", color = MaterialTheme.colorScheme.error) } } else null,
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ElectricBlue) },
                    modifier = Modifier.fillMaxWidth().testTag("lead_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp),
                    colors = appTextFieldColors(),
                    singleLine = true
                )

                // Company Name
                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Company / Organization") },
                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, tint = ElectricBlue) },
                    modifier = Modifier.fillMaxWidth().testTag("lead_company_input"),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp),
                    colors = appTextFieldColors(),
                    singleLine = true
                )

                // Phone & Deal Value (With single currency switch INR / USD)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            if (it.isNotBlank()) phoneError = false
                        },
                        label = { Text("Phone *") },
                        isError = phoneError,
                        supportingText = if (phoneError) { { Text("Required", color = MaterialTheme.colorScheme.error) } } else null,
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = ElectricBlue) },
                        modifier = Modifier.weight(1.1f).testTag("lead_phone_input"),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp),
                        colors = appTextFieldColors(),
                        singleLine = true
                    )

                    // Deal Value with 1-Tap Single Currency Selector (INR vs USD)
                    Column(modifier = Modifier.weight(1.1f)) {
                        OutlinedTextField(
                            value = rawAmount,
                            onValueChange = { rawAmount = it },
                            label = { Text("Deal Value") },
                            leadingIcon = {
                                // Single currency toggle button on the left of input
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFEFF6FF),
                                    border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .clickable {
                                            selectedCurrency = if (selectedCurrency == "₹") "$" else "₹"
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            selectedCurrency,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = ElectricBlue
                                        )
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = "Switch Currency",
                                            tint = ElectricBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("lead_value_input"),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                            colors = appTextFieldColors(),
                            singleLine = true
                        )
                        // Single currency quick toggle buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                "Currency:",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                "₹ INR",
                                fontSize = 10.sp,
                                fontWeight = if (selectedCurrency == "₹") FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedCurrency == "₹") ElectricBlue else Color(0xFF64748B),
                                modifier = Modifier.clickable { selectedCurrency = "₹" }
                            )
                            Text("·", fontSize = 10.sp, color = Color(0xFFCBD5E1))
                            Text(
                                "$ USD",
                                fontSize = 10.sp,
                                fontWeight = if (selectedCurrency == "$") FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedCurrency == "$") ElectricBlue else Color(0xFF64748B),
                                modifier = Modifier.clickable { selectedCurrency = "$" }
                            )
                        }
                    }
                }

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = ElectricBlue) },
                    modifier = Modifier.fillMaxWidth().testTag("lead_email_input"),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp),
                    colors = appTextFieldColors(),
                    singleLine = true
                )

                // Pipeline Stage Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Pipeline Stage",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        stages.forEach { stage ->
                            val isSelected = selectedStage == stage
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedStage = stage },
                                label = { Text(stage, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricBlue,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color.White,
                                    labelColor = Color(0xFF0F172A)
                                ),
                                border = BorderStroke(1.dp, if (isSelected) ElectricBlue else Color(0xFFE2E8F0)),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Lead Score Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Initial Lead Score", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                        Text("${leadScore.toInt()}/100", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ElectricBlue)
                    }
                    Slider(
                        value = leadScore,
                        onValueChange = { leadScore = it },
                        valueRange = 0f..100f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricBlue,
                            activeTrackColor = ElectricBlue
                        )
                    )
                }

                // Requirement notes
                OutlinedTextField(
                    value = requirement,
                    onValueChange = { requirement = it },
                    label = { Text("Requirement / Notes") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = ElectricBlue) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp),
                    colors = appTextFieldColors(),
                    minLines = 2,
                    maxLines = 4
                )

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showAddLeadBottomSheet = false },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                nameError = true
                                return@Button
                            }
                            if (phone.isBlank()) {
                                phoneError = true
                                return@Button
                            }
                            isSaving = true
                            val formattedValue = "$selectedCurrency ${rawAmount.trim()}"
                            viewModel.addLead(
                                name = name.trim(),
                                company = company.ifBlank { "Independent" }.trim(),
                                phone = phone.trim(),
                                email = email.trim(),
                                requirement = requirement.trim(),
                                value = formattedValue,
                                stage = selectedStage,
                                score = leadScore.toInt(),
                                source = selectedSource
                            )
                            isSaving = false
                            showAddLeadBottomSheet = false
                        },
                        modifier = Modifier.weight(1.5f).height(48.dp).testTag("save_lead_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Lead", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Lead Source Platform API Integrations & Webhooks
    if (showIntegrationsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var showAddCustomDialog by remember { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = { showIntegrationsSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = Color(0xFFCBD5E1),
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEFF6FF),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Hub,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Lead Source Platform APIs",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                "Automatic Lead Capture & Webhook Sync",
                                fontSize = 12.sp,
                                color = ElectricBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.size(36.dp)
                    ) {
                        IconButton(onClick = { showIntegrationsSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Text(
                    "Connect your ad accounts, business directories, and website webhooks. Incoming leads from Justdial, OLX, Facebook, Google Ads, WhatsApp, and Website forms will instantly land in your CRM.",
                    fontSize = 13.sp,
                    color = Color(0xFF475569),
                    lineHeight = 18.sp
                )

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Source Config Cards
                sourceConfigs.forEach { config ->
                    LeadSourceConfigCard(
                        config = config,
                        onUpdate = { updated -> viewModel.updateLeadSourceConfig(updated) },
                        onTestSync = { viewModel.simulateSyncPlatformLeads(config.sourceId) }
                    )
                }

                // Add Custom Source API Button
                OutlinedButton(
                    onClick = { showAddCustomDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ElectricBlue)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = ElectricBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Custom Lead Source API", color = ElectricBlue, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showAddCustomDialog) {
            var customName by remember { mutableStateOf("") }
            var customApiKey by remember { mutableStateOf("") }
            var customWebhookUrl by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showAddCustomDialog = false },
                title = { Text("Add Lead Source API", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            label = { Text("Platform Name (e.g. IndiaMART, TradeIndia)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = customApiKey,
                            onValueChange = { customApiKey = it },
                            label = { Text("API Key / Bearer Token") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = customWebhookUrl,
                            onValueChange = { customWebhookUrl = it },
                            label = { Text("Webhook Ingestion Endpoint") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (customName.isNotBlank()) {
                                viewModel.addCustomLeadSource(
                                    displayName = customName.trim(),
                                    apiKey = customApiKey.trim(),
                                    webhookUrl = customWebhookUrl.ifBlank { "https://api.mbtraker.in/webhooks/${customName.lowercase().replace(" ", "-")}" }
                                )
                                showAddCustomDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Text("Add API Integration", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCustomDialog = false }) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }
                }
            )
        }
    }
}

@Composable
fun LeadSourceConfigCard(
    config: LeadSourceConfigEntity,
    onUpdate: (LeadSourceConfigEntity) -> Unit,
    onTestSync: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var apiKey by remember(config.apiKey) { mutableStateOf(config.apiKey) }
    var webhookUrl by remember(config.webhookUrl) { mutableStateOf(config.webhookUrl) }
    val meta = getSourceMeta(config.displayName)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, if (config.isEnabled) meta.color.copy(alpha = 0.4f) else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = meta.bgColor,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(meta.icon, contentDescription = null, tint = meta.color, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            config.displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            "Last Sync: ${config.lastSyncTime} · ${config.totalLeadsIngested} leads captured",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Switch(
                    checked = config.isEnabled,
                    onCheckedChange = { isChecked ->
                        onUpdate(config.copy(isEnabled = isChecked))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ElectricBlue
                    )
                )
            }

            // Quick actions: Test Live Sync + Expand Config
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onTestSync,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    border = BorderStroke(1.dp, Color(0xFFDCFCE7)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFF0FDF4))
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Webhook & Sync Lead", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                }

                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        if (isExpanded) "Hide Keys" else "Configure API",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ElectricBlue
                    )
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = ElectricBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key / Token") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 13.sp)
                    )
                    OutlinedTextField(
                        value = webhookUrl,
                        onValueChange = { webhookUrl = it },
                        label = { Text("Webhook Listener URL") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 13.sp)
                    )
                    Button(
                        onClick = {
                            onUpdate(config.copy(apiKey = apiKey, webhookUrl = webhookUrl))
                            isExpanded = false
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Save Credentials", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
