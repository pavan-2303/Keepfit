package com.keepfit.core.database.profile

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keepfit.core.database.KeepfitDatabase
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BodyProfileDaoTest {
    private lateinit var database: KeepfitDatabase
    private lateinit var dao: BodyProfileDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KeepfitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.bodyProfileDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insertAndObserveLocalProfile() = runBlocking {
        val profile = BodyProfileEntity(
            id = "profile-id",
            displayName = "Pavan",
            heightCm = 178.0,
            birthDate = LocalDate.parse("1994-01-12"),
            createdAt = 100L,
            updatedAt = 100L,
        )

        dao.upsert(profile)

        assertEquals(profile, dao.observeLocalProfile().first())
    }
}

