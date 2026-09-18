package com.keepfit.feature.assistant.planning

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.keepfit.feature.assistant.coaching.CoachingToolContract

object AssistantPlanToolContract {
    fun create(): CoachingToolContract = CoachingToolContract(
        name = "create_workout_plan",
        description = "Create a safe weekly workout draft using supplied exercise identifiers or complete new exercise definitions.",
        parameters = objectSchema(
            properties = mapOf(
                "plan_name" to stringSchema(80),
                "overview" to stringSchema(300),
                "days" to arraySchema(1, 4, objectSchema(
                    properties = mapOf(
                        "day_of_week" to enumSchema(DAYS),
                        "template_name" to stringSchema(80),
                        "notes" to stringSchema(240, allowEmpty = true),
                        "exercises" to arraySchema(1, 8, objectSchema(
                            properties = mapOf(
                                "exercise_id" to stringSchema(64, allowEmpty = true),
                                "name" to stringSchema(80, allowEmpty = true),
                                "muscle_group" to stringSchema(60, allowEmpty = true),
                                "equipment" to stringSchema(60, allowEmpty = true),
                                "target_muscle" to stringSchema(60, allowEmpty = true),
                                "secondary_muscles" to stringSchema(120, allowEmpty = true),
                                "instructions" to stringSchema(500, allowEmpty = true),
                                "is_bodyweight" to booleanSchema(),
                                "target_sets" to integerSchema(1, 6),
                                "target_reps" to stringSchema(20),
                                "notes" to stringSchema(160, allowEmpty = true),
                            ),
                            required = listOf(
                                "exercise_id", "name", "muscle_group", "equipment",
                                "target_muscle", "secondary_muscles", "instructions",
                                "is_bodyweight", "target_sets", "target_reps", "notes",
                            ),
                        )),
                    ),
                    required = listOf("day_of_week", "template_name", "notes", "exercises"),
                )),
            ),
            required = listOf("plan_name", "overview", "days"),
        ),
    )

    private fun stringSchema(maxLength: Int, allowEmpty: Boolean = false) = JsonObject().apply {
        addProperty("type", "string")
        addProperty("minLength", if (allowEmpty) 0 else 1)
        addProperty("maxLength", maxLength)
    }

    private fun enumSchema(values: List<String>) = stringSchema(12).apply {
        add("enum", JsonArray().apply { values.forEach(::add) })
    }

    private fun integerSchema(minimum: Int, maximum: Int) = JsonObject().apply {
        addProperty("type", "integer")
        addProperty("minimum", minimum)
        addProperty("maximum", maximum)
    }

    private fun booleanSchema() = JsonObject().apply { addProperty("type", "boolean") }

    private fun arraySchema(min: Int, max: Int, items: JsonObject) = JsonObject().apply {
        addProperty("type", "array")
        addProperty("minItems", min)
        addProperty("maxItems", max)
        add("items", items)
    }

    private fun objectSchema(properties: Map<String, JsonObject>, required: List<String>) = JsonObject().apply {
        addProperty("type", "object")
        addProperty("additionalProperties", false)
        add("properties", JsonObject().apply { properties.forEach(::add) })
        add("required", JsonArray().apply { required.forEach(::add) })
    }

    private val DAYS = java.time.DayOfWeek.entries.map { it.name }
}
