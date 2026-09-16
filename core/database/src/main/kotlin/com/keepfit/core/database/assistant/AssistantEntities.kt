package com.keepfit.core.database.assistant

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.keepfit.core.database.profile.BodyProfileEntity

@Entity(
    tableName = "assistant_conversations",
    foreignKeys = [
        ForeignKey(
            entity = BodyProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["bodyProfileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bodyProfileId"), Index(value = ["bodyProfileId", "updatedAt"])],
)
data class AssistantConversationEntity(
    @PrimaryKey val id: String,
    val bodyProfileId: String,
    val coachId: String,
    val title: String,
    val memorySummary: String?,
    val memoryClearedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "assistant_messages",
    foreignKeys = [
        ForeignKey(
            entity = AssistantConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["assistantConversationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("assistantConversationId"),
        Index(value = ["assistantConversationId", "createdAt"]),
    ],
)
data class AssistantMessageEntity(
    @PrimaryKey val id: String,
    val assistantConversationId: String,
    val role: String,
    val content: String,
    val includedLocalContext: Boolean,
    val createdAt: Long,
)
