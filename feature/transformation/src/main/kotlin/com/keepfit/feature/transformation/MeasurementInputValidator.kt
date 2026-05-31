package com.keepfit.feature.transformation

import java.time.LocalDate

data class MeasurementInput(
    val measurementDate: LocalDate,
    val weightKg: Double?,
    val waistCm: Double?,
    val chestCm: Double?,
    val hipsCm: Double?,
    val leftArmCm: Double?,
    val rightArmCm: Double?,
    val leftThighCm: Double?,
    val rightThighCm: Double?,
    val notes: String?,
)

object MeasurementInputValidator {
    fun validate(
        measurementDate: String,
        weightKg: String,
        waistCm: String,
        chestCm: String,
        hipsCm: String,
        leftArmCm: String,
        rightArmCm: String,
        leftThighCm: String,
        rightThighCm: String,
        notes: String,
    ): Result<MeasurementInput> {
        val parsedDate = runCatching { LocalDate.parse(measurementDate.trim()) }
            .getOrElse { return Result.failure(IllegalArgumentException("Enter a valid measurement date.")) }
        val normalized = listOf(
            "Weight" to weightKg.toPositiveOptionalDoubleOrNull(),
            "Waist" to waistCm.toPositiveOptionalDoubleOrNull(),
            "Chest" to chestCm.toPositiveOptionalDoubleOrNull(),
            "Hips" to hipsCm.toPositiveOptionalDoubleOrNull(),
            "Left arm" to leftArmCm.toPositiveOptionalDoubleOrNull(),
            "Right arm" to rightArmCm.toPositiveOptionalDoubleOrNull(),
            "Left thigh" to leftThighCm.toPositiveOptionalDoubleOrNull(),
            "Right thigh" to rightThighCm.toPositiveOptionalDoubleOrNull(),
        )
        normalized.firstOrNull { (_, value) -> value == InvalidDouble }?.let { (label, _) ->
            return Result.failure(IllegalArgumentException("$label must be greater than 0."))
        }
        val values = normalized.map { it.second as Double? }
        val normalizedNotes = notes.trim().ifEmpty { null }
        if (values.all { it == null } && normalizedNotes == null) {
            return Result.failure(IllegalArgumentException("Add at least one measurement or note."))
        }
        return Result.success(
            MeasurementInput(
                measurementDate = parsedDate,
                weightKg = values[0],
                waistCm = values[1],
                chestCm = values[2],
                hipsCm = values[3],
                leftArmCm = values[4],
                rightArmCm = values[5],
                leftThighCm = values[6],
                rightThighCm = values[7],
                notes = normalizedNotes,
            ),
        )
    }
}

private object InvalidDouble

private fun String.toPositiveOptionalDoubleOrNull(): Any? {
    val trimmed = trim()
    if (trimmed.isEmpty()) {
        return null
    }
    return trimmed.toDoubleOrNull()?.takeIf { it > 0.0 } ?: InvalidDouble
}
