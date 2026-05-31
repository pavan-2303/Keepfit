package com.keepfit.feature.assistant.data

import com.google.gson.Gson

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

    val structuredMessage = runCatching {
        ollamaGson.fromJson(responseBody, OllamaChatResponse::class.java).error
    }.getOrNull()?.trim()
    if (!structuredMessage.isNullOrEmpty()) {
        return structuredMessage
    }

    return responseBody.trim().takeIf { it.isNotEmpty() }
}
