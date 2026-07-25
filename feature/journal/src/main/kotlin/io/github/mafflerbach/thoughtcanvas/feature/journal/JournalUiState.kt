package io.github.mafflerbach.thoughtcanvas.feature.journal

import android.net.Uri
import androidx.compose.ui.text.input.TextFieldValue
import java.time.LocalDate

data class JournalUiState(
    val date: LocalDate,
    val markdown: TextFieldValue = TextFieldValue(""),
    val tags: List<String> = emptyList(),
    val tagInput: String = "",
    val images: List<Uri> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
)
