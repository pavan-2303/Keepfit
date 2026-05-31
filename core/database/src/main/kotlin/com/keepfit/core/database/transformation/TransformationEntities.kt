package com.keepfit.core.database.transformation

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.keepfit.core.database.profile.BodyProfileEntity
import java.time.LocalDate

enum class TransformationPhotoAngle {
    FRONT,
    LEFT,
    RIGHT,
    BACK,
    LEGS,
}

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
    tableName = "transformation_weeks",
    foreignKeys = [
        ForeignKey(
            entity = BodyProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["bodyProfileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bodyProfileId"), Index(value = ["bodyProfileId", "weekStartDate"], unique = true)],
)
data class TransformationWeekEntity(
    @PrimaryKey val id: String,
    val bodyProfileId: String,
    val weekStartDate: LocalDate,
    val notes: String?,
    val createdAt: Long,
)

@Entity(
    tableName = "transformation_photos",
    foreignKeys = [
        ForeignKey(
            entity = TransformationWeekEntity::class,
            parentColumns = ["id"],
            childColumns = ["transformationWeekId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("transformationWeekId"), Index(value = ["transformationWeekId", "angle"], unique = true)],
)
data class TransformationPhotoEntity(
    @PrimaryKey val id: String,
    val transformationWeekId: String,
    val angle: TransformationPhotoAngle,
    val relativePath: String,
    val mimeType: String,
    val sizeBytes: Long,
    val createdAt: Long,
)

data class TransformationWeekDetails(
    @Embedded val week: TransformationWeekEntity,
    @Relation(parentColumn = "id", entityColumn = "transformationWeekId")
    val photos: List<TransformationPhotoEntity>,
)
