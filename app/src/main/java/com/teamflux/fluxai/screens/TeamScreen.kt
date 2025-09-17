package com.teamflux.fluxai.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teamflux.fluxai.model.Team
import com.teamflux.fluxai.viewmodel.AuthViewModel
import com.teamflux.fluxai.viewmodel.TeamInsightsViewModel
import com.teamflux.fluxai.viewmodel.TeamViewModel
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen(authViewModel: AuthViewModel? = null) {
    val teamViewModel: TeamViewModel = viewModel()
    val viewModel = authViewModel ?: viewModel<AuthViewModel>()

    val teamState by teamViewModel.uiState.collectAsState()
    val authState by viewModel.uiState.collectAsState()

    val isAdmin = authState.selectedRole == "admin"
    val currentUserId = authState.currentUser?.uid

    // Context for sharing functionality
    val context = LocalContext.current

    // Clipboard manager for copying team ID
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Load teams based on user role - this is the primary data loading effect
    LaunchedEffect(currentUserId, isAdmin) {
        currentUserId?.let { userId ->
            if (isAdmin) {
                teamViewModel.loadTeams(userId)
            } else {
                // For employees, load only their team
                println("TeamScreen: Loading employee team for userId: $userId")
                teamViewModel.loadEmployeeTeam(userId)
            }
        }
    }

    // Auto-select first team for employees when teams are loaded
    LaunchedEffect(teamState.teams, isAdmin) {
        if (!isAdmin && teamState.teams.isNotEmpty()) {
            val firstTeam = teamState.teams.first()
            println("TeamScreen: Auto-selecting team for employee: ${firstTeam.teamName}")
            teamViewModel.selectTeam(firstTeam)
        }
    }

    // Load team members whenever the selected team changes
    LaunchedEffect(teamState.selectedTeam?.teamId) {
        teamState.selectedTeam?.teamId?.let { teamId ->
            println("TeamScreen: Loading team members for teamId: $teamId")
            teamViewModel.loadTeamMembers(teamId)
        }
    }

    // For employees, ensure team members are loaded even when using fallback team
    LaunchedEffect(teamState.teams, isAdmin) {
        if (!isAdmin && teamState.teams.isNotEmpty() && teamState.teamMembers.isEmpty()) {
            val teamToLoad = teamState.selectedTeam ?: teamState.teams.firstOrNull()
            teamToLoad?.let { team ->
                println("TeamScreen: Loading team members for fallback team: ${team.teamId}")
                teamViewModel.loadTeamMembers(team.teamId)
            }
        }
    }

    // Debug output to track state
    LaunchedEffect(teamState.teams, teamState.selectedTeam, teamState.teamMembers) {
        println("TeamScreen Debug - Teams: ${teamState.teams.size}, Selected: ${teamState.selectedTeam?.teamName}, Members: ${teamState.teamMembers.size}")
    }

    // Check if employee is already in a team
    val isEmployeeInTeam = !isAdmin && teamState.teams.isNotEmpty()

    // Error handling with Snackbar
    teamState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(
                message = error,
                withDismissAction = true,
                duration = SnackbarDuration.Long
            )
            teamViewModel.clearError()
        }
    }

    // Show snackbar when team ID is copied
    var lastCopiedTeamId by remember { mutableStateOf<String?>(null) }

    // Handle copy team ID functionality
    val handleCopyTeamId: (String) -> Unit = { teamId ->
        clipboardManager.setText(AnnotatedString(teamId))
        lastCopiedTeamId = teamId // Trigger snackbar
    }

    // Handle share team ID functionality
    val handleShareTeamId: (Team) -> Unit = { team ->
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Join Team: ${team.teamName}")
            putExtra(Intent.EXTRA_TEXT, team.teamId)
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Share Team ID")
        context.startActivity(chooserIntent)
    }

    lastCopiedTeamId?.let { teamId ->
        LaunchedEffect(teamId) {
            snackbarHostState.showSnackbar(
                message = "Team ID copied to clipboard: $teamId",
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )
            lastCopiedTeamId = null // Reset after showing
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with create team button (admin only)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAdmin) "Team Management" else "My Team",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (isAdmin) {
                        FloatingActionButton(
                            onClick = { teamViewModel.openCreateTeamDialog() },
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Create Team")
                        }
                    }
                }
            }

            if (teamState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Main content based on whether user has teams
            if (teamState.teams.isEmpty() && !teamState.isLoading) {
                item {
                    EmptyTeamState(
                        isAdmin = isAdmin,
                        onJoinTeam = { teamViewModel.openJoinTeamDialog() }
                    )
                }
            } else if (teamState.teams.isNotEmpty()) {
                // User has teams - show unified team view
                if (isAdmin) {
                    // Admin view - show team selection and management
                    items(teamState.teams) { team ->
                        TeamCard(
                            team = team,
                            isSelected = teamState.selectedTeam?.teamId == team.teamId,
                            onTeamClick = { teamViewModel.selectTeam(team) },
                            onDeleteTeam = { teamViewModel.openDeleteTeamDialog(it) },
                            showDeleteButton = true,
                            onCopyTeamId = handleCopyTeamId,
                            onShareTeamId = handleShareTeamId
                        )
                    }
                }

                // For employees, always show their team info if they have a team
                // For admins, only show when a team is selected
                val teamToShow = if (isAdmin) {
                    teamState.selectedTeam
                } else {
                    // For employees, use selected team if available, otherwise use first team
                    teamState.selectedTeam ?: teamState.teams.firstOrNull()
                }

                teamToShow?.let { selectedTeam ->
                    item {
                        UnifiedTeamMembersSection(
                            team = selectedTeam,
                            members = teamState.teamMembers,
                            isAdmin = isAdmin,
                            currentUserId = currentUserId,
                            onAddMember = if (isAdmin) {
                                { teamViewModel.openAddMemberDialog() }
                            } else null,
                            onEditMember = if (isAdmin) {
                                { member -> teamViewModel.openEditMemberDialog(member) }
                            } else null,
                            onRemoveMember = if (isAdmin) {
                                { memberId -> teamViewModel.removeMember(memberId) }
                            } else null,
                            onLeaveTeam = if (!isAdmin) {
                                { teamViewModel.leaveTeam(currentUserId!!) }
                            } else null
                        )
                    }

                    // AI Insights section
                    item {
                        AIInsightsSection(selectedTeam, teamState.teamMembers)
                    }
                }
            }
        }

        // Dialogs
        if (teamState.isCreateTeamDialogOpen) {
            CreateTeamDialog(
                onDismiss = { teamViewModel.closeCreateTeamDialog() },
                onConfirm = { teamName ->
                    authState.currentUser?.uid?.let { adminId ->
                        teamViewModel.createTeam(teamName, adminId)
                    }
                }
            )
        }

        if (teamState.isDeleteTeamDialogOpen && teamState.teamToDelete != null) {
            DeleteTeamConfirmationDialog(
                team = teamState.teamToDelete!!,
                onDismiss = { teamViewModel.closeDeleteTeamDialog() },
                onConfirm = { teamToDelete ->
                    authState.currentUser?.uid?.let { adminId ->
                        teamViewModel.deleteTeam(teamToDelete.teamId, adminId)
                    }
                }
            )
        }

        if (teamState.isAddMemberDialogOpen) {
            AddMemberDialog(
                onDismiss = { teamViewModel.closeAddMemberDialog() },
                onConfirm = { githubUsername, role ->
                    teamState.selectedTeam?.let { team ->
                        authState.currentUser?.uid?.let { adminId ->
                            teamViewModel.addTeamMember(team.teamId, githubUsername, role, adminId)
                        }
                    }
                }
            )
        }

        if (teamState.isEditMemberDialogOpen && teamState.selectedMember != null) {
            EditMemberDialog(
                member = teamState.selectedMember!!,
                onDismiss = { teamViewModel.closeEditMemberDialog() },
                onConfirm = { memberId, newRole ->
                    teamViewModel.updateMemberRole(memberId, newRole)
                }
            )
        }

        if (teamState.isJoinTeamDialogOpen) {
            JoinTeamDialog(
                onDismiss = { teamViewModel.closeJoinTeamDialog() },
                onConfirm = { teamId ->
                    authState.currentUser?.uid?.let { userId ->
                        teamViewModel.joinTeam(teamId, userId)
                    }
                },
                isDisabled = isEmployeeInTeam
            )
        }
    }
}

