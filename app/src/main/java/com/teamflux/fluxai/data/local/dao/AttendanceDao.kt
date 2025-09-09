package com.teamflux.fluxai.data.local.dao

import androidx.room.*
import com.teamflux.fluxai.data.local.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE userId = :userId ORDER BY date DESC")
    fun getUserAttendance(userId: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE userId = :userId AND date BETWEEN :startDate AND :endDate")
    fun getUserAttendanceBetweenDates(userId: String, startDate: Date, endDate: Date): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Query("SELECT * FROM attendance WHERE userId = :userId AND date = :date")
    fun getAttendanceForDate(userId: String, date: Date): Flow<AttendanceEntity?>
}
