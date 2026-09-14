package com.keepfit.feature.assistant.data

import java.net.URI
import java.time.DayOfWeek
import java.time.LocalDate
import com.keepfit.feature.assistant.access.AssistantAccessState
import com.keepfit.feature.assistant.coaching.CoachingIntent
import com.keepfit.feature.assistant.coaching.CoachingProposal

enum class AssistantConnectionStatus {
    DISABLED,
    IDLE,
    TESTING,
    CONNECTED,
    ERROR,
}

enum class AssistantMessageRole {
    SYSTEM,
    USER,
    ASSISTANT,
}

data class AssistantRuntimeConfig(
    val baseUrl: String,
    val generalChatModelName: String,
    val reasoningModelName: String,
    val apiKey: String? = null,
)

enum class AssistantModelPurpose {
    CHAT,
    REASONING,
}

data class AssistantChatMessage(
    val id: String,
    val role: AssistantMessageRole,
    val content: String,
    val createdAtUtcEpochMillis: Long,
    val isError: Boolean = false,
)

data class AssistantUiState(
    val connectionStatus: AssistantConnectionStatus = AssistantConnectionStatus.IDLE,
    val messages: List<AssistantChatMessage> = emptyList(),
    val draftMessage: String = "",
    val pendingDraftPlan: AssistantDraftWorkoutPlan? = null,
    val isWorking: Boolean = false,
    val errorMessage: String? = null,
    val accessState: AssistantAccessState = AssistantAccessState(),
    val lastResponseUsedLocalContext: Boolean = false,
    val showPrivacyDisclosure: Boolean = false,
    val authorizationUrl: String? = null,
    val selectedCoachingIntent: CoachingIntent = CoachingIntent.WEEKLY_SUMMARY,
    val pendingCoachingProposal: CoachingProposal? = null,
    val safetyMessage: String? = null,
)

data class AssistantDraftInput(
    val goal: String,
    val notes: String? = null,
)

data class AssistantRequestError(
    val message: String,
)

data class AssistantProgressSummary(
    val title: String,
    val summary: String,
)

data class AssistantRecentWorkoutSummary(
    val workoutDate: LocalDate,
    val exerciseNames: List<String>,
)

data class AssistantRecordSummary(
    val exerciseName: String,
    val highestWeightKg: Double,
    val highestRepetitions: Int,
)

data class AssistantNutritionSnapshot(
    val date: LocalDate,
    val calories: Double,
    val proteinGrams: Double,
    val carbohydrateGrams: Double,
    val fatGrams: Double,
    val calorieGoal: Double?,
    val proteinGoalGrams: Double?,
    val carbohydrateGoalGrams: Double?,
    val fatGoalGrams: Double?,
    val hasEntries: Boolean,
)

data class AssistantTransformationCycleSnapshot(
    val startDate: LocalDate,
    val latestCaptureDate: LocalDate,
    val latestDayNumber: Int,
    val workoutsCompleted: Int,
    val averageCalories: Double?,
    val averageProteinGrams: Double?,
    val averageCarbohydrateGrams: Double?,
    val averageFatGrams: Double?,
    val weightChangeKg: Double?,
)

data class AssistantProgressSnapshot(
    val latestMeasurementDate: LocalDate?,
    val latestWeightKg: Double?,
    val heightCm: Double?,
    val bmi: Double?,
    val activeCycle: AssistantTransformationCycleSnapshot? = null,
)

data class AssistantStepsSnapshotSummary(
    val todaySteps: Long,
    val sevenDayTotal: Long,
) {
    val sevenDayAverage: Long
        get() = if (sevenDayTotal == 0L) 0L else kotlin.math.round(sevenDayTotal / 7.0).toLong()
}

data class AssistantLocalSummary(
    val generatedOn: LocalDate,
    val recentWorkouts: List<AssistantRecentWorkoutSummary>,
    val records: List<AssistantRecordSummary>,
    val nutrition: AssistantNutritionSnapshot,
    val progress: AssistantProgressSnapshot,
    val steps: AssistantStepsSnapshotSummary?,
)

data class AssistantDraftWorkoutExercise(
    val name: String,
    val targetSets: Int? = null,
    val targetReps: String? = null,
    val notes: String? = null,
)

data class AssistantDraftWorkoutDay(
    val dayOfWeek: DayOfWeek,
    val templateName: String,
    val notes: String? = null,
    val exercises: List<AssistantDraftWorkoutExercise>,
)

data class AssistantDraftWorkoutPlan(
    val name: String,
    val overview: String? = null,
    val days: List<AssistantDraftWorkoutDay>,
)

fun validateAssistantRuntimeConfig(
    baseUrl: String,
    generalChatModelName: String,
    reasoningModelName: String,
    apiKey: String?,
): Result<AssistantRuntimeConfig> {
    val normalizedBaseUrl = baseUrl.trim()
    val normalizedChatModelName = generalChatModelName.trim()
    val normalizedReasoningModelName = reasoningModelName.trim()
    val normalizedApiKey = apiKey?.trim()?.ifBlank { null }

    if (normalizedChatModelName.isEmpty()) {
        return Result.failure(IllegalArgumentException("Assistant chat model name is required."))
    }

    if (normalizedReasoningModelName.isEmpty()) {
        return Result.failure(IllegalArgumentException("Assistant reasoning model name is required."))
    }

    val uri = runCatching { URI(normalizedBaseUrl) }.getOrNull()
    val scheme = uri?.scheme?.lowercase()
    if (normalizedBaseUrl.isEmpty() || uri == null || uri.host.isNullOrBlank() || (scheme != "http" && scheme != "https")) {
        return Result.failure(
            IllegalArgumentException("Assistant endpoint must be a valid http or https URL."),
        )
    }

    if (uri.isDirectOllamaCloudEndpoint() && normalizedApiKey == null) {
        return Result.failure(IllegalArgumentException("Ollama Cloud requires an API key."))
    }

    if (
        uri.isDirectOllamaCloudEndpoint() &&
        (normalizedChatModelName.endsWith("-cloud") || normalizedReasoningModelName.endsWith("-cloud"))
    ) {
        return Result.failure(
            IllegalArgumentException("Use the direct Ollama Cloud model name without '-cloud'."),
        )
    }

    return Result.success(
        AssistantRuntimeConfig(
            baseUrl = normalizedBaseUrl,
            generalChatModelName = normalizedChatModelName,
            reasoningModelName = normalizedReasoningModelName,
            apiKey = normalizedApiKey,
        ),
    )
}

private fun URI.isDirectOllamaCloudEndpoint(): Boolean =
    host.equals("ollama.com", ignoreCase = true)

fun AssistantRuntimeConfig.modelNameFor(purpose: AssistantModelPurpose): String =
    when (purpose) {
        AssistantModelPurpose.CHAT -> generalChatModelName
        AssistantModelPurpose.REASONING -> reasoningModelName
    }
