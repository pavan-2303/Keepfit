package com.keepfit.core.database.profile

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyProfileDao {
    @Upsert
    suspend fun upsert(profile: BodyProfileEntity)

    @Query("SELECT * FROM body_profiles WHERE archivedAt IS NULL ORDER BY createdAt, id")
    fun observeProfiles(): Flow<List<BodyProfileEntity>>

    @Query("SELECT * FROM body_profiles WHERE id = :profileId AND archivedAt IS NULL LIMIT 1")
    fun observeProfile(profileId: String): Flow<BodyProfileEntity?>

    @Query("SELECT * FROM body_profiles WHERE archivedAt IS NULL ORDER BY createdAt, id LIMIT 1")
    fun observeLocalProfile(): Flow<BodyProfileEntity?>

    @Query("SELECT * FROM body_profiles WHERE archivedAt IS NULL ORDER BY createdAt, id LIMIT 1")
    suspend fun findLocalProfile(): BodyProfileEntity?

    @Query("SELECT * FROM body_profiles WHERE id = :profileId AND archivedAt IS NULL LIMIT 1")
    suspend fun findProfile(profileId: String): BodyProfileEntity?

    @Query("SELECT * FROM body_profiles WHERE archivedAt IS NULL ORDER BY createdAt, id")
    suspend fun findProfiles(): List<BodyProfileEntity>

    @Query("UPDATE body_profiles SET archivedAt = :archivedAt, updatedAt = :archivedAt WHERE id = :profileId AND archivedAt IS NULL")
    suspend fun archive(profileId: String, archivedAt: Long): Int

    @Query(
        """
        UPDATE body_profiles
        SET dailyCalorieGoal = :dailyCalorieGoal,
            dailyProteinGoalGrams = :dailyProteinGoalGrams,
            dailyCarbohydrateGoalGrams = :dailyCarbohydrateGoalGrams,
            dailyFatGoalGrams = :dailyFatGoalGrams,
            updatedAt = :updatedAt
        WHERE id = :profileId
        """,
    )
    suspend fun updateNutritionGoals(
        profileId: String,
        dailyCalorieGoal: Double?,
        dailyProteinGoalGrams: Double?,
        dailyCarbohydrateGoalGrams: Double?,
        dailyFatGoalGrams: Double?,
        updatedAt: Long,
    )
}
