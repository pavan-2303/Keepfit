package com.keepfit.core.database

import android.content.Context
import androidx.room.Room

object KeepfitDatabaseFactory {
    const val DATABASE_NAME = "keepfit.db"

    fun create(
        context: Context,
        databaseName: String = DATABASE_NAME,
    ): KeepfitDatabase =
        Room.databaseBuilder(
            context,
            KeepfitDatabase::class.java,
            databaseName,
        )
            .addMigrations(KeepfitMigrations.ONE_TO_TWO)
            .addMigrations(KeepfitMigrations.TWO_TO_THREE)
            .addMigrations(KeepfitMigrations.THREE_TO_FOUR)
            .addMigrations(KeepfitMigrations.FOUR_TO_FIVE)
            .addMigrations(KeepfitMigrations.FIVE_TO_SIX)
            .addMigrations(KeepfitMigrations.SIX_TO_SEVEN)
            .addMigrations(KeepfitMigrations.SEVEN_TO_EIGHT)
            .addMigrations(KeepfitMigrations.EIGHT_TO_NINE)
            .addMigrations(KeepfitMigrations.NINE_TO_TEN)
            .addMigrations(KeepfitMigrations.TEN_TO_ELEVEN)
            .addMigrations(KeepfitMigrations.ELEVEN_TO_TWELVE)
            .addMigrations(KeepfitMigrations.TWELVE_TO_THIRTEEN)
            .addMigrations(KeepfitMigrations.THIRTEEN_TO_FOURTEEN)
            .addMigrations(KeepfitMigrations.FOURTEEN_TO_FIFTEEN)
            .build()
}
