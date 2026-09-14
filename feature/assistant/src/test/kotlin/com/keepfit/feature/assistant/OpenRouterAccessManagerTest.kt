package com.keepfit.feature.assistant

import com.keepfit.feature.assistant.access.AssistantAccessStatus
import com.keepfit.feature.assistant.access.AssistantCredentialStore
import com.keepfit.feature.assistant.access.AssistantHttpRequest
import com.keepfit.feature.assistant.access.AssistantHttpResponse
import com.keepfit.feature.assistant.access.AssistantHttpTransport
import com.keepfit.feature.assistant.access.OAuthCallbackServer
import com.keepfit.feature.assistant.access.OpenRouterAccessManager
import com.keepfit.feature.assistant.access.OpenRouterApi
import com.keepfit.feature.assistant.access.OpenRouterPendingAuthorization
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenRouterAccessManagerTest {
    @Test
    fun disclosureAuthorizationExchangeAndDisconnectOwnCredentialLifecycle() = runTest {
        val store = MemoryCredentialStore()
        val callbackServer = FakeCallbackServer()
        val manager = manager(store, callbackServer)

        assertTrue(manager.beginAuthorization().isFailure)

        manager.acknowledgeDisclosure()
        val browserUrl = manager.beginAuthorization().getOrThrow()
        assertTrue(browserUrl.startsWith("https://openrouter.ai/auth?"))
        assertEquals(AssistantAccessStatus.CONNECTING, manager.state.value.status)

        callbackServer.deliver("${store.pending!!.callbackUrl}?code=one-time")

        assertEquals("user-controlled-key", store.token)
        assertNull(store.pending)
        assertEquals(AssistantAccessStatus.CONNECTED, manager.state.value.status)

        manager.disconnect()
        assertNull(store.token)
        assertEquals(AssistantAccessStatus.DISCONNECTED, manager.state.value.status)
    }

    @Test
    fun mismatchedCallbackFailsClosedAndConsumesPendingTransaction() = runTest {
        val store = MemoryCredentialStore(accepted = true)
        val callbackServer = FakeCallbackServer()
        val manager = manager(store, callbackServer)

        manager.beginAuthorization().getOrThrow()
        val pending = store.pending!!
        callbackServer.deliver(
            pending.callbackUrl.replace(pending.state, "wrong-state") + "?code=one-time",
        )

        assertNull(store.token)
        assertNull(store.pending)
        assertEquals(AssistantAccessStatus.INVALID, manager.state.value.status)
    }

    @Test
    fun userOwnedAccessIsNotBlockedByALocalDailyAllowance() = runTest {
        val store = MemoryCredentialStore(accepted = true).apply {
            token = "user-controlled-key"
        }
        val manager = manager(store, FakeCallbackServer())

        repeat(25) {
            assertEquals("user-controlled-key", manager.reserveInferenceRequest().getOrThrow())
        }
        assertEquals(AssistantAccessStatus.CONNECTED, manager.state.value.status)
    }

    private fun manager(
        store: MemoryCredentialStore,
        callbackServer: FakeCallbackServer,
    ) = OpenRouterAccessManager(
        credentialStore = store,
        api = OpenRouterApi(
            object : AssistantHttpTransport {
                override suspend fun execute(request: AssistantHttpRequest): AssistantHttpResponse =
                    when {
                        request.url.endsWith("/auth/keys") -> AssistantHttpResponse(200, "{\"key\":\"user-controlled-key\"}")
                        request.url.endsWith("/key") -> AssistantHttpResponse(200, "{\"data\":{\"is_free_tier\":true}}")
                        else -> AssistantHttpResponse(500, null)
                    }
            },
        ),
        callbackServer = callbackServer,
    )

    private class MemoryCredentialStore(
        var accepted: Boolean = false,
    ) : AssistantCredentialStore {
        var token: String? = null
        var pending: OpenRouterPendingAuthorization? = null

        override fun readToken(): String? = token
        override fun writeToken(token: String) { this.token = token }
        override fun readPendingAuthorization(): OpenRouterPendingAuthorization? = pending
        override fun writePendingAuthorization(pending: OpenRouterPendingAuthorization) { this.pending = pending }
        override fun clearPendingAuthorization() { pending = null }
        override fun clearCredentials() { token = null; pending = null }
        override fun isDisclosureAccepted(): Boolean = accepted
        override fun setDisclosureAccepted(accepted: Boolean) { this.accepted = accepted }
    }

    private class FakeCallbackServer : OAuthCallbackServer {
        private var callback: (suspend (String) -> Unit)? = null

        override suspend fun start(
            state: String,
            preferredPort: Int?,
            onCallback: suspend (String) -> Unit,
            onFailure: suspend (Throwable) -> Unit,
        ): Result<String> {
            callback = onCallback
            return Result.success("http://127.0.0.1:${preferredPort ?: 49152}/oauth/callback/$state")
        }

        suspend fun deliver(url: String) {
            checkNotNull(callback).invoke(url)
        }

        override fun stop() = Unit
    }

}
