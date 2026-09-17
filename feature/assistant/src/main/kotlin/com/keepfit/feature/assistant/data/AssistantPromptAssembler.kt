package com.keepfit.feature.assistant.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssistantPromptAssembler @Inject constructor() {
    fun buildContextAwareChatPrompt(
        question: String,
        summary: AssistantLocalSummary,
    ): String = buildString {
        appendLine("Answer the user's question using the compact Keepfit snapshot when it is relevant.")
        appendLine("Say plainly when the snapshot does not contain enough evidence. Do not claim a diagnosis.")
        appendLine()
        appendLine("User question")
        appendLine(question.trim())
        appendLine()
        appendLine("Keepfit snapshot")
        appendLocalSummarySections(summary)
    }

    fun buildProgressSummaryPrompt(summary: AssistantLocalSummary): String = buildString {
        appendLine("You are Keepfit's private fitness assistant.")
        appendLine("Use only the local data below.")
        appendLine("If data is missing, say so plainly instead of guessing.")
        appendLine()
        appendLocalSummarySections(summary)
        appendLine("Write a concise progress summary with strengths, gaps, and one or two practical next steps.")
        appendLine("Do not give medical advice.")
    }

    fun buildDraftPlanPrompt(
        summary: AssistantLocalSummary,
        input: AssistantDraftInput,
    ): String = buildString {
        appendLine("You are Keepfit's private fitness assistant.")
        appendLine("Use the local data below plus the user's goal to draft a weekly workout plan.")
        appendLine("If data is missing, compensate conservatively instead of inventing history.")
        appendLine("Return JSON only. Do not wrap it in explanation before or after the JSON.")
        appendLine()
        appendLine("User goal")
        appendLine("- ${input.goal.trim()}")
        input.notes?.trim()?.takeIf { it.isNotEmpty() }?.let { appendLine("- Notes: $it") }
        appendLine()
        appendLine("Local data snapshot")
        appendLocalSummarySections(summary)
        appendLine()
        appendLine("Return this JSON shape exactly:")
        appendLine(
            """{"plan_name":"string","overview":"string","days":[{"day_of_week":"MONDAY","template_name":"string","notes":"string","exercises":[{"name":"string","target_sets":4,"target_reps":"6-8","notes":"string"}]}]}""",
        )
        appendLine("Use 3 to 5 training days unless the goal clearly calls for less.")
        appendLine("Keep exercise order practical and keep notes concise.")
        appendLine("Do not give medical advice.")
    }

    private fun StringBuilder.appendLocalSummarySections(summary: AssistantLocalSummary) {
        appendLine("Generated on: ${summary.generatedOn}")
        appendLine()

        appendLine("Recent workouts")
        if (summary.recentWorkouts.isEmpty()) {
            appendLine("- No recent workout sessions logged.")
        } else {
            summary.recentWorkouts.forEach { workout ->
                appendLine("- ${workout.workoutDate}: ${workout.exerciseNames.joinToString()}")
            }
        }
        appendLine()

        appendLine("Personal records")
        if (summary.records.isEmpty()) {
            appendLine("- No personal records available.")
        } else {
            summary.records.forEach { record ->
                appendLine("- ${record.exerciseName}: ${record.highestRepetitions} reps recorded")
            }
        }
        appendLine()

        val nutrition = summary.nutrition
        appendLine("Nutrition today (${nutrition.date})")
        if (!nutrition.hasEntries) {
            appendLine("- No meals logged today.")
        } else {
            appendLine(
                "- Totals: ${formatMetric(nutrition.calories)} kcal, " +
                    "${formatMetric(nutrition.proteinGrams)} g protein, " +
                    "${formatMetric(nutrition.carbohydrateGrams)} g carbs, " +
                    "${formatMetric(nutrition.fatGrams)} g fat",
            )
        }
        nutrition.calorieGoal?.let { appendLine("- ${formatMetric(it)} kcal goal") }
        nutrition.proteinGoalGrams?.let { appendLine("- ${formatMetric(it)} g protein goal") }
        nutrition.carbohydrateGoalGrams?.let { appendLine("- ${formatMetric(it)} g carbs goal") }
        nutrition.fatGoalGrams?.let { appendLine("- ${formatMetric(it)} g fat goal") }
        appendLine()

        appendLine("Progress")
        val progress = summary.progress
        appendLine("- Body measurements are excluded from assistant context.")
        progress.activeCycle?.let { cycle ->
            appendLine("- Active transformation cycle: started ${cycle.startDate}, latest day ${cycle.latestDayNumber}")
            appendLine("- Workouts completed: ${cycle.workoutsCompleted}")
            cycle.averageCalories?.let { appendLine("- Average calories: ${formatMetric(it)} kcal") }
            cycle.averageProteinGrams?.let { appendLine("- Average protein: ${formatMetric(it)} g") }
            cycle.averageCarbohydrateGrams?.let { appendLine("- Average carbs: ${formatMetric(it)} g") }
            cycle.averageFatGrams?.let { appendLine("- Average fat: ${formatMetric(it)} g") }
        }
        appendLine()

        summary.steps?.let { steps ->
            appendLine("Steps")
            appendLine("- Today steps: ${steps.todaySteps}")
            appendLine("- 7-day total: ${steps.sevenDayTotal}")
            appendLine("- 7-day average: ${steps.sevenDayAverage}")
            appendLine()
        }
    }

    private fun formatMetric(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)

}
