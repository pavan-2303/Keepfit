package com.keepfit.core.database.review

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyReviewDao {
    @Query("SELECT * FROM weekly_review_outcomes WHERE weekStart = :weekStart LIMIT 1")
    fun observeForWeek(weekStart: LocalDate): Flow<WeeklyReviewOutcomeEntity?>

    @Query("SELECT * FROM weekly_review_outcomes WHERE weekStart = :weekStart LIMIT 1")
    suspend fun findForWeek(weekStart: LocalDate): WeeklyReviewOutcomeEntity?

    @Upsert
    suspend fun upsert(outcome: WeeklyReviewOutcomeEntity)
}
