package com.keepfit.core.database.workout

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keepfit.core.database.KeepfitDatabase
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
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KeepfitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.workoutDao()
    }

    @After
    fun closeDatabase() {
        database.close()
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
        dao.completeSession("session", 5L)
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
}
