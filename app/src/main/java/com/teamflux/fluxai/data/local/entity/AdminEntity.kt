package com.teamflux.fluxai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "admins")
data class AdminEntity(
    @PrimaryKey
    val adminId: String,
    val username: String,
    val email: String,
    val phone: String
)
