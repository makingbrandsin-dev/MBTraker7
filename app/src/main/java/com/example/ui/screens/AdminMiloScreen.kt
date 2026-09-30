package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import com.example.domain.milo.MiloState
import com.example.milo.MiloAssistantCard
import com.example.milo.MiloCharacter
import com.example.milo.MiloRealStatusView
import com.example.milo.MiloVideoHelper
import com.example.milo.MiloVideoSurface
import com.example.ui.components.liftOnPress
import com.example.ui.theme.*

/**
 * 👑 Admin Control Panel for Milo AI Assistant, MP4 Video Files, and Splash Screen Status.
 *
 * Capabilities:
 * 1. Per-Status MP4 Video Upload for Milo Smart Assistant (all 13 Milo states).
 * 2. Configure Splash Screen Milo Status & MP4 Video loop.
 * 3. Upload / Restore Milo Assistant Card MP4 Video files & custom speech.
 * 4. Configure Center Bottom Bar "ASK MILO" MP4 Video.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMiloScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // State refresh counter to trigger recomposition when videos are uploaded/deleted
    var refreshKey by remember { mutableIntStateOf(0) }

    // Chroma Key Green Screen Removal States
    var isChromaKeyActive by remember(refreshKey) { mutableStateOf(MiloVideoHelper.isChromaKeyEnabled(context)) }
    var chromaSensitivity by remember(refreshKey) { mutableStateOf(MiloVideoHelper.getChromaKeyThreshold(context)) }

    // Per-Status MP4 Video Upload States
    var selectedMiloState by remember { mutableStateOf(MiloState.IDLE) }
    var stateUrlInput by remember(selectedMiloState, refreshKey) {
        mutableStateOf(MiloVideoHelper.getMiloStateUrl(context, selectedMiloState) ?: "")
    }

    // Splash Screen States
    var splashStatusText by remember { mutableStateOf(MiloVideoHelper.getSplashScreenStatusText(context)) }
    var selectedSplashState by remember { mutableStateOf(MiloVideoHelper.getSplashScreenMiloState(context)) }
    var splashVideoUri by remember(refreshKey) { mutableStateOf(MiloVideoHelper.getSplashScreenVideoUri(context)) }

    // Assistant Card States
    var assistantCardVideoUri by remember(refreshKey) { mutableStateOf(MiloVideoHelper.getMiloAssistantCardVideoUri(context)) }
    var askMiloStatusText by remember { mutableStateOf(MiloVideoHelper.getAskMiloDefaultStatusText(context)) }

    // Ask Milo Button States
    var askButtonVideoUri by remember(refreshKey) { mutableStateOf(MiloVideoHelper.getAskMiloButtonVideoUri(context)) }

    var testPlayVideoUri by remember { mutableStateOf<Uri?>(null) }
    var showSplashStateDropdown by remember { mutableStateOf(false) }

    // File Pickers
    val perStateVideoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val success = MiloVideoHelper.saveMiloStateVideo(context, uri, selectedMiloState)
            if (success) {
                refreshKey++
                Toast.makeText(context, "✅ MP4 video for ${selectedMiloState.title} saved successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "❌ Failed to save MP4 video.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val splashVideoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val success = MiloVideoHelper.saveSplashScreenVideo(context, uri)
            if (success) {
                refreshKey++
                splashVideoUri = MiloVideoHelper.getSplashScreenVideoUri(context)
                Toast.makeText(context, "✅ Splash Screen MP4 video saved successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val assistantCardVideoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val success = MiloVideoHelper.saveMiloAssistantCardVideo(context, uri)
            if (success) {
                refreshKey++
                assistantCardVideoUri = MiloVideoHelper.getMiloAssistantCardVideoUri(context)
                Toast.makeText(context, "✅ Milo Assistant Card MP4 video saved!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val askButtonVideoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val success = MiloVideoHelper.saveAskMiloButtonVideo(context, uri)
            if (success) {
                refreshKey++
                askButtonVideoUri = MiloVideoHelper.getAskMiloButtonVideoUri(context)
                Toast.makeText(context, "✅ Ask Milo Button MP4 video saved!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Milo AI & Video Admin", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                        Text("Per-status MP4 uploads, Splash screen & Assistant controls", fontSize = 11.sp, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_milo_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 🟢 Green Screen Background Removal (Chroma Key Engine) Options
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFDCFCE7),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.VideoCameraBack, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Remove Green Screen Background", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                    Text("Hardware OpenGL ES 2.0 Chroma Key Shader", fontSize = 11.sp, color = TextSecondary)
                                }
                            }

                            Switch(
                                checked = isChromaKeyActive,
                                onCheckedChange = { checked ->
                                    isChromaKeyActive = checked
                                    MiloVideoHelper.setChromaKeyEnabled(context, checked)
                                    refreshKey++
                                    Toast.makeText(
                                        context,
                                        if (checked) "🟢 Green screen background removal ENABLED!" else "⚪ Green screen background removal DISABLED",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = StatusGreen)
                            )
                        }

                        if (isChromaKeyActive) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Green Keying Sensitivity Threshold: ${String.format("%.2f", chromaSensitivity)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            Slider(
                                value = chromaSensitivity,
                                onValueChange = { newValue ->
                                    chromaSensitivity = newValue
                                    MiloVideoHelper.setChromaKeyThreshold(context, newValue)
                                },
                                onValueChangeFinished = {
                                    refreshKey++
                                },
                                valueRange = 0.10f..0.50f,
                                colors = SliderDefaults.colors(thumbColor = StatusGreen, activeTrackColor = StatusGreen)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("High Precision (0.10)", fontSize = 10.sp, color = TextSecondary)
                                Text("Aggressive Cut (0.50)", fontSize = 10.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }

            // 1. 🦁 Milo Smart Assistant - Per-Status MP4 Video Uploads
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEFF6FF),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.SmartToy, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Milo Smart Assistant — Per-Status MP4 Videos", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                Text("Upload separate MP4 video for each Milo status (13 states)", fontSize = 11.sp, color = TextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // State Selection Horizontal Strip
                        Text(
                            text = "SELECT STATUS TO UPLOAD / MANAGE:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(MiloState.entries.toTypedArray()) { st ->
                                val isSelected = st == selectedMiloState
                                val hasCustom = MiloVideoHelper.hasCustomMiloVideo(context, st)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) BrandBlue else if (hasCustom) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) BrandBlue else if (hasCustom) Color(0xFF86EFAC) else Color(0xFFCBD5E1)
                                    ),
                                    modifier = Modifier
                                        .liftOnPress()
                                        .clickable {
                                            selectedMiloState = st
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(st.emoji, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = st.title,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else if (hasCustom) Color(0xFF166534) else TextPrimary
                                        )
                                        if (hasCustom) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Surface(
                                                shape = CircleShape,
                                                color = if (isSelected) Color.White else StatusGreen,
                                                modifier = Modifier.size(6.dp)
                                            ) {}
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Selected Status Studio Card
                        val currentVideoUri = remember(selectedMiloState, refreshKey) {
                            MiloVideoHelper.getMiloVideoUri(context, selectedMiloState)
                        }
                        val hasCustomVideo = remember(selectedMiloState, refreshKey) {
                            MiloVideoHelper.hasCustomMiloVideo(context, selectedMiloState)
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Live mascot preview with video loop (Clean, no circle shape)
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        MiloCharacter(
                                            state = selectedMiloState,
                                            size = 72.dp,
                                            showStateBadge = false
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${selectedMiloState.emoji} ${selectedMiloState.title}",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = selectedMiloState.description,
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            lineHeight = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (hasCustomVideo) Color(0xFFDCFCE7) else Color(0xFFEFF6FF)
                                        ) {
                                            Text(
                                                text = if (hasCustomVideo) "✨ Custom MP4 Active" else "🦁 Default Mascot Animation",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (hasCustomVideo) StatusGreen else BrandBlue,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Upload & Preview Buttons for Selected State
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { perStateVideoPicker.launch("video/mp4") },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                        modifier = Modifier
                                            .weight(1f)
                                            .liftOnPress()
                                    ) {
                                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Upload MP4", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    if (currentVideoUri != null) {
                                        OutlinedButton(
                                            onClick = { testPlayVideoUri = currentVideoUri },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .liftOnPress()
                                        ) {
                                            Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Preview", fontSize = 12.sp)
                                        }
                                    }

                                    if (hasCustomVideo) {
                                        OutlinedButton(
                                            onClick = {
                                                MiloVideoHelper.deleteMiloStateVideo(context, selectedMiloState)
                                                refreshKey++
                                                Toast.makeText(context, "Reverted ${selectedMiloState.title} to default mascot!", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed),
                                            modifier = Modifier.liftOnPress()
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Remote URL input option
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = stateUrlInput,
                                        onValueChange = { stateUrlInput = it },
                                        label = { Text("Or Remote MP4 URL for ${selectedMiloState.title}") },
                                        placeholder = { Text("https://cdn.example.com/milo_${selectedMiloState.name.lowercase()}.mp4") },
                                        singleLine = true,
                                        colors = appTextFieldColors(),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    FilledTonalButton(
                                        onClick = {
                                            MiloVideoHelper.saveMiloStateUrl(context, stateUrlInput, selectedMiloState)
                                            refreshKey++
                                            Toast.makeText(context, "Saved URL for ${selectedMiloState.title}!", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.height(52.dp)
                                    ) {
                                        Text("Save", fontSize = 12.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Button to test activate this state in App
                                Button(
                                    onClick = {
                                        viewModel.miloViewModel.setState(selectedMiloState)
                                        Toast.makeText(context, "🦁 Activated ${selectedMiloState.title} state in Milo Copilot!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .liftOnPress()
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Set as Active Milo State in App", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 2. 🚀 Splash Screen Milo Status & MP4 Configuration
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEFF6FF),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Splash Screen Milo Status", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                Text("Status text & MP4 animation on startup", fontSize = 11.sp, color = TextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Status Text Input
                        OutlinedTextField(
                            value = splashStatusText,
                            onValueChange = {
                                splashStatusText = it
                                MiloVideoHelper.saveSplashScreenStatusText(context, it)
                            },
                            label = { Text("Splash Screen Status Message") },
                            placeholder = { Text("e.g. 🤖 Milo AI: Initializing Making Brands Cloud...") },
                            singleLine = true,
                            colors = appTextFieldColors(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Milo Mood/State Dropdown
                        Text("Splash Screen Milo State / Mood:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))

                        ExposedDropdownMenuBox(
                            expanded = showSplashStateDropdown,
                            onExpandedChange = { showSplashStateDropdown = it }
                        ) {
                            OutlinedTextField(
                                value = "${selectedSplashState.emoji} ${selectedSplashState.title}",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSplashStateDropdown) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = showSplashStateDropdown,
                                onDismissRequest = { showSplashStateDropdown = false }
                            ) {
                                MiloState.entries.forEach { st ->
                                    DropdownMenuItem(
                                        text = { Text("${st.emoji} ${st.title}", fontSize = 13.sp) },
                                        onClick = {
                                            selectedSplashState = st
                                            MiloVideoHelper.saveSplashScreenMiloState(context, st)
                                            showSplashStateDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // MP4 Video Upload / File Selector for Splash
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { splashVideoPicker.launch("video/mp4") },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                modifier = Modifier
                                    .weight(1f)
                                    .liftOnPress()
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload Splash MP4", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            if (splashVideoUri != null) {
                                OutlinedButton(
                                    onClick = { testPlayVideoUri = splashVideoUri },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .liftOnPress()
                                ) {
                                    Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Preview Video", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 3. 🦁 Home Screen Milo Assistant Card MP4 & Status
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF0FDF4),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.SmartToy, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Milo Assistant Card (Home Screen)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                Text("Upload MP4 files & custom executive greeting", fontSize = 11.sp, color = TextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = askMiloStatusText,
                            onValueChange = {
                                askMiloStatusText = it
                                MiloVideoHelper.saveAskMiloDefaultStatusText(context, it)
                            },
                            label = { Text("Milo Card Default Status Text") },
                            placeholder = { Text("e.g. Ready for duty! Tap to ask Milo anything.") },
                            singleLine = true,
                            colors = appTextFieldColors(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Upload MP4 for Assistant Card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { assistantCardVideoPicker.launch("video/mp4") },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                                modifier = Modifier
                                    .weight(1f)
                                    .liftOnPress()
                            ) {
                                Icon(Icons.Default.VideoCall, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload Card MP4", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            if (assistantCardVideoUri != null) {
                                OutlinedButton(
                                    onClick = { testPlayVideoUri = assistantCardVideoUri },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .liftOnPress()
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Preview Card MP4", fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Live Assistant Card Preview
                        Text("Live Preview on Home Screen:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))

                        MiloAssistantCard(
                            isWorking = true,
                            speechText = askMiloStatusText,
                            onOpenAssistant = {
                                Toast.makeText(context, "Milo Assistant Dialog would open on tap!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            // 4. 🔘 Bottom Bar "ASK MILO" Center Button MP4
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEDE9FE),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Navigation, contentDescription = null, tint = VibrantPurple, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Ask Milo Center Button MP4", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                Text("Video for middle button in bottom navigation", fontSize = 11.sp, color = TextSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { askButtonVideoPicker.launch("video/mp4") },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VibrantPurple),
                                modifier = Modifier
                                    .weight(1f)
                                    .liftOnPress()
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload Button MP4", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            if (askButtonVideoUri != null) {
                                OutlinedButton(
                                    onClick = { testPlayVideoUri = askButtonVideoUri },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .liftOnPress()
                                ) {
                                    Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Preview", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Video Test Dialog
    testPlayVideoUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { testPlayVideoUri = null },
            title = { Text("MP4 Video Playback Preview", fontWeight = FontWeight.Bold) },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    MiloVideoSurface(videoUri = uri, modifier = Modifier.fillMaxSize())
                }
            },
            confirmButton = {
                Button(onClick = { testPlayVideoUri = null }) {
                    Text("Done")
                }
            }
        )
    }
}
