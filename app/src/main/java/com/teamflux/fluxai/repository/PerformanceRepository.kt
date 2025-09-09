package com.teamflux.fluxai.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.teamflux.fluxai.model.Performance
import kotlinx.coroutines.tasks.await
import java.util.*

class PerformanceRepository {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun createPerformanceRecord(
        userId: String,
        teamId: String,
        commits: Int,
        performanceScore: Float,
        startDate: Date,
        endDate: Date
    ): Result<String> {
        return try {
            val performanceId = UUID.randomUUID().toString()
            val performance = Performance(
                performanceId = performanceId,
                userId = userId,
                teamId = teamId,
                commits = commits,
                performanceScore = performanceScore,
                startDate = startDate,
                endDate = endDate
            )
            firestore.collection("performance").document(performanceId).set(performance).await()
            Result.success(performanceId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserPerformance(
        userId: String,
        startDate: Date? = null,
        endDate: Date? = null
    ): Result<List<Performance>> {
        return try {
            var query = firestore.collection("performance")
                .whereEqualTo("userId", userId)
                .orderBy("startDate", Query.Direction.DESCENDING)

            startDate?.let { start ->
                query = query.whereGreaterThanOrEqualTo("startDate", start)
            }

            endDate?.let { end ->
                query = query.whereLessThanOrEqualTo("endDate", end)
            }

            val snapshot = query.get().await()
            val performanceList = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Performance::class.java)
            }
            Result.success(performanceList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTeamPerformance(
        teamId: String,
        startDate: Date? = null,
        endDate: Date? = null
    ): Result<List<Performance>> {
        return try {
            var query = firestore.collection("performance")
                .whereEqualTo("teamId", teamId)
                .orderBy("startDate", Query.Direction.DESCENDING)

            startDate?.let { start ->
                query = query.whereGreaterThanOrEqualTo("startDate", start)
            }

            endDate?.let { end ->
                query = query.whereLessThanOrEqualTo("endDate", end)
            }

            val snapshot = query.get().await()
            val performanceList = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Performance::class.java)
            }
            Result.success(performanceList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLatestUserPerformance(userId: String): Result<Performance?> {
        return try {
            val snapshot = firestore.collection("performance")
                .whereEqualTo("userId", userId)
                .orderBy("endDate", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            val performance = snapshot.documents.firstOrNull()?.toObject(Performance::class.java)
            Result.success(performance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserAveragePerformance(userId: String, days: Int = 30): Result<Float> {
        return try {
            val calendar = Calendar.getInstance()
            val endDate = calendar.time
            calendar.add(Calendar.DAY_OF_MONTH, -days)
            val startDate = calendar.time

            val performances = getUserPerformance(userId, startDate, endDate).getOrThrow()
            val averageScore = if (performances.isNotEmpty()) {
                performances.map { it.performanceScore }.average().toFloat()
            } else {
                0.0f
            }
            Result.success(averageScore)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTeamAveragePerformance(teamId: String, days: Int = 30): Result<Float> {
        return try {
            val calendar = Calendar.getInstance()
            val endDate = calendar.time
            calendar.add(Calendar.DAY_OF_MONTH, -days)
            val startDate = calendar.time

            val performances = getTeamPerformance(teamId, startDate, endDate).getOrThrow()
            val averageScore = if (performances.isNotEmpty()) {
                performances.map { it.performanceScore }.average().toFloat()
            } else {
                0.0f
            }
            Result.success(averageScore)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePerformance(performance: Performance): Result<Unit> {
        return try {
            firestore.collection("performance").document(performance.performanceId)
                .set(performance).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePerformance(performanceId: String): Result<Unit> {
        return try {
            firestore.collection("performance").document(performanceId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
