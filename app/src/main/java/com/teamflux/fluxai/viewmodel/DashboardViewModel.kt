package com.teamflux.fluxai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teamflux.fluxai.model.EmployeePerformance
import com.teamflux.fluxai.repository.PerformanceDataRepository
import com.teamflux.fluxai.repository.TeamRepository
import com.teamflux.fluxai.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
    data class Team(val performances: List<EmployeePerformance>) : DashboardUiState()
    data class Admin(
        val teams: List<TeamUi> = emptyList(),
        val selectedTeamId: String? = null,
        val performances: List<EmployeePerformance> = emptyList(),
        val loadingPerformances: Boolean = false
    ) : DashboardUiState()
}

data class TeamUi(
    val id: String,
    val name: String,
    val members: List<MemberUi>
)

data class MemberUi(
    val id: String,            // memberId
    val userId: String?,       // optional userId
    val githubUsername: String,
    val name: String,          // fallback to githubUsername if no explicit name
    val role: String
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val performanceDataRepository: PerformanceDataRepository,
    private val teamRepository: TeamRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init { bootstrap() }

    private fun bootstrap() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            try {
                val role = userRepository.getCurrentUserRole()
                if (role == "admin" || role == "owner") {
                    loadAdminTeams()
                } else {
                    // For non-admin we expect user performance only
                    val userId = userRepository.getCurrentUserId()
                    performanceDataRepository.setSelectedTeamFromUsernames(listOf(userId to ("")))
                    refreshTeamPerformances()
                }
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun loadAdminTeams() {
        viewModelScope.launch {
            try {
                val adminId = userRepository.getCurrentUserId()
                val teamsResult = teamRepository.getTeamsByAdmin(adminId)
                teamsResult.fold(onSuccess = { teams ->
                    val uiTeams = teams.map { team ->
                        TeamUi(
                            id = team.teamId,
                            name = team.teamName,
                            members = emptyList() // members loaded lazily on selection
                        )
                    }
                    _uiState.value = DashboardUiState.Admin(teams = uiTeams)
                }, onFailure = { e ->
                    _uiState.value = DashboardUiState.Error(e.message ?: "Failed loading teams")
                })
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error(e.message ?: "Failed loading teams")
            }
        }
    }

    fun selectTeam(teamId: String) {
        val current = _uiState.value
        if (current !is DashboardUiState.Admin) return
        // set selected and start loading members + performances
        _uiState.value = current.copy(selectedTeamId = teamId, loadingPerformances = true, performances = emptyList())
        viewModelScope.launch {
            try {
                val membersResult = teamRepository.getTeamMembersByTeamId(teamId)
                val memberMaps = membersResult.getOrElse { emptyList() }
                val members = memberMaps.map { map ->
                    val memberId = map["memberId"] as? String ?: map["id"] as? String ?: ""
                    val userId = map["userId"] as? String
                    val githubUsername = map["githubUsername"] as? String ?: ""
                    val role = map["role"] as? String ?: ""
                    MemberUi(
                        id = memberId,
                        userId = userId,
                        githubUsername = githubUsername,
                        name = githubUsername,
                        role = role
                    )
                }.filter { it.githubUsername.isNotBlank() }

                val expectedCount = members.size

                // Update team with members in state
                val updatedTeams = current.teams.map { t ->
                    if (t.id == teamId) t.copy(members = members) else t
                }
                _uiState.value = current.copy(
                    teams = updatedTeams,
                    selectedTeamId = teamId,
                    loadingPerformances = true,
                    performances = emptyList()
                )

                // Wire repository with members and refresh performances
                performanceDataRepository.setSelectedTeamMembers(
                    members.map { m ->
                        PerformanceDataRepository.BaseEmployee(
                            id = m.id,
                            name = m.name,
                            role = m.role,
                            githubUsername = m.githubUsername,
                            teamId = teamId
                        )
                    }
                )
                performanceDataRepository.refreshAllEmployeePerformances().collect { perfs ->
                    val after = _uiState.value
                    if (after is DashboardUiState.Admin && after.selectedTeamId == teamId) {
                        val complete = perfs.size >= expectedCount && expectedCount != 0
                        _uiState.value = after.copy(
                            performances = perfs,
                            loadingPerformances = !complete
                        )
                    }
                }
            } catch (e: Exception) {
                val cur = _uiState.value
                if (cur is DashboardUiState.Admin) {
                    _uiState.value = cur.copy(loadingPerformances = false)
                }
            }
        }
    }

    fun refreshTeamPerformances() {
        when (val state = _uiState.value) {
            is DashboardUiState.Admin -> {
                val teamId = state.selectedTeamId ?: return
                selectTeam(teamId) // re-select triggers refresh
            }
            is DashboardUiState.Team -> {
                viewModelScope.launch {
                    try {
                        performanceDataRepository.refreshAllEmployeePerformances().collect { perfs ->
                            _uiState.value = DashboardUiState.Team(perfs)
                        }
                    } catch (e: Exception) {
                        _uiState.value = DashboardUiState.Error(e.message ?: "Refresh failed")
                    }
                }
            }
            else -> {}
        }
    }

    // Legacy API kept for screen compatibility
    fun setSelectedTeamMembers(members: List<PerformanceDataRepository.BaseEmployee>) {
        performanceDataRepository.setSelectedTeamMembers(members)
        viewModelScope.launch {
            try {
                performanceDataRepository.refreshAllEmployeePerformances().collect { perfs ->
                    _uiState.value = DashboardUiState.Team(perfs)
                }
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error(e.message ?: "Failed loading performances")
            }
        }
    }
}