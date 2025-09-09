package com.teamflux.fluxai.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teamflux.fluxai.viewmodel.ThemeViewModel
import com.teamflux.fluxai.viewmodel.UserViewModel

@Composable
fun SettingsScreen(
    themeViewModel: ThemeViewModel,
    onSignOut: () -> Unit = {},
    userViewModel: UserViewModel = viewModel()
) {
    val isDarkTheme by themeViewModel.isDarkTheme.collectAsStateWithLifecycle()
    val profileState by userViewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()) // Add scrolling
    ) {
        Text(
            text = "Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Profile Info Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Flat design
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Profile",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Profile Information",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline
                )

                // Use Firestore-backed profile data
                val profile = profileState.profile
                val username = profile?.githubUsername ?: profile?.displayName ?: "-"
                val email = profile?.email ?: "-"
                val github = profile?.githubProfileUrl
                    ?: profile?.githubUsername?.let { "https://github.com/$it" }
                    ?: "-"

                if (profileState.isLoading) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else {
                    ProfileInfoRow("Username", username)
                    ProfileInfoRow("Email", email)
                    ProfileInfoRow("GitHub", github)

                    profileState.error?.let { err ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Theme Settings Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Flat design
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Theme",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Appearance",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dark Theme",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { themeViewModel.setTheme(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }

        // Add spacer before sign out button for better spacing
        Spacer(modifier = Modifier.height(24.dp))

        // Sign Out Button
        Button(
            onClick = { onSignOut() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = "Sign Out",
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = "Sign Out",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onError
            )
        }

        // Add bottom padding to ensure button is above navigation bar
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SettingsItem(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = title,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 16.sp
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Navigate",
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
