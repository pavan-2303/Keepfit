package com.keepfit.app.profile

import com.keepfit.core.database.profile.BodyProfileDao
import com.keepfit.core.database.profile.BodyProfileEntity
import com.keepfit.core.database.profile.ProfileSetupDao
import com.keepfit.core.database.transformation.BodyMeasurementEntity
import com.keepfit.core.model.BodyProfile
import com.keepfit.core.preferences.ActiveProfileStore
import com.keepfit.core.preferences.AppSettingsRepository
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class RoomProfileRepository internal constructor(
    private val dao: BodyProfileDao,
    private val setupDao: ProfileSetupDao,
    private val activeProfileStore: ActiveProfileStore,
    private val settingsRepository: AppSettingsRepository,
    private val idFactory: () -> String,
    private val clock: () -> Long,
    private val today: () -> LocalDate = LocalDate::now,
) : ProfileRepository {
    constructor(
        dao: BodyProfileDao,
        setupDao: ProfileSetupDao,
        activeProfileStore: ActiveProfileStore,
        settingsRepository: AppSettingsRepository,
    ) : this(
        dao = dao,
        setupDao = setupDao,
        activeProfileStore = activeProfileStore,
        settingsRepository = settingsRepository,
        idFactory = { UUID.randomUUID().toString() },
        clock = System::currentTimeMillis,
        today = LocalDate::now,
    )

    override fun observeProfiles(): Flow<List<BodyProfile>> =
        dao.observeProfiles().map { profiles -> profiles.map(BodyProfileEntity::toDomainModel) }

    override fun observeActiveProfile(): Flow<BodyProfile?> =
        activeProfileStore.observeActiveProfileId().flatMapLatest { profileId ->
            if (profileId == null) dao.observeLocalProfile() else dao.observeProfile(profileId)
        }.map { it?.toDomainModel() }

    override suspend fun ensureActiveProfile() {
        val profiles = dao.findProfiles()
        if (profiles.isEmpty()) {
            activeProfileStore.clearSelection()
            return
        }
        val selectedId = activeProfileStore.observeActiveProfileId().firstOrNull()
        val selected = selectedId?.let { id -> profiles.firstOrNull { it.id == id } }
        val active = selected ?: profiles.first()
        settingsRepository.initializeProfileSettings(active.id, inheritLegacy = selectedId == null)
        if (selected == null) activeProfileStore.selectProfile(active.id)
        settingsRepository.refreshReminders()
    }

    override suspend fun saveProfile(input: ProfileInput) {
        addProfile(input)
    }

    override suspend fun addProfile(input: ProfileInput) {
        val isFirstProfile = dao.findProfiles().isEmpty()
        val now = clock()
        val profileId = idFactory()
        val profile = BodyProfileEntity(
            id = profileId,
            displayName = input.displayName,
            heightCm = input.heightCm,
            birthDate = input.birthDate,
            dailyCalorieGoal = null,
            dailyProteinGoalGrams = null,
            dailyCarbohydrateGoalGrams = null,
            dailyFatGoalGrams = null,
            createdAt = now,
            updatedAt = now,
        )
        val startingMeasurement = input.startingWeightKg?.let { weightKg ->
            BodyMeasurementEntity(
                id = idFactory(),
                bodyProfileId = profileId,
                measurementDate = today(),
                weightKg = weightKg,
                waistCm = null,
                chestCm = null,
                hipsCm = null,
                leftArmCm = null,
                rightArmCm = null,
                leftThighCm = null,
                rightThighCm = null,
                notes = "Starting weight from onboarding",
                createdAt = now,
            )
        }
        setupDao.saveProfileWithStartingMeasurement(
            profile = profile,
            measurement = startingMeasurement,
        )
        settingsRepository.initializeProfileSettings(profileId, inheritLegacy = isFirstProfile)
        activeProfileStore.selectProfile(profileId)
        settingsRepository.refreshReminders()
    }

    override suspend fun editProfile(profileId: String, input: ProfileInput) {
        val existing = requireNotNull(dao.findProfile(profileId)) { "Profile not found." }
        dao.upsert(
            existing.copy(
                displayName = input.displayName,
                heightCm = input.heightCm,
                birthDate = input.birthDate,
                updatedAt = clock(),
            ),
        )
    }

    override suspend fun selectProfile(profileId: String) {
        requireNotNull(dao.findProfile(profileId)) { "Profile not found." }
        settingsRepository.initializeProfileSettings(profileId, inheritLegacy = false)
        activeProfileStore.selectProfile(profileId)
        settingsRepository.refreshReminders()
    }

    override suspend fun archiveProfile(profileId: String): Result<Unit> = runCatching {
        val profiles = dao.findProfiles()
        check(profiles.size > 1) { "Keep at least one profile." }
        requireNotNull(profiles.firstOrNull { it.id == profileId }) { "Profile not found." }
        if (activeProfileStore.observeActiveProfileId().firstOrNull() == profileId) {
            selectProfile(profiles.first { it.id != profileId }.id)
        }
        check(dao.archive(profileId, clock()) == 1) { "Profile could not be archived." }
    }
}

private fun BodyProfileEntity.toDomainModel() = BodyProfile(
    id = id,
    displayName = displayName,
    heightCm = heightCm,
    birthDate = birthDate,
    dailyCalorieGoal = dailyCalorieGoal,
    dailyProteinGoalGrams = dailyProteinGoalGrams,
    dailyCarbohydrateGoalGrams = dailyCarbohydrateGoalGrams,
    dailyFatGoalGrams = dailyFatGoalGrams,
    createdAt = createdAt,
    updatedAt = updatedAt,
    archivedAt = archivedAt,
)
