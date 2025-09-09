package com.teamflux.fluxai.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeTeamJoinScreen(
    onTeamJoined: () -> Unit,
    onSkipForNow: () -> Unit, // New parameter for skip functionality
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.uiState.collectAsState()

    var teamId by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }

    val username = authState.currentUser?.displayName ?: authState.currentUser?.email?.substringBefore("@") ?: "User"

    val roles = listOf(
        "Frontend Developer",
        "Backend Developer",
        "Full Stack Developer",
        "UI/UX Designer",
        "DevOps Engineer",
        "QA Engineer",
        "Product Manager",
        "Data Analyst",
        "Mobile Developer"
    )

    // Handle successful team join - redirect to team screen
    LaunchedEffect(authState.selectedRole, authState.isLoading) {
        // Check if user has just become a team member and is no longer loading
        if (authState.selectedRole == "team_member" && !authState.isLoading && !authState.isFirstTimeUser) {
            onTeamJoined()
        }
    }

    // Show error message if exists
    authState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            // Error will be displayed via Snackbar in parent
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Welcome message
        Text(
            text = "Welcome $username!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Join Your Team",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Please enter your team details to get started",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Team joining form
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Groups,
                        contentDescription = "Team",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Team Information",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Team ID input
                OutlinedTextField(
                    value = teamId,
                    onValueChange = { teamId = it },
                    label = { Text("Team ID") },
                    placeholder = { Text("Enter your team ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Role selection
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = role,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Your Role") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        roles.forEach { roleOption ->
                            DropdownMenuItem(
                                text = { Text(roleOption) },
                                onClick = {
                                    role = roleOption
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Phone number
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    placeholder = { Text("+1234567890") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Join team button
        Button(
            onClick = {
                if (teamId.isNotBlank() && role.isNotBlank() && phone.isNotBlank()) {
                    authViewModel.createTeamMemberProfile(
                        teamId = teamId.trim(),
                        role = role,
                        githubProfileLink = "", // Pass empty string instead of removing parameter
                        phone = phone.trim()
                    )
                }
            },
            enabled = teamId.isNotBlank() && role.isNotBlank() && phone.isNotBlank() && !authState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (authState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = "Join Team",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Skip for now button
        OutlinedButton(
            onClick = onSkipForNow,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(
                text = "Skip for now",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Helper text for skip option
        Text(
            text = "You can join a team later from the settings menu",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
