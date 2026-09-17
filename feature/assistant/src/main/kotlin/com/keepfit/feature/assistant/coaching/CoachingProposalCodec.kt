package com.keepfit.feature.assistant.coaching

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoachingProposalCodec @Inject constructor() {
    private val gson = Gson()

    fun encode(proposal: CoachingProposal): String = gson.toJson(JsonObject().apply {
        addProperty("id", proposal.id)
        addProperty("intent", proposal.intent.name)
        addProperty("title", proposal.title)
        addProperty("observed", proposal.observed)
        addProperty("current", proposal.current)
        addProperty("proposed", proposal.proposed)
        addProperty("reason", proposal.reason)
        addProperty("original_request", proposal.originalRequest)
        addProperty("generated_at", proposal.generatedAtUtcEpochMillis)
        addProperty("model", proposal.model)
        val (type, operation) = when (val value = proposal.operation) {
            CoachingProposalOperation.None -> "NONE" to JsonObject()
            is CoachingProposalOperation.ReplaceWeeklySchedule -> "SCHEDULE" to gson.toJsonTree(value)
            is CoachingProposalOperation.AdjustTodayWorkout -> "TODAY" to gson.toJsonTree(value)
            is CoachingProposalOperation.AddExistingFoodMeal -> "MEAL" to gson.toJsonTree(value)
        }
        addProperty("operation_type", type)
        add("operation", operation)
    })

    fun decode(value: String): CoachingProposal? = runCatching {
        val json = JsonParser.parseString(value).asJsonObject
        val operationJson = json.getAsJsonObject("operation")
        val operation = when (json.get("operation_type").asString) {
            "NONE" -> CoachingProposalOperation.None
            "SCHEDULE" -> gson.fromJson(operationJson, CoachingProposalOperation.ReplaceWeeklySchedule::class.java)
            "TODAY" -> gson.fromJson(operationJson, CoachingProposalOperation.AdjustTodayWorkout::class.java)
            "MEAL" -> gson.fromJson(operationJson, CoachingProposalOperation.AddExistingFoodMeal::class.java)
            else -> error("Unknown stored proposal operation.")
        }
        CoachingProposal(
            id = json.text("id", 80),
            intent = CoachingIntent.valueOf(json.text("intent", 40)),
            title = json.text("title", 80),
            observed = json.text("observed", 240),
            current = json.text("current", 300),
            proposed = json.text("proposed", 300),
            reason = json.text("reason", 240),
            operation = operation,
            originalRequest = json.text("original_request", 500),
            generatedAtUtcEpochMillis = json.get("generated_at").asLong,
            model = json.text("model", 100),
        )
    }.getOrNull()

    private fun JsonObject.text(name: String, max: Int): String =
        get(name)?.asString?.trim()?.also { require(it.isNotEmpty() && it.length <= max) }
            ?: error("Missing stored proposal field.")
}
