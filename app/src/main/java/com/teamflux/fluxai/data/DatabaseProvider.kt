package com.teamflux.fluxai.data

import android.content.Context
import androidx.room.Room
import com.teamflux.fluxai.data.room.FluxDatabase

object DatabaseProvider {
    private var database: FluxDatabase? = null

    @Synchronized
    fun getDatabase(context: Context): FluxDatabase {
        return database ?: Room.databaseBuilder(
            context.applicationContext,
            FluxDatabase::class.java,
            FluxDatabase.DATABASE_NAME
        )
            .addMigrations(FluxDatabase.MIGRATION_1_2)
            .build().also { database = it }
    }
}
