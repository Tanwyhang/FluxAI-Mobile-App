package com.teamflux.fluxai.repository

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.teamflux.fluxai.model.Admin
import com.teamflux.fluxai.model.TeamMember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.net.UnknownHostException
import kotlin.coroutines.resume
import com.google.firebase.FirebaseNetworkException
import android.util.Log
import java.util.Date
import java.util.UUID

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    init {
        auth.setLanguageCode("en")
    }

    val currentUser: FirebaseUser? get() = auth.currentUser

    private fun isPlayServicesUpToDate(activity: Activity): Boolean {
        return try {
            val availability = com.google.android.gms.common.GoogleApiAvailability.getInstance()
            val status = availability.isGooglePlayServicesAvailable(activity)
            Log.d("AuthRepository", "Google Play Services status: $status")

            when (status) {
                com.google.android.gms.common.ConnectionResult.SUCCESS -> true
                com.google.android.gms.common.ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> {
                    Log.e("AuthRepository", "Google Play Services needs update. Status=$status")
                    false
                }
                com.google.android.gms.common.ConnectionResult.SERVICE_MISSING -> {
                    Log.e("AuthRepository", "Google Play Services is missing. Status=$status")
                    false
                }
                com.google.android.gms.common.ConnectionResult.SERVICE_DISABLED -> {
                    Log.e("AuthRepository", "Google Play Services is disabled. Status=$status")
                    false
                }
                else -> {
                    Log.e("AuthRepository", "Google Play Services error. Status=$status")
                    false
                }
            }
        } catch (e: Exception) {
            Log.w("AuthRepository", "Failed to check Play Services status", e)
            true // Don't block sign-in if check fails
        }
    }

    private fun classifyAuthError(exception: Exception): Exception {
        fun Throwable.causeChain(): Sequence<Throwable> = generateSequence(this) { it.cause }
        val chain = exception.causeChain().toList()
        val isNetwork = chain.any {
            it is FirebaseNetworkException || it is UnknownHostException ||
                    it.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
                    it.message?.contains("Failed to connect", ignoreCase = true) == true
        }

        return when {
            !isNetwork && exception.message?.contains("INTERNAL") == true ->
                Exception("Internal auth error. Please retry.")
            isNetwork ->
                Exception("Network issue. Check internet connection / DNS and try again.")
            exception.message?.contains("CANCELED") == true ->
                Exception("Authentication cancelled.")
            exception.message?.contains("SIGN_IN_CURRENTLY_IN_PROGRESS") == true ->
                Exception("Sign-in already in progress. Please wait.")
            exception.message?.contains("INVALID_PROVIDER_ID") == true ->
                Exception("GitHub provider misconfigured in Firebase Console.")
            exception.message?.contains("redirect_uri_mismatch") == true ->
                Exception("Redirect URI mismatch. Verify Firebase auth domain & GitHub OAuth callback.")
            exception.message?.contains("WEB_NETWORK_REQUEST_FAILED") == true ->
                Exception("Web network request failed. Check connectivity.")
            exception.message?.contains("WEB_INTERNAL_ERROR") == true ->
                Exception("Web internal error. Please retry.")
            else ->
                Exception("GitHub authentication failed: ${exception.message}")
        }
    }

    suspend fun signInWithGitHub(activity: Activity): Result<FirebaseUser> {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (!isPlayServicesUpToDate(activity)) {
                    return Result.failure(Exception("Google Play Services outdated. Update it in the emulator/device and retry."))
                }
            }

            val pendingResultTask = auth.pendingAuthResult
            if (pendingResultTask != null) {
                Log.d("AuthRepository", "Found pending GitHub auth result")
                return suspendCancellableCoroutine { continuation ->
                    pendingResultTask
                        .addOnSuccessListener { authResult ->
                            Log.d("AuthRepository", "GitHub pending auth successful for user: ${authResult.user?.email}")
                            repositoryScope.launch {
                                storeGitHubUserData(authResult)
                                requestFirestorePermissions(authResult.user!!)
                            }
                            continuation.resume(Result.success(authResult.user!!))
                        }
                        .addOnFailureListener { exception ->
                            Log.e("AuthRepository", "GitHub pending auth failed", exception)
                            continuation.resume(Result.failure(classifyAuthError(exception as Exception)))
                        }
                }
            }

            val provider = OAuthProvider.newBuilder("github.com")
                .setScopes(listOf("user:email", "read:user", "repo"))
                .addCustomParameters(mapOf("allow_signup" to "true"))
                .build()

            Log.d("AuthRepository", "Starting GitHub OAuth flow")
            suspendCancellableCoroutine { continuation ->
                auth.startActivityForSignInWithProvider(activity, provider)
                    .addOnSuccessListener { authResult ->
                        Log.d("AuthRepository", "GitHub sign-in successful for user: ${authResult.user?.email}")
                        repositoryScope.launch {
                            storeGitHubUserData(authResult)
                            requestFirestorePermissions(authResult.user!!)
                        }
                        continuation.resume(Result.success(authResult.user!!))
                    }
                    .addOnFailureListener { exception ->
                        Log.e("AuthRepository", "GitHub sign-in failed", exception)
                        continuation.resume(Result.failure(classifyAuthError(exception as Exception)))
                    }
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "GitHub sign-in setup failed", e)
            Result.failure(classifyAuthError(e))
        }
    }

    private suspend fun storeGitHubUserData(result: com.google.firebase.auth.AuthResult) {
        try {
            val user = result.user ?: return
            val additionalUserInfo = result.additionalUserInfo

            // Extract GitHub-specific data from AdditionalUserInfo
            val githubUsername = additionalUserInfo?.username
            val githubProfileUrl = githubUsername?.let { "https://github.com/$it" }

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

            Log.d("AuthRepository", "Stored GitHub user data for ${user.email} with username: $githubUsername")
        } catch (e: Exception) {
            Log.w("AuthRepository", "Failed to store GitHub user data", e)
            // Don't fail the entire auth process if this fails
        }
    }

    private suspend fun requestFirestorePermissions(user: FirebaseUser) {
        try {
            val testData = mapOf(
                "permissionTest" to System.currentTimeMillis(),
                "firestoreWritePermission" to "granted"
            )

            firestore.collection("users").document(user.uid)
                .update(testData)
                .await()

            Log.d("AuthRepository", "Firestore write permission confirmed for user: ${user.uid}")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Firestore permission test failed for user: ${user.uid}", e)
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
            val userDoc = firestore.collection("users").document(userId).get().await()
            val githubData = userDoc.data ?: emptyMap()

            val admin = Admin(
                adminId = userId,
                username = username,
                email = email,
                phone = phone
            )

            val adminData = mutableMapOf<String, Any?>()
            adminData.putAll(admin.toMap())
            githubData["githubUsername"]?.let { adminData["githubUsername"] = it }
            githubData["avatarUrl"]?.let { adminData["avatarUrl"] = it }
            adminData["createdAt"] = System.currentTimeMillis()

            firestore.collection("admins").document(userId).set(adminData).await()

            firestore.collection("users").document(userId)
                .update("role", "admin")
                .await()

            Log.d("AuthRepository", "Admin profile created successfully for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to create admin profile for user: $userId", e)
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
            Log.d("AuthRepository", "Creating team member profile for userId: $userId, teamId: $teamId")

            val userDoc = firestore.collection("users").document(userId).get().await()
            val githubData = userDoc.data ?: emptyMap()
            val githubUsername = githubData["githubUsername"] as? String ?: ""

            Log.d("AuthRepository", "Found GitHub username: $githubUsername")

            val memberId = UUID.randomUUID().toString()

            val memberData = mapOf(
                "memberId" to memberId,
                "userId" to userId,
                "teamId" to teamId,
                "githubUsername" to githubUsername,
                "role" to role,
                "email" to email,
                "phone" to phone,
                "githubProfileLink" to githubProfileLink,
                "avatarUrl" to (githubData["avatarUrl"] ?: ""),
                "status" to "active",
                "createdAt" to Date(),
                "addedBy" to userId
            )

            firestore.collection("teamMembers").document(memberId).set(memberData).await()

            Log.d("AuthRepository", "Team member document created with ID: $memberId")

            firestore.collection("users").document(userId)
                .update(mapOf(
                    "role" to "team_member",
                    "teamId" to teamId
                ))
                .await()

            Log.d("AuthRepository", "User document updated with team information")
            Log.d("AuthRepository", "Team member profile created successfully for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to create team member profile for user: $userId", e)
            Result.failure(e)
        }
    }

    suspend fun getUserRole(userId: String): String? {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val role = userDoc.getString("role")
            if (role != null) {
                return role
            }

            val adminDoc = firestore.collection("admins").document(userId).get().await()
            if (adminDoc.exists()) {
                return "admin"
            }

            val memberDocs = firestore.collection("teamMembers")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            if (memberDocs.documents.isNotEmpty()) {
                return "team_member"
            }

            null
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to get user role for: $userId", e)
            null
        }
    }

    suspend fun getUserRoles(userId: String): List<String> {
        return try {
            val roles = mutableListOf<String>()

            val adminDoc = firestore.collection("admins").document(userId).get().await()
            if (adminDoc.exists()) {
                roles.add("admin")
            }

            val memberDocs = firestore.collection("teamMembers")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            if (memberDocs.documents.isNotEmpty()) {
                roles.add("team_member")
            }

            Log.d("AuthRepository", "User $userId has roles: $roles")
            roles
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to get user roles for $userId", e)
            emptyList()
        }
    }

    suspend fun signInWithDifferentGitHubAccount(activity: Activity): Result<FirebaseUser> {
        return try {
            auth.signOut()
            Log.d("AuthRepository", "Clearing pending auth result for fresh login")

            if (!isPlayServicesUpToDate(activity)) {
                return Result.failure(Exception("Google Play Services outdated. Update it and retry."))
            }

            val provider = OAuthProvider.newBuilder("github.com")
                .setScopes(listOf("user:email", "read:user", "repo"))
                .addCustomParameters(mapOf(
                    "allow_signup" to "true",
                    "prompt" to "select_account",
                    "login_hint" to ""
                ))
                .build()

            Log.d("AuthRepository", "Starting fresh GitHub OAuth flow with account selection")
            suspendCancellableCoroutine { continuation ->
                auth.startActivityForSignInWithProvider(activity, provider)
                    .addOnSuccessListener { authResult ->
                        Log.d("AuthRepository", "Fresh GitHub sign-in successful for user: ${authResult.user?.email}")
                        repositoryScope.launch {
                            storeGitHubUserData(authResult)
                            requestFirestorePermissions(authResult.user!!)
                        }
                        continuation.resume(Result.success(authResult.user!!))
                    }
                    .addOnFailureListener { exception ->
                        Log.e("AuthRepository", "Fresh GitHub sign-in failed", exception)
                        continuation.resume(Result.failure(classifyAuthError(exception as Exception)))
                    }
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Fresh GitHub sign-in setup failed", e)
            Result.failure(classifyAuthError(e))
        }
    }

    fun clearAuthState() {
        auth.signOut()
        Log.d("AuthRepository", "Auth state cleared for account switching")
    }

    fun signOut() {
        auth.signOut()
    }
}

// Extension function to convert data classes to Map
private fun Admin.toMap(): Map<String, Any?> = mapOf(
    "adminId" to adminId,
    "email" to email
)

private fun TeamMember.toMap(): Map<String, Any?> = mapOf(
    "userId" to userId,
    "teamId" to teamId,
    "username" to githubUsername,
    "role" to role,
    "email" to email,
    "phone" to phone
)