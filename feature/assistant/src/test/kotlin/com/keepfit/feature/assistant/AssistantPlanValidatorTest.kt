package com.keepfit.feature.assistant

import com.keepfit.feature.assistant.coaching.CoachingToolCall
import com.keepfit.feature.assistant.planning.AssistantPlanContext
import com.keepfit.feature.assistant.planning.AssistantPlanExerciseOption
import com.keepfit.feature.assistant.planning.AssistantPlanValidator
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantPlanValidatorTest {
    private val context = AssistantPlanContext(
        goal = "Build muscle",
        experience = "Beginner",
        sessionMinutes = 30,
        preferredDays = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
        equipment = setOf("Dumbbells"),
        exercises = listOf(
            AssistantPlanExerciseOption("11111111-1111-1111-1111-111111111111", "Goblet squat", "Dumbbell", "Quads"),
            AssistantPlanExerciseOption("22222222-2222-2222-2222-222222222222", "Dumbbell row", "Dumbbell", "Back"),
        ),
    )
    private val validator = AssistantPlanValidator()

    @Test
    fun resolvesKnownExerciseIdentifiersToLocalNames() {
        val plan = validator.validate(toolCall(validArguments()), context).getOrThrow()

        assertEquals("11111111-1111-1111-1111-111111111111", plan.days.single().exercises.first().exerciseId)
        assertEquals("Goblet squat", plan.days.single().exercises.first().name)
        assertEquals(3, plan.days.single().exercises.first().targetSets)
    }

    @Test
    fun rejectsUnknownExerciseIdentifier() {
        val result = validator.validate(
            toolCall(validArguments().replace("11111111-1111-1111-1111-111111111111", "99999999-9999-9999-9999-999999999999")),
            context,
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("not available"))
    }

    @Test
    fun rejectsRepeatedDaysAndRepeatedExercises() {
        val repeatedDay = validArguments().replace(
            "]}",
            "]},{\"day_of_week\":\"MONDAY\",\"template_name\":\"Repeat\",\"notes\":\"\",\"exercises\":[{\"exercise_id\":\"22222222-2222-2222-2222-222222222222\",\"target_sets\":3,\"target_reps\":\"8-12\",\"notes\":\"\"}]}]}",
        )
        val repeatedExercise = validArguments().replace(
            "]}]}",
            ",{\"exercise_id\":\"11111111-1111-1111-1111-111111111111\",\"target_sets\":3,\"target_reps\":\"8-12\",\"notes\":\"\"}]}]}",
        )

        assertTrue(validator.validate(toolCall(repeatedDay), context).isFailure)
        assertTrue(validator.validate(toolCall(repeatedExercise), context).isFailure)
    }

    @Test
    fun rejectsUnexpectedFieldsAndTargetsOutsideBounds() {
        val unexpected = validArguments().replace("\"overview\":", "\"medical_advice\":\"none\",\"overview\":")
        val excessiveSets = validArguments().replace("\"target_sets\":3", "\"target_sets\":12")

        assertTrue(validator.validate(toolCall(unexpected), context).isFailure)
        assertTrue(validator.validate(toolCall(excessiveSets), context).isFailure)
    }

    private fun toolCall(arguments: String) = CoachingToolCall("create_workout_plan", arguments)

    private fun validArguments() =
        """{"plan_name":"Strong start","overview":"A manageable full-body week.","days":[{"day_of_week":"MONDAY","template_name":"Full body A","notes":"Move with control.","exercises":[{"exercise_id":"11111111-1111-1111-1111-111111111111","target_sets":3,"target_reps":"8-12","notes":"Leave two reps in reserve."}]}]}"""
}
