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
        dao = database.transformationDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun measurementsAndWeeksWithPhotosAreObservable() = runBlocking {
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

        dao.upsertWeek(
            TransformationWeekEntity(
                id = "week-1",
                bodyProfileId = "profile-id",
                weekStartDate = LocalDate.parse("2026-05-25"),
                notes = "Week one",
                createdAt = 30L,
            ),
        )
        dao.upsertPhoto(
            TransformationPhotoEntity(
                id = "photo-1",
                transformationWeekId = "week-1",
                angle = TransformationPhotoAngle.FRONT,
                relativePath = "media/transformation/week-1/front.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 123L,
                createdAt = 31L,
            ),
        )

        val weeks = dao.observeWeeks("profile-id").first()
        assertEquals(1, weeks.size)
        assertEquals("Week one", weeks.single().week.notes)
        assertEquals(listOf(TransformationPhotoAngle.FRONT), weeks.single().photos.map { it.angle })
    }
}
