package com.teamflux.fluxai.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.teamflux.fluxai.model.EmployeePerformance
import com.teamflux.fluxai.viewmodel.DashboardUiState
import com.teamflux.fluxai.viewmodel.DashboardViewModel
import com.teamflux.fluxai.viewmodel.TeamUi
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isSyncing by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        HeaderBar(isSyncing = isSyncing, onRefresh = {
            isSyncing = true
            viewModel.refreshTeamPerformances()
            isSyncing = false
        })

        when (uiState) {
            is DashboardUiState.Loading -> LoadingContent()
            is DashboardUiState.Error -> ErrorContent(
                message = (uiState as DashboardUiState.Error).message,
                onRetry = { viewModel.refreshTeamPerformances() }
            )
            is DashboardUiState.Team -> {
                val performances = (uiState as DashboardUiState.Team).performances
                TeamPerformanceSection(performances)
            }
            is DashboardUiState.Admin -> {
                val adminState = uiState as DashboardUiState.Admin
                AdminTeamSelectionSection(
                    state = adminState,
                    onSelectTeam = { viewModel.selectTeam(it) }
                )
            }
        }
    }
}

@Composable
private fun AdminTeamSelectionSection(
    state: DashboardUiState.Admin,
    onSelectTeam: (String) -> Unit
) {
    TeamSelector(
        teams = state.teams,
        selectedTeamId = state.selectedTeamId,
        onSelect = onSelectTeam,
        loading = state.loadingPerformances && state.selectedTeamId == null
    )
    Spacer(Modifier.height(12.dp))
    when {
        state.selectedTeamId == null -> Text("Select a team to view performance")
        state.performances.isEmpty() && state.loadingPerformances -> LoadingRow("Loading team performances...")
        state.performances.isEmpty() && !state.loadingPerformances -> Text("No performance data for this team.")
        else -> {
            TeamPerformanceSection(state.performances)
            if (state.loadingPerformances) {
                Spacer(Modifier.height(8.dp))
                LoadingRow("Fetching remaining teammates...")
            }
        }
    }
}

@Composable
private fun TeamPerformanceSection(performances: List<EmployeePerformance>) {
    if (performances.isEmpty()) {
        Text("No performance data.")
    } else {
        EmployeePerformanceList(performances)
    }
}

@Composable
private fun LoadingRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(12.dp))
        Text(text)
    }
}

@Composable
private fun HeaderBar(isSyncing: Boolean, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Dashboard",
            style = MaterialTheme.typography.headlineMedium
        )
        Button(onClick = onRefresh, enabled = !isSyncing) {
            Text(if (isSyncing) "Syncing..." else "Refresh")
        }
    }
}

