package io.github.mafflerbach.thoughtcanvas.feature.canvas

import io.github.mafflerbach.thoughtcanvas.feature.canvas.model.Block
import io.github.mafflerbach.thoughtcanvas.feature.canvas.model.BlockKind
import io.github.mafflerbach.thoughtcanvas.feature.canvas.model.CanvasManifest
import io.github.mafflerbach.thoughtcanvas.feature.canvas.model.Edge
import io.github.mafflerbach.thoughtcanvas.feature.canvas.model.EdgeEndpoint
import io.github.mafflerbach.thoughtcanvas.feature.canvas.model.EdgeSide
import io.github.mafflerbach.thoughtcanvas.feature.canvas.model.FloatingStrokesRef
import io.github.mafflerbach.thoughtcanvas.feature.canvas.model.InkFile
import io.github.mafflerbach.thoughtcanvas.feature.canvas.model.PersistedBrush
import io.github.mafflerbach.thoughtcanvas.feature.canvas.model.PersistedStroke
import io.github.mafflerbach.thoughtcanvas.feature.canvas.model.PersistedStrokeInput
import io.github.mafflerbach.thoughtcanvas.feature.canvas.model.Viewport
import io.github.mafflerbach.thoughtcanvas.feature.canvas.model.WorldBounds
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasManifestSerializationTest {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @Test
    fun `canvas manifest round trips`() {
        val original = CanvasManifest(
            id = "canvas-uuid",
            createdAt = 1L,
            updatedAt = 2L,
            tags = listOf("daily", "work"),
            world = WorldBounds(width = 4096, height = 4096),
            viewport = Viewport(x = 100f, y = -50f, zoom = 1.5f),
            blocks = listOf(
                Block(
                    id = "b1",
                    kind = BlockKind.MARKDOWN_EMBED,
                    x = 0f,
                    y = 0f,
                    width = 320f,
                    height = 240f,
                    z = 0,
                    ref = "journal.md",
                ),
                Block(
                    id = "b2",
                    kind = BlockKind.INK_REGION,
                    x = 400f,
                    y = 0f,
                    width = 500f,
                    height = 500f,
                    z = 1,
                    ref = "abc.ink.json",
                ),
            ),
            floatingStrokes = FloatingStrokesRef(ref = "daily.floating.ink.json"),
            edges = listOf(
                Edge(
                    id = "e1",
                    from = EdgeEndpoint(blockId = "b1", side = EdgeSide.RIGHT),
                    to = EdgeEndpoint(blockId = "b2", side = EdgeSide.LEFT),
                    label = "explains",
                ),
            ),
        )

        val encoded = json.encodeToString(CanvasManifest.serializer(), original)
        val decoded = json.decodeFromString(CanvasManifest.serializer(), encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `manifest json shape matches ADR schema`() {
        val manifest = CanvasManifest(
            id = "cid",
            createdAt = 100L,
            updatedAt = 200L,
            blocks = listOf(
                Block(
                    id = "bid",
                    kind = BlockKind.MARKDOWN_EMBED,
                    x = 1f,
                    y = 2f,
                    width = 3f,
                    height = 4f,
                    ref = "journal.md",
                ),
            ),
        )
        val root = Json
            .parseToJsonElement(
                json.encodeToString(CanvasManifest.serializer(), manifest),
            ).jsonObject

        assertEquals("1", root["schemaVersion"]!!.jsonPrimitive.content)
        assertEquals("cid", root["id"]!!.jsonPrimitive.content)
        assertTrue(root.containsKey("world"))
        assertTrue(root.containsKey("viewport"))
        assertTrue(root.containsKey("blocks"))
    }

    @Test
    fun `unknown block kind survives round-trip`() {
        val payload =
            """
            {
              "schemaVersion": 1, "id": "c", "createdAt": 1, "updatedAt": 2,
              "blocks": [
                {"id":"b","kind":"future-widget","x":0,"y":0,"width":1,"height":1,"z":0,"ref":"x"}
              ]
            }
            """.trimIndent()
        val decoded = json.decodeFromString(CanvasManifest.serializer(), payload)
        assertEquals("future-widget", decoded.blocks.single().kind)
    }

    @Test
    fun `ink file round trips`() {
        val original = InkFile(
            id = "ink-uuid",
            tags = listOf("sketch"),
            createdAt = 1L,
            updatedAt = 2L,
            strokes = listOf(
                PersistedStroke(
                    id = "s1",
                    brush = PersistedBrush(
                        family = "pressure-pen-v1",
                        color = "#000000FF",
                        size = 4f,
                        epsilon = 0.1f,
                    ),
                    inputs = listOf(
                        PersistedStrokeInput(
                            x = 1.5f,
                            y = 2.5f,
                            t = 12L,
                            pressure = 0.42f,
                            tiltX = 0.1f,
                            tiltY = -0.2f,
                            orientation = 1.57f,
                        ),
                    ),
                    createdAt = 10L,
                ),
            ),
        )
        val encoded = json.encodeToString(InkFile.serializer(), original)
        val decoded = json.decodeFromString(InkFile.serializer(), encoded)
        assertEquals(original, decoded)
    }
}
