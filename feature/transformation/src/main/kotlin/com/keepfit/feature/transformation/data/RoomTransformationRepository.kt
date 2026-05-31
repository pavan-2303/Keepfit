package com.keepfit.feature.transformation.data

import android.net.Uri
import com.keepfit.core.database.nutrition.DailyNutritionTotalsByDateRow
import com.keepfit.core.database.nutrition.NutritionDao
import com.keepfit.core.database.profile.BodyProfileDao
import com.keepfit.core.database.profile.BodyProfileEntity
import com.keepfit.core.database.transformation.BodyMeasurementEntity
import com.keepfit.core.database.transformation.TransformationDao
import com.keepfit.core.database.transformation.TransformationPhotoAngle
import com.keepfit.core.database.transformation.TransformationPhotoEntity
import com.keepfit.core.database.transformation.TransformationWeekDetails
import com.keepfit.core.database.transformation.TransformationWeekEntity
import com.keepfit.core.database.workout.CompletedWorkoutDayRow
import com.keepfit.core.database.workout.WorkoutDao
import com.keepfit.core.media.TransformationPhotoStore
import com.keepfit.feature.transformation.MeasurementInput
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class RoomTransformationRepository(
    private val transformationDao: TransformationDao,
    private val bodyProfileDao: BodyProfileDao,
    private val nutritionDao: NutritionDao,
    private val workoutDao: WorkoutDao,
    private val photoStore: TransformationPhotoStore,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
    private val clock: () -> Long = System::currentTimeMillis,
) : TransformationRepository {
    private val profileFlow = bodyProfileDao.observeLocalProfile().filterNotNull()

    override fun observeCurrentOverview(): Flow<CurrentProgressOverview> =
        profileFlow.flatMapLatest { profile ->
            transformationDao.observeLatestMeasurement(profile.id).map { latest ->
                CurrentProgressOverview(
                    latestMeasurement = latest?.toModel(),
                    heightCm = profile.heightCm,
                    bmi = calculateBmi(profile.heightCm, latest?.weightKg),
                )
            }
        }

    override fun observeMeasurements(): Flow<List<BodyMeasurement>> =
        profileFlow.flatMapLatest { profile ->
            transformationDao.observeMeasurements(profile.id).map { measurements ->
                measurements.map(BodyMeasurementEntity::toModel)
            }
        }

    override fun observeWeeks(): Flow<List<TransformationWeek>> =
        combine(
            profileFlow.flatMapLatest { profile -> transformationDao.observeWeeks(profile.id) },
            workoutDao.observeCompletedWorkoutDays(),
            nutritionDao.observeAllDailyTotals(),
            profileFlow.flatMapLatest { profile -> transformationDao.observeMeasurements(profile.id) },
        ) { weeks, completedWorkoutDays, nutritionDays, measurements ->
            weeks.map { it.toModel(completedWorkoutDays, nutritionDays, measurements, photoStore) }
        }

    override suspend fun saveMeasurement(input: MeasurementInput) {
        val profile = requireProfile()
        transformationDao.upsertMeasurement(
            BodyMeasurementEntity(
                id = idFactory(),
                bodyProfileId = profile.id,
                measurementDate = input.measurementDate,
                weightKg = input.weightKg,
                waistCm = input.waistCm,
                chestCm = input.chestCm,
                hipsCm = input.hipsCm,
                leftArmCm = input.leftArmCm,
                rightArmCm = input.rightArmCm,
                leftThighCm = input.leftThighCm,
                rightThighCm = input.rightThighCm,
                notes = input.notes,
                createdAt = clock(),
            ),
        )
    }

    override suspend fun saveWeekNotes(weekStartDate: LocalDate, notes: String) {
        val profile = requireProfile()
        val existing = ensureWeek(profile, normalizeWeekStart(weekStartDate))
        transformationDao.upsertWeek(existing.copy(notes = notes.trim().ifEmpty { null }))
    }

    override suspend fun importPhoto(
        weekStartDate: LocalDate,
        angle: TransformationPhotoAngle,
        uri: Uri,
    ) {
        val profile = requireProfile()
        val normalizedWeekStart = normalizeWeekStart(weekStartDate)
        val week = ensureWeek(profile, normalizedWeekStart)
        val existingPhoto = transformationDao.findPhoto(week.id, angle)
        val imported = photoStore.import(week.id, uri)
        try {
            transformationDao.upsertPhoto(
                TransformationPhotoEntity(
                    id = existingPhoto?.id ?: imported.id,
                    transformationWeekId = week.id,
                    angle = angle,
                    relativePath = imported.relativePath,
                    mimeType = imported.mimeType,
                    sizeBytes = imported.sizeBytes,
                    createdAt = clock(),
                ),
            )
            existingPhoto?.relativePath
                ?.takeIf { it != imported.relativePath }
                ?.let(photoStore::delete)
        } catch (error: Exception) {
            photoStore.delete(imported.relativePath)
            throw error
        }
    }

    private suspend fun requireProfile(): BodyProfileEntity =
        requireNotNull(bodyProfileDao.findLocalProfile()) { "Profile missing." }

    private suspend fun ensureWeek(
        profile: BodyProfileEntity,
        weekStartDate: LocalDate,
    ): TransformationWeekEntity {
        val existing = transformationDao.findWeek(profile.id, weekStartDate)
        if (existing != null) {
            return existing
        }
        val created = TransformationWeekEntity(
            id = idFactory(),
            bodyProfileId = profile.id,
            weekStartDate = weekStartDate,
            notes = null,
            createdAt = clock(),
        )
        transformationDao.upsertWeek(created)
        return created
    }
}

