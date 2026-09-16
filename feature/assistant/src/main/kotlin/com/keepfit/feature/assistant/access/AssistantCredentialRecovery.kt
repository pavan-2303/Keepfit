package com.keepfit.feature.assistant.access

import com.keepfit.core.preferences.AppSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

interface AssistantCredentialRecoveryStore {
    suspend fun isAvailable(): Boolean
    suspend fun store(token: ByteArray)
    suspend fun retrieve(): ByteArray?
    suspend fun delete()
}

enum class CredentialRecoveryOutcome {
    DISABLED,
    UNAVAILABLE,
    NOTHING_TO_RECOVER,
    LOCAL_CREDENTIAL_PRESENT,
    RECOVERED,
    INVALID,
}

interface AssistantCredentialRecoveryController {
    suspend fun isAvailable(): Boolean
    suspend fun setEnabled(enabled: Boolean): Result<Unit>
    suspend fun recoverIfNeeded(): CredentialRecoveryOutcome
    suspend fun backUpCurrentCredentialIfEnabled(): Result<Unit>
    suspend fun clearForDisconnect(): Result<Unit>
}

@Singleton
class AssistantCredentialRecoveryCoordinator @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
    private val credentialStore: AssistantCredentialStore,
    private val recoveryStore: AssistantCredentialRecoveryStore,
    private val api: OpenRouterApi,
) : AssistantCredentialRecoveryController {
    override suspend fun isAvailable(): Boolean = runCatching { recoveryStore.isAvailable() }.getOrDefault(false)

    override suspend fun setEnabled(enabled: Boolean): Result<Unit> = runCatching {
        if (enabled) {
            check(recoveryStore.isAvailable()) {
                "Secure OpenRouter recovery is not available on this device."
            }
            val token = credentialStore.readToken()
                ?: error("Connect OpenRouter before enabling access recovery.")
            recoveryStore.store(token.toByteArray(Charsets.UTF_8))
            settingsRepository.updateCredentialRecoveryEnabled(true)
        } else {
            val deletion = runCatching { recoveryStore.delete() }
            settingsRepository.updateCredentialRecoveryEnabled(false)
            deletion.getOrThrow()
        }
    }

    override suspend fun recoverIfNeeded(): CredentialRecoveryOutcome {
        if (!settingsRepository.observeSettings().first().credentialRecoveryEnabled) {
            return CredentialRecoveryOutcome.DISABLED
        }
        if (credentialStore.readToken() != null) {
            return CredentialRecoveryOutcome.LOCAL_CREDENTIAL_PRESENT
        }
        if (!isAvailable()) return CredentialRecoveryOutcome.UNAVAILABLE

        val tokenBytes = runCatching { recoveryStore.retrieve() }
            .getOrElse { return CredentialRecoveryOutcome.UNAVAILABLE }
            ?: return CredentialRecoveryOutcome.NOTHING_TO_RECOVER
        val token = tokenBytes
            .takeIf { it.size in 1..MAX_TOKEN_BYTES }
            ?.toString(Charsets.UTF_8)
            ?.trim()
            ?.takeIf(String::isNotEmpty)

        if (token == null) {
            runCatching { recoveryStore.delete() }
            return CredentialRecoveryOutcome.INVALID
        }

        return when (val validationFailure = api.inspectCurrentKey(token).exceptionOrNull()) {
            null -> {
                credentialStore.writeToken(token)
                CredentialRecoveryOutcome.RECOVERED
            }
            is AssistantProviderException.InvalidCredential,
            is AssistantProviderException.Revoked -> {
                runCatching { recoveryStore.delete() }
                CredentialRecoveryOutcome.INVALID
            }
            else -> CredentialRecoveryOutcome.UNAVAILABLE
        }
    }

    override suspend fun backUpCurrentCredentialIfEnabled(): Result<Unit> = runCatching {
        if (!settingsRepository.observeSettings().first().credentialRecoveryEnabled) return@runCatching
        check(recoveryStore.isAvailable()) {
            "Secure OpenRouter recovery is not available on this device."
        }
        val token = credentialStore.readToken() ?: return@runCatching
        recoveryStore.store(token.toByteArray(Charsets.UTF_8))
    }

    override suspend fun clearForDisconnect(): Result<Unit> {
        val deletion = runCatching { recoveryStore.delete() }
        val consentUpdate = runCatching {
            settingsRepository.updateCredentialRecoveryEnabled(false)
        }
        return deletion.fold(
            onSuccess = { consentUpdate },
            onFailure = { Result.failure(it) },
        )
    }

    private companion object {
        const val MAX_TOKEN_BYTES = 4 * 1024
    }
}
