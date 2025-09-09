package com.teamflux.fluxai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teamflux.fluxai.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class AttendanceRecord(
    val id: String = "",
    val userId: String = "",
    val teamId: String = "",
    val date: String = "",
    val signInTime: String = "",
    val attendanceCode: String = "",
    val timestamp: Long = 0
)

data class TeamAttendanceCode(
    val teamId: String = "",
    val code: String = "",
    val date: String = "",
    val generatedAt: Long = 0
)

data class AttendanceUiState(
    val isLoading: Boolean = false,
    val hasSignedInToday: Boolean = false,
    val todayAttendance: AttendanceRecord? = null,
    val currentTeamCode: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AttendanceViewModel : ViewModel() {
    private val attendanceRepository = AttendanceRepository()

    private val _uiState = MutableStateFlow(AttendanceUiState())
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    fun loadTodayAttendance(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            attendanceRepository.getTodayAttendance(userId, today).fold(
                onSuccess = { attendance ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hasSignedInToday = attendance != null,
                        todayAttendance = attendance,
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

    fun loadTeamAttendanceCode(teamId: String) {
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            attendanceRepository.getTeamAttendanceCode(teamId, today).fold(
                onSuccess = { teamCode ->
                    _uiState.value = _uiState.value.copy(
                        currentTeamCode = teamCode?.code,
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message
                    )
                }
            )
        }
    }

    fun generateAttendanceCode(teamId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val code = generateRandomCode()

            attendanceRepository.createTeamAttendanceCode(teamId, code, today).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentTeamCode = code,
                        successMessage = "Attendance code generated successfully!",
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to generate code: ${error.message}"
                    )
                }
            )
        }
    }

    fun regenerateAttendanceCode(teamId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val newCode = generateRandomCode()

            attendanceRepository.updateTeamAttendanceCode(teamId, newCode, today).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentTeamCode = newCode,
                        successMessage = "New attendance code generated!",
                        errorMessage = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to regenerate code: ${error.message}"
                    )
                }
            )
        }
    }

    fun signInAttendance(userId: String, teamId: String, enteredCode: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            // First, verify the code is correct
            attendanceRepository.getTeamAttendanceCode(teamId, today).fold(
                onSuccess = { teamCode ->
                    if (teamCode != null && teamCode.code == enteredCode) {
                        // Code is correct, record attendance
                        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

                        attendanceRepository.recordAttendance(
                            userId = userId,
                            teamId = teamId,
                            date = today,
                            signInTime = currentTime,
                            attendanceCode = enteredCode
                        ).fold(
                            onSuccess = { attendance ->
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    hasSignedInToday = true,
                                    todayAttendance = attendance,
                                    successMessage = "Attendance recorded successfully!",
                                    errorMessage = null
                                )
                            },
                            onFailure = { error ->
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = "Failed to record attendance: ${error.message}"
                                )
                            }
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Invalid attendance code. Please check with your admin."
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Error verifying code: ${error.message}"
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    private fun generateRandomCode(): String {
        return String.format(Locale.US, "%06d", (100000..999999).random())
    }
}
