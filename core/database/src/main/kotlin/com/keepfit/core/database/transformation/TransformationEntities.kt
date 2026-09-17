package com.keepfit.core.database.transformation

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.keepfit.core.database.profile.BodyProfileEntity
import java.time.LocalDate

@Entity(
    tableName = "body_measurements",
    foreignKeys = [
        ForeignKey(
            entity = BodyProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["bodyProfileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bodyProfileId"), Index("measurementDate")],
)
data class BodyMeasurementEntity(
    @PrimaryKey val id: String,
    val bodyProfileId: String,
    val measurementDate: LocalDate,
    val weightKg: Double?,
    val waistCm: Double?,
    val chestCm: Double?,
    val hipsCm: Double?,
    val leftArmCm: Double?,
    val rightArmCm: Double?,
    val leftThighCm: Double?,
    val rightThighCm: Double?,
    val notes: String?,
    val createdAt: Long,
)

@Entity(
    tableName = "transformation_cycles",
    foreignKeys = [
        ForeignKey(
            entity = BodyProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["bodyProfileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bodyProfileId"), Index("startDate"), Index("closedAt")],
)
data class TransformationCycleEntity(
    @PrimaryKey val id: String,
    val bodyProfileId: String,
    val startDate: LocalDate,
    val notes: String?,
    val closedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "transformation_photos",
    foreignKeys = [
        ForeignKey(
            entity = TransformationCycleEntity::class,
            parentColumns = ["id"],
            childColumns = ["transformationCycleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("transformationCycleId"),
        Index("captureDate"),
        Index(value = ["transformationCycleId", "captureDate", "poseKey"], unique = true),
    ],
)
data class TransformationPhotoEntity(
    @PrimaryKey val id: String,
    val transformationCycleId: String,
    val captureDate: LocalDate,
    val poseKey: String,
    val relativePath: String,
    val mimeType: String,
    val sizeBytes: Long,
    val createdAt: Long,
)

@Entity(
    tableName = "transformation_pose_preferences",
    primaryKeys = ["bodyProfileId", "poseKey"],
    foreignKeys = [
        ForeignKey(
            entity = BodyProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["bodyProfileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class TransformationPosePreferenceEntity(
    val bodyProfileId: String,
    val poseKey: String,
    val updatedAt: Long,
)

data class TransformationCycleDetails(
    @Embedded val cycle: TransformationCycleEntity,
    @Relation(parentColumn = "id", entityColumn = "transformationCycleId")
    val photos: List<TransformationPhotoEntity>,
)
