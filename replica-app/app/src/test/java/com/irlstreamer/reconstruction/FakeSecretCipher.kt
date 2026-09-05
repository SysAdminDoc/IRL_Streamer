package com.irlstreamer.reconstruction

import com.irlstreamer.reconstruction.data.SecretCipher

/**
 * A cipher for the JVM tests, where there is no AndroidKeyStore.
 *
 * Reversing the string and Base64-ing it is not encryption and is not meant to
 * be: what the tests need is a transform that makes the plaintext absent from
 * the stored bytes, so an assertion that the store holds no cleartext key is
 * checking the repository's plumbing rather than the strength of a cipher. The
 * real one is `KeystoreSecretCipher`, and it only runs on a device.
 *
 * [refuse] makes every call fail, which is how a device with an unusable
 * keystore behaves.
 */
internal class FakeSecretCipher(private val refuse: Boolean = false) : SecretCipher {
    var encryptCalls = 0
        private set

    override fun encrypt(plaintext: String): String? {
        encryptCalls++
        if (refuse) return null
        return java.util.Base64.getEncoder().encodeToString(plaintext.reversed().toByteArray())
    }

    override fun decrypt(ciphertext: String): String? {
        if (refuse) return null
        return runCatching {
            String(java.util.Base64.getDecoder().decode(ciphertext)).reversed()
        }.getOrNull()
    }
}
