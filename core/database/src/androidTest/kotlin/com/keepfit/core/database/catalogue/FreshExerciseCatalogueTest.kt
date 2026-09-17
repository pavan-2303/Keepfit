package com.keepfit.core.database.catalogue

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keepfit.core.database.KeepfitDatabase
import com.keepfit.core.database.KeepfitDatabaseFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FreshExerciseCatalogueTest {
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
    fun freshDatabaseStartsWithAnEmptyPersonalCatalogue() = runBlocking {
        database = KeepfitDatabaseFactory.create(context, TEST_DATABASE)

        assertTrue(requireNotNull(database).workoutDao().observeExercises("").first().isEmpty())
    }

    private companion object {
        const val TEST_DATABASE = "fresh-exercise-catalogue-test"
    }
}
