package io.github.mafflerbach.thoughtcanvas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.mafflerbach.thoughtcanvas.core.storage.StorageRoot
import io.github.mafflerbach.thoughtcanvas.feature.journal.TodayJournalScreen
import io.github.mafflerbach.thoughtcanvas.onboarding.FolderPickerScreen
import io.github.mafflerbach.thoughtcanvas.onboarding.FolderPickerViewModel
import io.github.mafflerbach.thoughtcanvas.ui.theme.ThoughtCanvasTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ThoughtCanvasTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(padding),
                    ) {
                        Root()
                    }
                }
            }
        }
    }
}

@Composable
private fun Root(viewModel: FolderPickerViewModel = hiltViewModel()) {
    val root by viewModel.root.collectAsStateWithLifecycle()
    when (root) {
        is StorageRoot.Unconfigured -> FolderPickerScreen(viewModel = viewModel)
        is StorageRoot.Configured -> TodayJournalScreen()
    }
}
