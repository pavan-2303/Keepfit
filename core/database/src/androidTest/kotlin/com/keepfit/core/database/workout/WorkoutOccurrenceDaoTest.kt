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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutOccurrenceDaoTest {
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
    fun closeDatabase() = database.close()

    @Test
    fun occurrenceSnapshotSurvivesSourcePlanRemovalAndLinksToSession() = runBlocking {
        seedPlan()
        val occurrence = WorkoutOccurrenceEntity(
            id = "occurrence",
            sourcePlannedWorkoutId = "planned",
            sourceTemplateId = "template",
            templateNameSnapshot = "Foundation A",
            originalDate = LocalDate.parse("2026-09-12"),
            scheduledDate = LocalDate.parse("2026-09-13"),
            decisionType = "RESCHEDULED",
            createdAt = 10L,
            updatedAt = 10L,
        )
        val snapshot = WorkoutOccurrenceExerciseEntity(
            id = "occurrence-exercise",
            workoutOccurrenceId = occurrence.id,
            sourceTemplateExerciseId = "template-exercise",
            exerciseId = "exercise",
            exerciseNameSnapshot = "Chair squat",
            position = 0,
            targetSets = 2,
            targetReps = "8-10",
        )

        dao.replaceOccurrence(occurrence, listOf(snapshot))

        val stored = dao.observeOccurrenceDetails(
            LocalDate.parse("2026-09-05"),
            LocalDate.parse("2026-09-20"),
        ).first().single()
        assertEquals("Chair squat", stored.exercises.single().exerciseNameSnapshot)

        dao.deletePlannedWorkouts("plan", DayOfWeek.SATURDAY)
        assertNull(dao.findOccurrence(occurrence.id)?.sourcePlannedWorkoutId)
        assertEquals(1, dao.findOccurrenceDetails(occurrence.id)?.exercises?.size)

        dao.startSessionIfNoneActive(
            session = WorkoutSessionEntity(
                id = "session",
                workoutTemplateId = "template",
                plannedWorkoutId = null,
                workoutDate = LocalDate.parse("2026-09-13"),
                startedAt = 11L,
                completedAt = null,
                notes = null,
                workoutOccurrenceId = occurrence.id,
            ),
            logs = listOf(
                ExerciseLogEntity(
                    id = "log",
                    workoutSessionId = "session",
                    exerciseId = "exercise",
                    position = 0,
                    notes = null,
                ),
            ),
        )

        assertEquals(occurrence.id, dao.findActiveSession()?.workoutOccurrenceId)
    }

    private suspend fun seedPlan() {
        dao.upsertExercise(
            ExerciseEntity(
                id = "exercise",
                name = "Chair squat",
                muscleGroup = "Legs",
                instructions = null,
                notes = null,
                isBodyweight = true,
                createdAt = 1L,
                updatedAt = 1L,
                archivedAt = null,
            ),
        )
        dao.upsertTemplate(
            WorkoutTemplateEntity(
                id = "template",
                name = "Foundation A",
                notes = null,
                createdAt = 2L,
                updatedAt = 2L,
                archivedAt = null,
            ),
        )
        dao.replaceTemplateExercises(
            "template",
            listOf(
                WorkoutTemplateExerciseEntity(
                    id = "template-exercise",
                    workoutTemplateId = "template",
                    exerciseId = "exercise",
                    position = 0,
                    targetSets = 3,
                    targetReps = "8-10",
                    notes = null,
                ),
            ),
        )
        dao.upsertWeeklyPlan(
            WeeklyPlanEntity(
                id = "plan",
                name = "Starter week",
                startsOn = LocalDate.parse("2026-09-07"),
                isActive = true,
                createdAt = 3L,
                updatedAt = 3L,
            ),
        )
        dao.upsertPlannedWorkout(
            PlannedWorkoutEntity(
                id = "planned",
                weeklyPlanId = "plan",
                workoutTemplateId = "template",
                dayOfWeek = DayOfWeek.SATURDAY,
                position = 0,
            ),
        )
    }
}
