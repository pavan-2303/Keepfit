package com.keepfit.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeepfitMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        databaseClass = KeepfitDatabase::class.java,
        specs = emptyList(),
        openFactory = FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrateTwoToThreePreservesLocalProfileAndAddsNutritionGoals() {
        helper.createDatabase(TEST_DATABASE, 2).use { database ->
            database.execSQL(
                """
                INSERT INTO body_profiles (
                    id, displayName, heightCm, birthDate, createdAt, updatedAt
                ) VALUES (
                    'profile-id', 'Pavan', 178.0, NULL, 100, 100
                )
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            3,
            true,
            KeepfitMigrations.TWO_TO_THREE,
        ).use { database ->
            database.query(
                """
                SELECT displayName, dailyCalorieGoal, dailyProteinGoalGrams,
                    dailyCarbohydrateGoalGrams, dailyFatGoalGrams
                FROM body_profiles
                """.trimIndent(),
            ).use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals("Pavan", cursor.getString(0))
                assertEquals(true, cursor.isNull(1))
                assertEquals(true, cursor.isNull(2))
                assertEquals(true, cursor.isNull(3))
                assertEquals(true, cursor.isNull(4))
            }
            database.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'foods'").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
            }
        }
    }

    private companion object {
        const val TEST_DATABASE = "keepfit-migration-test"
    }
}
