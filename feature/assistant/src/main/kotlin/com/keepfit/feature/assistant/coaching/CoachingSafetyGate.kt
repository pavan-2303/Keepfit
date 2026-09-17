package com.keepfit.feature.assistant.coaching

import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

sealed interface CoachingSafetyDecision {
    data object Allowed : CoachingSafetyDecision
    data class Refused(val message: String) : CoachingSafetyDecision
}

class CoachingSafetyRefusalException(val guidance: String) : IllegalArgumentException(guidance)

@Singleton
class CoachingSafetyGate @Inject constructor() {
    fun evaluate(request: String): CoachingSafetyDecision {
        val text = request.lowercase(Locale.ROOT).replace(Regex("\\s+"), " ").trim()
        val refusal = when {
            text.containsAny(MEDICAL_TERMS) ->
                "Keepfit cannot diagnose symptoms or provide medical treatment. Stop if you feel unwell and contact a qualified healthcare professional; seek urgent care for severe or sudden symptoms."
            text.containsAny(MEDICATION_TERMS) ->
                "Keepfit cannot advise starting, stopping, or changing medication. Ask the prescribing clinician or a pharmacist."
            text.containsAny(REHABILITATION_TERMS) ->
                "Keepfit cannot design injury rehabilitation. A qualified physiotherapist or clinician can assess the limitation and provide an appropriate plan."
            text.containsAny(EATING_DISORDER_TERMS) || EXTREME_CALORIE_PATTERN.containsMatchIn(text) || text.containsAny(EXTREME_DIET_TERMS) ->
                "Keepfit cannot create extreme restriction or compensatory eating plans. Choose a sustainable goal and speak with a qualified clinician or dietitian if eating or weight concerns feel difficult to control."
            text.containsAny(UNSAFE_PROGRESSION_TERMS) ->
                "Keepfit cannot recommend training through sharp pain or abrupt load increases. Use a conservative progression and stop the movement if pain occurs."
            else -> null
        }
        return refusal?.let(CoachingSafetyDecision::Refused) ?: CoachingSafetyDecision.Allowed
    }

    private fun String.containsAny(terms: Set<String>): Boolean = terms.any(::contains)

    private companion object {
        val MEDICAL_TERMS = setOf(
            "diagnose", "chest pain", "fainting", "passed out", "heart attack", "shortness of breath",
        )
        val MEDICATION_TERMS = setOf(
            "medication", "medicine dose", "stop taking", "blood pressure pills", "insulin dose", "prescription",
        )
        val REHABILITATION_TERMS = setOf(
            "rehabilitation", "rehab plan", "torn acl", "torn rotator", "recover from surgery", "injury therapy",
        )
        val EATING_DISORDER_TERMS = setOf(
            "purge", "make myself vomit", "starve", "anorexia", "bulimia", "binge and compensate",
        )
        val EXTREME_DIET_TERMS = setOf(
            "crash diet", "stop eating", "lose 10kg in a week", "lose 10 kg in a week", "water fast for",
        )
        val UNSAFE_PROGRESSION_TERMS = setOf(
            "double all my", "double my lifting", "max out every day", "train through sharp", "lift through pain",
            "ignore the pain", "add 50kg", "add 50 kg",
        )
        val EXTREME_CALORIE_PATTERN = Regex("(?:^|\\D)(?:[1-7]\\d{2})\\s*(?:kcal|calorie).*(?:day|daily)")
    }
}
