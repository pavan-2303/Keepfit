package com.keepfit.feature.review

import com.keepfit.core.preferences.ActiveProfileStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class TestActiveProfileStore(initialProfileId: String = "profile") : ActiveProfileStore {
    private val active = MutableStateFlow<String?>(initialProfileId)
    override fun observeActiveProfileId(): Flow<String?> = active
    override suspend fun selectProfile(profileId: String) { active.value = profileId }
    override suspend fun clearSelection() { active.value = null }
}
