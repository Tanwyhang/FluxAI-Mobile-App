package com.teamflux.fluxai.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.teamflux.fluxai.repository.AuthRepository
import com.teamflux.fluxai.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val currentUser: FirebaseUser? = null,
    val availableRoles: List<String> = emptyList(), // Changed from single role to multiple roles
    val selectedRole: String? = null, // Currently active role
    val errorMessage: String? = null,
    val isFirstTimeUser: Boolean = false,
    val isOAuthInProgress: Boolean = false,
    val needsRoleSelection: Boolean = false // New flag for role selection
)

class   AuthViewModel : ViewModel() {
    private val authRepository = AuthRepository()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            val currentUser = authRepository.currentUser
            if (currentUser != null) {
                val userRoles = authRepository.getUserRoles(currentUser.uid)
                _uiState.value = _uiState.value.copy(
                    isLoggedIn = true,
                    currentUser = currentUser,
                    availableRoles = userRoles,
                    selectedRole = null, // Don't auto-select role - require user to choose
                    isFirstTimeUser = userRoles.isEmpty(),
                    needsRoleSelection = userRoles.isNotEmpty() // Need role selection if user has roles
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoggedIn = false)
            }
        }
    }

    fun signInWithGitHub(activity: Activity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                isOAuthInProgress = true,
                errorMessage = null
            )

            authRepository.signInWithGitHub(activity)
                .onSuccess { user ->
                    val userRoles = authRepository.getUserRoles(user.uid)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isOAuthInProgress = false,
                        isLoggedIn = true,
                        currentUser = user,
                        availableRoles = userRoles,
                        selectedRole = null, // Don't auto-select role after login
                        isFirstTimeUser = userRoles.isEmpty(),
                        needsRoleSelection = userRoles.isNotEmpty() // Need role selection if user has roles
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isOAuthInProgress = false,
                        errorMessage = exception.message
                    )
                }
        }
    }

    fun handleOAuthError(error: String, errorDescription: String?) {
        val friendlyMessage = when (error) {
            "access_denied" -> "GitHub access was denied. Please try again."
            "unauthorized_client" -> "App not authorized. Please contact support."
            "invalid_request" -> "Invalid authentication request. Please try again."
            "server_error" -> "Server error occurred. Please try again later."
            else -> errorDescription ?: "GitHub authentication failed: $error"
        }

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isOAuthInProgress = false,
            errorMessage = friendlyMessage
        )
    }

    fun createAdminProfile(username: String, phone: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val currentUser = _uiState.value.currentUser
            if (currentUser != null) {
                authRepository.createAdminProfile(
                    userId = currentUser.uid,
                    username = username,
                    email = currentUser.email ?: "",
                    phone = phone
                )
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            selectedRole = "admin",
                            isFirstTimeUser = false
                        )
                    }
                    .onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = exception.message
                        )
                    }
            }
        }
    }

    fun createTeamMemberProfile(
        teamId: String,
        role: String,
        githubProfileLink: String,
        phone: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val currentUser = _uiState.value.currentUser
            if (currentUser != null) {
                authRepository.createTeamMemberProfile(
                    userId = currentUser.uid,
                    teamId = teamId,
                    role = role,
                    githubProfileLink = githubProfileLink,
                    email = currentUser.email ?: "",
                    phone = phone
                )
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            selectedRole = "team_member",
                            isFirstTimeUser = false
                        )
                    }
                    .onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = exception.message
                        )
                    }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            // Reset to completely clean state with explicit values
            _uiState.value = AuthUiState(
                isLoading = false,
                isLoggedIn = false,
                currentUser = null,
                availableRoles = emptyList(),
                selectedRole = null,
                errorMessage = null,
                isFirstTimeUser = false,
                isOAuthInProgress = false,
                needsRoleSelection = false
            )
        }
    }

    fun signInWithDifferentGitHubAccount(activity: Activity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                isOAuthInProgress = true,
                errorMessage = null
            )

            // Clear current auth state first
            authRepository.clearAuthState()

            authRepository.signInWithDifferentGitHubAccount(activity)
                .onSuccess { user ->
                    val userRoles = authRepository.getUserRoles(user.uid)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isOAuthInProgress = false,
                        isLoggedIn = true,
                        currentUser = user,
                        availableRoles = userRoles,
                        selectedRole = null, // Don't auto-select role after switching accounts
                        isFirstTimeUser = userRoles.isEmpty(),
                        needsRoleSelection = userRoles.isNotEmpty() // Need role selection if user has roles
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isOAuthInProgress = false,
                        errorMessage = exception.message
                    )
                }
        }
    }

    suspend fun checkAdminHasTeams(adminId: String): Boolean {
        return try {
            val result = TeamRepository().getTeamsByAdmin(adminId)
            result.getOrNull()?.isNotEmpty() == true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun checkEmployeeHasTeam(employeeId: String): Boolean {
        return try {
            val result = TeamRepository().getEmployeeTeam(employeeId)
            result.getOrNull() != null
        } catch (e: Exception) {
            false
        }
    }

    fun selectRole(role: String) {
        _uiState.value = _uiState.value.copy(
            selectedRole = role,
            needsRoleSelection = false
        )
    }

    fun checkIfNeedsRoleSelection(): Boolean {
        return _uiState.value.availableRoles.size > 1
    }

    fun setUserRole(role: String) {
        _uiState.value = _uiState.value.copy(
            selectedRole = role,
            needsRoleSelection = false
        )
    }

    fun requireRoleSelection() {
        _uiState.value = _uiState.value.copy(
            needsRoleSelection = true,
            selectedRole = null
        )
    }

    fun setEmployeeNeedsTeam(needsTeam: Boolean) {
        _uiState.value = _uiState.value.copy(
            needsRoleSelection = needsTeam
        )
    }
}
