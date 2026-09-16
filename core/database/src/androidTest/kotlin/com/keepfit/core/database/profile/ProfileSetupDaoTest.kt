package com.keepfit.core.database.profile

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keepfit.core.database.KeepfitDatabase
import com.keepfit.core.database.transformation.BodyMeasurementEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileSetupDaoTest {
    private lateinit var database: KeepfitDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KeepfitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun profileAndStartingMeasurementAreSavedTogetherWithProfileOwnership() = runBlocking {
        val birthDate = LocalDate.of(1992, 6, 15)
        val measurementDate = LocalDate.of(2026, 9, 16)
        database.profileSetupDao().saveProfileWithStartingMeasurement(
            profile = BodyProfileEntity(
                id = "profile-id",
                displayName = "Pavan",
                heightCm = 178.0,
                birthDate = birthDate,
                createdAt = 123L,
                updatedAt = 123L,
            ),
            measurement = BodyMeasurementEntity(
                id = "measurement-id",
                bodyProfileId = "profile-id",
                measurementDate = measurementDate,
                weightKg = 79.2,
                waistCm = null,
                chestCm = null,
                hipsCm = null,
                leftArmCm = null,
                rightArmCm = null,
                leftThighCm = null,
                rightThighCm = null,
                notes = "Starting weight from onboarding",
                createdAt = 123L,
            ),
        )

        assertEquals(birthDate, database.bodyProfileDao().findProfile("profile-id")?.birthDate)
        val measurements = database.transformationDao().observeMeasurements("profile-id").first()
        assertEquals(listOf("measurement-id"), measurements.map { it.id })
        assertEquals(79.2, measurements.single().weightKg ?: 0.0, 0.0)
    }

    @Test
    fun invalidMeasurementRollsBackProfileCreation() = runBlocking {
        val result = runCatching {
            database.profileSetupDao().saveProfileWithStartingMeasurement(
                profile = BodyProfileEntity(
                    id = "profile-id",
                    displayName = "Pavan",
                    heightCm = 178.0,
                    birthDate = null,
                    createdAt = 123L,
                    updatedAt = 123L,
                ),
                measurement = BodyMeasurementEntity(
                    id = "measurement-id",
                    bodyProfileId = "different-profile",
                    measurementDate = LocalDate.of(2026, 9, 16),
                    weightKg = 79.2,
                    waistCm = null,
                    chestCm = null,
                    hipsCm = null,
                    leftArmCm = null,
                    rightArmCm = null,
                    leftThighCm = null,
                    rightThighCm = null,
                    notes = null,
                    createdAt = 123L,
                ),
            )
        }

        assertTrue(result.isFailure)
        assertNull(database.bodyProfileDao().findProfile("profile-id"))
    }
}
