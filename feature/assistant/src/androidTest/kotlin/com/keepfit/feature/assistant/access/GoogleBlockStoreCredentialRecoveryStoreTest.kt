package com.keepfit.feature.assistant.access

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoogleBlockStoreCredentialRecoveryStoreTest {

    @Test
    fun supportedServiceStoresRetrievesAndDeletesOnlyTheRecoveryEntry() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = GoogleBlockStoreCredentialRecoveryStore(context)
        assumeTrue("Google Block Store is unavailable on this device", store.isAvailable())
        val token = "isolated-instrumentation-token".toByteArray()

        try {
            store.store(token)
            assertArrayEquals(token, store.retrieve())
        } finally {
            store.delete()
        }
    }
}
