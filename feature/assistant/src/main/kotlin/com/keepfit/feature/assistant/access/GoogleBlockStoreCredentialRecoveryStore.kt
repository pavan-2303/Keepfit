package com.keepfit.feature.assistant.access

import android.content.Context
import com.google.android.gms.auth.blockstore.Blockstore
import com.google.android.gms.auth.blockstore.DeleteBytesRequest
import com.google.android.gms.auth.blockstore.RetrieveBytesRequest
import com.google.android.gms.auth.blockstore.StoreBytesData
import com.google.android.gms.tasks.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class GoogleBlockStoreCredentialRecoveryStore @Inject constructor(
    @ApplicationContext context: Context,
) : AssistantCredentialRecoveryStore {
    private val client = Blockstore.getClient(context)

    override suspend fun isAvailable(): Boolean =
        runCatching {
            client.isEndToEndEncryptionAvailable().awaitResult()
            true
        }.getOrDefault(false)

    override suspend fun store(token: ByteArray) {
        require(token.size in 1..MAX_ENTRY_BYTES) { "The OpenRouter credential cannot be recovered safely." }
        val isCloudEncryptionAvailable = client.isEndToEndEncryptionAvailable().awaitResult()
        val request = StoreBytesData.Builder()
            .setBytes(token)
            .setKey(RECOVERY_KEY)
            .apply {
                if (isCloudEncryptionAvailable) setShouldBackupToCloud(true)
            }
            .build()
        client.storeBytes(request).awaitResult()
    }

    override suspend fun retrieve(): ByteArray? {
        val request = RetrieveBytesRequest.Builder()
            .setKeys(listOf(RECOVERY_KEY))
            .build()
        return client.retrieveBytes(request)
            .awaitResult()
            .blockstoreDataMap[RECOVERY_KEY]
            ?.bytes
    }

    override suspend fun delete() {
        val request = DeleteBytesRequest.Builder()
            .setKeys(listOf(RECOVERY_KEY))
            .build()
        client.deleteBytes(request).awaitResult()
    }

    private companion object {
        const val RECOVERY_KEY = "com.keepfit.openrouter.token.v1"
        const val MAX_ENTRY_BYTES = 4 * 1024
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        if (continuation.isActive) continuation.resume(result)
    }
    addOnFailureListener { error ->
        if (continuation.isActive) continuation.resumeWithException(error)
    }
    addOnCanceledListener { continuation.cancel() }
}
