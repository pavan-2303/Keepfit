package com.keepfit.feature.assistant

import com.keepfit.feature.assistant.access.LoopbackOAuthCallbackServer
import java.net.URL
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoopbackOAuthCallbackServerTest {
    @Test
    fun acceptsOneLocalCallbackAndReturnsAReadableBrowserPage() = runBlocking {
        val callback = CompletableDeferred<String>()
        val server = LoopbackOAuthCallbackServer()
        val callbackUrl = server.start(
            state = "one-time-state",
            onCallback = { url -> callback.complete(url) },
        ).getOrThrow()

        val response = URL("$callbackUrl?code=one-time-code").readText()
        val deliveredUrl = withTimeout(2_000L) { callback.await() }

        assertTrue(response.contains("Return to Keepfit"))
        assertEquals("$callbackUrl?code=one-time-code", deliveredUrl)
    }
}
