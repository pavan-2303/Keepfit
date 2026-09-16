package com.keepfit.feature.assistant.planning

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.keepfit.feature.assistant.coaching.CoachingToolCall
import com.keepfit.feature.assistant.data.AssistantDraftWorkoutDay
import com.keepfit.feature.assistant.data.AssistantDraftWorkoutExercise
import com.keepfit.feature.assistant.data.AssistantDraftWorkoutPlan
import java.time.DayOfWeek
import javax.inject.Inject

class AssistantPlanValidator @Inject constructor() {
    fun validate(call: CoachingToolCall, context: AssistantPlanContext): Result<AssistantDraftWorkoutPlan> = runCatching {
        require(call.name == AssistantPlanToolContract.create().name) { "Unexpected planning tool '${call.name}'." }
        val json = JsonParser.parseString(call.arguments).asJsonObject
        json.requireOnly(ROOT_KEYS)
        val available = context.exercises.associateBy { it.id }
        val days = json.requiredArray("days", 1, 4).mapObject { day ->
            day.requireOnly(DAY_KEYS)
            val dayOfWeek = enumValue<DayOfWeek>(day.requiredText("day_of_week", 12))
            require(dayOfWeek in context.preferredDays) { "$dayOfWeek is not an available training day." }
            val exercises = day.requiredArray("exercises", 1, 8).mapObject { item ->
                item.requireOnly(EXERCISE_KEYS)
                val exerciseId = item.requiredText("exercise_id", 64)
                val option = available[exerciseId] ?: error("Exercise $exerciseId is not available.")
                val sets = item.requiredInt("target_sets", 1, 6)
                val reps = item.requiredText("target_reps", 20)
                require(TARGET_REPS.matches(reps)) { "Exercise target is invalid." }
                AssistantDraftWorkoutExercise(
                    exerciseId = option.id,
                    name = option.name,
                    targetSets = sets,
                    targetReps = reps,
                    notes = item.optionalText("notes", 160),
                )
            }
            require(exercises.map { it.exerciseId }.distinct().size == exercises.size) {
                "An exercise can appear only once per day."
            }
            AssistantDraftWorkoutDay(
                dayOfWeek = dayOfWeek,
                templateName = day.requiredText("template_name", 80),
                notes = day.optionalText("notes", 240),
                exercises = exercises,
            )
        }
        require(days.map { it.dayOfWeek }.distinct().size == days.size) { "A training day can appear only once." }
        AssistantDraftWorkoutPlan(
            name = json.requiredText("plan_name", 80),
            overview = json.optionalText("overview", 300),
            days = days,
        )
    }

    private fun JsonObject.requiredText(name: String, maxLength: Int): String =
        optionalText(name, maxLength)?.takeIf { it.isNotEmpty() } ?: error("$name is required.")

    private fun JsonObject.optionalText(name: String, maxLength: Int): String? {
        val element = get(name) ?: return null
        require(element.isJsonPrimitive && element.asJsonPrimitive.isString) { "$name is invalid." }
        return element.asString.trim().also { value ->
            require(value.length <= maxLength && value.none(Char::isISOControl)) { "$name is invalid." }
        }.ifBlank { null }
    }

    private fun JsonObject.requiredInt(name: String, min: Int, max: Int): Int {
        val primitive = get(name)?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive ?: error("$name is required.")
        require(primitive.isNumber) { "$name is invalid." }
        val value = primitive.asInt
        require(value in min..max && primitive.asDouble == value.toDouble()) { "$name is invalid." }
        return value
    }

    private fun JsonObject.requiredArray(name: String, min: Int, max: Int): JsonArray {
        val array = get(name)?.takeIf { it.isJsonArray }?.asJsonArray ?: error("$name is required.")
        require(array.size() in min..max) { "$name has an invalid number of items." }
        return array
    }

    private fun JsonObject.requireOnly(keys: Set<String>) {
        require(keySet() == keys) { "Tool arguments contain missing or unexpected fields." }
    }

    private fun <T> JsonArray.mapObject(transform: (JsonObject) -> T): List<T> = map { element ->
        require(element.isJsonObject) { "Tool list entries must be objects." }
        transform(element.asJsonObject)
    }

    private inline fun <reified T : Enum<T>> enumValue(value: String): T =
        enumValues<T>().singleOrNull { it.name == value } ?: error("Unknown ${T::class.java.simpleName} value.")

    private companion object {
        val ROOT_KEYS = setOf("plan_name", "overview", "days")
        val DAY_KEYS = setOf("day_of_week", "template_name", "notes", "exercises")
        val EXERCISE_KEYS = setOf("exercise_id", "target_sets", "target_reps", "notes")
        val TARGET_REPS = Regex("^(?:[1-9]\\d?(?:-[1-9]\\d?)?|[1-9]\\d? (?:sec|secs|seconds|min|mins|minutes))$", RegexOption.IGNORE_CASE)
    }
}
