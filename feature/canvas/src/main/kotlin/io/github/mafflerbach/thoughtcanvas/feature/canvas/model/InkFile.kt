package io.github.mafflerbach.thoughtcanvas.feature.canvas.model

import kotlinx.serialization.Serializable

/**
 * On-disk `<id>.ink.json` file.
 *
 * Two flavours share this shape:
 *  - **Per-region file**, referenced by an `ink-region` block. Strokes are
 *    stored in **region-local** coordinates so moving the region is cheap.
 *  - **Floating strokes file**, referenced from the canvas manifest.
 *    Strokes are stored in **world** coordinates.
 *
 * The shape does not distinguish; the meaning of the coordinates is
 * determined by which reference points at the file.
 */
@Serializable
data class InkFile(
    val schemaVersion: Int = SCHEMA_VERSION,
    val id: String,
    val tags: List<String> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long,
    val strokes: List<PersistedStroke> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION: Int = 1
    }
}

@Serializable
data class PersistedStroke(
    val id: String,
    val brush: PersistedBrush,
    val inputs: List<PersistedStrokeInput>,
    val createdAt: Long,
)

@Serializable
data class PersistedBrush(
    /** e.g. "pressure-pen-v1"; app maps to a concrete `androidx.ink.brush.BrushFamily`. */
    val family: String,
    /** #RRGGBBAA hex string. */
    val color: String,
    val size: Float,
    val epsilon: Float,
)

@Serializable
data class PersistedStrokeInput(
    val x: Float,
    val y: Float,
    /** Milliseconds since stroke start. */
    val t: Long,
    val pressure: Float,
    val tiltX: Float,
    val tiltY: Float,
    val orientation: Float,
)
