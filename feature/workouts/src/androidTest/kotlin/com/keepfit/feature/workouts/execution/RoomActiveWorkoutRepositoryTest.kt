package com.keepfit.feature.workouts.execution

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keepfit.core.database.KeepfitDatabase
import com.keepfit.core.database.workout.ExerciseEntity
import com.keepfit.core.database.workout.ExerciseLogEntity
import com.keepfit.core.database.workout.SetLogEntity
import com.keepfit.core.database.workout.WorkoutSessionEntity
import com.keepfit.core.database.workout.WorkoutTemplateEntity
import com.keepfit.core.database.workout.WorkoutTemplateExerciseEntity
import com.keepfit.core.media.ExerciseMediaStore
import com.keepfit.core.preferences.AppSettings
import com.keepfit.core.preferences.AppSettingsRepository
import com.keepfit.core.preferences.MeasurementUnit
import com.keepfit.core.preferences.WeightUnit
import com.keepfit.feature.workouts.CompletedSetInput
import com.keepfit.feature.workouts.data.RoomWorkoutRepository
import com.keepfit.feature.workouts.data.WorkoutFeedback
import java.time.LocalDate
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomActiveWorkoutRepositoryTest {
    private val today = LocalDate.parse("2026-09-12")
    private lateinit var database: KeepfitDatabase
    private lateinit var repository: RoomWorkoutRepository
    private var nextId = 0

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KeepfitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = newRepository(context)
        seedExercisesAndTemplate()
        seedCompletedPerformance()
        seedActiveSession()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun activeSessionRestoresTargetsPreviousSetsAndProgression() = runBlocking {
        val active = repository.observeActiveWorkout().first()!!
        val first = active.exercises.first()

        assertEquals(3, first.targetSets)
        assertEquals("8-10", first.targetReps)
        assertEquals(3, first.previousSets.size)
        assertEquals(20.0, first.nextSetSuggestion?.weightKg ?: 0.0, 0.0)
        assertEquals(ProgressionChange.INCREASE_WEIGHT, first.progressionSuggestion?.change)

        val recreated = newRepository(ApplicationProvider.getApplicationContext())
            .observeActiveWorkout()
            .first()!!
        assertEquals(active.sessionId, recreated.sessionId)
        assertEquals(first.targetSets, recreated.exercises.first().targetSets)
    }

    @Test
    fun repeatPreviousSetAppendsTheMatchingPriorPositions() = runBlocking {
        val exerciseLogId = repository.observeActiveWorkout().first()!!.exercises.first().exerciseLogId

        repository.repeatPreviousSet(exerciseLogId)
        repository.repeatPreviousSet(exerciseLogId)

        val sets = repository.observeActiveWorkout().first()!!.exercises.first().sets
        assertEquals(listOf(10, 10), sets.map { it.repetitions })
        assertEquals(listOf(20.0, 20.0), sets.map { it.weightKg })
    }

    @Test
    fun concurrentSetCommandsReceiveUniqueSequentialPositions() = runBlocking {
        val exerciseLogId = repository.observeActiveWorkout().first()!!.exercises.first().exerciseLogId

        coroutineScope {
            List(5) { index ->
                async { repository.addSet(exerciseLogId, CompletedSetInput(8 + index, 20.0)) }
            }.awaitAll()
        }

        database.openHelper.readableDatabase.query(
            "SELECT position FROM set_logs WHERE exerciseLogId = '$exerciseLogId' ORDER BY position",
        ).use { cursor ->
            val positions = buildList {
                while (cursor.moveToNext()) add(cursor.getInt(0))
            }
            assertEquals(listOf(0, 1, 2, 3, 4), positions)
        }
    }

    @Test
    fun substitutionChangesOnlyAnUntouchedActiveExercise() = runBlocking {
        val active = repository.observeActiveWorkout().first()!!
        val source = active.exercises[1]

        repository.substituteActiveExercise(source.exerciseLogId, "exercise-5")

        val updated = repository.observeActiveWorkout().first()!!
        assertEquals("exercise-5", updated.exercises[1].exerciseId)
        assertEquals(
            "exercise-2",
            database.workoutDao().findTemplateDetails("template")
                ?.exercises
                ?.sortedBy { it.templateExercise.position }
                ?.get(1)
                ?.templateExercise
                ?.exerciseId,
        )

        val duplicate = runCatching {
            repository.substituteActiveExercise(updated.exercises[2].exerciseLogId, "exercise-5")
        }
        assertTrue(duplicate.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun substitutionRejectsAnExerciseWithLoggedWork() = runBlocking {
        val first = repository.observeActiveWorkout().first()!!.exercises.first()
        repository.repeatPreviousSet(first.exerciseLogId)

        val result = runCatching {
            repository.substituteActiveExercise(first.exerciseLogId, "exercise-5")
        }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("exercise-1", repository.observeActiveWorkout().first()!!.exercises.first().exerciseId)
    }

    @Test
    fun minimumSessionKeepsFirstTwoExercisesAndNeverDeletesLoggedWork() = runBlocking {
        repository.convertActiveWorkoutToMinimum()

        val minimum = repository.observeActiveWorkout().first()!!
        assertEquals(2, minimum.exercises.size)
        assertEquals("MINIMUM", minimum.sessionVariant)
    }

    @Test
    fun minimumSessionRejectsRemovalWhenLaterExerciseHasSets() = runBlocking {
        val third = repository.observeActiveWorkout().first()!!.exercises[2]
        database.workoutDao().insertSetLog(
            SetLogEntity("late-set", third.exerciseLogId, 0, 8, 10.0, true),
        )

        val result = runCatching { repository.convertActiveWorkoutToMinimum() }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals(4, repository.observeActiveWorkout().first()!!.exercises.size)
    }

    @Test
    fun completionStoresOptionalFeedbackAndEndsTheActiveSession() = runBlocking {
        repository.completeActiveWorkout(WorkoutFeedback(energyLevel = 4, difficulty = 3))

        assertNull(repository.observeActiveWorkout().first())
        database.openHelper.readableDatabase.query(
            "SELECT energyLevel, difficulty FROM workout_sessions WHERE id = 'active-session'",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(4, cursor.getInt(0))
            assertEquals(3, cursor.getInt(1))
        }
    }

    private fun newRepository(context: Context) = RoomWorkoutRepository(
        dao = database.workoutDao(),
        mediaStore = ExerciseMediaStore(context),
        settingsRepository = FixedSettingsRepository,
        idFactory = { "generated-${++nextId}" },
        clock = { 1_000L + nextId },
        today = { today },
    )

    private suspend fun seedExercisesAndTemplate() {
        val dao = database.workoutDao()
        (1..5).forEach { position ->
            dao.upsertExercise(
                ExerciseEntity(
                    id = "exercise-$position",
                    name = "Exercise $position",
                    muscleGroup = "General",
                    instructions = null,
                    notes = null,
                    isBodyweight = false,
                    createdAt = position.toLong(),
                    updatedAt = position.toLong(),
                    archivedAt = null,
                ),
            )
        }
        dao.upsertTemplate(WorkoutTemplateEntity("template", "Foundation A", null, 10, 10, null))
        dao.replaceTemplateExercises(
            "template",
            (1..4).map { position ->
                WorkoutTemplateExerciseEntity(
                    id = "template-exercise-$position",
                    workoutTemplateId = "template",
                    exerciseId = "exercise-$position",
                    position = position - 1,
                    targetSets = 3,
                    targetReps = "8-10",
                    notes = null,
                )
            },
        )
    }

    private suspend fun seedCompletedPerformance() {
        val dao = database.workoutDao()
        dao.insertSession(
            WorkoutSessionEntity(
                id = "past-session",
                workoutTemplateId = "template",
                plannedWorkoutId = null,
                workoutDate = today.minusDays(3),
                startedAt = 20,
                completedAt = 30,
                notes = null,
            ),
        )
        dao.insertExerciseLog(
            ExerciseLogEntity(
                id = "past-log",
                workoutSessionId = "past-session",
                exerciseId = "exercise-1",
                position = 0,
                notes = null,
                targetSets = 3,
                targetReps = "8-10",
            ),
        )
        repeat(3) { position ->
            dao.insertSetLog(SetLogEntity("past-set-$position", "past-log", position, 10, 20.0, true))
        }
    }

    private suspend fun seedActiveSession() {
        val dao = database.workoutDao()
        dao.insertSession(
            WorkoutSessionEntity(
                id = "active-session",
                workoutTemplateId = "template",
                plannedWorkoutId = null,
                workoutDate = today,
                startedAt = 100,
                completedAt = null,
                notes = null,
            ),
        )
        (1..4).forEach { position ->
            dao.insertExerciseLog(
                ExerciseLogEntity(
                    id = "active-log-$position",
                    workoutSessionId = "active-session",
                    exerciseId = "exercise-$position",
                    position = position - 1,
                    notes = null,
                    targetSets = 3,
                    targetReps = "8-10",
                ),
            )
        }
    }

    private object FixedSettingsRepository : AppSettingsRepository {
        override fun observeSettings(): Flow<AppSettings> = flowOf(AppSettings())
        override suspend fun updateUnits(weightUnit: WeightUnit, measurementUnit: MeasurementUnit) = Unit
        override suspend fun updateAssistantSettings(enabled: Boolean) = Unit
        override suspend fun updateRestTimerSeconds(seconds: Int) = Unit
        override suspend fun updateWorkoutReminder(enabled: Boolean, hour: Int, minute: Int) = Unit
        override suspend fun updateTransformationReminder(
            enabled: Boolean,
            dayOfWeekOrdinal: Int,
            hour: Int,
            minute: Int,
        ) = Unit
    }
}
