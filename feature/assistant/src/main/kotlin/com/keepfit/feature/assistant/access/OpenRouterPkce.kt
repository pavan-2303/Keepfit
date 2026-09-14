package com.keepfit.feature.assistant.access

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

data class OpenRouterPendingAuthorization(
    val callbackUrl: String,
    val state: String,
    val codeVerifier: String,
    val codeChallenge: String,
    val createdAtUtcEpochMillis: Long,
)

data class OpenRouterAuthorizationRequest(
    val authorizationUrl: String,
    val pending: OpenRouterPendingAuthorization,
)

object OpenRouterPkce {
    private const val authorizationEndpoint = "https://openrouter.ai/auth"
    private const val transactionLifetimeMillis = 10 * 60 * 1_000L

    fun randomUrlSafeValue(byteCount: Int = 32): String {
        val bytes = ByteArray(byteCount)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun createAuthorization(
        callbackUrl: String,
        verifier: String = randomUrlSafeValue(48),
        state: String = randomUrlSafeValue(),
        createdAtUtcEpochMillis: Long = System.currentTimeMillis(),
    ): OpenRouterAuthorizationRequest {
        require(verifier.length in 43..128) { "PKCE verifier must contain 43 to 128 characters." }
        val challenge = codeChallenge(verifier)
        val pending = OpenRouterPendingAuthorization(
            callbackUrl = callbackUrl,
            state = state,
            codeVerifier = verifier,
            codeChallenge = challenge,
            createdAtUtcEpochMillis = createdAtUtcEpochMillis,
        )
        val encodedCallback = URLEncoder.encode(callbackUrl, StandardCharsets.UTF_8.name())
        val authorizationUrl = "$authorizationEndpoint?callback_url=$encodedCallback" +
            "&code_challenge=$challenge&code_challenge_method=S256"
        return OpenRouterAuthorizationRequest(authorizationUrl, pending)
    }

    fun validateCallback(
        pending: OpenRouterPendingAuthorization,
        callbackUrl: String,
        nowUtcEpochMillis: Long = System.currentTimeMillis(),
    ): Result<String> = runCatching {
        if (nowUtcEpochMillis - pending.createdAtUtcEpochMillis !in 0..transactionLifetimeMillis) {
            error("OpenRouter authorization expired. Start the connection again.")
        }

        val expected = URI(pending.callbackUrl)
        val actual = URI(callbackUrl)
        val expectedPathPrefix = expected.path.substringBeforeLast('/')
        val actualPathPrefix = actual.path.substringBeforeLast('/')
        val actualState = actual.path.substringAfterLast('/')
        val stateMatches = MessageDigest.isEqual(
            pending.state.toByteArray(StandardCharsets.UTF_8),
            actualState.toByteArray(StandardCharsets.UTF_8),
        )
        if (
            expected.scheme != actual.scheme ||
            expected.host != actual.host ||
            expected.port != actual.port ||
            expectedPathPrefix != actualPathPrefix ||
            !stateMatches
        ) {
            error("OpenRouter authorization state did not match. Start the connection again.")
        }

        queryParameters(actual.rawQuery)["code"]?.takeIf { it.isNotBlank() }
            ?: error("OpenRouter did not return an authorization code.")
    }

    private fun codeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(StandardCharsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    private fun queryParameters(query: String?): Map<String, String> = query.orEmpty()
        .split('&')
        .mapNotNull { part ->
            val pieces = part.split('=', limit = 2)
            pieces.firstOrNull()?.takeIf { it.isNotBlank() }?.let { key ->
                key to java.net.URLDecoder.decode(pieces.getOrElse(1) { "" }, StandardCharsets.UTF_8.name())
            }
        }
        .toMap()
}
