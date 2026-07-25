package io.github.mafflerbach.thoughtcanvas.feature.canvas

import org.junit.Assert.assertEquals
import org.junit.Test

class CanvasPathsTest {
    @Test
    fun `resolveRef resolves sibling file`() {
        val actual = CanvasPaths.resolveRef(
            canvasPath = "Journal/2025/01/17/daily.canvas.json",
            ref = "journal.md",
        )
        assertEquals("Journal/2025/01/17/journal.md", actual)
    }

    @Test
    fun `resolveRef handles nested images dir`() {
        val actual = CanvasPaths.resolveRef(
            canvasPath = "Journal/2025/01/17/daily.canvas.json",
            ref = "images/photo.jpg",
        )
        assertEquals("Journal/2025/01/17/images/photo.jpg", actual)
    }

    @Test
    fun `resolveRef collapses parent segments`() {
        val actual = CanvasPaths.resolveRef(
            canvasPath = "Journal/2025/01/17/daily.canvas.json",
            ref = "../shared/note.md",
        )
        assertEquals("Journal/2025/01/shared/note.md", actual)
    }

    @Test
    fun `resolveRef strips leading slash instead of escaping root`() {
        val actual = CanvasPaths.resolveRef(
            canvasPath = "Canvases/Ideas.canvas.json",
            ref = "/Shared/x.md",
        )
        assertEquals("Canvases/Shared/x.md", actual)
    }

    @Test
    fun `directoryOf returns parent`() {
        assertEquals(
            "Journal/2025/01/17",
            CanvasPaths.directoryOf("Journal/2025/01/17/daily.canvas.json"),
        )
        assertEquals("", CanvasPaths.directoryOf("top.canvas.json"))
    }
}
