package com.keepfit.feature.assistant

import com.keepfit.feature.assistant.data.AssistantNutritionSnapshot
import com.keepfit.feature.assistant.data.AssistantProgressSnapshot
import com.keepfit.feature.assistant.data.AssistantPromptAssembler
import com.keepfit.feature.assistant.data.AssistantRecordSummary
import com.keepfit.feature.assistant.data.AssistantRecentWorkoutSummary
import com.keepfit.feature.assistant.data.AssistantRuntimeConfig
import com.keepfit.feature.assistant.data.AssistantSummaryDataSource
import com.keepfit.feature.assistant.data.AssistantSummaryRepository
import com.keepfit.feature.assistant.data.AssistantStepsSnapshotSummary
import com.keepfit.feature.assistant.data.OllamaAssistantRepository
import com.sun.net.httpserver.HttpServer
import java.time.Clock
import java.net.InetSocketAddress
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OllamaAssistantRepositoryTest {
    @Test
    fun chatRequestsSendExplicitCloudSafeUserAgent() = runBlocking {
        val userAgent = AtomicReference<String?>()
        val server = HttpServer.create(InetSocketAddress(0), 0)
        server.createContext("/api/chat") { exchange ->
            userAgent.set(exchange.requestHeaders.getFirst("User-Agent"))
            val responseBody = """
                {
                  "message": {
                    "role": "assistant",
                    "content": "OK"
                  }
                }
            """.trimIndent()
            val bytes = responseBody.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()

        try {
            val result = repositoryUnderTest().sendChatTurn(
                config = sampleConfig(server.address.port),
                history = emptyList(),
                userMessage = "Reply with only OK.",
            )

            assertTrue(result.isSuccess)
            assertEquals("Keepfit-Android/0.1", userAgent.get())
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun htmlCloudErrorsFallBackToGenericMessage() = runBlocking {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        server.createContext("/api/chat") { exchange ->
            val responseBody = "<!doctype html><title>403</title>403 Forbidden"
            val bytes = responseBody.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "text/html; charset=UTF-8")
            exchange.sendResponseHeaders(403, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()

        try {
            val result = repositoryUnderTest().sendChatTurn(
                config = sampleConfig(server.address.port),
                history = emptyList(),
                userMessage = "Reply with only OK.",
            )

            assertTrue(result.isFailure)
            assertEquals(
                "Assistant access was denied by Ollama Cloud. Check the API key and model access.",
                result.exceptionOrNull()?.message,
            )
        } finally {
            server.stop(0)
        }
    }

    private fun sampleConfig(port: Int): AssistantRuntimeConfig = AssistantRuntimeConfig(
        baseUrl = "http://127.0.0.1:$port/api",
        generalChatModelName = "mistral-large-3:675b",
        reasoningModelName = "qwen3.5:397b",
        apiKey = "test-key",
    )

    private fun repositoryUnderTest(): OllamaAssistantRepository = OllamaAssistantRepository(
        summaryRepository = AssistantSummaryRepository(
            dataSource = object : AssistantSummaryDataSource {
                override suspend fun readRecentWorkouts(): List<AssistantRecentWorkoutSummary> = emptyList()

                override suspend fun readRecords(): List<AssistantRecordSummary> = emptyList()

                override suspend fun readNutritionSnapshot(onDate: LocalDate): AssistantNutritionSnapshot =
                    AssistantNutritionSnapshot(
                        date = onDate,
                        calories = 0.0,
                        proteinGrams = 0.0,
                        carbohydrateGrams = 0.0,
                        fatGrams = 0.0,
                        calorieGoal = null,
                        proteinGoalGrams = null,
                        carbohydrateGoalGrams = null,
                        fatGoalGrams = null,
                        hasEntries = false,
                    )

                override suspend fun readProgressSnapshot(): AssistantProgressSnapshot =
                    AssistantProgressSnapshot(
                        latestMeasurementDate = null,
                        latestWeightKg = null,
                        heightCm = null,
                        bmi = null,
                    )

                override suspend fun readStepsSnapshot(): AssistantStepsSnapshotSummary? = null
            },
            clock = Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC),
        ),
        promptAssembler = AssistantPromptAssembler(),
    )
}
