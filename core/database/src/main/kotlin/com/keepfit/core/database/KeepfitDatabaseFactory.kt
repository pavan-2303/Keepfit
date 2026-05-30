package com.keepfit.core.database

import android.content.Context
import androidx.room.Room

object KeepfitDatabaseFactory {
    fun create(context: Context): KeepfitDatabase =
        Room.databaseBuilder(
            context,
            KeepfitDatabase::class.java,
            "keepfit.db",
        )
            .addMigrations(KeepfitMigrations.ONE_TO_TWO)
            .addMigrations(KeepfitMigrations.TWO_TO_THREE)
            .build()
}
