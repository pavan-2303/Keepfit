package com.keepfit.feature.transformation.data

import com.keepfit.core.database.transformation.TransformationPhotoAngle
import java.time.LocalDate
import kotlin.math.pow
import kotlin.math.round

data class BodyMeasurement(
    val id: String,
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

data class TransformationPhoto(
    val id: String,
    val angle: TransformationPhotoAngle,
    val relativePath: String,
    val absolutePath: String,
    val mimeType: String,
    val sizeBytes: Long,
    val createdAt: Long,
)

data class WeeklyProgressSummary(
    val workoutsCompleted: Int,
    val averageCalories: Double?,
    val averageProteinGrams: Double?,
    val averageCarbohydrateGrams: Double?,
    val averageFatGrams: Double?,
    val weightChangeKg: Double?,
)

data class TransformationWeek(
    val id: String,
    val weekStartDate: LocalDate,
    val notes: String?,
    val photos: List<TransformationPhoto>,
    val summary: WeeklyProgressSummary,
)

data class CurrentProgressOverview(
    val latestMeasurement: BodyMeasurement?,
    val heightCm: Double?,
    val bmi: Double?,
)

fun calculateBmi(heightCm: Double?, weightKg: Double?): Double? {
    val normalizedHeightCm = heightCm?.takeIf { it > 0.0 } ?: return null
    val normalizedWeightKg = weightKg?.takeIf { it > 0.0 } ?: return null
    val bmi = normalizedWeightKg / (normalizedHeightCm / 100.0).pow(2)
    return round(bmi * 10.0) / 10.0
}

fun Double.formatMetric(): String =
    if (this % 1.0 == 0.0) toInt().toString() else "%.1f".format(this)
