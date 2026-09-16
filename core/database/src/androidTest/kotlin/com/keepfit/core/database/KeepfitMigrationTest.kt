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

    @Test
    fun migrateTenToElevenAssignsEveryPersonalRootToTheExistingProfile() {
        helper.createDatabase(TEST_DATABASE, 10).use { database ->
            database.execSQL(
                """
                INSERT INTO body_profiles (
                    id, displayName, heightCm, birthDate, dailyCalorieGoal,
                    dailyProteinGoalGrams, dailyCarbohydrateGoalGrams,
                    dailyFatGoalGrams, createdAt, updatedAt
                ) VALUES ('owner', 'Owner', 175.0, NULL, NULL, NULL, NULL, NULL, 1, 1)
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO workout_templates (id, name, notes, createdAt, updatedAt, archivedAt, origin)
                VALUES ('template', 'Full body', NULL, 1, 1, NULL, 'CUSTOM')
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO weekly_plans (id, name, startsOn, isActive, createdAt, updatedAt)
                VALUES ('plan', 'Week', '2026-09-14', 1, 1, 1)
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO workout_occurrences (
                    id, sourcePlannedWorkoutId, sourceTemplateId,
                    templateNameSnapshot, originalDate, scheduledDate,
                    decisionType, createdAt, updatedAt
                ) VALUES ('occurrence', NULL, 'template', 'Full body', '2026-09-14', '2026-09-14', 'FULL', 1, 1)
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO workout_sessions (
                    id, workoutTemplateId, plannedWorkoutId, workoutDate,
                    startedAt, completedAt, notes, workoutOccurrenceId,
                    sessionVariant, energyLevel, difficulty
                ) VALUES ('session', 'template', NULL, '2026-09-14', 1, NULL, NULL, NULL, 'FULL', NULL, NULL)
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO foods (
                    id, name, servingLabel, servingAmount, calories, proteinGrams,
                    carbohydrateGrams, fatGrams, isFavorite, createdAt, updatedAt, archivedAt
                ) VALUES ('food', 'Dal', '1 bowl', 1.0, 220.0, 14.0, 34.0, 4.0, 0, 1, 1, NULL)
                """.trimIndent(),
            )
            database.execSQL(
                "INSERT INTO saved_meals (id, name, createdAt, updatedAt) VALUES ('meal', 'Lunch', 1, 1)",
            )
            database.execSQL(
                """
                INSERT INTO food_diary_entries (id, diaryDate, mealType, foodId, savedMealId, servings, loggedAt)
                VALUES ('entry', '2026-09-14', 'LUNCH', 'food', 'meal', 1.0, 1)
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO meal_quality_check_ins (id, diaryDate, mealType, quality, loggedAt)
                VALUES ('quality', '2026-09-14', 'LUNCH', 'BALANCED', 1)
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO weekly_review_outcomes (
                    id, weekStart, status, draftType, sourcePlannedWorkoutId,
                    sourceDate, targetDate, occurrenceId, decidedAt
                ) VALUES ('review', '2026-09-14', 'DISMISSED', NULL, NULL, NULL, NULL, NULL, 1)
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            11,
            true,
            KeepfitMigrations.TEN_TO_ELEVEN,
        ).use { database ->
            assertEquals("owner", database.stringFor("SELECT bodyProfileId FROM workout_templates WHERE id = 'template'"))
            assertEquals("owner", database.stringFor("SELECT bodyProfileId FROM weekly_plans WHERE id = 'plan'"))
            assertEquals("owner", database.stringFor("SELECT bodyProfileId FROM workout_occurrences WHERE id = 'occurrence'"))
            assertEquals("owner", database.stringFor("SELECT bodyProfileId FROM workout_sessions WHERE id = 'session'"))
            assertEquals("owner", database.stringFor("SELECT bodyProfileId FROM saved_meals WHERE id = 'meal'"))
            assertEquals("owner", database.stringFor("SELECT bodyProfileId FROM food_diary_entries WHERE id = 'entry'"))
            assertEquals("owner", database.stringFor("SELECT bodyProfileId FROM meal_quality_check_ins WHERE id = 'quality'"))
            assertEquals("owner", database.stringFor("SELECT bodyProfileId FROM weekly_review_outcomes WHERE id = 'review'"))
            assertEquals(null, database.stringFor("SELECT archivedAt FROM body_profiles WHERE id = 'owner'"))
        }
    }

    @Test
    fun migrateElevenToTwelveMapsLegacyAnglesToStablePoseKeys() {
        helper.createDatabase(TEST_DATABASE, 11).use { database ->
            database.execSQL(
                """
                INSERT INTO body_profiles (
                    id, displayName, heightCm, birthDate, dailyCalorieGoal,
                    dailyProteinGoalGrams, dailyCarbohydrateGoalGrams,
                    dailyFatGoalGrams, createdAt, updatedAt, archivedAt
                ) VALUES ('owner', 'Owner', 175.0, NULL, NULL, NULL, NULL, NULL, 1, 1, NULL)
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO transformation_cycles (
                    id, bodyProfileId, startDate, notes, closedAt, createdAt, updatedAt
                ) VALUES ('cycle', 'owner', '2026-09-14', 'Keep me', NULL, 2, 3)
                """.trimIndent(),
            )
            listOf("FRONT", "RIGHT", "BACK", "LEFT").forEachIndexed { index, angle ->
                database.execSQL(
                    """
                    INSERT INTO transformation_photos (
                        id, transformationCycleId, captureDate, angle,
                        relativePath, mimeType, sizeBytes, createdAt
                    ) VALUES (
                        'photo-$index', 'cycle', '2026-09-14', '$angle',
                        'media/transformation/cycle/$index.jpg', 'image/jpeg', ${100 + index}, ${10 + index}
                    )
                    """.trimIndent(),
                )
            }
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            12,
            true,
            KeepfitMigrations.ELEVEN_TO_TWELVE,
        ).use { database ->
            database.query(
                """
                SELECT poseKey, relativePath, sizeBytes, createdAt
                FROM transformation_photos
                ORDER BY id
                """.trimIndent(),
            ).use { cursor ->
                val rows = buildList {
                    while (cursor.moveToNext()) {
                        add(listOf(cursor.getString(0), cursor.getString(1), cursor.getLong(2), cursor.getLong(3)))
                    }
                }
                assertEquals(
                    listOf("front_relaxed", "right_side_relaxed", "back_relaxed", "left_side_relaxed"),
                    rows.map { it[0] },
                )
                assertEquals((0..3).map { "media/transformation/cycle/$it.jpg" }, rows.map { it[1] })
                assertEquals(listOf(100L, 101L, 102L, 103L), rows.map { it[2] })
                assertEquals(listOf(10L, 11L, 12L, 13L), rows.map { it[3] })
            }
            database.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'transformation_pose_preferences'",
            ).use { cursor ->
                assertEquals(true, cursor.moveToFirst())
            }
        }
    }

    @Test
    fun migrateTwelveToThirteenAddsCatalogueMetadataWithoutChangingExercises() {
        helper.createDatabase(TEST_DATABASE, 12).use { database ->
            database.execSQL(
                """
                INSERT INTO exercises (
                    id, name, muscleGroup, instructions, notes, isBodyweight,
                    createdAt, updatedAt, archivedAt
                ) VALUES (
                    'personal', 'My row', 'Back', 'Pull with control.', 'Keep this',
                    0, 10, 11, NULL
                )
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            13,
            true,
            KeepfitMigrations.TWELVE_TO_THIRTEEN,
        ).use { database ->
            database.query(
                """
                SELECT name, notes, source, sourceId, equipment, targetMuscle, secondaryMuscles
                FROM exercises WHERE id = 'personal'
                """.trimIndent(),
            ).use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals("My row", cursor.getString(0))
                assertEquals("Keep this", cursor.getString(1))
                assertEquals(null, cursor.getString(2))
                assertEquals(null, cursor.getString(3))
                assertEquals(null, cursor.getString(4))
                assertEquals(null, cursor.getString(5))
                assertEquals(null, cursor.getString(6))
            }
            database.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'catalogue_imports'",
            ).use { cursor ->
                assertEquals(true, cursor.moveToFirst())
            }
        }
    }

    @Test
    fun migrateThirteenToFourteenAddsProfileOwnedCoachHistory() {
        helper.createDatabase(TEST_DATABASE, 13).use { database ->
            database.execSQL(
                """
                INSERT INTO body_profiles (
                    id, displayName, heightCm, birthDate, dailyCalorieGoal,
                    dailyProteinGoalGrams, dailyCarbohydrateGoalGrams,
                    dailyFatGoalGrams, createdAt, updatedAt, archivedAt
                ) VALUES ('owner', 'Owner', 175.0, NULL, NULL, NULL, NULL, NULL, 1, 1, NULL)
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            14,
            true,
            KeepfitMigrations.THIRTEEN_TO_FOURTEEN,
        ).use { database ->
            database.execSQL(
                """
                INSERT INTO assistant_conversations (
                    id, bodyProfileId, coachId, title, memorySummary,
                    memoryClearedAt, createdAt, updatedAt
                ) VALUES ('conversation', 'owner', 'mira', 'First chat', NULL, NULL, 2, 2)
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO assistant_messages (
                    id, assistantConversationId, role, content,
                    includedLocalContext, createdAt
                ) VALUES ('message', 'conversation', 'USER', 'Hello', 0, 3)
                """.trimIndent(),
            )
            assertEquals(
                "owner",
                database.stringFor("SELECT bodyProfileId FROM assistant_conversations WHERE id = 'conversation'"),
            )
            assertEquals(
                "Hello",
                database.stringFor("SELECT content FROM assistant_messages WHERE id = 'message'"),
            )
        }
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.stringFor(query: String): String? =
        query(query).use { cursor ->
            check(cursor.moveToFirst())
            if (cursor.isNull(0)) null else cursor.getString(0)
        }

    private companion object {
        const val TEST_DATABASE = "keepfit-migration-test"
    }
}
