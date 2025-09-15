package com.teamflux.fluxai.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teamflux.fluxai.model.Team
import com.teamflux.fluxai.viewmodel.AuthViewModel
import com.teamflux.fluxai.viewmodel.TeamViewModel
import com.teamflux.fluxai.viewmodel.AttendanceViewModel
import com.teamflux.fluxai.ui.theme.typewriterEffect
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    authViewModel: AuthViewModel = viewModel()
) {
    val teamViewModel: TeamViewModel = viewModel()
    val attendanceViewModel: AttendanceViewModel = viewModel()
    val authState by authViewModel.uiState.collectAsState()
    val teamState by teamViewModel.uiState.collectAsState()
    val attendanceState by attendanceViewModel.uiState.collectAsState()

    val currentUserId = authState.currentUser?.uid
    val isAdmin = authState.selectedRole == "admin"
    val snackbarHostState = remember { SnackbarHostState() }

    var attendanceCode by rememberSaveable { mutableStateOf("") }
    var selectedTeamId by rememberSaveable { mutableStateOf<String?>(null) }

    // Load data
    LaunchedEffect(currentUserId) {
        currentUserId?.let { userId ->
            if (isAdmin) {
                teamViewModel.loadTeams(userId)
            } else {
                teamViewModel.loadEmployeeTeam(userId)
                attendanceViewModel.loadTodayAttendance(userId)
            }
        }
    }

    // Handle messages
    attendanceState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(error)
            attendanceViewModel.clearError()
        }
    }

    attendanceState.successMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            attendanceViewModel.clearSuccess()
            // Reload the attendance code after successful generation
            selectedTeamId?.let { teamId ->
                attendanceViewModel.loadTeamAttendanceCode(teamId)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp).typewriterEffect("Attendance")
            )

            if (isAdmin) {
                AdminAttendanceView(
                    teams = teamState.teams,
                    selectedTeamId = selectedTeamId,
                    currentCode = attendanceState.currentTeamCode?.toString(),
                    attendanceRecords = emptyList(), // Remove reference to non-existent property
                    onTeamSelected = { teamId ->
                        selectedTeamId = teamId
                        attendanceViewModel.loadTeamAttendanceCode(teamId)
                        // Remove call to non-existent method
                    },
                    onGenerateCode = { teamId ->
                        attendanceViewModel.generateAttendanceCode(teamId)
                    }
                )
            } else {
                EmployeeAttendanceView(
                    attendanceCode = attendanceCode,
                    onCodeChange = { attendanceCode = it },
                    hasSignedInToday = attendanceState.hasSignedInToday,
                    signInTime = attendanceState.todayAttendance?.signInTime,
                    onSignIn = {
                        currentUserId?.let { userId ->
                            teamState.teams.firstOrNull()?.let { team ->
                                attendanceViewModel.signInAttendance(
                                    userId = userId,
                                    teamId = team.teamId,
                                    enteredCode = attendanceCode
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAttendanceView(
    teams: List<Team>,
    selectedTeamId: String?,
    currentCode: String?,
    attendanceRecords: List<Map<String, Any>>,
    onTeamSelected: (String) -> Unit,
    onGenerateCode: (String) -> Unit
) {
    // Team Selection
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Select Team",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            if (teams.isNotEmpty()) {
                var expanded by rememberSaveable { mutableStateOf(false) }
                var selectedTeamName by rememberSaveable { mutableStateOf("Choose a team") }

                selectedTeamId?.let { id ->
                    selectedTeamName = teams.find { it.teamId == id }?.teamName ?: "Choose a team"
                }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedTeamName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        teams.forEach { team ->
                            DropdownMenuItem(
                                text = { Text(team.teamName) },
                                onClick = {
                                    onTeamSelected(team.teamId)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "No teams available",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Generate Code Section
    if (selectedTeamId != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Attendance Code",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                if (currentCode != null && currentCode != "null") {
                    Text(
                        text = currentCode,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 8.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Button(
                    onClick = { onGenerateCode(selectedTeamId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Code")
                }
            }
        }

        // Attendance Records
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Today's Attendance",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                if (attendanceRecords.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(attendanceRecords) { record ->
                            AttendanceRecordItem(record = record)
                        }
                    }
                } else {
                    Text(
                        text = "No attendance records yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun EmployeeAttendanceView(
    attendanceCode: String,
    onCodeChange: (String) -> Unit,
    hasSignedInToday: Boolean,
    signInTime: String?,
    onSignIn: () -> Unit
) {
    if (hasSignedInToday) {
        // Already signed in
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Attendance Marked",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Signed in at: ${signInTime ?: "Unknown time"}",
                    fontSize = 16.sp
                )

                Text(
                    text = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date()),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    } else {
        // Sign in form
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Sign In",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date()),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = attendanceCode,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            onCodeChange(it)
                        }
                    },
                    label = { Text("Enter 6-digit code") },
                    textStyle = TextStyle(
                        fontSize = 18.sp,
                        letterSpacing = 4.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = onSignIn,
                    enabled = attendanceCode.length == 6,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign In")
                }
            }
        }
    }
}

@Composable
fun AttendanceRecordItem(record: Map<String, Any>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record["userName"]?.toString() ?: "Unknown User",
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Signed in at: ${record["signInTime"]?.toString() ?: "Unknown time"}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
