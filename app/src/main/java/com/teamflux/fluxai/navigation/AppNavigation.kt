package com.teamflux.fluxai.navigation

import androidx.activity.ComponentActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.teamflux.fluxai.screens.AdminTeamSetupScreen
import com.teamflux.fluxai.screens.EmployeeTeamJoinScreen
import com.teamflux.fluxai.screens.LoginScreen
import com.teamflux.fluxai.screens.RoleChoiceScreen
import com.teamflux.fluxai.screens.RoleSelectionScreen
import com.teamflux.fluxai.viewmodel.AuthViewModel
import com.teamflux.fluxai.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    themeViewModel: ThemeViewModel,
    authViewModel: AuthViewModel,
    activity: ComponentActivity
) {
    val authState by authViewModel.uiState.collectAsState()
    val navController = rememberNavController()

    // Determine start destination based on auth state
    val startDestination = when {
        !authState.isLoggedIn -> "login"
        authState.isFirstTimeUser -> "role_selection"
        authState.needsRoleSelection || authState.selectedRole == null -> "role_choice"
        else -> "main"
    }

    // Effect to handle navigation based on auth state changes
    LaunchedEffect(authState.isLoggedIn, authState.selectedRole, authState.needsRoleSelection, authState.isFirstTimeUser) {
        // Add a small delay to ensure state is fully updated
        kotlinx.coroutines.delay(100)

        when {
            !authState.isLoggedIn -> {
                // User signed out or not logged in - go to login
                if (navController.currentDestination?.route != "login") {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            authState.isLoggedIn && authState.isFirstTimeUser -> {
                // New user needs to select initial role
                if (navController.currentDestination?.route != "role_selection") {
                    navController.navigate("role_selection") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            authState.isLoggedIn && (authState.needsRoleSelection || authState.selectedRole == null) -> {
                // Existing user needs to select role (every app start)
                if (navController.currentDestination?.route != "role_choice") {
                    navController.navigate("role_choice") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            authState.isLoggedIn && authState.selectedRole == "team_member" -> {
                // Team member selected - check if they have a team
                val hasTeam = authViewModel.checkEmployeeHasTeam(authState.currentUser!!.uid)
                if (!hasTeam && navController.currentDestination?.route != "employee_team_join") {
                    navController.navigate("employee_team_join") {
                        popUpTo(0) { inclusive = true }
                    }
                } else if (hasTeam && navController.currentDestination?.route != "main") {
                    navController.navigate("main") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            authState.isLoggedIn && authState.selectedRole == "admin" -> {
                // Admin selected - check if they have teams
                val hasTeams = authViewModel.checkAdminHasTeams(authState.currentUser!!.uid)
                if (!hasTeams && navController.currentDestination?.route != "admin_team_setup") {
                    navController.navigate("admin_team_setup") {
                        popUpTo(0) { inclusive = true }
                    }
                } else if (hasTeams && navController.currentDestination?.route != "main") {
                    navController.navigate("main") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            authState.isLoggedIn && authState.selectedRole != null -> {
                // User has selected a role - go to main
                if (navController.currentDestination?.route != "main") {
                    navController.navigate("main") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Authentication Flow
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    // This is handled by LaunchedEffect now
                },
                onGitHubSignIn = {
                    authViewModel.signInWithGitHub(activity)
                },
                isLoading = authState.isLoading || authState.isOAuthInProgress,
                errorMessage = authState.errorMessage,
                onErrorDismissed = { authViewModel.clearError() }
            )
        }

        composable("role_selection") {
            RoleSelectionScreen(
                onAdminSelected = { username, phone ->
                    authViewModel.createAdminProfile(username, phone)
                },
                onEmployeeSelected = {
                    // Route to team join screen instead of creating profile directly
                    navController.navigate("employee_team_join") {
                        popUpTo("role_selection") { inclusive = false }
                    }
                },
                isLoading = authState.isLoading,
                errorMessage = authState.errorMessage,
                onErrorDismissed = { authViewModel.clearError() },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate("login") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                onUseOtherAccount = {
                    // Sign out current user and redirect to login for different account
                    authViewModel.signOut()
                    navController.navigate("login") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            MainNavigation(
                themeViewModel = themeViewModel,
                authViewModel = authViewModel // Pass the same AuthViewModel instance
            )

            // Effect to handle sign-out
            LaunchedEffect(authState.isLoggedIn) {
                if (!authState.isLoggedIn) {
                    navController.navigate("login") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }
        }

        composable("admin_team_setup") {
            AdminTeamSetupScreen(
                onTeamCreated = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                authViewModel = authViewModel
            )
        }

        composable("employee_team_join") {
            EmployeeTeamJoinScreen(
                onTeamJoined = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onSkipForNow = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                authViewModel = authViewModel
            )
        }

        composable("role_choice") {
            RoleChoiceScreen(
                availableRoles = listOf("admin", "team_member"), // Provide both roles for selection
                onRoleSelected = { selectedRole ->
                    authViewModel.setUserRole(selectedRole)
                },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate("login") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                onUseOtherAccount = {
                    // Use the new account switching functionality
                    authViewModel.signInWithDifferentGitHubAccount(activity)
                }
            )
        }
    }
}
