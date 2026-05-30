package com.keepfit.app.profile

import com.keepfit.core.database.profile.BodyProfileDao
import com.keepfit.core.database.profile.BodyProfileEntity
import com.keepfit.core.model.BodyProfile
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomProfileRepository internal constructor(
    private val dao: BodyProfileDao,
    private val idFactory: () -> String,
    private val clock: () -> Long,
) : ProfileRepository {
    constructor(dao: BodyProfileDao) : this(
        dao = dao,
        idFactory = { UUID.randomUUID().toString() },
        clock = System::currentTimeMillis,
    )

    override fun observeLocalProfile(): Flow<BodyProfile?> =
        dao.observeLocalProfile().map { entity -> entity?.toDomainModel() }

    override suspend fun saveProfile(input: ProfileInput) {
        val now = clock()
        dao.upsert(
            BodyProfileEntity(
                id = idFactory(),
                displayName = input.displayName,
                heightCm = input.heightCm,
                birthDate = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }
}

private fun BodyProfileEntity.toDomainModel() = BodyProfile(
    id = id,
    displayName = displayName,
    heightCm = heightCm,
    birthDate = birthDate,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
