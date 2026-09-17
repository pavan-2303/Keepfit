package com.keepfit.core.database.journey

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
class JourneyDaoTest {
    private lateinit var database: KeepfitDatabase
    private lateinit var dao: JourneyDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KeepfitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.journeyDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun journeyPreferencesRoundTripForLocalProfile() = runBlocking {
        database.bodyProfileDao().upsert(
            BodyProfileEntity(
                id = "profile-id",
                displayName = "Pavan",
                heightCm = 178.0,
                birthDate = LocalDate.parse("1994-01-12"),
                createdAt = 1L,
                updatedAt = 1L,
            ),
        )
        val entity = JourneyProfileEntity(
            id = "journey-id",
            bodyProfileId = "profile-id",
            primaryGoal = "GENERAL_FITNESS",
            experienceLevel = "BEGINNER",
            preferredDays = "MONDAY,WEDNESDAY,FRIDAY",
            sessionMinutes = 30,
            equipment = "BODYWEIGHT,DUMBBELLS",
            avoidedExerciseKeys = "leg-press",
            createdAt = 2L,
            updatedAt = 2L,
        )

        dao.upsert(entity)

        assertEquals(entity, dao.observeForBodyProfile("profile-id").first())
        assertEquals(entity, dao.findForBodyProfile("profile-id"))
    }
}
