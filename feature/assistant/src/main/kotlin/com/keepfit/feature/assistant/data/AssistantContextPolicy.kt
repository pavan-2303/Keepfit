package com.keepfit.feature.assistant.data

import javax.inject.Inject
import javax.inject.Singleton

/** Selects local context only when a question is about the user's own records. */
@Singleton
class AssistantContextPolicy @Inject constructor() {
    fun shouldIncludeLocalContext(question: String): Boolean {
        val normalized = question.lowercase()
        val personalSignal = personalSignals.any(normalized::contains)
        val recordSignal = recordSignals.any(normalized::contains)
        return personalSignal && recordSignal
    }

    private companion object {
        val personalSignals = listOf(
            "my ", "mine", "i have", "i've", "am i", "did i", "for me",
        )
        val recordSignals = listOf(
            "progress", "recent", "history", "log", "record", "workout",
            "training", "strength", "stronger", "nutrition", "meal", "protein",
            "calorie", "step", "consistency", "week", "month", "trend",
        )
    }
}
