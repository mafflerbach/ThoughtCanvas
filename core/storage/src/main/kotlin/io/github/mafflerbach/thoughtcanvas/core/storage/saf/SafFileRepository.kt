package io.github.mafflerbach.thoughtcanvas.core.storage.saf

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.github.mafflerbach.thoughtcanvas.core.storage.FileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * [FileRepository] backed by the Storage Access Framework via [DocumentFile].
 *
 * The root [Uri] is provided lazily so the app can create a repository
 * instance before the user has picked a folder; the first call after
 * configuration will resolve the tree.
 *
 * Design notes:
 * - All I/O is dispatched to [ioDispatcher] because DocumentFile calls hit
 *   the content resolver synchronously.
 * - Text is UTF-8. No charset override in Phase 1.
 * - "path" is `/`-separated, relative to the root, e.g. `Journal/2025/01/17/journal.md`.
 */
class SafFileRepository(
    private val context: Context,
    private val rootUriProvider: () -> Uri?,
    private val ioDispatcher: CoroutineDispatcher,
) : FileRepository {
    private fun requireRoot(): DocumentFile {
        val uri =
            rootUriProvider()
                ?: error("Storage root not configured. Call StorageRootPreferences.setRoot(...) first.")
        return DocumentFile.fromTreeUri(context, uri)
            ?: error("Cannot open document tree for uri=$uri")
    }

    override suspend fun exists(path: String): Boolean =
        withContext(ioDispatcher) {
            resolve(path) != null
        }

    override suspend fun createDirectories(path: String): Unit =
        withContext(ioDispatcher) {
            ensureDirectory(path.segments())
        }

    override suspend fun readText(path: String): String? =
        withContext(ioDispatcher) {
            val file = resolve(path)?.takeIf { it.isFile } ?: return@withContext null
            context.contentResolver.openInputStream(file.uri)?.use { it.readBytes().decodeToString() }
        }

    override suspend fun writeText(
        path: String,
        content: String,
    ) {
        writeBytes(path, content.encodeToByteArray())
    }

    override suspend fun writeBytes(
        path: String,
        bytes: ByteArray,
    ): Unit =
        withContext(ioDispatcher) {
            val segments = path.segments()
            require(segments.isNotEmpty()) { "path must not be empty" }
            val parent = ensureDirectory(segments.dropLast(1))
            val name = segments.last()
            val target =
                parent.findFile(name)?.takeIf { it.isFile }
                    ?: parent.createFile("application/octet-stream", name)
                    ?: error("Cannot create file at $path")
            context.contentResolver.openOutputStream(target.uri, "wt")?.use { it.write(bytes) }
                ?: error("Cannot open output stream for $path")
        }

    override suspend fun list(path: String): List<String> =
        withContext(ioDispatcher) {
            val dir = resolve(path)?.takeIf { it.isDirectory } ?: return@withContext emptyList()
            dir.listFiles().mapNotNull { it.name }
        }

    override suspend fun delete(path: String): Boolean =
        withContext(ioDispatcher) {
            resolve(path)?.delete() ?: false
        }

    // --- helpers -----------------------------------------------------------

    private fun String.segments(): List<String> = split('/').filter { it.isNotEmpty() }

    private fun resolve(path: String): DocumentFile? {
        var current: DocumentFile = requireRoot()
        for (segment in path.segments()) {
            current = current.findFile(segment) ?: return null
        }
        return current
    }

    private fun ensureDirectory(segments: List<String>): DocumentFile {
        var current: DocumentFile = requireRoot()
        for (segment in segments) {
            val existing = current.findFile(segment)
            current =
                when {
                    existing != null && existing.isDirectory -> existing
                    existing != null -> error("Path segment '$segment' exists but is not a directory")
                    else ->
                        current.createDirectory(segment)
                            ?: error("Failed to create directory segment '$segment'")
                }
        }
        return current
    }
}
