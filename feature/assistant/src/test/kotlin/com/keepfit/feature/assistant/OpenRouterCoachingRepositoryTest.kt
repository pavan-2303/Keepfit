package com.keepfit.feature.assistant

import com.keepfit.feature.assistant.access.AssistantAccessController
import com.keepfit.feature.assistant.access.AssistantAccessState
import com.keepfit.feature.assistant.access.AssistantHttpRequest
import com.keepfit.feature.assistant.access.AssistantHttpResponse
import com.keepfit.feature.assistant.access.AssistantHttpTransport
import com.keepfit.feature.assistant.access.OpenRouterApi
import com.keepfit.feature.assistant.access.OpenRouterKeyMetadata
import com.keepfit.feature.assistant.coaching.CoachingContext
import com.keepfit.feature.assistant.coaching.CoachingContextDataSource
import com.keepfit.feature.assistant.coaching.CoachingFoodOption
import com.keepfit.feature.assistant.coaching.CoachingIntent
import com.keepfit.feature.assistant.coaching.CoachingSafetyRefusalException
import com.keepfit.feature.assistant.coaching.CoachingSafetyGate
import com.keepfit.feature.assistant.coaching.CoachingTemplateOption
import com.keepfit.feature.assistant.coaching.CoachingProposalValidator
import com.keepfit.feature.assistant.data.AssistantLocalSummary
import com.keepfit.feature.assistant.data.AssistantContextPolicy
import com.keepfit.feature.assistant.data.AssistantNutritionSnapshot
import com.keepfit.feature.assistant.data.AssistantProgressSnapshot
import com.keepfit.feature.assistant.data.AssistantPromptAssembler
import com.keepfit.feature.assistant.data.AssistantSummaryDataSource
import com.keepfit.feature.assistant.data.AssistantSummaryRepository
import com.keepfit.feature.assistant.data.OpenRouterAssistantRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenRouterCoachingRepositoryTest {
    private val clock = Clock.fixed(Instant.parse("2026-09-13T12:00:00Z"), ZoneOffset.UTC)

    @Test
    fun safeProposalUsesAliasesAndNeverSendsLocalIdentifiers() = runBlocking {
        val transport = RecordingTransport(validSummaryResponse())
        val access = FakeAccessController()
        val repository = repository(transport, access)

        val proposal = repository.requestCoachingProposal(
            sampleConfig(),
            CoachingIntent.WEEKLY_SUMMARY,
            "How did this week go?",
        ).getOrThrow()

        assertEquals("Week in review", proposal.title)
        assertEquals(1, access.reservations)
        val body = transport.requests.single().body!!
        assertTrue(body.contains("template_1"))
        assertTrue(body.contains("food_1"))
        assertFalse(body.contains("template-local-id"))
        assertFalse(body.contains("food-local-id"))
        assertFalse(body.contains("planned-local-id"))
    }

    @Test
    fun unsafeRequestIsRefusedBeforeQuotaOrNetwork() = runBlocking {
        val transport = RecordingTransport(validSummaryResponse())
        val access = FakeAccessController()
        val repository = repository(transport, access)

        val result = repository.requestCoachingProposal(
            sampleConfig(),
            CoachingIntent.WORKOUT_SHORTENING,
            "Help me train through sharp knee pain",
        )

        assertTrue(result.exceptionOrNull() is CoachingSafetyRefusalException)
        assertEquals(0, access.reservations)
        assertTrue(transport.requests.isEmpty())
    }

    @Test
    fun personalProgressQuestionIncludesCompactKeepfitSnapshot() = runBlocking {
        val transport = RecordingTransport(chatResponse())
        val repository = repository(transport, FakeAccessController())

        repository.sendChatTurn(sampleConfig(), emptyList(), "How is my recent workout consistency?").getOrThrow()

        val body = transport.requests.single().body.orEmpty()
        assertTrue(body.contains("Keepfit snapshot"))
        assertTrue(body.contains("Recent workouts"))
    }

    @Test
    fun generalQuestionDoesNotIncludeLocalRecords() = runBlocking {
        val transport = RecordingTransport(chatResponse())
        val repository = repository(transport, FakeAccessController())

        repository.sendChatTurn(sampleConfig(), emptyList(), "What is progressive overload?").getOrThrow()

        val body = transport.requests.single().body.orEmpty()
        assertFalse(body.contains("Keepfit snapshot"))
        assertFalse(body.contains("Recent workouts"))
    }

    private fun repository(transport: RecordingTransport, access: FakeAccessController) =
        OpenRouterAssistantRepository(
            summaryRepository = AssistantSummaryRepository(EmptySummarySource(), clock),
            promptAssembler = AssistantPromptAssembler(),
            contextPolicy = AssistantContextPolicy(),
            api = OpenRouterApi(transport),
            accessManager = access,
            coachingContextDataSource = CoachingContextDataSource { context() },
            safetyGate = CoachingSafetyGate(),
            proposalValidator = CoachingProposalValidator(),
            clock = clock,
        )

    private fun context() = CoachingContext(
        generatedOn = LocalDate.of(2026, 9, 13),
        evidence = listOf("Completed three workouts."),
        templates = listOf(CoachingTemplateOption("template_1", "template-local-id", "Upper")),
        todayWorkout = com.keepfit.feature.assistant.coaching.CoachingWorkoutOption(
            alias = "workout_today",
            plannedWorkoutId = "planned-local-id",
            occurrenceId = null,
            title = "Upper",
            originalDate = LocalDate.of(2026, 9, 13),
            scheduledDate = LocalDate.of(2026, 9, 13),
            exerciseAliases = mapOf("exercise_1" to "exercise-local-id"),
            exerciseNames = mapOf("exercise_1" to "Bench press"),
        ),
        replacementExercises = mapOf("exercise_2" to ("replacement-local-id" to "Dumbbell row")),
        foods = listOf(CoachingFoodOption("food_1", "food-local-id", "Oats", "bowl")),
        currentSchedule = mapOf("MONDAY" to "Upper"),
    )

    private fun validSummaryResponse() = AssistantHttpResponse(
        200,
        """{"model":"inclusionai/ling-3.0-flash-sante:free","choices":[{"message":{"role":"assistant","tool_calls":[{"id":"call-1","type":"function","function":{"name":"summarize_week","arguments":"{\"title\":\"Week in review\",\"observed\":\"Three sessions completed.\",\"current\":\"Training is consistent.\",\"proposed\":\"Repeat the schedule.\",\"reason\":\"The current load is sustainable.\"}"}}]}}]}""",
    )

    private fun chatResponse() = AssistantHttpResponse(
        200,
        """{"model":"inclusionai/ling-3.0-flash-sante:free","choices":[{"message":{"role":"assistant","content":"A useful answer."}}]}""",
    )

    private fun sampleConfig() = com.keepfit.feature.assistant.data.AssistantRuntimeConfig(
        baseUrl = "https://openrouter.ai/api/v1",
        generalChatModelName = OpenRouterApi.MODEL,
        reasoningModelName = OpenRouterApi.MODEL,
    )

    private class RecordingTransport(private val response: AssistantHttpResponse) : AssistantHttpTransport {
        val requests = mutableListOf<AssistantHttpRequest>()
        override suspend fun execute(request: AssistantHttpRequest): AssistantHttpResponse {
            requests += request
            return response
        }
    }

    private class FakeAccessController : AssistantAccessController {
        var reservations = 0
        override val state = MutableStateFlow(
            AssistantAccessState(),
        )
        override fun acknowledgeDisclosure() = Unit
        override suspend fun beginAuthorization() = Result.success("url")
        override suspend fun resumePendingAuthorization() = Unit
        override suspend fun inspectConnection() = Result.success(OpenRouterKeyMetadata(null, true, null))
        override fun cancelAuthorization(message: String) = Unit
        override fun disconnect() = Unit
        override fun reserveInferenceRequest(): Result<String> {
            reservations++
            return Result.success("token")
        }
        override fun recordProviderSuccess() = Unit
        override fun recordProviderFailure(exception: Throwable) = Unit
    }

    private class EmptySummarySource : AssistantSummaryDataSource {
        override suspend fun readRecentWorkouts() = emptyList<com.keepfit.feature.assistant.data.AssistantRecentWorkoutSummary>()
        override suspend fun readRecords() = emptyList<com.keepfit.feature.assistant.data.AssistantRecordSummary>()
        override suspend fun readNutritionSnapshot(onDate: LocalDate) = AssistantNutritionSnapshot(
            onDate, 0.0, 0.0, 0.0, 0.0, null, null, null, null, false,
        )
        override suspend fun readProgressSnapshot() = AssistantProgressSnapshot(null, null, null, null)
        override suspend fun readStepsSnapshot() = null
    }
}
