package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.data.model.ChatMessageEntity
import com.example.data.model.EmployeeEntity
import com.example.data.model.PresenceStatus
import com.example.ui.components.AppHeader
import com.example.ui.components.StatusIndicatorBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun TeamChatListScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenChannel: (String, String) -> Unit,
    onNavigateToProfile: (() -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    val employees by viewModel.employees.collectAsState(initial = emptyList())

    val channels = listOf(
        Triple("company_chat", "Company Chat", "Arjun: Great work everyone!"),
        Triple("dev_team", "Development Team", "Looks good. Deploying today."),
        Triple("sales_team", "Sales Team", "Rohit: New lead received 🔥"),
        Triple("marketing", "Marketing", "Priya: Campaign updated"),
        Triple("project_alpha", "Project Alpha", "Suresh: Files shared")
    )

    Scaffold(
        topBar = {
            AppHeader(
                title = "Team Chat",
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search chats or team...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        unfocusedBorderColor = BorderLight
                    )
                )
            }

            // Tab Switcher between Channels & Direct Messages
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = BrandBlue,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "Channels",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Team Members",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = BrandBlue.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        "${employees.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandBlue,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    )
                }
            }

            if (selectedTab == 0) {
                val filteredChannels = channels.filter {
                    it.second.contains(searchQuery, ignoreCase = true) || it.third.contains(searchQuery, ignoreCase = true)
                }
                items(filteredChannels) { (id, name, lastMsg) ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectChatChannel(id)
                                onOpenChannel(id, name)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box {
                                    Surface(
                                        shape = CircleShape,
                                        color = BrandBlue.copy(alpha = 0.15f),
                                        modifier = Modifier.size(46.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Forum, contentDescription = null, tint = BrandBlue)
                                        }
                                    }
                                    StatusIndicatorBadge(
                                        status = PresenceStatus.ONLINE,
                                        modifier = Modifier.align(Alignment.BottomEnd)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(lastMsg, fontSize = 12.sp, color = TextSecondary)
                                }
                            }

                            Text("2m", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                }
            } else {
                val filteredEmployees = employees.filter {
                    it.name.contains(searchQuery, ignoreCase = true) || it.designation.contains(searchQuery, ignoreCase = true)
                }
                items(filteredEmployees) { employee ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectChatChannel("dm_${employee.id}")
                                onOpenChannel("dm_${employee.id}", employee.name)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box {
                                    Surface(
                                        shape = CircleShape,
                                        color = BrandBlue.copy(alpha = 0.12f),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                employee.name.take(1),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = BrandBlue
                                            )
                                        }
                                    }
                                    StatusIndicatorBadge(
                                        status = employee.presenceStatus,
                                        modifier = Modifier.align(Alignment.BottomEnd)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(employee.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        StatusIndicatorBadge(
                                            status = employee.presenceStatus,
                                            showLabel = true
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("${employee.designation} · ${employee.department.name.lowercase().capitalize()}", fontSize = 12.sp, color = TextSecondary)
                                }
                            }

                            Icon(Icons.Default.ChevronRight, contentDescription = "Chat", tint = TextMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatRoomScreen(
    channelId: String,
    channelTitle: String,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val messages by viewModel.chatMessages.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            AppHeader(
                title = channelTitle,
                onBack = onBack,
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = BrandBlue)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = BrandBlue)
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        // Quick send sample release APK file attachment
                        viewModel.sendChatMessage(
                            text = "Sharing the latest release build",
                            fileName = "app-release-v1.3.0.apk",
                            fileSize = "2.6 MB"
                        )
                    }) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = TextSecondary)
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Type a message...", fontSize = 14.sp, color = Color(0xFF94A3B8)) },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = appTextFieldColors()
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendChatMessage(inputText)
                                inputText = ""
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message)
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessageEntity) {
    val isMe = message.isMe
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (!isMe) {
            val senderStatus = when {
                message.senderName.contains("Priya", ignoreCase = true) -> PresenceStatus.IN_MEETING
                message.senderName.contains("Suresh", ignoreCase = true) -> PresenceStatus.IN_MEETING
                message.senderName.contains("Neha", ignoreCase = true) -> PresenceStatus.OFFLINE
                else -> PresenceStatus.ONLINE
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            ) {
                StatusIndicatorBadge(
                    status = senderStatus,
                    dotSize = 7.dp
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    "${message.senderName} · ${message.timestampText}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            color = if (isMe) BrandBlue else Color.White,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!message.messageText.isNullOrBlank()) {
                    Text(
                        message.messageText,
                        fontSize = 14.sp,
                        color = if (isMe) Color.White else TextPrimary
                    )
                }

                // APK / Document Attachment Preview Card
                if (message.attachmentFileName != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isMe) BrandDarkBlue else SurfaceBg,
                        modifier = Modifier.widthIn(min = 200.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BrandLightBlue.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Android, contentDescription = null, tint = BrandAccent)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    message.attachmentFileName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMe) Color.White else TextPrimary
                                )
                                Text(
                                    message.attachmentFileSize ?: "",
                                    fontSize = 10.sp,
                                    color = if (isMe) Color.White.copy(alpha = 0.7f) else TextSecondary
                                )
                            }
                        }
                    }
                }

                if (isMe) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        message.timestampText,
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}
