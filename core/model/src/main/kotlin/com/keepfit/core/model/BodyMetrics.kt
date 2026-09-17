package com.keepfit.core.model

import java.time.LocalDate
import java.time.Period
import kotlin.math.pow
import kotlin.math.round

fun calculateBodyMassIndex(heightCm: Double?, weightKg: Double?): Double? {
    val height = heightCm?.takeIf { it > 0.0 } ?: return null
    val weight = weightKg?.takeIf { it > 0.0 } ?: return null
    val bmi = weight / (height / 100.0).pow(2)
    return round(bmi * 10.0) / 10.0
}

fun calculateAge(birthDate: LocalDate?, today: LocalDate): Int? {
    val date = birthDate?.takeUnless { it.isAfter(today) } ?: return null
    return Period.between(date, today).years
}
