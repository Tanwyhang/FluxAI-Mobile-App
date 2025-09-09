package com.teamflux.fluxai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teamflux.fluxai.model.Team
import com.teamflux.fluxai.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TeamUiState(
    val isLoading: Boolean = false,
    val teams: List<Team> = emptyList(),
    val selectedTeam: Team? = null,
    val selectedTeamId: String? = null, // Add selected team ID for attendance
    val teamMembers: List<Map<String, Any>> = emptyList(),
    val errorMessage: String? = null,
    val isCreateTeamDialogOpen: Boolean = false,
    val isAddMemberDialogOpen: Boolean = false,
    val isEditMemberDialogOpen: Boolean = false,
    val selectedMember: Map<String, Any>? = null,
    val isDeleteTeamDialogOpen: Boolean = false,
    val teamToDelete: Team? = null,
    val isJoinTeamDialogOpen: Boolean = false
)

class TeamViewModel : ViewModel() {
    private val teamRepository = TeamRepository()

    private val _uiState = MutableStateFlow(TeamUiState())
    val uiState: StateFlow<TeamUiState> = _uiState.asStateFlow()

    fun loadTeams(adminId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            teamRepository.getTeamsByAdmin(adminId).fold(
                onSuccess = { teams ->
                    _uiState.value = _uiState.value.copy(
                        teams = teams,
                        isLoading = false,
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
            )
        }
    }

    fun selectTeam(team: Team) {
        _uiState.value = _uiState.value.copy(selectedTeam = team)
        loadTeamMembers(team.teamId)
    }

    // Overloaded method for selecting team by ID (for attendance)
    fun selectTeam(teamId: String) {
        _uiState.value = _uiState.value.copy(selectedTeamId = teamId)
        // Find the team object and set it as selected too
        val team = _uiState.value.teams.find { it.teamId == teamId }
        team?.let {
            _uiState.value = _uiState.value.copy(selectedTeam = it)
        }
    }

    fun loadTeamMembers(teamId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            println("TeamViewModel: Loading team members for teamId: $teamId")

            teamRepository.getTeamMembersByTeamId(teamId).fold(
                onSuccess = { members ->
                    println("TeamViewModel: Loaded ${members.size} team members")
                    members.forEach { member ->
                        println("TeamViewModel: Member - ${member}")
                    }
                    _uiState.value = _uiState.value.copy(
                        teamMembers = members,
                        isLoading = false,
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    println("TeamViewModel: Error loading team members: ${error.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
            )
        }
    }

    fun createTeam(teamName: String, adminId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            teamRepository.createTeam(teamName, adminId).fold(
                onSuccess = { teamId ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isCreateTeamDialogOpen = false,
                        errorMessage = null
                    )
                    loadTeams(adminId) // Refresh teams list
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
            )
        }
    }

    fun addTeamMember(teamId: String, githubUsername: String, role: String, adminId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            teamRepository.addTeamMemberByGithubUsername(teamId, githubUsername, role, adminId).fold(
                onSuccess = { memberId ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAddMemberDialogOpen = false,
                        errorMessage = null
                    )
                    loadTeamMembers(teamId) // Refresh members list
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
            )
        }
    }

    fun updateMemberRole(memberId: String, newRole: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            teamRepository.updateTeamMemberRole(memberId, newRole).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isEditMemberDialogOpen = false,
                        selectedMember = null,
                        errorMessage = null
                    )
                    // Refresh members list
                    _uiState.value.selectedTeam?.let { team ->
                        loadTeamMembers(team.teamId)
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
            )
        }
    }

    fun removeMember(memberId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            teamRepository.removeTeamMember(memberId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = null
                    )
                    // Refresh members list
                    _uiState.value.selectedTeam?.let { team ->
                        loadTeamMembers(team.teamId)
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
            )
        }
    }

    fun deleteTeam(teamId: String, adminId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            teamRepository.deleteTeam(teamId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isDeleteTeamDialogOpen = false,
                        teamToDelete = null,
                        selectedTeam = null, // Clear selection since team is deleted
                        teamMembers = emptyList(),
                        errorMessage = null
                    )
                    loadTeams(adminId) // Refresh teams list
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
            )
        }
    }

    fun openCreateTeamDialog() {
        _uiState.value = _uiState.value.copy(isCreateTeamDialogOpen = true)
    }

    fun closeCreateTeamDialog() {
        _uiState.value = _uiState.value.copy(isCreateTeamDialogOpen = false)
    }

    fun openAddMemberDialog() {
        _uiState.value = _uiState.value.copy(isAddMemberDialogOpen = true)
    }

    fun closeAddMemberDialog() {
        _uiState.value = _uiState.value.copy(isAddMemberDialogOpen = false)
    }

    fun openEditMemberDialog(member: Map<String, Any>) {
        _uiState.value = _uiState.value.copy(
            isEditMemberDialogOpen = true,
            selectedMember = member
        )
    }

    fun closeEditMemberDialog() {
        _uiState.value = _uiState.value.copy(
            isEditMemberDialogOpen = false,
            selectedMember = null
        )
    }

    fun openDeleteTeamDialog(team: Team) {
        _uiState.value = _uiState.value.copy(
            isDeleteTeamDialogOpen = true,
            teamToDelete = team
        )
    }

    fun closeDeleteTeamDialog() {
        _uiState.value = _uiState.value.copy(
            isDeleteTeamDialogOpen = false,
            teamToDelete = null
        )
    }

    fun openJoinTeamDialog() {
        _uiState.value = _uiState.value.copy(isJoinTeamDialogOpen = true)
    }

    fun closeJoinTeamDialog() {
        _uiState.value = _uiState.value.copy(isJoinTeamDialogOpen = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun searchTeamById(teamId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            teamRepository.getTeamById(teamId).fold(
                onSuccess = { team ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = null
                    )
                    // Store found team for UI to access
                    if (team != null) {
                        _uiState.value = _uiState.value.copy(
                            selectedTeam = team
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
            )
        }
    }

    fun checkAdminHasTeams(adminId: String): Boolean {
        return _uiState.value.teams.isNotEmpty()
    }

    fun loadEmployeeTeam(employeeId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            println("TeamViewModel: Loading employee team for userId: $employeeId")

            teamRepository.getEmployeeTeam(employeeId).fold(
                onSuccess = { team ->
                    val teams = if (team != null) listOf(team) else emptyList()
                    println("TeamViewModel: Employee team loaded: ${team?.teamName}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        teams = teams,
                        selectedTeam = team,
                        errorMessage = null
                    )
                    // Load team members if team exists
                    team?.let {
                        println("TeamViewModel: Loading team members for employee's team: ${it.teamId}")
                        loadTeamMembers(it.teamId)
                    }
                },
                onFailure = { error ->
                    println("TeamViewModel: Error loading employee team: ${error.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
            )
        }
    }

    fun joinTeam(teamId: String, userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            println("TeamViewModel: User $userId attempting to join team $teamId")

            teamRepository.getTeamById(teamId).fold(
                onSuccess = { team ->
                    if (team != null) {
                        println("TeamViewModel: Team $teamId exists: ${team.teamName}")
                        // Team exists, now create team member profile for the user
                        val authRepository = com.teamflux.fluxai.repository.AuthRepository()
                        authRepository.createTeamMemberProfile(
                            userId = userId,
                            teamId = teamId,
                            role = "Developer", // Default role for self-joining employees
                            githubProfileLink = "", // Can be updated later
                            email = "", // Will be filled from user's auth profile
                            phone = "" // Can be updated later
                        ).fold(
                            onSuccess = {
                                println("TeamViewModel: Successfully created team member profile for user $userId")
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    isJoinTeamDialogOpen = false,
                                    errorMessage = null
                                )
                                // Reload employee team to show the newly joined team
                                println("TeamViewModel: Reloading employee team after join")
                                loadEmployeeTeam(userId)
                            },
                            onFailure = { error ->
                                println("TeamViewModel: Failed to create team member profile: ${error.message}")
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = "Failed to join team: ${error.message}"
                                )
                            }
                        )
                    } else {
                        println("TeamViewModel: Team with ID '$teamId' not found")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Team with ID '$teamId' not found"
                        )
                    }
                },
                onFailure = { error ->
                    println("TeamViewModel: Error finding team: ${error.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Error finding team: ${error.message}"
                    )
                }
            )
        }
    }

    fun leaveTeam(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Find the user's team membership and remove it
            teamRepository.removeUserFromTeam(userId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        teams = emptyList(),
                        selectedTeam = null,
                        teamMembers = emptyList(),
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to leave team: ${error.message}"
                    )
                }
            )
        }
    }
}