@Composable
private fun TeamSelector(
    teams: List<TeamUi>,
    selectedTeamId: String?,
    onSelect: (String) -> Unit,
    loading: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = teams.firstOrNull { it.id == selectedTeamId }?.name ?: "Select Team"
    Box {
        OutlinedButton(onClick = { expanded = true }, enabled = !loading) {
            Text(selectedName)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            teams.forEach { team ->
                DropdownMenuItem(
                    text = { Text(team.name) },
                    onClick = {
                        expanded = false
                        onSelect(team.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun EmployeePerformanceList(performances: List<EmployeePerformance>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(performances, key = { it.id }) { perf ->
            PerformanceCard(perf)
        }
    }
}

@Composable
private fun PerformanceCard(perf: EmployeePerformance) {
    val colors = MaterialTheme.colorScheme
    val borderColor = colors.outlineVariant.copy(alpha = 0.6f)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Column {
                Text(
                    perf.githubUsername.ifBlank { "(unknown)" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
                Spacer(Modifier.height(4.dp))
                // Use Material3 HorizontalDivider (remove deprecated Divider)
                HorizontalDivider(color = borderColor, thickness = 1.dp)
            }

            // Metrics row with visual contrast
            MetricsRow(
                attendance = perf.attendanceRate,
                collaboration = perf.collaborationScore,
                productivity = perf.productivityScore
            )

            // AI Evaluation (if present)
            perf.aiEvaluation?.let { ai ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledValue("AI Rating", ai.overallRating, colors.primary)
                    if (ai.strengths.isNotEmpty()) BulletList("Strengths", ai.strengths, colors.tertiary)
                    if (ai.improvements.isNotEmpty()) BulletList("Improvements", ai.improvements, colors.error)
                    if (ai.recommendation.isNotBlank()) LabeledValue("Recommendation", ai.recommendation, colors.secondary)
                    if (ai.performanceTrend.isNotBlank()) LabeledValue("Trend", ai.performanceTrend, colors.primary)
                }
            }

            // Commit chart or empty
            if (perf.commits == 0) {
                Surface(
                    tonalElevation = 0.dp,
                    color = colors.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "empty",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            } else {
                CommitChart(commitDates = perf.commitDates)
            }
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String, accent: Color) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = accent)
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

@Composable
private fun BulletList(label: String, items: List<String>, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = accent)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items.forEach { line ->
                Row(verticalAlignment = Alignment.Top) {
                    Text("•", color = accent, modifier = Modifier.padding(end = 6.dp))
                    Text(line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun MetricsRow(attendance: Double, collaboration: Double, productivity: Double) {
    val cs = MaterialTheme.colorScheme
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricPill(
            title = "Attendance",
            value = "${"%.0f".format(attendance * 100)}%",
            container = cs.primaryContainer,
            content = cs.onPrimaryContainer
        )
        MetricPill(
            title = "Collab",
            value = "%.1f".format(collaboration),
            container = cs.secondaryContainer,
            content = cs.onSecondaryContainer
        )
        MetricPill(
            title = "Productivity",
            value = "%.1f".format(productivity),
            container = cs.tertiaryContainer,
            content = cs.onTertiaryContainer
        )
    }
}

@Composable
private fun MetricPill(title: String, value: String, container: Color, content: Color) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    Surface(
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.small,
        tonalElevation = 0.dp,
        modifier = Modifier.border(1.dp, outline.copy(alpha = 0.6f), MaterialTheme.shapes.small)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.Start) {
            Text(title, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun CommitChart(commitDates: List<String>, height: Dp = 140.dp) {
    val cs = MaterialTheme.colorScheme
    val series = remember(commitDates) { buildCumulativeSeries(commitDates) }
    val maxVal = remember(series) { max(1, series.maxOrNull() ?: 1) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(6.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .background(cs.surfaceContainerLow)
        ) {
            val barCount = 30
            val w = size.width
            val h = size.height
            val spacing = 4f
            val totalSpacing = spacing * (barCount + 1)
            val barWidth = (w - totalSpacing) / barCount

            // subtle vertical grid lines every 5 bars
            for (i in 0..barCount step 5) {
                val x = spacing + i * (barWidth + spacing)
                drawLine(
                    color = cs.outlineVariant.copy(alpha = 0.25f),
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1f
                )
            }

            // Baseline
            drawLine(
                color = cs.outlineVariant,
                start = Offset(0f, h - 1f),
                end = Offset(w, h - 1f),
                strokeWidth = 1f
            )

            // Bars
            series.forEachIndexed { idx, v ->
                val left = spacing + idx * (barWidth + spacing)
                val top = h - (h * (v.toFloat() / maxVal.toFloat())).coerceAtMost(h)
                drawRect(
                    color = cs.primary,
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(barWidth, h - top)
                )
            }

            // Optional line overlay for trend
            val path = Path()
            series.forEachIndexed { idx, v ->
                val xCenter = spacing + idx * (barWidth + spacing) + barWidth / 2f
                val y = h - (h * (v.toFloat() / maxVal.toFloat())).coerceAtMost(h)
                if (idx == 0) path.moveTo(xCenter, y) else path.lineTo(xCenter, y)
            }
            drawPath(
                path = path,
                color = cs.secondary,
                style = Stroke(width = 2f, cap = StrokeCap.Round)
            )
        }
    }
}

private fun buildCumulativeSeries(commitDates: List<String>): List<Int> {
    if (commitDates.isEmpty()) return List(30) { 0 }
    val dayMs = 86_400_000L
    val epochs = commitDates.mapNotNull { it.toLongOrNull() }
    if (epochs.isEmpty()) return List(30) { 0 }
    val days = epochs.map { it / dayMs }
    val maxDay = days.maxOrNull() ?: 0L
    val minDay = maxDay - 29
    val daily = IntArray(30)
    days.forEach { d ->
        val idx = (d - minDay).toInt()
        if (idx in 0 until 30) daily[idx]++
    }
    val cumulative = IntArray(30)
    var running = 0
    for (i in 0 until 30) {
        running += daily[i]
        cumulative[i] = running
    }
    return cumulative.toList()
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Error: $message", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}