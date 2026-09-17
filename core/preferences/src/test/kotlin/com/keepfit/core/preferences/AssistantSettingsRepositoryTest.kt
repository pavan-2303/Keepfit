package com.keepfit.core.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class AssistantSettingsRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val repository = DataStoreAppSettingsRepository(
        context = context,
        reminderScheduler = NoOpReminderScheduler,
    )

    @After
    fun tearDown() {
        dataStoreFile().delete()
    }

    @Test
    fun observesAssistantSettingsDefaults() = runBlocking {
        val settings = repository.observeSettings()
            .first()

        assertFalse(settings.assistant.enabled)
    }

    @Test
    fun updatesAssistantSettingsRoundTrip() = runBlocking {
        repository.updateAssistantSettings(
            enabled = true,
        )

        val settings = repository.observeSettings()
            .first()

        assertEquals(
            AssistantSettings(
                enabled = true,
            ),
            settings.assistant,
        )
    }

    @Test
    fun credentialRecoveryConsentDefaultsOffAndPersistsExplicitOptIn() = runBlocking {
        assertFalse(repository.observeSettings().first().credentialRecoveryEnabled)

        repository.updateCredentialRecoveryEnabled(true)

        assertEquals(true, repository.observeSettings().first().credentialRecoveryEnabled)
    }

    @Test
    fun reducedMotionDefaultsOffAndPersistsExplicitPreference() = runBlocking {
        assertFalse(repository.observeSettings().first().reduceMotion)

        repository.updateReduceMotion(true)

        assertEquals(true, repository.observeSettings().first().reduceMotion)
    }

    private fun dataStoreFile(): File =
        context.filesDir.parentFile
            ?.resolve("datastore/keepfit_settings.preferences_pb")
            ?: error("Unable to resolve DataStore file")

    private object NoOpReminderScheduler : ReminderScheduler {
        override suspend fun sync(settings: AppSettings) = Unit
    }
}
