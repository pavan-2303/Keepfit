package com.keepfit.feature.transformation.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keepfit.core.database.KeepfitDatabase
import com.keepfit.core.database.profile.BodyProfileEntity
import com.keepfit.core.media.TransformationPhotoStore
import com.keepfit.core.model.TransformationPose
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomTransformationRepositoryTest {
    private lateinit var database: KeepfitDatabase
    private lateinit var activeProfileStore: TestActiveProfileStore
    private lateinit var repository: RoomTransformationRepository

    @Before
    fun createDatabase() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KeepfitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        database.bodyProfileDao().upsert(profile("profile-one", "One", 1L))
        database.bodyProfileDao().upsert(profile("profile-two", "Two", 2L))
        activeProfileStore = TestActiveProfileStore("profile-one")
        repository = RoomTransformationRepository(
            transformationDao = database.transformationDao(),
            bodyProfileDao = database.bodyProfileDao(),
            nutritionDao = database.nutritionDao(),
            workoutDao = database.workoutDao(),
            photoStore = TransformationPhotoStore(context),
            activeProfileStore = activeProfileStore,
            clock = { 100L },
        )
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun optionalPoseSelectionsFollowActiveProfileWithoutChangingBasicDefaults() = runBlocking {
        assertEquals(TransformationPose.defaultPoses, repository.observeEnabledPoses().first())

        repository.setOptionalPoseEnabled(TransformationPose.FRONT_DOUBLE_BICEPS, true)
        assertEquals(
            TransformationPose.defaultPoses + TransformationPose.FRONT_DOUBLE_BICEPS,
            repository.observeEnabledPoses().first(),
        )

        activeProfileStore.selectProfile("profile-two")
        assertEquals(TransformationPose.defaultPoses, repository.observeEnabledPoses().first())
        repository.setOptionalPoseEnabled(TransformationPose.BACK_LAT_SPREAD, true)

        activeProfileStore.selectProfile("profile-one")
        assertEquals(
            TransformationPose.defaultPoses + TransformationPose.FRONT_DOUBLE_BICEPS,
            repository.observeEnabledPoses().first(),
        )
    }

    @Test
    fun basicPoseCannotBeDisabled() = runBlocking {
        val result = runCatching {
            repository.setOptionalPoseEnabled(TransformationPose.FRONT_RELAXED, false)
        }

        assertTrue(result.isFailure)
        assertEquals(TransformationPose.defaultPoses, repository.observeEnabledPoses().first())
    }

    private fun profile(id: String, name: String, createdAt: Long) = BodyProfileEntity(
        id = id,
        displayName = name,
        heightCm = null,
        birthDate = null,
        createdAt = createdAt,
        updatedAt = createdAt,
    )
}
