package io.github.mafflerbach.thoughtcanvas.core.storage

/**
 * The user-picked filesystem root the app operates on.
 *
 * Kept as a sealed type so consumers can react to the "not yet chosen"
 * state at compile time (typically driving a folder-picker UI).
 */
sealed interface StorageRoot {
    data object Unconfigured : StorageRoot

    data class Configured(
        val uriString: String,
    ) : StorageRoot
}