@Composable
fun EmptyTeamState(
    isAdmin: Boolean,
    onJoinTeam: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isAdmin) {
                Text("No teams created yet")
                Text("Create your first team to get started",
                     style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("You're not part of any team yet")
                Text("Contact your admin to be added to a team or join with a team ID",
                     style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onJoinTeam,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Join Team",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Join Team with ID")
                }
            }
        }
    }
}

@Composable
fun UnifiedTeamMembersSection(
    team: Team,
    members: List<Map<String, Any>>,
    isAdmin: Boolean,
    currentUserId: String?,
    onAddMember: (() -> Unit)?,
    onEditMember: ((Map<String, Any>) -> Unit)?,
    onRemoveMember: ((String) -> Unit)?,
    onLeaveTeam: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAdmin) "Team Members" else "My Teammates",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                }

                // Action button based on role
                if (isAdmin && onAddMember != null) {
                    Button(
                        onClick = onAddMember,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add")
                    }
                } else if (!isAdmin && onLeaveTeam != null) {
                    OutlinedButton(
                        onClick = onLeaveTeam,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("Leave Team")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Team info for employees
            if (!isAdmin) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = team.teamName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Team ID: ${team.teamId}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "Created: ${team.createdAt}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Divider
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            Spacer(modifier = Modifier.height(16.dp))

            // Members list
            if (members.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isAdmin) "No team members yet" else "Your team is empty",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isAdmin) "Add members to start collaborating" else "Ask your admin to add team members",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Members list with proper spacing
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    members.forEach { member ->
                        UnifiedTeamMemberItem(
                            member = member,
                            isAdmin = isAdmin,
                            isCurrentUser = member["userId"]?.toString() == currentUserId,
                            onEdit = if (isAdmin && onEditMember != null) {
                                { onEditMember(member) }
                            } else null,
                            onRemove = if (isAdmin && onRemoveMember != null) {
                                {
                                    member["memberId"]?.toString()?.let { memberId ->
                                        onRemoveMember(memberId)
                                    }
                                }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UnifiedTeamMemberItem(
    member: Map<String, Any>,
    isAdmin: Boolean,
    isCurrentUser: Boolean,
    onEdit: (() -> Unit)?,
    onRemove: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = if (isCurrentUser)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = member["githubUsername"]?.toString()
                                ?: member["username"]?.toString()
                                ?: "Unknown",
                            fontWeight = FontWeight.Medium
                        )
                        if (isCurrentUser) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    text = "You",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                    Text(
                        text = member["role"]?.toString() ?: "No role",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action buttons (only for admin)
            if (isAdmin) {
                Row {
                    onEdit?.let { editAction ->
                        IconButton(onClick = editAction) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    onRemove?.let { removeAction ->
                        IconButton(onClick = removeAction) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateTeamDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var teamName by rememberSaveable { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Create New Team",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = teamName,
                    onValueChange = { teamName = it },
                    label = { Text("Team Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (teamName.isNotBlank()) {
                                onConfirm(teamName.trim())
                            }
                        },
                        enabled = teamName.isNotBlank()
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemberDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var githubUsername by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }

    val roles = listOf(
        "Frontend Developer",
        "Backend Developer",
        "Full Stack Developer",
        "UI/UX Designer",
        "DevOps Engineer",
        "QA Engineer",
        "Product Manager",
        "Data Analyst",
        "Mobile Developer"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Add Team Member",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = githubUsername,
                    onValueChange = { githubUsername = it },
                    label = { Text("GitHub Username") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = role,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Role") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        roles.forEach { roleOption ->
                            DropdownMenuItem(
                                text = { Text(roleOption) },
                                onClick = {
                                    role = roleOption
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (githubUsername.isNotBlank() && role.isNotBlank()) {
                                onConfirm(githubUsername.trim(), role)
                            }
                        },
                        enabled = githubUsername.isNotBlank() && role.isNotBlank()
                    ) {
                        Text("Add Member")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMemberDialog(
    member: Map<String, Any>,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var role by rememberSaveable { mutableStateOf(member["role"]?.toString() ?: "") }
    var expanded by rememberSaveable { mutableStateOf(false) }

    val roles = listOf(
        "Frontend Developer",
        "Backend Developer",
        "Full Stack Developer",
        "UI/UX Designer",
        "DevOps Engineer",
        "QA Engineer",
        "Product Manager",
        "Data Analyst",
        "Mobile Developer"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Edit Member Role",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Member: ${member["githubUsername"]}",
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = role,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Role") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        roles.forEach { roleOption ->
                            DropdownMenuItem(
                                text = { Text(roleOption) },
                                onClick = {
                                    role = roleOption
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            member["memberId"]?.toString()?.let { memberId ->
                                onConfirm(memberId, role)
                            }
                        }
                    ) {
                        Text("Update")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteTeamConfirmationDialog(
    team: Team,
    onDismiss: () -> Unit,
    onConfirm: (Team) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterHorizontally),
                    tint = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Delete Team?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Are you sure you want to delete '${team.teamName}'?",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "This will permanently delete the team and all its members. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(team) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete Team", color = MaterialTheme.colorScheme.onError)
                    }
                }
            }
        }
    }
}

@Composable
fun AIInsightsSection(team: Team, members: List<Map<String, Any>>) {
    val teamInsightsViewModel: TeamInsightsViewModel = hiltViewModel()
    val insights by teamInsightsViewModel.insights.collectAsState()
    val isLoading by teamInsightsViewModel.isLoading.collectAsState()

    // Restore cached insights when team changes (do not clear on rotation)
    LaunchedEffect(team.teamId) {
        teamInsightsViewModel.setCurrentTeam(team.teamId)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "AI Insights",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Insights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    if (isLoading) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    }
                }

                OutlinedButton(
                    onClick = {
                        val teamRoles = members.mapNotNull { it["role"]?.toString() }.filter { it.isNotEmpty() }
                        teamInsightsViewModel.generateTeamInsights(team, members.size, teamRoles)
                    },
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh AI Insights", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Refresh")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Generating team insights...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            } else if (insights.isEmpty()) {
                // Empty state when no insights are available
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "No insights yet. Tap Refresh to generate.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            } else {
                insights.forEach { insight ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = insight,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun generateTeamInsights(teamName: String): List<String> {
    // Use the team name to seed the pseudo-random generator for consistent but different insights
    val random = Random(teamName.hashCode())

    // Pool of possible insights
    val insightPool = listOf(
        "Team velocity is trending upward by ${5 + random.nextInt(20)}% this month",
        "Code review completion time has improved by ${1 + random.nextInt(8)} hours on average",
        "Sprint planning accuracy has reached ${75 + random.nextInt(20)}%",
        "Bug discovery to fix ratio is at a healthy ${random.nextInt(20) + 80}%",
        "Team collaboration score is ${70 + random.nextInt(30)}/100 based on commit patterns",
        "There's an opportunity to improve test coverage by ~${5 + random.nextInt(25)}%",
        "${3 + random.nextInt(8)} pending pull requests need review",
        "Documentation coverage could be increased for better onboarding efficiency",
        "Consider rotating tech leads for broader knowledge sharing",
        "DevOps automation could save approximately ${2 + random.nextInt(6)} hours per week",
        "Recommend implementing ${if (random.nextBoolean()) "weekly" else "bi-weekly"} tech sharing sessions"
    )

    // Select a subset of insights (3-5) based on the team name hash
    val numInsights = 3 + (teamName.length % 3)
    return insightPool.shuffled(random).take(numInsights)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinTeamDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    isDisabled: Boolean = false // New parameter to control button state
) {
    var teamId by rememberSaveable { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Join Team",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = teamId,
                    onValueChange = { teamId = it },
                    label = { Text("Team ID") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (teamId.isNotBlank()) {
                                onConfirm(teamId.trim())
                            }
                        },
                        enabled = teamId.isNotBlank() && !isDisabled // Disable button if isDisabled is true
                    ) {
                        Text("Join")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamCard(
    team: Team,
    isSelected: Boolean,
    onTeamClick: () -> Unit,
    onDeleteTeam: (Team) -> Unit,
    showDeleteButton: Boolean,
    onCopyTeamId: ((String) -> Unit)? = null,
    onShareTeamId: ((Team) -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onTeamClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Team Name and Created Date
            Text(
                text = team.teamName,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Created: ${team.createdAt}",
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Show Team ID and actions only when selected
            if (isSelected) {
                Spacer(modifier = Modifier.height(16.dp))

                // Team ID section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Team ID: ${team.teamId}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )

                    // Copy Team ID button
                    if (onCopyTeamId != null) {
                        Button(
                            onClick = { onCopyTeamId(team.teamId) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy Team ID",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy ID", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (onShareTeamId != null) {
                        OutlinedButton(
                            onClick = { onShareTeamId(team) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Share Team ID"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share")
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (showDeleteButton) {
                        OutlinedButton(
                            onClick = { onDeleteTeam(team) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            contentPadding = PaddingValues(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Team"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}
