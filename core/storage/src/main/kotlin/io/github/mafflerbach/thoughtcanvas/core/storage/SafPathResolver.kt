package io.github.mafflerbach.thoughtcanvas.core.storage

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves a storage-relative path (e.g. `Journal/2025/01/17/images/x.jpg`)
 * to a real `content://` [Uri] under the user-picked SAF root.
 *
 * Returns `null` if the root is not configured or the file does not exist.
 * Non-suspend for use from Compose bodies; each call walks the tree via
 * DocumentFile which is O(depth) — acceptable for the handful of images
 * per journal entry we expect in Phase 1.
 */
@Singleton
class SafPathResolver
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val rootState: StorageRootState,
    ) {
        fun resolveUri(relativePath: String): Uri? {
            val rootUri = (rootState.state.value as? StorageRoot.Configured)?.uriString?.toUri() ?: return null
            var current: DocumentFile = DocumentFile.fromTreeUri(context, rootUri) ?: return null
            for (segment in relativePath.split('/').filter { it.isNotEmpty() }) {
                current = current.findFile(segment) ?: return null
            }
            return current.uri
        }
    }
