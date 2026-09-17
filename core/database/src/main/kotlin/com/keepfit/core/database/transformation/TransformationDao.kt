package com.keepfit.core.database.transformation

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface TransformationDao {
    @Upsert
    suspend fun upsertMeasurement(measurement: BodyMeasurementEntity)

    @Query(
        """
        SELECT * FROM body_measurements
        WHERE bodyProfileId = :profileId
        ORDER BY measurementDate DESC, createdAt DESC
        """,
    )
    fun observeMeasurements(profileId: String): Flow<List<BodyMeasurementEntity>>

    @Query(
        """
        SELECT * FROM body_measurements
        WHERE bodyProfileId = :profileId
        ORDER BY measurementDate DESC, createdAt DESC
        LIMIT 1
        """,
    )
    fun observeLatestMeasurement(profileId: String): Flow<BodyMeasurementEntity?>

    @Query(
        """
        SELECT * FROM body_measurements
        WHERE bodyProfileId = :profileId
        ORDER BY measurementDate DESC, createdAt DESC
        """,
    )
    suspend fun listMeasurements(profileId: String): List<BodyMeasurementEntity>

    @Upsert
    suspend fun upsertCycle(cycle: TransformationCycleEntity)

    @Query(
        """
        SELECT * FROM transformation_cycles
        WHERE bodyProfileId = :profileId AND closedAt IS NULL
        LIMIT 1
        """,
    )
    suspend fun findActiveCycle(profileId: String): TransformationCycleEntity?

    @Query(
        """
        SELECT * FROM transformation_cycles
        WHERE id = :cycleId
        LIMIT 1
        """,
    )
    suspend fun findCycleById(cycleId: String): TransformationCycleEntity?

    @Transaction
    @Query(
        """
        SELECT * FROM transformation_cycles
        WHERE bodyProfileId = :profileId
        ORDER BY CASE WHEN closedAt IS NULL THEN 0 ELSE 1 END, startDate DESC
        """,
    )
    fun observeCycles(profileId: String): Flow<List<TransformationCycleDetails>>

    @Query(
        """
        SELECT * FROM transformation_cycles
        WHERE bodyProfileId = :profileId
        ORDER BY startDate DESC
        """,
    )
    suspend fun listCycles(profileId: String): List<TransformationCycleEntity>

    @Query(
        """
        SELECT * FROM transformation_photos
        WHERE transformationCycleId = :cycleId
            AND captureDate = :captureDate
            AND poseKey = :poseKey
        LIMIT 1
        """,
    )
    suspend fun findPhoto(
        cycleId: String,
        captureDate: LocalDate,
        poseKey: String,
    ): TransformationPhotoEntity?

    @Upsert
    suspend fun upsertPhoto(photo: TransformationPhotoEntity)

    @Query(
        """
        SELECT poseKey FROM transformation_pose_preferences
        WHERE bodyProfileId = :profileId
        ORDER BY poseKey
        """,
    )
    fun observeOptionalPoseKeys(profileId: String): Flow<List<String>>

    @Upsert
    suspend fun upsertPosePreference(preference: TransformationPosePreferenceEntity)

    @Query(
        """
        DELETE FROM transformation_pose_preferences
        WHERE bodyProfileId = :profileId AND poseKey = :poseKey
        """,
    )
    suspend fun deletePosePreference(profileId: String, poseKey: String)
}
