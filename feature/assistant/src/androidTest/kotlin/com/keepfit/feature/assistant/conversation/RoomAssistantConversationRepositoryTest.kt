package com.keepfit.feature.assistant.conversation

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keepfit.core.database.KeepfitDatabase
import com.keepfit.core.database.profile.BodyProfileEntity
import com.keepfit.core.preferences.ActiveProfileStore
import com.keepfit.feature.assistant.data.AssistantChatMessage
import com.keepfit.feature.assistant.data.AssistantMessageRole
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomAssistantConversationRepositoryTest {
    private lateinit var database: KeepfitDatabase
    private lateinit var activeProfiles: FakeActiveProfileStore
    private lateinit var repository: RoomAssistantConversationRepository

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            KeepfitDatabase::class.java,
        ).allowMainThreadQueries().build()
        database.bodyProfileDao().upsert(profile("profile-a"))
        database.bodyProfileDao().upsert(profile("profile-b"))
        activeProfiles = FakeActiveProfileStore("profile-a")
        repository = RoomAssistantConversationRepository(
            dao = database.assistantConversationDao(),
            activeProfileStore = activeProfiles,
            clock = Clock.fixed(Instant.ofEpochMilli(1_000L), ZoneOffset.UTC),
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun completedTurnsPersistWithPersonaAndBoundedProviderHistory() = runBlocking {
        val conversation = repository.createConversation(CoachPersona.ATLAS)
        repeat(8) { index ->
            repository.appendCompletedTurn(
                conversationId = conversation.id,
                userMessage = message("user-$index", AssistantMessageRole.USER, "Question $index", index * 2L + 1),
                assistantMessage = message("coach-$index", AssistantMessageRole.ASSISTANT, "Answer $index", index * 2L + 2),
            ).getOrThrow()
        }

        val visible = repository.observeMessages(conversation.id).first()
        val providerHistory = repository.loadRequestHistory(conversation.id).getOrThrow()

        assertEquals(16, visible.size)
        assertEquals(14, providerHistory.size)
        assertTrue(providerHistory.first().content.contains("Atlas"))
        assertTrue(providerHistory[1].content.contains("Question 0"))
        assertEquals((2..7).flatMap { listOf("user-$it", "coach-$it") }, providerHistory.drop(2).map { it.id })
    }

    @Test
    fun clearingMemoryKeepsTranscriptButExcludesItFromFutureProviderHistory() = runBlocking {
        val conversation = repository.createConversation(CoachPersona.MIRA)
        repository.appendCompletedTurn(
            conversation.id,
            message("user", AssistantMessageRole.USER, "Private old context", 1L),
            message("coach", AssistantMessageRole.ASSISTANT, "Old answer", 2L),
        ).getOrThrow()

        repository.clearMemory(conversation.id).getOrThrow()

        assertEquals(2, repository.observeMessages(conversation.id).first().size)
        val providerHistory = repository.loadRequestHistory(conversation.id).getOrThrow()
        assertEquals(1, providerHistory.size)
        assertTrue(providerHistory.single().content.contains("Mira"))
    }

    @Test
    fun activeProfileCannotObserveOrMutateAnotherProfilesConversation() = runBlocking {
        val owned = repository.createConversation(CoachPersona.ROOK)
        activeProfiles.selectProfile("profile-b")

        assertTrue(repository.observeConversations().first().isEmpty())
        assertTrue(repository.observeMessages(owned.id).first().isEmpty())
        assertTrue(repository.renameConversation(owned.id, "Not mine").isFailure)
        assertTrue(repository.deleteConversation(owned.id).isFailure)
    }

    private fun profile(id: String) = BodyProfileEntity(
        id = id,
        displayName = id,
        heightCm = null,
        birthDate = null,
        createdAt = 1L,
        updatedAt = 1L,
    )

    private fun message(
        id: String,
        role: AssistantMessageRole,
        content: String,
        createdAt: Long,
    ) = AssistantChatMessage(id, role, content, createdAt)

    private class FakeActiveProfileStore(initialId: String) : ActiveProfileStore {
        private val selected = MutableStateFlow<String?>(initialId)
        override fun observeActiveProfileId(): Flow<String?> = selected
        override suspend fun selectProfile(profileId: String) {
            selected.value = profileId
        }
        override suspend fun clearSelection() {
            selected.value = null
        }
    }
}
