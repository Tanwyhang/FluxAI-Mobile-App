package com.teamflux.fluxai.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.teamflux.fluxai.model.UserProfile
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    fun currentUserId(): String? = auth.currentUser?.uid

    fun getCurrentUserId(): String = auth.currentUser?.uid ?: ""

    suspend fun getCurrentUserRole(): String {
        val uid = auth.currentUser?.uid ?: return "team_member"
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            doc.getString("role") ?: "team_member"
        } catch (_: Exception) {
            "team_member"
        }
    }

    suspend fun getUserProfile(userId: String): Result<UserProfile> {
        var displayName: String? = null
        var email: String? = null
        var phone: String? = null
        var githubUsername: String? = null
        var githubProfileUrl: String? = null
        var avatarUrl: String? = null
        return try {
            // 1) Base: users/{uid}
            val userDoc = firestore.collection("users").document(userId).get().await()
            if (userDoc.exists()) {
                displayName = userDoc.getString("displayName") ?: displayName
                email = userDoc.getString("email") ?: email
                avatarUrl = userDoc.getString("avatarUrl") ?: avatarUrl
                githubUsername = userDoc.getString("githubUsername") ?: githubUsername
                githubProfileUrl = userDoc.getString("githubProfileUrl") ?: githubProfileUrl
                phone = userDoc.getString("phone") ?: phone
            }

            // 2) Admin profile fallback (do NOT use username/phone)
            val adminDoc = firestore.collection("admins").document(userId).get().await()
            if (adminDoc.exists()) {
                if (avatarUrl.isNullOrBlank()) avatarUrl = adminDoc.getString("avatarUrl")
                if (githubUsername.isNullOrBlank()) githubUsername = adminDoc.getString("githubUsername")
            }

            // 3) Team member fallback for phone/email when active
            val memberQuery = firestore.collection("teamMembers")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "active")
                .limit(1)
                .get()
                .await()
            if (!memberQuery.isEmpty) {
                val memberDoc = memberQuery.documents.first()
                if (email.isNullOrBlank()) email = memberDoc.getString("email")
                if (phone.isNullOrBlank()) phone = memberDoc.getString("phone")
                if (githubUsername.isNullOrBlank()) githubUsername = memberDoc.getString("githubUsername")
                if (avatarUrl.isNullOrBlank()) avatarUrl = memberDoc.getString("avatarUrl")
            }

            // 4) Construct github profile url if missing
            if (githubProfileUrl.isNullOrBlank() && !githubUsername.isNullOrBlank()) {
                githubProfileUrl = "https://github.com/$githubUsername"
            }

            val profile = UserProfile(
                displayName = displayName,
                email = email,
                phone = phone,
                githubUsername = githubUsername,
                githubProfileUrl = githubProfileUrl,
                avatarUrl = avatarUrl
            )
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
