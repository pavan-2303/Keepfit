package com.keepfit.feature.workouts.data

import android.net.Uri
import com.keepfit.feature.workouts.CompletedSetInput
import com.keepfit.feature.workouts.ExerciseInput
import com.keepfit.feature.workouts.today.TodayChangeRequest
import com.keepfit.feature.workouts.today.TodayWorkoutAction
import com.keepfit.feature.workouts.today.TodayWorkoutPreview
import com.keepfit.feature.workouts.today.TodayWorkoutState
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun observeRestTimerSeconds(): Flow<Int>
    fun observeExercises(query: String): Flow<List<Exercise>>
    fun observeTemplates(): Flow<List<WorkoutTemplate>>
    fun observeWeeklySchedule(): Flow<List<PlannedWorkout>>
    fun observeTodayPlan(): Flow<List<PlannedWorkout>>
    fun observeTodayWorkout(): Flow<TodayWorkoutState>
    fun observeActiveWorkout(): Flow<ActiveWorkout?>
    fun observeHistory(): Flow<List<WorkoutHistory>>
    fun observeRecords(): Flow<List<PersonalRecord>>

    suspend fun saveExercise(id: String?, input: ExerciseInput, mediaUri: Uri?)
    suspend fun archiveExercise(id: String)
    suspend fun deleteExercise(id: String)
    suspend fun createTemplate(name: String)
    suspend fun updateTemplate(id: String, name: String, exerciseIds: List<String>)
    suspend fun renameTemplate(id: String, name: String)
    suspend fun addTemplateExercises(id: String, exerciseIds: List<String>)
    suspend fun updateTemplateExercise(
        templateId: String,
        templateExerciseId: String,
        targetSets: Int,
        targetReps: String?,
    )
    suspend fun removeTemplateExercise(templateId: String, templateExerciseId: String)
    suspend fun deleteTemplate(id: String)
    suspend fun deleteTemplates(ids: Set<String>)
    suspend fun assignTemplate(dayOfWeek: DayOfWeek, templateId: String)
    suspend fun clearPlannedWorkout(dayOfWeek: DayOfWeek)
    suspend fun replaceWeeklySchedule(assignments: List<Pair<DayOfWeek, String>>) {
        DayOfWeek.entries.forEach { clearPlannedWorkout(it) }
        assignments.forEach { (day, templateId) -> assignTemplate(day, templateId) }
    }
    suspend fun startOrResume(plannedWorkout: PlannedWorkout): String
    suspend fun previewTodayChange(request: TodayChangeRequest): TodayWorkoutPreview
    suspend fun confirmTodayChange(request: TodayChangeRequest)
    suspend fun startOrResumeToday(action: TodayWorkoutAction): String
    suspend fun addSet(exerciseLogId: String, input: CompletedSetInput)
    suspend fun repeatPreviousSet(exerciseLogId: String)
    suspend fun substituteActiveExercise(exerciseLogId: String, replacementExerciseId: String)
    suspend fun convertActiveWorkoutToMinimum()
    suspend fun updateExerciseNotes(exerciseLogId: String, notes: String)
    suspend fun completeActiveWorkout(feedback: WorkoutFeedback? = null)
}
