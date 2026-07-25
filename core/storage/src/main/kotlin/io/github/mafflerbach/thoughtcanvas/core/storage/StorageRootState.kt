package io.github.mafflerbach.thoughtcanvas.core.storage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Application-wide snapshot of the current [StorageRoot], suitable for
 * synchronous reads by non-coroutine callers (e.g. [SafFileRepository]).
 *
 * Backed by [StorageRootPreferences] and started eagerly in [scope] so the
 * first synchronous read after app start is already populated.
 */
class StorageRootState(
    preferences: StorageRootPreferences,
    scope: CoroutineScope,
) {
    val state: StateFlow<StorageRoot> =
        preferences.root.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = StorageRoot.Unconfigured,
        )
}
