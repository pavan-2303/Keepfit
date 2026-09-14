package com.keepfit.feature.review

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keepfit.core.database.KeepfitDatabase
import com.keepfit.core.database.workout.ExerciseEntity
import com.keepfit.core.database.nutrition.MealQuality
import com.keepfit.core.database.nutrition.MealQualityCheckInEntity
import com.keepfit.core.database.nutrition.MealType
import com.keepfit.core.database.workout.PlannedWorkoutEntity
import com.keepfit.core.database.workout.WeeklyPlanEntity
import com.keepfit.core.database.workout.WorkoutTemplateEntity
import com.keepfit.core.database.workout.WorkoutTemplateExerciseEntity
import com.keepfit.core.preferences.AppSettings
import com.keepfit.core.preferences.AppSettingsRepository
import com.keepfit.core.preferences.MeasurementUnit
import com.keepfit.core.preferences.NutritionTrackingDepth
import com.keepfit.core.preferences.WeightUnit
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomWeeklyReviewRepositoryTest {
    private lateinit var database: KeepfitDatabase
    private lateinit var settings: FakeSettingsRepository
    private lateinit var repository: RoomWeeklyReviewRepository

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KeepfitDatabase::class.java).build()
        settings = FakeSettingsRepository()
        repository = RoomWeeklyReviewRepository(
            database = database,
            workoutDao = database.workoutDao(),
            nutritionDao = database.nutritionDao(),
            settingsRepository = settings,
            activityProvider = object : WeeklyActivityProvider {
                override suspend fun loadSteps(window: WeeklyReviewWindow) = null
            },
            today = { LocalDate.of(2026, 9, 12) },
            clock = { 1_000L },
            idFactory = IdFactory(),
        )
        seedPlan()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun previewDoesNotMutatePlanTemplateOrOccurrences() = runBlocking {
        val snapshot = repository.load() as WeeklyReviewSnapshot.Ready

        assertEquals(3, snapshot.result.plannedCount)
        assertEquals(2, snapshot.result.drafts.size)
        assertNull(database.weeklyReviewDao().findForWeek(LocalDate.of(2026, 8, 31)))
        assertNull(database.workoutDao().findOccurrenceForSource("planned-monday", LocalDate.of(2026, 9, 14)))
        assertEquals(3, database.workoutDao().findTemplateDetails("template")?.exercises?.single()?.templateExercise?.targetSets)
    }

    @Test
    fun approvalCreatesOneDatedOccurrenceAndIsIdempotent() = runBlocking {
        val draft = (repository.load() as WeeklyReviewSnapshot.Ready).result.drafts.first()

        repository.approve(draft)
        repository.approve(draft)

        val occurrence = database.workoutDao().findOccurrenceForSource("planned-monday", LocalDate.of(2026, 9, 14))
        assertNotNull(occurrence)
        assertEquals("MINIMUM", occurrence?.decisionType)
        val details = database.workoutDao().findOccurrenceDetails(requireNotNull(occurrence).id)
        assertEquals(2, details?.exercises?.single()?.targetSets)
        assertEquals("APPROVED", database.weeklyReviewDao().findForWeek(LocalDate.of(2026, 8, 31))?.status)
        assertEquals(3, database.workoutDao().findTemplateDetails("template")?.exercises?.single()?.templateExercise?.targetSets)
    }

    @Test
    fun editedMoveApprovalUsesTheChosenFreeDay() = runBlocking {
        val moveDraft = (repository.load() as WeeklyReviewSnapshot.Ready).result.drafts
            .first { it.type == ReviewDraftType.MOVE_SESSION }
            .copy(targetDate = LocalDate.of(2026, 9, 15))

        repository.approve(moveDraft)

        val occurrence = database.workoutDao().findOccurrenceForSource(
            moveDraft.sourcePlannedWorkoutId,
            moveDraft.sourceDate,
        )
        assertEquals(LocalDate.of(2026, 9, 15), occurrence?.scheduledDate)
        assertEquals("MOVE_SESSION", database.weeklyReviewDao().findForWeek(LocalDate.of(2026, 8, 31))?.draftType)
    }

    @Test
    fun dismissalRecordsDecisionWithoutCreatingOccurrence() = runBlocking {
        repository.dismiss()
        repository.dismiss()

        assertEquals("DISMISSED", database.weeklyReviewDao().findForWeek(LocalDate.of(2026, 8, 31))?.status)
        assertNull(database.workoutDao().findOccurrenceForSource("planned-monday", LocalDate.of(2026, 9, 14)))
    }

    @Test
    fun pauseAndResumeAreReturnedAsExplicitStates() = runBlocking {
        repository.setPaused(true)
        assertEquals(WeeklyReviewSnapshot.Paused, repository.load())

        repository.setPaused(false)
        assertEquals(true, repository.load() is WeeklyReviewSnapshot.Ready)
    }

    @Test
    fun mealQualityModeUsesCheckInsAndDisabledModeOmitsThem() = runBlocking {
        database.nutritionDao().upsertMealQualityCheckIn(
            MealQualityCheckInEntity(
                id = "quality",
                diaryDate = LocalDate.of(2026, 9, 2),
                mealType = MealType.LUNCH,
                quality = MealQuality.BALANCED,
                loggedAt = 1L,
            ),
        )
        settings.updateNutritionTracking(NutritionTrackingDepth.MEAL_QUALITY, 10)
        val qualityResult = (repository.load() as WeeklyReviewSnapshot.Ready).result
        assertEquals(true, qualityResult.supportingSignals.single().startsWith("Meal check-ins"))

        settings.updateNutritionTracking(NutritionTrackingDepth.DISABLED, 10)
        val disabledResult = (repository.load() as WeeklyReviewSnapshot.Ready).result
        assertEquals(emptyList<String>(), disabledResult.supportingSignals)
    }

    private suspend fun seedPlan() {
        val dao = database.workoutDao()
        dao.upsertExercise(
            ExerciseEntity("exercise", "Goblet squat", "Legs", null, null, false, 1, 1, null),
        )
        dao.upsertTemplate(WorkoutTemplateEntity("template", "Foundation", null, 1, 1, null))
        dao.insertTemplateExercises(
            listOf(WorkoutTemplateExerciseEntity("template-exercise", "template", "exercise", 0, 3, "8-10", null)),
        )
        dao.upsertWeeklyPlan(WeeklyPlanEntity("plan", "My week", LocalDate.of(2026, 8, 1), true, 1, 1))
        listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY).forEach { day ->
            dao.upsertPlannedWorkout(
                PlannedWorkoutEntity("planned-${day.name.lowercase()}", "plan", "template", day, day.value),
            )
        }
    }
}

private class IdFactory : () -> String {
    private var value = 0
    override fun invoke(): String = "id-${value++}"
}

private class FakeSettingsRepository : AppSettingsRepository {
    private val state = MutableStateFlow(AppSettings())
    override fun observeSettings(): Flow<AppSettings> = state
    override suspend fun updateWeeklyReviewPaused(paused: Boolean) {
        state.value = state.value.copy(weeklyReviewPaused = paused)
    }
    override suspend fun updateUnits(weightUnit: WeightUnit, measurementUnit: MeasurementUnit) = Unit
    override suspend fun updateAssistantSettings(enabled: Boolean) = Unit
    override suspend fun updateRestTimerSeconds(seconds: Int) = Unit
    override suspend fun updateNutritionTracking(depth: NutritionTrackingDepth, targetRangePercent: Int) {
        state.value = state.value.copy(
            nutritionTrackingDepth = depth,
            nutritionTargetRangePercent = targetRangePercent,
        )
    }
    override suspend fun updateWorkoutReminder(enabled: Boolean, hour: Int, minute: Int) = Unit
    override suspend fun updateTransformationReminder(enabled: Boolean, dayOfWeekOrdinal: Int, hour: Int, minute: Int) = Unit
}
