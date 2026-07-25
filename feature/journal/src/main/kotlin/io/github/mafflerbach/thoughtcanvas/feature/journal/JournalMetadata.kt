package io.github.mafflerbach.thoughtcanvas.feature.journal

import kotlinx.serialization.Serializable

/**
 * On-disk companion to `journal.md`. Kept small; anything tag/attachment-shaped
 * that also needs querying lives in the Room index alongside this file.
 *
 * Serialised as pretty-printed JSON so the user can eyeball or diff it in git.
 */
@Serializable
data class JournalMetadata(
    val schemaVersion: Int = SCHEMA_VERSION,
    val date: String, // ISO-8601 YYYY-MM-DD
    val createdAt: Long,
    val updatedAt: Long,
    val tags: List<String> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION: Int = 1
    }
}
