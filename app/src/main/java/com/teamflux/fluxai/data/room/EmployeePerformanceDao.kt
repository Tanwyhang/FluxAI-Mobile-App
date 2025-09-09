package com.teamflux.fluxai.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeePerformanceDao {
    @Query("SELECT * FROM employee_performance WHERE id = :employeeId")
    fun getEmployeePerformance(employeeId: String): Flow<EmployeePerformanceEntity?>

    @Query("SELECT * FROM employee_performance")
    fun getAllEmployeePerformances(): Flow<List<EmployeePerformanceEntity>>

    @Query("SELECT id FROM employee_performance")
    suspend fun getAllEmployeeIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployeePerformance(performance: EmployeePerformanceEntity)

    @Query("DELETE FROM employee_performance WHERE id = :employeeId")
    suspend fun deleteEmployeePerformance(employeeId: String)

    // Commit data operations
    @Query("SELECT * FROM commit_data WHERE employeeId = :employeeId")
    suspend fun getCommitData(employeeId: String): CommitDataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommitData(data: CommitDataEntity)

    @Query("DELETE FROM commit_data WHERE employeeId = :employeeId")
    suspend fun deleteCommitData(employeeId: String)

    @Query("DELETE FROM commit_data WHERE timestamp < :timestamp")
    suspend fun deleteOldCommitData(timestamp: Long)
}
