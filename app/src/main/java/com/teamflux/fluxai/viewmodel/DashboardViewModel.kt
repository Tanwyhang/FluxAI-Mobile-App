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
    // Employee mode (self only)
    data class Team(val performances: List<EmployeePerformance>) : DashboardUiState()
    // Admin mode (team selectable)
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
    val id: String,
    val userId: String?,
    val githubUsername: String,
    val name: String,
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
                val roleRaw = userRepository.getCurrentUserRole()
                val role = roleRaw.trim().lowercase()
                if (role == "admin" || role == "owner") {
                    loadAdminTeams()
                } else {
                    initEmployeeMode()
                }
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun ensureRoleState() {
        viewModelScope.launch {
            try {
                val roleRaw = userRepository.getCurrentUserRole()
                val role = roleRaw.trim().lowercase()
                val current = _uiState.value
                val isAdmin = role == "admin" || role == "owner"
                if (isAdmin && current !is DashboardUiState.Admin) {
                    loadAdminTeams()
                } else if (!isAdmin && current !is DashboardUiState.Team) {
                    initEmployeeMode()
                }
            } catch (_: Exception) {}
        }
    }

    private suspend fun initEmployeeMode() {
        val userId = userRepository.getCurrentUserId()
        val profile = userRepository.getUserProfile(userId).getOrNull()
        val gh = profile?.githubUsername
        if (gh.isNullOrBlank()) {
            _uiState.value = DashboardUiState.Error("GitHub username missing in profile")
            return
        }
        performanceDataRepository.setSelectedTeamFromUsernames(listOf(userId to gh))
        _uiState.value = DashboardUiState.Team(emptyList())
        refreshTeamPerformances() // self only
    }

    private fun loadAdminTeams() {
        viewModelScope.launch {
            try {
                val adminId = userRepository.getCurrentUserId()
                val result = teamRepository.getTeamsByAdmin(adminId)
                result.fold(onSuccess = { teams ->
                    val uiTeams = teams.map { t ->
                        TeamUi(
                            id = t.teamId,
                            name = t.teamName,
                            members = emptyList()
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
        _uiState.value = current.copy(selectedTeamId = teamId, loadingPerformances = true, performances = emptyList())
        viewModelScope.launch {
            try {
                val membersResult = teamRepository.getTeamMembersByTeamId(teamId)
                membersResult.fold(onSuccess = { maps ->
                    val members = maps.map { map ->
                        val memberId = (map["memberId"] as? String).orEmpty()
                        val userId = map["userId"] as? String
                        val githubUsername = (map["githubUsername"] as? String).orEmpty()
                        val role = (map["role"] as? String).orEmpty()
                        MemberUi(
                            id = memberId,
                            userId = userId,
                            githubUsername = githubUsername,
                            name = githubUsername,
                            role = role
                        )
                    }.filter { it.githubUsername.isNotBlank() }

                    val expected = members.size
                    val updatedTeams = current.teams.map { t -> if (t.id == teamId) t.copy(members = members) else t }
                    _uiState.value = current.copy(
                        teams = updatedTeams,
                        selectedTeamId = teamId,
                        loadingPerformances = expected > 0,
                        performances = emptyList()
                    )
                    if (expected == 0) return@fold

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
                    viewModelScope.launch {
                        performanceDataRepository.refreshAllEmployeePerformances().collect { perfs ->
                            val after = _uiState.value
                            if (after is DashboardUiState.Admin && after.selectedTeamId == teamId) {
                                val complete = perfs.size >= expected
                                _uiState.value = after.copy(
                                    performances = perfs,
                                    loadingPerformances = !complete
                                )
                            }
                        }
                    }
                }, onFailure = { e ->
                    val cur = _uiState.value
                    if (cur is DashboardUiState.Admin) {
                        _uiState.value = cur.copy(loadingPerformances = false)
                    }
                    if (cur !is DashboardUiState.Error) {
                        _uiState.value = DashboardUiState.Error(e.message ?: "Failed loading members")
                    }
                })
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
                val id = state.selectedTeamId ?: return
                selectTeam(id) // re-trigger
            }
            is DashboardUiState.Team -> {
                viewModelScope.launch {
                    try {
                        performanceDataRepository.refreshAllEmployeePerformances().collect { perfs ->
                            val single = perfs.firstOrNull()?.let { listOf(it) } ?: emptyList()
                            _uiState.value = DashboardUiState.Team(single)
                        }
                    } catch (e: Exception) {
                        _uiState.value = DashboardUiState.Error(e.message ?: "Refresh failed")
                    }
                }
            }
            else -> {}
        }
    }

    // Legacy external setter (still restrict to first when in Team mode)
    fun setSelectedTeamMembers(members: List<PerformanceDataRepository.BaseEmployee>) {
        performanceDataRepository.setSelectedTeamMembers(members)
        viewModelScope.launch {
            try {
                performanceDataRepository.refreshAllEmployeePerformances().collect { perfs ->
                    if (_uiState.value is DashboardUiState.Team) {
                        _uiState.value = DashboardUiState.Team(perfs.take(1))
                    } else {
                        _uiState.value = DashboardUiState.Admin(performances = perfs) // fallback (should not generally happen)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error(e.message ?: "Failed loading performances")
            }
        }
    }
}