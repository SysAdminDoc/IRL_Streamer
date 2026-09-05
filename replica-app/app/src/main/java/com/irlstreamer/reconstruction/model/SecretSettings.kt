package com.irlstreamer.reconstruction.model

/**
 * Stored text values that hold a credential.
 *
 * Listed rather than sniffed from the id, because the ids that read like
 * secrets are mostly not: `keyframe` is an interval in seconds and
 * `volume_keys` is a hardware button action. Encrypting those would cost
 * nothing but hide two perfectly ordinary settings, and a rule that fires on
 * the wrong half of the catalog is a rule nobody can reason about.
 *
 * `SecretSettingsCoverageTest` walks the catalog and fails when a text field
 * whose label names a key, token, password or secret is missing from here, so
 * the list cannot quietly fall behind the settings screens.
 */
internal val SECRET_CHOICE_IDS = setOf(
    "dashboard_a_key",
    "dashboard_c_key",
)

/** True when the value stored under [id] must be encrypted at rest. */
internal fun isSecretChoiceId(id: String): Boolean = id in SECRET_CHOICE_IDS
