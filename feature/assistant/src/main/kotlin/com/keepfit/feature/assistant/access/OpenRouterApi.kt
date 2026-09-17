package com.keepfit.feature.assistant.access

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.keepfit.feature.assistant.data.AssistantChatMessage
import com.keepfit.feature.assistant.data.AssistantMessageRole
import com.keepfit.feature.assistant.coaching.CoachingToolCall
import com.keepfit.feature.assistant.coaching.CoachingToolContract
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

data class AssistantHttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val readTimeoutMillis: Int = 60_000,
)

data class AssistantHttpResponse(
    val statusCode: Int,
    val body: String?,
)

interface AssistantHttpTransport {
    suspend fun execute(request: AssistantHttpRequest): AssistantHttpResponse
}

@Singleton
class UrlConnectionAssistantHttpTransport @Inject constructor() : AssistantHttpTransport {
    override suspend fun execute(request: AssistantHttpRequest): AssistantHttpResponse = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(request.url).openConnection() as HttpURLConnection).apply {
                requestMethod = request.method
                connectTimeout = 10_000
                readTimeout = request.readTimeoutMillis
                doInput = true
                request.headers.forEach(::setRequestProperty)
                request.body?.let { body ->
                    doOutput = true
                    outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
                }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            AssistantHttpResponse(status, stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() })
        } catch (_: UnknownHostException) {
            throw AssistantProviderException.Unavailable("OpenRouter could not be reached. Check your connection.")
        } catch (_: ConnectException) {
            throw AssistantProviderException.Unavailable("OpenRouter is unavailable. Try again later.")
        } catch (_: SocketTimeoutException) {
            throw AssistantProviderException.Unavailable("OpenRouter timed out. Your draft is still here.")
        } catch (exception: IOException) {
            throw AssistantProviderException.Unavailable(exception.message ?: "OpenRouter request failed.")
        } finally {
            connection?.disconnect()
        }
    }
}

sealed class AssistantProviderException(message: String) : IllegalStateException(message) {
    class InvalidCredential(message: String = "OpenRouter access is invalid. Reconnect your account.") : AssistantProviderException(message)
    class Revoked(message: String = "OpenRouter access was revoked. Reconnect your account.") : AssistantProviderException(message)
    class Quota(message: String = "OpenRouter's request limit has been reached.") : AssistantProviderException(message)
    class Unavailable(message: String = "OpenRouter is unavailable. Try again later.") : AssistantProviderException(message)
    class Malformed(message: String = "OpenRouter returned an unreadable response.") : AssistantProviderException(message)
}

data class OpenRouterKeyMetadata(
    val label: String?,
    val isFreeTier: Boolean,
    val limitRemaining: Double?,
)

