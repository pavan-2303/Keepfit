package com.keepfit.feature.assistant.access

import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AssistantAccessStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    INVALID,
    REVOKED,
    PROVIDER_LIMIT_REACHED,
    PROVIDER_UNAVAILABLE,
}

data class AssistantAccessState(
    val status: AssistantAccessStatus = AssistantAccessStatus.DISCONNECTED,
    val hasCredential: Boolean = false,
    val disclosureAccepted: Boolean = false,
    val isFreeTier: Boolean? = null,
    val providerLimitRemaining: Double? = null,
    val credentialLabel: String? = null,
    val message: String? = null,
)

interface AssistantAccessController {
    val state: StateFlow<AssistantAccessState>
    fun acknowledgeDisclosure()
    suspend fun beginAuthorization(): Result<String>
    suspend fun resumePendingAuthorization()
    suspend fun inspectConnection(): Result<OpenRouterKeyMetadata>
    fun cancelAuthorization(message: String = "OpenRouter authorization was cancelled.")
    fun disconnect()
    fun reserveInferenceRequest(): Result<String>
    fun recordProviderSuccess()
    fun recordProviderFailure(exception: Throwable)
}

@Singleton
class OpenRouterAccessManager @Inject constructor(
    private val credentialStore: AssistantCredentialStore,
    private val api: OpenRouterApi,
    private val callbackServer: OAuthCallbackServer,
) : AssistantAccessController {
    private val _state = MutableStateFlow(
        AssistantAccessState(
            status = if (credentialStore.readToken() == null) {
                AssistantAccessStatus.DISCONNECTED
            } else {
                AssistantAccessStatus.CONNECTED
            },
            hasCredential = credentialStore.readToken() != null,
            disclosureAccepted = credentialStore.isDisclosureAccepted(),
        ),
    )
    override val state: StateFlow<AssistantAccessState> = _state.asStateFlow()

    override fun acknowledgeDisclosure() {
        credentialStore.setDisclosureAccepted(true)
        _state.value = _state.value.copy(disclosureAccepted = true)
    }

    override suspend fun beginAuthorization(): Result<String> {
        if (!credentialStore.isDisclosureAccepted()) {
            return Result.failure(IllegalStateException("Review the privacy disclosure before connecting."))
        }
        _state.value = currentState(AssistantAccessStatus.CONNECTING, "Waiting for OpenRouter authorization.")
        val stateValue = OpenRouterPkce.randomUrlSafeValue()
        val verifier = OpenRouterPkce.randomUrlSafeValue(48)
        val callback = callbackServer.start(
            state = stateValue,
            onCallback = { callbackUrl -> completeAuthorization(callbackUrl) },
            onFailure = { handleCallbackFailure() },
        ).getOrElse { exception ->
            _state.value = currentState(
                AssistantAccessStatus.PROVIDER_UNAVAILABLE,
                "Keepfit could not open its secure local callback. Try again.",
            )
            return Result.failure(exception)
        }
        val request = OpenRouterPkce.createAuthorization(
            callbackUrl = callback,
            verifier = verifier,
            state = stateValue,
        )
        credentialStore.writePendingAuthorization(request.pending)
        return Result.success(request.authorizationUrl)
    }

    override suspend fun resumePendingAuthorization() {
        val pending = credentialStore.readPendingAuthorization() ?: return
        _state.value = currentState(AssistantAccessStatus.CONNECTING, "Resuming OpenRouter authorization.")
        val port = runCatching { URI(pending.callbackUrl).port }.getOrNull()?.takeIf { it > 0 }
        callbackServer.start(
            state = pending.state,
            preferredPort = port,
            onCallback = { callbackUrl -> completeAuthorization(callbackUrl) },
            onFailure = { handleCallbackFailure() },
        ).onFailure {
            credentialStore.clearPendingAuthorization()
            _state.value = currentState(
                AssistantAccessStatus.INVALID,
                "The interrupted authorization could not resume. Connect again.",
            )
        }
    }

    override suspend fun inspectConnection(): Result<OpenRouterKeyMetadata> {
        val token = credentialStore.readToken() ?: return Result.failure(
            IllegalStateException("Connect an OpenRouter account first."),
        )
        _state.value = currentState(AssistantAccessStatus.CONNECTING, "Checking OpenRouter access.")
        return api.inspectCurrentKey(token)
            .onSuccess { metadata ->
                _state.value = currentState(AssistantAccessStatus.CONNECTED).copy(
                    isFreeTier = metadata.isFreeTier,
                    providerLimitRemaining = metadata.limitRemaining,
                    credentialLabel = metadata.label,
                    message = "OpenRouter access is ready.",
                )
            }
            .onFailure(::recordProviderFailure)
    }

    override fun cancelAuthorization(message: String) {
        callbackServer.stop()
        credentialStore.clearPendingAuthorization()
        _state.value = currentState(
            if (credentialStore.readToken() == null) AssistantAccessStatus.DISCONNECTED else AssistantAccessStatus.CONNECTED,
            message,
        )
    }

    override fun disconnect() {
        callbackServer.stop()
        credentialStore.clearCredentials()
        _state.value = currentState(
            AssistantAccessStatus.DISCONNECTED,
            "OpenRouter access was removed from this device.",
        ).copy(
            hasCredential = false,
            isFreeTier = null,
            providerLimitRemaining = null,
            credentialLabel = null,
        )
    }

    override fun reserveInferenceRequest(): Result<String> {
        val token = credentialStore.readToken() ?: return Result.failure(
            AssistantProviderException.InvalidCredential("Connect an OpenRouter account before using the assistant."),
        )
        _state.value = _state.value.copy(
            status = AssistantAccessStatus.CONNECTED,
            message = null,
        )
        return Result.success(token)
    }

    override fun recordProviderSuccess() {
        _state.value = currentState(AssistantAccessStatus.CONNECTED)
    }

    override fun recordProviderFailure(exception: Throwable) {
        val status = when (exception) {
            is AssistantProviderException.InvalidCredential -> AssistantAccessStatus.INVALID
            is AssistantProviderException.Revoked -> AssistantAccessStatus.REVOKED
            is AssistantProviderException.Quota -> AssistantAccessStatus.PROVIDER_LIMIT_REACHED
            else -> AssistantAccessStatus.PROVIDER_UNAVAILABLE
        }
        _state.value = currentState(status, exception.message)
    }

    private suspend fun completeAuthorization(callbackUrl: String) {
        val pending = credentialStore.readPendingAuthorization()
        if (pending == null) {
            _state.value = currentState(
                AssistantAccessStatus.INVALID,
                "OpenRouter authorization was not expected. Connect again.",
            )
            return
        }
        val codeResult = OpenRouterPkce.validateCallback(pending, callbackUrl)
        credentialStore.clearPendingAuthorization()
        codeResult.fold(
            onSuccess = { code ->
                api.exchangeCode(code, pending.codeVerifier)
                    .onSuccess { token ->
                        credentialStore.writeToken(token)
                        _state.value = currentState(
                            AssistantAccessStatus.CONNECTED,
                            "OpenRouter is connected. Check access to inspect the account.",
                        )
                    }
                    .onFailure(::recordProviderFailure)
            },
            onFailure = { exception ->
                _state.value = currentState(AssistantAccessStatus.INVALID, exception.message)
            },
        )
    }

    private fun handleCallbackFailure() {
        credentialStore.clearPendingAuthorization()
        _state.value = currentState(
            AssistantAccessStatus.INVALID,
            "OpenRouter authorization timed out. Connect again when you are ready.",
        )
    }

    private fun currentState(status: AssistantAccessStatus, message: String? = null): AssistantAccessState =
        _state.value.copy(
            status = status,
            hasCredential = credentialStore.readToken() != null,
            disclosureAccepted = credentialStore.isDisclosureAccepted(),
            message = message,
        )
}
