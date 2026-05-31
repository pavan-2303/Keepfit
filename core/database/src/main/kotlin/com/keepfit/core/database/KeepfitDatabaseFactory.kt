package com.keepfit.core.database

import android.content.Context
import androidx.room.Room

object KeepfitDatabaseFactory {
    const val DATABASE_NAME = "keepfit.db"

    fun create(context: Context): KeepfitDatabase =
        Room.databaseBuilder(
            context,
            KeepfitDatabase::class.java,
            DATABASE_NAME,
        )
            .addMigrations(KeepfitMigrations.ONE_TO_TWO)
            .addMigrations(KeepfitMigrations.TWO_TO_THREE)
            .addMigrations(KeepfitMigrations.THREE_TO_FOUR)
            .build()
}
