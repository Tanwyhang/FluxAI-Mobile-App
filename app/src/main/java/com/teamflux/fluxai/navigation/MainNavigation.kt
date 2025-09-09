package com.teamflux.fluxai.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.teamflux.fluxai.screens.AttendanceScreen
import com.teamflux.fluxai.screens.ChatbotScreen
import com.teamflux.fluxai.screens.DashboardScreen
import com.teamflux.fluxai.screens.SettingsScreen
import com.teamflux.fluxai.screens.TeamScreen
import com.teamflux.fluxai.viewmodel.AuthViewModel
import com.teamflux.fluxai.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation(
    themeViewModel: ThemeViewModel,
    authViewModel: AuthViewModel // Accept AuthViewModel as parameter instead of creating new instance
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val authState by authViewModel.uiState.collectAsState()

    // Effect to handle navigation after sign-out - this will trigger AppNavigation
    LaunchedEffect(authState.isLoggedIn) {
        if (!authState.isLoggedIn) {
            // Force trigger navigation in AppNavigation by clearing everything
            return@LaunchedEffect
        }
    }

    // Effect to monitor sign-out and trigger immediate action
    LaunchedEffect(authState) {
        if (!authState.isLoggedIn) {
            // This will cause AppNavigation to handle the redirect
            return@LaunchedEffect
        }
    }

    // Determine navigation items based on user role
    val bottomNavItems = when (authState.selectedRole) {
        "admin" -> listOf(
            BottomNavItem.Dashboard,
            BottomNavItem.Team,
            BottomNavItem.Attendance,
            BottomNavItem.Chatbot,
            BottomNavItem.Settings
        )
        "team_member" -> listOf(
            BottomNavItem.Dashboard,
            BottomNavItem.Team, // Add Team tab for employees (view-only)
            BottomNavItem.Attendance,
            BottomNavItem.Chatbot,
            BottomNavItem.Settings
        )
        else -> listOf(BottomNavItem.Dashboard, BottomNavItem.Chatbot, BottomNavItem.Settings)
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp // Flat design - no elevation
            ) {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Dashboard.route) {
                DashboardScreen()
            }
            composable(BottomNavItem.Chatbot.route) {
                ChatbotScreen(authViewModel = authViewModel) // Pass AuthViewModel to fix scoping
            }
            composable(BottomNavItem.Team.route) {
                TeamScreen(authViewModel = authViewModel) // Pass AuthViewModel to TeamScreen
            }
            composable(BottomNavItem.Attendance.route) {
                AttendanceScreen(authViewModel = authViewModel)
            }
            composable(BottomNavItem.Settings.route) {
                SettingsScreen(
                    themeViewModel = themeViewModel,
                    onSignOut = { authViewModel.signOut() }
                )
            }
        }
    }
}
