package com.keepfit.feature.assistant.data

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.time.DayOfWeek

data class OllamaChatRequest(
    val model: String,
    val messages: List<OllamaChatMessageDto>,
    val stream: Boolean = false,
)

data class OllamaChatMessageDto(
    val role: String,
    val content: String,
)

data class OllamaChatResponse(
    val message: OllamaChatMessageDto? = null,
    val error: String? = null,
)

private data class OllamaDraftPlanResponse(
    val plan_name: String? = null,
    val overview: String? = null,
    val days: List<OllamaDraftPlanDayDto>? = null,
)

private data class OllamaDraftPlanDayDto(
    val day_of_week: String? = null,
    val template_name: String? = null,
    val notes: String? = null,
    val exercises: List<OllamaDraftPlanExerciseDto>? = null,
)

private data class OllamaDraftPlanExerciseDto(
    val name: String? = null,
    val target_sets: Int? = null,
    val target_reps: String? = null,
    val notes: String? = null,
)

private val ollamaGson = Gson()

internal fun assistantMessageToOllamaDto(message: AssistantChatMessage): OllamaChatMessageDto =
    OllamaChatMessageDto(
        role = when (message.role) {
            AssistantMessageRole.SYSTEM -> "system"
            AssistantMessageRole.USER -> "user"
            AssistantMessageRole.ASSISTANT -> "assistant"
        },
        content = message.content,
    )

fun parseOllamaChatResponse(
    json: String,
    createdAtUtcEpochMillis: Long,
): Result<AssistantChatMessage> {
    val response = runCatching {
        ollamaGson.fromJson(json, OllamaChatResponse::class.java)
    }.getOrElse {
        return Result.failure(IllegalStateException("Assistant response could not be parsed."))
    }

    val message = response.message
    val content = message?.content?.trim().orEmpty()
    if (content.isEmpty()) {
        return Result.failure(IllegalStateException("Assistant response did not include a message."))
    }

    val role = when (message?.role?.lowercase()) {
        "system" -> AssistantMessageRole.SYSTEM
        "user" -> AssistantMessageRole.USER
        else -> AssistantMessageRole.ASSISTANT
    }

    return Result.success(
        AssistantChatMessage(
            id = "assistant-$createdAtUtcEpochMillis",
            role = role,
            content = content,
            createdAtUtcEpochMillis = createdAtUtcEpochMillis,
        ),
    )
}

internal fun parseOllamaErrorMessage(responseBody: String?): String? {
    if (responseBody.isNullOrBlank()) return null
    val trimmedBody = responseBody.trim()
    if (trimmedBody.startsWith("<!doctype", ignoreCase = true) || trimmedBody.startsWith("<html", ignoreCase = true)) {
        return null
    }

    val structuredMessage = runCatching {
        ollamaGson.fromJson(trimmedBody, OllamaChatResponse::class.java).error
    }.getOrNull()?.trim()
    if (!structuredMessage.isNullOrEmpty()) {
        return structuredMessage
    }

    return trimmedBody.takeIf { it.isNotEmpty() }
}

fun parseOllamaDraftPlanResponse(json: String): Result<AssistantDraftWorkoutPlan> {
    val normalizedJson = stripMarkdownCodeFence(json)
    val response = try {
        ollamaGson.fromJson(normalizedJson, OllamaDraftPlanResponse::class.java)
    } catch (_: JsonSyntaxException) {
        return Result.failure(IllegalStateException("Assistant draft plan response was incomplete or malformed."))
    }

    val planName = response.plan_name?.trim().orEmpty()
    val days = response.days.orEmpty()
    if (planName.isEmpty() || days.isEmpty()) {
        return Result.failure(IllegalStateException("Assistant draft plan response was incomplete or malformed."))
    }

    val parsedDays = days.mapNotNull { day ->
        val dayOfWeek = runCatching { DayOfWeek.valueOf(day.day_of_week.orEmpty().trim().uppercase()) }.getOrNull()
        val templateName = day.template_name?.trim().orEmpty()
        val exercises = day.exercises.orEmpty().mapNotNull { exercise ->
            val exerciseName = exercise.name?.trim().orEmpty()
            if (exerciseName.isEmpty()) {
                null
            } else {
                AssistantDraftWorkoutExercise(
                    name = exerciseName,
                    targetSets = exercise.target_sets,
                    targetReps = exercise.target_reps?.trim()?.ifEmpty { null },
                    notes = exercise.notes?.trim()?.ifEmpty { null },
                )
            }
        }
        if (dayOfWeek == null || templateName.isEmpty() || exercises.isEmpty()) {
            null
        } else {
            AssistantDraftWorkoutDay(
                dayOfWeek = dayOfWeek,
                templateName = templateName,
                notes = day.notes?.trim()?.ifEmpty { null },
                exercises = exercises,
            )
        }
    }

    if (parsedDays.size != days.size) {
        return Result.failure(IllegalStateException("Assistant draft plan response was incomplete or malformed."))
    }

    return Result.success(
        AssistantDraftWorkoutPlan(
            name = planName,
            overview = response.overview?.trim()?.ifEmpty { null },
            days = parsedDays,
        ),
    )
}

private fun stripMarkdownCodeFence(json: String): String {
    val trimmed = json.trim()
    if (!trimmed.startsWith("```")) {
        return trimmed
    }

    return trimmed
        .removePrefix("```json")
        .removePrefix("```JSON")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()
}
