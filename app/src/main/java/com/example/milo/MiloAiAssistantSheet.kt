package com.example.milo

import android.content.Context
import com.example.domain.milo.MiloEvent
import com.example.domain.milo.MiloState
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Interactive Milo AI Assistant Bottom Sheet / Dialogue.
 * Implements:
 * - "What should I focus on today?"
 * - "Summarize lead activity"
 * - "Identify follow-ups"
 * - "Suggest next lead action"
 * - "Generate follow-up message drafts"
 * - "Summarize daily performance"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiloAiAssistantSheet(
    miloViewModel: MiloViewModel,
    onDismiss: () -> Unit,
    onNavigateToLeads: () -> Unit = {},
    onNavigateToFollowUps: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {}
) {
    val state by miloViewModel.state.collectAsState()
    val speechText by miloViewModel.speechText.collectAsState()
    val subSpeechText by miloViewModel.subSpeechText.collectAsState()
    val isThinking by miloViewModel.isAiThinking.collectAsState()
    val insights by miloViewModel.aiInsights.collectAsState()

    var userCustomQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val quickPrompts = listOf(
        "What should I focus on today? 🎯",
        "Summarize lead activity 📥",
        "Generate WhatsApp follow-up draft 💬",
        "Check team attendance 👥",
        "How are our monthly conversion targets? 📈"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = TextSecondary.copy(alpha = 0.4f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Milo Header Area
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiloCharacter(
                    state = state,
                    size = 75.dp,
                    showStateBadge = true
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Milo AI Copilot",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = BrandDarkBlue
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ElectricBlueBg
                        ) {
                            Text(
                                "PRO",
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue
                            )
                        }
                    }
                    Text(
                        text = "Intelligent CRM & Employee Tracking Companion",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Milo Active Response Box
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, state.primaryColor.copy(alpha = 0.3f)),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (isThinking) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = state.primaryColor
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Milo is analyzing Making Brands CRM & Activity...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = state.primaryColor
                            )
                        }
                    } else {
                        Text(
                            text = speechText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BrandDarkBlue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subSpeechText,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Ask Milo Anything:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Prompt Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Scrollable or wrapping chips
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickPrompts) { prompt ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, BorderLight),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                miloViewModel.askMiloAiQuery(prompt)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = prompt,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = BrandDarkBlue
                            )
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // AI Suggested Actionable Cards
                items(insights) { insight ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = insight.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = ElectricBlueBg,
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("AI", fontSize = 9.sp, fontWeight = FontWeight.Black, color = BrandBlue)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = insight.summary,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )

                            if (insight.draftMessage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SurfaceBg,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            "WhatsApp Draft:",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF059669)
                                        )
                                        Text(
                                            insight.draftMessage,
                                            fontSize = 11.sp,
                                            color = TextPrimary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val sendIntent = Intent(Intent.ACTION_VIEW).apply {
                                                data = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(insight.draftMessage)}")
                                            }
                                            try {
                                                context.startActivity(sendIntent)
                                            } catch (e: Exception) {
                                                clipboardManager.setText(AnnotatedString(insight.draftMessage))
                                                Toast.makeText(context, "Copied draft to clipboard!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(34.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Send WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(insight.draftMessage))
                                            Toast.makeText(context, "Copied draft to clipboard!", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(34.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy", fontSize = 11.sp)
                                    }
                                }
                            } else if (insight.actionLabel != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        when (insight.actionRoute) {
                                            "leads" -> {
                                                onDismiss()
                                                onNavigateToLeads()
                                            }
                                            "followups" -> {
                                                onDismiss()
                                                onNavigateToFollowUps()
                                            }
                                            "tasks" -> {
                                                onDismiss()
                                                onNavigateToTasks()
                                            }
                                            else -> {
                                                onDismiss()
                                                onNavigateToLeads()
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                    modifier = Modifier.height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(insight.actionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Custom Question Input Field
            OutlinedTextField(
                value = userCustomQuery,
                onValueChange = { userCustomQuery = it },
                placeholder = { Text("Ask Milo a question...", fontSize = 13.sp, color = TextSecondary) },
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (userCustomQuery.isNotBlank()) {
                                miloViewModel.askMiloAiQuery(userCustomQuery)
                                userCustomQuery = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = BrandBlue)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandBlue,
                    unfocusedBorderColor = BorderLight,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true
            )
        }
    }
}
