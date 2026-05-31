package com.keepfit.feature.steps.data

interface StepsRepository {
    val requiredPermissions: Set<String>

    suspend fun loadSnapshot(): StepsSnapshot
}
