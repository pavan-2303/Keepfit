package com.keepfit.feature.transformation.data

import android.net.Uri
import com.keepfit.core.database.nutrition.DailyNutritionTotalsByDateRow
import com.keepfit.core.database.nutrition.NutritionDao
import com.keepfit.core.database.profile.BodyProfileDao
import com.keepfit.core.database.profile.BodyProfileEntity
import com.keepfit.core.database.transformation.BodyMeasurementEntity
import com.keepfit.core.database.transformation.TransformationCycleDetails
import com.keepfit.core.database.transformation.TransformationCycleEntity
import com.keepfit.core.database.transformation.TransformationDao
import com.keepfit.core.database.transformation.TransformationPhotoEntity
import com.keepfit.core.database.transformation.TransformationPosePreferenceEntity
import com.keepfit.core.database.workout.CompletedWorkoutDayRow
import com.keepfit.core.database.workout.WorkoutDao
import com.keepfit.core.media.TransformationPhotoStore
import com.keepfit.core.model.TransformationPose
import com.keepfit.core.preferences.ActiveProfileStore
import com.keepfit.feature.transformation.MeasurementInput
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class RoomTransformationRepository(
    private val transformationDao: TransformationDao,
    private val bodyProfileDao: BodyProfileDao,
    private val nutritionDao: NutritionDao,
    private val workoutDao: WorkoutDao,
    private val photoStore: TransformationPhotoStore,
    private val activeProfileStore: ActiveProfileStore,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
    private val clock: () -> Long = System::currentTimeMillis,
) : TransformationRepository {
    private val profileFlow = activeProfileStore.observeActiveProfileId().filterNotNull()
        .flatMapLatest(bodyProfileDao::observeProfile)
        .filterNotNull()

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

    override fun observeTimeline(): Flow<TransformationTimeline> =
        combine(
            profileFlow.flatMapLatest { profile -> transformationDao.observeCycles(profile.id) },
            activeProfileStore.observeActiveProfileId().filterNotNull().flatMapLatest(
                workoutDao::observeCompletedWorkoutDaysForProfile,
            ),
            activeProfileStore.observeActiveProfileId().filterNotNull().flatMapLatest(
                nutritionDao::observeAllDailyTotalsForProfile,
            ),
            profileFlow.flatMapLatest { profile -> transformationDao.observeMeasurements(profile.id) },
        ) { cycles, completedWorkoutDays, nutritionDays, measurements ->
            buildTransformationTimeline(
                cycles = cycles,
                completedWorkoutDays = completedWorkoutDays,
                nutritionDays = nutritionDays,
                measurements = measurements,
                photoPathResolver = photoStore::resolve,
            )
        }

    override fun observeEnabledPoses(): Flow<List<TransformationPose>> =
        activeProfileStore.observeActiveProfileId()
            .filterNotNull()
            .flatMapLatest(transformationDao::observeOptionalPoseKeys)
            .map(::resolveEnabledPoses)

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

    override suspend fun saveCycleNotes(notes: String) {
        val profile = requireProfile()
        val activeCycle = requireNotNull(transformationDao.findActiveCycle(profile.id)) {
            "Import a photo to start a transformation cycle."
        }
        transformationDao.upsertCycle(
            activeCycle.copy(
                notes = notes.trim().ifEmpty { null },
                updatedAt = clock(),
            ),
        )
    }

    override suspend fun importPhoto(
        captureDate: LocalDate,
        pose: TransformationPose,
        uri: Uri,
    ) {
        val profile = requireProfile()
        val cycle = ensureActiveCycle(profile, captureDate)
        val existingPhoto = transformationDao.findPhoto(cycle.id, captureDate, pose.key)
        val imported = photoStore.import(cycle.id, uri)
        try {
            transformationDao.upsertPhoto(
                TransformationPhotoEntity(
                    id = existingPhoto?.id ?: imported.id,
                    transformationCycleId = cycle.id,
                    captureDate = captureDate,
                    poseKey = pose.key,
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

    override suspend fun setOptionalPoseEnabled(pose: TransformationPose, enabled: Boolean) {
        require(!pose.isDefault) { "The four basic poses are always enabled." }
        val profile = requireProfile()
        if (enabled) {
            transformationDao.upsertPosePreference(
                TransformationPosePreferenceEntity(
                    bodyProfileId = profile.id,
                    poseKey = pose.key,
                    updatedAt = clock(),
                ),
            )
        } else {
            transformationDao.deletePosePreference(profile.id, pose.key)
        }
    }

    override suspend fun closeActiveCycle() {
        val profile = requireProfile()
        val activeCycle = requireNotNull(transformationDao.findActiveCycle(profile.id)) {
            "No active transformation cycle to close."
        }
        val now = clock()
        transformationDao.upsertCycle(
            activeCycle.copy(
                closedAt = now,
                updatedAt = now,
            ),
        )
    }

    override suspend fun reopenCycle(cycleId: String) {
        val profile = requireProfile()
        require(transformationDao.findActiveCycle(profile.id) == null) {
            "Close the current transformation cycle before reopening history."
        }
        val cycle = requireNotNull(transformationDao.findCycleById(cycleId)) {
            "Transformation cycle not found."
        }
        require(cycle.bodyProfileId == profile.id) { "Transformation cycle not found." }
        require(cycle.closedAt != null) { "Only closed transformation cycles can be reopened." }
        val latestClosedCycleId = transformationDao.listCycles(profile.id)
            .filter { it.closedAt != null }
            .maxByOrNull(TransformationCycleEntity::startDate)
            ?.id
        require(cycle.id == latestClosedCycleId) {
            "Only the most recent closed transformation cycle can be reopened."
        }
        transformationDao.upsertCycle(
            cycle.copy(
                closedAt = null,
                updatedAt = clock(),
            ),
        )
    }

    private suspend fun requireProfile(): BodyProfileEntity =
        requireNotNull(activeProfileStore.observeActiveProfileId().first()?.let { bodyProfileDao.findProfile(it) }) {
            "Profile missing."
        }

    private suspend fun ensureActiveCycle(
        profile: BodyProfileEntity,
        captureDate: LocalDate,
    ): TransformationCycleEntity {
        val existing = transformationDao.findActiveCycle(profile.id)
        if (existing != null) {
            return existing
        }
        val now = clock()
        val created = TransformationCycleEntity(
            id = idFactory(),
            bodyProfileId = profile.id,
            startDate = captureDate,
            notes = null,
            closedAt = null,
            createdAt = now,
            updatedAt = now,
        )
        transformationDao.upsertCycle(created)
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

private inline fun <T> List<T>.averageOrNull(selector: (T) -> Double): Double? =
    if (isEmpty()) null else sumOf(selector) / size.toDouble()

fun buildTransformationTimeline(
    cycles: List<TransformationCycleDetails>,
    completedWorkoutDays: List<CompletedWorkoutDayRow> = emptyList(),
    nutritionDays: List<DailyNutritionTotalsByDateRow> = emptyList(),
    measurements: List<BodyMeasurementEntity> = emptyList(),
    photoPathResolver: (String) -> String?,
): TransformationTimeline {
    val latestClosedCycleId = cycles
        .filter { it.cycle.closedAt != null }
        .maxByOrNull { it.cycle.startDate }
        ?.cycle
        ?.id
    val hasActiveCycle = cycles.any { it.cycle.closedAt == null }
    val mappedCycles = cycles.map { details ->
        val startDate = details.cycle.startDate
        val latestCaptureDate = details.photos.maxOfOrNull(TransformationPhotoEntity::captureDate) ?: startDate
        val nutritionInCycle = nutritionDays.filter { it.diaryDate in startDate..latestCaptureDate }
        val measurementsInCycle = measurements
            .filter { it.measurementDate in startDate..latestCaptureDate }
            .sortedWith(compareByDescending<BodyMeasurementEntity> { it.measurementDate }.thenByDescending { it.createdAt })
        val currentWeight = measurementsInCycle.firstOrNull { it.weightKg != null }?.weightKg
        val previousWeight = measurements
            .filter { it.measurementDate < startDate && it.weightKg != null }
            .sortedWith(compareByDescending<BodyMeasurementEntity> { it.measurementDate }.thenByDescending { it.createdAt })
            .firstOrNull()
            ?.weightKg
        details.toCycleModel(
            canReopen = !hasActiveCycle && details.cycle.id == latestClosedCycleId,
            photoPathResolver = photoPathResolver,
            workoutsCompleted = completedWorkoutDays
                .filter { it.workoutDate in startDate..latestCaptureDate }
                .sumOf(CompletedWorkoutDayRow::completedCount),
            averageCalories = nutritionInCycle.averageOrNull { it.calories },
            averageProteinGrams = nutritionInCycle.averageOrNull { it.proteinGrams },
            averageCarbohydrateGrams = nutritionInCycle.averageOrNull { it.carbohydrateGrams },
            averageFatGrams = nutritionInCycle.averageOrNull { it.fatGrams },
            weightChangeKg = if (currentWeight != null && previousWeight != null) {
                currentWeight - previousWeight
            } else {
                null
            },
        )
    }
    return TransformationTimeline(
        activeCycle = mappedCycles.firstOrNull { it.isActive },
        history = mappedCycles.filterNot(TransformationCycle::isActive),
    )
}

fun TransformationCycleDetails.toCycleModel(
    canReopen: Boolean,
    photoPathResolver: (String) -> String?,
    workoutsCompleted: Int = 0,
    averageCalories: Double? = null,
    averageProteinGrams: Double? = null,
    averageCarbohydrateGrams: Double? = null,
    averageFatGrams: Double? = null,
    weightChangeKg: Double? = null,
): TransformationCycle {
    val poseOrder = TransformationPose.entries.map(TransformationPose::key).withIndex().associate { it.value to it.index }
    val sortedPhotos = photos.sortedWith(
        compareBy<TransformationPhotoEntity> { it.captureDate }
            .thenBy { poseOrder[it.poseKey] ?: Int.MAX_VALUE },
    )
    val mappedDays = sortedPhotos
        .groupBy(TransformationPhotoEntity::captureDate)
        .map { (captureDate, dayPhotos) ->
            TransformationCycleDay(
                captureDate = captureDate,
                dayNumber = ChronoUnit.DAYS.between(cycle.startDate, captureDate).toInt(),
                photos = dayPhotos.mapNotNull { photo ->
                    val pose = TransformationPose.fromKey(photo.poseKey) ?: return@mapNotNull null
                    TransformationPhoto(
                        id = photo.id,
                        captureDate = photo.captureDate,
                        pose = pose,
                        relativePath = photo.relativePath,
                        absolutePath = photoPathResolver(photo.relativePath),
                        mimeType = photo.mimeType,
                        sizeBytes = photo.sizeBytes,
                        createdAt = photo.createdAt,
                    )
                },
            )
        }
        .sortedBy(TransformationCycleDay::captureDate)
        .ifEmpty {
            listOf(
                TransformationCycleDay(
                    captureDate = cycle.startDate,
                    dayNumber = 0,
                    photos = emptyList(),
                ),
            )
        }
    return TransformationCycle(
        id = cycle.id,
        startDate = cycle.startDate,
        latestCaptureDate = mappedDays.last().captureDate,
        isActive = cycle.closedAt == null,
        canReopen = canReopen,
        notes = cycle.notes,
        days = mappedDays,
        summary = TransformationCycleSummary(
            workoutsCompleted = workoutsCompleted,
            averageCalories = averageCalories,
            averageProteinGrams = averageProteinGrams,
            averageCarbohydrateGrams = averageCarbohydrateGrams,
            averageFatGrams = averageFatGrams,
            weightChangeKg = weightChangeKg,
        ),
        defaultComparison = TransformationComparison(
            leftDay = mappedDays.first(),
            rightDay = mappedDays.last(),
        ),
    )
}

fun resolveEnabledPoses(optionalPoseKeys: List<String>): List<TransformationPose> {
    val selected = optionalPoseKeys.toSet()
    return TransformationPose.entries.filter { pose -> pose.isDefault || pose.key in selected }
}
