package io.github.mafflerbach.thoughtcanvas.core.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.storageDataStore: DataStore<Preferences> by preferencesDataStore(name = "storage_root")

/**
 * Persists the SAF tree Uri chosen by the user and takes the persistable
 * permission so it survives process death and device reboots.
 */
class StorageRootPreferences(
    private val context: Context,
) {
    private val store = context.storageDataStore

    val root: Flow<StorageRoot> =
        store.data.map { prefs ->
            prefs[KEY_URI]?.let { StorageRoot.Configured(it) } ?: StorageRoot.Unconfigured
        }

    /**
     * Persist [uri] as the new root, taking persistable read/write permission.
     *
     * The caller is expected to have received [uri] from
     * `Intent.ACTION_OPEN_DOCUMENT_TREE` with `FLAG_GRANT_READ_URI_PERMISSION`
     * and `FLAG_GRANT_WRITE_URI_PERMISSION` set on the launcher.
     */
    suspend fun setRoot(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, flags)
        store.edit { it[KEY_URI] = uri.toString() }
    }

    suspend fun clear() {
        store.edit { it.remove(KEY_URI) }
    }

    private companion object {
        val KEY_URI = stringPreferencesKey("root_uri")
    }
}
