package com.keepfit.feature.assistant.coaching

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoachingProposalValidator @Inject constructor() {
    fun validate(
        intent: CoachingIntent,
        call: CoachingToolCall,
        context: CoachingContext,
        originalRequest: String,
        generatedAtUtcEpochMillis: Long,
        model: String,
    ): Result<CoachingProposal> = runCatching {
        val contract = CoachingToolContract.forIntent(intent)
        require(call.name == contract.name) { "Unexpected coaching tool '${call.name}'." }
        val json = JsonParser.parseString(call.arguments).asJsonObject
        val operation = when (intent) {
            CoachingIntent.WEEKLY_SUMMARY -> {
                json.requireOnly(COMMON_KEYS)
                CoachingProposalOperation.None
            }
            CoachingIntent.WEEKLY_PLAN -> validateSchedule(json, context)
            CoachingIntent.SCHEDULE_CHANGE -> validateReschedule(json, context)
            CoachingIntent.WORKOUT_SHORTENING -> validateShortening(json, context)
            CoachingIntent.EXERCISE_SUBSTITUTION -> validateSubstitution(json, context)
            CoachingIntent.EXISTING_FOOD_MEAL -> validateMeal(json, context)
        }
        CoachingProposal(
            id = "proposal-$generatedAtUtcEpochMillis",
            intent = intent,
            title = json.requiredText("title", 80),
            observed = json.requiredText("observed", 240),
            current = json.requiredText("current", 300),
            proposed = json.requiredText("proposed", 300),
            reason = json.requiredText("reason", 240),
            operation = operation,
            originalRequest = originalRequest.trim().take(500),
            generatedAtUtcEpochMillis = generatedAtUtcEpochMillis,
            model = model,
        )
    }

    private fun validateSchedule(json: JsonObject, context: CoachingContext): CoachingProposalOperation {
        json.requireOnly(COMMON_KEYS + "assignments")
        val items = json.requiredArray("assignments", 1, 6)
        val assignments = items.mapObject { item ->
            item.requireOnly(setOf("day_of_week", "template_alias"))
            val day = enumValue<DayOfWeek>(item.requiredText("day_of_week", 12))
            val templateAlias = item.requiredText("template_alias", 40)
            val template = context.templates.singleOrNull { it.alias == templateAlias }
                ?: error("Unknown workout template alias.")
            ScheduleAssignment(day.name, template.localId, template.name)
        }
        require(assignments.map { it.dayOfWeek }.distinct().size == assignments.size) {
            "A weekday can be assigned only once."
        }
        return CoachingProposalOperation.ReplaceWeeklySchedule(assignments)
    }

    private fun validateReschedule(json: JsonObject, context: CoachingContext): CoachingProposalOperation {
        json.requireOnly(COMMON_KEYS + setOf("workout_alias", "target_date"))
        val workout = context.resolveWorkout(json.requiredText("workout_alias", 40))
        val target = LocalDate.parse(json.requiredText("target_date", 10))
        require(target.isAfter(context.generatedOn) && !target.isAfter(context.generatedOn.plusDays(14))) {
            "The proposed date must be within the next fourteen days."
        }
        return workout.toAdjustment(TodayAdjustmentType.RESCHEDULED, targetDate = target)
    }

    private fun validateShortening(json: JsonObject, context: CoachingContext): CoachingProposalOperation {
        json.requireOnly(COMMON_KEYS + setOf("workout_alias", "variant"))
        val workout = context.resolveWorkout(json.requiredText("workout_alias", 40))
        val type = enumValue<TodayAdjustmentType>(json.requiredText("variant", 12))
        require(type == TodayAdjustmentType.SHORTENED || type == TodayAdjustmentType.MINIMUM)
        return workout.toAdjustment(type)
    }

    private fun validateSubstitution(json: JsonObject, context: CoachingContext): CoachingProposalOperation {
        json.requireOnly(COMMON_KEYS + setOf("workout_alias", "source_exercise_alias", "replacement_exercise_alias"))
        val workout = context.resolveWorkout(json.requiredText("workout_alias", 40))
        val sourceAlias = json.requiredText("source_exercise_alias", 40)
        val sourceId = workout.exerciseAliases[sourceAlias] ?: error("Unknown source exercise alias.")
        val replacementAlias = json.requiredText("replacement_exercise_alias", 40)
        val replacementId = context.replacementExercises[replacementAlias]?.first
            ?: error("Unknown replacement exercise alias.")
        require(sourceId != replacementId) { "The replacement must be a different exercise." }
        return workout.toAdjustment(
            TodayAdjustmentType.SUBSTITUTED,
            sourceExerciseId = sourceId,
            replacementExerciseId = replacementId,
        )
    }

    private fun validateMeal(json: JsonObject, context: CoachingContext): CoachingProposalOperation {
        json.requireOnly(COMMON_KEYS + setOf("meal_type", "items"))
        val mealType = json.requiredText("meal_type", 12)
        require(mealType in MEAL_TYPES) { "Unknown meal type." }
        val items = json.requiredArray("items", 1, 6).mapObject { item ->
            item.requireOnly(setOf("food_alias", "servings"))
            val food = context.foods.singleOrNull { it.alias == item.requiredText("food_alias", 40) }
                ?: error("Unknown food alias.")
            val servings = item.get("servings")?.takeIf { it.isJsonPrimitive }?.asDouble
                ?: error("Servings are required.")
            require(servings.isFinite() && servings in 0.25..5.0) { "Servings must be from 0.25 to 5." }
            FoodAmount(food.localId, food.name, servings)
        }
        require(items.map { it.foodId }.distinct().size == items.size) { "A food can appear only once." }
        return CoachingProposalOperation.AddExistingFoodMeal(context.generatedOn, mealType, items)
    }

    private fun CoachingContext.resolveWorkout(alias: String): CoachingWorkoutOption =
        todayWorkout?.takeIf { it.alias == alias } ?: error("Unknown workout alias.")

    private fun CoachingWorkoutOption.toAdjustment(
        type: TodayAdjustmentType,
        targetDate: LocalDate? = null,
        sourceExerciseId: String? = null,
        replacementExerciseId: String? = null,
    ) = CoachingProposalOperation.AdjustTodayWorkout(
        plannedWorkoutId = plannedWorkoutId,
        occurrenceId = occurrenceId,
        originalDate = originalDate,
        type = type,
        targetDate = targetDate,
        sourceExerciseId = sourceExerciseId,
        replacementExerciseId = replacementExerciseId,
    )

    private fun JsonObject.requiredText(name: String, maxLength: Int): String {
        val value = get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString?.trim()
            ?: error("$name is required.")
        require(value.isNotEmpty() && value.length <= maxLength && value.none(Char::isISOControl)) {
            "$name is invalid."
        }
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
        val COMMON_KEYS = setOf("title", "observed", "current", "proposed", "reason")
        val MEAL_TYPES = setOf("BREAKFAST", "LUNCH", "DINNER", "SNACK")
    }
}
