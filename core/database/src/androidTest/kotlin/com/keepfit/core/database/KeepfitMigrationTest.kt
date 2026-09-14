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

    @Test
    fun migrateThreeToFourPreservesProfileAndAddsTransformationTables() {
        helper.createDatabase(TEST_DATABASE, 3).use { database ->
            database.execSQL(
                """
                INSERT INTO body_profiles (
                    id, displayName, heightCm, birthDate, dailyCalorieGoal,
                    dailyProteinGoalGrams, dailyCarbohydrateGoalGrams, dailyFatGoalGrams,
                    createdAt, updatedAt
                ) VALUES (
                    'profile-id', 'Pavan', 178.0, NULL, 2400.0,
                    150.0, 250.0, 70.0, 100, 100
                )
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            4,
            true,
            KeepfitMigrations.THREE_TO_FOUR,
        ).use { database ->
            database.query(
                """
                SELECT displayName, dailyCalorieGoal
                FROM body_profiles
                """.trimIndent(),
            ).use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals("Pavan", cursor.getString(0))
                assertEquals(2400.0, cursor.getDouble(1), 0.0)
            }
            database.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'transformation_weeks'",
            ).use { cursor ->
                assertEquals(true, cursor.moveToFirst())
            }
        }
    }

    @Test
    fun migrateFourToFiveConvertsWeeksIntoClosedCyclesAndDropsLegPhotos() {
        helper.createDatabase(TEST_DATABASE, 4).use { database ->
            database.execSQL(
                """
                INSERT INTO body_profiles (
                    id, displayName, heightCm, birthDate, dailyCalorieGoal,
                    dailyProteinGoalGrams, dailyCarbohydrateGoalGrams, dailyFatGoalGrams,
                    createdAt, updatedAt
                ) VALUES (
                    'profile-id', 'Pavan', 178.0, NULL, 2400.0,
                    150.0, 250.0, 70.0, 100, 100
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO transformation_weeks (
                    id, bodyProfileId, weekStartDate, notes, createdAt
                ) VALUES (
                    'week-1', 'profile-id', '2026-05-25', 'Week one', 200
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO transformation_photos (
                    id, transformationWeekId, angle, relativePath, mimeType, sizeBytes, createdAt
                ) VALUES (
                    'photo-front', 'week-1', 'FRONT', 'media/transformation/week-1/front.jpg', 'image/jpeg', 123, 201
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO transformation_photos (
                    id, transformationWeekId, angle, relativePath, mimeType, sizeBytes, createdAt
                ) VALUES (
                    'photo-legs', 'week-1', 'LEGS', 'media/transformation/week-1/legs.jpg', 'image/jpeg', 124, 202
                )
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            5,
            true,
            KeepfitMigrations.FOUR_TO_FIVE,
        ).use { database ->
            database.query(
                """
                SELECT startDate, closedAt, notes
                FROM transformation_cycles
                WHERE id = 'week-1'
                """.trimIndent(),
            ).use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals("2026-05-25", cursor.getString(0))
                assertEquals(200L, cursor.getLong(1))
                assertEquals("Week one", cursor.getString(2))
            }
            database.query(
                """
                SELECT COUNT(*)
                FROM transformation_photos
                WHERE transformationCycleId = 'week-1'
                """.trimIndent(),
            ).use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
        }
    }

    @Test
    fun migrateFiveToSixAddsJourneyProfilesAndDefaultsTemplateOrigin() {
        helper.createDatabase(TEST_DATABASE, 5).use { database ->
            database.execSQL(
                """
                INSERT INTO body_profiles (
                    id, displayName, heightCm, birthDate, dailyCalorieGoal,
                    dailyProteinGoalGrams, dailyCarbohydrateGoalGrams, dailyFatGoalGrams,
                    createdAt, updatedAt
                ) VALUES (
                    'profile-id', 'Pavan', 178.0, NULL, NULL,
                    NULL, NULL, NULL, 100, 100
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO workout_templates (
                    id, name, notes, createdAt, updatedAt, archivedAt
                ) VALUES (
                    'template-id', 'Personal template', NULL, 100, 100, NULL
                )
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            6,
            true,
            KeepfitMigrations.FIVE_TO_SIX,
        ).use { database ->
            database.query(
                "SELECT origin FROM workout_templates WHERE id = 'template-id'",
            ).use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals("CUSTOM", cursor.getString(0))
            }
            database.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'journey_profiles'",
            ).use { cursor ->
                assertEquals(true, cursor.moveToFirst())
            }
        }
    }

    @Test
    fun migrateSixToSevenAddsOccurrencesAndPreservesLegacyWorkoutSession() {
        helper.createDatabase(TEST_DATABASE, 6).use { database ->
            database.execSQL(
                """
                INSERT INTO exercises (
                    id, name, muscleGroup, instructions, notes, isBodyweight,
                    createdAt, updatedAt, archivedAt
                ) VALUES (
                    'exercise', 'Chair squat', 'Legs', NULL, NULL, 1,
                    1, 1, NULL
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO workout_templates (
                    id, name, notes, createdAt, updatedAt, archivedAt, origin
                ) VALUES (
                    'template', 'Foundation A', NULL, 2, 2, NULL, 'STARTER_PLAN'
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO weekly_plans (
                    id, name, startsOn, isActive, createdAt, updatedAt
                ) VALUES (
                    'plan', 'Starter week', '2026-09-07', 1, 3, 3
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO planned_workouts (
                    id, weeklyPlanId, workoutTemplateId, dayOfWeek, position
                ) VALUES (
                    'planned', 'plan', 'template', 'SATURDAY', 0
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO workout_sessions (
                    id, workoutTemplateId, plannedWorkoutId, workoutDate,
                    startedAt, completedAt, notes
                ) VALUES (
                    'session', 'template', 'planned', '2026-09-12',
                    4, 5, 'Completed before migration'
                )
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            7,
            true,
            KeepfitMigrations.SIX_TO_SEVEN,
        ).use { database ->
            database.query(
                "SELECT plannedWorkoutId, workoutOccurrenceId, notes FROM workout_sessions WHERE id = 'session'",
            ).use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals("planned", cursor.getString(0))
                assertEquals(true, cursor.isNull(1))
                assertEquals("Completed before migration", cursor.getString(2))
            }
            database.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'workout_occurrences'",
            ).use { cursor ->
                assertEquals(true, cursor.moveToFirst())
            }
            database.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'workout_occurrence_exercises'",
            ).use { cursor ->
                assertEquals(true, cursor.moveToFirst())
            }
        }
    }

    @Test
    fun migrateSevenToEightAddsDurableExecutionContext() {
        helper.createDatabase(TEST_DATABASE, 7).use { database ->
            database.execSQL(
                """
                INSERT INTO exercises (
                    id, name, muscleGroup, instructions, notes, isBodyweight,
                    createdAt, updatedAt, archivedAt
                ) VALUES (
                    'exercise', 'Chair squat', 'Legs', NULL, NULL, 1,
                    1, 1, NULL
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO workout_sessions (
                    id, workoutTemplateId, plannedWorkoutId, workoutDate,
                    startedAt, completedAt, notes, workoutOccurrenceId
                ) VALUES (
                    'session', NULL, NULL, '2026-09-12',
                    2, NULL, 'Still active', NULL
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO exercise_logs (
                    id, workoutSessionId, exerciseId, position, notes
                ) VALUES (
                    'log', 'session', 'exercise', 0, NULL
                )
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            8,
            true,
            KeepfitMigrations.SEVEN_TO_EIGHT,
        ).use { database ->
            database.query(
                """
                SELECT sessionVariant, energyLevel, difficulty
                FROM workout_sessions
                WHERE id = 'session'
                """.trimIndent(),
            ).use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals("FULL", cursor.getString(0))
                assertEquals(true, cursor.isNull(1))
                assertEquals(true, cursor.isNull(2))
            }
            database.query(
                """
                SELECT targetSets, targetReps
                FROM exercise_logs
                WHERE id = 'log'
                """.trimIndent(),
            ).use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals(true, cursor.isNull(0))
                assertEquals(true, cursor.isNull(1))
            }
        }
    }

    @Test
    fun migrateEightToNineAddsWeeklyReviewOutcomesWithoutChangingSessions() {
        helper.createDatabase(TEST_DATABASE, 8).use { database ->
            database.execSQL(
                """
                INSERT INTO workout_sessions (
                    id, workoutTemplateId, plannedWorkoutId, workoutDate,
                    startedAt, completedAt, notes, workoutOccurrenceId,
                    sessionVariant, energyLevel, difficulty
                ) VALUES (
                    'session', NULL, NULL, '2026-09-06',
                    2, 3, 'Existing history', NULL, 'MINIMUM', 3, 4
                )
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            9,
            true,
            KeepfitMigrations.EIGHT_TO_NINE,
        ).use { database ->
            database.query(
                "SELECT notes, sessionVariant FROM workout_sessions WHERE id = 'session'",
            ).use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals("Existing history", cursor.getString(0))
                assertEquals("MINIMUM", cursor.getString(1))
            }
            database.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'weekly_review_outcomes'",
            ).use { cursor ->
                assertEquals(true, cursor.moveToFirst())
            }
        }
    }

    @Test
    fun migrateNineToTenAddsMealQualityCheckInsWithoutChangingDiary() {
        helper.createDatabase(TEST_DATABASE, 9).use { database ->
            database.execSQL(
                """
                INSERT INTO foods (
                    id, name, servingLabel, servingAmount, calories, proteinGrams,
                    carbohydrateGrams, fatGrams, isFavorite, createdAt, updatedAt, archivedAt
                ) VALUES ('food', 'Dal', '1 bowl', 1.0, 220.0, 14.0, 34.0, 4.0, 0, 1, 1, NULL)
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO food_diary_entries (
                    id, diaryDate, mealType, foodId, savedMealId, servings, loggedAt
                ) VALUES ('entry', '2026-09-12', 'LUNCH', 'food', NULL, 1.5, 2)
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            10,
            true,
            KeepfitMigrations.NINE_TO_TEN,
        ).use { database ->
            database.query("SELECT servings FROM food_diary_entries WHERE id = 'entry'").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals(1.5, cursor.getDouble(0), 0.0)
            }
            database.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'meal_quality_check_ins'",
            ).use { cursor ->
                assertEquals(true, cursor.moveToFirst())
            }
        }
    }

    private companion object {
        const val TEST_DATABASE = "keepfit-migration-test"
    }
}
