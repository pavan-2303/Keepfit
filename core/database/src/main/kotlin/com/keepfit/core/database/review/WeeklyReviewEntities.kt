package com.keepfit.core.database.review

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "weekly_review_outcomes",
    indices = [
        Index(value = ["weekStart"], unique = true),
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
)
