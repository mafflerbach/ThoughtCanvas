package io.github.mafflerbach.thoughtcanvas.feature.journal

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HeaderDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayJournalScreen(viewModel: JournalViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val resolver = context.contentResolver
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@rememberLauncherForActivityResult
        val mime = resolver.getType(uri).orEmpty()
        val extension = when {
            mime.endsWith("/png") -> "png"
            mime.endsWith("/webp") -> "webp"
            mime.endsWith("/heic") -> "heic"
            mime.endsWith("/gif") -> "gif"
            else -> "jpg"
        }
        viewModel.onImagePicked(bytes, extension)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Journal", style = MaterialTheme.typography.titleMedium)
                        Text(
                            state.date.format(HeaderDateFormatter),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                actions = {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                photoLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            }) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add photo")
            }
        },
    ) { padding ->
        if (state.isLoading) {
            LoadingState(padding)
        } else {
            JournalBody(
                state = state,
                onMarkdownChanged = viewModel::onMarkdownChanged,
                onTagInputChanged = viewModel::onTagInputChanged,
                onCommitTag = viewModel::onCommitTag,
                onRemoveTag = viewModel::onRemoveTag,
                padding = padding,
            )
        }
    }
}

@Composable
private fun LoadingState(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JournalBody(
    state: JournalUiState,
    onMarkdownChanged: (TextFieldValue) -> Unit,
    onTagInputChanged: (String) -> Unit,
    onCommitTag: () -> Unit,
    onRemoveTag: (String) -> Unit,
    padding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TagRow(
            tags = state.tags,
            tagInput = state.tagInput,
            onTagInputChanged = onTagInputChanged,
            onCommitTag = onCommitTag,
            onRemoveTag = onRemoveTag,
        )

        OutlinedTextField(
            value = state.markdown,
            onValueChange = onMarkdownChanged,
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp),
            label = { Text("Markdown") },
            placeholder = { Text("What happened today?") },
        )

        if (state.images.isNotEmpty()) {
            ImageStrip(uris = state.images)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagRow(
    tags: List<String>,
    tagInput: String,
    onTagInputChanged: (String) -> Unit,
    onCommitTag: () -> Unit,
    onRemoveTag: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (tags.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tags, key = { it }) { name ->
                    FilterChip(
                        selected = true,
                        onClick = { onRemoveTag(name) },
                        label = { Text(name) },
                    )
                }
            }
        }
        OutlinedTextField(
            value = tagInput,
            onValueChange = onTagInputChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Add tag") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onCommitTag() }),
        )
    }
}

@Composable
private fun ImageStrip(uris: List<android.net.Uri>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(uris, key = { it.toString() }) { uri ->
            AsyncImage(
                model = ImageRequest
                    .Builder(LocalContext.current)
                    .data(uri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
        }
    }
}
