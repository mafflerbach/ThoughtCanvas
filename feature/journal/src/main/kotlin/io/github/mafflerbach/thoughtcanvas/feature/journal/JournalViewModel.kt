package io.github.mafflerbach.thoughtcanvas.feature.journal

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mafflerbach.thoughtcanvas.core.storage.SafPathResolver
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Owns the state for the currently viewed journal entry (typically today).
 *
 * Autosave strategy: every user edit updates in-memory state immediately;
 * a debounced coroutine flushes to disk 750ms after the last text/tags
 * change. Selection-only changes do not trigger a save.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class JournalViewModel
    @Inject
    constructor(
        private val repository: JournalRepository,
        private val pathResolver: SafPathResolver,
    ) : ViewModel() {
        private val _state = MutableStateFlow(JournalUiState(date = LocalDate.now()))
        val state: StateFlow<JournalUiState> = _state.asStateFlow()

        private var autosaveJob: Job? = null

        init {
            val date = _state.value.date
            loadForDate(date)
            observeImages(date)
            startAutosave(date)
        }

        private fun loadForDate(date: LocalDate) {
            viewModelScope.launch {
                val content = repository.load(date)
                _state.value = _state.value.copy(
                    markdown = TextFieldValue(
                        text = content.markdown,
                        selection = TextRange(content.markdown.length),
                    ),
                    tags = content.tags,
                    isLoading = false,
                )
            }
        }

        private fun observeImages(date: LocalDate) {
            repository
                .observeImageAttachments(date)
                .onEach { paths ->
                    val uris = paths.mapNotNull { pathResolver.resolveUri(it) }
                    _state.value = _state.value.copy(images = uris)
                }.launchIn(viewModelScope)
        }

        private fun startAutosave(date: LocalDate) {
            autosaveJob?.cancel()
            autosaveJob = _state
                .drop(1)
                .distinctUntilChanged { old, new ->
                    // Only trigger save on text/tag changes; selection moves alone don't count.
                    old.markdown.text == new.markdown.text && old.tags == new.tags
                }.debounce(AUTOSAVE_DEBOUNCE_MS)
                .onEach { current ->
                    if (current.isLoading) return@onEach
                    _state.value = current.copy(isSaving = true)
                    repository.save(date, current.markdown.text, current.tags)
                    _state.value = _state.value.copy(isSaving = false)
                }.launchIn(viewModelScope)
        }

        fun onMarkdownChanged(newValue: TextFieldValue) {
            _state.value = _state.value.copy(markdown = newValue)
        }

        fun onTagInputChanged(newValue: String) {
            _state.value = _state.value.copy(tagInput = newValue)
        }

        fun onCommitTag() {
            val current = _state.value
            val name = current.tagInput.trim()
            if (name.isEmpty() || name in current.tags) {
                _state.value = current.copy(tagInput = "")
                return
            }
            _state.value = current.copy(tags = current.tags + name, tagInput = "")
        }

        fun onRemoveTag(name: String) {
            val current = _state.value
            _state.value = current.copy(tags = current.tags - name)
        }

        fun onImagePicked(bytes: ByteArray, extension: String) {
            viewModelScope.launch {
                val entryRelative = repository.addImage(_state.value.date, bytes, extension)
                insertMarkdownLink(entryRelative)
            }
        }

        /**
         * Insert `![](images/x.jpg)` at the current cursor. Adds newlines
         * around it when needed so the link renders on its own line in most
         * Markdown viewers.
         */
        private fun insertMarkdownLink(entryRelativePath: String) {
            val current = _state.value
            val existing = current.markdown.text
            val cursor = current.markdown.selection.start
                .coerceIn(0, existing.length)
            val prefix = existing.substring(0, cursor)
            val suffix = existing.substring(cursor)

            val leading = when {
                prefix.isEmpty() -> ""
                prefix.endsWith("\n\n") -> ""
                prefix.endsWith("\n") -> "\n"
                else -> "\n\n"
            }
            val trailing = when {
                suffix.isEmpty() -> "\n"
                suffix.startsWith("\n\n") -> ""
                suffix.startsWith("\n") -> "\n"
                else -> "\n\n"
            }

            val snippet = "$leading![]($entryRelativePath)$trailing"
            val newText = prefix + snippet + suffix
            val newCursor = (prefix + snippet).length

            _state.value = current.copy(
                markdown = TextFieldValue(
                    text = newText,
                    selection = TextRange(newCursor),
                ),
            )
        }

        private companion object {
            const val AUTOSAVE_DEBOUNCE_MS = 750L
        }
    }
