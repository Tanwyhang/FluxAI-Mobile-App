package com.teamflux.fluxai.data.local.dao

import androidx.room.*
import com.teamflux.fluxai.data.local.entity.TeamMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamMemberDao {
    @Query("SELECT * FROM team_members WHERE teamId = :teamId")
    fun getTeamMembers(teamId: String): Flow<List<TeamMemberEntity>>

    @Query("SELECT * FROM team_members WHERE userId = :userId")
    fun getTeamMemberById(userId: String): Flow<TeamMemberEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeamMember(member: TeamMemberEntity)

    @Update
    suspend fun updateTeamMember(member: TeamMemberEntity)

    @Delete
    suspend fun deleteTeamMember(member: TeamMemberEntity)

    @Query("SELECT * FROM team_members WHERE userId = :userId AND teamId = :teamId")
    fun getTeamMemberRole(userId: String, teamId: String): Flow<TeamMemberEntity?>
}
