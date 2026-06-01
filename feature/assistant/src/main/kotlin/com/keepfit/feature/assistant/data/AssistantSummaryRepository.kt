package com.keepfit.feature.assistant.data

import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

interface AssistantSummaryDataSource {
    suspend fun readRecentWorkouts(): List<AssistantRecentWorkoutSummary>

    suspend fun readRecords(): List<AssistantRecordSummary>

    suspend fun readNutritionSnapshot(onDate: LocalDate): AssistantNutritionSnapshot

    suspend fun readProgressSnapshot(): AssistantProgressSnapshot

    suspend fun readStepsSnapshot(): AssistantStepsSnapshotSummary?
}

@Singleton
class AssistantSummaryRepository @Inject constructor(
    private val dataSource: AssistantSummaryDataSource,
    private val clock: Clock,
) {
    suspend fun loadProgressSummary(): AssistantLocalSummary {
        val generatedOn = LocalDate.now(clock)
        return AssistantLocalSummary(
            generatedOn = generatedOn,
            recentWorkouts = dataSource.readRecentWorkouts(),
            records = dataSource.readRecords(),
            nutrition = dataSource.readNutritionSnapshot(generatedOn),
            progress = dataSource.readProgressSnapshot(),
            steps = dataSource.readStepsSnapshot(),
        )
    }
}
