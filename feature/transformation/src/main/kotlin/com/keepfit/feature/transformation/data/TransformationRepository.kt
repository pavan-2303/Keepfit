package com.keepfit.feature.transformation.data

import android.net.Uri
import com.keepfit.core.database.transformation.TransformationPhotoAngle
import com.keepfit.feature.transformation.MeasurementInput
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface TransformationRepository {
    fun observeCurrentOverview(): Flow<CurrentProgressOverview>
    fun observeMeasurements(): Flow<List<BodyMeasurement>>
    fun observeWeeks(): Flow<List<TransformationWeek>>

    suspend fun saveMeasurement(input: MeasurementInput)
    suspend fun saveWeekNotes(weekStartDate: LocalDate, notes: String)
    suspend fun importPhoto(weekStartDate: LocalDate, angle: TransformationPhotoAngle, uri: Uri)
}
