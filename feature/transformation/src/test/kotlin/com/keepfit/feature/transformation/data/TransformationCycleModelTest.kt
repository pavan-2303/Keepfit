package com.keepfit.feature.transformation.data

import com.keepfit.core.database.transformation.TransformationCycleDetails
import com.keepfit.core.database.transformation.TransformationCycleEntity
import com.keepfit.core.database.transformation.TransformationPhotoAngle
import com.keepfit.core.database.transformation.TransformationPhotoEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransformationCycleModelTest {
    @Test
    fun groupsPhotosIntoDerivedCycleDaysAndCalculatesOffsetsFromStartDate() {
        val cycle = sampleCycle(
            id = "cycle-1",
            startDate = LocalDate.parse("2026-05-01"),
            closedAt = null,
        )
        val details = TransformationCycleDetails(
            cycle = cycle,
            photos = listOf(
                samplePhoto(
                    id = "front-day-0",
                    cycleId = cycle.id,
                    captureDate = LocalDate.parse("2026-05-01"),
                    angle = TransformationPhotoAngle.FRONT,
                ),
                samplePhoto(
                    id = "back-day-0",
                    cycleId = cycle.id,
                    captureDate = LocalDate.parse("2026-05-01"),
                    angle = TransformationPhotoAngle.BACK,
                ),
                samplePhoto(
                    id = "front-day-12",
                    cycleId = cycle.id,
                    captureDate = LocalDate.parse("2026-05-13"),
                    angle = TransformationPhotoAngle.FRONT,
                ),
            ),
        )

        val model = details.toCycleModel(
            canReopen = false,
            photoPathResolver = { "/abs/$it" },
            workoutsCompleted = 3,
            averageCalories = 2300.0,
            averageProteinGrams = 160.0,
            averageCarbohydrateGrams = 220.0,
            averageFatGrams = 70.0,
            weightChangeKg = -1.5,
        )

        assertEquals(LocalDate.parse("2026-05-13"), model.latestCaptureDate)
        assertEquals(listOf(0, 12), model.days.map(TransformationCycleDay::dayNumber))
        assertEquals(2, model.days.first().photos.size)
        assertEquals(TransformationPhotoAngle.FRONT, model.defaultComparison.leftDay.photos.first().angle)
        assertEquals(12, model.defaultComparison.rightDay.dayNumber)
    }

    @Test
    fun allowsReopenOnlyForLatestClosedCycleWhenNoActiveCycleExists() {
        val oldestClosed = sampleCycle(
            id = "cycle-old",
            startDate = LocalDate.parse("2026-05-01"),
            closedAt = 100L,
        )
        val latestClosed = sampleCycle(
            id = "cycle-latest",
            startDate = LocalDate.parse("2026-05-20"),
            closedAt = 200L,
        )

        val withoutActive = buildTransformationTimeline(
            cycles = listOf(
                TransformationCycleDetails(cycle = oldestClosed, photos = emptyList()),
                TransformationCycleDetails(cycle = latestClosed, photos = emptyList()),
            ),
            photoPathResolver = { it },
        )

        assertTrue(withoutActive.history.first { it.id == "cycle-latest" }.canReopen)
        assertFalse(withoutActive.history.first { it.id == "cycle-old" }.canReopen)

        val activeCycle = sampleCycle(
            id = "cycle-active",
            startDate = LocalDate.parse("2026-05-31"),
            closedAt = null,
        )
        val withActive = buildTransformationTimeline(
            cycles = listOf(
                TransformationCycleDetails(cycle = oldestClosed, photos = emptyList()),
                TransformationCycleDetails(cycle = latestClosed, photos = emptyList()),
                TransformationCycleDetails(cycle = activeCycle, photos = emptyList()),
            ),
            photoPathResolver = { it },
        )

        assertEquals("cycle-active", withActive.activeCycle?.id)
        assertFalse(withActive.history.any { it.canReopen })
    }

    private fun sampleCycle(
        id: String,
        startDate: LocalDate,
        closedAt: Long?,
    ) = TransformationCycleEntity(
        id = id,
        bodyProfileId = "profile-1",
        startDate = startDate,
        notes = "Cycle notes",
        closedAt = closedAt,
        createdAt = 1L,
        updatedAt = 2L,
    )

    private fun samplePhoto(
        id: String,
        cycleId: String,
        captureDate: LocalDate,
        angle: TransformationPhotoAngle,
    ) = TransformationPhotoEntity(
        id = id,
        transformationCycleId = cycleId,
        captureDate = captureDate,
        angle = angle,
        relativePath = "media/transformation/$cycleId/$id.jpg",
        mimeType = "image/jpeg",
        sizeBytes = 100L,
        createdAt = 10L,
    )
}
