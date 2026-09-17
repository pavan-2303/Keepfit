package com.keepfit.feature.assistant.data

import com.keepfit.feature.assistant.access.AssistantAccessController
import com.keepfit.feature.assistant.access.OpenRouterApi
import com.keepfit.feature.assistant.coaching.CoachingContextDataSource
import com.keepfit.feature.assistant.coaching.CoachingContext
import com.keepfit.feature.assistant.coaching.CoachingIntent
import com.keepfit.feature.assistant.coaching.CoachingPromptBuilder
import com.keepfit.feature.assistant.coaching.CoachingProposal
import com.keepfit.feature.assistant.coaching.CoachingProposalValidator
import com.keepfit.feature.assistant.coaching.CoachingSafetyDecision
import com.keepfit.feature.assistant.coaching.CoachingSafetyGate
import com.keepfit.feature.assistant.coaching.CoachingSafetyRefusalException
import com.keepfit.feature.assistant.coaching.CoachingToolContract
import java.time.Clock
import com.keepfit.feature.assistant.planning.AssistantPlanContextDataSource
import com.keepfit.feature.assistant.planning.AssistantPlanPromptBuilder
import com.keepfit.feature.assistant.planning.AssistantPlanToolContract
import com.keepfit.feature.assistant.planning.AssistantPlanValidator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenRouterAssistantRepository @Inject constructor(
    private val summaryRepository: AssistantSummaryRepository,
    private val promptAssembler: AssistantPromptAssembler,
    private val contextPolicy: AssistantContextPolicy,
    private val api: OpenRouterApi,
    private val accessManager: AssistantAccessController,
    private val coachingContextDataSource: CoachingContextDataSource,
    private val safetyGate: CoachingSafetyGate,
    private val proposalValidator: CoachingProposalValidator,
    private val planContextDataSource: AssistantPlanContextDataSource,
    private val planValidator: AssistantPlanValidator,
    private val clock: Clock,
    private val coachingPromptBuilder: CoachingPromptBuilder = CoachingPromptBuilder(),
    private val planPromptBuilder: AssistantPlanPromptBuilder = AssistantPlanPromptBuilder(),
) : AssistantRepository {
    override suspend fun testConnection(config: AssistantRuntimeConfig): Result<Unit> =
        accessManager.inspectConnection().map { Unit }

    override suspend fun sendChatTurn(
        config: AssistantRuntimeConfig,
        history: List<AssistantChatMessage>,
        userMessage: String,
    ): Result<AssistantChatMessage> {
        safetyFailure(userMessage)?.let { return Result.failure(it) }
        val outgoingMessage = if (contextPolicy.shouldIncludeLocalContext(userMessage)) {
            promptAssembler.buildContextAwareChatPrompt(
                question = userMessage,
                summary = summaryRepository.loadProgressSummary(),
            )
        } else {
            userMessage
        }
        return execute(history, outgoingMessage)
    }

    override suspend fun generateProgressSummary(
        config: AssistantRuntimeConfig,
    ): Result<AssistantProgressSummary> {
        val prompt = promptAssembler.buildProgressSummaryPrompt(summaryRepository.loadProgressSummary())
        return execute(emptyList(), prompt).map { response ->
            AssistantProgressSummary("Progress summary", response.content)
        }
    }

    override suspend fun requestDraftPlan(
        config: AssistantRuntimeConfig,
        input: AssistantDraftInput,
    ): Result<AssistantDraftWorkoutPlan> {
        safetyFailure(listOfNotNull(input.goal, input.notes).joinToString(" "))?.let { return Result.failure(it) }
        val context = runCatching { planContextDataSource.loadContext() }.getOrElse { return Result.failure(it) }
        if (context.exercises.isEmpty()) return Result.failure(IllegalStateException("No suitable catalogue exercises are available."))
        if (context.preferredDays.isEmpty()) return Result.failure(IllegalStateException("Choose at least one training day first."))
        val prompts = planPromptBuilder.build(input, context)
        val token = accessManager.reserveInferenceRequest().getOrElse { return Result.failure(it) }
        return api.requestToolCall(
            token = token,
            systemPrompt = prompts.system,
            userPrompt = prompts.user,
            contract = AssistantPlanToolContract.create(),
        ).fold(
            onSuccess = { call ->
                val result = planValidator.validate(call, context)
                if (result.isSuccess) accessManager.recordProviderSuccess()
                else accessManager.recordProviderFailure(result.exceptionOrNull()!!)
                result
            },
            onFailure = { exception ->
                accessManager.recordProviderFailure(exception)
                Result.failure(exception)
            },
        )
    }

    override suspend fun requestCoachingProposal(
        config: AssistantRuntimeConfig,
        intent: CoachingIntent,
        userRequest: String,
    ): Result<CoachingProposal> {
        safetyFailure(userRequest)?.let { return Result.failure(it) }
        val context = runCatching { coachingContextDataSource.loadContext() }
            .getOrElse { return Result.failure(it) }
        validateAvailableContext(intent, context)?.let { return Result.failure(it) }
        val prompts = coachingPromptBuilder.build(intent, userRequest, context)
        val token = accessManager.reserveInferenceRequest().getOrElse { return Result.failure(it) }
        return api.requestToolCall(
            token = token,
            systemPrompt = prompts.system,
            userPrompt = prompts.user,
            contract = CoachingToolContract.forIntent(intent),
        ).fold(
            onSuccess = { call ->
                val result = proposalValidator.validate(
                    intent = intent,
                    call = call,
                    context = context,
                    originalRequest = userRequest,
                    generatedAtUtcEpochMillis = clock.millis(),
                    model = OpenRouterApi.MODEL,
                )
                if (result.isSuccess) accessManager.recordProviderSuccess()
                else accessManager.recordProviderFailure(result.exceptionOrNull()!!)
                result
            },
            onFailure = { exception ->
                accessManager.recordProviderFailure(exception)
                Result.failure(exception)
            },
        )
    }

    private suspend fun execute(
        history: List<AssistantChatMessage>,
        userMessage: String,
    ): Result<AssistantChatMessage> {
        val token = accessManager.reserveInferenceRequest().getOrElse { return Result.failure(it) }
        return api.chat(token, history, userMessage)
            .onSuccess { accessManager.recordProviderSuccess() }
            .onFailure(accessManager::recordProviderFailure)
    }

    private fun safetyFailure(request: String): CoachingSafetyRefusalException? =
        when (val decision = safetyGate.evaluate(request)) {
            CoachingSafetyDecision.Allowed -> null
            is CoachingSafetyDecision.Refused -> CoachingSafetyRefusalException(decision.message)
        }

    private fun validateAvailableContext(
        intent: CoachingIntent,
        context: CoachingContext,
    ): IllegalStateException? = when {
        intent == CoachingIntent.WEEKLY_PLAN && context.templates.isEmpty() ->
            IllegalStateException("Create or generate a workout template before asking the coach to plan the week.")
        intent in setOf(
            CoachingIntent.SCHEDULE_CHANGE,
            CoachingIntent.WORKOUT_SHORTENING,
            CoachingIntent.EXERCISE_SUBSTITUTION,
        ) && context.todayWorkout == null ->
            IllegalStateException("There is no current workout available for this coaching action.")
        intent == CoachingIntent.EXERCISE_SUBSTITUTION && context.replacementExercises.isEmpty() ->
            IllegalStateException("Add another exercise to your library before asking for a substitution.")
        intent == CoachingIntent.EXISTING_FOOD_MEAL && context.foods.isEmpty() ->
            IllegalStateException("Add foods to your library before asking for an existing-food meal.")
        else -> null
    }
}
