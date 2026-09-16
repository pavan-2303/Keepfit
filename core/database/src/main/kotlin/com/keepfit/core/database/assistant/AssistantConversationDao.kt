package com.keepfit.core.database.assistant

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AssistantConversationDao {
    @Query(
        """
        SELECT * FROM assistant_conversations
        WHERE bodyProfileId = :profileId
        ORDER BY updatedAt DESC, id DESC
        """,
    )
    abstract fun observeConversations(profileId: String): Flow<List<AssistantConversationEntity>>

    @Query(
        """
        SELECT message.* FROM assistant_messages AS message
        INNER JOIN assistant_conversations AS conversation
            ON conversation.id = message.assistantConversationId
        WHERE conversation.bodyProfileId = :profileId
            AND conversation.id = :conversationId
        ORDER BY message.createdAt, message.id
        """,
    )
    abstract fun observeMessages(profileId: String, conversationId: String): Flow<List<AssistantMessageEntity>>

    @Query(
        """
        SELECT * FROM assistant_conversations
        WHERE bodyProfileId = :profileId AND id = :conversationId
        LIMIT 1
        """,
    )
    abstract suspend fun findOwnedConversation(
        profileId: String,
        conversationId: String,
    ): AssistantConversationEntity?

    @Query(
        """
        SELECT message.* FROM assistant_messages AS message
        INNER JOIN assistant_conversations AS conversation
            ON conversation.id = message.assistantConversationId
        WHERE conversation.bodyProfileId = :profileId
            AND conversation.id = :conversationId
        ORDER BY message.createdAt, message.id
        """,
    )
    abstract suspend fun findMessages(
        profileId: String,
        conversationId: String,
    ): List<AssistantMessageEntity>

    @Insert
    abstract suspend fun insertConversation(conversation: AssistantConversationEntity)

    @Insert
    abstract suspend fun insertMessages(messages: List<AssistantMessageEntity>)

    @Query(
        """
        UPDATE assistant_conversations
        SET title = :title, updatedAt = :updatedAt
        WHERE bodyProfileId = :profileId AND id = :conversationId
        """,
    )
    abstract suspend fun renameConversation(
        profileId: String,
        conversationId: String,
        title: String,
        updatedAt: Long,
    ): Int

    @Query(
        """
        UPDATE assistant_conversations
        SET memorySummary = :summary, updatedAt = :updatedAt
        WHERE bodyProfileId = :profileId AND id = :conversationId
        """,
    )
    abstract suspend fun updateMemory(
        profileId: String,
        conversationId: String,
        summary: String?,
        updatedAt: Long,
    ): Int

    @Query(
        """
        UPDATE assistant_conversations
        SET memorySummary = NULL, memoryClearedAt = :clearedAt, updatedAt = :clearedAt
        WHERE bodyProfileId = :profileId AND id = :conversationId
        """,
    )
    abstract suspend fun clearMemory(
        profileId: String,
        conversationId: String,
        clearedAt: Long,
    ): Int

    @Query(
        """
        DELETE FROM assistant_conversations
        WHERE bodyProfileId = :profileId AND id = :conversationId
        """,
    )
    abstract suspend fun deleteConversation(profileId: String, conversationId: String): Int

    @Query(
        """
        UPDATE assistant_conversations
        SET title = :title, memorySummary = :memorySummary, updatedAt = :updatedAt
        WHERE bodyProfileId = :profileId AND id = :conversationId
        """,
    )
    protected abstract suspend fun finishTurn(
        profileId: String,
        conversationId: String,
        title: String,
        memorySummary: String?,
        updatedAt: Long,
    ): Int

    @Transaction
    open suspend fun appendCompletedTurn(
        profileId: String,
        conversationId: String,
        userMessage: AssistantMessageEntity,
        assistantMessage: AssistantMessageEntity,
        title: String,
        memorySummary: String?,
        updatedAt: Long,
    ): Boolean {
        if (findOwnedConversation(profileId, conversationId) == null) return false
        insertMessages(listOf(userMessage, assistantMessage))
        return finishTurn(profileId, conversationId, title, memorySummary, updatedAt) == 1
    }
}
