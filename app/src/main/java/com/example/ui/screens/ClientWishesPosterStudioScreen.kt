package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.ClientOccasionWishEntity
import com.example.data.model.FestivalOccasionItem
import com.example.data.model.LeadEntity
import com.example.data.model.WrittenDraftEntity
import com.example.ui.theme.BrandBlue
import com.example.util.MiloHaptics
import com.example.util.WhatsAppHelper
import java.text.SimpleDateFormat
import java.util.*

/**
 * 🌟 Client Wishes & Festival Poster Studio Screen:
 * Allows Admin to oversee and dispatch personalized festival greetings,
 * national holiday wishes, client milestones, upload custom posters,
 * and broadcast directly to all clients via WhatsApp.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientWishesPosterStudioScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    initialOccasionId: String? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current

    val festivalOccasions by viewModel.upcomingFestivalOccasions.collectAsState()
    val allLeads by viewModel.leads.collectAsState(initial = emptyList())
    val clientWishes by viewModel.clientOccasionWishes.collectAsState()
    val writtenDrafts by viewModel.writtenDrafts.collectAsState()

    var selectedStudioTab by remember { mutableStateOf(0) } // 0: Festive Wishes & Posters, 1: Written Drafts, 2: Wishes Log
    var selectedOccasion by remember {
        mutableStateOf(
            festivalOccasions.firstOrNull { it.id == initialOccasionId } ?: festivalOccasions.first()
        )
    }

    // Poster Upload & Broadcast State
    var uploadedPosterUri by remember { mutableStateOf<Uri?>(null) }
    var customWishText by remember { mutableStateOf(selectedOccasion.defaultWishTemplate) }
    var selectedAudienceType by remember { mutableStateOf("All Clients") } // "All Clients", "Hot Leads", "Converted Won", "Custom Select"
    var selectedLeadIds by remember { mutableStateOf(setOf<Long>()) }
    var isBroadcasting by remember { mutableStateOf(false) }
    var broadcastProgressCount by remember { mutableStateOf(0) }

    // Dialog States
    var showDraftDialog by remember { mutableStateOf(false) }
    var draftToEdit by remember { mutableStateOf<WrittenDraftEntity?>(null) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var showAddOccasionDialog by remember { mutableStateOf(false) }

    // Update wish text when occasion changes
    LaunchedEffect(selectedOccasion) {
        customWishText = selectedOccasion.defaultWishTemplate
    }

    // Photo Picker for Poster Image
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            uploadedPosterUri = uri
            Toast.makeText(context, "Poster uploaded successfully! Ready for WhatsApp broadcast.", Toast.LENGTH_SHORT).show()
        }
    }

    // Resolved Target Clients
    val targetClients: List<LeadEntity> = remember(allLeads, selectedAudienceType, selectedLeadIds) {
        when (selectedAudienceType) {
            "Hot Leads" -> allLeads.filter { it.leadScore >= 75 || it.stage.contains("Hot", ignoreCase = true) }
            "Converted Won" -> allLeads.filter { it.stage.equals("Won", ignoreCase = true) || it.stage.equals("Converted", ignoreCase = true) }
            "Custom Select" -> allLeads.filter { selectedLeadIds.contains(it.id) }
            else -> allLeads.ifEmpty {
                // Fallback default sample clients if DB has few
                listOf(
                    LeadEntity(id = 101, name = "Ananya Roy", company = "TechInnovate Ltd", phone = "+91 98765 11223", email = "ananya@techinnovate.com", leadScore = 85, requirement = "Enterprise CRM", potentialValue = "₹ 2,50,000", stage = "Proposal"),
                    LeadEntity(id = 102, name = "Vikram Malhotra", company = "Apex Global Corp", phone = "+91 98111 22334", email = "vikram@apex.com", leadScore = 90, requirement = "Mobile App Suite", potentialValue = "₹ 4,00,000", stage = "Won"),
                    LeadEntity(id = 103, name = "Sneha Kulkarni", company = "Kulkarni Retail", phone = "+91 98222 33445", email = "sneha@retail.com", leadScore = 80, requirement = "Marketing Automation", potentialValue = "₹ 1,80,000", stage = "Contacted"),
                    LeadEntity(id = 104, name = "Rajesh Gupta", company = "Gupta Logistics", phone = "+91 98333 44556", email = "rajesh@guptalog.com", leadScore = 70, requirement = "Live Fleet Tracker", potentialValue = "₹ 3,20,000", stage = "New")
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Client Wishes & Poster Studio",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.5.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    text = "WHATSAPP READY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF16A34A),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Send posters & event greetings to ${allLeads.size.coerceAtLeast(4)} clients",
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            MiloHaptics.performButtonClick(context, haptic)
                            showDraftDialog = true
                            draftToEdit = null
                        }
                    ) {
                        Icon(Icons.Default.PostAdd, contentDescription = "Write New Draft", tint = BrandBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 🏷️ Sub-Navigation Tabs
            TabRow(
                selectedTabIndex = selectedStudioTab,
                containerColor = Color.White,
                contentColor = BrandBlue,
                divider = { HorizontalDivider(color = Color(0xFFE2E8F0)) }
            ) {
                Tab(
                    selected = selectedStudioTab == 0,
                    onClick = {
                        MiloHaptics.performButtonClick(context, haptic)
                        selectedStudioTab = 0
                    },
                    text = {
                        Text(
                            text = "Festivals & Posters",
                            fontSize = 12.sp,
                            fontWeight = if (selectedStudioTab == 0) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    icon = { Icon(Icons.Default.Festival, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )

                Tab(
                    selected = selectedStudioTab == 1,
                    onClick = {
                        MiloHaptics.performButtonClick(context, haptic)
                        selectedStudioTab = 1
                    },
                    text = {
                        Text(
                            text = "Written Drafts (${writtenDrafts.size})",
                            fontSize = 12.sp,
                            fontWeight = if (selectedStudioTab == 1) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    icon = { Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )

                Tab(
                    selected = selectedStudioTab == 2,
                    onClick = {
                        MiloHaptics.performButtonClick(context, haptic)
                        selectedStudioTab = 2
                    },
                    text = {
                        Text(
                            text = "Wishes Log (${clientWishes.size})",
                            fontSize = 12.sp,
                            fontWeight = if (selectedStudioTab == 2) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    icon = { Icon(Icons.Default.HistoryEdu, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            // 📱 Tab Body Content
            when (selectedStudioTab) {
                0 -> {
                    // TAB 1: Festival & Occasions Studio with Poster Uploader & WhatsApp Broadcast
                    FestivalPosterStudioTabContent(
                        occasions = festivalOccasions,
                        selectedOccasion = selectedOccasion,
                        onSelectOccasion = { selectedOccasion = it },
                        uploadedPosterUri = uploadedPosterUri,
                        onUploadPosterClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onClearPoster = { uploadedPosterUri = null },
                        customWishText = customWishText,
                        onWishTextChange = { customWishText = it },
                        onAskMiloAi = {
                            MiloHaptics.performButtonClick(context, haptic)
                            customWishText = viewModel.generateMiloClientWishDraft(
                                clientName = "{{client_name}}",
                                companyName = "{{company_name}}",
                                occasionName = selectedOccasion.title,
                                category = selectedOccasion.category
                            )
                            Toast.makeText(context, "Milo generated festive wish draft!", Toast.LENGTH_SHORT).show()
                        },
                        selectedAudienceType = selectedAudienceType,
                        onAudienceTypeSelected = { selectedAudienceType = it },
                        targetClients = targetClients,
                        onPreviewClick = { showPreviewDialog = true },
                        onSendSingleClient = { client ->
                            MiloHaptics.performButtonClick(context, haptic)
                            viewModel.dispatchWhatsAppPosterToClient(
                                context = context,
                                clientPhone = client.phone,
                                clientName = client.name,
                                clientCompany = client.company,
                                occasionName = selectedOccasion.title,
                                customMessage = customWishText,
                                posterUriString = uploadedPosterUri?.toString()
                            )
                        },
                        onBroadcastAll = {
                            MiloHaptics.performButtonClick(context, haptic)
                            if (targetClients.isEmpty()) {
                                Toast.makeText(context, "No clients in target audience", Toast.LENGTH_SHORT).show()
                                return@FestivalPosterStudioTabContent
                            }
                            // Start sequential broadcast
                            isBroadcasting = true
                            broadcastProgressCount = 0
                            val firstClient = targetClients.first()
                            viewModel.dispatchWhatsAppPosterToClient(
                                context = context,
                                clientPhone = firstClient.phone,
                                clientName = firstClient.name,
                                clientCompany = firstClient.company,
                                occasionName = selectedOccasion.title,
                                customMessage = customWishText,
                                posterUriString = uploadedPosterUri?.toString()
                            )
                            Toast.makeText(context, "Opening WhatsApp for ${firstClient.name}. Showing list to continue broadcasting to all ${targetClients.size} clients.", Toast.LENGTH_LONG).show()
                        }
                    )
                }

                1 -> {
                    // TAB 2: Written Drafts Manager
                    WrittenDraftsManagerTabContent(
                        drafts = writtenDrafts,
                        onAddNewDraft = {
                            draftToEdit = null
                            showDraftDialog = true
                        },
                        onEditDraft = { draft ->
                            draftToEdit = draft
                            showDraftDialog = true
                        },
                        onDeleteDraft = { id ->
                            viewModel.deleteWrittenDraft(id)
                            Toast.makeText(context, "Draft deleted", Toast.LENGTH_SHORT).show()
                        },
                        onCopyDraft = { text ->
                            clipboardManager.setText(AnnotatedString(text))
                            Toast.makeText(context, "Draft copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        onSendDraftViaWhatsApp = { draft ->
                            val defaultPhone = allLeads.firstOrNull()?.phone ?: "+91 98765 11223"
                            WhatsAppHelper.sendWhatsAppMessage(context, defaultPhone, draft.content)
                        }
                    )
                }

                2 -> {
                    // TAB 3: Dispatched Wishes Log & History
                    ClientWishesLogTabContent(
                        wishes = clientWishes,
                        onDeleteWish = { id -> viewModel.deleteClientOccasionWish(id) },
                        onResendWish = { wish ->
                            viewModel.dispatchWhatsAppPosterToClient(
                                context = context,
                                clientPhone = wish.clientPhone,
                                clientName = wish.clientName,
                                clientCompany = wish.clientCompany,
                                occasionName = wish.occasionName,
                                customMessage = wish.wishMessage,
                                posterUriString = wish.posterImageUri,
                                wishIdToMarkSent = wish.id
                            )
                        }
                    )
                }
            }
        }
    }

    // 📝 Draft Create/Edit Dialog
    if (showDraftDialog) {
        WrittenDraftEditorDialog(
            draft = draftToEdit,
            viewModel = viewModel,
            onDismiss = { showDraftDialog = false },
            onSave = { newDraft ->
                viewModel.saveWrittenDraft(newDraft) {
                    showDraftDialog = false
                    Toast.makeText(context, "Draft saved successfully", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // 🖼️ Poster & Message Preview Dialog
    if (showPreviewDialog) {
        PosterMessagePreviewDialog(
            occasion = selectedOccasion,
            customMessage = customWishText,
            posterUri = uploadedPosterUri,
            sampleClient = targetClients.firstOrNull() ?: LeadEntity(
                name = "Rajesh Sharma",
                company = "Apex Industries",
                phone = "+91 98765 43210",
                email = "rajesh@apex.com",
                leadScore = 85,
                requirement = "Enterprise CRM",
                potentialValue = "₹ 2,50,000",
                stage = "Proposal"
            ),
            onDismiss = { showPreviewDialog = false }
        )
    }
}

/**
 * TAB 1 Content: Festivals, Poster Upload, and WhatsApp Broadcaster
 */
