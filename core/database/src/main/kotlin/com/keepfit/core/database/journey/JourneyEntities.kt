package com.keepfit.core.database.journey

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.keepfit.core.database.profile.BodyProfileEntity

@Entity(
    tableName = "journey_profiles",
    foreignKeys = [
        ForeignKey(
            entity = BodyProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["bodyProfileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["bodyProfileId"], unique = true)],
)
data class JourneyProfileEntity(
    @PrimaryKey val id: String,
    val bodyProfileId: String,
    val primaryGoal: String,
    val experienceLevel: String,
    val preferredDays: String,
    val sessionMinutes: Int,
    val equipment: String,
    val avoidedExerciseKeys: String,
    val createdAt: Long,
    val updatedAt: Long,
)
