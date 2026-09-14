package com.keepfit.feature.workouts.catalog

import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseDbCatalogProviderTest {
    @Test
    fun queryIsEncodedAndSuccessfulRowsAreMapped() = runBlocking {
        val client = FakeCatalogHttpClient(
            responses = ArrayDeque(
                listOf(
                    CatalogHttpResponse(200, SUCCESS_FIXTURE),
                ),
            ),
        )
        val provider = ExerciseDbCatalogProvider(client)

        val result = provider.search(
            CatalogQuery(
                text = "bench press",
                movementArea = "upper arms",
                targetMuscle = "triceps",
                equipment = "body weight",
            ),
        )

        assertEquals(
            "https://oss.exercisedb.dev/api/v1/exercises?name=bench+press&bodyParts=upper+arms&targetMuscles=triceps&equipments=body+weight&limit=20",
            client.requestedUrls.single(),
        )
        val success = result as CatalogSearchResult.Success
        assertEquals(1, success.items.size)
        assertEquals(42, success.total)
        assertTrue(success.hasMore)
        assertEquals("Barbell Bench Press", success.items.single().name)
        assertEquals(listOf("Lie flat.", "Press smoothly."), success.items.single().instructions)
        assertEquals("https://static.exercisedb.dev/media/bench.gif", success.items.single().demoUrl)
        assertEquals(CatalogOrigin.EXERCISE_DB_LIVE, success.items.single().origin)
    }

    @Test
    fun malformedRowsAreSkippedAndUntrustedMediaUrlsAreRemoved() = runBlocking {
        val provider = ExerciseDbCatalogProvider(
            FakeCatalogHttpClient(
                ArrayDeque(listOf(CatalogHttpResponse(200, MIXED_FIXTURE))),
            ),
        )

        val success = provider.search(CatalogQuery()) as CatalogSearchResult.Success

        assertEquals(listOf("Safe Exercise"), success.items.map(CatalogExercise::name))
        assertNull(success.items.single().demoUrl)
    }

    @Test
    fun eachSearchUsesTheFreshRotatingMediaUrl() = runBlocking {
        val client = FakeCatalogHttpClient(
            ArrayDeque(
                listOf(
                    CatalogHttpResponse(200, rotatingFixture("monday.gif")),
                    CatalogHttpResponse(200, rotatingFixture("next-monday.gif")),
                ),
            ),
        )
        val provider = ExerciseDbCatalogProvider(client)

        val first = provider.search(CatalogQuery(text = "row")) as CatalogSearchResult.Success
        val second = provider.search(CatalogQuery(text = "row")) as CatalogSearchResult.Success

        assertEquals("https://static.exercisedb.dev/media/monday.gif", first.items.single().demoUrl)
        assertEquals("https://static.exercisedb.dev/media/next-monday.gif", second.items.single().demoUrl)
        assertEquals(2, client.requestedUrls.size)
    }

    @Test
    fun httpMalformedOfflineAndTimeoutFailuresAreExplicit() = runBlocking {
        val httpFailure = ExerciseDbCatalogProvider(
            FakeCatalogHttpClient(ArrayDeque(listOf(CatalogHttpResponse(503, "unavailable")))),
        ).search(CatalogQuery())
        val malformed = ExerciseDbCatalogProvider(
            FakeCatalogHttpClient(ArrayDeque(listOf(CatalogHttpResponse(200, "not-json")))),
        ).search(CatalogQuery())
        val offline = ExerciseDbCatalogProvider(FailingClient(UnknownHostException())).search(CatalogQuery())
        val timeout = ExerciseDbCatalogProvider(FailingClient(SocketTimeoutException())).search(CatalogQuery())

        assertEquals(CatalogFailureKind.PROVIDER_ERROR, (httpFailure as CatalogSearchResult.Failure).kind)
        assertEquals(CatalogFailureKind.MALFORMED_RESPONSE, (malformed as CatalogSearchResult.Failure).kind)
        assertEquals(CatalogFailureKind.OFFLINE, (offline as CatalogSearchResult.Failure).kind)
        assertEquals(CatalogFailureKind.TIMEOUT, (timeout as CatalogSearchResult.Failure).kind)
    }

    @Test(expected = CancellationException::class)
    fun cancellationIsNotConvertedToAProviderFailure() {
        runBlocking {
            ExerciseDbCatalogProvider(FailingClient(CancellationException())).search(CatalogQuery())
        }
    }

    private class FakeCatalogHttpClient(
        private val responses: ArrayDeque<CatalogHttpResponse>,
    ) : CatalogHttpClient {
        val requestedUrls = mutableListOf<String>()

        override suspend fun get(url: String): CatalogHttpResponse {
            requestedUrls += url
            return responses.removeFirst()
        }
    }

    private class FailingClient(
        private val throwable: Throwable,
    ) : CatalogHttpClient {
        override suspend fun get(url: String): CatalogHttpResponse = throw throwable
    }

    private companion object {
        const val SUCCESS_FIXTURE = """
            {
              "success": true,
              "meta": {"total": 42, "hasNextPage": true},
              "data": [{
                "exerciseId": "bench",
                "name": "barbell bench press",
                "gifUrl": "https://static.exercisedb.dev/media/bench.gif",
                "bodyParts": ["chest"],
                "equipments": ["barbell"],
                "targetMuscles": ["pectorals"],
                "secondaryMuscles": ["triceps"],
                "instructions": ["Step:1 Lie flat.", "Step:2 Press smoothly."]
              }]
            }
        """

        const val MIXED_FIXTURE = """
            {
              "success": true,
              "meta": {"total": 2, "hasNextPage": false},
              "data": [
                {"exerciseId": "", "name": "Missing id"},
                {
                  "exerciseId": "safe",
                  "name": "safe exercise",
                  "gifUrl": "https://tracker.example.com/not-allowed.gif",
                  "bodyParts": [],
                  "equipments": ["body weight"],
                  "targetMuscles": ["core"],
                  "secondaryMuscles": [],
                  "instructions": ["Step:1 Hold steady."]
                }
              ]
            }
        """

        fun rotatingFixture(fileName: String) = """
            {
              "success": true,
              "meta": {"total": 1, "hasNextPage": false},
              "data": [{
                "exerciseId": "row",
                "name": "row",
                "gifUrl": "https://static.exercisedb.dev/media/$fileName",
                "bodyParts": ["back"],
                "equipments": ["cable"],
                "targetMuscles": ["lats"],
                "secondaryMuscles": [],
                "instructions": ["Step:1 Pull."]
              }]
            }
        """
    }
}
