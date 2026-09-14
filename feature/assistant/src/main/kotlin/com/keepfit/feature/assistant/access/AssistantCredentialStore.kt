package com.keepfit.feature.assistant.access

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

interface AssistantCredentialStore {
    fun readToken(): String?
    fun writeToken(token: String)
    fun readPendingAuthorization(): OpenRouterPendingAuthorization?
    fun writePendingAuthorization(pending: OpenRouterPendingAuthorization)
    fun clearPendingAuthorization()
    fun clearCredentials()
    fun isDisclosureAccepted(): Boolean
    fun setDisclosureAccepted(accepted: Boolean)
}

@Singleton
class AndroidKeystoreAssistantCredentialStore @Inject constructor(
    @ApplicationContext context: Context,
) : AssistantCredentialStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    override fun readToken(): String? = decryptPreference(KEY_TOKEN)

    override fun writeToken(token: String) {
        require(token.isNotBlank())
        encryptPreference(KEY_TOKEN, token)
    }

    override fun readPendingAuthorization(): OpenRouterPendingAuthorization? =
        decryptPreference(KEY_PENDING)?.let { json ->
            runCatching { gson.fromJson(json, OpenRouterPendingAuthorization::class.java) }.getOrNull()
        }

    override fun writePendingAuthorization(pending: OpenRouterPendingAuthorization) {
        encryptPreference(KEY_PENDING, gson.toJson(pending))
    }

    override fun clearPendingAuthorization() {
        preferences.edit().remove(KEY_PENDING).commit()
    }

    override fun clearCredentials() {
        preferences.edit().remove(KEY_TOKEN).remove(KEY_PENDING).commit()
    }

    override fun isDisclosureAccepted(): Boolean = preferences.getBoolean(KEY_DISCLOSURE_ACCEPTED, false)

    override fun setDisclosureAccepted(accepted: Boolean) {
        preferences.edit().putBoolean(KEY_DISCLOSURE_ACCEPTED, accepted).commit()
    }

    private fun encryptPreference(key: String, plaintext: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val payload = cipher.iv + cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        check(preferences.edit().putString(key, Base64.getEncoder().encodeToString(payload)).commit()) {
            "Secure assistant credential could not be persisted."
        }
    }

    private fun decryptPreference(key: String): String? {
        val encoded = preferences.getString(key, null) ?: return null
        return runCatching {
            val payload = Base64.getDecoder().decode(encoded)
            require(payload.size > IV_SIZE)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(GCM_TAG_BITS, payload.copyOfRange(0, IV_SIZE)),
            )
            String(cipher.doFinal(payload.copyOfRange(IV_SIZE, payload.size)), Charsets.UTF_8)
        }.getOrElse {
            preferences.edit().remove(key).commit()
            null
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "keepfit_assistant_secure"
        const val KEY_TOKEN = "credential"
        const val KEY_PENDING = "pending_authorization"
        const val KEY_DISCLOSURE_ACCEPTED = "disclosure_accepted"
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "keepfit.openrouter.credential.v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val GCM_TAG_BITS = 128
    }
}
