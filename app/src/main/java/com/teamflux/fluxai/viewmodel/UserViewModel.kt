package com.teamflux.fluxai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teamflux.fluxai.model.UserProfile
import com.teamflux.fluxai.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserProfileUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val error: String? = null
)

class UserViewModel : ViewModel() {
    private val repository = UserRepository()

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val userId = repository.currentUserId()
            if (userId.isNullOrBlank()) {
                _uiState.value = UserProfileUiState(isLoading = false, error = "User not signed in")
                return@launch
            }

            repository.getUserProfile(userId)
                .onSuccess { profile ->
                    _uiState.value = UserProfileUiState(isLoading = false, profile = profile)
                }
                .onFailure { e ->
                    _uiState.value = UserProfileUiState(isLoading = false, error = e.message)
                }
        }
    }
}

