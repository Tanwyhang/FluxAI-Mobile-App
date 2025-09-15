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
        ChatInterface(if (isAdmin) "Admin" else "Employee")
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ChatInterface(userType: String) {
    val scope = rememberCoroutineScope()

    var messages by remember {
        mutableStateOf(
            if (userType == "Admin") {
                listOf(
                    ChatMessage("Hello! I'm your AI assistant for team management. I can provide insights on team performance, analytics, and management recommendations. How can I help you today?", false),
                    ChatMessage("What insights do you have for my team?", true),
                    ChatMessage("Based on recent activity, your team shows strong collaboration. I recommend focusing on code review efficiency to boost productivity further.", false)
                )
            } else {
                listOf(
                    ChatMessage("Hello! I'm your AI assistant for personal development and productivity. I can help with coding guidance, career advice, and skill improvement. What can I assist you with?", false),
                    ChatMessage("How can I improve my coding skills?", true),
                    ChatMessage("Based on your recent commits, I recommend focusing on test-driven development. Here are some resources and practices that can help you improve...", false)
                )
            }
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

                            // Call webhook for employee chat (no fallbacks). Admin continues using local fallback path.
                            scope.launch {
                                try {
                                    Log.d("ChatInterface", "Processing message: $userMessage")

                                    val aiResponse = if (userType == "Admin") {
                                        // Admin path may use older behavior
                                        generateFallbackChatResponse(userMessage, userType)
                                    } else {
                                        // Employee: always call webhook endpoint via postEmployeeChat (no fallback)
                                        com.teamflux.fluxai.network.postEmployeeChat(message = userMessage)
                                    }

                                    Log.d("ChatInterface", "Received response: $aiResponse")
                                    messages = messages + ChatMessage(aiResponse, false)
                                } catch (e: Exception) {
                                    // No fallback for employee; show error to user and log
                                    Log.e("ChatInterface", "Chat webhook failed: ${e.message}", e)
                                    messages = messages + ChatMessage("Sorry, the chat service is currently unavailable. ${e.message ?: "Please try again later."}", false)
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

// Helper function to generate fallback responses
private fun generateFallbackChatResponse(message: String, userType: String): String {
    val lowercaseMessage = message.lowercase()

    return if (userType == "Admin") {
        when {
            lowercaseMessage.contains("performance") || lowercaseMessage.contains("analytics") ->
                "Team performance metrics show positive trends. Current velocity and code quality indicators suggest good productivity levels across team members."
            lowercaseMessage.contains("team") || lowercaseMessage.contains("collaboration") ->
                "Team dynamics analysis indicates strong collaboration patterns. Consider implementing regular knowledge-sharing sessions to further enhance team cohesion."
            lowercaseMessage.contains("productivity") || lowercaseMessage.contains("efficiency") ->
                "Productivity metrics are within expected ranges. Focus areas include optimizing code review cycles and reducing deployment friction."
            lowercaseMessage.contains("resource") || lowercaseMessage.contains("planning") ->
                "Resource allocation appears balanced based on current project demands. Monitor sprint capacity to ensure sustainable team velocity."
            lowercaseMessage.contains("quality") || lowercaseMessage.contains("review") ->
                "Code quality standards are being maintained well. Consider automated quality gates to further streamline the review process."
            lowercaseMessage.contains("budget") || lowercaseMessage.contains("cost") ->
                "Current project costs are tracking within budget. Consider optimizing development processes to improve cost efficiency."
            lowercaseMessage.contains("timeline") || lowercaseMessage.contains("deadline") ->
                "Project timelines appear manageable with current team capacity. Regular milestone reviews can help maintain schedule adherence."
            else ->
                "I can help with team analytics, performance insights, resource planning, and management recommendations. What specific area interests you?"
        }
    } else {
        when {
            lowercaseMessage.contains("performance") || lowercaseMessage.contains("progress") ->
                "Your development activity shows consistent patterns. Focus on maintaining code quality while gradually increasing your contribution velocity."
            lowercaseMessage.contains("skill") || lowercaseMessage.contains("learn") ->
                "Based on current trends, consider exploring advanced testing frameworks and architectural patterns to enhance your technical toolkit."
            lowercaseMessage.contains("career") || lowercaseMessage.contains("growth") ->
                "Your consistent contributions demonstrate growth potential. Consider taking on code review responsibilities and mentoring opportunities."
            lowercaseMessage.contains("productivity") || lowercaseMessage.contains("efficiency") ->
                "Focus on time management and breaking down complex tasks into smaller, manageable commits for better tracking and collaboration."
            lowercaseMessage.contains("team") || lowercaseMessage.contains("collaboration") ->
                "Effective team collaboration includes active participation in code reviews and clear communication in pull requests."
            lowercaseMessage.contains("code") || lowercaseMessage.contains("programming") ->
                "Good coding practices include writing clean, readable code, comprehensive testing, and thorough documentation. What specific area would you like to improve?"
            lowercaseMessage.contains("project") || lowercaseMessage.contains("task") ->
                "Break down complex projects into smaller, manageable tasks. Use version control effectively and communicate progress regularly with your team."
            else ->
                "I'm here to help with your professional development, coding practices, and career growth. What would you like to focus on?"
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
                modifier = Modifier.padding(12.dp),
                color = if (message.isUser) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
