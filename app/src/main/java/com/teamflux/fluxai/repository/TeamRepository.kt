package com.teamflux.fluxai.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.teamflux.fluxai.model.Team
import com.teamflux.fluxai.model.TeamMember
import kotlinx.coroutines.tasks.await
import java.util.*

class TeamRepository {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun createTeam(
        teamName: String,
        createdBy: String
    ): Result<String> {
        return try {
            val teamId = UUID.randomUUID().toString()
            val team = Team(
                teamId = teamId,
                teamName = teamName,
                createdBy = createdBy,
                createdAt = Date()
            )
            firestore.collection("teams").document(teamId).set(team).await()
            Result.success(teamId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTeamsByAdmin(adminId: String): Result<List<Team>> {
        return try {
            val snapshot = firestore.collection("teams")
                .whereEqualTo("createdBy", adminId)
                .get()
                .await()

            val teams = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Team::class.java)
            }
            Result.success(teams)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTeamMembers(teamId: String): Result<List<TeamMember>> {
        return try {
            val snapshot = firestore.collection("teamMembers")
                .whereEqualTo("teamId", teamId)
                .get()
                .await()

            val members = snapshot.documents.mapNotNull { doc ->
                doc.toObject(TeamMember::class.java)
            }
            Result.success(members)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTeamById(teamId: String): Result<Team?> {
        return try {
            val document = firestore.collection("teams").document(teamId).get().await()
            val team = document.toObject(Team::class.java)
            Result.success(team)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTeam(team: Team): Result<Unit> {
        return try {
            firestore.collection("teams").document(team.teamId).set(team).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTeam(teamId: String): Result<Unit> {
        return try {
            firestore.collection("teams").document(teamId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addTeamMemberByGithubUsername(
        teamId: String,
        githubUsername: String,
        role: String,
        addedBy: String
    ): Result<String> {
        return try {
            // First, validate that the GitHub username exists in our users collection
            val userValidation = validateGithubUserExists(githubUsername)
            if (userValidation.isFailure) {
                return Result.failure(userValidation.exceptionOrNull() ?: Exception("User validation failed"))
            }

            val userId = userValidation.getOrThrow()

            // Check if user is already a member of this team
            val existingMemberCheck = firestore.collection("teamMembers")
                .whereEqualTo("userId", userId)
                .whereEqualTo("teamId", teamId)
                .get()
                .await()

            if (!existingMemberCheck.isEmpty) {
                return Result.failure(Exception("User is already a member of this team"))
            }

            val memberId = UUID.randomUUID().toString()
            val member = hashMapOf(
                "memberId" to memberId,
                "teamId" to teamId,
                "userId" to userId, // Add userId for proper user linking
                "githubUsername" to githubUsername,
                "role" to role,
                "status" to "active",
                "addedBy" to addedBy,
                "addedAt" to Date()
            )

            firestore.collection("teamMembers").document(memberId).set(member).await()
            Result.success(memberId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun validateGithubUserExists(githubUsername: String): Result<String> {
        return try {
            // Query users collection to find user with matching GitHub username
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("githubUsername", githubUsername)
                .limit(1)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                Result.failure(Exception("User not registered in FluxAI, please inform your employee"))
            } else {
                val userDoc = querySnapshot.documents.first()
                val userId = userDoc.id
                Result.success(userId)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to validate GitHub user: ${e.message}"))
        }
    }

    suspend fun updateTeamMemberRole(memberId: String, newRole: String): Result<Unit> {
        return try {
            firestore.collection("teamMembers")
                .document(memberId)
                .update("role", newRole, "updatedAt", Date())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeTeamMember(memberId: String): Result<Unit> {
        return try {
            firestore.collection("teamMembers").document(memberId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTeamMembersByTeamId(teamId: String): Result<List<Map<String, Any>>> {
        return try {
            // First try strict query using status == active
            var snapshot = firestore.collection("teamMembers")
                .whereEqualTo("teamId", teamId)
                .whereEqualTo("status", "active")
                .get()
                .await()

            // Fallback for legacy docs without status field
            if (snapshot.isEmpty) {
                snapshot = firestore.collection("teamMembers")
                    .whereEqualTo("teamId", teamId)
                    .get()
                    .await()
            }

            val members = snapshot.documents.map { doc ->
                doc.data?.plus("memberId" to doc.id) ?: emptyMap()
            }
            Result.success(members)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEmployeeTeam(employeeId: String): Result<Team?> {
        return try {
            println("TeamRepository: Searching for team membership for userId: $employeeId")

            // 1) Primary: active membership
            var membershipSnapshot = firestore.collection("teamMembers")
                .whereEqualTo("userId", employeeId)
                .whereEqualTo("status", "active")
                .limit(1)
                .get()
                .await()

            println("TeamRepository: Found ${membershipSnapshot.size()} team memberships (active)")

            // 2) Fallback: any membership (legacy without status field)
            if (membershipSnapshot.isEmpty) {
                membershipSnapshot = firestore.collection("teamMembers")
                    .whereEqualTo("userId", employeeId)
                    .limit(1)
                    .get()
                    .await()
                println("TeamRepository: Fallback query found ${membershipSnapshot.size()} memberships (any status)")
            }

            // 3) Fallback: users/{uid}.teamId reference
            var teamId: String? = null
            var createdMembership = false
            if (!membershipSnapshot.isEmpty) {
                val membership = membershipSnapshot.documents.first()
                teamId = membership.getString("teamId")
                println("TeamRepository: Using teamId from membership: $teamId")
            } else {
                val userDoc = firestore.collection("users").document(employeeId).get().await()
                teamId = userDoc.getString("teamId")
                println("TeamRepository: Using teamId from user doc: $teamId")

                // Auto-heal: if user has a teamId but no membership doc, create one
                if (!teamId.isNullOrBlank()) {
                    try {
                        val newMemberId = UUID.randomUUID().toString()
                        val username = userDoc.getString("githubUsername") ?: userDoc.getString("displayName") ?: ""
                        val avatarUrl = userDoc.getString("avatarUrl") ?: ""
                        val memberData = mapOf(
                            "memberId" to newMemberId,
                            "userId" to employeeId,
                            "teamId" to teamId!!,
                            "githubUsername" to username,
                            "role" to (userDoc.getString("role") ?: "Developer"),
                            "status" to "active",
                            "avatarUrl" to avatarUrl,
                            "addedBy" to employeeId,
                            "addedAt" to Date()
                        )
                        firestore.collection("teamMembers").document(newMemberId).set(memberData).await()
                        createdMembership = true
                        println("TeamRepository: Auto-created membership $newMemberId for user $employeeId in team $teamId")
                    } catch (ce: Exception) {
                        println("TeamRepository: Failed to auto-create membership: ${ce.message}")
                    }
                }
            }

            if (teamId.isNullOrBlank()) {
                println("TeamRepository: No teamId found for user $employeeId")
                return Result.success(null)
            }

            val teamSnapshot = firestore.collection("teams")
                .document(teamId!!)
                .get()
                .await()

            if (!teamSnapshot.exists()) {
                println("TeamRepository: Team document $teamId does not exist")
                return Result.success(null)
            }

            val team = teamSnapshot.toObject(Team::class.java)
            println("TeamRepository: Successfully loaded team: ${team?.teamName}. Auto-heal createdMembership=$createdMembership")
            Result.success(team)
        } catch (e: Exception) {
            println("TeamRepository: Exception in getEmployeeTeam: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun removeUserFromTeam(userId: String): Result<Unit> {
        return try {
            // Find and remove the user's team membership
            val membershipSnapshot = firestore.collection("teamMembers")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "active")
                .get()
                .await()

            if (!membershipSnapshot.isEmpty) {
                val membershipDoc = membershipSnapshot.documents.first()
                firestore.collection("teamMembers")
                    .document(membershipDoc.id)
                    .update("status", "inactive")
                    .await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
