package com.keepfit.app.profile

data class ProfileInput(
    val displayName: String,
    val heightCm: Double?,
)

sealed interface ProfileValidationResult {
    data class Valid(val input: ProfileInput) : ProfileValidationResult
    data class Invalid(val message: String) : ProfileValidationResult
}

object ProfileInputValidator {
    fun validate(displayName: String, heightCm: String): ProfileValidationResult {
        val normalizedName = displayName.trim()
        if (normalizedName.isEmpty()) {
            return ProfileValidationResult.Invalid("Enter your name to continue.")
        }

        val normalizedHeight = heightCm.trim()
        val parsedHeight = normalizedHeight.takeIf(String::isNotEmpty)?.toDoubleOrNull()
        if (normalizedHeight.isNotEmpty() && (parsedHeight == null || parsedHeight !in 50.0..260.0)) {
            return ProfileValidationResult.Invalid("Enter a height between 50 and 260 cm.")
        }

        return ProfileValidationResult.Valid(
            ProfileInput(
                displayName = normalizedName,
                heightCm = parsedHeight,
            ),
        )
    }
}

