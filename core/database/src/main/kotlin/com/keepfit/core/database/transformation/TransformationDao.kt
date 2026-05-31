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
    suspend fun upsertWeek(week: TransformationWeekEntity)

    @Query(
        """
        SELECT * FROM transformation_weeks
        WHERE bodyProfileId = :profileId AND weekStartDate = :weekStartDate
        LIMIT 1
        """,
    )
    suspend fun findWeek(profileId: String, weekStartDate: LocalDate): TransformationWeekEntity?

    @Transaction
    @Query(
        """
        SELECT * FROM transformation_weeks
        WHERE bodyProfileId = :profileId
        ORDER BY weekStartDate DESC
        """,
    )
    fun observeWeeks(profileId: String): Flow<List<TransformationWeekDetails>>

    @Query(
        """
        SELECT * FROM transformation_photos
        WHERE transformationWeekId = :weekId AND angle = :angle
        LIMIT 1
        """,
    )
    suspend fun findPhoto(weekId: String, angle: TransformationPhotoAngle): TransformationPhotoEntity?

    @Upsert
    suspend fun upsertPhoto(photo: TransformationPhotoEntity)
}
