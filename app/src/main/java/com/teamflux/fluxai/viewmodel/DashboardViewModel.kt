package com.teamflux.fluxai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teamflux.fluxai.model.EmployeePerformance
import com.teamflux.fluxai.repository.PerformanceDataRepository
import com.teamflux.fluxai.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val performanceDataRepository: PerformanceDataRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        loadDashboardData()
    }

    fun refresh() {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading

            try {
                val userRole = userRepository.getCurrentUserRole()
                if (userRole == "team_member") {
                    // For team members, only load their own performance
                    val userId = userRepository.getCurrentUserId()
                    performanceDataRepository.getEmployeePerformance(userId)
                        .catch { e ->
                            _uiState.value = DashboardUiState.Error(
                                "Failed to load performance data: ${e.message}"
                            )
                        }
                        .collect { performance ->
                            _uiState.value = DashboardUiState.Success(
                                userRole = userRole,
                                performances = listOfNotNull(performance)
                            )
                        }
                } else {
                    // For admins, load all employee performances
                    performanceDataRepository.getAllEmployeePerformances()
                        .catch { e ->
                            _uiState.value = DashboardUiState.Error(
                                "Failed to load team performance data: ${e.message}"
                            )
                        }
                        .collect { performances ->
                            _uiState.value = DashboardUiState.Success(
                                userRole = userRole,
                                performances = performances
                            )
                        }
                }
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error(
                    "Failed to determine user role: ${e.message}"
                )
            }
        }
    }

    suspend fun syncWorkflowData() {
        _uiState.value = DashboardUiState.Loading

        try {
            val userRole = userRepository.getCurrentUserRole()
            if (userRole == "team_member") {
                val userId = userRepository.getCurrentUserId()
                // Force webhook refresh for current user
                performanceDataRepository.refreshEmployeePerformance(userId)
                    .catch { e ->
                        _uiState.value = DashboardUiState.Error(
                            "Failed to sync performance data: ${e.message}"
                        )
                    }
                    .collect { performance ->
                        _uiState.value = DashboardUiState.Success(
                            userRole = userRole,
                            performances = listOfNotNull(performance)
                        )
                    }
            } else {
                // For admins, force webhook refresh for all employees
                performanceDataRepository.refreshAllEmployeePerformances()
                    .catch { e ->
                        _uiState.value = DashboardUiState.Error(
                            "Failed to sync team performance data: ${e.message}"
                        )
                    }
                    .collect { performances ->
                        _uiState.value = DashboardUiState.Success(
                            userRole = userRole,
                            performances = performances
                        )
                    }
            }
        } catch (e: Exception) {
            _uiState.value = DashboardUiState.Error(
                "Failed to sync data: ${e.message}"
            )
        }
    }
}

sealed class DashboardUiState {
    data object Loading : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
    data class Success(
        val userRole: String,
        val performances: List<EmployeePerformance>
    ) : DashboardUiState()
}
