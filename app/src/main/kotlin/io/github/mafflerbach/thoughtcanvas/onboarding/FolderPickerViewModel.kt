package io.github.mafflerbach.thoughtcanvas.onboarding

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mafflerbach.thoughtcanvas.core.storage.StorageRoot
import io.github.mafflerbach.thoughtcanvas.core.storage.StorageRootPreferences
import io.github.mafflerbach.thoughtcanvas.core.storage.StorageRootState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FolderPickerViewModel
    @Inject
    constructor(
        private val preferences: StorageRootPreferences,
        rootState: StorageRootState,
    ) : ViewModel() {
        val root: StateFlow<StorageRoot> = rootState.state

        fun onFolderPicked(uri: Uri) {
            viewModelScope.launch { preferences.setRoot(uri) }
        }
    }
