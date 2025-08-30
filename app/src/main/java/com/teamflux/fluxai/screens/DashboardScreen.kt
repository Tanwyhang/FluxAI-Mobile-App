package com.teamflux.fluxai.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teamflux.fluxai.model.AIEvaluation
import com.teamflux.fluxai.model.EmployeePerformance
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Admin View", "Employee View")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Dashboard",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
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

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> AdminDashboard()
            1 -> EmployeeDashboard()
        }
    }
}

@Composable
fun AdminDashboard() {
    val employeePerformances = remember {
        getSampleEmployeePerformances()
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Team Overview Card
        item {
            TeamOverviewCard()
        }

        // Employee Performance Section Header
        item {
            Text(
                text = "Employee Performance Analytics",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Employee Performance Cards
        items(employeePerformances) { employee ->
            EmployeePerformanceCard(employee = employee)
        }
    }
}

@Composable
fun TeamOverviewCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Team Performance Overview",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Use a grid layout for better space distribution
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    KPIItem("Members", "8", Icons.Default.Face)
                    KPIItem("Performance", "8.7/10", Icons.Default.ThumbUp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    KPIItem("Projects", "3", Icons.Default.Build)
                    KPIItem("Completion", "92%", Icons.Default.CheckCircle)
                }
            }
        }
    }
}

@Composable
fun KPIItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(120.dp) // Fixed width to prevent clipping
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
fun EmployeePerformanceCard(employee: EmployeePerformance) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Employee Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = employee.name.split(" ").map { it.first() }.take(2).joinToString(""),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = employee.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = employee.role,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "@${employee.githubUsername}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Performance Trend Icon
                Icon(
                    when (employee.aiEvaluation.performanceTrend) {
                        "Improving" -> Icons.Default.KeyboardArrowUp
                        "Declining" -> Icons.Default.KeyboardArrowDown
                        else -> Icons.Default.ThumbUp
                    },
                    contentDescription = "Trend",
                    tint = when (employee.aiEvaluation.performanceTrend) {
                        "Improving" -> Color(0xFF4CAF50)
                        "Declining" -> Color(0xFFF44336)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // KPI Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EmployeeKPIItem("Commits", "${employee.commitsThisWeek}", "this week")
                EmployeeKPIItem("LOC", "${employee.linesOfCode/1000}k", "total")
                EmployeeKPIItem("Tasks", "${employee.tasksCompleted}/${employee.totalTasks}", "completed")
                EmployeeKPIItem("Attendance", "${(employee.attendanceRate * 100).toInt()}%", "rate")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Commit Activity Visualization
            Text(
                text = "Commit Activity (Last 7 Days)",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            CommitActivityChart(commitHistory = employee.commitHistory)

            Spacer(modifier = Modifier.height(16.dp))

            // Performance Scores
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ScoreIndicator("Code Quality", employee.codeQualityScore)
                ScoreIndicator("Collaboration", employee.collaborationScore)
                ScoreIndicator("Productivity", employee.productivityScore)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI Evaluation
            AIEvaluationCard(evaluation = employee.aiEvaluation)
        }
    }
}

