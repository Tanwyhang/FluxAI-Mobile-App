package com.teamflux.fluxai.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val title: String
) {
    object Dashboard : BottomNavItem("dashboard", Icons.Default.Home, "FluxBOARD")
    object Chatbot : BottomNavItem("chatbot", Icons.Default.Send, "FluxCHAT")
    object Team : BottomNavItem("team", Icons.Default.Face, "Team")
    object Settings : BottomNavItem("settings", Icons.Default.Settings, "Settings")
}
