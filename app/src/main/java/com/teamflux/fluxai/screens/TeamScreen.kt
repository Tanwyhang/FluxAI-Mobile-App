package com.teamflux.fluxai.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class TeamMember(
    val name: String,
    val role: String,
    val githubLink: String,
    val email: String,
    val phone: String
)

@Composable
fun TeamScreen() {
    val teamMembers = remember {
        listOf(
            TeamMember("John Doe", "Frontend Developer", "github.com/johndoe", "john@example.com", "+1234567890"),
            TeamMember("Jane Smith", "Backend Developer", "github.com/janesmith", "jane@example.com", "+1234567891"),
            TeamMember("Mike Johnson", "UI/UX Designer", "github.com/mikejohnson", "mike@example.com", "+1234567892"),
            TeamMember("Sarah Wilson", "Product Manager", "github.com/sarahwilson", "sarah@example.com", "+1234567893"),
            TeamMember("David Brown", "DevOps Engineer", "github.com/davidbrown", "david@example.com", "+1234567894"),
            TeamMember("Emily Davis", "QA Engineer", "github.com/emilydavis", "emily@example.com", "+1234567895"),
            TeamMember("Alex Chen", "Full Stack Developer", "github.com/alexchen", "alex@example.com", "+1234567896"),
            TeamMember("Lisa Garcia", "Data Analyst", "github.com/lisagarcia", "lisa@example.com", "+1234567897")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Team Lineup",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Team insights card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Flat design
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "AI Team Insights",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Well-balanced team composition", color = MaterialTheme.colorScheme.onSurface)
                Text("• Strong technical diversity", color = MaterialTheme.colorScheme.onSurface)
                Text("• Recommended: Add one more senior developer", color = MaterialTheme.colorScheme.onSurface)
                Text("• Team collaboration score: 9.2/10", color = MaterialTheme.colorScheme.onSurface)
            }
        }

        // Team members list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(teamMembers) { member ->
                TeamMemberCard(member = member)
            }
        }
    }
}

@Composable
fun TeamMemberCard(member: TeamMember) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Flat design
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar placeholder
            Card(
                modifier = Modifier.size(56.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Flat design
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Member details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = member.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = member.role,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                Text(
                    text = member.githubLink,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp
                )
                Text(
                    text = member.email,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}
