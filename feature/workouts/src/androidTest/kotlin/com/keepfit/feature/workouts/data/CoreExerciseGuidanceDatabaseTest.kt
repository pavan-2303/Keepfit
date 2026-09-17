package com.keepfit.feature.workouts.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keepfit.core.database.KeepfitDatabase
import com.keepfit.core.database.KeepfitDatabaseFactory
import com.keepfit.core.media.CoreExerciseGuidanceCatalog
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoreExerciseGuidanceDatabaseTest {
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
    fun everyGuideResolvesToItsPinnedBundledExercise() = runBlocking {
        database = KeepfitDatabaseFactory.create(context, TEST_DATABASE)
        val dao = requireNotNull(database).workoutDao()

        CoreExerciseGuidanceCatalog.entries.forEach { guidance ->
            val exercise = dao.findActiveExercise(guidance.exerciseId)
            assertNotNull("Missing ${guidance.exerciseName}", exercise)
            assertEquals(guidance.sourceId, exercise?.sourceId)
            assertEquals(guidance.exerciseName, exercise?.name)
            assertEquals(DATASET_SOURCE, exercise?.source)
        }
    }

    private companion object {
        const val TEST_DATABASE = "core-exercise-guidance-test"
        const val DATASET_SOURCE = "hasaneyldrm/exercises-dataset"
    }
}
