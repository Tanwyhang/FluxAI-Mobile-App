package com.teamflux.fluxai.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamflux.fluxai.viewmodel.AuthViewModel
import com.teamflux.fluxai.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectionScreen(
    onAdminSelected: (String, String) -> Unit,
    onEmployeeSelected: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onErrorDismissed: () -> Unit,
    onSignOut: () -> Unit,
    onUseOtherAccount: (() -> Unit)? = null // Add new parameter with default null for backwards compatibility
) {
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.uiState.collectAsState()

    // Load profile for githubUsername
    val userViewModel: UserViewModel = viewModel()
    val profileState by userViewModel.uiState.collectAsStateWithLifecycle()

    val username = profileState.profile?.githubUsername
        ?: authState.currentUser?.email?.substringBefore("@")
        ?: "User"

    // Show error message if exists
    errorMessage?.let { error ->
        LaunchedEffect(error) {
            // Handle error display - could show a snackbar or alert dialog
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
            text = "Logging in as:",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp)
            )
        } else {
            // Role selection cards
            Card(
                onClick = {
                    // For now, pass empty strings - this will need to be handled differently
                    // since we need username and phone input for admin
                    onAdminSelected("", "")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Admin",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Admin",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Team management & analytics",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                onClick = {
                    onEmployeeSelected()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Face,
                        contentDescription = "Team Member",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Team Member",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Personal development & tasks",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Select your role to continue",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

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
        TextButton(
            onClick = onSignOut
        ) {
            Text("Sign Out")
        }
    }
}
