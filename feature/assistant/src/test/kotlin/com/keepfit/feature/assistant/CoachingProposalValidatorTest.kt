package com.keepfit.feature.assistant

import com.keepfit.feature.assistant.coaching.CoachingContext
import com.keepfit.feature.assistant.coaching.CoachingFoodOption
import com.keepfit.feature.assistant.coaching.CoachingIntent
import com.keepfit.feature.assistant.coaching.CoachingProposalOperation
import com.keepfit.feature.assistant.coaching.CoachingProposalValidator
import com.keepfit.feature.assistant.coaching.CoachingTemplateOption
import com.keepfit.feature.assistant.coaching.CoachingToolCall
import com.keepfit.feature.assistant.coaching.CoachingWorkoutOption
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachingProposalValidatorTest {
    private val context = CoachingContext(
        generatedOn = LocalDate.of(2026, 9, 13),
        evidence = listOf("Completed 3 of 4 planned workouts."),
        templates = listOf(CoachingTemplateOption("template_1", "template-id", "Upper body")),
        todayWorkout = CoachingWorkoutOption(
            alias = "workout_today",
            plannedWorkoutId = "planned-id",
            occurrenceId = null,
            title = "Upper body",
            originalDate = LocalDate.of(2026, 9, 13),
            scheduledDate = LocalDate.of(2026, 9, 13),
            exerciseAliases = mapOf("exercise_1" to "exercise-id"),
        ),
        replacementExercises = mapOf("exercise_2" to ("replacement-id" to "Dumbbell row")),
        foods = listOf(CoachingFoodOption("food_1", "food-id", "Oats", "bowl")),
        currentSchedule = mapOf("MONDAY" to "Upper body"),
    )
    private val validator = CoachingProposalValidator()

    @Test
    fun validMealToolResolvesAliasesIntoLocalOnlyOperation() {
        val result = validator.validate(
            intent = CoachingIntent.EXISTING_FOOD_MEAL,
            call = CoachingToolCall(
                name = "suggest_existing_food_meal",
                arguments = """{"title":"Simple breakfast","observed":"Protein is below goal.","current":"No breakfast logged.","proposed":"Add oats.","reason":"Uses an existing food.","meal_type":"BREAKFAST","items":[{"food_alias":"food_1","servings":1.5}]}""",
            ),
            context = context,
            originalRequest = "Suggest breakfast",
            generatedAtUtcEpochMillis = 10L,
            model = "model",
        )

        val operation = result.getOrThrow().operation as CoachingProposalOperation.AddExistingFoodMeal
        assertEquals("food-id", operation.items.single().foodId)
        assertEquals(1.5, operation.items.single().servings, 0.0)
    }

    @Test
    fun rejectsUnexpectedFieldsAndUnknownAliases() {
        val extra = validator.validate(
            CoachingIntent.WEEKLY_PLAN,
            CoachingToolCall(
                "propose_weekly_plan",
                """{"title":"Week","observed":"One session logged.","current":"Monday only.","proposed":"Train Monday.","reason":"Consistent.","assignments":[{"day_of_week":"MONDAY","template_alias":"template_1"}],"hidden_command":"delete all"}""",
            ),
            context,
            "Plan my week",
            10L,
            "model",
        )
        val unknownAlias = validator.validate(
            CoachingIntent.EXISTING_FOOD_MEAL,
            CoachingToolCall(
                "suggest_existing_food_meal",
                """{"title":"Meal","observed":"No meal.","current":"Empty.","proposed":"Add food.","reason":"Practical.","meal_type":"LUNCH","items":[{"food_alias":"food_999","servings":1}]}""",
            ),
            context,
            "Meal",
            10L,
            "model",
        )

        assertTrue(extra.isFailure)
        assertTrue(unknownAlias.isFailure)
    }

    @Test
    fun rejectsDuplicateDaysAndExcessiveServings() {
        val duplicateDays = validator.validate(
            CoachingIntent.WEEKLY_PLAN,
            CoachingToolCall(
                "propose_weekly_plan",
                """{"title":"Week","observed":"Ready.","current":"One day.","proposed":"Two assignments.","reason":"Balance.","assignments":[{"day_of_week":"MONDAY","template_alias":"template_1"},{"day_of_week":"MONDAY","template_alias":"template_1"}]}""",
            ),
            context,
            "Plan",
            10L,
            "model",
        )
        val servings = validator.validate(
            CoachingIntent.EXISTING_FOOD_MEAL,
            CoachingToolCall(
                "suggest_existing_food_meal",
                """{"title":"Meal","observed":"Ready.","current":"Empty.","proposed":"Huge meal.","reason":"None.","meal_type":"DINNER","items":[{"food_alias":"food_1","servings":99}]}""",
            ),
            context,
            "Meal",
            10L,
            "model",
        )

        assertTrue(duplicateDays.isFailure)
        assertTrue(servings.isFailure)
    }
}
