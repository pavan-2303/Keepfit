package com.keepfit.feature.assistant.access

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keepfit.feature.assistant.coaching.AssistantDraftSnapshot
import com.keepfit.feature.assistant.coaching.CoachingIntent
import com.keepfit.feature.assistant.coaching.CoachingProposal
import com.keepfit.feature.assistant.coaching.CoachingProposalCodec
import com.keepfit.feature.assistant.coaching.CoachingProposalOperation
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedAssistantDraftStoreTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val store = EncryptedAssistantDraftStore(context, CoachingProposalCodec())

    @After
    fun cleanUp() {
        store.clear()
    }

    @Test
    fun draftAndProposalSurviveRecreationWithoutPlaintextAtRest() {
        val privateDraft = "Make Tuesday shorter because I only have 20 minutes"
        val privateObservation = "Two sessions were completed this week."
        val proposal = CoachingProposal(
            id = "proposal-1",
            intent = CoachingIntent.WORKOUT_SHORTENING,
            title = "Shorter Tuesday",
            observed = privateObservation,
            current = "The scheduled session is longer than the available time.",
            proposed = "Use the minimum version.",
            reason = "It preserves the habit.",
            operation = CoachingProposalOperation.None,
            originalRequest = privateDraft,
            generatedAtUtcEpochMillis = 1L,
            model = "test-model",
        )

        store.write(
            AssistantDraftSnapshot(
                draftText = privateDraft,
                selectedIntent = CoachingIntent.WORKOUT_SHORTENING,
                proposal = proposal,
            ),
        )

        val recreatedStore = EncryptedAssistantDraftStore(context, CoachingProposalCodec())
        assertEquals(privateDraft, recreatedStore.read().draftText)
        assertEquals(CoachingIntent.WORKOUT_SHORTENING, recreatedStore.read().selectedIntent)
        assertEquals(privateObservation, recreatedStore.read().proposal?.observed)
        val preferencesFile = File(
            context.applicationInfo.dataDir,
            "shared_prefs/${EncryptedAssistantDraftStore.PREFERENCES_NAME}.xml",
        )
        val persisted = preferencesFile.readText()
        assertFalse(persisted.contains(privateDraft))
        assertFalse(persisted.contains(privateObservation))

        recreatedStore.clear()
        assertEquals("", recreatedStore.read().draftText)
        assertNull(recreatedStore.read().proposal)
    }
}
