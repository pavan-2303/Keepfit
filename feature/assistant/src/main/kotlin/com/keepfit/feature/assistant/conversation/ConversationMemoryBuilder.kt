package com.keepfit.feature.assistant.conversation

import com.keepfit.feature.assistant.data.AssistantChatMessage
import com.keepfit.feature.assistant.data.AssistantMessageRole

data class ConversationMemory(
    val summary: String?,
    val recentMessages: List<AssistantChatMessage>,
)

object ConversationMemoryBuilder {
    const val RECENT_MESSAGE_LIMIT = 12
    const val MAX_SUMMARY_CHARACTERS = 1_800
    private const val MAX_MESSAGE_EXCERPT_CHARACTERS = 220

    fun build(
        messages: List<AssistantChatMessage>,
        memoryClearedAtUtcEpochMillis: Long? = null,
    ): ConversationMemory {
        val remembered = messages
            .asSequence()
            .filter { message ->
                memoryClearedAtUtcEpochMillis == null ||
                    message.createdAtUtcEpochMillis > memoryClearedAtUtcEpochMillis
            }
            .sortedWith(compareBy(AssistantChatMessage::createdAtUtcEpochMillis, AssistantChatMessage::id))
            .toList()
        val recent = remembered.takeLast(RECENT_MESSAGE_LIMIT)
        val earlier = remembered.dropLast(recent.size)
        val summary = earlier
            .joinToString(separator = "\n") { message ->
                val speaker = when (message.role) {
                    AssistantMessageRole.USER -> "User"
                    AssistantMessageRole.ASSISTANT -> "Coach"
                    AssistantMessageRole.SYSTEM -> "System"
                }
                "$speaker: ${message.content.normalizedExcerpt()}"
            }
            .takeLast(MAX_SUMMARY_CHARACTERS)
            .ifBlank { null }
        return ConversationMemory(summary = summary, recentMessages = recent)
    }

    private fun String.normalizedExcerpt(): String =
        trim().replace(WHITESPACE, " ").take(MAX_MESSAGE_EXCERPT_CHARACTERS)

    private val WHITESPACE = Regex("\\s+")
}
