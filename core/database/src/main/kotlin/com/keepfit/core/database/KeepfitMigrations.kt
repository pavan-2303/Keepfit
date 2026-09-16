package com.keepfit.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object KeepfitMigrations {
    val THIRTEEN_TO_FOURTEEN = object : Migration(13, 14) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `assistant_conversations` (
                    `id` TEXT NOT NULL,
                    `bodyProfileId` TEXT NOT NULL,
                    `coachId` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `memorySummary` TEXT,
                    `memoryClearedAt` INTEGER,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`bodyProfileId`) REFERENCES `body_profiles`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_assistant_conversations_bodyProfileId` ON `assistant_conversations` (`bodyProfileId`)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_assistant_conversations_bodyProfileId_updatedAt` ON `assistant_conversations` (`bodyProfileId`, `updatedAt`)",
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `assistant_messages` (
                    `id` TEXT NOT NULL,
                    `assistantConversationId` TEXT NOT NULL,
                    `role` TEXT NOT NULL,
                    `content` TEXT NOT NULL,
                    `includedLocalContext` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`assistantConversationId`) REFERENCES `assistant_conversations`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_assistant_messages_assistantConversationId` ON `assistant_messages` (`assistantConversationId`)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_assistant_messages_assistantConversationId_createdAt` ON `assistant_messages` (`assistantConversationId`, `createdAt`)",
            )
        }
    }

    val TWELVE_TO_THIRTEEN = object : Migration(12, 13) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE `exercises` ADD COLUMN `source` TEXT")
            database.execSQL("ALTER TABLE `exercises` ADD COLUMN `sourceId` TEXT")
            database.execSQL("ALTER TABLE `exercises` ADD COLUMN `equipment` TEXT")
            database.execSQL("ALTER TABLE `exercises` ADD COLUMN `targetMuscle` TEXT")
            database.execSQL("ALTER TABLE `exercises` ADD COLUMN `secondaryMuscles` TEXT")
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `catalogue_imports` (
                    `source` TEXT NOT NULL,
                    `revision` TEXT NOT NULL,
                    `recordCount` INTEGER NOT NULL,
                    `importedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`source`)
                )
                """.trimIndent(),
            )
        }
    }

    val ONE_TO_TWO = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `exercises` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `muscleGroup` TEXT NOT NULL,
                    `instructions` TEXT,
                    `notes` TEXT,
                    `isBodyweight` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `archivedAt` INTEGER,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `exercise_media` (
                    `id` TEXT NOT NULL,
                    `exerciseId` TEXT NOT NULL,
                    `mediaType` TEXT NOT NULL,
                    `relativePath` TEXT NOT NULL,
                    `mimeType` TEXT NOT NULL,
                    `sizeBytes` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_exercise_media_exerciseId` ON `exercise_media` (`exerciseId`)")
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `workout_templates` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `notes` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `archivedAt` INTEGER,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `workout_template_exercises` (
                    `id` TEXT NOT NULL,
                    `workoutTemplateId` TEXT NOT NULL,
                    `exerciseId` TEXT NOT NULL,
                    `position` INTEGER NOT NULL,
                    `targetSets` INTEGER NOT NULL,
                    `targetReps` TEXT,
                    `notes` TEXT,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`workoutTemplateId`) REFERENCES `workout_templates`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`)
                        ON UPDATE NO ACTION ON DELETE NO ACTION
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_template_exercises_workoutTemplateId` ON `workout_template_exercises` (`workoutTemplateId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_template_exercises_exerciseId` ON `workout_template_exercises` (`exerciseId`)")
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `weekly_plans` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `startsOn` TEXT NOT NULL,
                    `isActive` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `planned_workouts` (
                    `id` TEXT NOT NULL,
                    `weeklyPlanId` TEXT NOT NULL,
                    `workoutTemplateId` TEXT NOT NULL,
                    `dayOfWeek` TEXT NOT NULL,
                    `position` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`weeklyPlanId`) REFERENCES `weekly_plans`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`workoutTemplateId`) REFERENCES `workout_templates`(`id`)
                        ON UPDATE NO ACTION ON DELETE NO ACTION
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_planned_workouts_weeklyPlanId` ON `planned_workouts` (`weeklyPlanId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_planned_workouts_workoutTemplateId` ON `planned_workouts` (`workoutTemplateId`)")
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `workout_sessions` (
                    `id` TEXT NOT NULL,
                    `workoutTemplateId` TEXT,
                    `plannedWorkoutId` TEXT,
                    `workoutDate` TEXT NOT NULL,
                    `startedAt` INTEGER NOT NULL,
                    `completedAt` INTEGER,
                    `notes` TEXT,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`workoutTemplateId`) REFERENCES `workout_templates`(`id`)
                        ON UPDATE NO ACTION ON DELETE NO ACTION,
                    FOREIGN KEY(`plannedWorkoutId`) REFERENCES `planned_workouts`(`id`)
                        ON UPDATE NO ACTION ON DELETE NO ACTION
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sessions_workoutTemplateId` ON `workout_sessions` (`workoutTemplateId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sessions_plannedWorkoutId` ON `workout_sessions` (`plannedWorkoutId`)")
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `exercise_logs` (
                    `id` TEXT NOT NULL,
                    `workoutSessionId` TEXT NOT NULL,
                    `exerciseId` TEXT NOT NULL,
                    `position` INTEGER NOT NULL,
                    `notes` TEXT,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`workoutSessionId`) REFERENCES `workout_sessions`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`)
                        ON UPDATE NO ACTION ON DELETE NO ACTION
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_logs_workoutSessionId` ON `exercise_logs` (`workoutSessionId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_logs_exerciseId` ON `exercise_logs` (`exerciseId`)")
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `set_logs` (
                    `id` TEXT NOT NULL,
                    `exerciseLogId` TEXT NOT NULL,
                    `position` INTEGER NOT NULL,
                    `repetitions` INTEGER NOT NULL,
                    `weightKg` REAL NOT NULL,
                    `isCompleted` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`exerciseLogId`) REFERENCES `exercise_logs`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_set_logs_exerciseLogId` ON `set_logs` (`exerciseLogId`)")
        }
    }

    val TWO_TO_THREE = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE body_profiles ADD COLUMN `dailyCalorieGoal` REAL")
            database.execSQL("ALTER TABLE body_profiles ADD COLUMN `dailyProteinGoalGrams` REAL")
            database.execSQL("ALTER TABLE body_profiles ADD COLUMN `dailyCarbohydrateGoalGrams` REAL")
            database.execSQL("ALTER TABLE body_profiles ADD COLUMN `dailyFatGoalGrams` REAL")
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `foods` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `servingLabel` TEXT NOT NULL,
                    `servingAmount` REAL NOT NULL,
                    `calories` REAL NOT NULL,
                    `proteinGrams` REAL NOT NULL,
                    `carbohydrateGrams` REAL NOT NULL,
                    `fatGrams` REAL NOT NULL,
                    `isFavorite` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `archivedAt` INTEGER,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `saved_meals` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `saved_meal_items` (
                    `id` TEXT NOT NULL,
                    `savedMealId` TEXT NOT NULL,
                    `foodId` TEXT NOT NULL,
                    `servings` REAL NOT NULL,
                    `position` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`savedMealId`) REFERENCES `saved_meals`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`foodId`) REFERENCES `foods`(`id`)
                        ON UPDATE NO ACTION ON DELETE NO ACTION
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_saved_meal_items_savedMealId` ON `saved_meal_items` (`savedMealId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_saved_meal_items_foodId` ON `saved_meal_items` (`foodId`)")
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `food_diary_entries` (
                    `id` TEXT NOT NULL,
                    `diaryDate` TEXT NOT NULL,
                    `mealType` TEXT NOT NULL,
                    `foodId` TEXT NOT NULL,
                    `savedMealId` TEXT,
                    `servings` REAL NOT NULL,
                    `loggedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`foodId`) REFERENCES `foods`(`id`)
                        ON UPDATE NO ACTION ON DELETE NO ACTION,
                    FOREIGN KEY(`savedMealId`) REFERENCES `saved_meals`(`id`)
                        ON UPDATE NO ACTION ON DELETE NO ACTION
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_food_diary_entries_diaryDate` ON `food_diary_entries` (`diaryDate`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_food_diary_entries_foodId` ON `food_diary_entries` (`foodId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_food_diary_entries_savedMealId` ON `food_diary_entries` (`savedMealId`)")
        }
    }

    val THREE_TO_FOUR = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `body_measurements` (
                    `id` TEXT NOT NULL,
                    `bodyProfileId` TEXT NOT NULL,
                    `measurementDate` TEXT NOT NULL,
                    `weightKg` REAL,
                    `waistCm` REAL,
                    `chestCm` REAL,
                    `hipsCm` REAL,
                    `leftArmCm` REAL,
                    `rightArmCm` REAL,
                    `leftThighCm` REAL,
                    `rightThighCm` REAL,
                    `notes` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`bodyProfileId`) REFERENCES `body_profiles`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_body_measurements_bodyProfileId` ON `body_measurements` (`bodyProfileId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_body_measurements_measurementDate` ON `body_measurements` (`measurementDate`)")
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `transformation_weeks` (
                    `id` TEXT NOT NULL,
                    `bodyProfileId` TEXT NOT NULL,
                    `weekStartDate` TEXT NOT NULL,
                    `notes` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`bodyProfileId`) REFERENCES `body_profiles`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_transformation_weeks_bodyProfileId` ON `transformation_weeks` (`bodyProfileId`)")
            database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_transformation_weeks_bodyProfileId_weekStartDate` ON `transformation_weeks` (`bodyProfileId`, `weekStartDate`)",
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `transformation_photos` (
                    `id` TEXT NOT NULL,
                    `transformationWeekId` TEXT NOT NULL,
                    `angle` TEXT NOT NULL,
                    `relativePath` TEXT NOT NULL,
                    `mimeType` TEXT NOT NULL,
                    `sizeBytes` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`transformationWeekId`) REFERENCES `transformation_weeks`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_transformation_photos_transformationWeekId` ON `transformation_photos` (`transformationWeekId`)")
            database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_transformation_photos_transformationWeekId_angle` ON `transformation_photos` (`transformationWeekId`, `angle`)",
            )
        }
    }

    val FOUR_TO_FIVE = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `transformation_cycles` (
                    `id` TEXT NOT NULL,
                    `bodyProfileId` TEXT NOT NULL,
                    `startDate` TEXT NOT NULL,
                    `notes` TEXT,
                    `closedAt` INTEGER,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`bodyProfileId`) REFERENCES `body_profiles`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_transformation_cycles_bodyProfileId` ON `transformation_cycles` (`bodyProfileId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_transformation_cycles_startDate` ON `transformation_cycles` (`startDate`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_transformation_cycles_closedAt` ON `transformation_cycles` (`closedAt`)")
            database.execSQL(
                """
                INSERT INTO `transformation_cycles` (
                    `id`, `bodyProfileId`, `startDate`, `notes`, `closedAt`, `createdAt`, `updatedAt`
                )
                SELECT
                    `id`,
                    `bodyProfileId`,
                    `weekStartDate`,
                    `notes`,
                    `createdAt`,
                    `createdAt`,
                    `createdAt`
                FROM `transformation_weeks`
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `transformation_photos_new` (
                    `id` TEXT NOT NULL,
                    `transformationCycleId` TEXT NOT NULL,
                    `captureDate` TEXT NOT NULL,
                    `angle` TEXT NOT NULL,
                    `relativePath` TEXT NOT NULL,
                    `mimeType` TEXT NOT NULL,
                    `sizeBytes` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`transformationCycleId`) REFERENCES `transformation_cycles`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_transformation_photos_new_transformationCycleId` ON `transformation_photos_new` (`transformationCycleId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_transformation_photos_new_captureDate` ON `transformation_photos_new` (`captureDate`)")
            database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_transformation_photos_new_transformationCycleId_captureDate_angle` ON `transformation_photos_new` (`transformationCycleId`, `captureDate`, `angle`)",
            )
            database.execSQL(
                """
                INSERT INTO `transformation_photos_new` (
                    `id`, `transformationCycleId`, `captureDate`, `angle`, `relativePath`, `mimeType`, `sizeBytes`, `createdAt`
                )
                SELECT
                    photo.`id`,
                    photo.`transformationWeekId`,
                    week.`weekStartDate`,
                    photo.`angle`,
                    photo.`relativePath`,
                    photo.`mimeType`,
                    photo.`sizeBytes`,
                    photo.`createdAt`
                FROM `transformation_photos` AS photo
                INNER JOIN `transformation_weeks` AS week
                    ON week.`id` = photo.`transformationWeekId`
                WHERE photo.`angle` != 'LEGS'
                """.trimIndent(),
            )
            database.execSQL("DROP TABLE `transformation_photos`")
            database.execSQL("ALTER TABLE `transformation_photos_new` RENAME TO `transformation_photos`")
            database.execSQL("DROP TABLE `transformation_weeks`")
        }
    }

    val FIVE_TO_SIX = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE `workout_templates` ADD COLUMN `origin` TEXT NOT NULL DEFAULT 'CUSTOM'",
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `journey_profiles` (
                    `id` TEXT NOT NULL,
                    `bodyProfileId` TEXT NOT NULL,
                    `primaryGoal` TEXT NOT NULL,
                    `experienceLevel` TEXT NOT NULL,
                    `preferredDays` TEXT NOT NULL,
                    `sessionMinutes` INTEGER NOT NULL,
                    `equipment` TEXT NOT NULL,
                    `avoidedExerciseKeys` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`bodyProfileId`) REFERENCES `body_profiles`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_journey_profiles_bodyProfileId` ON `journey_profiles` (`bodyProfileId`)",
            )
        }
    }

    val SIX_TO_SEVEN = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `workout_occurrences` (
                    `id` TEXT NOT NULL,
                    `sourcePlannedWorkoutId` TEXT,
                    `sourceTemplateId` TEXT,
                    `templateNameSnapshot` TEXT NOT NULL,
                    `originalDate` TEXT NOT NULL,
                    `scheduledDate` TEXT NOT NULL,
                    `decisionType` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`sourcePlannedWorkoutId`) REFERENCES `planned_workouts`(`id`)
                        ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent(),
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_workout_occurrences_sourcePlannedWorkoutId` ON `workout_occurrences` (`sourcePlannedWorkoutId`)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_workout_occurrences_originalDate` ON `workout_occurrences` (`originalDate`)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_workout_occurrences_scheduledDate` ON `workout_occurrences` (`scheduledDate`)",
            )
            database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_workout_occurrences_sourcePlannedWorkoutId_originalDate` ON `workout_occurrences` (`sourcePlannedWorkoutId`, `originalDate`)",
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `workout_occurrence_exercises` (
                    `id` TEXT NOT NULL,
                    `workoutOccurrenceId` TEXT NOT NULL,
                    `sourceTemplateExerciseId` TEXT,
                    `exerciseId` TEXT NOT NULL,
                    `exerciseNameSnapshot` TEXT NOT NULL,
                    `position` INTEGER NOT NULL,
                    `targetSets` INTEGER NOT NULL,
                    `targetReps` TEXT,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`workoutOccurrenceId`) REFERENCES `workout_occurrences`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`)
                        ON UPDATE NO ACTION ON DELETE NO ACTION
                )
                """.trimIndent(),
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_workout_occurrence_exercises_workoutOccurrenceId` ON `workout_occurrence_exercises` (`workoutOccurrenceId`)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_workout_occurrence_exercises_exerciseId` ON `workout_occurrence_exercises` (`exerciseId`)",
            )
            database.execSQL(
                "ALTER TABLE `workout_sessions` ADD COLUMN `workoutOccurrenceId` TEXT",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_workout_sessions_workoutOccurrenceId` ON `workout_sessions` (`workoutOccurrenceId`)",
            )
        }
    }

    val SEVEN_TO_EIGHT = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE `workout_sessions` ADD COLUMN `sessionVariant` TEXT NOT NULL DEFAULT 'FULL'",
            )
            database.execSQL(
                "ALTER TABLE `workout_sessions` ADD COLUMN `energyLevel` INTEGER",
            )
            database.execSQL(
                "ALTER TABLE `workout_sessions` ADD COLUMN `difficulty` INTEGER",
            )
            database.execSQL(
                "ALTER TABLE `exercise_logs` ADD COLUMN `targetSets` INTEGER",
            )
            database.execSQL(
                "ALTER TABLE `exercise_logs` ADD COLUMN `targetReps` TEXT",
            )
        }
    }

    val EIGHT_TO_NINE = object : Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `weekly_review_outcomes` (
                    `id` TEXT NOT NULL,
                    `weekStart` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `draftType` TEXT,
                    `sourcePlannedWorkoutId` TEXT,
                    `sourceDate` TEXT,
                    `targetDate` TEXT,
                    `occurrenceId` TEXT,
                    `decidedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_weekly_review_outcomes_weekStart` ON `weekly_review_outcomes` (`weekStart`)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_weekly_review_outcomes_occurrenceId` ON `weekly_review_outcomes` (`occurrenceId`)",
            )
        }
    }

    val NINE_TO_TEN = object : Migration(9, 10) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `meal_quality_check_ins` (
                    `id` TEXT NOT NULL,
                    `diaryDate` TEXT NOT NULL,
                    `mealType` TEXT NOT NULL,
                    `quality` TEXT NOT NULL,
                    `loggedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_meal_quality_check_ins_diaryDate_mealType` ON `meal_quality_check_ins` (`diaryDate`, `mealType`)",
            )
        }
    }

    val TEN_TO_ELEVEN = object : Migration(10, 11) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE `body_profiles` ADD COLUMN `archivedAt` INTEGER")
            database.execSQL("PRAGMA defer_foreign_keys = ON")
            migrateWorkoutOwnership(database)
            migrateNutritionOwnership(database)
            migrateReviewOwnership(database)
        }
    }

    val ELEVEN_TO_TWELVE = object : Migration(11, 12) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE `transformation_photos_v12` (
                    `id` TEXT NOT NULL,
                    `transformationCycleId` TEXT NOT NULL,
                    `captureDate` TEXT NOT NULL,
                    `poseKey` TEXT NOT NULL,
                    `relativePath` TEXT NOT NULL,
                    `mimeType` TEXT NOT NULL,
                    `sizeBytes` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`transformationCycleId`) REFERENCES `transformation_cycles`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO `transformation_photos_v12` (
                    id, transformationCycleId, captureDate, poseKey,
                    relativePath, mimeType, sizeBytes, createdAt
                )
                SELECT id, transformationCycleId, captureDate,
                    CASE angle
                        WHEN 'FRONT' THEN 'front_relaxed'
                        WHEN 'RIGHT' THEN 'right_side_relaxed'
                        WHEN 'BACK' THEN 'back_relaxed'
                        WHEN 'LEFT' THEN 'left_side_relaxed'
                        ELSE lower(angle)
                    END,
                    relativePath, mimeType, sizeBytes, createdAt
                FROM `transformation_photos`
                """.trimIndent(),
            )
            database.execSQL("DROP TABLE `transformation_photos`")
            database.execSQL("ALTER TABLE `transformation_photos_v12` RENAME TO `transformation_photos`")
            database.execSQL(
                "CREATE INDEX `index_transformation_photos_transformationCycleId` ON `transformation_photos` (`transformationCycleId`)",
            )
            database.execSQL(
                "CREATE INDEX `index_transformation_photos_captureDate` ON `transformation_photos` (`captureDate`)",
            )
            database.execSQL(
                "CREATE UNIQUE INDEX `index_transformation_photos_transformationCycleId_captureDate_poseKey` ON `transformation_photos` (`transformationCycleId`, `captureDate`, `poseKey`)",
            )
            database.execSQL(
                """
                CREATE TABLE `transformation_pose_preferences` (
                    `bodyProfileId` TEXT NOT NULL,
                    `poseKey` TEXT NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`bodyProfileId`, `poseKey`),
                    FOREIGN KEY(`bodyProfileId`) REFERENCES `body_profiles`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
        }
    }

    private fun migrateWorkoutOwnership(database: SupportSQLiteDatabase) {
        val tables = listOf(
            "workout_templates",
            "workout_template_exercises",
            "weekly_plans",
            "planned_workouts",
            "workout_occurrences",
            "workout_occurrence_exercises",
            "workout_sessions",
            "exercise_logs",
            "set_logs",
        )
        tables.forEach { table ->
            database.execSQL("CREATE TEMP TABLE `${table}_v10` AS SELECT * FROM `$table`")
        }
        listOf(
            "set_logs",
            "exercise_logs",
            "workout_sessions",
            "workout_occurrence_exercises",
            "workout_occurrences",
            "planned_workouts",
            "workout_template_exercises",
            "weekly_plans",
            "workout_templates",
        ).forEach { database.execSQL("DROP TABLE `$it`") }

        database.execSQL(
            """
            CREATE TABLE `workout_templates` (
                `id` TEXT NOT NULL, `name` TEXT NOT NULL, `notes` TEXT,
                `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL,
                `archivedAt` INTEGER, `origin` TEXT NOT NULL,
                `bodyProfileId` TEXT NOT NULL, PRIMARY KEY(`id`),
                FOREIGN KEY(`bodyProfileId`) REFERENCES `body_profiles`(`id`)
                    ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX `index_workout_templates_bodyProfileId` ON `workout_templates` (`bodyProfileId`)")
        database.execSQL(
            """
            INSERT INTO `workout_templates`
            SELECT *, (SELECT id FROM body_profiles ORDER BY createdAt, id LIMIT 1)
            FROM `workout_templates_v10`
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE TABLE `workout_template_exercises` (
                `id` TEXT NOT NULL, `workoutTemplateId` TEXT NOT NULL,
                `exerciseId` TEXT NOT NULL, `position` INTEGER NOT NULL,
                `targetSets` INTEGER NOT NULL, `targetReps` TEXT, `notes` TEXT,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`workoutTemplateId`) REFERENCES `workout_templates`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`)
                    ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX `index_workout_template_exercises_workoutTemplateId` ON `workout_template_exercises` (`workoutTemplateId`)")
        database.execSQL("CREATE INDEX `index_workout_template_exercises_exerciseId` ON `workout_template_exercises` (`exerciseId`)")
        database.execSQL("INSERT INTO `workout_template_exercises` SELECT * FROM `workout_template_exercises_v10`")

        database.execSQL(
            """
            CREATE TABLE `weekly_plans` (
                `id` TEXT NOT NULL, `name` TEXT NOT NULL, `startsOn` TEXT NOT NULL,
                `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL, `bodyProfileId` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`bodyProfileId`) REFERENCES `body_profiles`(`id`)
                    ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX `index_weekly_plans_bodyProfileId` ON `weekly_plans` (`bodyProfileId`)")
        database.execSQL(
            """
            INSERT INTO `weekly_plans`
            SELECT *, (SELECT id FROM body_profiles ORDER BY createdAt, id LIMIT 1)
            FROM `weekly_plans_v10`
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE TABLE `planned_workouts` (
                `id` TEXT NOT NULL, `weeklyPlanId` TEXT NOT NULL,
                `workoutTemplateId` TEXT NOT NULL, `dayOfWeek` TEXT NOT NULL,
                `position` INTEGER NOT NULL, PRIMARY KEY(`id`),
                FOREIGN KEY(`weeklyPlanId`) REFERENCES `weekly_plans`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`workoutTemplateId`) REFERENCES `workout_templates`(`id`)
                    ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX `index_planned_workouts_weeklyPlanId` ON `planned_workouts` (`weeklyPlanId`)")
        database.execSQL("CREATE INDEX `index_planned_workouts_workoutTemplateId` ON `planned_workouts` (`workoutTemplateId`)")
        database.execSQL("INSERT INTO `planned_workouts` SELECT * FROM `planned_workouts_v10`")

        database.execSQL(
            """
            CREATE TABLE `workout_occurrences` (
                `id` TEXT NOT NULL, `sourcePlannedWorkoutId` TEXT,
                `sourceTemplateId` TEXT, `templateNameSnapshot` TEXT NOT NULL,
                `originalDate` TEXT NOT NULL, `scheduledDate` TEXT NOT NULL,
                `decisionType` TEXT NOT NULL, `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL, `bodyProfileId` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`sourcePlannedWorkoutId`) REFERENCES `planned_workouts`(`id`)
                    ON UPDATE NO ACTION ON DELETE SET NULL,
                FOREIGN KEY(`bodyProfileId`) REFERENCES `body_profiles`(`id`)
                    ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX `index_workout_occurrences_sourcePlannedWorkoutId` ON `workout_occurrences` (`sourcePlannedWorkoutId`)")
        database.execSQL("CREATE INDEX `index_workout_occurrences_originalDate` ON `workout_occurrences` (`originalDate`)")
        database.execSQL("CREATE INDEX `index_workout_occurrences_scheduledDate` ON `workout_occurrences` (`scheduledDate`)")
        database.execSQL("CREATE INDEX `index_workout_occurrences_bodyProfileId` ON `workout_occurrences` (`bodyProfileId`)")
        database.execSQL("CREATE UNIQUE INDEX `index_workout_occurrences_bodyProfileId_sourcePlannedWorkoutId_originalDate` ON `workout_occurrences` (`bodyProfileId`, `sourcePlannedWorkoutId`, `originalDate`)")
        database.execSQL(
            """
            INSERT INTO `workout_occurrences`
            SELECT *, (SELECT id FROM body_profiles ORDER BY createdAt, id LIMIT 1)
            FROM `workout_occurrences_v10`
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE TABLE `workout_occurrence_exercises` (
                `id` TEXT NOT NULL, `workoutOccurrenceId` TEXT NOT NULL,
                `sourceTemplateExerciseId` TEXT, `exerciseId` TEXT NOT NULL,
                `exerciseNameSnapshot` TEXT NOT NULL, `position` INTEGER NOT NULL,
                `targetSets` INTEGER NOT NULL, `targetReps` TEXT, PRIMARY KEY(`id`),
                FOREIGN KEY(`workoutOccurrenceId`) REFERENCES `workout_occurrences`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`)
                    ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX `index_workout_occurrence_exercises_workoutOccurrenceId` ON `workout_occurrence_exercises` (`workoutOccurrenceId`)")
        database.execSQL("CREATE INDEX `index_workout_occurrence_exercises_exerciseId` ON `workout_occurrence_exercises` (`exerciseId`)")
        database.execSQL("INSERT INTO `workout_occurrence_exercises` SELECT * FROM `workout_occurrence_exercises_v10`")

        database.execSQL(
            """
            CREATE TABLE `workout_sessions` (
                `id` TEXT NOT NULL, `workoutTemplateId` TEXT, `plannedWorkoutId` TEXT,
                `workoutDate` TEXT NOT NULL, `startedAt` INTEGER NOT NULL,
                `completedAt` INTEGER, `notes` TEXT, `workoutOccurrenceId` TEXT,
                `sessionVariant` TEXT NOT NULL, `energyLevel` INTEGER,
                `difficulty` INTEGER, `bodyProfileId` TEXT NOT NULL, PRIMARY KEY(`id`),
                FOREIGN KEY(`workoutTemplateId`) REFERENCES `workout_templates`(`id`)
                    ON UPDATE NO ACTION ON DELETE NO ACTION,
                FOREIGN KEY(`plannedWorkoutId`) REFERENCES `planned_workouts`(`id`)
                    ON UPDATE NO ACTION ON DELETE NO ACTION,
                FOREIGN KEY(`bodyProfileId`) REFERENCES `body_profiles`(`id`)
                    ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX `index_workout_sessions_workoutTemplateId` ON `workout_sessions` (`workoutTemplateId`)")
        database.execSQL("CREATE INDEX `index_workout_sessions_plannedWorkoutId` ON `workout_sessions` (`plannedWorkoutId`)")
        database.execSQL("CREATE INDEX `index_workout_sessions_workoutOccurrenceId` ON `workout_sessions` (`workoutOccurrenceId`)")
        database.execSQL("CREATE INDEX `index_workout_sessions_bodyProfileId` ON `workout_sessions` (`bodyProfileId`)")
        database.execSQL(
            """
            INSERT INTO `workout_sessions`
            SELECT *, (SELECT id FROM body_profiles ORDER BY createdAt, id LIMIT 1)
            FROM `workout_sessions_v10`
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE TABLE `exercise_logs` (
                `id` TEXT NOT NULL, `workoutSessionId` TEXT NOT NULL,
                `exerciseId` TEXT NOT NULL, `position` INTEGER NOT NULL,
                `notes` TEXT, `targetSets` INTEGER, `targetReps` TEXT, PRIMARY KEY(`id`),
                FOREIGN KEY(`workoutSessionId`) REFERENCES `workout_sessions`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`)
                    ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX `index_exercise_logs_workoutSessionId` ON `exercise_logs` (`workoutSessionId`)")
        database.execSQL("CREATE INDEX `index_exercise_logs_exerciseId` ON `exercise_logs` (`exerciseId`)")
        database.execSQL("INSERT INTO `exercise_logs` SELECT * FROM `exercise_logs_v10`")
        database.execSQL(
            """
            CREATE TABLE `set_logs` (
                `id` TEXT NOT NULL, `exerciseLogId` TEXT NOT NULL,
                `position` INTEGER NOT NULL, `repetitions` INTEGER NOT NULL,
                `weightKg` REAL NOT NULL, `isCompleted` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`exerciseLogId`) REFERENCES `exercise_logs`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX `index_set_logs_exerciseLogId` ON `set_logs` (`exerciseLogId`)")
        database.execSQL("INSERT INTO `set_logs` SELECT * FROM `set_logs_v10`")
        tables.forEach { database.execSQL("DROP TABLE `${it}_v10`") }
    }

    private fun migrateNutritionOwnership(database: SupportSQLiteDatabase) {
        val tables = listOf("saved_meals", "saved_meal_items", "food_diary_entries", "meal_quality_check_ins")
        tables.forEach { table ->
            database.execSQL("CREATE TEMP TABLE `${table}_v10` AS SELECT * FROM `$table`")
        }
        listOf("food_diary_entries", "saved_meal_items", "saved_meals", "meal_quality_check_ins")
            .forEach { database.execSQL("DROP TABLE `$it`") }

        database.execSQL(
            """
            CREATE TABLE `saved_meals` (
                `id` TEXT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL, `bodyProfileId` TEXT NOT NULL,
                PRIMARY KEY(`id`), FOREIGN KEY(`bodyProfileId`) REFERENCES `body_profiles`(`id`)
                    ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX `index_saved_meals_bodyProfileId` ON `saved_meals` (`bodyProfileId`)")
        database.execSQL("INSERT INTO `saved_meals` SELECT *, (SELECT id FROM body_profiles ORDER BY createdAt, id LIMIT 1) FROM `saved_meals_v10`")
        database.execSQL(
            """
            CREATE TABLE `saved_meal_items` (
                `id` TEXT NOT NULL, `savedMealId` TEXT NOT NULL, `foodId` TEXT NOT NULL,
                `servings` REAL NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`id`),
                FOREIGN KEY(`savedMealId`) REFERENCES `saved_meals`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`foodId`) REFERENCES `foods`(`id`)
                    ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX `index_saved_meal_items_savedMealId` ON `saved_meal_items` (`savedMealId`)")
        database.execSQL("CREATE INDEX `index_saved_meal_items_foodId` ON `saved_meal_items` (`foodId`)")
        database.execSQL("INSERT INTO `saved_meal_items` SELECT * FROM `saved_meal_items_v10`")
        database.execSQL(
            """
            CREATE TABLE `food_diary_entries` (
                `id` TEXT NOT NULL, `diaryDate` TEXT NOT NULL, `mealType` TEXT NOT NULL,
                `foodId` TEXT NOT NULL, `savedMealId` TEXT, `servings` REAL NOT NULL,
                `loggedAt` INTEGER NOT NULL, `bodyProfileId` TEXT NOT NULL, PRIMARY KEY(`id`),
                FOREIGN KEY(`foodId`) REFERENCES `foods`(`id`)
                    ON UPDATE NO ACTION ON DELETE NO ACTION,
                FOREIGN KEY(`savedMealId`) REFERENCES `saved_meals`(`id`)
                    ON UPDATE NO ACTION ON DELETE NO ACTION,
                FOREIGN KEY(`bodyProfileId`) REFERENCES `body_profiles`(`id`)
                    ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX `index_food_diary_entries_bodyProfileId` ON `food_diary_entries` (`bodyProfileId`)")
        database.execSQL("CREATE INDEX `index_food_diary_entries_diaryDate` ON `food_diary_entries` (`diaryDate`)")
        database.execSQL("CREATE INDEX `index_food_diary_entries_foodId` ON `food_diary_entries` (`foodId`)")
        database.execSQL("CREATE INDEX `index_food_diary_entries_savedMealId` ON `food_diary_entries` (`savedMealId`)")
        database.execSQL("INSERT INTO `food_diary_entries` SELECT *, (SELECT id FROM body_profiles ORDER BY createdAt, id LIMIT 1) FROM `food_diary_entries_v10`")
        database.execSQL(
            """
            CREATE TABLE `meal_quality_check_ins` (
                `id` TEXT NOT NULL, `diaryDate` TEXT NOT NULL, `mealType` TEXT NOT NULL,
                `quality` TEXT NOT NULL, `loggedAt` INTEGER NOT NULL,
                `bodyProfileId` TEXT NOT NULL, PRIMARY KEY(`id`),
                FOREIGN KEY(`bodyProfileId`) REFERENCES `body_profiles`(`id`)
                    ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX `index_meal_quality_check_ins_bodyProfileId` ON `meal_quality_check_ins` (`bodyProfileId`)")
        database.execSQL("CREATE UNIQUE INDEX `index_meal_quality_check_ins_bodyProfileId_diaryDate_mealType` ON `meal_quality_check_ins` (`bodyProfileId`, `diaryDate`, `mealType`)")
        database.execSQL("INSERT INTO `meal_quality_check_ins` SELECT *, (SELECT id FROM body_profiles ORDER BY createdAt, id LIMIT 1) FROM `meal_quality_check_ins_v10`")
        tables.forEach { database.execSQL("DROP TABLE `${it}_v10`") }
    }

    private fun migrateReviewOwnership(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TEMP TABLE `weekly_review_outcomes_v10` AS SELECT * FROM `weekly_review_outcomes`")
        database.execSQL("DROP TABLE `weekly_review_outcomes`")
        database.execSQL(
            """
            CREATE TABLE `weekly_review_outcomes` (
                `id` TEXT NOT NULL, `weekStart` TEXT NOT NULL, `status` TEXT NOT NULL,
                `draftType` TEXT, `sourcePlannedWorkoutId` TEXT, `sourceDate` TEXT,
                `targetDate` TEXT, `occurrenceId` TEXT, `decidedAt` INTEGER NOT NULL,
                `bodyProfileId` TEXT NOT NULL, PRIMARY KEY(`id`),
                FOREIGN KEY(`bodyProfileId`) REFERENCES `body_profiles`(`id`)
                    ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX `index_weekly_review_outcomes_bodyProfileId` ON `weekly_review_outcomes` (`bodyProfileId`)")
        database.execSQL("CREATE UNIQUE INDEX `index_weekly_review_outcomes_bodyProfileId_weekStart` ON `weekly_review_outcomes` (`bodyProfileId`, `weekStart`)")
        database.execSQL("CREATE INDEX `index_weekly_review_outcomes_occurrenceId` ON `weekly_review_outcomes` (`occurrenceId`)")
        database.execSQL("INSERT INTO `weekly_review_outcomes` SELECT *, (SELECT id FROM body_profiles ORDER BY createdAt, id LIMIT 1) FROM `weekly_review_outcomes_v10`")
        database.execSQL("DROP TABLE `weekly_review_outcomes_v10`")
    }
}
