package com.keepfit.feature.assistant

import com.keepfit.feature.assistant.coaching.CoachingSafetyDecision
import com.keepfit.feature.assistant.coaching.CoachingSafetyGate
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachingSafetyGateTest {
    private val gate = CoachingSafetyGate()

    @Test
    fun ordinaryFitnessRequestsRemainAvailable() {
        val requests = listOf(
            "Move my Friday workout to Saturday",
            "Make today's workout shorter",
            "Suggest a breakfast using my saved foods",
            "Summarize my training week",
            "Swap squats for another exercise I already have",
        )

        requests.forEach { request ->
            assertTrue(request, gate.evaluate(request) is CoachingSafetyDecision.Allowed)
        }
    }

    @Test
    fun highRiskRequestsAreRefusedBeforeDispatch() {
        val requests = listOf(
            "Diagnose why my chest hurts during training",
            "Tell me whether to stop my blood pressure medication",
            "Create rehabilitation exercises for my torn ACL",
            "Give me an 500 calorie per day crash diet",
            "Double all my lifting weights tomorrow",
            "Help me train through sharp knee pain",
        )

        requests.forEach { request ->
            assertTrue(request, gate.evaluate(request) is CoachingSafetyDecision.Refused)
        }
    }
}
