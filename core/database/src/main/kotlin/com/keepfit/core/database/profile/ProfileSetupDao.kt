package com.keepfit.core.database.profile

import androidx.room.Dao
import androidx.room.Transaction
import androidx.room.Upsert
import com.keepfit.core.database.transformation.BodyMeasurementEntity

@Dao
interface ProfileSetupDao {
    @Upsert
    suspend fun upsertProfile(profile: BodyProfileEntity)

    @Upsert
    suspend fun upsertMeasurement(measurement: BodyMeasurementEntity)

    @Transaction
    suspend fun saveProfileWithStartingMeasurement(
        profile: BodyProfileEntity,
        measurement: BodyMeasurementEntity?,
    ) {
        upsertProfile(profile)
        measurement?.let { upsertMeasurement(it) }
    }
}