@Composable
fun EmployeeKPIItem(label: String, value: String, subtitle: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CommitActivityChart(commitHistory: List<Int>) {
    val maxCommits = max(commitHistory.maxOrNull() ?: 1, 1)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(horizontal = 8.dp)
    ) {
        val barWidth = size.width / commitHistory.size
        val chartHeight = size.height - 20.dp.toPx()

        commitHistory.forEachIndexed { index, commits ->
            val barHeight = (commits.toFloat() / maxCommits) * chartHeight
            val x = index * barWidth + barWidth * 0.2f
            val barActualWidth = barWidth * 0.6f

            // Draw bar
            drawRect(
                color = Color(0xFF87CEEB),
                topLeft = Offset(x, size.height - barHeight),
                size = androidx.compose.ui.geometry.Size(barActualWidth, barHeight)
            )

            // Draw value on top if commits > 0
            if (commits > 0) {
                drawContext.canvas.nativeCanvas.drawText(
                    commits.toString(),
                    x + barActualWidth / 2,
                    size.height - barHeight - 10.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 24f
                    }
                )
            }
        }
    }

    // Days labels
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
            Text(
                text = day,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ScoreIndicator(label: String, score: Double) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(60.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { (score / 10.0).toFloat() },
                modifier = Modifier.fillMaxSize(),
                color = when {
                    score >= 8.0 -> Color(0xFF4CAF50)
                    score >= 6.0 -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                },
                strokeWidth = 6.dp,
                trackColor = MaterialTheme.colorScheme.outline
            )
            Text(
                text = String.format("%.1f", score),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AIEvaluationCard(evaluation: AIEvaluation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "AI Evaluation",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Performance Evaluation",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))

                // Rating Badge
                Surface(
                    color = when (evaluation.overallRating) {
                        "Excellent" -> Color(0xFF4CAF50)
                        "Good" -> Color(0xFF2196F3)
                        "Average" -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = evaluation.overallRating,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Strengths
            if (evaluation.strengths.isNotEmpty()) {
                Text(
                    text = "Strengths:",
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                evaluation.strengths.forEach { strength ->
                    Text(
                        text = "• $strength",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 8.dp, bottom = 1.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Improvements
            if (evaluation.improvements.isNotEmpty()) {
                Text(
                    text = "Areas for Improvement:",
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = Color(0xFFFF9800),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                evaluation.improvements.forEach { improvement ->
                    Text(
                        text = "• $improvement",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 8.dp, bottom = 1.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Recommendation
            Text(
                text = "Recommendation:",
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = evaluation.recommendation,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun EmployeeDashboard() {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "My Performance",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Commits This Week: 24", color = MaterialTheme.colorScheme.onSurface)
                    Text("Lines of Code: 1,250", color = MaterialTheme.colorScheme.onSurface)
                    Text("Tasks Completed: 8/10", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "My Attendance",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Status: Present", color = MaterialTheme.colorScheme.onSurface)
                    Text("This Month: 22/23 days", color = MaterialTheme.colorScheme.onSurface)
                    Text("Overall: 96%", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Personal AI Insights",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Great progress on React components", color = MaterialTheme.colorScheme.onSurface)
                    Text("• Consider more unit testing", color = MaterialTheme.colorScheme.onSurface)
                    Text("• Strong collaboration skills shown", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

fun getSampleEmployeePerformances(): List<EmployeePerformance> {
    return listOf(
        EmployeePerformance(
            id = "1",
            name = "John Doe",
            role = "Frontend Developer",
            githubUsername = "johndoe",
            commitsThisWeek = 24,
            commitsLastWeek = 18,
            linesOfCode = 12500,
            tasksCompleted = 8,
            totalTasks = 10,
            attendanceRate = 0.96,
            codeQualityScore = 8.7,
            collaborationScore = 9.2,
            productivityScore = 8.9,
            commitHistory = listOf(3, 5, 2, 4, 6, 2, 2),
            aiEvaluation = AIEvaluation(
                overallRating = "Excellent",
                strengths = listOf("Strong React expertise", "Consistent code quality", "Good documentation"),
                improvements = listOf("Could increase unit test coverage"),
                recommendation = "Consider mentoring junior developers to leverage expertise",
                performanceTrend = "Improving"
            )
        ),
        EmployeePerformance(
            id = "2",
            name = "Jane Smith",
            role = "Backend Developer",
            githubUsername = "janesmith",
            commitsThisWeek = 19,
            commitsLastWeek = 22,
            linesOfCode = 15200,
            tasksCompleted = 9,
            totalTasks = 10,
            attendanceRate = 0.98,
            codeQualityScore = 9.1,
            collaborationScore = 8.8,
            productivityScore = 9.3,
            commitHistory = listOf(4, 3, 5, 2, 3, 1, 1),
            aiEvaluation = AIEvaluation(
                overallRating = "Excellent",
                strengths = listOf("Excellent API design", "Strong problem-solving", "Reliable delivery"),
                improvements = listOf("Could improve code comments"),
                recommendation = "Perfect candidate for technical lead role",
                performanceTrend = "Stable"
            )
        ),
        EmployeePerformance(
            id = "3",
            name = "Mike Johnson",
            role = "UI/UX Designer",
            githubUsername = "mikejohnson",
            commitsThisWeek = 12,
            commitsLastWeek = 15,
            linesOfCode = 4800,
            tasksCompleted = 6,
            totalTasks = 8,
            attendanceRate = 0.92,
            codeQualityScore = 7.8,
            collaborationScore = 9.5,
            productivityScore = 8.2,
            commitHistory = listOf(2, 2, 3, 1, 2, 1, 1),
            aiEvaluation = AIEvaluation(
                overallRating = "Good",
                strengths = listOf("Creative design solutions", "Great team collaboration", "User-focused approach"),
                improvements = listOf("Increase commit frequency", "More technical documentation"),
                recommendation = "Explore more front-end development to enhance skill set",
                performanceTrend = "Improving"
            )
        ),
        EmployeePerformance(
            id = "4",
            name = "Sarah Wilson",
            role = "Product Manager",
            githubUsername = "sarahwilson",
            commitsThisWeek = 8,
            commitsLastWeek = 10,
            linesOfCode = 2100,
            tasksCompleted = 10,
            totalTasks = 10,
            attendanceRate = 1.0,
            codeQualityScore = 7.2,
            collaborationScore = 9.8,
            productivityScore = 9.5,
            commitHistory = listOf(1, 2, 1, 1, 2, 1, 0),
            aiEvaluation = AIEvaluation(
                overallRating = "Excellent",
                strengths = listOf("Outstanding project management", "Excellent communication", "Strategic thinking"),
                improvements = listOf("Technical knowledge could be deeper"),
                recommendation = "Continue leading cross-functional initiatives",
                performanceTrend = "Stable"
            )
        ),
        EmployeePerformance(
            id = "5",
            name = "Alex Chen",
            role = "Full Stack Developer",
            githubUsername = "alexchen",
            commitsThisWeek = 15,
            commitsLastWeek = 25,
            linesOfCode = 18900,
            tasksCompleted = 7,
            totalTasks = 12,
            attendanceRate = 0.89,
            codeQualityScore = 6.8,
            collaborationScore = 7.2,
            productivityScore = 7.1,
            commitHistory = listOf(2, 1, 4, 3, 2, 2, 1),
            aiEvaluation = AIEvaluation(
                overallRating = "Average",
                strengths = listOf("Versatile skill set", "Quick learner"),
                improvements = listOf("Focus on code quality", "Improve attendance", "Better task prioritization"),
                recommendation = "Provide additional mentoring and set clearer goals",
                performanceTrend = "Declining"
            )
        )
    )
}
