package com.teamflux.fluxai.data.local.dao

import androidx.room.*
import com.teamflux.fluxai.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE userId = :userId")
    fun getUserProfile(userId: String): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfileEntity)

    @Update
    suspend fun updateUserProfile(profile: UserProfileEntity)

    @Delete
    suspend fun deleteUserProfile(profile: UserProfileEntity)
}
