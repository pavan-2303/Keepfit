package com.keepfit.app.profile

import com.keepfit.core.database.profile.BodyProfileDao
import com.keepfit.core.database.profile.BodyProfileEntity
import com.keepfit.core.database.profile.ProfileSetupDao
import com.keepfit.core.database.transformation.BodyMeasurementEntity
import com.keepfit.core.preferences.ActiveProfileStore
import com.keepfit.core.preferences.AppSettings
import com.keepfit.core.preferences.AppSettingsRepository
import com.keepfit.core.preferences.MeasurementUnit
import com.keepfit.core.preferences.WeightUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomProfileRepositoryTest {
    @Test
    fun saveProfilePersistsAndExposesDomainModel() = runBlocking {
        val dao = FakeBodyProfileDao()
        val repository = RoomProfileRepository(
            dao = dao,
            setupDao = FakeProfileSetupDao(dao),
            activeProfileStore = FakeActiveProfileStore(),
            settingsRepository = FakeSettingsRepository,
            idFactory = { "profile-id" },
            clock = { 123L },
        )

        repository.saveProfile(ProfileInput(displayName = "Pavan", heightCm = 178.0))

        assertEquals(
            "Pavan",
            repository.observeActiveProfile().first()?.displayName,
        )
        assertEquals(123L, dao.savedProfile?.createdAt)
        assertEquals(123L, dao.savedProfile?.updatedAt)
    }

    @Test
    fun profilesCanSwitchEditAndArchiveWithoutRemovingTheLastProfile() = runBlocking {
        val dao = FakeBodyProfileDao()
        val activeProfiles = FakeActiveProfileStore()
        val ids = ArrayDeque(listOf("one", "two"))
        val repository = RoomProfileRepository(
            dao = dao,
            setupDao = FakeProfileSetupDao(dao),
            activeProfileStore = activeProfiles,
            settingsRepository = FakeSettingsRepository,
            idFactory = ids::removeFirst,
            clock = { 123L },
        )

        repository.saveProfile(ProfileInput("One", 170.0))
        repository.addProfile(ProfileInput("Two", 180.0))
        repository.selectProfile("one")
        repository.editProfile("one", ProfileInput("Updated", 171.0, LocalDate.of(1990, 1, 2)))

        assertEquals("Updated", repository.observeActiveProfile().first()?.displayName)
        assertEquals(LocalDate.of(1990, 1, 2), repository.observeActiveProfile().first()?.birthDate)
        assertTrue(repository.archiveProfile("one").isSuccess)
        assertEquals("two", activeProfiles.observeActiveProfileId().first())
        assertTrue(repository.archiveProfile("two").isFailure)
    }

    @Test
    fun firstProfileStoresBirthDateAndExactlyOneStartingMeasurement() = runBlocking {
        val dao = FakeBodyProfileDao()
        val setupDao = FakeProfileSetupDao(dao)
        val ids = ArrayDeque(listOf("profile-id", "measurement-id"))
        val repository = RoomProfileRepository(
            dao = dao,
            setupDao = setupDao,
            activeProfileStore = FakeActiveProfileStore(),
            settingsRepository = FakeSettingsRepository,
            idFactory = ids::removeFirst,
            clock = { 123L },
            today = { LocalDate.of(2026, 9, 16) },
        )

        repository.saveProfile(
            ProfileInput(
                displayName = "Pavan",
                heightCm = 178.0,
                birthDate = LocalDate.of(1992, 6, 15),
                startingWeightKg = 79.2,
            ),
        )

        assertEquals(LocalDate.of(1992, 6, 15), dao.savedProfile?.birthDate)
        assertEquals(
            listOf(
                BodyMeasurementEntity(
                    id = "measurement-id",
                    bodyProfileId = "profile-id",
                    measurementDate = LocalDate.of(2026, 9, 16),
                    weightKg = 79.2,
                    waistCm = null,
                    chestCm = null,
                    hipsCm = null,
                    leftArmCm = null,
                    rightArmCm = null,
                    leftThighCm = null,
                    rightThighCm = null,
                    notes = "Starting weight from onboarding",
                    createdAt = 123L,
                ),
            ),
            setupDao.measurements,
        )
    }

    @Test
    fun omittedStartingWeightDoesNotCreateMeasurement() = runBlocking {
        val dao = FakeBodyProfileDao()
        val setupDao = FakeProfileSetupDao(dao)
        val repository = RoomProfileRepository(
            dao = dao,
            setupDao = setupDao,
            activeProfileStore = FakeActiveProfileStore(),
            settingsRepository = FakeSettingsRepository,
            idFactory = { "profile-id" },
            clock = { 123L },
            today = { LocalDate.of(2026, 9, 16) },
        )

        repository.saveProfile(ProfileInput("Pavan", 178.0))

        assertTrue(setupDao.measurements.isEmpty())
    }
}

