package io.github.mafflerbach.thoughtcanvas.feature.canvas

import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.ink.authoring.compose.InProgressStrokes
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke

private const val DEFAULT_BRUSH_SIZE = 4f
private const val DEFAULT_BRUSH_EPSILON = 0.1f
private const val DEFAULT_BRUSH_COLOR_ARGB: Int = 0xFF000000.toInt() // opaque black

/**
 * Compose canvas that lets the user draw ink strokes and renders the
 * finished ones underneath the in-progress layer.
 *
 * Two layers, bottom to top:
 *  1. A finished-strokes layer drawn with [CanvasStrokeRenderer].
 *  2. `InProgressStrokes` — Ink 1.0's Compose composable that captures the
 *     pointer stream and emits completed [Stroke]s via [onStrokesFinished].
 *
 * The caller owns the [finishedStrokes] list so we can persist/undo without
 * touching this composable.
 */
@Composable
fun InkCanvas(
    finishedStrokes: List<Stroke>,
    onStrokesFinished: (List<Stroke>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val renderer = remember { CanvasStrokeRenderer.create() }
    val defaultBrush = remember {
        Brush.createWithColorIntArgb(
            family = StockBrushes.pressurePen(),
            colorIntArgb = DEFAULT_BRUSH_COLOR_ARGB,
            size = DEFAULT_BRUSH_SIZE,
            epsilon = DEFAULT_BRUSH_EPSILON,
        )
    }
    // No world transform yet — Phase 2 will add pan/zoom.
    val identity = remember { Matrix() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val nativeCanvas = drawContext.canvas.nativeCanvas
            for (stroke in finishedStrokes) {
                renderer.draw(nativeCanvas, stroke, identity)
            }
        }

        InProgressStrokes(
            defaultBrush = defaultBrush,
            onStrokesFinished = onStrokesFinished,
        )
    }
}
