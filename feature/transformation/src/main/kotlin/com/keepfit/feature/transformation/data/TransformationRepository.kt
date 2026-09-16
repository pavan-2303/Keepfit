package com.keepfit.feature.transformation.data

import android.net.Uri
import com.keepfit.core.model.TransformationPose
import com.keepfit.feature.transformation.MeasurementInput
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface TransformationRepository {
    fun observeCurrentOverview(): Flow<CurrentProgressOverview>
    fun observeMeasurements(): Flow<List<BodyMeasurement>>
    fun observeTimeline(): Flow<TransformationTimeline>
    fun observeEnabledPoses(): Flow<List<TransformationPose>>

    suspend fun saveMeasurement(input: MeasurementInput)
    suspend fun saveCycleNotes(notes: String)
    suspend fun importPhoto(captureDate: LocalDate, pose: TransformationPose, uri: Uri)
    suspend fun setOptionalPoseEnabled(pose: TransformationPose, enabled: Boolean)
    suspend fun closeActiveCycle()
    suspend fun reopenCycle(cycleId: String)
}
