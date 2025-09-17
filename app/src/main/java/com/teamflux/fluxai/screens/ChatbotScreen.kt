package com.teamflux.fluxai.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teamflux.fluxai.model.ChatMessage
import com.teamflux.fluxai.viewmodel.AuthViewModel
import com.teamflux.fluxai.ui.theme.typewriterEffect
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(authViewModel: AuthViewModel? = null) {
    val viewModel = authViewModel ?: viewModel<AuthViewModel>()
    val authState by viewModel.uiState.collectAsState()

    val userRole = authState.selectedRole ?: "employee"
    val isAdmin = userRole == "admin"
    val sessionId = rememberSaveable(authState.currentUser?.uid) { authState.currentUser?.uid ?: "guest-session" }
    var isKnowledgeBaseExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header with role-specific information
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "",
                        fontSize = 50.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.typewriterEffect(if (isAdmin) "Admin AI Assistant" else "Employee AI Assistant")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Collapsible Knowledge Base section
                Card(
                    onClick = { isKnowledgeBaseExpanded = !isKnowledgeBaseExpanded },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
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
                                text = "Knowledge Base Access",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Icon(
                                imageVector = if (isKnowledgeBaseExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isKnowledgeBaseExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        if (isKnowledgeBaseExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))

                            val knowledgePoints = if (isAdmin) {
                                listOf(
                                    "Team performance analytics and metrics",
                                    "Individual employee productivity insights",
                                    "Project management and resource allocation",
                                    "Code review patterns and quality assessments",
                                    "Team collaboration and communication analysis",
                                    "Sprint planning and delivery optimization",
                                    "Budget and timeline management guidance",
                                    "Employee skill gap analysis and recommendations"
                                )
                            } else {
                                listOf(
                                    "Personal performance tracking and improvement tips",
                                    "Code quality best practices and suggestions",
                                    "Learning resources and skill development paths",
                                    "Project status updates and task guidance",
                                    "Team collaboration tools and workflows",
                                    "Time management and productivity techniques",
                                    "Technical documentation and coding standards",
                                    "Career development and growth opportunities"
                                )
                            }

                            knowledgePoints.forEach { point ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "•",
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = point,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Chat Interface
        ChatInterface(
            userType = if (isAdmin) "Admin" else "Employee",
            sessionId = sessionId
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ChatInterface(userType: String, sessionId: String) {
    val scope = rememberCoroutineScope()

    var messages by remember {
        mutableStateOf(
            emptyList<ChatMessage>()
        )
    }
    var inputText by rememberSaveable { mutableStateOf("") }
    var isLoadingResponse by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Chat messages
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message = message)
            }

            // Show loading indicator when AI is responding
            if (isLoadingResponse) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Card(
                            modifier = Modifier.widthIn(max = 280.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AI is thinking...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Input area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask me anything...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoadingResponse,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isLoadingResponse) {
                            val userMessage = inputText
                            messages = messages + ChatMessage(inputText, true)
                            inputText = ""
                            isLoadingResponse = true

                            // Route both Admin and Employee via webhooks (sessionId envelope for n8n)
                            scope.launch {
                                try {
                                    Log.d("ChatInterface", "Processing message: $userMessage for $userType with sessionId=$sessionId")

                                    val aiResponse = if (userType == "Admin") {
                                        com.teamflux.fluxai.network.postAdminChat(
                                            sessionId = sessionId,
                                            message = userMessage,
                                            username = sessionId
                                        )
                                    } else {
                                        com.teamflux.fluxai.network.postEmployeeChat(
                                            sessionId = sessionId,
                                            message = userMessage,
                                            username = sessionId
                                        )
                                    }

                                    Log.d("ChatInterface", "Received response: ${aiResponse.take(120)}")
                                    messages = messages + ChatMessage(aiResponse, false)
                                } catch (e: Exception) {
                                    // Show error to user; no local fallback
                                    Log.e("ChatInterface", "Chat webhook failed: ${e.message}", e)
                                    val fallback = e.message ?: "Please try again later."
                                    messages = messages + ChatMessage("Sorry, the chat service is currently unavailable. $fallback", false)
                                } finally {
                                    isLoadingResponse = false
                                    // Scroll to bottom
                                    try {
                                        listState.animateScrollToItem(messages.size - 1)
                                    } catch (ex: Exception) {
                                        Log.d("ChatInterface", "Scroll to bottom failed", ex)
                                    }
                                }
                            }
                        }
                    },
                    enabled = inputText.isNotBlank() && !isLoadingResponse
                ) {
                    Icon(
                        Icons.AutoMirrored.Default.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank() && !isLoadingResponse)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                color = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
