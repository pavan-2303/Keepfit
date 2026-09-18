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
    fun acceptsValidatedNewExerciseWhenPersonalCatalogueIsEmpty() {
        val emptyContext = context.copy(exercises = emptyList())

        val plan = validator.validate(toolCall(newExerciseArguments()), emptyContext).getOrThrow()

        val exercise = plan.days.single().exercises.single()
        assertEquals("", exercise.exerciseId)
        assertEquals("Chair squat", exercise.name)
        assertEquals("Legs", exercise.newExercise?.muscleGroup)
        assertEquals("Chair", exercise.newExercise?.equipment)
        assertEquals(true, exercise.newExercise?.isBodyweight)
    }

    @Test
    fun rejectsIncompleteNewExerciseDefinition() {
        val result = validator.validate(
            toolCall(newExerciseArguments().replace("Sit back to the chair and stand with control.", "")),
            context.copy(exercises = emptyList()),
        )

        assertTrue(result.isFailure)
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
        val firstExercise = knownExerciseArguments("11111111-1111-1111-1111-111111111111")
        val secondExercise = knownExerciseArguments("22222222-2222-2222-2222-222222222222")
        val repeatedDay = planArguments("${dayArguments(firstExercise)},${dayArguments(secondExercise)}")
        val repeatedExercise = planArguments(dayArguments("$firstExercise,$firstExercise"))

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

    private fun validArguments() = planArguments(
        dayArguments(knownExerciseArguments("11111111-1111-1111-1111-111111111111")),
    )

    private fun planArguments(days: String) =
        """{"plan_name":"Strong start","overview":"A manageable full-body week.","days":[$days]}"""

    private fun dayArguments(exercises: String) =
        """{"day_of_week":"MONDAY","template_name":"Full body A","notes":"Move with control.","exercises":[$exercises]}"""

    private fun knownExerciseArguments(id: String) =
        """{"exercise_id":"$id","name":"","muscle_group":"","equipment":"","target_muscle":"","secondary_muscles":"","instructions":"","is_bodyweight":false,"target_sets":3,"target_reps":"8-12","notes":"Leave two reps in reserve."}"""

    private fun newExerciseArguments() =
        """{"plan_name":"Gentle start","overview":"A manageable first week.","days":[{"day_of_week":"MONDAY","template_name":"Foundation","notes":"Stop if pain increases.","exercises":[{"exercise_id":"","name":"Chair squat","muscle_group":"Legs","equipment":"Chair","target_muscle":"Quadriceps","secondary_muscles":"Glutes","instructions":"Sit back to the chair and stand with control.","is_bodyweight":true,"target_sets":2,"target_reps":"8-10","notes":"Use a comfortable range."}]}]}"""
}
