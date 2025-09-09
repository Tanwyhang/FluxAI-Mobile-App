package com.teamflux.fluxai.data.local.dao

import androidx.room.*
import com.teamflux.fluxai.data.local.entity.CommitDataEntity

@Dao
interface EmployeePerformanceDao {
    @Query("SELECT * FROM commit_data WHERE employeeId = :employeeId")
    suspend fun getCommitData(employeeId: String): CommitDataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommitData(data: CommitDataEntity)

    @Query("DELETE FROM commit_data WHERE employeeId = :employeeId")
    suspend fun deleteCommitData(employeeId: String)

    @Query("DELETE FROM commit_data WHERE timestamp < :timestamp")
    suspend fun deleteOldData(timestamp: Long)
}
