package com.keepfit.app.profile

import java.time.LocalDate

data class ProfileInput(
    val displayName: String,
    val heightCm: Double?,
    val birthDate: LocalDate? = null,
    val startingWeightKg: Double? = null,
)

sealed interface ProfileValidationResult {
    data class Valid(val input: ProfileInput) : ProfileValidationResult
    data class Invalid(val message: String) : ProfileValidationResult
}

object ProfileInputValidator {
    fun validate(
        displayName: String,
        heightCm: String,
        birthDate: LocalDate? = null,
        startingWeightKg: String = "",
        today: LocalDate = LocalDate.now(),
    ): ProfileValidationResult {
        val normalizedName = displayName.trim()
        if (normalizedName.isEmpty()) {
            return ProfileValidationResult.Invalid("Enter your name to continue.")
        }

        val normalizedHeight = heightCm.trim()
        val parsedHeight = normalizedHeight.takeIf(String::isNotEmpty)?.toDoubleOrNull()
        if (normalizedHeight.isNotEmpty() && (parsedHeight == null || parsedHeight !in 50.0..260.0)) {
            return ProfileValidationResult.Invalid("Enter a height between 50 and 260 cm.")
        }

        if (birthDate != null && !birthDate.isBefore(today)) {
            return ProfileValidationResult.Invalid("Choose a birth date in the past.")
        }
        if (birthDate != null && birthDate.isBefore(today.minusYears(120))) {
            return ProfileValidationResult.Invalid("Choose a birth date within the past 120 years.")
        }

        val normalizedWeight = startingWeightKg.trim()
        val parsedWeight = normalizedWeight.takeIf(String::isNotEmpty)?.toDoubleOrNull()
        if (normalizedWeight.isNotEmpty() && (parsedWeight == null || parsedWeight !in 10.0..500.0)) {
            return ProfileValidationResult.Invalid("Enter a starting weight between 10 and 500 kg.")
        }

        return ProfileValidationResult.Valid(
            ProfileInput(
                displayName = normalizedName,
                heightCm = parsedHeight,
                birthDate = birthDate,
                startingWeightKg = parsedWeight,
            ),
        )
    }
}
