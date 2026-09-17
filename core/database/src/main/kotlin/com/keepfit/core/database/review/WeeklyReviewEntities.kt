package com.keepfit.core.database.review

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.keepfit.core.database.profile.BodyProfileEntity
import java.time.LocalDate

@Entity(
    tableName = "weekly_review_outcomes",
    foreignKeys = [
        ForeignKey(
            entity = BodyProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["bodyProfileId"],
        ),
    ],
    indices = [
        Index("bodyProfileId"),
        Index(value = ["bodyProfileId", "weekStart"], unique = true),
        Index("occurrenceId"),
    ],
)
data class WeeklyReviewOutcomeEntity(
    @PrimaryKey val id: String,
    val weekStart: LocalDate,
    val status: String,
    val draftType: String?,
    val sourcePlannedWorkoutId: String?,
    val sourceDate: LocalDate?,
    val targetDate: LocalDate?,
    val occurrenceId: String?,
    val decidedAt: Long,
    val bodyProfileId: String = "",
)
