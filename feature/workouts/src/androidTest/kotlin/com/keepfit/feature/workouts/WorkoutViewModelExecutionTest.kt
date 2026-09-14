package com.keepfit.feature.workouts

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keepfit.feature.workouts.data.ActiveWorkout
import com.keepfit.feature.workouts.data.Exercise
import com.keepfit.feature.workouts.data.PersonalRecord
import com.keepfit.feature.workouts.data.PlannedWorkout
import com.keepfit.feature.workouts.data.WorkoutFeedback
import com.keepfit.feature.workouts.data.WorkoutHistory
import com.keepfit.feature.workouts.data.WorkoutRepository
import com.keepfit.feature.workouts.data.WorkoutTemplate
import com.keepfit.feature.workouts.today.TodayChangeRequest
import com.keepfit.feature.workouts.today.TodayWorkoutAction
import com.keepfit.feature.workouts.today.TodayWorkoutPreview
import com.keepfit.feature.workouts.today.TodayWorkoutState
import java.time.DayOfWeek
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutViewModelExecutionTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun successfulSetStartsTimerButInvalidInputDoesNotWrite() = runBlocking {
        val repository = FakeWorkoutRepository()
        val viewModel = WorkoutViewModel(repository, SavedStateHandle(), context)

        viewModel.addSet("log-1", "10", "20")
        withTimeout(2_000) {
            while (viewModel.timerSeconds.value == null) delay(10)
        }
        assertEquals(1, repository.addSetCalls)
        assertEquals(1, viewModel.timerSeconds.value)

        val invalidRepository = FakeWorkoutRepository()
        val invalidViewModel = WorkoutViewModel(invalidRepository, SavedStateHandle(), context)
        invalidViewModel.addSet("log-1", "", "20")
        delay(100)
        assertEquals(0, invalidRepository.addSetCalls)
        assertNull(invalidViewModel.timerSeconds.value)
    }

    @Test
    fun savedTimerDeadlineResumesAfterViewModelRecreation() = runBlocking {
        val state = SavedStateHandle(
            mapOf("active_workout_rest_timer_end_at" to System.currentTimeMillis() + 2_000L),
        )

        val viewModel = WorkoutViewModel(FakeWorkoutRepository(), state, context)

        withTimeout(1_000) {
            while (viewModel.timerSeconds.value == null) delay(10)
        }
        assertNotNull(viewModel.timerSeconds.value)
    }

    private class FakeWorkoutRepository : WorkoutRepository {
        var addSetCalls = 0

        override fun observeRestTimerSeconds(): Flow<Int> = flowOf(1)
        override fun observeExercises(query: String): Flow<List<Exercise>> = flowOf(emptyList())
        override fun observeTemplates(): Flow<List<WorkoutTemplate>> = flowOf(emptyList())
        override fun observeWeeklySchedule(): Flow<List<PlannedWorkout>> = flowOf(emptyList())
        override fun observeTodayPlan(): Flow<List<PlannedWorkout>> = flowOf(emptyList())
        override fun observeTodayWorkout(): Flow<TodayWorkoutState> = flowOf(TodayWorkoutState())
        override fun observeActiveWorkout(): Flow<ActiveWorkout?> = flowOf(null)
        override fun observeHistory(): Flow<List<WorkoutHistory>> = flowOf(emptyList())
        override fun observeRecords(): Flow<List<PersonalRecord>> = flowOf(emptyList())

        override suspend fun saveExercise(id: String?, input: ExerciseInput, mediaUri: Uri?) = Unit
        override suspend fun archiveExercise(id: String) = Unit
        override suspend fun deleteExercise(id: String) = Unit
        override suspend fun createTemplate(name: String, exerciseIds: List<String>) = Unit
        override suspend fun deleteTemplate(id: String) = Unit
        override suspend fun assignTemplate(dayOfWeek: DayOfWeek, templateId: String) = Unit
        override suspend fun clearPlannedWorkout(dayOfWeek: DayOfWeek) = Unit
        override suspend fun startOrResume(plannedWorkout: PlannedWorkout): String = "session"
        override suspend fun previewTodayChange(request: TodayChangeRequest): TodayWorkoutPreview =
            error("Not used")
        override suspend fun confirmTodayChange(request: TodayChangeRequest) = Unit
        override suspend fun startOrResumeToday(action: TodayWorkoutAction): String = "session"
        override suspend fun addSet(exerciseLogId: String, input: CompletedSetInput) {
            addSetCalls++
        }
        override suspend fun repeatPreviousSet(exerciseLogId: String) = Unit
        override suspend fun substituteActiveExercise(
            exerciseLogId: String,
            replacementExerciseId: String,
        ) = Unit
        override suspend fun convertActiveWorkoutToMinimum() = Unit
        override suspend fun updateExerciseNotes(exerciseLogId: String, notes: String) = Unit
        override suspend fun completeActiveWorkout(feedback: WorkoutFeedback?) = Unit
    }
}
