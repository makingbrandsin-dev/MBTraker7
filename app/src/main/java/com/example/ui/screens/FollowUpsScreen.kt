package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FollowUpEntity
import com.example.ui.components.AppHeader
import com.example.ui.theme.*

@Composable
fun FollowUpsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val followUps by viewModel.followUps.collectAsState()
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val filters = listOf("Today", "Upcoming", "Completed")

    val filteredFollowUps = remember(followUps, selectedFilterIndex) {
        val category = filters[selectedFilterIndex]
        followUps.filter { item ->
            if (category == "Completed") {
                item.isCompleted || item.scheduledDateCategory.equals("Completed", ignoreCase = true)
            } else {
                item.scheduledDateCategory.equals(category, ignoreCase = true)
            }
        }.sortedByDescending { it.id }
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Follow-ups",
                onBack = onBack,
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar", tint = TextPrimary)
                    }
                }
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
            // Segmented Filter Tabs
            item {
                TabRow(
                    selectedTabIndex = selectedFilterIndex,
                    containerColor = Color.White
                    
                ) {
                    filters.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedFilterIndex == index,
                            onClick = { selectedFilterIndex = index },
                            text = { Text(title, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            // Follow-up Items
            items(filteredFollowUps) { item ->
                FollowUpCardItem(item)
            }
        }
    }
}

@Composable
fun FollowUpCardItem(item: FollowUpEntity) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BrandBlue.copy(alpha = 0.1f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (item.actionType == "Call") Icons.Default.Phone else Icons.Default.Description,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(item.clientName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    Text(item.taskDescription, fontSize = 12.sp, color = TextSecondary)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(item.scheduledTime, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (item.actionType == "Call") StatusGreen else BrandBlue
                    ),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(item.actionType, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
