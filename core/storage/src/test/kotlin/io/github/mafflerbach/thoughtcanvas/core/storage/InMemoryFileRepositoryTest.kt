package io.github.mafflerbach.thoughtcanvas.core.storage

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryFileRepositoryTest {
    @Test
    fun `writeText then readText round-trips`() =
        runTest {
            val repo = InMemoryFileRepository()
            repo.writeText("Journal/2025/01/17/journal.md", "# hello")
            assertEquals("# hello", repo.readText("Journal/2025/01/17/journal.md"))
        }

    @Test
    fun `writeText creates parent directories implicitly`() =
        runTest {
            val repo = InMemoryFileRepository()
            repo.writeText("Journal/2025/01/17/journal.md", "x")
            assertTrue(repo.exists("Journal/2025/01/17"))
            assertTrue(repo.exists("Journal/2025"))
        }

    @Test
    fun `list returns immediate children only`() =
        runTest {
            val repo = InMemoryFileRepository()
            repo.writeText("Journal/2025/01/17/journal.md", "x")
            repo.writeText("Journal/2025/01/17/canvas.json", "y")
            repo.writeText("Journal/2025/01/18/journal.md", "z")

            assertEquals(listOf("17", "18"), repo.list("Journal/2025/01"))
            assertEquals(listOf("canvas.json", "journal.md"), repo.list("Journal/2025/01/17"))
        }

    @Test
    fun `readText returns null for missing file`() =
        runTest {
            val repo = InMemoryFileRepository()
            assertNull(repo.readText("nope.txt"))
        }

    @Test
    fun `delete removes files and directories recursively`() =
        runTest {
            val repo = InMemoryFileRepository()
            repo.writeText("Journal/2025/01/17/journal.md", "x")
            assertTrue(repo.delete("Journal/2025"))
            assertFalse(repo.exists("Journal/2025/01/17/journal.md"))
            assertFalse(repo.exists("Journal/2025"))
        }
}
