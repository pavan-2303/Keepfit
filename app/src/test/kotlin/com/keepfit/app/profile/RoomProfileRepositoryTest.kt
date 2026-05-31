package com.keepfit.app.profile

import com.keepfit.core.database.profile.BodyProfileDao
import com.keepfit.core.database.profile.BodyProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomProfileRepositoryTest {
    @Test
    fun saveProfilePersistsAndExposesDomainModel() = runBlocking {
        val dao = FakeBodyProfileDao()
        val repository = RoomProfileRepository(
            dao = dao,
            idFactory = { "profile-id" },
            clock = { 123L },
        )

        repository.saveProfile(ProfileInput(displayName = "Pavan", heightCm = 178.0))

        assertEquals(
            "Pavan",
            repository.observeLocalProfile().first()?.displayName,
        )
        assertEquals(123L, dao.savedProfile?.createdAt)
        assertEquals(123L, dao.savedProfile?.updatedAt)
    }
}

private class FakeBodyProfileDao : BodyProfileDao {
    private val profile = MutableStateFlow<BodyProfileEntity?>(null)

    var savedProfile: BodyProfileEntity? = null

    override suspend fun upsert(profile: BodyProfileEntity) {
        savedProfile = profile
        this.profile.value = profile
    }

    override fun observeLocalProfile(): Flow<BodyProfileEntity?> = profile

    override suspend fun findLocalProfile(): BodyProfileEntity? = profile.value
}