private fun BodyMeasurementEntity.toModel() = BodyMeasurement(
    id = id,
    measurementDate = measurementDate,
    weightKg = weightKg,
    waistCm = waistCm,
    chestCm = chestCm,
    hipsCm = hipsCm,
    leftArmCm = leftArmCm,
    rightArmCm = rightArmCm,
    leftThighCm = leftThighCm,
    rightThighCm = rightThighCm,
    notes = notes,
    createdAt = createdAt,
)

private fun TransformationWeekDetails.toModel(
    completedWorkoutDays: List<CompletedWorkoutDayRow>,
    nutritionDays: List<DailyNutritionTotalsByDateRow>,
    measurements: List<BodyMeasurementEntity>,
    photoStore: TransformationPhotoStore,
): TransformationWeek {
    val weekStart = week.weekStartDate
    val weekEnd = weekStart.plusDays(6)
    val nutritionInWeek = nutritionDays.filter { it.diaryDate in weekStart..weekEnd }
    val measurementsInWeek = measurements
        .filter { it.measurementDate in weekStart..weekEnd }
        .sortedWith(compareByDescending<BodyMeasurementEntity> { it.measurementDate }.thenByDescending { it.createdAt })
    val currentWeight = measurementsInWeek.firstOrNull { it.weightKg != null }?.weightKg
    val previousWeight = measurements
        .filter { it.measurementDate < weekStart && it.weightKg != null }
        .sortedWith(compareByDescending<BodyMeasurementEntity> { it.measurementDate }.thenByDescending { it.createdAt })
        .firstOrNull()
        ?.weightKg
    return TransformationWeek(
        id = week.id,
        weekStartDate = weekStart,
        notes = week.notes,
        photos = photos
            .sortedBy(TransformationPhotoEntity::angle)
            .map {
                TransformationPhoto(
                    id = it.id,
                    angle = it.angle,
                    relativePath = it.relativePath,
                    absolutePath = photoStore.resolveAbsolutePath(it.relativePath),
                    mimeType = it.mimeType,
                    sizeBytes = it.sizeBytes,
                    createdAt = it.createdAt,
                )
            },
        summary = WeeklyProgressSummary(
            workoutsCompleted = completedWorkoutDays
                .filter { it.workoutDate in weekStart..weekEnd }
                .sumOf(CompletedWorkoutDayRow::completedCount),
            averageCalories = nutritionInWeek.averageOrNull { it.calories },
            averageProteinGrams = nutritionInWeek.averageOrNull { it.proteinGrams },
            averageCarbohydrateGrams = nutritionInWeek.averageOrNull { it.carbohydrateGrams },
            averageFatGrams = nutritionInWeek.averageOrNull { it.fatGrams },
            weightChangeKg = if (currentWeight != null && previousWeight != null) {
                currentWeight - previousWeight
            } else {
                null
            },
        ),
    )
}

private fun normalizeWeekStart(date: LocalDate): LocalDate = date.with(DayOfWeek.MONDAY)

private inline fun <T> List<T>.averageOrNull(selector: (T) -> Double): Double? =
    if (isEmpty()) null else sumOf(selector) / size.toDouble()
