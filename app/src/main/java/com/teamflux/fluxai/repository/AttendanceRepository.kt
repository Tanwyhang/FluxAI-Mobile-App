package com.teamflux.fluxai.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.teamflux.fluxai.viewmodel.AttendanceRecord
import com.teamflux.fluxai.viewmodel.TeamAttendanceCode
import kotlinx.coroutines.tasks.await

class AttendanceRepository {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun getTodayAttendance(userId: String, date: String): Result<AttendanceRecord?> {
        return try {
            val snapshot = firestore.collection("attendance")
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", date)
                .limit(1)
                .get()
                .await()

            val attendance = if (snapshot.isEmpty) {
                null
            } else {
                snapshot.documents.first().toObject(AttendanceRecord::class.java)?.copy(
                    id = snapshot.documents.first().id
                )
            }

            Result.success(attendance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTeamAttendanceCode(teamId: String, date: String): Result<TeamAttendanceCode?> {
        return try {
            val snapshot = firestore.collection("teamAttendanceCodes")
                .whereEqualTo("teamId", teamId)
                .whereEqualTo("date", date)
                .limit(1)
                .get()
                .await()

            val teamCode = if (snapshot.isEmpty) {
                null
            } else {
                snapshot.documents.first().toObject(TeamAttendanceCode::class.java)
            }

            Result.success(teamCode)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTeamAttendanceCode(teamId: String, code: String, date: String): Result<Unit> {
        return try {
            val teamCode = TeamAttendanceCode(
                teamId = teamId,
                code = code,
                date = date,
                generatedAt = System.currentTimeMillis()
            )

            firestore.collection("teamAttendanceCodes")
                .add(teamCode)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTeamAttendanceCode(teamId: String, newCode: String, date: String): Result<Unit> {
        return try {
            // Find existing code document
            val snapshot = firestore.collection("teamAttendanceCodes")
                .whereEqualTo("teamId", teamId)
                .whereEqualTo("date", date)
                .limit(1)
                .get()
                .await()

            if (snapshot.isEmpty) {
                // No existing code, create new one
                return createTeamAttendanceCode(teamId, newCode, date)
            }

            // Update existing code
            val documentId = snapshot.documents.first().id
            firestore.collection("teamAttendanceCodes")
                .document(documentId)
                .update(
                    mapOf(
                        "code" to newCode,
                        "generatedAt" to System.currentTimeMillis()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordAttendance(
        userId: String,
        teamId: String,
        date: String,
        signInTime: String,
        attendanceCode: String
    ): Result<AttendanceRecord> {
        return try {
            // Check if user already has attendance for today
            val existingSnapshot = firestore.collection("attendance")
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", date)
                .limit(1)
                .get()
                .await()

            if (!existingSnapshot.isEmpty) {
                return Result.failure(Exception("You have already signed in for today"))
            }

            val attendance = AttendanceRecord(
                userId = userId,
                teamId = teamId,
                date = date,
                signInTime = signInTime,
                attendanceCode = attendanceCode,
                timestamp = System.currentTimeMillis()
            )

            val documentRef = firestore.collection("attendance")
                .add(attendance)
                .await()

            Result.success(attendance.copy(id = documentRef.id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTeamAttendanceRecords(teamId: String, date: String): Result<List<AttendanceRecord>> {
        return try {
            val snapshot = firestore.collection("attendance")
                .whereEqualTo("teamId", teamId)
                .whereEqualTo("date", date)
                .get()
                .await()

            val records = snapshot.documents.mapNotNull { doc ->
                doc.toObject(AttendanceRecord::class.java)?.copy(id = doc.id)
            }

            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserAttendanceHistory(userId: String, limit: Int = 30): Result<List<AttendanceRecord>> {
        return try {
            val snapshot = firestore.collection("attendance")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val records = snapshot.documents.mapNotNull { doc ->
                doc.toObject(AttendanceRecord::class.java)?.copy(id = doc.id)
            }

            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
