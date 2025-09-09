package com.teamflux.fluxai.repository

import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.teamflux.fluxai.model.Admin
import com.teamflux.fluxai.model.TeamMember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.resume

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    init {
        // Set explicit language for Firebase Auth to avoid locale warnings
        auth.setLanguageCode("en") // or use Locale.getDefault().language
    }

    val currentUser: FirebaseUser? get() = auth.currentUser

    suspend fun signInWithGitHub(activity: Activity): Result<FirebaseUser> {
        return try {
            // First check if there's a pending result (critical for activity lifecycle)
            val pendingResultTask = auth.pendingAuthResult
            if (pendingResultTask != null) {
                android.util.Log.d("AuthRepository", "Found pending GitHub auth result")
                // There's something already here! Finish the sign-in for your user.
                return suspendCancellableCoroutine { continuation ->
                    pendingResultTask
                        .addOnSuccessListener { authResult ->
                            android.util.Log.d("AuthRepository", "GitHub pending auth successful for user: ${authResult.user?.email}")
                            // Store GitHub data and request Firestore permissions in background
                            repositoryScope.launch {
                                storeGitHubUserData(authResult)
                                requestFirestorePermissions(authResult.user!!)
                            }
                            continuation.resume(Result.success(authResult.user!!))
                        }
                        .addOnFailureListener { exception ->
                            android.util.Log.e("AuthRepository", "GitHub pending auth failed", exception)
                            continuation.resume(Result.failure(exception))
                        }
                }
            }

            // No pending result, start new sign-in flow
            val provider = OAuthProvider.newBuilder("github.com")
                .setScopes(listOf("user:email", "read:user", "repo"))
                .addCustomParameters(mapOf(
                    "allow_signup" to "true"
                ))
                .build()

            android.util.Log.d("AuthRepository", "Starting GitHub OAuth flow")

            // Use callback-based approach to handle activity lifecycle properly
            suspendCancellableCoroutine { continuation ->
                auth.startActivityForSignInWithProvider(activity, provider)
                    .addOnSuccessListener { authResult ->
                        android.util.Log.d("AuthRepository", "GitHub sign-in successful for user: ${authResult.user?.email}")

                        // Store GitHub user data and request Firestore permissions in background
                        repositoryScope.launch {
                            storeGitHubUserData(authResult)
                            requestFirestorePermissions(authResult.user!!)
                        }

                        continuation.resume(Result.success(authResult.user!!))
                    }
                    .addOnFailureListener { exception ->
                        android.util.Log.e("AuthRepository", "GitHub sign-in failed", exception)

                        val friendlyError = when {
                            exception.message?.contains("CANCELED") == true -> {
                                Exception("Authentication was cancelled")
                            }
                            exception.message?.contains("NETWORK_ERROR") == true -> {
                                Exception("Network error. Please check your connection and try again.")
                            }
                            exception.message?.contains("SIGN_IN_CURRENTLY_IN_PROGRESS") == true -> {
                                Exception("Sign-in already in progress. Please wait.")
                            }
                            exception.message?.contains("INVALID_PROVIDER_ID") == true -> {
                                Exception("GitHub provider not configured properly. Please check Firebase setup.")
                            }
                            exception.message?.contains("WEB_CONTEXT_CANCELED") == true -> {
                                Exception("Authentication was cancelled by user")
                            }
                            exception.message?.contains("WEB_NETWORK_REQUEST_FAILED") == true -> {
                                Exception("Network request failed. Please check your internet connection.")
                            }
                            exception.message?.contains("WEB_INTERNAL_ERROR") == true -> {
                                Exception("Internal error occurred. Please try again.")
                            }
                            exception.message?.contains("redirect_uri_mismatch") == true -> {
                                Exception("OAuth redirect configuration error. Please check Firebase and GitHub settings.")
                            }
                            else -> {
                                Exception("GitHub authentication failed: ${exception.message}")
                            }
                        }

                        continuation.resume(Result.failure(friendlyError))
                    }
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "GitHub sign-in setup failed", e)
            Result.failure(e)
        }
    }

    private suspend fun storeGitHubUserData(result: com.google.firebase.auth.AuthResult) {
        try {
            val user = result.user ?: return
            val additionalUserInfo = result.additionalUserInfo

            // Extract GitHub-specific data from AdditionalUserInfo
            val githubUsername = additionalUserInfo?.username
            val githubProfileUrl = if (githubUsername != null) {
                "https://github.com/$githubUsername"
            } else {
                null
            }

            // Get profile data from additionalUserInfo
            val profileData = additionalUserInfo?.profile
            val avatarUrl = profileData?.get("avatar_url") as? String
            val publicRepos = profileData?.get("public_repos") as? Int
            val followers = profileData?.get("followers") as? Int
            val following = profileData?.get("following") as? Int
            val company = profileData?.get("company") as? String
            val location = profileData?.get("location") as? String
            val bio = profileData?.get("bio") as? String
            val name = profileData?.get("name") as? String

            // Store comprehensive GitHub data in Firestore
            val githubData = mutableMapOf<String, Any?>(
                "email" to user.email,
                "displayName" to (name ?: user.displayName),
                "lastSignInTime" to System.currentTimeMillis(),
                "provider" to "github.com",
                "isNewUser" to (additionalUserInfo?.isNewUser == true),
                "firestorePermissionGranted" to true
            )

            // Add GitHub-specific data if available
            githubUsername?.let { githubData["githubUsername"] = it }
            githubProfileUrl?.let { githubData["githubProfileUrl"] = it }
            avatarUrl?.let { githubData["avatarUrl"] = it }
            publicRepos?.let { githubData["publicRepos"] = it }
            followers?.let { githubData["followers"] = it }
            following?.let { githubData["following"] = it }
            company?.let { githubData["company"] = it }
            location?.let { githubData["location"] = it }
            bio?.let { githubData["bio"] = it }

            firestore.collection("users").document(user.uid)
                .set(githubData, com.google.firebase.firestore.SetOptions.merge())
                .await()

            android.util.Log.d("AuthRepository", "Stored GitHub user data for ${user.email} with username: $githubUsername")
        } catch (e: Exception) {
            android.util.Log.w("AuthRepository", "Failed to store GitHub user data", e)
            // Don't fail the entire auth process if this fails
        }
    }

    private suspend fun requestFirestorePermissions(user: FirebaseUser) {
        try {
            // Test write permission by attempting to update user document
            val testData = mapOf(
                "permissionTest" to System.currentTimeMillis(),
                "firestoreWritePermission" to "granted"
            )

            firestore.collection("users").document(user.uid)
                .update(testData)
                .await()

            android.util.Log.d("AuthRepository", "Firestore write permission confirmed for user: ${user.uid}")
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Firestore permission test failed for user: ${user.uid}", e)
            // This will help debug permission issues
            throw Exception("Firestore write permission denied. Please check security rules.", e)
        }
    }

    suspend fun createAdminProfile(
        userId: String,
        username: String,
        email: String,
        phone: String
    ): Result<Unit> {
        return try {
            // Get existing GitHub data from users collection
            val userDoc = firestore.collection("users").document(userId).get().await()
            val githubData = userDoc.data ?: emptyMap()

            val admin = Admin(
                adminId = userId,
                username = username,
                email = email,
                phone = phone
            )

            // Store admin profile WITHOUT username/phone
            val adminData = mutableMapOf<String, Any?>()
            adminData.putAll(admin.toMap()) // now only adminId + email
            adminData["githubUsername"] = githubData["githubUsername"]
            adminData["avatarUrl"] = githubData["avatarUrl"]
            adminData["createdAt"] = System.currentTimeMillis()

            firestore.collection("admins").document(userId).set(adminData).await()

            // Update user document with role
            firestore.collection("users").document(userId)
                .update("role", "admin")
                .await()

            android.util.Log.d("AuthRepository", "Admin profile created successfully for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Failed to create admin profile for user: $userId", e)
            Result.failure(e)
        }
    }

    suspend fun createTeamMemberProfile(
        userId: String,
        teamId: String,
        role: String,
        githubProfileLink: String,
        email: String,
        phone: String
    ): Result<Unit> {
        return try {
            println("AuthRepository: Creating team member profile for userId: $userId, teamId: $teamId")

            // Get existing GitHub data from users collection
            val userDoc = firestore.collection("users").document(userId).get().await()
            val githubData = userDoc.data ?: emptyMap()
            val githubUsername = githubData["githubUsername"] as? String ?: ""

            println("AuthRepository: Found GitHub username: $githubUsername")

            // Generate a unique member ID for the document
            val memberId = java.util.UUID.randomUUID().toString()

            // Create team member data with all required fields
            val memberData = mapOf(
                "memberId" to memberId,
                "userId" to userId,  // Store userId as a field, not document ID
                "teamId" to teamId,
                "githubUsername" to githubUsername,
                "role" to role,
                "email" to email,
                "phone" to phone,
                "githubProfileLink" to githubProfileLink,
                "avatarUrl" to (githubData["avatarUrl"] ?: ""),
                "status" to "active",  // Important: set status to active
                "createdAt" to java.util.Date(),
                "addedBy" to userId  // Self-joined
            )

            // Store with generated memberId as document ID (not userId)
            firestore.collection("teamMembers").document(memberId).set(memberData).await()

            println("AuthRepository: Team member document created with ID: $memberId")

            // Update user document with role and teamId
            firestore.collection("users").document(userId)
                .update(mapOf(
                    "role" to "team_member",
                    "teamId" to teamId
                ))
                .await()

            println("AuthRepository: User document updated with team information")
            android.util.Log.d("AuthRepository", "Team member profile created successfully for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            println("AuthRepository: Error creating team member profile: ${e.message}")
            android.util.Log.e("AuthRepository", "Failed to create team member profile for user: $userId", e)
            Result.failure(e)
        }
    }

    suspend fun getUserRole(userId: String): String? {
        return try {
            // First check user document for role
            val userDoc = firestore.collection("users").document(userId).get().await()
            val role = userDoc.getString("role")
            if (role != null) {
                return role
            }

            // Fallback: Check if user is admin
            val adminDoc = firestore.collection("admins").document(userId).get().await()
            if (adminDoc.exists()) {
                return "admin"
            }

            // Check if user is team member
            val memberDoc = firestore.collection("teamMembers").document(userId).get().await()
            if (memberDoc.exists()) {
                return "team_member"
            }

            null
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Failed to get user role for: $userId", e)
            null
        }
    }

    suspend fun getUserRoles(userId: String): List<String> {
        return try {
            val roles = mutableListOf<String>()

            // Check if user is an admin
            val adminDoc = firestore.collection("admins").document(userId).get().await()
            if (adminDoc.exists()) {
                roles.add("admin")
            }

            // Check if user is a team member
            val teamMemberDoc = firestore.collection("teamMembers").document(userId).get().await()
            if (teamMemberDoc.exists()) {
                roles.add("team_member")
            }

            android.util.Log.d("AuthRepository", "User $userId has roles: $roles")
            roles
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Failed to get user roles for $userId", e)
            emptyList()
        }
    }

    suspend fun signInWithDifferentGitHubAccount(activity: Activity): Result<FirebaseUser> {
        return try {
            // First sign out to clear any existing session
            auth.signOut()

            // Clear any pending auth results to ensure fresh flow
            auth.pendingAuthResult?.let { pendingTask ->
                // Cancel any pending operations if possible
                android.util.Log.d("AuthRepository", "Clearing pending auth result for fresh login")
            }

            // Force a fresh authentication by adding prompt parameter
            val provider = OAuthProvider.newBuilder("github.com")
                .setScopes(listOf("user:email", "read:user", "repo"))
                .addCustomParameters(mapOf(
                    "allow_signup" to "true",
                    "prompt" to "select_account", // Force account selection
                    "login_hint" to "", // Clear any login hints
                ))
                .build()

            android.util.Log.d("AuthRepository", "Starting fresh GitHub OAuth flow with account selection")

            suspendCancellableCoroutine { continuation ->
                auth.startActivityForSignInWithProvider(activity, provider)
                    .addOnSuccessListener { authResult ->
                        android.util.Log.d("AuthRepository", "Fresh GitHub sign-in successful for user: ${authResult.user?.email}")

                        // Store GitHub user data and request Firestore permissions in background
                        repositoryScope.launch {
                            storeGitHubUserData(authResult)
                            requestFirestorePermissions(authResult.user!!)
                        }

                        continuation.resume(Result.success(authResult.user!!))
                    }
                    .addOnFailureListener { exception ->
                        android.util.Log.e("AuthRepository", "Fresh GitHub sign-in failed", exception)

                        val friendlyError = when {
                            exception.message?.contains("CANCELED") == true -> {
                                Exception("Authentication was cancelled")
                            }
                            exception.message?.contains("NETWORK_ERROR") == true -> {
                                Exception("Network error. Please check your connection and try again.")
                            }
                            else -> {
                                Exception("GitHub authentication failed: ${exception.message}")
                            }
                        }

                        continuation.resume(Result.failure(friendlyError))
                    }
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Fresh GitHub sign-in setup failed", e)
            Result.failure(e)
        }
    }

    fun clearAuthState() {
        // Clear Firebase Auth state completely
        auth.signOut()
        android.util.Log.d("AuthRepository", "Auth state cleared for account switching")
    }

    fun signOut() {
        auth.signOut()
    }
}

// Extension function to convert data classes to Map
private fun Admin.toMap(): Map<String, Any?> = mapOf(
    "adminId" to adminId,
    // remove username and phone storage per requirement
    "email" to email
)

private fun TeamMember.toMap(): Map<String, Any?> = mapOf(
    "userId" to userId,
    "teamId" to teamId,
    "username" to username,
    "role" to role,
    "email" to email,
    "phone" to phone
)
