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
    fun migrateOneToTwoPreservesLocalProfile() {
        helper.createDatabase(TEST_DATABASE, 1).use { database ->
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
            2,
            true,
            KeepfitMigrations.ONE_TO_TWO,
        ).use { database ->
            database.query("SELECT displayName FROM body_profiles").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals("Pavan", cursor.getString(0))
            }
        }
    }

    private companion object {
        const val TEST_DATABASE = "keepfit-migration-test"
    }
}

