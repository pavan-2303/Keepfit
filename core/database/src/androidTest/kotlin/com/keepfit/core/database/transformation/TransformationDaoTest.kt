package com.keepfit.core.database.transformation

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keepfit.core.database.KeepfitDatabase
import com.keepfit.core.database.profile.BodyProfileEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransformationDaoTest {
    private lateinit var database: KeepfitDatabase
    private lateinit var dao: TransformationDao

    @Before
    fun createDatabase() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KeepfitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        database.bodyProfileDao().upsert(
            BodyProfileEntity(
                id = "profile-id",
                displayName = "Pavan",
                heightCm = 178.0,
                birthDate = null,
                createdAt = 1L,
                updatedAt = 1L,
            ),
        )
        database.bodyProfileDao().upsert(
            BodyProfileEntity(
                id = "profile-two",
                displayName = "Second",
                heightCm = null,
                birthDate = null,
                createdAt = 2L,
                updatedAt = 2L,
            ),
        )
        dao = database.transformationDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun measurementsAndCyclesWithPhotosAreObservable() = runBlocking {
        dao.upsertMeasurement(
            BodyMeasurementEntity(
                id = "m-1",
                bodyProfileId = "profile-id",
                measurementDate = LocalDate.parse("2026-05-25"),
                weightKg = 80.0,
                waistCm = null,
                chestCm = null,
                hipsCm = null,
                leftArmCm = null,
                rightArmCm = null,
                leftThighCm = null,
                rightThighCm = null,
                notes = "Start",
                createdAt = 10L,
            ),
        )
        dao.upsertMeasurement(
            BodyMeasurementEntity(
                id = "m-2",
                bodyProfileId = "profile-id",
                measurementDate = LocalDate.parse("2026-05-31"),
                weightKg = 79.0,
                waistCm = 86.0,
                chestCm = null,
                hipsCm = null,
                leftArmCm = null,
                rightArmCm = null,
                leftThighCm = null,
                rightThighCm = null,
                notes = null,
                createdAt = 20L,
            ),
        )

        assertEquals("m-2", dao.observeLatestMeasurement("profile-id").first()?.id)

        dao.upsertCycle(
            TransformationCycleEntity(
                id = "cycle-1",
                bodyProfileId = "profile-id",
                startDate = LocalDate.parse("2026-05-25"),
                notes = "Cycle one",
                closedAt = null,
                createdAt = 30L,
                updatedAt = 30L,
            ),
        )
        dao.upsertPhoto(
            TransformationPhotoEntity(
                id = "photo-1",
                transformationCycleId = "cycle-1",
                captureDate = LocalDate.parse("2026-05-25"),
                poseKey = "front_relaxed",
                relativePath = "media/transformation/cycle-1/front.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 123L,
                createdAt = 31L,
            ),
        )

        val cycles = dao.observeCycles("profile-id").first()
        assertEquals(1, cycles.size)
        assertEquals("Cycle one", cycles.single().cycle.notes)
        assertEquals(listOf("front_relaxed"), cycles.single().photos.map { it.poseKey })
    }

    @Test
    fun findsSameDayPhotoByCycleAndAngle() = runBlocking {
        dao.upsertCycle(
            TransformationCycleEntity(
                id = "cycle-1",
                bodyProfileId = "profile-id",
                startDate = LocalDate.parse("2026-05-25"),
                notes = null,
                closedAt = null,
                createdAt = 30L,
                updatedAt = 30L,
            ),
        )
        dao.upsertPhoto(
            TransformationPhotoEntity(
                id = "photo-1",
                transformationCycleId = "cycle-1",
                captureDate = LocalDate.parse("2026-05-25"),
                poseKey = "front_relaxed",
                relativePath = "media/transformation/cycle-1/front.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 123L,
                createdAt = 31L,
            ),
        )

        val photo = dao.findPhoto("cycle-1", LocalDate.parse("2026-05-25"), "front_relaxed")
        assertEquals("photo-1", photo?.id)
    }

    @Test
    fun optionalPosePreferencesAreIsolatedByProfile() = runBlocking {
        dao.upsertPosePreference(
            TransformationPosePreferenceEntity(
                bodyProfileId = "profile-id",
                poseKey = "front_double_biceps",
                updatedAt = 10L,
            ),
        )
        dao.upsertPosePreference(
            TransformationPosePreferenceEntity(
                bodyProfileId = "profile-two",
                poseKey = "back_lat_spread",
                updatedAt = 11L,
            ),
        )

        assertEquals(listOf("front_double_biceps"), dao.observeOptionalPoseKeys("profile-id").first())
        assertEquals(listOf("back_lat_spread"), dao.observeOptionalPoseKeys("profile-two").first())

        dao.deletePosePreference("profile-id", "front_double_biceps")

        assertEquals(emptyList<String>(), dao.observeOptionalPoseKeys("profile-id").first())
        assertEquals(listOf("back_lat_spread"), dao.observeOptionalPoseKeys("profile-two").first())
    }
}
