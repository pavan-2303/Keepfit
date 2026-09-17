package com.keepfit.app.ui.home

import com.keepfit.core.preferences.AppSettings
import com.keepfit.core.preferences.AppSettingsRepository
import com.keepfit.core.preferences.MeasurementUnit
import com.keepfit.core.preferences.WeightUnit
import com.keepfit.feature.assistant.access.AssistantCredentialRecoveryController
import com.keepfit.feature.assistant.access.AssistantAccessController
import com.keepfit.feature.assistant.access.AssistantAccessState
import com.keepfit.feature.assistant.access.CredentialRecoveryOutcome
import com.keepfit.feature.assistant.access.OpenRouterKeyMetadata
import com.keepfit.feature.assistant.data.AssistantChatMessage
import com.keepfit.feature.assistant.data.AssistantDraftInput
import com.keepfit.feature.assistant.data.AssistantDraftWorkoutPlan
import com.keepfit.feature.assistant.data.AssistantProgressSummary
import com.keepfit.feature.assistant.data.AssistantRepository
import com.keepfit.feature.assistant.data.AssistantRuntimeConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeShellViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var settingsRepository: FakeAppSettingsRepository
    private lateinit var assistantRepository: FakeAssistantRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        settingsRepository = FakeAppSettingsRepository()
        assistantRepository = FakeAssistantRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun reportsSuccessfulAssistantConnectionTest() = runTest(dispatcher) {
        settingsRepository.updateAssistantSettings(enabled = true)
        val viewModel = HomeShellViewModel(
            settingsRepository = settingsRepository,
            buildTimeConfig = sampleConfig(),
            assistantRepository = assistantRepository,
            credentialRecovery = FakeCredentialRecoveryController,
            assistantAccess = FakeAssistantAccessController(),
        )
        advanceUntilIdle()

        viewModel.testAssistantConnection()
        advanceUntilIdle()

        assertEquals("Assistant connection successful.", viewModel.assistantConnectionMessage.value)
    }

    @Test
    fun reportsValidationFailureForMalformedEndpoint() = runTest(dispatcher) {
        settingsRepository.updateAssistantSettings(enabled = true)
        val viewModel = HomeShellViewModel(
            settingsRepository = settingsRepository,
            buildTimeConfig = sampleConfig(baseUrl = "not-a-url"),
            assistantRepository = assistantRepository,
            credentialRecovery = FakeCredentialRecoveryController,
            assistantAccess = FakeAssistantAccessController(),
        )
        advanceUntilIdle()

        viewModel.testAssistantConnection()
        advanceUntilIdle()

        assertEquals(
            "Assistant endpoint must be a valid http or https URL.",
            viewModel.assistantConnectionMessage.value,
        )
    }

    @Test
    fun reportsValidationFailureWhenCloudApiKeyIsMissing() = runTest(dispatcher) {
        settingsRepository.updateAssistantSettings(enabled = true)
        val viewModel = HomeShellViewModel(
            settingsRepository = settingsRepository,
            buildTimeConfig = sampleConfig(apiKey = ""),
            assistantRepository = assistantRepository,
            credentialRecovery = FakeCredentialRecoveryController,
            assistantAccess = FakeAssistantAccessController(),
        )
        advanceUntilIdle()

        viewModel.testAssistantConnection()
        advanceUntilIdle()

        assertEquals(
            "Ollama Cloud requires an API key.",
            viewModel.assistantConnectionMessage.value,
        )
    }

    @Test
    fun coachConnectionIsAvailableWithoutASeparateEnableToggle() = runTest(dispatcher) {
        val viewModel = HomeShellViewModel(
            settingsRepository = settingsRepository,
            buildTimeConfig = sampleConfig(),
            assistantRepository = assistantRepository,
            credentialRecovery = FakeCredentialRecoveryController,
            assistantAccess = FakeAssistantAccessController(),
        )
        advanceUntilIdle()

        viewModel.testAssistantConnection()
        advanceUntilIdle()

        assertEquals("Assistant connection successful.", viewModel.assistantConnectionMessage.value)
    }

    @Test
    fun recoveredCredentialRefreshesTheAssistantAccessState() = runTest(dispatcher) {
        val accessController = FakeAssistantAccessController()
        val viewModel = HomeShellViewModel(
            settingsRepository = settingsRepository,
            buildTimeConfig = sampleConfig(),
            assistantRepository = assistantRepository,
            credentialRecovery = object : AssistantCredentialRecoveryController by FakeCredentialRecoveryController {
                override suspend fun recoverIfNeeded() = CredentialRecoveryOutcome.RECOVERED
            },
            assistantAccess = accessController,
        )

        advanceUntilIdle()

        assertEquals(1, accessController.synchronizationCount)
        assertEquals("OpenRouter access was recovered and verified.", viewModel.assistantConnectionMessage.value)
    }

    @Test
    fun exposesReducedMotionPreferenceToTheShell() = runTest(dispatcher) {
        settingsRepository.updateReduceMotion(true)
        val viewModel = HomeShellViewModel(
            settingsRepository = settingsRepository,
            buildTimeConfig = sampleConfig(),
            assistantRepository = assistantRepository,
            credentialRecovery = FakeCredentialRecoveryController,
            assistantAccess = FakeAssistantAccessController(),
        )

        advanceUntilIdle()

        assertTrue(viewModel.reduceMotion.value)
    }

    private class FakeAppSettingsRepository : AppSettingsRepository {
        private val settings = MutableStateFlow(AppSettings())

        override fun observeSettings(): Flow<AppSettings> = settings

        override suspend fun updateUnits(weightUnit: WeightUnit, measurementUnit: MeasurementUnit) = Unit

        override suspend fun updateAssistantSettings(enabled: Boolean) {
            settings.value = settings.value.copy(
                assistant = settings.value.assistant.copy(enabled = enabled),
            )
        }

        override suspend fun updateRestTimerSeconds(seconds: Int) = Unit

        override suspend fun updateReduceMotion(enabled: Boolean) {
            settings.value = settings.value.copy(reduceMotion = enabled)
        }

        override suspend fun updateWorkoutReminder(enabled: Boolean, hour: Int, minute: Int) = Unit

        override suspend fun updateTransformationReminder(
            enabled: Boolean,
            dayOfWeekOrdinal: Int,
            hour: Int,
            minute: Int,
        ) = Unit
    }

    private class FakeAssistantRepository : AssistantRepository {
        override suspend fun testConnection(config: AssistantRuntimeConfig): Result<Unit> = Result.success(Unit)

        override suspend fun sendChatTurn(
            config: AssistantRuntimeConfig,
            history: List<AssistantChatMessage>,
            userMessage: String,
        ): Result<AssistantChatMessage> = error("Not used in this test.")

        override suspend fun generateProgressSummary(
            config: AssistantRuntimeConfig,
        ): Result<AssistantProgressSummary> = error("Not used in this test.")

        override suspend fun requestDraftPlan(
            config: AssistantRuntimeConfig,
            input: AssistantDraftInput,
        ): Result<AssistantDraftWorkoutPlan> = error("Not used in this test.")
    }

    private object FakeCredentialRecoveryController : AssistantCredentialRecoveryController {
        override suspend fun isAvailable() = false
        override suspend fun setEnabled(enabled: Boolean) = Result.success(Unit)
        override suspend fun recoverIfNeeded() = CredentialRecoveryOutcome.DISABLED
        override suspend fun backUpCurrentCredentialIfEnabled() = Result.success(Unit)
        override suspend fun clearForDisconnect() = Result.success(Unit)
    }

    private class FakeAssistantAccessController : AssistantAccessController {
        override val state: StateFlow<AssistantAccessState> = MutableStateFlow(AssistantAccessState())
        var synchronizationCount = 0

        override fun synchronizeCredentialState() {
            synchronizationCount += 1
        }

        override fun acknowledgeDisclosure() = Unit
        override suspend fun beginAuthorization() = Result.success("")
        override suspend fun resumePendingAuthorization() = Unit
        override suspend fun inspectConnection() = Result.success(
            OpenRouterKeyMetadata(label = null, isFreeTier = true, limitRemaining = null),
        )
        override fun cancelAuthorization(message: String) = Unit
        override suspend fun disconnect() = Unit
        override fun reserveInferenceRequest() = Result.success("")
        override fun recordProviderSuccess() = Unit
        override fun recordProviderFailure(exception: Throwable) = Unit
    }

    private fun sampleConfig(
        baseUrl: String = "https://ollama.com/api",
        generalChatModelName: String = "mistral-large-3:675b",
        reasoningModelName: String = "qwen3.5:397b",
        apiKey: String = "secret-token",
    ) = AssistantRuntimeConfig(
        baseUrl = baseUrl,
        generalChatModelName = generalChatModelName,
        reasoningModelName = reasoningModelName,
        apiKey = apiKey,
    )
}
