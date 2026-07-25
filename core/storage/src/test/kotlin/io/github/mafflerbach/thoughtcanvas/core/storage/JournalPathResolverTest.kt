package io.github.mafflerbach.thoughtcanvas.core.storage

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class JournalPathResolverTest {
    private val resolver = JournalPathResolver()

    @Test
    fun `entry directory zero-pads month and day`() {
        val date = LocalDate.of(2025, 1, 7)
        assertEquals("Journal/2025/01/07", resolver.entryDirectory(date))
    }

    @Test
    fun `standard files resolve under the entry directory`() {
        val date = LocalDate.of(2025, 12, 31)
        val dir = "Journal/2025/12/31"
        assertEquals("$dir/metadata.json", resolver.metadataFile(date))
        assertEquals("$dir/journal.md", resolver.markdownFile(date))
        assertEquals("$dir/canvas.json", resolver.canvasFile(date))
        assertEquals("$dir/images", resolver.imagesDirectory(date))
        assertEquals("$dir/attachments", resolver.attachmentsDirectory(date))
    }
}
