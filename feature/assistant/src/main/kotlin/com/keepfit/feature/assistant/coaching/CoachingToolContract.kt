package com.keepfit.feature.assistant.coaching

import com.google.gson.JsonArray
import com.google.gson.JsonObject

data class CoachingToolContract(
    val name: String,
    val description: String,
    val parameters: JsonObject,
) {
    companion object {
        fun forIntent(intent: CoachingIntent): CoachingToolContract {
            val name = intent.toolName()
            val description = intent.toolDescription()
            val properties = JsonObject().apply {
                add("title", stringSchema(maxLength = 80))
                add("observed", stringSchema(maxLength = 240))
                add("current", stringSchema(maxLength = 300))
                add("proposed", stringSchema(maxLength = 300))
                add("reason", stringSchema(maxLength = 240))
            }
            val required = mutableListOf("title", "observed", "current", "proposed", "reason")
            when (intent) {
                CoachingIntent.WEEKLY_SUMMARY -> Unit
                CoachingIntent.WEEKLY_PLAN -> {
                    properties.add("assignments", arraySchema(1, 6, objectSchema(
                        properties = mapOf(
                            "day_of_week" to enumSchema(DAYS),
                            "template_alias" to stringSchema(),
                        ),
                        required = listOf("day_of_week", "template_alias"),
                    )))
                    required += "assignments"
                }
                CoachingIntent.SCHEDULE_CHANGE -> {
                    properties.add("workout_alias", stringSchema())
                    properties.add("target_date", stringSchema(format = "date"))
                    required += listOf("workout_alias", "target_date")
                }
                CoachingIntent.WORKOUT_SHORTENING -> {
                    properties.add("workout_alias", stringSchema())
                    properties.add("variant", enumSchema(listOf("SHORTENED", "MINIMUM")))
                    required += listOf("workout_alias", "variant")
                }
                CoachingIntent.EXERCISE_SUBSTITUTION -> {
                    properties.add("workout_alias", stringSchema())
                    properties.add("source_exercise_alias", stringSchema())
                    properties.add("replacement_exercise_alias", stringSchema())
                    required += listOf("workout_alias", "source_exercise_alias", "replacement_exercise_alias")
                }
                CoachingIntent.EXISTING_FOOD_MEAL -> {
                    properties.add("meal_type", enumSchema(MEAL_TYPES))
                    properties.add("items", arraySchema(1, 6, objectSchema(
                        properties = mapOf(
                            "food_alias" to stringSchema(),
                            "servings" to numberSchema(0.25, 5.0),
                        ),
                        required = listOf("food_alias", "servings"),
                    )))
                    required += listOf("meal_type", "items")
                }
            }
            val propertyMap = properties.entrySet().associate { it.key to it.value.asJsonObject }
            return CoachingToolContract(name, description, objectSchema(propertyMap, required))
        }

        private fun CoachingIntent.toolName(): String = when (this) {
            CoachingIntent.WEEKLY_SUMMARY -> "summarize_week"
            CoachingIntent.WEEKLY_PLAN -> "propose_weekly_plan"
            CoachingIntent.SCHEDULE_CHANGE -> "propose_schedule_change"
            CoachingIntent.WORKOUT_SHORTENING -> "propose_workout_shortening"
            CoachingIntent.EXERCISE_SUBSTITUTION -> "propose_exercise_substitution"
            CoachingIntent.EXISTING_FOOD_MEAL -> "suggest_existing_food_meal"
        }

        private fun CoachingIntent.toolDescription(): String = when (this) {
            CoachingIntent.WEEKLY_SUMMARY -> "Summarize the supplied weekly evidence without changing data."
            CoachingIntent.WEEKLY_PLAN -> "Propose one to six weekday assignments using only supplied template aliases."
            CoachingIntent.SCHEDULE_CHANGE -> "Move the supplied workout to one date within the next fourteen days."
            CoachingIntent.WORKOUT_SHORTENING -> "Choose a shorter or minimum variant for the supplied workout."
            CoachingIntent.EXERCISE_SUBSTITUTION -> "Replace one supplied workout exercise with one supplied library exercise."
            CoachingIntent.EXISTING_FOOD_MEAL -> "Suggest one meal using only supplied food aliases and conservative servings."
        }

        private fun stringSchema(maxLength: Int = 40, format: String? = null) = JsonObject().apply {
            addProperty("type", "string")
            addProperty("minLength", 1)
            addProperty("maxLength", maxLength)
            format?.let { addProperty("format", it) }
        }

        private fun enumSchema(values: List<String>) = stringSchema().apply {
            add("enum", JsonArray().apply { values.forEach(::add) })
        }

        private fun numberSchema(minimum: Double, maximum: Double) = JsonObject().apply {
            addProperty("type", "number")
            addProperty("minimum", minimum)
            addProperty("maximum", maximum)
        }

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

        private val DAYS = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
        private val MEAL_TYPES = listOf("BREAKFAST", "LUNCH", "DINNER", "SNACK")
    }
}
