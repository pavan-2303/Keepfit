package com.keepfit.feature.transformation.data

import com.keepfit.core.preferences.ActiveProfileStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class TestActiveProfileStore(initialProfileId: String) : ActiveProfileStore {
    private val activeProfileId = MutableStateFlow<String?>(initialProfileId)

    override fun observeActiveProfileId(): Flow<String?> = activeProfileId

    override suspend fun selectProfile(profileId: String) {
        activeProfileId.value = profileId
    }

    override suspend fun clearSelection() {
        activeProfileId.value = null
    }
}
