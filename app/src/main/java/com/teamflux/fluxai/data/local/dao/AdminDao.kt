package com.teamflux.fluxai.data.local.dao

import androidx.room.*
import com.teamflux.fluxai.data.local.entity.AdminEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AdminDao {
    @Query("SELECT * FROM admins WHERE adminId = :adminId")
    fun getAdmin(adminId: String): Flow<AdminEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdmin(admin: AdminEntity)

    @Update
    suspend fun updateAdmin(admin: AdminEntity)

    @Delete
    suspend fun deleteAdmin(admin: AdminEntity)
}
