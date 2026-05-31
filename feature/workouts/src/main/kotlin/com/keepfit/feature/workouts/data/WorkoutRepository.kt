package com.keepfit.feature.workouts.data

import android.net.Uri
import com.keepfit.feature.workouts.CompletedSetInput
import com.keepfit.feature.workouts.ExerciseInput
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun observeRestTimerSeconds(): Flow<Int>
    fun observeExercises(query: String): Flow<List<Exercise>>
    fun observeTemplates(): Flow<List<WorkoutTemplate>>
    fun observeWeeklySchedule(): Flow<List<PlannedWorkout>>
    fun observeTodayPlan(): Flow<List<PlannedWorkout>>
    fun observeActiveWorkout(): Flow<ActiveWorkout?>
    fun observeHistory(): Flow<List<WorkoutHistory>>
    fun observeRecords(): Flow<List<PersonalRecord>>

    suspend fun saveExercise(id: String?, input: ExerciseInput, mediaUri: Uri?)
    suspend fun archiveExercise(id: String)
    suspend fun deleteExercise(id: String)
    suspend fun createTemplate(name: String, exerciseIds: List<String>)
    suspend fun deleteTemplate(id: String)
    suspend fun assignTemplate(dayOfWeek: DayOfWeek, templateId: String)
    suspend fun clearPlannedWorkout(dayOfWeek: DayOfWeek)
    suspend fun startOrResume(plannedWorkout: PlannedWorkout): String
    suspend fun addSet(exerciseLogId: String, input: CompletedSetInput)
    suspend fun updateExerciseNotes(exerciseLogId: String, notes: String)
    suspend fun completeActiveWorkout()
}
