package com.keepfit.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object KeepfitMigrations {
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
}
