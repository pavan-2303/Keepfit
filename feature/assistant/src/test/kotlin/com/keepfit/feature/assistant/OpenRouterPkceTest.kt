package com.keepfit.feature.assistant

import com.keepfit.feature.assistant.access.OpenRouterPendingAuthorization
import com.keepfit.feature.assistant.access.OpenRouterPkce
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenRouterPkceTest {
    @Test
    fun createsS256AuthorizationUrlWithoutPuttingVerifierInBrowserUrl() {
        val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        val callback = "http://127.0.0.1:49152/oauth/callback/state-123"

        val request = OpenRouterPkce.createAuthorization(
            callbackUrl = callback,
            verifier = verifier,
            state = "state-123",
            createdAtUtcEpochMillis = 1_000L,
        )

        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            request.pending.codeChallenge,
        )
        assertTrue(request.authorizationUrl.contains("code_challenge_method=S256"))
        assertTrue(request.authorizationUrl.contains("callback_url="))
        assertTrue(!request.authorizationUrl.contains(verifier))
    }

    @Test
    fun callbackFailsClosedForWrongStateOrExpiredTransaction() {
        val pending = OpenRouterPendingAuthorization(
            callbackUrl = "http://127.0.0.1:49152/oauth/callback/right-state",
            state = "right-state",
            codeVerifier = "verifier",
            codeChallenge = "challenge",
            createdAtUtcEpochMillis = 1_000L,
        )

        val wrongState = OpenRouterPkce.validateCallback(
            pending = pending,
            callbackUrl = "http://127.0.0.1:49152/oauth/callback/wrong-state?code=one-time-code",
            nowUtcEpochMillis = 2_000L,
        )
        val expired = OpenRouterPkce.validateCallback(
            pending = pending,
            callbackUrl = "http://127.0.0.1:49152/oauth/callback/right-state?code=one-time-code",
            nowUtcEpochMillis = Instant.ofEpochMilli(1_000L).plusSeconds(601).toEpochMilli(),
        )

        assertTrue(wrongState.isFailure)
        assertTrue(expired.isFailure)
    }
}
