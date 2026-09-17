package com.keepfit.core.database.assistant

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keepfit.core.database.KeepfitDatabase
import com.keepfit.core.database.profile.BodyProfileEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssistantConversationDaoTest {
    private lateinit var database: KeepfitDatabase
    private lateinit var dao: AssistantConversationDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KeepfitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.assistantConversationDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun conversationsAndMessagesAreFilteredByOwningProfile() = runBlocking {
        insertProfile("profile-a")
        insertProfile("profile-b")
        dao.insertConversation(conversation("conversation-a", "profile-a"))
        dao.insertConversation(conversation("conversation-b", "profile-b"))
        dao.insertMessages(
            listOf(
                message("message-a", "conversation-a", "A only"),
                message("message-b", "conversation-b", "B only"),
            ),
        )

        assertEquals(listOf("conversation-a"), dao.observeConversations("profile-a").first().map { it.id })
        assertEquals(listOf("A only"), dao.observeMessages("profile-a", "conversation-a").first().map { it.content })
        assertEquals(emptyList<AssistantMessageEntity>(), dao.observeMessages("profile-b", "conversation-a").first())
        assertNull(dao.findOwnedConversation("profile-b", "conversation-a"))
    }

    @Test
    fun profileAndConversationDeletionCascadeOnlyThroughOwnedHistory() = runBlocking {
        insertProfile("profile-a")
        insertProfile("profile-b")
        dao.insertConversation(conversation("conversation-a", "profile-a"))
        dao.insertConversation(conversation("conversation-b", "profile-b"))
        dao.insertMessages(
            listOf(
                message("message-a", "conversation-a", "A only"),
                message("message-b", "conversation-b", "B only"),
            ),
        )

        assertEquals(1, dao.deleteConversation("profile-a", "conversation-a"))
        assertEquals(emptyList<AssistantMessageEntity>(), dao.observeMessages("profile-a", "conversation-a").first())
        assertEquals(1, dao.observeMessages("profile-b", "conversation-b").first().size)

        database.bodyProfileDao().archive("profile-b", 200L)
        database.openHelper.writableDatabase.execSQL("DELETE FROM body_profiles WHERE id = 'profile-b'")
        assertEquals(emptyList<AssistantConversationEntity>(), dao.observeConversations("profile-b").first())
    }

    private suspend fun insertProfile(id: String) {
        database.bodyProfileDao().upsert(
            BodyProfileEntity(
                id = id,
                displayName = id,
                heightCm = null,
                birthDate = null,
                createdAt = 1L,
                updatedAt = 1L,
            ),
        )
    }

    private fun conversation(id: String, profileId: String) = AssistantConversationEntity(
        id = id,
        bodyProfileId = profileId,
        coachId = "mira",
        title = "Conversation $id",
        memorySummary = null,
        memoryClearedAt = null,
        createdAt = 10L,
        updatedAt = 10L,
    )

    private fun message(id: String, conversationId: String, content: String) = AssistantMessageEntity(
        id = id,
        assistantConversationId = conversationId,
        role = "USER",
        content = content,
        includedLocalContext = false,
        createdAt = 11L,
    )
}