@Composable
private fun FestivalPosterStudioTabContent(
    occasions: List<FestivalOccasionItem>,
    selectedOccasion: FestivalOccasionItem,
    onSelectOccasion: (FestivalOccasionItem) -> Unit,
    uploadedPosterUri: Uri?,
    onUploadPosterClick: () -> Unit,
    onClearPoster: () -> Unit,
    customWishText: String,
    onWishTextChange: (String) -> Unit,
    onAskMiloAi: () -> Unit,
    selectedAudienceType: String,
    onAudienceTypeSelected: (String) -> Unit,
    targetClients: List<LeadEntity>,
    onPreviewClick: () -> Unit,
    onSendSingleClient: (LeadEntity) -> Unit,
    onBroadcastAll: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Upcoming National Holidays & Festivals Selector
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. Select Occasion or Festival",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFEDE9FE)
                        ) {
                            Text(
                                text = selectedOccasion.category.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6D28D9),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(occasions, key = { it.id }) { occ ->
                            val isSelected = occ.id == selectedOccasion.id
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) BrandBlue else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectOccasion(occ) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(occ.iconEmoji, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = occ.title,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = if (isSelected) BrandBlue else Color(0xFF334155)
                                        )
                                        Text(
                                            text = occ.dateText,
                                            fontSize = 10.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Poster Upload / Image Customizer
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "2. Upload Festive Poster for Clients",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Upload custom graphics from device or use our auto-styled festive banner",
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (uploadedPosterUri != null) {
                        // Display Uploaded Poster Preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF0F172A)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = uploadedPosterUri,
                                contentDescription = "Uploaded Poster",
                                modifier = Modifier.fillMaxSize()
                            )

                            IconButton(
                                onClick = onClearPoster,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    } else {
                        // Pre-built AI Festive Gradient Canvas Card
                        val gradientColors = selectedOccasion.bannerGradientColors.map { Color(it) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.linearGradient(gradientColors))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${selectedOccasion.iconEmoji} ${selectedOccasion.title}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Making Brands • Warmest Wishes & Greetings",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = "Generated Festive Digital Card",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onUploadPosterClick,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (uploadedPosterUri != null) "Replace Poster" else "Upload Custom Image", fontSize = 12.sp, color = BrandBlue, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onPreviewClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Preview", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Personalized WhatsApp Greeting Message & Milo AI Draft Writer
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "3. Personalized WhatsApp Message",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )

                        // Milo AI Generator Button
                        Button(
                            onClick = onAskMiloAi,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF3C7), contentColor = Color(0xFFB45309)),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Milo AI Draft", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customWishText,
                        onValueChange = onWishTextChange,
                        placeholder = { Text("Write your greeting... (Use {{client_name}}, {{company_name}} tags)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Helpful Variable Insertion Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onWishTextChange("$customWishText {{client_name}}") }
                        ) {
                            Text("+ {{client_name}}", fontSize = 10.sp, color = Color(0xFF475569), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onWishTextChange("$customWishText {{company_name}}") }
                        ) {
                            Text("+ {{company_name}}", fontSize = 10.sp, color = Color(0xFF475569), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onWishTextChange("$customWishText {{occasion}}") }
                        ) {
                            Text("+ {{occasion}}", fontSize = 10.sp, color = Color(0xFF475569), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                        }
                    }
                }
            }
        }

        // 4. Target Client Audience Selector & WhatsApp Broadcast
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "4. Target Audience (${targetClients.size} Clients)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = selectedAudienceType,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Audience Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("All Clients", "Hot Leads", "Converted Won").forEach { audience ->
                            val isSelected = selectedAudienceType == audience
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) BrandBlue else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isSelected) BrandBlue else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onAudienceTypeSelected(audience) }
                            ) {
                                Text(
                                    text = audience,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF475569),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 🚀 WhatsApp Broadcast Button (All Clients)
                    Button(
                        onClick = onBroadcastAll,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366), contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("broadcast_whatsapp_poster_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Broadcast Poster to All ${targetClients.size} Clients",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // 5. Individual Client Dispatch List
        item {
            Text(
                text = "Send 1-by-1 to Individual Clients:",
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = Color(0xFF334155)
            )
        }

        items(targetClients, key = { it.id }) { client ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
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
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = client.name.take(2).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = client.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "${client.company.ifBlank { "Client" }} • ${client.phone}",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Button(
                        onClick = { onSendSingleClient(client) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDCFCE7), contentColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * TAB 2 Content: Written Drafts Manager
 */
@Composable
private fun WrittenDraftsManagerTabContent(
    drafts: List<WrittenDraftEntity>,
    onAddNewDraft: () -> Unit,
    onEditDraft: (WrittenDraftEntity) -> Unit,
    onDeleteDraft: (Long) -> Unit,
    onCopyDraft: (String) -> Unit,
    onSendDraftViaWhatsApp: (WrittenDraftEntity) -> Unit
) {
    var selectedCategoryFilter by remember { mutableStateOf("All Drafts") }
    val categories = listOf("All Drafts", "Festival & Holiday Wishes", "Client Proposal", "Broadcast Notice", "Project Update")

    val filteredDrafts = remember(drafts, selectedCategoryFilter) {
        if (selectedCategoryFilter == "All Drafts") drafts else drafts.filter { it.category == selectedCategoryFilter }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Action Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Organizational Written Drafts",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Proposals, client wishes, marketing blurbs, and announcements",
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Button(
                    onClick = onAddNewDraft,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Draft", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Category Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(categories) { cat ->
                    val isSelected = selectedCategoryFilter == cat
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) BrandBlue else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) BrandBlue else Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedCategoryFilter = cat }
                    ) {
                        Text(
                            text = cat,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        if (filteredDrafts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Article, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No drafts saved under this category", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(filteredDrafts, key = { it.id }) { draft ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = draft.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.weight(1f)
                            )

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = draft.category,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = draft.content,
                            fontSize = 12.sp,
                            color = Color(0xFF334155),
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Audience: ${draft.targetAudience}",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { onCopyDraft(draft.content) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { onEditDraft(draft) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = BrandBlue, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { onSendDraftViaWhatsApp(draft) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "Send WhatsApp", tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { onDeleteDraft(draft.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * TAB 3 Content: Dispatched Client Wishes Log
 */
@Composable
private fun ClientWishesLogTabContent(
    wishes: List<ClientOccasionWishEntity>,
    onDeleteWish: (Long) -> Unit,
    onResendWish: (ClientOccasionWishEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Client Wishing Audit & Dispatch History",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "Real-time records of all festive wishes & posters sent to clients",
                fontSize = 11.5.sp,
                color = Color(0xFF64748B)
            )
        }

        if (wishes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.MarkChatRead, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No wishes dispatched yet. Broadcast your first festive poster!", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(wishes, key = { it.id }) { wish ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = wish.clientName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "${wish.clientCompany.ifBlank { "Client" }} • ${wish.clientPhone}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    text = wish.status,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16A34A),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Occasion: ${wish.occasionName}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue
                        )

                        Text(
                            text = wish.wishMessage,
                            fontSize = 11.5.sp,
                            color = Color(0xFF334155),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val timeStr = if (wish.sentTimestamp > 0) SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(wish.sentTimestamp)) else "Recently"
                            Text(
                                text = "Sent: $timeStr",
                                fontSize = 10.5.sp,
                                color = Color(0xFF94A3B8)
                            )

                            Row {
                                TextButton(
                                    onClick = { onResendWish(wish) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Resend WhatsApp", fontSize = 11.sp, color = BrandBlue, fontWeight = FontWeight.Bold)
                                }
                                IconButton(
                                    onClick = { onDeleteWish(wish.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Written Draft Editor Dialog
 */
@Composable
private fun WrittenDraftEditorDialog(
    draft: WrittenDraftEntity?,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSave: (WrittenDraftEntity) -> Unit
) {
    var title by remember { mutableStateOf(draft?.title ?: "") }
    var category by remember { mutableStateOf(draft?.category ?: "Festival & Holiday Wishes") }
    var targetAudience by remember { mutableStateOf(draft?.targetAudience ?: "All Clients") }
    var content by remember { mutableStateOf(draft?.content ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (draft != null) "Edit Written Draft" else "New Written Draft",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )

                    Button(
                        onClick = {
                            content = viewModel.generateMiloCorporateDraft(
                                topic = title.ifBlank { "Client Proposal & Partnership" },
                                targetAudience = targetAudience,
                                category = category
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF3C7), contentColor = Color(0xFFB45309)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Milo AI", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Draft Title") },
                    placeholder = { Text("e.g. Diwali Corporate Greeting 2026") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = targetAudience,
                    onValueChange = { targetAudience = it },
                    label = { Text("Target Audience") },
                    placeholder = { Text("e.g. All Clients, Won Enterprise Leads") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Draft Content") },
                    placeholder = { Text("Compose the body of the message or proposal...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (title.isBlank() || content.isBlank()) return@Button
                            val toSave = draft?.copy(
                                title = title,
                                category = category,
                                targetAudience = targetAudience,
                                content = content,
                                lastEdited = System.currentTimeMillis()
                            ) ?: WrittenDraftEntity(
                                title = title,
                                category = category,
                                targetAudience = targetAudience,
                                content = content
                            )
                            onSave(toSave)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Draft", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Poster & Message Preview Dialog
 */
@Composable
private fun PosterMessagePreviewDialog(
    occasion: FestivalOccasionItem,
    customMessage: String,
    posterUri: Uri?,
    sampleClient: LeadEntity,
    onDismiss: () -> Unit
) {
    val formattedMessage = customMessage
        .replace("{{client_name}}", sampleClient.name)
        .replace("{{company_name}}", sampleClient.company.ifBlank { "Your Company" })
        .replace("{{occasion}}", occasion.title)
        .replace("{{sender_name}}", "Making Brands Leadership")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "WhatsApp Dispatch Preview",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Previewing for ${sampleClient.name} (${sampleClient.phone})",
                    fontSize = 11.5.sp,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Poster Preview Box
                if (posterUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = posterUri,
                            contentDescription = "Poster Preview",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    val colors = occasion.bannerGradientColors.map { Color(it) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(colors))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${occasion.iconEmoji} ${occasion.title}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Specially prepared for ${sampleClient.name}",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // WhatsApp Speech Bubble Preview
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFDCF8C6), // WhatsApp bubble green
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formattedMessage,
                        fontSize = 11.5.sp,
                        color = Color(0xFF0F172A),
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close Preview", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
