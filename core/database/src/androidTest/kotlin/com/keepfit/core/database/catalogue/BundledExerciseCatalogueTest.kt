package com.keepfit.core.database.catalogue

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keepfit.core.database.KeepfitDatabase
import com.keepfit.core.database.KeepfitDatabaseFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BundledExerciseCatalogueTest {
    private lateinit var context: Context
    private var database: KeepfitDatabase? = null

    @Before
    fun prepare() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(TEST_DATABASE)
    }

    @After
    fun cleanUp() {
        database?.close()
        context.deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun freshAndRepeatedOpenSeedsPinnedCatalogueOnceWithoutOverwritingEdits() = runBlocking {
        database = KeepfitDatabaseFactory.create(context, TEST_DATABASE)
        val firstDao = requireNotNull(database).workoutDao()
        val firstExercises = firstDao.observeExercises("").first()

        assertEquals(1316, firstExercises.count { it.source == SOURCE })
        val sitUp = firstExercises.single { it.sourceId == "0001" }
        assertEquals("213c323a-1011-590b-99f8-e7e9cf85aee4", sitUp.id)
        assertEquals("waist", sitUp.muscleGroup)
        assertEquals("body weight", sitUp.equipment)
        assertEquals("abs", sitUp.targetMuscle)
        assertNull(sitUp.notes)

        firstDao.upsertExercise(sitUp.copy(name = "My edited sit-up", updatedAt = 99L))
        requireNotNull(database).close()
        database = KeepfitDatabaseFactory.create(context, TEST_DATABASE)

        val reopened = requireNotNull(database).workoutDao().observeExercises("My edited").first().single()
        assertEquals(sitUp.id, reopened.id)
        requireNotNull(database).openHelper.readableDatabase.query(
            "SELECT COUNT(*) FROM catalogue_imports WHERE source = '$SOURCE'",
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
    }

    private companion object {
        const val TEST_DATABASE = "bundled-exercise-catalogue-test"
        const val SOURCE = "hasaneyldrm/exercises-dataset"
    }
}
