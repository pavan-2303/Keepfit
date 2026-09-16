package com.keepfit.app.profile

import com.keepfit.core.model.BodyProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfiles(): Flow<List<BodyProfile>>

    fun observeActiveProfile(): Flow<BodyProfile?>

    suspend fun ensureActiveProfile()

    suspend fun saveProfile(input: ProfileInput)

    suspend fun addProfile(input: ProfileInput)

    suspend fun editProfile(profileId: String, input: ProfileInput)

    suspend fun selectProfile(profileId: String)

    suspend fun archiveProfile(profileId: String): Result<Unit>
}
