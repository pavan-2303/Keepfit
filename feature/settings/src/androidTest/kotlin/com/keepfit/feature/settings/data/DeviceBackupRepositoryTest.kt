package com.keepfit.feature.settings.data

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keepfit.core.database.KeepfitDatabase
import com.keepfit.core.database.KeepfitDatabaseFactory
import com.keepfit.core.database.nutrition.FoodDiaryEntryEntity
import com.keepfit.core.database.nutrition.FoodEntity
import com.keepfit.core.database.nutrition.MealType
import com.keepfit.core.database.nutrition.MealQuality
import com.keepfit.core.database.nutrition.MealQualityCheckInEntity
import com.keepfit.core.database.journey.JourneyProfileEntity
import com.keepfit.core.database.profile.BodyProfileEntity
import com.keepfit.core.database.review.WeeklyReviewOutcomeEntity
import com.keepfit.core.database.transformation.BodyMeasurementEntity
import com.keepfit.core.database.transformation.TransformationCycleEntity
import com.keepfit.core.database.transformation.TransformationPhotoAngle
import com.keepfit.core.database.transformation.TransformationPhotoEntity
import com.keepfit.core.database.workout.ExerciseEntity
import com.keepfit.core.database.workout.ExerciseLogEntity
import com.keepfit.core.database.workout.ExerciseMediaEntity
import com.keepfit.core.database.workout.PlannedWorkoutEntity
import com.keepfit.core.database.workout.SetLogEntity
import com.keepfit.core.database.workout.WeeklyPlanEntity
import com.keepfit.core.database.workout.WorkoutSessionEntity
import com.keepfit.core.database.workout.WorkoutTemplateEntity
import com.keepfit.core.database.workout.WorkoutTemplateExerciseEntity
import com.keepfit.core.database.workout.WorkoutOccurrenceEntity
import com.keepfit.core.database.workout.WorkoutOccurrenceExerciseEntity
import com.keepfit.core.preferences.AppSettingsRepository
import com.keepfit.core.preferences.DataStoreAppSettingsRepository
import com.keepfit.core.preferences.MeasurementUnit
import com.keepfit.core.preferences.NutritionTrackingDepth
import com.keepfit.core.preferences.ReminderScheduler
import com.keepfit.core.preferences.WeightUnit
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceBackupRepositoryTest {
    private lateinit var context: Context
    private lateinit var database: KeepfitDatabase
    private lateinit var settingsRepository: AppSettingsRepository
    private lateinit var repository: DeviceBackupRepository

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        resetAppState()
        database = KeepfitDatabaseFactory.create(context)
        settingsRepository = DataStoreAppSettingsRepository(
            context = context,
            reminderScheduler = NoOpReminderScheduler,
        )
        repository = DeviceBackupRepository(context, database, settingsRepository)
    }

    @After
    fun tearDown() = runBlocking {
        runCatching { database.close() }
        resetAppState()
    }

    @Test
    fun exportPreviewAndRestoreRoundTripRecoversDatabaseSettingsAndMedia() = runBlocking {
        val now = 1_717_200_000_000L
        val profileId = "profile-1"
        val foodId = "food-1"
        val cycleId = "cycle-1"
        val exerciseId = "exercise-1"
        val templateId = "template-1"
        val planId = "plan-1"
        val plannedWorkoutId = "planned-1"
        val sessionId = "session-1"
        val exerciseLogId = "exercise-log-1"
        val frontPhotoDate = LocalDate.parse("2026-06-01")
        val exerciseMediaBytes = "exercise-demo".encodeToByteArray()
        val transformationPhotoBytes = "front-photo".encodeToByteArray()

        seedOriginalState(
            now = now,
            profileId = profileId,
            foodId = foodId,
            cycleId = cycleId,
            exerciseId = exerciseId,
            templateId = templateId,
            planId = planId,
            plannedWorkoutId = plannedWorkoutId,
            sessionId = sessionId,
            exerciseLogId = exerciseLogId,
            frontPhotoDate = frontPhotoDate,
            exerciseMediaBytes = exerciseMediaBytes,
            transformationPhotoBytes = transformationPhotoBytes,
        )
        val assistantCredentialMarker = "assistant-credential-must-not-enter-backup"
        context.getSharedPreferences("keepfit_assistant_secure", Context.MODE_PRIVATE)
            .edit()
            .putString("credential", assistantCredentialMarker)
            .commit()
        val assistantDraftMarker = "assistant-draft-must-not-enter-backup"
        context.getSharedPreferences("keepfit_assistant_drafts", Context.MODE_PRIVATE)
            .edit()
            .putString("encrypted_draft", assistantDraftMarker)
            .commit()

        val backupFile = File(context.cacheDir, "backup-roundtrip-test.kfit").apply {
            if (exists()) delete()
        }
        val backupUri = Uri.fromFile(backupFile)

        repository.exportBackup(backupUri, "long-secret")

        val inspectionDirectory = File(context.cacheDir, "backup-secret-exclusion").apply {
            deleteRecursively()
            mkdirs()
        }
        val extracted = BackupArchiveCodec().extractValidatedArchive(
            passphrase = "long-secret",
            inputStream = backupFile.inputStream(),
            workingDirectory = inspectionDirectory,
        )
        assertFalse(
            extracted.rootDirectory.walkTopDown()
                .filter(File::isFile)
                .any { file -> file.readBytes().toString(Charsets.ISO_8859_1).contains(assistantCredentialMarker) },
        )
        assertFalse(
            extracted.rootDirectory.walkTopDown()
                .filter(File::isFile)
                .any { file -> file.readBytes().toString(Charsets.ISO_8859_1).contains(assistantDraftMarker) },
        )
        assertFalse(extracted.rootDirectory.walkTopDown().any { it.name.contains("assistant_secure") })
        assertFalse(extracted.rootDirectory.walkTopDown().any { it.name.contains("assistant_drafts") })
        inspectionDirectory.deleteRecursively()

        val preview = repository.previewBackup(backupUri, "long-secret")
        assertEquals(KeepfitDatabase.VERSION, preview.databaseSchemaVersion)
        assertEquals(1, preview.recordCounts["profiles"])
        assertEquals(1, preview.recordCounts["journeyProfiles"])
        assertEquals(1, preview.recordCounts["foods"])
        assertEquals(1, preview.recordCounts["diaryEntries"])
        assertEquals(1, preview.recordCounts["mealQualityCheckIns"])
        assertEquals(1, preview.recordCounts["exercises"])
        assertEquals(1, preview.recordCounts["plannedWorkouts"])
        assertEquals(1, preview.recordCounts["workoutOccurrences"])
        assertEquals(1, preview.recordCounts["workoutOccurrenceExercises"])
        assertEquals(1, preview.recordCounts["weeklyReviewOutcomes"])
        assertEquals(1, preview.recordCounts["measurements"])
        assertEquals(1, preview.recordCounts["transformationCycles"])
        assertEquals(1, preview.recordCounts["transformationPhotos"])
        assertEquals(
            (exerciseMediaBytes.size + transformationPhotoBytes.size).toLong(),
            preview.mediaSizeBytes,
        )

        mutateLocalState(now + 5_000L)

        repository.restoreBackup(backupUri, "long-secret")
        database = KeepfitDatabaseFactory.create(context)

        val restoredProfile = database.bodyProfileDao().findLocalProfile()
        assertNotNull(restoredProfile)
        assertEquals("Pavan", restoredProfile?.displayName)
        assertEquals(178.0, restoredProfile?.heightCm)
        assertEquals(
            "CONSISTENCY",
            database.journeyDao().findForBodyProfile(profileId)?.primaryGoal,
        )

        val restoredFood = database.nutritionDao().findFood(foodId)
        assertNotNull(restoredFood)
        assertEquals("Chicken and rice", restoredFood?.name)
        assertEquals(
            MealQuality.BALANCED,
            database.nutritionDao().observeMealQualityCheckIns(frontPhotoDate).first().single().quality,
        )

        val restoredCycle = database.transformationDao().findCycleById(cycleId)
        assertNotNull(restoredCycle)
        assertEquals(frontPhotoDate, restoredCycle?.startDate)

        val restoredPhoto = database.transformationDao().findPhoto(
            cycleId = cycleId,
            captureDate = frontPhotoDate,
            angle = TransformationPhotoAngle.FRONT,
        )
        assertNotNull(restoredPhoto)

        val restoredExerciseMedia = database.workoutDao().findExerciseMedia(exerciseId)
        assertNotNull(restoredExerciseMedia)
        val restoredOccurrence = database.workoutDao().findOccurrenceDetails("occurrence-1")
        assertNotNull(restoredOccurrence)
        assertEquals("Barbell Row", restoredOccurrence?.exercises?.single()?.exerciseNameSnapshot)
        assertEquals(
            "APPROVED",
            database.weeklyReviewDao().findForWeek(frontPhotoDate)?.status,
        )
        database.openHelper.readableDatabase.query(
            "SELECT sessionVariant, energyLevel, difficulty FROM workout_sessions WHERE id = '$sessionId'",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("SHORTENED", cursor.getString(0))
            assertEquals(4, cursor.getInt(1))
            assertEquals(3, cursor.getInt(2))
        }
        database.openHelper.readableDatabase.query(
            "SELECT targetSets, targetReps FROM exercise_logs WHERE id = '$exerciseLogId'",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(4, cursor.getInt(0))
            assertEquals("8-10", cursor.getString(1))
        }

        val restoredSettings = settingsRepository.observeSettings().first()
        assertEquals(WeightUnit.LB, restoredSettings.weightUnit)
        assertEquals(MeasurementUnit.IN, restoredSettings.measurementUnit)
        assertEquals(135, restoredSettings.restTimerSeconds)
        assertEquals(true, restoredSettings.weeklyReviewPaused)
        assertEquals(NutritionTrackingDepth.MEAL_QUALITY, restoredSettings.nutritionTrackingDepth)
        assertEquals(15, restoredSettings.nutritionTargetRangePercent)
        assertEquals(true, restoredSettings.workoutReminder.enabled)
        assertEquals(6, restoredSettings.workoutReminder.hour)
        assertEquals(45, restoredSettings.workoutReminder.minute)
        assertEquals(true, restoredSettings.transformationReminder.enabled)
        assertEquals(DayOfWeek.THURSDAY, restoredSettings.transformationReminder.dayOfWeek)

        val restoredExerciseMediaFile = File(context.filesDir, restoredExerciseMedia!!.relativePath)
        val restoredTransformationPhotoFile = File(context.filesDir, restoredPhoto!!.relativePath)
        assertArrayEquals(exerciseMediaBytes, restoredExerciseMediaFile.readBytes())
        assertArrayEquals(transformationPhotoBytes, restoredTransformationPhotoFile.readBytes())
    }

    private suspend fun seedOriginalState(
        now: Long,
        profileId: String,
        foodId: String,
        cycleId: String,
        exerciseId: String,
        templateId: String,
        planId: String,
        plannedWorkoutId: String,
        sessionId: String,
        exerciseLogId: String,
        frontPhotoDate: LocalDate,
        exerciseMediaBytes: ByteArray,
        transformationPhotoBytes: ByteArray,
    ) {
        val bodyProfileDao = database.bodyProfileDao()
        val nutritionDao = database.nutritionDao()
        val transformationDao = database.transformationDao()
        val workoutDao = database.workoutDao()

        bodyProfileDao.upsert(
            BodyProfileEntity(
                id = profileId,
                displayName = "Pavan",
                heightCm = 178.0,
                birthDate = LocalDate.parse("1994-01-12"),
                dailyCalorieGoal = 2600.0,
                dailyProteinGoalGrams = 180.0,
                dailyCarbohydrateGoalGrams = 250.0,
                dailyFatGoalGrams = 70.0,
                createdAt = now,
                updatedAt = now,
            ),
        )
        database.journeyDao().upsert(
            JourneyProfileEntity(
                id = "journey-1",
                bodyProfileId = profileId,
                primaryGoal = "CONSISTENCY",
                experienceLevel = "BEGINNER",
                preferredDays = "MONDAY,THURSDAY",
                sessionMinutes = 30,
                equipment = "BODYWEIGHT",
                avoidedExerciseKeys = "reverse-lunge",
                createdAt = now,
                updatedAt = now,
            ),
        )

        nutritionDao.upsertFood(
            FoodEntity(
                id = foodId,
                name = "Chicken and rice",
                servingLabel = "1 bowl",
                servingAmount = 1.0,
                calories = 520.0,
                proteinGrams = 38.0,
                carbohydrateGrams = 55.0,
                fatGrams = 12.0,
                isFavorite = true,
                createdAt = now,
                updatedAt = now,
                archivedAt = null,
            ),
        )
        nutritionDao.insertDiaryEntry(
            FoodDiaryEntryEntity(
                id = "diary-1",
                diaryDate = frontPhotoDate,
                mealType = MealType.LUNCH,
                foodId = foodId,
                savedMealId = null,
                servings = 1.0,
                loggedAt = now,
            ),
        )
        nutritionDao.upsertMealQualityCheckIn(
            MealQualityCheckInEntity(
                id = "quality-1",
                diaryDate = frontPhotoDate,
                mealType = MealType.LUNCH,
                quality = MealQuality.BALANCED,
                loggedAt = now,
            ),
        )

        transformationDao.upsertMeasurement(
            BodyMeasurementEntity(
                id = "measurement-1",
                bodyProfileId = profileId,
                measurementDate = frontPhotoDate,
                weightKg = 82.5,
                waistCm = 83.0,
                chestCm = 101.0,
                hipsCm = 95.0,
                leftArmCm = 36.0,
                rightArmCm = 36.5,
                leftThighCm = 57.0,
                rightThighCm = 57.5,
                notes = "Baseline",
                createdAt = now,
            ),
        )
        transformationDao.upsertCycle(
            TransformationCycleEntity(
                id = cycleId,
                bodyProfileId = profileId,
                startDate = frontPhotoDate,
                notes = "Summer cut",
                closedAt = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
        val transformationPhotoPath = "media/transformation/$cycleId/front-day-0.jpg"
        writeMediaFile(transformationPhotoPath, transformationPhotoBytes)
        transformationDao.upsertPhoto(
            TransformationPhotoEntity(
                id = "photo-1",
                transformationCycleId = cycleId,
                captureDate = frontPhotoDate,
                angle = TransformationPhotoAngle.FRONT,
                relativePath = transformationPhotoPath,
                mimeType = "image/jpeg",
                sizeBytes = transformationPhotoBytes.size.toLong(),
                createdAt = now,
            ),
        )

        workoutDao.upsertExercise(
            ExerciseEntity(
                id = exerciseId,
                name = "Barbell Row",
                muscleGroup = "Back",
                instructions = "Drive elbows back",
                notes = "Use straps if needed",
                isBodyweight = false,
                createdAt = now,
                updatedAt = now,
                archivedAt = null,
            ),
        )
        val exerciseMediaPath = "media/exercises/barbell-row-demo.mp4"
        writeMediaFile(exerciseMediaPath, exerciseMediaBytes)
        workoutDao.upsertExerciseMedia(
            ExerciseMediaEntity(
                id = "exercise-media-1",
                exerciseId = exerciseId,
                mediaType = "VIDEO",
                relativePath = exerciseMediaPath,
                mimeType = "video/mp4",
                sizeBytes = exerciseMediaBytes.size.toLong(),
                createdAt = now,
            ),
        )
        workoutDao.upsertTemplate(
            WorkoutTemplateEntity(
                id = templateId,
                name = "Pull Day",
                notes = "Focus on back volume",
                createdAt = now,
                updatedAt = now,
                archivedAt = null,
            ),
        )
        workoutDao.replaceTemplateExercises(
            templateId = templateId,
            exercises = listOf(
                WorkoutTemplateExerciseEntity(
                    id = "template-exercise-1",
                    workoutTemplateId = templateId,
                    exerciseId = exerciseId,
                    position = 0,
                    targetSets = 4,
                    targetReps = "8-10",
                    notes = null,
                ),
            ),
        )
        workoutDao.deactivateWeeklyPlans()
        workoutDao.upsertWeeklyPlan(
            WeeklyPlanEntity(
                id = planId,
                name = "Default Week",
                startsOn = frontPhotoDate,
                isActive = true,
                createdAt = now,
                updatedAt = now,
            ),
        )
        workoutDao.upsertPlannedWorkout(
            PlannedWorkoutEntity(
                id = plannedWorkoutId,
                weeklyPlanId = planId,
                workoutTemplateId = templateId,
                dayOfWeek = DayOfWeek.MONDAY,
                position = 0,
            ),
        )
        workoutDao.replaceOccurrence(
            occurrence = WorkoutOccurrenceEntity(
                id = "occurrence-1",
                sourcePlannedWorkoutId = plannedWorkoutId,
                sourceTemplateId = templateId,
                templateNameSnapshot = "Pull Day",
                originalDate = frontPhotoDate,
                scheduledDate = frontPhotoDate,
                decisionType = "FULL",
                createdAt = now,
                updatedAt = now,
            ),
            exercises = listOf(
                WorkoutOccurrenceExerciseEntity(
                    id = "occurrence-exercise-1",
                    workoutOccurrenceId = "occurrence-1",
                    sourceTemplateExerciseId = "template-exercise-1",
                    exerciseId = exerciseId,
                    exerciseNameSnapshot = "Barbell Row",
                    position = 0,
                    targetSets = 4,
                    targetReps = "8-10",
                ),
            ),
        )
        workoutDao.insertSession(
            WorkoutSessionEntity(
                id = sessionId,
                workoutTemplateId = templateId,
                plannedWorkoutId = plannedWorkoutId,
                workoutDate = frontPhotoDate,
                startedAt = now,
                completedAt = now + 3_600_000L,
                notes = "Strong session",
                workoutOccurrenceId = "occurrence-1",
                sessionVariant = "SHORTENED",
                energyLevel = 4,
                difficulty = 3,
            ),
        )
        workoutDao.insertExerciseLog(
            ExerciseLogEntity(
                id = exerciseLogId,
                workoutSessionId = sessionId,
                exerciseId = exerciseId,
                position = 0,
                notes = "Top set felt solid",
                targetSets = 4,
                targetReps = "8-10",
            ),
        )
        workoutDao.insertSetLog(
            SetLogEntity(
                id = "set-1",
                exerciseLogId = exerciseLogId,
                position = 0,
                repetitions = 10,
                weightKg = 70.0,
                isCompleted = true,
            ),
        )

        database.weeklyReviewDao().upsert(
            WeeklyReviewOutcomeEntity(
                id = "review-1",
                weekStart = frontPhotoDate,
                status = "APPROVED",
                draftType = "SHORTEN_SESSION",
                sourcePlannedWorkoutId = plannedWorkoutId,
                sourceDate = frontPhotoDate,
                targetDate = frontPhotoDate,
                occurrenceId = "occurrence-1",
                decidedAt = now,
            ),
        )

        settingsRepository.updateUnits(WeightUnit.LB, MeasurementUnit.IN)
        settingsRepository.updateRestTimerSeconds(135)
        settingsRepository.updateWeeklyReviewPaused(true)
        settingsRepository.updateNutritionTracking(NutritionTrackingDepth.MEAL_QUALITY, 15)
        settingsRepository.updateWorkoutReminder(enabled = true, hour = 6, minute = 45)
        settingsRepository.updateTransformationReminder(
            enabled = true,
            dayOfWeekOrdinal = DayOfWeek.THURSDAY.value,
            hour = 8,
            minute = 15,
        )
    }

    private suspend fun mutateLocalState(now: Long) {
        database.clearAllTables()
        deleteMediaDirectory()
        settingsRepository.updateUnits(WeightUnit.KG, MeasurementUnit.CM)
        settingsRepository.updateRestTimerSeconds(45)
        settingsRepository.updateWeeklyReviewPaused(false)
        settingsRepository.updateNutritionTracking(NutritionTrackingDepth.DISABLED, 5)
        settingsRepository.updateWorkoutReminder(enabled = false, hour = 18, minute = 0)
        settingsRepository.updateTransformationReminder(
            enabled = false,
            dayOfWeekOrdinal = DayOfWeek.SUNDAY.value,
            hour = 9,
            minute = 0,
        )

        database.bodyProfileDao().upsert(
            BodyProfileEntity(
                id = "mutated-profile",
                displayName = "Changed",
                heightCm = 165.0,
                birthDate = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    private suspend fun resetAppState() {
        runCatching { database.close() }
        val existingDatabase = KeepfitDatabaseFactory.create(context)
        try {
            existingDatabase.clearAllTables()
        } finally {
            existingDatabase.close()
        }
        deleteMediaDirectory()
        DataStoreAppSettingsRepository(
            context = context,
            reminderScheduler = NoOpReminderScheduler,
        ).apply {
            updateUnits(WeightUnit.KG, MeasurementUnit.CM)
            updateRestTimerSeconds(90)
            updateWeeklyReviewPaused(false)
            updateNutritionTracking(NutritionTrackingDepth.DETAILED_MACROS, 10)
            updateWorkoutReminder(enabled = false, hour = 18, minute = 0)
            updateTransformationReminder(
                enabled = false,
                dayOfWeekOrdinal = DayOfWeek.SUNDAY.value,
                hour = 9,
                minute = 0,
            )
            updateAssistantSettings(false)
        }
        context.getSharedPreferences("keepfit_assistant_secure", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        context.getSharedPreferences("keepfit_assistant_drafts", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun writeMediaFile(relativePath: String, bytes: ByteArray) {
        val file = File(context.filesDir, relativePath)
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
    }

    private fun deleteMediaDirectory() {
        File(context.filesDir, BackupArchiveCodec.MEDIA_DIRECTORY).deleteRecursively()
    }

    private object NoOpReminderScheduler : ReminderScheduler {
        override suspend fun sync(settings: com.keepfit.core.preferences.AppSettings) = Unit
    }
}
