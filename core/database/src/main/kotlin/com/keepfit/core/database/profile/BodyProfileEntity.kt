package com.keepfit.core.database.profile

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "body_profiles")
data class BodyProfileEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val heightCm: Double?,
    val birthDate: LocalDate?,
    val createdAt: Long,
    val updatedAt: Long,
)

