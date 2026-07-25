package io.github.mafflerbach.thoughtcanvas.feature.canvas

import androidx.ink.strokes.Stroke
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Minimal state holder for the ink canvas.
 *
 * Persistence and undo/redo will be layered on in follow-up slices; this
 * exists now so the UI has a stable ViewModel to bind against.
 */
@HiltViewModel
class CanvasViewModel
    @Inject
    constructor() : ViewModel() {
        private val _strokes = MutableStateFlow<List<Stroke>>(emptyList())
        val strokes: StateFlow<List<Stroke>> = _strokes.asStateFlow()

        fun onStrokesFinished(newStrokes: List<Stroke>) {
            _strokes.value = _strokes.value + newStrokes
        }

        fun clear() {
            _strokes.value = emptyList()
        }
    }
