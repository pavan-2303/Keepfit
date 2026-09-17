package com.keepfit.feature.assistant.data

import com.google.gson.Gson
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class OllamaAssistantRepository @Inject constructor(
    private val summaryRepository: AssistantSummaryRepository,
    private val promptAssembler: AssistantPromptAssembler,
) : AssistantRepository {
    private val gson = Gson()

    override suspend fun testConnection(config: AssistantRuntimeConfig): Result<Unit> =
        executeChatRequest(
            config = config,
            purpose = AssistantModelPurpose.CHAT,
            history = emptyList(),
            userMessage = "Reply with only OK.",
        ).map { Unit }

    override suspend fun sendChatTurn(
        config: AssistantRuntimeConfig,
        history: List<AssistantChatMessage>,
        userMessage: String,
    ): Result<AssistantChatMessage> = executeChatRequest(
        config = config,
        purpose = AssistantModelPurpose.CHAT,
        history = history,
        userMessage = userMessage,
    )

    override suspend fun generateProgressSummary(
        config: AssistantRuntimeConfig,
    ): Result<AssistantProgressSummary> {
        val prompt = promptAssembler.buildProgressSummaryPrompt(
            summaryRepository.loadProgressSummary(),
        )
        return executeChatRequest(
            config = config,
            purpose = AssistantModelPurpose.REASONING,
            history = emptyList(),
            userMessage = prompt,
        ).map { response ->
            AssistantProgressSummary(
                title = "Progress summary",
                summary = response.content,
            )
        }
    }

    override suspend fun requestDraftPlan(
        config: AssistantRuntimeConfig,
        input: AssistantDraftInput,
    ): Result<AssistantDraftWorkoutPlan> {
        val prompt = promptAssembler.buildDraftPlanPrompt(
            summaryRepository.loadProgressSummary(),
            input,
        )
        return executeChatRequest(
            config = config,
            purpose = AssistantModelPurpose.REASONING,
            history = emptyList(),
            userMessage = prompt,
        ).fold(
            onSuccess = { response ->
                parseOllamaDraftPlanResponse(response.content)
            },
            onFailure = { Result.failure(it) },
        )
    }

    private suspend fun executeChatRequest(
        config: AssistantRuntimeConfig,
        purpose: AssistantModelPurpose,
        history: List<AssistantChatMessage>,
        userMessage: String,
    ): Result<AssistantChatMessage> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = openConnection(config)
            val request = OllamaChatRequest(
                model = config.modelNameFor(purpose),
                messages = history.map(::assistantMessageToOllamaDto) + OllamaChatMessageDto(
                    role = "user",
                    content = userMessage,
                ),
            )

            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(gson.toJson(request))
            }

            val statusCode = connection.responseCode
            val responseBody = readResponseBody(connection, statusCode)
            if (statusCode !in 200..299) {
                return@withContext Result.failure(
                    IllegalStateException(mapHttpFailure(statusCode, responseBody)),
                )
            }

            parseOllamaChatResponse(
                json = responseBody.orEmpty(),
                createdAtUtcEpochMillis = System.currentTimeMillis(),
            )
        } catch (_: UnknownHostException) {
            Result.failure(IllegalStateException("Assistant host could not be resolved. Check the endpoint."))
        } catch (_: ConnectException) {
            Result.failure(
                IllegalStateException("Assistant endpoint is unreachable. Check the API URL or your network connection."),
            )
        } catch (_: SocketTimeoutException) {
            Result.failure(IllegalStateException("Assistant request timed out. Check the endpoint and try again."))
        } catch (exception: IOException) {
            Result.failure(
                IllegalStateException(
                    exception.message ?: "Assistant request failed.",
                ),
            )
        } finally {
            connection?.disconnect()
        }
    }

    private fun openConnection(config: AssistantRuntimeConfig): HttpURLConnection =
        (URL(buildChatEndpoint(config.baseUrl)).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 60_000
            doInput = true
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", OLLAMA_CLOUD_USER_AGENT)
            config.apiKey?.takeIf { it.isNotBlank() }?.let {
                setRequestProperty("Authorization", "Bearer $it")
            }
        }

    private fun buildChatEndpoint(baseUrl: String): String {
        val normalizedBaseUrl = baseUrl.trimEnd('/')
        return if (normalizedBaseUrl.endsWith("/chat")) {
            normalizedBaseUrl
        } else {
            "$normalizedBaseUrl/chat"
        }
    }

    private fun readResponseBody(
        connection: HttpURLConnection,
        statusCode: Int,
    ): String? {
        val stream = if (statusCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        } ?: return null

        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun mapHttpFailure(statusCode: Int, responseBody: String?): String {
        val structuredError = parseOllamaErrorMessage(responseBody)
        return when (statusCode) {
            HttpURLConnection.HTTP_UNAUTHORIZED,
            HttpURLConnection.HTTP_FORBIDDEN,
            -> structuredError ?: "Assistant access was denied by Ollama Cloud. Check the API key and model access."

            in 500..599 -> structuredError ?: "Assistant server error ($statusCode)."
            else -> structuredError ?: "Assistant request failed ($statusCode)."
        }
    }

    private companion object {
        const val OLLAMA_CLOUD_USER_AGENT = "Keepfit-Android/0.1"
    }
}
