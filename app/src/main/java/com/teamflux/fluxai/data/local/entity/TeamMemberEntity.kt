package com.teamflux.fluxai.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "team_members",
    primaryKeys = ["userId", "teamId"],
    foreignKeys = [
        ForeignKey(
            entity = TeamEntity::class,
            parentColumns = ["teamId"],
            childColumns = ["teamId"]
        )
    ],
    indices = [Index(value = ["teamId"])]
)
data class TeamMemberEntity(
    val userId: String,
    val teamId: String,
    val username: String,
    val role: String,
    val email: String,
    val phone: String
)
