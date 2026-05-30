package com.keepfit.app.profile

import com.keepfit.core.model.BodyProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeLocalProfile(): Flow<BodyProfile?>

    suspend fun saveProfile(input: ProfileInput)
}

