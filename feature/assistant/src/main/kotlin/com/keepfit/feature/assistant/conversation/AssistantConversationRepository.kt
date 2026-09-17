package com.keepfit.feature.assistant.conversation

import com.keepfit.feature.assistant.data.AssistantChatMessage
import kotlinx.coroutines.flow.Flow

data class AssistantConversation(
    val id: String,
    val coach: CoachPersona,
    val title: String,
    val createdAtUtcEpochMillis: Long,
    val updatedAtUtcEpochMillis: Long,
)

interface AssistantConversationRepository {
    fun observeConversations(): Flow<List<AssistantConversation>>
    fun observeMessages(conversationId: String): Flow<List<AssistantChatMessage>>
    suspend fun createConversation(coach: CoachPersona): AssistantConversation
    suspend fun loadRequestHistory(conversationId: String): Result<List<AssistantChatMessage>>
    suspend fun appendCompletedTurn(
        conversationId: String,
        userMessage: AssistantChatMessage,
        assistantMessage: AssistantChatMessage,
    ): Result<Unit>
    suspend fun renameConversation(conversationId: String, title: String): Result<Unit>
    suspend fun clearMemory(conversationId: String): Result<Unit>
    suspend fun deleteConversation(conversationId: String): Result<Unit>
}
