package com.keepfit.feature.assistant

import com.keepfit.feature.assistant.access.AssistantHttpRequest
import com.keepfit.feature.assistant.access.AssistantHttpResponse
import com.keepfit.feature.assistant.access.AssistantHttpTransport
import com.keepfit.feature.assistant.access.OpenRouterApi
import com.keepfit.feature.assistant.data.AssistantMessageRole
import com.keepfit.feature.assistant.coaching.CoachingIntent
import com.keepfit.feature.assistant.coaching.CoachingToolContract
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenRouterApiTest {
    @Test
    fun exchangeUsesVerifierAndNeverUsesAClientSecret() = runBlocking {
        val transport = RecordingTransport(
            AssistantHttpResponse(200, "{\"key\":\"user-key\"}"),
        )
        val api = OpenRouterApi(transport)

        val result = api.exchangeCode("one-time-code", "pkce-verifier")

        assertEquals("user-key", result.getOrThrow())
        assertTrue(transport.lastRequest!!.body!!.contains("\"code_verifier\":\"pkce-verifier\""))
        assertTrue(!transport.lastRequest!!.body!!.contains("client_secret"))
    }

    @Test
    fun chatUsesNamedFreeModelAndStrictPrivacyRouting() = runBlocking {
        val transport = RecordingTransport(
            AssistantHttpResponse(
                200,
                "{\"model\":\"inclusionai/ling-3.0-flash-sante:free\",\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Keep going.\"}}]}",
            ),
        )
        val api = OpenRouterApi(transport)

        val result = api.chat(
            token = "user-key",
            history = emptyList(),
            userMessage = "How should I train today?",
            createdAtUtcEpochMillis = 10L,
        )

        assertEquals(AssistantMessageRole.ASSISTANT, result.getOrThrow().role)
        assertEquals("Bearer user-key", transport.lastRequest!!.headers["Authorization"])
        assertTrue(transport.lastRequest!!.body!!.contains("\"model\":\"inclusionai/ling-3.0-flash-sante:free\""))
        assertTrue(transport.lastRequest!!.body!!.contains("\"data_collection\":\"deny\""))
        assertTrue(transport.lastRequest!!.body!!.contains("\"zdr\":true"))
    }

    @Test
    fun currentKeyInspectionReturnsOnlySafeMetadata() = runBlocking {
        val transport = RecordingTransport(
            AssistantHttpResponse(
                200,
                "{\"data\":{\"label\":\"sk-or-v1-au7...890\",\"is_free_tier\":true,\"limit_remaining\":4.5}}",
            ),
        )
        val api = OpenRouterApi(transport)

        val metadata = api.inspectCurrentKey("user-key").getOrThrow()

        assertTrue(metadata.isFreeTier)
        assertEquals(4.5, metadata.limitRemaining!!, 0.0)
        assertEquals("Authorized key ending .890", metadata.label)
        assertTrue(transport.lastRequest!!.url.endsWith("/api/v1/key"))
    }

    @Test
    fun structuredCoachingForcesOneNamedToolAndPrivateRouting() = runBlocking {
        val transport = RecordingTransport(
            AssistantHttpResponse(
                200,
                """{"model":"inclusionai/ling-3.0-flash-sante:free","choices":[{"message":{"role":"assistant","tool_calls":[{"id":"call-1","type":"function","function":{"name":"summarize_week","arguments":"{\"title\":\"Week\",\"observed\":\"Three sessions.\",\"current\":\"Consistent.\",\"proposed\":\"Repeat the week.\",\"reason\":\"It is working.\"}"}}]}}]}""",
            ),
        )
        val api = OpenRouterApi(transport)

        val call = api.requestToolCall(
            token = "user-key",
            systemPrompt = "Use the supplied data only.",
            userPrompt = "Summarize this week.",
            contract = CoachingToolContract.forIntent(CoachingIntent.WEEKLY_SUMMARY),
        ).getOrThrow()

        assertEquals("summarize_week", call.name)
        val body = transport.lastRequest!!.body!!
        assertTrue(body.contains("\"parallel_tool_calls\":false"))
        assertTrue(body.contains("\"name\":\"summarize_week\""))
        assertTrue(body.contains("\"data_collection\":\"deny\""))
        assertTrue(body.contains("\"zdr\":true"))
    }

    private class RecordingTransport(
        private val response: AssistantHttpResponse,
    ) : AssistantHttpTransport {
        var lastRequest: AssistantHttpRequest? = null

        override suspend fun execute(request: AssistantHttpRequest): AssistantHttpResponse {
            lastRequest = request
            return response
        }
    }
}
