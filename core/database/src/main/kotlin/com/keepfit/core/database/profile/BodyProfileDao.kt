package com.keepfit.core.database.profile

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyProfileDao {
    @Upsert
    suspend fun upsert(profile: BodyProfileEntity)

    @Query("SELECT * FROM body_profiles LIMIT 1")
    fun observeLocalProfile(): Flow<BodyProfileEntity?>

    @Query("SELECT * FROM body_profiles LIMIT 1")
    suspend fun findLocalProfile(): BodyProfileEntity?
}
