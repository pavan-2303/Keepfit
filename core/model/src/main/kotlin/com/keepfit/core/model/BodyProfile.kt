package com.keepfit.core.model

import java.time.LocalDate

data class BodyProfile(
    val id: String,
    val displayName: String,
    val heightCm: Double?,
    val birthDate: LocalDate?,
    val createdAt: Long,
    val updatedAt: Long,
)

