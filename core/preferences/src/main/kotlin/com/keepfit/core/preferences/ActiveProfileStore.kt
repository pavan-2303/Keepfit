package com.keepfit.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ActiveProfileStore {
    fun observeActiveProfileId(): Flow<String?>
    suspend fun selectProfile(profileId: String)
    suspend fun clearSelection()
}

class DataStoreActiveProfileStore(
    private val context: Context,
) : ActiveProfileStore {
    override fun observeActiveProfileId(): Flow<String?> =
        context.keepfitDataStore.data.map { it[ACTIVE_PROFILE_ID] }

    override suspend fun selectProfile(profileId: String) {
        require(profileId.isNotBlank()) { "Profile id cannot be blank." }
        context.keepfitDataStore.edit { it[ACTIVE_PROFILE_ID] = profileId }
    }

    override suspend fun clearSelection() {
        context.keepfitDataStore.edit { it.remove(ACTIVE_PROFILE_ID) }
    }

    private companion object {
        val ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
    }
}
