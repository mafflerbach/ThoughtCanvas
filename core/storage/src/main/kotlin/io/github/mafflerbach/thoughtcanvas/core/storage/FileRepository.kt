package io.github.mafflerbach.thoughtcanvas.core.storage

/**
 * Abstraction over the user-owned storage root. Paths are always
 * relative to the root (e.g. "Journal/2025/01/17/journal.md").
 *
 * Kept intentionally minimal in Phase 1: text + bytes only, no streaming.
 * All operations are suspend so implementations can move I/O off the main
 * thread.
 */
interface FileRepository {
    suspend fun exists(path: String): Boolean

    /** Create the directory at [path] and any missing parents. No-op if it already exists. */
    suspend fun createDirectories(path: String)

    /** Returns the file contents as UTF-8 text, or `null` if the file does not exist. */
    suspend fun readText(path: String): String?

    /** Writes UTF-8 text, creating parent directories as needed. Overwrites if the file exists. */
    suspend fun writeText(
        path: String,
        content: String,
    )

    /** Writes raw bytes, creating parent directories as needed. Overwrites if the file exists. */
    suspend fun writeBytes(
        path: String,
        bytes: ByteArray,
    )

    /** Lists immediate children of the directory at [path]. Empty list if missing. */
    suspend fun list(path: String): List<String>

    /** Deletes the file or (recursively) the directory at [path]. Returns `true` if anything was removed. */
    suspend fun delete(path: String): Boolean
}