@Singleton
class OpenRouterApi @Inject constructor(
    private val transport: AssistantHttpTransport,
) {
    private val gson = Gson()
    suspend fun exchangeCode(code: String, codeVerifier: String): Result<String> = runApiRequest {
        val body = JsonObject().apply {
            addProperty("code", code)
            addProperty("code_verifier", codeVerifier)
            addProperty("code_challenge_method", "S256")
        }
        val response = transport.execute(
            AssistantHttpRequest(
                method = "POST",
                url = "$BASE_URL/api/v1/auth/keys",
                headers = jsonHeaders(),
                body = gson.toJson(body),
            ),
        )
        requireSuccess(response, exchangingCode = true)
        gson.fromJson(response.body, ExchangeResponse::class.java).key?.takeIf { it.isNotBlank() }
            ?: throw AssistantProviderException.Malformed()
    }

    suspend fun inspectCurrentKey(token: String): Result<OpenRouterKeyMetadata> = runApiRequest {
        val response = transport.execute(
            AssistantHttpRequest(
                method = "GET",
                url = "$BASE_URL/api/v1/key",
                headers = authorizedHeaders(token),
            ),
        )
        requireSuccess(response)
        val data = gson.fromJson(response.body, CurrentKeyResponse::class.java).data
            ?: throw AssistantProviderException.Malformed()
        OpenRouterKeyMetadata(sanitizeCredentialLabel(data.label), data.isFreeTier ?: false, data.limitRemaining)
    }

    suspend fun chat(
        token: String,
        history: List<AssistantChatMessage>,
        userMessage: String,
        createdAtUtcEpochMillis: Long = System.currentTimeMillis(),
    ): Result<AssistantChatMessage> = runApiRequest {
        val request = OpenRouterChatRequest(
            model = MODEL,
            messages = history.map { message ->
                OpenRouterMessage(
                    role = when (message.role) {
                        AssistantMessageRole.SYSTEM -> "system"
                        AssistantMessageRole.USER -> "user"
                        AssistantMessageRole.ASSISTANT -> "assistant"
                    },
                    content = message.content,
                )
            } + OpenRouterMessage("user", userMessage),
            provider = OpenRouterProviderPreferences(),
        )
        val response = transport.execute(
            AssistantHttpRequest(
                method = "POST",
                url = "$BASE_URL/api/v1/chat/completions",
                headers = authorizedHeaders(token),
                body = gson.toJson(request),
            ),
        )
        requireSuccess(response)
        val parsed = gson.fromJson(response.body, OpenRouterChatResponse::class.java)
        val content = parsed.choices.orEmpty().firstOrNull()?.message?.content?.trim().orEmpty()
        if (content.isEmpty()) throw AssistantProviderException.Malformed()
        AssistantChatMessage(
            id = "assistant-$createdAtUtcEpochMillis",
            role = AssistantMessageRole.ASSISTANT,
            content = content,
            createdAtUtcEpochMillis = createdAtUtcEpochMillis,
        )
    }

    suspend fun requestToolCall(
        token: String,
        systemPrompt: String,
        userPrompt: String,
        contract: CoachingToolContract,
    ): Result<CoachingToolCall> = runApiRequest {
        val function = JsonObject().apply {
            addProperty("name", contract.name)
            addProperty("description", contract.description)
            add("parameters", contract.parameters)
        }
        val tool = JsonObject().apply {
            addProperty("type", "function")
            add("function", function)
        }
        val toolChoice = JsonObject().apply {
            addProperty("type", "function")
            add("function", JsonObject().apply { addProperty("name", contract.name) })
        }
        val request = JsonObject().apply {
            addProperty("model", MODEL)
            add("messages", gson.toJsonTree(listOf(
                OpenRouterMessage("system", systemPrompt),
                OpenRouterMessage("user", userPrompt),
            )))
            add("provider", gson.toJsonTree(OpenRouterProviderPreferences()))
            add("tools", gson.toJsonTree(listOf(tool)))
            add("tool_choice", toolChoice)
            addProperty("parallel_tool_calls", false)
            addProperty("stream", false)
        }
        val response = transport.execute(
            AssistantHttpRequest(
                method = "POST",
                url = "$BASE_URL/api/v1/chat/completions",
                headers = authorizedHeaders(token),
                body = gson.toJson(request),
            ),
        )
        requireSuccess(response)
        val parsed = gson.fromJson(response.body, OpenRouterChatResponse::class.java)
        val calls = parsed.choices.orEmpty().firstOrNull()?.message?.toolCalls.orEmpty()
        if (calls.size != 1) throw AssistantProviderException.Malformed("OpenRouter did not return one coaching proposal.")
        val call = calls.single()
        if (call.type != "function" || call.function?.name != contract.name || call.function.arguments.isNullOrBlank()) {
            throw AssistantProviderException.Malformed("OpenRouter returned an unexpected coaching proposal.")
        }
        CoachingToolCall(call.function.name, call.function.arguments)
    }

    private fun requireSuccess(response: AssistantHttpResponse, exchangingCode: Boolean = false) {
        if (response.statusCode in 200..299) return
        val providerMessage = runCatching {
            gson.fromJson(response.body, ProviderErrorResponse::class.java).error?.message
        }.getOrNull()?.takeIf { it.isNotBlank() }
        throw when (response.statusCode) {
            401 -> AssistantProviderException.InvalidCredential(providerMessage ?: "OpenRouter access is invalid. Reconnect your account.")
            403 -> if (exchangingCode) {
                AssistantProviderException.InvalidCredential("OpenRouter authorization expired or did not match. Connect again.")
            } else {
                AssistantProviderException.Revoked(providerMessage ?: "OpenRouter access was revoked. Reconnect your account.")
            }
            402, 429 -> AssistantProviderException.Quota(providerMessage ?: "OpenRouter's request limit has been reached.")
            in 500..599 -> AssistantProviderException.Unavailable(providerMessage ?: "OpenRouter is unavailable. Try again later.")
            else -> AssistantProviderException.Unavailable(providerMessage ?: "OpenRouter request failed (${response.statusCode}).")
        }
    }

    private fun jsonHeaders(): Map<String, String> = mapOf(
        "Content-Type" to "application/json",
        "Accept" to "application/json",
        "User-Agent" to "Keepfit-Android/0.9",
    )

    private fun authorizedHeaders(token: String): Map<String, String> = jsonHeaders() +
        ("Authorization" to "Bearer $token")

    private fun sanitizeCredentialLabel(label: String?): String? {
        val normalized = label?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return if (normalized.startsWith("sk-", ignoreCase = true)) {
            "Authorized key ending ${normalized.takeLast(4)}"
        } else {
            normalized.take(40)
        }
    }

    private suspend fun <T> runApiRequest(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (exception: AssistantProviderException) {
        Result.failure(exception)
    } catch (_: Exception) {
        Result.failure(AssistantProviderException.Malformed())
    }

    private data class ExchangeResponse(val key: String? = null)
    private data class CurrentKeyResponse(val data: CurrentKeyData? = null)
    private data class CurrentKeyData(
        val label: String? = null,
        @SerializedName("is_free_tier") val isFreeTier: Boolean? = null,
        @SerializedName("limit_remaining") val limitRemaining: Double? = null,
    )
    private data class OpenRouterMessage(
        val role: String,
        val content: String? = null,
        @SerializedName("tool_calls") val toolCalls: List<OpenRouterToolCall>? = null,
    )
    private data class OpenRouterToolCall(
        val id: String? = null,
        val type: String? = null,
        val function: OpenRouterFunctionCall? = null,
    )
    private data class OpenRouterFunctionCall(
        val name: String? = null,
        val arguments: String? = null,
    )
    private data class OpenRouterProviderPreferences(
        @SerializedName("data_collection") val dataCollection: String = "deny",
        val zdr: Boolean = true,
    )
    private data class OpenRouterChatRequest(
        val model: String,
        val messages: List<OpenRouterMessage>,
        val provider: OpenRouterProviderPreferences,
        val stream: Boolean = false,
    )
    private data class OpenRouterChoice(val message: OpenRouterMessage? = null)
    private data class OpenRouterChatResponse(val choices: List<OpenRouterChoice>? = null)
    private data class ProviderError(val message: String? = null)
    private data class ProviderErrorResponse(val error: ProviderError? = null)

    companion object {
        const val MODEL = "inclusionai/ling-3.0-flash-sante:free"
        private const val BASE_URL = "https://openrouter.ai"
    }
}
