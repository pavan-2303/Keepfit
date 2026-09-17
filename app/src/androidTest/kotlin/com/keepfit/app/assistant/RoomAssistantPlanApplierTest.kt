package com.keepfit.app.assistant

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.keepfit.core.database.KeepfitDatabase
import com.keepfit.core.database.profile.BodyProfileEntity
import com.keepfit.core.database.workout.ExerciseEntity
import com.keepfit.core.database.workout.PlannedWorkoutEntity
import com.keepfit.core.database.workout.WeeklyPlanEntity
import com.keepfit.core.database.workout.WorkoutTemplateEntity
import com.keepfit.core.preferences.ActiveProfileStore
import com.keepfit.feature.assistant.data.AssistantDraftWorkoutDay
import com.keepfit.feature.assistant.data.AssistantDraftWorkoutExercise
import com.keepfit.feature.assistant.data.AssistantDraftWorkoutPlan
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomAssistantPlanApplierTest {
    private lateinit var database: KeepfitDatabase
    private lateinit var applier: RoomAssistantPlanApplier
    private var id = 0

    @Before
    fun setUp() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext as Context
        database = Room.inMemoryDatabaseBuilder(context, KeepfitDatabase::class.java).allowMainThreadQueries().build()
        listOf("profile", "other").forEach { profileId ->
            database.bodyProfileDao().upsert(BodyProfileEntity(profileId, profileId, 170.0, null, createdAt = 1, updatedAt = 1))
            seedSchedule(profileId)
        }
        database.workoutDao().upsertExercise(
            ExerciseEntity(
                "catalogue-exercise", "Goblet squat", "Legs", null, null, false, 1, 1, null,
            ),
        )
        applier = RoomAssistantPlanApplier(
            database = database,
            activeProfileStore = FakeActiveProfileStore("profile"),
            idFactory = { "new-${++id}" },
            clock = { 20L },
            today = { LocalDate.parse("2026-09-16") },
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun invalidCatalogueReferenceLeavesExistingPlanUntouched() = runBlocking {
        val result = applier.applyDraftPlan(draft("missing"))

        assertTrue(result.isFailure)
        assertEquals("old-template-profile", database.workoutDao().observeWeeklyScheduleForProfile("profile").first().single().workoutTemplateId)
    }

    @Test
    fun approvalReplacesOnlyActiveProfileAndKeepsExactTargets() = runBlocking {
        applier.applyDraftPlan(draft(PersonalExerciseAlias.forId("catalogue-exercise"))).getOrThrow()

        val schedule = database.workoutDao().observeWeeklyScheduleForProfile("profile").first()
        assertEquals(DayOfWeek.WEDNESDAY, schedule.single().dayOfWeek)
        val template = database.workoutDao().findTemplateDetailsForProfile(schedule.single().workoutTemplateId, "profile")!!
        assertEquals(3, template.exercises.single().templateExercise.targetSets)
        assertEquals("8-12", template.exercises.single().templateExercise.targetReps)
        assertEquals("old-template-other", database.workoutDao().observeWeeklyScheduleForProfile("other").first().single().workoutTemplateId)
    }

    private suspend fun seedSchedule(profileId: String) {
        val templateId = "old-template-$profileId"
        val planId = "old-plan-$profileId"
        database.workoutDao().upsertTemplate(WorkoutTemplateEntity(templateId, "Old", null, 1, 1, null, bodyProfileId = profileId))
        database.workoutDao().upsertWeeklyPlan(WeeklyPlanEntity(planId, "Old", LocalDate.parse("2026-09-14"), true, 1, 1, profileId))
        database.workoutDao().upsertPlannedWorkout(PlannedWorkoutEntity("old-workout-$profileId", planId, templateId, DayOfWeek.MONDAY, 0))
    }

    private fun draft(exerciseId: String) = AssistantDraftWorkoutPlan(
        name = "Strong start",
        days = listOf(
            AssistantDraftWorkoutDay(
                dayOfWeek = DayOfWeek.WEDNESDAY,
                templateName = "Full body",
                exercises = listOf(AssistantDraftWorkoutExercise(exerciseId, "Goblet squat", 3, "8-12")),
            ),
        ),
    )

    private class FakeActiveProfileStore(profileId: String) : ActiveProfileStore {
        private val selected = MutableStateFlow<String?>(profileId)
        override fun observeActiveProfileId(): Flow<String?> = selected
        override suspend fun selectProfile(profileId: String) { selected.value = profileId }
        override suspend fun clearSelection() { selected.value = null }
    }
}
