package com.keepfit.feature.workouts.planning

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keepfit.core.database.KeepfitDatabase
import com.keepfit.core.database.profile.BodyProfileEntity
import com.keepfit.core.database.workout.WorkoutTemplateEntity
import com.keepfit.feature.workouts.TestActiveProfileStore
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomStarterPlanRepositoryTest {
    private lateinit var database: KeepfitDatabase
    private lateinit var repository: RoomStarterPlanRepository
    private var id = 0

    @Before
    fun createDatabase() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KeepfitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        database.bodyProfileDao().upsert(
            BodyProfileEntity("profile", "Alex", 170.0, null, createdAt = 1L, updatedAt = 1L),
        )
        repository = RoomStarterPlanRepository(
            database = database,
            activeProfileStore = TestActiveProfileStore(),
            idFactory = { "id-${++id}" },
            clock = { 10L },
            today = { LocalDate.parse("2026-09-12") },
        )
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun savesSurveyPreferencesBeforeAWeekIsApplied() = runBlocking {
        val input = StarterPlanInput(
            goal = JourneyGoal.STRENGTH,
            experienceLevel = ExperienceLevel.INTERMEDIATE,
            preferredDays = setOf(DayOfWeek.TUESDAY, DayOfWeek.SATURDAY),
            sessionMinutes = 45,
            equipment = setOf(EquipmentOption.BODYWEIGHT, EquipmentOption.DUMBBELLS),
            avoidedExerciseKeys = setOf("push-up"),
            activityLevel = ActivityLevel.MOSTLY_SEATED,
            sleepDuration = SleepDuration.SIX_TO_SEVEN_HOURS,
            sleepSchedule = SleepSchedule.SHIFT_BASED,
            currentBuild = CurrentBuild.LARGER_BUILD,
            routineChallenges = setOf(RoutineChallenge.LOW_ENERGY, RoutineChallenge.INCONSISTENT_SCHEDULE),
            limitationAreas = setOf(LimitationArea.KNEES),
            limitationNotes = "Deep knee flexion can be uncomfortable.",
        )

        repository.savePreferences(input)

        assertEquals(input, repository.loadPreferences())
        assertEquals(emptyList<Any>(), database.workoutDao().observeWeeklySchedule().first())
    }

    @Test
    fun savesPreferencesAndAppliesReviewedWeekWithoutArchivingCustomTemplates() = runBlocking {
        database.workoutDao().upsertTemplate(
            WorkoutTemplateEntity("custom", "My routine", null, 1L, 1L, null, bodyProfileId = "profile"),
        )
        val input = StarterPlanInput(
            goal = JourneyGoal.CONSISTENCY,
            experienceLevel = ExperienceLevel.BEGINNER,
            preferredDays = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
            sessionMinutes = 30,
            equipment = setOf(EquipmentOption.BODYWEIGHT),
            avoidedExerciseKeys = setOf("reverse-lunge"),
        )
        val draft = StarterWeekPlanner().createDraft(input).getOrThrow()

        repository.apply(input, draft)

        assertEquals(input, repository.loadPreferences())
        assertNotNull(database.workoutDao().findTemplate("custom"))
        assertNull(database.workoutDao().findTemplate("custom")?.archivedAt)
        val generated = database.workoutDao().findTemplatesByOrigin("STARTER_PLAN")
        assertEquals(2, generated.size)
        assertEquals(
            setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
            database.workoutDao().observeWeeklySchedule().first().map { it.dayOfWeek }.toSet(),
        )
    }

    @Test
    fun applyingAgainArchivesOnlyPreviouslyGeneratedTemplates() = runBlocking {
        val input = StarterPlanInput(
            goal = JourneyGoal.GENERAL_FITNESS,
            experienceLevel = ExperienceLevel.BEGINNER,
            preferredDays = setOf(DayOfWeek.TUESDAY),
            sessionMinutes = 15,
            equipment = setOf(EquipmentOption.BODYWEIGHT),
            avoidedExerciseKeys = emptySet(),
        )

        repository.apply(input, StarterWeekPlanner().createDraft(input).getOrThrow())
        val firstGeneratedId = database.workoutDao().findTemplatesByOrigin("STARTER_PLAN").single().id
        repository.apply(input, StarterWeekPlanner().createDraft(input).getOrThrow())

        assertNotNull(database.workoutDao().findTemplate(firstGeneratedId)?.archivedAt)
        assertEquals(1, database.workoutDao().findTemplatesByOrigin("STARTER_PLAN").count { it.archivedAt == null })
    }
}
