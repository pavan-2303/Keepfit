package com.keepfit.feature.assistant.access

import com.keepfit.core.preferences.AppSettings
import com.keepfit.core.preferences.AppSettingsRepository
import com.keepfit.core.preferences.MeasurementUnit
import com.keepfit.core.preferences.WeightUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantCredentialRecoveryCoordinatorTest {

    @Test
    fun enablingRequiresAvailabilityAndCurrentCredentialBeforePersistingConsent() = runTest {
        val settings = FakeSettingsRepository()
        val credentials = MemoryCredentialStore(token = "current-token")
        val recoveryStore = FakeRecoveryStore(available = true)
        val coordinator = coordinator(settings, credentials, recoveryStore)

        val result = coordinator.setEnabled(true)

        assertTrue(result.isSuccess)
        assertTrue(settings.state.value.credentialRecoveryEnabled)
        assertEquals("current-token", recoveryStore.storedToken)
    }

    @Test
    fun unavailableStoreDoesNotEnableConsent() = runTest {
        val settings = FakeSettingsRepository()
        val coordinator = coordinator(
            settings,
            MemoryCredentialStore(token = "current-token"),
            FakeRecoveryStore(available = false),
        )

        assertTrue(coordinator.setEnabled(true).isFailure)
        assertFalse(settings.state.value.credentialRecoveryEnabled)
    }

    @Test
    fun validRecoveredTokenIsValidatedBeforeLocalStorage() = runTest {
        val settings = FakeSettingsRepository(AppSettings(credentialRecoveryEnabled = true))
        val credentials = MemoryCredentialStore()
        val recoveryStore = FakeRecoveryStore(retrievedToken = "valid-token")
        val coordinator = coordinator(settings, credentials, recoveryStore)

        assertEquals(CredentialRecoveryOutcome.RECOVERED, coordinator.recoverIfNeeded())
        assertEquals("valid-token", credentials.token)
        assertFalse(recoveryStore.deleted)
    }

    @Test
    fun invalidRecoveredTokenIsDeletedAndNeverStoredLocally() = runTest {
        val settings = FakeSettingsRepository(AppSettings(credentialRecoveryEnabled = true))
        val credentials = MemoryCredentialStore()
        val recoveryStore = FakeRecoveryStore(retrievedToken = "invalid-token")
        val coordinator = coordinator(settings, credentials, recoveryStore)

        assertEquals(CredentialRecoveryOutcome.INVALID, coordinator.recoverIfNeeded())
        assertNull(credentials.token)
        assertTrue(recoveryStore.deleted)
    }

    @Test
    fun temporaryProviderFailureKeepsRecoveredTokenForALaterRetry() = runTest {
        val settings = FakeSettingsRepository(AppSettings(credentialRecoveryEnabled = true))
        val credentials = MemoryCredentialStore()
        val recoveryStore = FakeRecoveryStore(retrievedToken = "unavailable-token")

        assertEquals(
            CredentialRecoveryOutcome.UNAVAILABLE,
            coordinator(settings, credentials, recoveryStore).recoverIfNeeded(),
        )
        assertNull(credentials.token)
        assertFalse(recoveryStore.deleted)
    }

    @Test
    fun existingLocalCredentialIsNeverReplaced() = runTest {
        val settings = FakeSettingsRepository(AppSettings(credentialRecoveryEnabled = true))
        val credentials = MemoryCredentialStore(token = "current-token")
        val recoveryStore = FakeRecoveryStore(retrievedToken = "valid-token")

        assertEquals(
            CredentialRecoveryOutcome.LOCAL_CREDENTIAL_PRESENT,
            coordinator(settings, credentials, recoveryStore).recoverIfNeeded(),
        )
        assertEquals("current-token", credentials.token)
    }

    @Test
    fun disablingErasesRecoveryBytesBeforeClearingConsent() = runTest {
        val settings = FakeSettingsRepository(AppSettings(credentialRecoveryEnabled = true))
        val recoveryStore = FakeRecoveryStore()
        val coordinator = coordinator(settings, MemoryCredentialStore(), recoveryStore)

        assertTrue(coordinator.setEnabled(false).isSuccess)
        assertTrue(recoveryStore.deleted)
        assertFalse(settings.state.value.credentialRecoveryEnabled)
    }

    @Test
    fun failedDeletionStillWithdrawsConsentAndPreventsAutomaticReuse() = runTest {
        val settings = FakeSettingsRepository(AppSettings(credentialRecoveryEnabled = true))
        val recoveryStore = FakeRecoveryStore(deleteFails = true)
        val coordinator = coordinator(settings, MemoryCredentialStore(), recoveryStore)

        assertTrue(coordinator.setEnabled(false).isFailure)
        assertFalse(settings.state.value.credentialRecoveryEnabled)
    }

    @Test
    fun disconnectCleanupReturnsSettingsFailureInsteadOfThrowing() = runTest {
        val settings = FakeSettingsRepository(
            initial = AppSettings(credentialRecoveryEnabled = true),
            credentialRecoveryUpdateFails = true,
        )
        val coordinator = coordinator(settings, MemoryCredentialStore(), FakeRecoveryStore())

        val result = coordinator.clearForDisconnect()

        assertTrue(result.isFailure)
    }

    private fun coordinator(
        settings: FakeSettingsRepository,
        credentials: MemoryCredentialStore,
        recoveryStore: FakeRecoveryStore,
    ) = AssistantCredentialRecoveryCoordinator(
        settingsRepository = settings,
        credentialStore = credentials,
        recoveryStore = recoveryStore,
        api = OpenRouterApi(
            object : AssistantHttpTransport {
                override suspend fun execute(request: AssistantHttpRequest): AssistantHttpResponse =
                    when (request.headers["Authorization"]) {
                        "Bearer valid-token" -> AssistantHttpResponse(200, "{\"data\":{\"is_free_tier\":true}}")
                        "Bearer unavailable-token" -> throw AssistantProviderException.Unavailable()
                        else -> AssistantHttpResponse(401, "{}")
                    }
            },
        ),
    )

    private class FakeRecoveryStore(
        private val available: Boolean = true,
        private val retrievedToken: String? = null,
        private val deleteFails: Boolean = false,
    ) : AssistantCredentialRecoveryStore {
        var storedToken: String? = null
        var deleted = false

        override suspend fun isAvailable(): Boolean = available

        override suspend fun store(token: ByteArray) {
            storedToken = token.toString(Charsets.UTF_8)
        }

        override suspend fun retrieve(): ByteArray? = retrievedToken?.toByteArray()

        override suspend fun delete() {
            if (deleteFails) error("delete unavailable")
            deleted = true
        }
    }

    private class FakeSettingsRepository(
        initial: AppSettings = AppSettings(),
        private val credentialRecoveryUpdateFails: Boolean = false,
    ) : AppSettingsRepository {
        val state = MutableStateFlow(initial)

        override fun observeSettings(): Flow<AppSettings> = state

        override suspend fun updateCredentialRecoveryEnabled(enabled: Boolean) {
            if (credentialRecoveryUpdateFails) error("settings unavailable")
            state.value = state.value.copy(credentialRecoveryEnabled = enabled)
        }

        override suspend fun updateUnits(weightUnit: WeightUnit, measurementUnit: MeasurementUnit) = Unit
        override suspend fun updateAssistantSettings(enabled: Boolean) = Unit
        override suspend fun updateRestTimerSeconds(seconds: Int) = Unit
        override suspend fun updateWorkoutReminder(enabled: Boolean, hour: Int, minute: Int) = Unit
        override suspend fun updateTransformationReminder(
            enabled: Boolean,
            dayOfWeekOrdinal: Int,
            hour: Int,
            minute: Int,
        ) = Unit
    }

    private class MemoryCredentialStore(var token: String? = null) : AssistantCredentialStore {
        override fun readToken(): String? = token
        override fun writeToken(token: String) { this.token = token }
        override fun readPendingAuthorization(): OpenRouterPendingAuthorization? = null
        override fun writePendingAuthorization(pending: OpenRouterPendingAuthorization) = Unit
        override fun clearPendingAuthorization() = Unit
        override fun clearCredentials() { token = null }
        override fun isDisclosureAccepted(): Boolean = true
        override fun setDisclosureAccepted(accepted: Boolean) = Unit
    }
}
