package com.teamflux.fluxai.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.teamflux.fluxai.model.ChatMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Admin Chat", "Employee Chat")

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "AI Chatbot",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title) },
                    selected = selectedTab == index,
                    onClick = { selectedTab = index }
                )
            }
        }

        when (selectedTab) {
            0 -> ChatInterface("Admin")
            1 -> ChatInterface("Employee")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInterface(userType: String) {
    var messages by remember {
        mutableStateOf(listOf(
            ChatMessage("Hello! I'm your AI assistant. How can I help you today?", false),
            ChatMessage("What insights do you have for my team?", true),
            ChatMessage("Based on recent activity, your team shows strong collaboration. I recommend focusing on code review efficiency to boost productivity further.", false)
        ))
    }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

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
        }

        // Input area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Flat design
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
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            messages = messages + ChatMessage(inputText, true)
                            val userMessage = inputText
                            inputText = ""

                            // Simulate AI response
                            scope.launch {
                                kotlinx.coroutines.delay(1000)
                                val aiResponse = generateAIResponse(userMessage, userType)
                                messages = messages + ChatMessage(aiResponse, false)
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        }
                    }
                ) {
                    Icon(
                        Icons.AutoMirrored.Default.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.primary
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
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = if (message.isUser)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun generateAIResponse(userMessage: String, userType: String): String {
    // Simple keyword-based responses for demo purposes
    val keywordResponses = mapOf(
        "performance" to "I've analyzed recent performance metrics and identified key improvement areas.",
        "team" to "Your team dynamics show strong collaboration patterns.",
        "commits" to "Commit activity indicates consistent development velocity.",
        "productivity" to "Productivity metrics suggest optimized workflow processes."
    )

    // Check for keywords in user message
    val matchedResponse = keywordResponses.entries.find {
        userMessage.lowercase().contains(it.key)
    }?.value

    val responses = if (userType == "Admin") {
        listOf(
            matchedResponse ?: "Based on team analytics, I can see improved collaboration patterns. Would you like specific recommendations?",
            "Your team's productivity metrics show positive trends. I can provide detailed insights on individual performance.",
            "I've analyzed the latest commit patterns and attendance data. Here are my observations...",
            "Team efficiency has increased by 12% this month. Key factors include better code review processes."
        )
    } else {
        listOf(
            matchedResponse ?: "Your personal performance shows consistent growth. Keep up the excellent work!",
            "I notice you've been very active with commits lately. Consider balancing with more code reviews.",
            "Your attendance record is exemplary. This consistency reflects well on your professional development.",
            "Based on your recent work, I recommend exploring advanced testing frameworks for your projects."
        )
    }

    return responses.random()
}
