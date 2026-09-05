package com.irlstreamer.reconstruction.model

/** One saved destination. [name] identifies it, so saving the same name edits it. */
data class OutgoingConnection(
    val name: String,
    val url: String,
)

/**
 * Field and record separators for the stored list.
 *
 * DataStore Preferences has no list type, so the connections are kept as one
 * string. ASCII unit and record separators are used because they cannot appear
 * in a name a user types or in a URL, which keeps the encoding unambiguous
 * without needing a JSON dependency.
 */
private const val FIELD = '\u001F'
private const val RECORD = '\u001E'

/** Encodes for storage. Entries with a blank name or URL are dropped. */
fun encodeConnections(connections: List<OutgoingConnection>): String =
    connections
        .filter { it.name.isNotBlank() && it.url.isNotBlank() }
        .joinToString(RECORD.toString()) { "${sanitise(it.name)}$FIELD${sanitise(it.url)}" }

/** Reads back what [encodeConnections] wrote, ignoring anything malformed. */
fun decodeConnections(stored: String): List<OutgoingConnection> {
    if (stored.isBlank()) return emptyList()
    return stored.split(RECORD).mapNotNull { record ->
        val parts = record.split(FIELD)
        if (parts.size != 2) return@mapNotNull null
        val name = parts[0]
        val url = parts[1]
        if (name.isBlank() || url.isBlank()) null else OutgoingConnection(name, url)
    }
        // Names identify an entry case-insensitively, so two that differ only by
        // case would make upsert edit one and delete remove both, and would give
        // two rows the same id. Stored data from before that rule could hold a
        // pair like that.
        .distinctBy { it.name.lowercase() }
}

/**
 * Adds [connection], or replaces the one that already has its name.
 *
 * Saving is how the form both creates and edits, so matching on the name is
 * what stops an edit from leaving a duplicate behind. The edited entry keeps
 * its position in the list.
 */
fun List<OutgoingConnection>.upsert(connection: OutgoingConnection): List<OutgoingConnection> {
    val index = indexOfFirst { it.name.equals(connection.name, ignoreCase = true) }
    return if (index < 0) this + connection else toMutableList().also { it[index] = connection }
}

/** Removes the entry with [name], if there is one. */
fun List<OutgoingConnection>.removeNamed(name: String): List<OutgoingConnection> =
    filterNot { it.name.equals(name, ignoreCase = true) }

/** The entry [name] refers to, falling back to the first saved one. */
fun List<OutgoingConnection>.activeOrFirst(name: String): OutgoingConnection? =
    firstOrNull { it.name.equals(name, ignoreCase = true) } ?: firstOrNull()

/**
 * The destinations to show for these settings.
 *
 * The repository fills [ReplicaSettings.connections], but a settings object can
 * also be built directly with only the active pair - the debug catalog and the
 * tests both do it. Falling back keeps one saved destination visible instead of
 * showing an empty page.
 */
val ReplicaSettings.effectiveConnections: List<OutgoingConnection>
    get() = connections.ifEmpty {
        if (connectionName.isNotBlank() && connectionUrl.isNotBlank()) {
            listOf(OutgoingConnection(connectionName, connectionUrl))
        } else {
            emptyList()
        }
    }

/** The separators are structural, so they never survive into stored text. */
private fun sanitise(value: String) = value.trim().filterNot { it == FIELD || it == RECORD }
