package com.teamflux.fluxai.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.teamflux.fluxai.model.EmployeePerformance
import com.teamflux.fluxai.viewmodel.DashboardUiState
import com.teamflux.fluxai.viewmodel.DashboardViewModel
import com.teamflux.fluxai.viewmodel.TeamUi

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
        loading = state.loadingPerformances
    )
    Spacer(Modifier.height(12.dp))
    when {
        state.selectedTeamId == null -> Text("Select a team to view performance")
        state.loadingPerformances -> LoadingRow("Loading team performances...")
        state.performances.isEmpty() -> Text("No performance data for this team.")
        else -> TeamPerformanceSection(state.performances)
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
    Column {
        performances.forEach { perf ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(perf.githubUsername, style = MaterialTheme.typography.titleMedium)
                    Text("Commits: ${perf.commits}")
                    Text("Attendance: ${"%.2f".format(perf.attendanceRate)}")
                    Text("Collaboration: ${"%.2f".format(perf.collaborationScore)}")
                    Text("Productivity: ${"%.2f".format(perf.productivityScore)}")
                    perf.aiEvaluation?.let { ai ->
                        Text("AI Rating: ${ai.overallRating}")
                        Text("Strengths: ${ai.strengths.joinToString()}")
                        Text("Improvements: ${ai.improvements.joinToString()}")
                        Text("Recommendation: ${ai.recommendation}")
                        Text("Trend: ${ai.performanceTrend}")
                    }
                }
            }
        }
    }
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
