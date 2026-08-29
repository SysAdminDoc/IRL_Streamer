package com.irlstreamer.reconstruction

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Storage that stands in for the file-backed store.
 *
 * The JVM DataStore cannot write under a Windows unit test: its `.tmp` rename
 * fails on the first write. What these tests cover is the repository's
 * snapshot-and-restore, not persistence to disk, and `edit {}` plus the
 * preference transforms are the library's own code either way.
 */
class InMemoryPreferencesDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}
