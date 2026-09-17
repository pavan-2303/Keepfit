package com.keepfit.feature.steps.data

import java.time.LocalDate

interface StepsRepository {
    val requiredPermissions: Set<String>

    suspend fun loadSnapshot(): StepsSnapshot

    suspend fun loadTotal(startDate: LocalDate, endDate: LocalDate): Long? = null
}
