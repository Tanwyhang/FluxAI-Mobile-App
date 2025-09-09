package com.teamflux.fluxai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey
    val userId: String,
    val displayName: String?,
    val email: String?,
    val phone: String?,
    val githubUsername: String?,
    val githubProfileUrl: String?,
    val avatarUrl: String?
)
