package com.example.jarvis.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.jarvis.utils.Constants
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class PreferencesManager(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            Constants.PREFS_NAME,
            Context.MODE_PRIVATE
        )

    companion object {
        private const val KEYSTORE_ALIAS = "jarvis_key"

        private const val KEY_VOICE_WAKE_ENABLED =
            "voice_wake_enabled"
    }

    /*
     * Android Keystore AES key.
     */
    private fun key(): SecretKey {

        val keyStore =
            java.security.KeyStore
                .getInstance("AndroidKeyStore")
                .apply {
                    load(null)
                }

        (
            keyStore.getKey(
                KEYSTORE_ALIAS,
                null
            ) as? SecretKey
        )?.let {
            return it
        }

        val keyGenerator =
            KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )

        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(
                    KeyProperties.BLOCK_MODE_GCM
                )
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .build()
        )

        return keyGenerator.generateKey()
    }

    /*
     * Save Gemini API key securely.
     */
    fun saveApiKey(value: String) {

        if (value.isBlank()) {

            preferences
                .edit()
                .remove(Constants.KEY_API)
                .apply()

            return
        }

        try {

            val cipher =
                Cipher.getInstance(
                    "AES/GCM/NoPadding"
                )

            cipher.init(
                Cipher.ENCRYPT_MODE,
                key()
            )

            val encrypted =
                cipher.doFinal(
                    value.trim().toByteArray(
                        Charsets.UTF_8
                    )
                )

            /*
             * Store:
             * IV + encrypted data
             */
            val combined =
                cipher.iv + encrypted

            preferences
                .edit()
                .putString(
                    Constants.KEY_API,
                    Base64.encodeToString(
                        combined,
                        Base64.NO_WRAP
                    )
                )
                .apply()

        } catch (_: Exception) {
            // Do not store the API key in plaintext.
        }
    }

    /*
     * Read and decrypt Gemini API key.
     */
    fun getApiKey(): String {

        return try {

            val encoded =
                preferences.getString(
                    Constants.KEY_API,
                    null
                ) ?: return ""

            val data =
                Base64.decode(
                    encoded,
                    Base64.NO_WRAP
                )

            if (data.size <= 12) {
                return ""
            }

            val cipher =
                Cipher.getInstance(
                    "AES/GCM/NoPadding"
                )

            val iv =
                data.copyOfRange(
                    0,
                    12
                )

            val encrypted =
                data.copyOfRange(
                    12,
                    data.size
                )

            cipher.init(
                Cipher.DECRYPT_MODE,
                key(),
                GCMParameterSpec(
                    128,
                    iv
                )
            )

            String(
                cipher.doFinal(encrypted),
                Charsets.UTF_8
            )

        } catch (_: Exception) {
            ""
        }
    }

    /*
     * Voice Wake Mode
     *
     * true  = background wake mode enabled
     * false = background wake mode disabled
     */
    fun setVoiceWakeEnabled(
        enabled: Boolean
    ) {

        preferences
            .edit()
            .putBoolean(
                KEY_VOICE_WAKE_ENABLED,
                enabled
            )
            .apply()
    }

    fun isVoiceWakeEnabled(): Boolean {

        return preferences.getBoolean(
            KEY_VOICE_WAKE_ENABLED,
            false
        )
    }
}