package com.teamflux.fluxai.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teamflux.fluxai.viewmodel.AuthViewModel
import com.teamflux.fluxai.viewmodel.TeamViewModel

@Composable
fun AdminTeamSetupScreen(
    onTeamCreated: () -> Unit,
    onSkip: () -> Unit,
    authViewModel: AuthViewModel
) {
    val teamViewModel: TeamViewModel = viewModel()
    val authState by authViewModel.uiState.collectAsState()
    val teamState by teamViewModel.uiState.collectAsState()

    var teamName by rememberSaveable { mutableStateOf("") }
    var showCreateForm by rememberSaveable { mutableStateOf(false) }

    // Check if admin already has teams
    LaunchedEffect(authState.currentUser?.uid) {
        authState.currentUser?.uid?.let { adminId ->
            teamViewModel.loadTeams(adminId)
        }
    }

    // Navigate to main if teams exist
    LaunchedEffect(teamState.teams) {
        if (teamState.teams.isNotEmpty()) {
            onTeamCreated()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Group,
            contentDescription = "Team Setup",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome, Admin!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Let's get you started by creating your first team. You can manage team members, track performance, and collaborate effectively.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (!showCreateForm) {
            // Initial buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { showCreateForm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Create Your First Team",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Skip for now")
                }
            }
        } else {
            // Team creation form
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Create New Team",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = teamName,
                        onValueChange = { teamName = it },
                        label = { Text("Team Name") },
                        placeholder = { Text("e.g., Development Team, Marketing Team") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showCreateForm = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (teamName.isNotBlank()) {
                                    authState.currentUser?.uid?.let { adminId ->
                                        teamViewModel.createTeam(teamName.trim(), adminId)
                                    }
                                }
                            },
                            enabled = teamName.isNotBlank() && !teamState.isLoading,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (teamState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Create Team")
                            }
                        }
                    }
                }
            }
        }

        // Error handling
        teamState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
