package com.teamflux.fluxai.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teamflux.fluxai.viewmodel.AuthViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamflux.fluxai.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleChoiceScreen(
    availableRoles: List<String>,
    onRoleSelected: (String) -> Unit,
    onSignOut: () -> Unit,
    onUseOtherAccount: (() -> Unit)? = null // Add new parameter for account switching
) {
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.uiState.collectAsState()

    // Load profile for githubUsername
    val userViewModel: UserViewModel = viewModel()
    val profileState by userViewModel.uiState.collectAsStateWithLifecycle()

    val username = profileState.profile?.githubUsername
        ?: authState.currentUser?.email?.substringBefore("@")
        ?: "User"

    var selectedRole by rememberSaveable { mutableStateOf<String?>(null) }

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
            text = "Logging in as:",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Role selection cards
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            availableRoles.forEach { role ->
                RoleCard(
                    role = role,
                    isSelected = selectedRole == role,
                    onRoleClick = { selectedRole = role }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Continue button
        Button(
            onClick = {
                selectedRole?.let { onRoleSelected(it) }
            },
            enabled = selectedRole != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Use Other Account button for testing
        OutlinedButton(
            onClick = { onUseOtherAccount?.invoke() ?: onSignOut() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Use Other Account")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sign out button
        TextButton(onClick = onSignOut) {
            Text("Sign Out")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleCard(
    role: String,
    isSelected: Boolean,
    onRoleClick: () -> Unit
) {
    val (icon, title, description) = when (role) {
        "admin" -> Triple(
            Icons.Default.AdminPanelSettings,
            "Admin",
            "Manage teams, view performance metrics, and oversee projects"
        )
        "team_member" -> Triple(
            Icons.Default.Group,
            "Team Member",
            "Collaborate with your team, track your progress, and contribute to projects"
        )
        else -> Triple(
            Icons.Default.Group,
            role.replaceFirstChar { it.uppercase() },
            "Access your role-specific features"
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        onClick = onRoleClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(48.dp),
                tint = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
