package com.keepfit.feature.assistant

import com.keepfit.feature.assistant.data.AssistantMessageRole
import com.keepfit.feature.assistant.data.parseOllamaDraftPlanResponse
import com.keepfit.feature.assistant.data.parseOllamaChatResponse
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OllamaResponseParsingTest {
    @Test
    fun parsesAssistantReplyFromChatPayload() {
        val json = """
            {
              "message": {
                "role": "assistant",
                "content": "Keep your volume steady this week."
              }
            }
        """.trimIndent()

        val result = parseOllamaChatResponse(json, createdAtUtcEpochMillis = 42L)

        assertTrue(result.isSuccess)
        val message = result.getOrThrow()
        assertEquals(AssistantMessageRole.ASSISTANT, message.role)
        assertEquals("Keep your volume steady this week.", message.content)
        assertEquals(42L, message.createdAtUtcEpochMillis)
    }

    @Test
    fun rejectsChatPayloadWithoutAssistantContent() {
        val json = """
            {
              "message": {
                "role": "assistant",
                "content": ""
              }
            }
        """.trimIndent()

        val result = parseOllamaChatResponse(json, createdAtUtcEpochMillis = 42L)

        assertTrue(result.isFailure)
        assertEquals(
            "Assistant response did not include a message.",
            result.exceptionOrNull()?.message,
        )
    }

    @Test
    fun parsesStructuredDraftPlanResponse() {
        val json = """
            ```json
            {
              "plan_name": "Balanced Week",
              "overview": "A four-day split that emphasizes consistency.",
              "days": [
                {
                  "day_of_week": "MONDAY",
                  "template_name": "Upper A",
                  "notes": "Start the week with compound pressing and rowing.",
                  "exercises": [
                    {
                      "name": "Bench Press",
                      "target_sets": 4,
                      "target_reps": "6-8"
                    },
                    {
                      "name": "Chest-Supported Row",
                      "target_sets": 4,
                      "target_reps": "8-10",
                      "notes": "Pause briefly at the top."
                    }
                  ]
                },
                {
                  "day_of_week": "THURSDAY",
                  "template_name": "Lower A",
                  "exercises": [
                    {
                      "name": "Back Squat",
                      "target_sets": 4,
                      "target_reps": "5-8"
                    }
                  ]
                }
              ]
            }
            ```
        """.trimIndent()

        val result = parseOllamaDraftPlanResponse(json)

        assertTrue(result.isSuccess)
        val plan = result.getOrThrow()
        assertEquals("Balanced Week", plan.name)
        assertEquals("A four-day split that emphasizes consistency.", plan.overview)
        assertEquals(2, plan.days.size)
        assertEquals(DayOfWeek.MONDAY, plan.days.first().dayOfWeek)
        assertEquals("Upper A", plan.days.first().templateName)
        assertEquals(2, plan.days.first().exercises.size)
        assertEquals("Bench Press", plan.days.first().exercises.first().name)
        assertEquals(4, plan.days.first().exercises.first().targetSets)
        assertEquals("6-8", plan.days.first().exercises.first().targetReps)
    }

    @Test
    fun rejectsDraftPlanResponseMissingRequiredDayFields() {
        val json = """
            {
              "plan_name": "Broken Week",
              "days": [
                {
                  "template_name": "Upper A",
                  "exercises": [
                    {
                      "name": "Bench Press"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = parseOllamaDraftPlanResponse(json)

        assertTrue(result.isFailure)
        assertEquals(
            "Assistant draft plan response was incomplete or malformed.",
            result.exceptionOrNull()?.message,
        )
    }
}
