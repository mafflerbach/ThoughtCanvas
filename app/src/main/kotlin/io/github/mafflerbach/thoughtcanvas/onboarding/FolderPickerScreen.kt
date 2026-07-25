package io.github.mafflerbach.thoughtcanvas.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun FolderPickerScreen(viewModel: FolderPickerViewModel = hiltViewModel()) {
    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
            uri?.let(viewModel::onFolderPicked)
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Welcome to ThoughtCanvas",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text =
                "Choose a folder where your journal, drawings and photos will live. " +
                    "Sync it with any tool you like — the app never moves your data.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .widthIn(max = 480.dp)
                    .padding(top = 16.dp, bottom = 32.dp),
        )
        Button(onClick = { launcher.launch(null) }) {
            Text("Pick storage folder")
        }
    }
}
