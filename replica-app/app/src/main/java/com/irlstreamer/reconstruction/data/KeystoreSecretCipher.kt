package com.irlstreamer.reconstruction.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val TAG = "SecretCipher"
private const val PROVIDER = "AndroidKeyStore"
private const val ALIAS = "irl_streamer_secrets"
private const val TRANSFORMATION = "AES/GCM/NoPadding"

/** GCM's standard nonce length. Anything else costs an extra derivation step. */
private const val IV_LENGTH = 12

/** GCM tag length in bits; 128 is the longest and the default. */
private const val TAG_LENGTH_BITS = 128

/**
 * AES-GCM with a key the AndroidKeyStore holds and this process never sees.
 *
 * The key material stays inside the keystore - on most current hardware inside
 * the TEE or a dedicated security chip - so a copy of the settings file taken
 * off the device decrypts to nothing. It is not tied to a user presence check:
 * the engine has to reconnect while the screen is off, and a key that needs an
 * unlock would end a broadcast at the display timeout.
 *
 * `setRandomizedEncryptionRequired` is left on, so the platform generates the
 * nonce and refuses a caller that tries to supply one. The nonce is not secret
 * and travels with the ciphertext.
 */
internal class KeystoreSecretCipher : SecretCipher {

    override fun encrypt(plaintext: String): String? = runCatching {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        require(iv.size == IV_LENGTH) { "unexpected nonce length ${iv.size}" }
        Base64.getEncoder().encodeToString(iv + encrypted)
    }.getOrElse { failure ->
        // Never the value itself, and never the exception's message, which on
        // some keystore failures quotes the input.
        Log.e(TAG, "could not protect a secret: ${failure.javaClass.simpleName}")
        null
    }

    override fun decrypt(ciphertext: String): String? = runCatching {
        val bytes = Base64.getDecoder().decode(ciphertext)
        require(bytes.size > IV_LENGTH) { "ciphertext too short" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(TAG_LENGTH_BITS, bytes, 0, IV_LENGTH),
        )
        String(cipher.doFinal(bytes, IV_LENGTH, bytes.size - IV_LENGTH), Charsets.UTF_8)
    }.getOrElse { failure ->
        Log.e(TAG, "could not read a stored secret: ${failure.javaClass.simpleName}")
        null
    }

    /**
     * The app's key, generated on first use.
     *
     * Generating rather than failing when the alias is absent covers both a
     * fresh install and a restore that brought the settings file back without
     * the key: the old ciphertext is unreadable either way, and a working key
     * means the next save is protected instead of the app never protecting
     * anything again.
     */
    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }
}
