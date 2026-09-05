package com.irlstreamer.reconstruction.data

/**
 * Reversible protection for a value that must not sit on disk in the clear.
 *
 * The settings store holds destination URLs, and an RTMP or SRT URL carries the
 * stream key that lets anyone broadcast to the user's channel. It also holds
 * dashboard API keys. DataStore writes its file with app-private permissions,
 * which protects it from other apps but not from a backup, a rooted phone or
 * anyone with the handset unlocked.
 *
 * Both operations return null rather than throwing: a device whose keystore is
 * unusable must not take the app down, and the caller decides what to do about
 * a secret it cannot protect. Silently writing the plaintext is never that
 * decision.
 */
internal interface SecretCipher {
    fun encrypt(plaintext: String): String?

    fun decrypt(ciphertext: String): String?
}

/**
 * Marks a stored value as protected.
 *
 * Versioned, because a stored secret outlives the code that wrote it: changing
 * the scheme later means recognising what the old one produced rather than
 * handing the wrong bytes to a new cipher.
 */
internal const val SECRET_PREFIX = "enc1:"

/** True when [value] was written by [protect] rather than left in the clear. */
internal fun isProtected(value: String): Boolean = value.startsWith(SECRET_PREFIX)

/**
 * The form of [value] that belongs on disk, or null when it cannot be protected.
 *
 * An empty value is not a secret and stays as it is: encrypting it would turn
 * "no destination saved" into an opaque blob that still has to be decrypted to
 * find out it means nothing.
 */
internal fun protect(value: String, cipher: SecretCipher): String? {
    if (value.isEmpty() || isProtected(value)) return value
    return cipher.encrypt(value)?.let { SECRET_PREFIX + it }
}

/**
 * The plaintext behind [value].
 *
 * A value with no prefix was written before this existed, or by a build that
 * could not protect it, so it is returned as it stands: refusing it would lose
 * the user's saved destination on upgrade. A value that claims to be protected
 * and will not decrypt is another matter - that is a key that changed under a
 * restore or a reinstall, and returning the ciphertext would put a blob into
 * the URL field and into the connect attempt.
 */
internal fun reveal(value: String, cipher: SecretCipher): String {
    if (!isProtected(value)) return value
    return cipher.decrypt(value.removePrefix(SECRET_PREFIX)) ?: ""
}