private class FakeProfileSetupDao(
    private val profileDao: FakeBodyProfileDao,
) : ProfileSetupDao {
    val measurements = mutableListOf<BodyMeasurementEntity>()

    override suspend fun upsertProfile(profile: BodyProfileEntity) = profileDao.upsert(profile)

    override suspend fun upsertMeasurement(measurement: BodyMeasurementEntity) {
        measurements += measurement
    }
}

private class FakeBodyProfileDao : BodyProfileDao {
    private val profiles = MutableStateFlow<List<BodyProfileEntity>>(emptyList())

    var savedProfile: BodyProfileEntity? = null

    override suspend fun upsert(profile: BodyProfileEntity) {
        savedProfile = profile
        profiles.value = profiles.value.filterNot { it.id == profile.id } + profile
    }

    override fun observeProfiles(): Flow<List<BodyProfileEntity>> = profiles

    override fun observeProfile(profileId: String): Flow<BodyProfileEntity?> =
        MutableStateFlow(profiles.value.firstOrNull { it.id == profileId && it.archivedAt == null })

    override fun observeLocalProfile(): Flow<BodyProfileEntity?> =
        MutableStateFlow(profiles.value.firstOrNull { it.archivedAt == null })

    override suspend fun findLocalProfile(): BodyProfileEntity? = profiles.value.firstOrNull { it.archivedAt == null }

    override suspend fun findProfile(profileId: String): BodyProfileEntity? =
        profiles.value.firstOrNull { it.id == profileId && it.archivedAt == null }

    override suspend fun findProfiles(): List<BodyProfileEntity> = profiles.value.filter { it.archivedAt == null }

    override suspend fun archive(profileId: String, archivedAt: Long): Int {
        val existing = profiles.value.firstOrNull { it.id == profileId && it.archivedAt == null } ?: return 0
        profiles.value = profiles.value.filterNot { it.id == profileId } + existing.copy(archivedAt = archivedAt)
        return 1
    }

    override suspend fun updateNutritionGoals(
        profileId: String,
        dailyCalorieGoal: Double?,
        dailyProteinGoalGrams: Double?,
        dailyCarbohydrateGoalGrams: Double?,
        dailyFatGoalGrams: Double?,
        updatedAt: Long,
    ) = Unit
}

private class FakeActiveProfileStore : ActiveProfileStore {
    private val activeProfileId = MutableStateFlow<String?>(null)

    override fun observeActiveProfileId(): Flow<String?> = activeProfileId

    override suspend fun selectProfile(profileId: String) {
        activeProfileId.value = profileId
    }

    override suspend fun clearSelection() {
        activeProfileId.value = null
    }
}

private object FakeSettingsRepository : AppSettingsRepository {
    override fun observeSettings(): Flow<AppSettings> = MutableStateFlow(AppSettings())
    override suspend fun updateUnits(weightUnit: WeightUnit, measurementUnit: MeasurementUnit) = Unit
    override suspend fun updateAssistantSettings(enabled: Boolean) = Unit
    override suspend fun updateRestTimerSeconds(seconds: Int) = Unit
    override suspend fun updateWorkoutReminder(enabled: Boolean, hour: Int, minute: Int) = Unit
    override suspend fun updateTransformationReminder(
        enabled: Boolean,
        dayOfWeekOrdinal: Int,
        hour: Int,
        minute: Int,
    ) = Unit
}
