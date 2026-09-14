package com.keepfit.core.database.journey

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface JourneyDao {
    @Upsert
    suspend fun upsert(profile: JourneyProfileEntity)

    @Query("SELECT * FROM journey_profiles WHERE bodyProfileId = :bodyProfileId LIMIT 1")
    fun observeForBodyProfile(bodyProfileId: String): Flow<JourneyProfileEntity?>

    @Query("SELECT * FROM journey_profiles WHERE bodyProfileId = :bodyProfileId LIMIT 1")
    suspend fun findForBodyProfile(bodyProfileId: String): JourneyProfileEntity?
}
