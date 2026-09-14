package com.keepfit.feature.workouts.catalog

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CatalogHttpResponse(
    val statusCode: Int,
    val body: String,
)

interface CatalogHttpClient {
    suspend fun get(url: String): CatalogHttpResponse
}

class UrlConnectionCatalogHttpClient(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : CatalogHttpClient {
    override suspend fun get(url: String): CatalogHttpResponse = withContext(ioDispatcher) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Cache-Control", "no-cache, no-store")
            connection.setRequestProperty("User-Agent", "Keepfit-Android/0.7")
            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
            CatalogHttpResponse(
                statusCode = statusCode,
                body = stream?.bufferedReader()?.use { it.readText() }.orEmpty(),
            )
        } finally {
            connection.disconnect()
        }
    }
}

class ExerciseDbCatalogProvider(
    private val httpClient: CatalogHttpClient,
    private val baseUrl: String = "https://oss.exercisedb.dev/api/v1/exercises",
) : ExerciseCatalogProvider {
    override suspend fun search(query: CatalogQuery): CatalogSearchResult {
        return try {
            val response = httpClient.get(buildUrl(query))
            if (response.statusCode !in 200..299) {
                CatalogSearchResult.Failure(
                    CatalogFailureKind.PROVIDER_ERROR,
                    "Live demos are unavailable right now. Try again when the provider recovers.",
                )
            } else {
                parse(response.body)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: UnknownHostException) {
            CatalogSearchResult.Failure(
                CatalogFailureKind.OFFLINE,
                "No network connection. The 40-exercise offline guide is still available.",
            )
        } catch (error: SocketTimeoutException) {
            CatalogSearchResult.Failure(
                CatalogFailureKind.TIMEOUT,
                "The live catalogue took too long to respond. Check your connection and try again.",
            )
        } catch (error: Exception) {
            CatalogSearchResult.Failure(
                CatalogFailureKind.PROVIDER_ERROR,
                "Live demos could not be loaded. Your local exercise library is unchanged.",
            )
        }
    }

    internal fun buildUrl(query: CatalogQuery): String {
        val parameters = buildList {
            query.text.trim().takeIf(String::isNotEmpty)?.let { add("name" to it) }
            query.movementArea?.trim()?.takeIf(String::isNotEmpty)?.let { add("bodyParts" to it) }
            query.targetMuscle?.trim()?.takeIf(String::isNotEmpty)?.let { add("targetMuscles" to it) }
            query.equipment?.trim()?.takeIf(String::isNotEmpty)?.let { add("equipments" to it) }
            add("limit" to "20")
        }
        return "$baseUrl?" + parameters.joinToString("&") { (name, value) ->
            "$name=${URLEncoder.encode(value, StandardCharsets.UTF_8.name())}"
        }
    }

    private fun parse(body: String): CatalogSearchResult = try {
        val root = JsonParser.parseString(body).asJsonObject
        require(root.boolean("success") == true)
        val data = root.getAsJsonArray("data") ?: error("Missing data")
        val meta = root.getAsJsonObject("meta")
        val items = data.mapNotNull { element ->
            runCatching { element.asJsonObject.toCatalogExercise() }.getOrNull()
        }
        CatalogSearchResult.Success(
            items = items,
            total = meta?.integer("total") ?: items.size,
            hasMore = meta?.boolean("hasNextPage") ?: false,
        )
    } catch (error: Exception) {
        CatalogSearchResult.Failure(
            CatalogFailureKind.MALFORMED_RESPONSE,
            "The live catalogue returned data Keepfit could not safely display. Try again later.",
        )
    }
}

private fun JsonObject.toCatalogExercise(): CatalogExercise {
    val exerciseId = string("exerciseId")?.trim().orEmpty()
    val name = string("name")?.trim().orEmpty()
    require(exerciseId.isNotEmpty() && name.isNotEmpty())
    val bodyParts = stringList("bodyParts")
    val targetMuscles = stringList("targetMuscles")
    val equipment = stringList("equipments")
    return CatalogExercise(
        id = "exercisedb:$exerciseId",
        name = name.toDisplayName(),
        movementArea = bodyParts.firstOrNull()?.toDisplayName() ?: "General",
        bodyParts = bodyParts.map(String::toDisplayName),
        targetMuscles = targetMuscles.map(String::toDisplayName),
        secondaryMuscles = stringList("secondaryMuscles").map(String::toDisplayName),
        equipment = equipment.map(String::toDisplayName),
        instructions = stringList("instructions").map { instruction ->
            instruction.replace(STEP_PREFIX, "").trim()
        }.filter(String::isNotEmpty),
        isBodyweight = equipment.any { it.equals("body weight", ignoreCase = true) },
        demoUrl = string("gifUrl")?.takeIf(::isTrustedMediaUrl),
        origin = CatalogOrigin.EXERCISE_DB_LIVE,
        attribution = "ExerciseDB by AscendAPI",
    )
}

private fun JsonObject.string(name: String): String? =
    get(name)?.takeUnless { it.isJsonNull || !it.isJsonPrimitive }?.asString

private fun JsonObject.boolean(name: String): Boolean? =
    get(name)?.takeUnless { it.isJsonNull || !it.isJsonPrimitive }?.asBoolean

private fun JsonObject.integer(name: String): Int? =
    get(name)?.takeUnless { it.isJsonNull || !it.isJsonPrimitive }?.asInt

private fun JsonObject.stringList(name: String): List<String> =
    getAsJsonArray(name)?.mapNotNull { element ->
        element.takeIf { it.isJsonPrimitive }?.asString?.trim()?.takeIf(String::isNotEmpty)
    }.orEmpty()

private fun String.toDisplayName(): String = trim()
    .split(WHITESPACE)
    .filter(String::isNotEmpty)
    .joinToString(" ") { word -> word.lowercase().replaceFirstChar { it.titlecase() } }

private fun isTrustedMediaUrl(value: String): Boolean = runCatching {
    val uri = URI(value)
    uri.scheme.equals("https", ignoreCase = true) &&
        (uri.host == "exercisedb.dev" || uri.host?.endsWith(".exercisedb.dev") == true)
}.getOrDefault(false)

private val STEP_PREFIX = Regex("^Step:\\s*\\d+\\s*", RegexOption.IGNORE_CASE)
private val WHITESPACE = Regex("\\s+")
