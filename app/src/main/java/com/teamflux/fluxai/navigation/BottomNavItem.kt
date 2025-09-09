package com.teamflux.fluxai.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val title: String
) {
    object Dashboard : BottomNavItem("dashboard", Icons.Default.Home, "FluxBOARD")
    object Chatbot : BottomNavItem("chatbot", Icons.AutoMirrored.Filled.Message, "FluxCHAT")
    object Team : BottomNavItem("team", Icons.Default.Face, "Team")
    object Attendance : BottomNavItem("attendance", Icons.Default.AccessTime, "Attendance")
    object Settings : BottomNavItem("settings", Icons.Default.Settings, "Settings")
}
