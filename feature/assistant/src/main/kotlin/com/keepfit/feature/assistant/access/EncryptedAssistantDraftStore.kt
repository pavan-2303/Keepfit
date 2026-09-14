package com.keepfit.feature.assistant.access

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.keepfit.feature.assistant.coaching.AssistantDraftSnapshot
import com.keepfit.feature.assistant.coaching.AssistantDraftStore
import com.keepfit.feature.assistant.coaching.CoachingIntent
import com.keepfit.feature.assistant.coaching.CoachingProposalCodec
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedAssistantDraftStore @Inject constructor(
    @ApplicationContext context: Context,
    private val proposalCodec: CoachingProposalCodec,
) : AssistantDraftStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    override fun read(): AssistantDraftSnapshot {
        val json = decrypt() ?: return AssistantDraftSnapshot()
        return runCatching {
            val value = JsonParser.parseString(json).asJsonObject
            AssistantDraftSnapshot(
                draftText = value.get("draft")?.asString.orEmpty().take(MAX_DRAFT_LENGTH),
                selectedIntent = value.get("intent")?.asString
                    ?.let { name -> CoachingIntent.entries.singleOrNull { it.name == name } }
                    ?: CoachingIntent.WEEKLY_SUMMARY,
                proposal = value.get("proposal")?.takeUnless { it.isJsonNull }
                    ?.let { proposalCodec.decode(gson.toJson(it)) },
            )
        }.getOrElse {
            clear()
            AssistantDraftSnapshot()
        }
    }

    override fun write(snapshot: AssistantDraftSnapshot) {
        val json = JsonObject().apply {
            addProperty("draft", snapshot.draftText.take(MAX_DRAFT_LENGTH))
            addProperty("intent", snapshot.selectedIntent.name)
            snapshot.proposal?.let { add("proposal", JsonParser.parseString(proposalCodec.encode(it))) }
        }
        encrypt(gson.toJson(json))
    }

    override fun clear() {
        preferences.edit().remove(KEY_PAYLOAD).commit()
    }

    private fun encrypt(plaintext: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val payload = cipher.iv + cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        check(preferences.edit().putString(KEY_PAYLOAD, Base64.getEncoder().encodeToString(payload)).commit())
    }

    private fun decrypt(): String? {
        val encoded = preferences.getString(KEY_PAYLOAD, null) ?: return null
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
            clear()
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
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            generateKey()
        }
    }

    companion object {
        const val PREFERENCES_NAME = "keepfit_assistant_drafts"
        private const val KEY_PAYLOAD = "encrypted_draft"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "keepfit.assistant.drafts.v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val GCM_TAG_BITS = 128
        private const val MAX_DRAFT_LENGTH = 500
    }
}
