package com.keepfit.core.database.workout

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keepfit.core.database.KeepfitDatabase
import com.keepfit.core.database.profile.BodyProfileEntity
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutDaoTest {
    private lateinit var database: KeepfitDatabase
    private lateinit var dao: WorkoutDao

    @Before
    fun createDatabase() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KeepfitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.workoutDao()
        database.bodyProfileDao().upsert(
            BodyProfileEntity("", "Test", null, null, createdAt = 1L, updatedAt = 1L),
        )
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun exerciseDetailsIncludeOptionalPrivateDemoMedia() = runBlocking {
        val exercise = ExerciseEntity(
            id = "demo-exercise",
            name = "Demo exercise",
            muscleGroup = "General",
            instructions = "Move with control.",
            notes = null,
            isBodyweight = true,
            createdAt = 1L,
            updatedAt = 1L,
            archivedAt = null,
        )
        val media = ExerciseMediaEntity(
            id = "demo-media",
            exerciseId = exercise.id,
            mediaType = "VIDEO",
            relativePath = "media/exercises/demo.mp4",
            mimeType = "video/mp4",
            sizeBytes = 42L,
            createdAt = 2L,
        )
        dao.upsertExercise(exercise)
        dao.upsertExerciseMedia(media)

        val details = dao.observeExerciseDetails("demo").first().single()

        assertEquals(exercise, details.exercise)
        assertEquals(media, details.media)
    }

    @Test
    fun exerciseSearchMatchesPersonalMetadataAndInstructions() = runBlocking {
        val exercise = ExerciseEntity(
            id = "catalogue-exercise",
            name = "Supported row",
            muscleGroup = "upper arms",
            instructions = "Keep the torso stable.",
            notes = null,
            isBodyweight = false,
            createdAt = 1L,
            updatedAt = 1L,
            archivedAt = null,
            source = null,
            sourceId = null,
            equipment = "dumbbell",
            targetMuscle = "biceps",
            secondaryMuscles = "forearms, shoulders",
        )
        dao.upsertExercise(exercise)

        listOf("supported", "upper arms", "stable", "dumbbell", "biceps", "shoulders").forEach { query ->
            assertEquals(exercise.id, dao.observeExercises(query).first().single().id)
        }
    }

    @Test
    fun workoutLifecyclePreservesHistoryAndDerivesRecords() = runBlocking {
        val exercise = ExerciseEntity(
            id = "exercise",
            name = "Bench Press",
            muscleGroup = "Chest",
            instructions = null,
            notes = null,
            isBodyweight = false,
            createdAt = 1L,
            updatedAt = 1L,
            archivedAt = null,
        )
        dao.upsertExercise(exercise)

        assertEquals(listOf(exercise), dao.observeExercises("bench").first())

        val template = WorkoutTemplateEntity(
            id = "template",
            name = "Push day",
            notes = null,
            createdAt = 2L,
            updatedAt = 2L,
            archivedAt = null,
        )
        dao.upsertTemplate(template)
        dao.replaceTemplateExercises(
            templateId = template.id,
            exercises = listOf(
                WorkoutTemplateExerciseEntity(
                    id = "template-exercise",
                    workoutTemplateId = template.id,
                    exerciseId = exercise.id,
                    position = 0,
                    targetSets = 3,
                    targetReps = "8-10",
                    notes = null,
                ),
            ),
        )

        assertEquals(
            listOf("Bench Press"),
            dao.observeTemplateDetails().first().single().exercises.map { it.exerciseName },
        )

        dao.upsertWeeklyPlan(
            WeeklyPlanEntity(
                id = "plan",
                name = "Default week",
                startsOn = LocalDate.parse("2026-05-25"),
                isActive = true,
                createdAt = 3L,
                updatedAt = 3L,
            ),
        )
        dao.replacePlannedWorkout(
            PlannedWorkoutEntity(
                id = "planned",
                weeklyPlanId = "plan",
                workoutTemplateId = template.id,
                dayOfWeek = DayOfWeek.SATURDAY,
                position = 0,
            ),
        )

        assertEquals(
            "Push day",
            dao.observePlannedWorkouts(DayOfWeek.SATURDAY).first().single().templateName,
        )

        dao.insertSession(
            WorkoutSessionEntity(
                id = "session",
                workoutTemplateId = template.id,
                plannedWorkoutId = "planned",
                workoutDate = LocalDate.parse("2026-05-30"),
                startedAt = 4L,
                completedAt = null,
                notes = null,
            ),
        )
        dao.insertExerciseLog(
            ExerciseLogEntity(
                id = "exercise-log",
                workoutSessionId = "session",
                exerciseId = exercise.id,
                position = 0,
                notes = null,
            ),
        )
        dao.insertSetLog(
            SetLogEntity(
                id = "set",
                exerciseLogId = "exercise-log",
                position = 0,
                repetitions = 10,
                weightKg = 60.0,
                isCompleted = true,
            ),
        )
        dao.completeSession("session", 5L, energyLevel = null, difficulty = null)
        dao.archiveExercise(exercise.id, 6L)

        assertTrue(dao.observeExercises("").first().isEmpty())
        assertEquals("Bench Press", dao.observeSessionHistory().first().single().exercises.single().exerciseName)
        assertEquals(60.0, dao.observePersonalRecords().first().single().highestWeightKg, 0.0)
        assertEquals(10, dao.observePersonalRecords().first().single().highestRepetitions)
    }

    @Test
    fun deleteQueriesClearFuturePlanningWithoutTouchingHistoryTables() = runBlocking {
        val exercise = ExerciseEntity(
            id = "exercise-2",
            name = "Squat",
            muscleGroup = "Legs",
            instructions = null,
            notes = null,
            isBodyweight = false,
            createdAt = 1L,
            updatedAt = 1L,
            archivedAt = null,
        )
        dao.upsertExercise(exercise)
        val template = WorkoutTemplateEntity(
            id = "template-2",
            name = "Lower day",
            notes = null,
            createdAt = 2L,
            updatedAt = 2L,
            archivedAt = null,
        )
        dao.upsertTemplate(template)
        dao.replaceTemplateExercises(
            templateId = template.id,
            exercises = listOf(
                WorkoutTemplateExerciseEntity(
                    id = "template-exercise-2",
                    workoutTemplateId = template.id,
                    exerciseId = exercise.id,
                    position = 0,
                    targetSets = 3,
                    targetReps = "5",
                    notes = null,
                ),
            ),
        )
        dao.upsertWeeklyPlan(
            WeeklyPlanEntity(
                id = "plan-2",
                name = "Default week",
                startsOn = LocalDate.parse("2026-05-25"),
                isActive = true,
                createdAt = 3L,
                updatedAt = 3L,
            ),
        )
        dao.replacePlannedWorkout(
            PlannedWorkoutEntity(
                id = "planned-2",
                weeklyPlanId = "plan-2",
                workoutTemplateId = template.id,
                dayOfWeek = DayOfWeek.MONDAY,
                position = 0,
            ),
        )

        dao.deletePlannedWorkouts("plan-2", DayOfWeek.MONDAY)
        assertTrue(dao.observePlannedWorkouts(DayOfWeek.MONDAY).first().isEmpty())

        dao.deleteTemplate(template.id)
        assertTrue(dao.observeTemplates().first().isEmpty())

        dao.deleteExercise(exercise.id)
        assertTrue(dao.observeExercises("").first().isEmpty())
    }

    @Test
    fun weeklyScheduleReplacementRollsBackWhenAnyAssignmentIsInvalid() = runBlocking {
        val template = WorkoutTemplateEntity(
            id = "valid-template",
            name = "Full body",
            notes = null,
            createdAt = 1L,
            updatedAt = 1L,
            archivedAt = null,
        )
        dao.upsertTemplate(template)
        val originalPlan = WeeklyPlanEntity(
            id = "default-weekly-plan",
            name = "Default week",
            startsOn = LocalDate.parse("2026-09-07"),
            isActive = true,
            createdAt = 2L,
            updatedAt = 2L,
        )
        dao.upsertWeeklyPlan(originalPlan)
        dao.upsertPlannedWorkout(
            PlannedWorkoutEntity(
                id = "original-workout",
                weeklyPlanId = originalPlan.id,
                workoutTemplateId = template.id,
                dayOfWeek = DayOfWeek.MONDAY,
                position = 0,
            ),
        )

        val result = runCatching {
            dao.replaceWeeklySchedule(
                plan = originalPlan.copy(updatedAt = 3L),
                workouts = listOf(
                    PlannedWorkoutEntity(
                        id = "invalid-workout",
                        weeklyPlanId = originalPlan.id,
                        workoutTemplateId = "missing-template",
                        dayOfWeek = DayOfWeek.TUESDAY,
                        position = 0,
                    ),
                ),
            )
        }

        assertTrue(result.isFailure)
        assertEquals(
            "original-workout",
            dao.observePlannedWorkouts(DayOfWeek.MONDAY).first().single().id,
        )
        assertTrue(dao.observePlannedWorkouts(DayOfWeek.TUESDAY).first().isEmpty())
    }
}
