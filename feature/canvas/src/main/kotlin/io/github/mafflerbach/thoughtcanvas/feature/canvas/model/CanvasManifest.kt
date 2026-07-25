package io.github.mafflerbach.thoughtcanvas.feature.canvas.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * On-disk `*.canvas.json` file. See `architecture/ADR-0004-block-canvas.md`.
 *
 * All `ref` values on child objects are relative to the directory of the
 * canvas file itself. World coordinates and sizes are logical pixels.
 */
@Serializable
data class CanvasManifest(
    val schemaVersion: Int = SCHEMA_VERSION,
    val id: String,
    val createdAt: Long,
    val updatedAt: Long,
    val tags: List<String> = emptyList(),
    val world: WorldBounds = WorldBounds(),
    val viewport: Viewport = Viewport(),
    val blocks: List<Block> = emptyList(),
    val floatingStrokes: FloatingStrokesRef? = null,
    val edges: List<Edge> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION: Int = 1
        const val DEFAULT_WORLD_SIZE: Int = 8192
    }
}

@Serializable
data class WorldBounds(
    val width: Int = CanvasManifest.DEFAULT_WORLD_SIZE,
    val height: Int = CanvasManifest.DEFAULT_WORLD_SIZE,
)

@Serializable
data class Viewport(
    val x: Float = 0f,
    val y: Float = 0f,
    val zoom: Float = 1f,
)

/**
 * A block placed on the canvas. `kind` is a string (not an enum) so old app
 * versions can still parse manifests written by newer versions and just
 * skip block kinds they do not understand.
 *
 * `ref` may be null now to leave room for future inline kinds; the three
 * Phase-2 MVP kinds all require `ref`.
 */
@Serializable
data class Block(
    val id: String,
    val kind: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val z: Int = 0,
    val ref: String? = null,
)

@Serializable
data class FloatingStrokesRef(
    val ref: String,
)

@Serializable
data class Edge(
    val id: String,
    val from: EdgeEndpoint,
    val to: EdgeEndpoint,
    val label: String? = null,
)

@Serializable
data class EdgeEndpoint(
    @SerialName("blockId") val blockId: String,
    val side: String? = null,
)

/**
 * Known block kinds. Compared as strings so unknown values from newer
 * schemas do not crash the parser.
 */
object BlockKind {
    const val MARKDOWN_EMBED: String = "markdown-embed"
    const val IMAGE_EMBED: String = "image-embed"
    const val INK_REGION: String = "ink-region"
}

/** Edge attachment sides (optional). */
object EdgeSide {
    const val TOP: String = "top"
    const val RIGHT: String = "right"
    const val BOTTOM: String = "bottom"
    const val LEFT: String = "left"
}
