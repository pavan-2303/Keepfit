package com.keepfit.feature.workouts.today

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keepfit.core.database.KeepfitDatabase
import com.keepfit.core.database.profile.BodyProfileEntity
import com.keepfit.core.database.workout.ExerciseEntity
import com.keepfit.core.database.workout.PlannedWorkoutEntity
import com.keepfit.core.database.workout.WeeklyPlanEntity
import com.keepfit.core.database.workout.WorkoutTemplateEntity
import com.keepfit.core.database.workout.WorkoutTemplateExerciseEntity
import com.keepfit.core.media.ExerciseMediaStore
import com.keepfit.core.preferences.AppSettings
import com.keepfit.core.preferences.AppSettingsRepository
import com.keepfit.core.preferences.MeasurementUnit
import com.keepfit.core.preferences.WeightUnit
import com.keepfit.feature.workouts.data.RoomWorkoutRepository
import com.keepfit.feature.workouts.TestActiveProfileStore
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomTodayWorkoutRepositoryTest {
    private val today = LocalDate.parse("2026-09-12")
    private lateinit var database: KeepfitDatabase
    private lateinit var repository: RoomWorkoutRepository
    private lateinit var activeProfileStore: TestActiveProfileStore
    private var nextId = 0

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KeepfitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        database.bodyProfileDao().upsert(
            BodyProfileEntity("profile", "Alex", 170.0, null, createdAt = 1L, updatedAt = 1L),
        )
        activeProfileStore = TestActiveProfileStore()
        repository = RoomWorkoutRepository(
            dao = database.workoutDao(),
            mediaStore = ExerciseMediaStore(context),
            settingsRepository = FixedSettingsRepository,
            activeProfileStore = activeProfileStore,
            idFactory = { "generated-${++nextId}" },
            clock = { 100L + nextId },
            today = { today },
        )
        seedTodayPlan()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun previewDoesNotWriteAndConfirmingShortenedSnapshotPreservesTemplate() = runBlocking {
        val action = repository.observeTodayWorkout().first().primary!!
        val request = TodayChangeRequest(
            plannedWorkoutId = action.plannedWorkoutId,
            occurrenceId = null,
            originalDate = today,
            type = TodayChangeType.SHORTEN,
        )

        val preview = repository.previewTodayChange(request)

        assertEquals(4, preview.exercises.size)
        assertTrue(database.workoutDao().observeOccurrenceDetails(today, today).first().isEmpty())

        repository.confirmTodayChange(request)

        val occurrence = database.workoutDao().observeOccurrenceDetails(today, today).first().single()
        assertEquals("SHORTENED", occurrence.occurrence.decisionType)
        assertEquals(4, occurrence.exercises.size)
        assertEquals(5, database.workoutDao().findTemplateDetails("template")?.exercises?.size)
    }

    @Test
    fun switchingProfilesHidesPlansAndRejectsAnotherProfilesWorkout() = runBlocking {
        val planned = repository.observeWeeklySchedule().first().single()
        database.bodyProfileDao().upsert(
            BodyProfileEntity("profile-b", "Sam", 165.0, null, createdAt = 2L, updatedAt = 2L),
        )

        activeProfileStore.selectProfile("profile-b")

        assertTrue(repository.observeWeeklySchedule().first().isEmpty())
        assertTrue(repository.observeExercises("").first().isNotEmpty())
        assertTrue(runCatching { repository.startOrResume(planned) }.isFailure)
    }

    @Test
    fun updatingTemplatePreservesIdentityAndWeeklyAssignment() = runBlocking {
        repository.updateTemplate(
            id = "template",
            name = "Foundation edited",
            exerciseIds = listOf("exercise-3", "exercise-1"),
        )

        val updated = repository.observeTemplates().first().single()
        val planned = repository.observeWeeklySchedule().first().single()

        assertEquals("template", updated.id)
        assertEquals("Foundation edited", updated.name)
        assertEquals(listOf("exercise-3", "exercise-1"), updated.exercises.map { it.exerciseId })
        assertEquals("template", planned.templateId)
        assertEquals("Foundation edited", planned.templateName)
    }

    @Test
    fun directTemplateMaintenanceUpdatesOnlyTheRequestedFields() = runBlocking {
        val exerciseRowId = repository.observeTemplates().first().single().exercises.first().id

        repository.renameTemplate("template", "Upper pull")
        repository.updateTemplateExercise(
            templateId = "template",
            templateExerciseId = exerciseRowId,
            targetSets = 5,
            targetReps = "5-7",
        )
        repository.removeTemplateExercise("template", "template-exercise-4")

        val updated = repository.observeTemplates().first().single()
        val planned = repository.observeWeeklySchedule().first().single()

        assertEquals("Upper pull", updated.name)
        assertEquals(4, updated.exercises.size)
        assertEquals(5, updated.exercises.first().targetSets)
        assertEquals("5-7", updated.exercises.first().targetReps)
        assertEquals("Upper pull", planned.templateName)
    }

    @Test
    fun emptyTemplateCanBeCreatedButCannotBeScheduled() = runBlocking {
        repository.createTemplate("Draft day")

        val draft = repository.observeTemplates().first().single { it.name == "Draft day" }
        val assignment = runCatching { repository.assignTemplate(DayOfWeek.MONDAY, draft.id) }

        assertTrue(draft.exercises.isEmpty())
        assertTrue(assignment.isFailure)
    }

    @Test
    fun removingTheFinalExerciseKeepsTemplateAndClearsItsSchedule() = runBlocking {
        repository.observeTemplates().first().single().exercises.forEach { exercise ->
            repository.removeTemplateExercise("template", exercise.id)
        }

        val template = repository.observeTemplates().first().single { it.id == "template" }

        assertTrue(template.exercises.isEmpty())
        assertTrue(repository.observeWeeklySchedule().first().isEmpty())
    }

    @Test
    fun bulkDeleteValidatesEveryTemplateBeforeDeletingAny() = runBlocking {
        repository.createTemplate("Spare")
        val spare = repository.observeTemplates().first().single { it.name == "Spare" }
        repository.addTemplateExercises(spare.id, listOf("exercise-1"))
        repository.startOrResume(repository.observeWeeklySchedule().first().single())
        val templatesBefore = repository.observeTemplates().first()

        val result = runCatching {
            repository.deleteTemplates(templatesBefore.map { it.id }.toSet())
        }

        assertTrue(result.isFailure)
        assertEquals(
            templatesBefore.map { it.id }.toSet(),
            repository.observeTemplates().first().map { it.id }.toSet(),
        )
    }

    @Test
    fun skipIsIdempotentAndRestoreReturnsToRecurringPlan() = runBlocking {
        val action = repository.observeTodayWorkout().first().primary!!
        val skip = TodayChangeRequest(
            plannedWorkoutId = action.plannedWorkoutId,
            occurrenceId = null,
            originalDate = today,
            type = TodayChangeType.SKIP,
        )

        repository.confirmTodayChange(skip)
        repository.confirmTodayChange(skip)

        val skipped = repository.observeTodayWorkout().first()
        assertEquals(TodayWorkoutStatus.SKIPPED, skipped.status)
        assertEquals(1, database.workoutDao().observeOccurrenceDetails(today, today).first().size)

        repository.confirmTodayChange(
            skip.copy(
                occurrenceId = skipped.primary?.occurrenceId,
                type = TodayChangeType.RESTORE,
            ),
        )

        assertEquals(TodayWorkoutStatus.PLANNED, repository.observeTodayWorkout().first().status)
        assertTrue(database.workoutDao().observeOccurrenceDetails(today, today).first().isEmpty())
    }

    @Test
    fun minimumOccurrenceStartsOnceAndUsesItsSnapshot() = runBlocking {
        val action = repository.observeTodayWorkout().first().primary!!
        val request = TodayChangeRequest(
            plannedWorkoutId = action.plannedWorkoutId,
            occurrenceId = null,
            originalDate = today,
            type = TodayChangeType.MINIMUM,
        )
        repository.confirmTodayChange(request)
        val minimum = repository.observeTodayWorkout().first().primary!!

        val firstSession = repository.startOrResumeToday(minimum)
        val repeatedSession = repository.startOrResumeToday(minimum)

        assertEquals(firstSession, repeatedSession)
        val active = database.workoutDao().observeActiveSession().first()!!
        assertEquals(minimum.occurrenceId, active.session.workoutOccurrenceId)
        assertEquals(2, active.exercises.size)
    }

    @Test
    fun rescheduleRejectsDatesOutsideTheSevenDayWindow() = runBlocking {
        val action = repository.observeTodayWorkout().first().primary!!
        val request = TodayChangeRequest(
            plannedWorkoutId = action.plannedWorkoutId,
            occurrenceId = null,
            originalDate = today,
            type = TodayChangeType.RESCHEDULE,
            targetDate = today.plusDays(8),
        )

        val result = runCatching { repository.previewTodayChange(request) }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun validRescheduleMovesOnlyTheDatedOccurrence() = runBlocking {
        val action = repository.observeTodayWorkout().first().primary!!
        val request = TodayChangeRequest(
            plannedWorkoutId = action.plannedWorkoutId,
            occurrenceId = null,
            originalDate = today,
            type = TodayChangeType.RESCHEDULE,
            targetDate = today.plusDays(2),
        )

        repository.confirmTodayChange(request)

        val occurrence = database.workoutDao()
            .observeOccurrenceDetails(today, today.plusDays(2))
            .first()
            .single()
        assertEquals("RESCHEDULED", occurrence.occurrence.decisionType)
        assertEquals(today.plusDays(2), occurrence.occurrence.scheduledDate)
        assertEquals(TodayWorkoutStatus.RESCHEDULED, repository.observeTodayWorkout().first().status)
        assertEquals(today.dayOfWeek, database.workoutDao().findPlannedWorkout("planned")?.dayOfWeek)
    }

    @Test
    fun substitutionChangesOnlyTheDatedSnapshot() = runBlocking {
        val dao = database.workoutDao()
        dao.upsertExercise(
            ExerciseEntity(
                id = "replacement",
                name = "Replacement exercise",
                muscleGroup = "General",
                instructions = null,
                notes = null,
                isBodyweight = true,
                createdAt = 30L,
                updatedAt = 30L,
                archivedAt = null,
            ),
        )
        val action = repository.observeTodayWorkout().first().primary!!
        val sourceExerciseId = action.exercises.first().exerciseId
        val request = TodayChangeRequest(
            plannedWorkoutId = action.plannedWorkoutId,
            occurrenceId = null,
            originalDate = today,
            type = TodayChangeType.SUBSTITUTE,
            sourceExerciseId = sourceExerciseId,
            replacementExerciseId = "replacement",
        )

        repository.confirmTodayChange(request)

        val occurrence = dao.observeOccurrenceDetails(today, today).first().single()
        assertEquals("SUBSTITUTED", occurrence.occurrence.decisionType)
        assertTrue(occurrence.exercises.any { it.exerciseId == "replacement" })
        assertTrue(occurrence.exercises.none { it.exerciseId == sourceExerciseId })
        assertTrue(dao.findTemplateDetails("template")!!.exercises.any {
            it.templateExercise.exerciseId == sourceExerciseId
        })
    }

    @Test
    fun skippedOccurrenceCannotBeStarted() = runBlocking {
        val action = repository.observeTodayWorkout().first().primary!!
        repository.confirmTodayChange(
            TodayChangeRequest(
                plannedWorkoutId = action.plannedWorkoutId,
                occurrenceId = null,
                originalDate = today,
                type = TodayChangeType.SKIP,
            ),
        )
        val skipped = repository.observeTodayWorkout().first().primary!!

        val result = runCatching { repository.startOrResumeToday(skipped) }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals(null, database.workoutDao().findActiveSession())
    }

    private suspend fun seedTodayPlan() {
        val dao = database.workoutDao()
        val exercises = (1..5).map { position ->
            ExerciseEntity(
                id = "exercise-$position",
                name = "Exercise $position",
                muscleGroup = "General",
                instructions = null,
                notes = null,
                isBodyweight = true,
                createdAt = position.toLong(),
                updatedAt = position.toLong(),
                archivedAt = null,
            )
        }
        exercises.forEach { dao.upsertExercise(it) }
        dao.upsertTemplate(
            WorkoutTemplateEntity("template", "Foundation A", null, 10L, 10L, null, bodyProfileId = "profile"),
        )
        dao.replaceTemplateExercises(
            "template",
            exercises.mapIndexed { index, exercise ->
                WorkoutTemplateExerciseEntity(
                    id = "template-exercise-$index",
                    workoutTemplateId = "template",
                    exerciseId = exercise.id,
                    position = index,
                    targetSets = 3,
                    targetReps = "8-10",
                    notes = null,
                )
            },
        )
        dao.upsertWeeklyPlan(
            WeeklyPlanEntity(
                id = "plan",
                name = "Starter week",
                startsOn = today.with(DayOfWeek.MONDAY),
                isActive = true,
                createdAt = 20L,
                updatedAt = 20L,
                bodyProfileId = "profile",
            ),
        )
        dao.upsertPlannedWorkout(
            PlannedWorkoutEntity(
                id = "planned",
                weeklyPlanId = "plan",
                workoutTemplateId = "template",
                dayOfWeek = today.dayOfWeek,
                position = 0,
            ),
        )
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
