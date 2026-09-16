package com.keepfit.core.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ActiveProfileSettingsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val activeProfiles = DataStoreActiveProfileStore(context)
    private val repository = DataStoreAppSettingsRepository(
        context = context,
        reminderScheduler = NoOpReminderScheduler,
        activeProfileStore = activeProfiles,
    )

    @After
    fun tearDown() {
        dataStoreFile().delete()
    }

    @Test
    fun activeProfileSurvivesStoreRecreation() = runBlocking {
        activeProfiles.selectProfile("profile-a")

        assertEquals(
            "profile-a",
            DataStoreActiveProfileStore(context).observeActiveProfileId().first(),
        )
    }

    @Test
    fun profileOwnedSettingsRemainIsolatedAcrossSwitches() = runBlocking {
        activeProfiles.selectProfile("profile-a")
        repository.initializeProfileSettings("profile-a", inheritLegacy = true)
        repository.updateUnits(WeightUnit.LB, MeasurementUnit.IN)

        activeProfiles.selectProfile("profile-b")
        repository.initializeProfileSettings("profile-b", inheritLegacy = false)
        assertEquals(WeightUnit.KG, repository.observeSettings().first().weightUnit)
        assertEquals(MeasurementUnit.CM, repository.observeSettings().first().measurementUnit)

        activeProfiles.selectProfile("profile-a")
        assertEquals(WeightUnit.LB, repository.observeSettings().first().weightUnit)
        assertEquals(MeasurementUnit.IN, repository.observeSettings().first().measurementUnit)
    }

    private fun dataStoreFile(): File =
        context.filesDir.parentFile
            ?.resolve("datastore/keepfit_settings.preferences_pb")
            ?: error("Unable to resolve DataStore file")

    private object NoOpReminderScheduler : ReminderScheduler {
        override suspend fun sync(settings: AppSettings) = Unit
    }
}
