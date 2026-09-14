package com.keepfit.feature.assistant.access

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidKeystoreAssistantCredentialStoreTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val store = AndroidKeystoreAssistantCredentialStore(context)

    @After
    fun cleanUp() {
        store.clearCredentials()
        store.setDisclosureAccepted(false)
    }

    @Test
    fun tokenAndPendingTransactionRoundTripEncryptedAndEraseOnDisconnect() {
        val token = "sk-or-v1-user-secret-value"
        val pending = OpenRouterPendingAuthorization(
            callbackUrl = "http://127.0.0.1:49152/oauth/callback/state",
            state = "state",
            codeVerifier = "verifier-value-that-must-not-be-plain-text",
            codeChallenge = "challenge",
            createdAtUtcEpochMillis = 10L,
        )

        store.writeToken(token)
        store.writePendingAuthorization(pending)

        assertEquals(token, store.readToken())
        assertEquals(pending, store.readPendingAuthorization())
        val preferencesFile = File(context.applicationInfo.dataDir, "shared_prefs/keepfit_assistant_secure.xml")
        val persisted = preferencesFile.readText()
        assertFalse(persisted.contains(token))
        assertFalse(persisted.contains(pending.codeVerifier))

        store.clearCredentials()
        assertNull(store.readToken())
        assertNull(store.readPendingAuthorization())
    }

    @Test
    fun disclosureDecisionIsSeparateFromCredentialErasure() {
        store.setDisclosureAccepted(true)
        store.writeToken("temporary-secret")

        store.clearCredentials()

        assertTrue(store.isDisclosureAccepted())
        assertNull(store.readToken())
    }
}
