package com.keepfit.feature.assistant

import com.keepfit.feature.assistant.data.AssistantNutritionSnapshot
import com.keepfit.feature.assistant.data.AssistantProgressSnapshot
import com.keepfit.feature.assistant.data.AssistantPromptAssembler
import com.keepfit.feature.assistant.data.AssistantRecordSummary
import com.keepfit.feature.assistant.data.AssistantRecentWorkoutSummary
import com.keepfit.feature.assistant.data.AssistantStepsSnapshotSummary
import com.keepfit.feature.assistant.data.AssistantSummaryDataSource
import com.keepfit.feature.assistant.data.AssistantSummaryRepository
import com.keepfit.feature.assistant.data.AssistantTransformationCycleSnapshot
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantPromptAssemblerTest {
    @Test
    fun buildsDeterministicPromptFromLocalFitnessData() = runBlocking {
        val repository = AssistantSummaryRepository(
            dataSource = FakeSummaryDataSource(),
            clock = fixedClock,
        )

        val summary = repository.loadProgressSummary()
        val prompt = AssistantPromptAssembler().buildProgressSummaryPrompt(summary)

        assertEquals(LocalDate.of(2026, 6, 1), summary.generatedOn)
        assertTrue(prompt.contains("Recent workouts"))
        assertTrue(prompt.contains("2026-05-31: Bench Press, Barbell Row"))
        assertTrue(prompt.contains("Personal records"))
        assertTrue(prompt.contains("Bench Press: 100 kg x5"))
        assertTrue(prompt.contains("Nutrition today (2026-06-01)"))
        assertTrue(prompt.contains("2200 kcal"))
        assertTrue(prompt.contains("180 g protein goal"))
        assertTrue(prompt.contains("Latest weight: 81.2 kg on 2026-05-31"))
        assertTrue(prompt.contains("BMI: 26.5"))
        assertTrue(prompt.contains("Active transformation cycle"))
        assertTrue(prompt.contains("Today steps: 8342"))
        assertTrue(prompt.contains("7-day average: 8014"))
    }

    @Test
    fun omitsStepsSectionWhenNoStepsSnapshotIsAvailable() = runBlocking {
        val repository = AssistantSummaryRepository(
            dataSource = FakeSummaryDataSource(steps = null),
            clock = fixedClock,
        )

        val prompt = AssistantPromptAssembler().buildProgressSummaryPrompt(
            repository.loadProgressSummary(),
        )

        assertFalse(prompt.contains("Today steps:"))
        assertFalse(prompt.contains("7-day average:"))
    }

    private class FakeSummaryDataSource(
        private val steps: AssistantStepsSnapshotSummary? = AssistantStepsSnapshotSummary(
            todaySteps = 8_342,
            sevenDayTotal = 56_098,
        ),
    ) : AssistantSummaryDataSource {
        override suspend fun readRecentWorkouts(): List<AssistantRecentWorkoutSummary> = listOf(
            AssistantRecentWorkoutSummary(
                workoutDate = LocalDate.of(2026, 5, 31),
                exerciseNames = listOf("Bench Press", "Barbell Row"),
            ),
            AssistantRecentWorkoutSummary(
                workoutDate = LocalDate.of(2026, 5, 29),
                exerciseNames = listOf("Squat", "Romanian Deadlift"),
            ),
        )

        override suspend fun readRecords(): List<AssistantRecordSummary> = listOf(
            AssistantRecordSummary(
                exerciseName = "Bench Press",
                highestWeightKg = 100.0,
                highestRepetitions = 5,
            ),
        )

        override suspend fun readNutritionSnapshot(onDate: LocalDate): AssistantNutritionSnapshot =
            AssistantNutritionSnapshot(
                date = onDate,
                calories = 2_200.0,
                proteinGrams = 170.0,
                carbohydrateGrams = 210.0,
                fatGrams = 70.0,
                calorieGoal = 2_400.0,
                proteinGoalGrams = 180.0,
                carbohydrateGoalGrams = 240.0,
                fatGoalGrams = 75.0,
                hasEntries = true,
            )

        override suspend fun readProgressSnapshot(): AssistantProgressSnapshot = AssistantProgressSnapshot(
            latestMeasurementDate = LocalDate.of(2026, 5, 31),
            latestWeightKg = 81.2,
            heightCm = 175.0,
            bmi = 26.5,
            activeCycle = AssistantTransformationCycleSnapshot(
                startDate = LocalDate.of(2026, 5, 1),
                latestCaptureDate = LocalDate.of(2026, 5, 31),
                latestDayNumber = 30,
                workoutsCompleted = 12,
                averageCalories = 2_180.0,
                averageProteinGrams = 168.0,
                averageCarbohydrateGrams = 208.0,
                averageFatGrams = 68.0,
                weightChangeKg = -2.4,
            ),
        )

        override suspend fun readStepsSnapshot(): AssistantStepsSnapshotSummary? = steps
    }

    private companion object {
        val fixedClock: Clock = Clock.fixed(
            Instant.parse("2026-06-01T06:00:00Z"),
            ZoneOffset.UTC,
        )
    }
}
