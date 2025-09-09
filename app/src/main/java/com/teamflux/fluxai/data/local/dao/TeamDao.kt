package com.teamflux.fluxai.data.local.dao

import androidx.room.*
import com.teamflux.fluxai.data.local.entity.TeamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {
    @Query("SELECT * FROM teams")
    fun getAllTeams(): Flow<List<TeamEntity>>

    @Query("SELECT * FROM teams WHERE teamId = :teamId")
    fun getTeamById(teamId: String): Flow<TeamEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeam(team: TeamEntity)

    @Update
    suspend fun updateTeam(team: TeamEntity)

    @Delete
    suspend fun deleteTeam(team: TeamEntity)
}
