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
}

