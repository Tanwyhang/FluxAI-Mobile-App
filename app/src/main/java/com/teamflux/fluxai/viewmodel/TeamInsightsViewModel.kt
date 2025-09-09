package com.teamflux.fluxai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teamflux.fluxai.model.Team
import com.teamflux.fluxai.network.WebhookService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeamInsightsViewModel @Inject constructor(
    private val webhookService: WebhookService
) : ViewModel() {

    private val _insights = MutableStateFlow<List<String>>(emptyList())
    val insights: StateFlow<List<String>> = _insights

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun generateTeamInsights(team: Team, memberCount: Int = 5, teamRoles: List<String> = emptyList()) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val teamInsights = webhookService.generateTeamInsights(
                    teamName = team.teamName,
                    memberCount = memberCount,
                    teamRoles = teamRoles
                )
                _insights.value = teamInsights
            } catch (e: Exception) {
                // If webhook fails, Room fallback is handled inside WebhookService
                android.util.Log.e("TeamInsightsViewModel", "Error generating insights", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
