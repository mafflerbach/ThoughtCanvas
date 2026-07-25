package io.github.mafflerbach.thoughtcanvas.feature.journal

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.mafflerbach.thoughtcanvas.core.database.JournalIndexRepository
import io.github.mafflerbach.thoughtcanvas.core.database.ThoughtCanvasDatabase
import io.github.mafflerbach.thoughtcanvas.core.storage.JournalPathResolver
import io.github.mafflerbach.thoughtcanvas.feature.journal.testing.InMemoryFileRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class JournalRepositoryTest {
    private lateinit var db: ThoughtCanvasDatabase
    private lateinit var index: JournalIndexRepository
    private lateinit var files: InMemoryFileRepository
    private lateinit var repo: JournalRepository

    private val today = LocalDate.of(2025, 1, 17)
    private val paths = JournalPathResolver()

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room
            .inMemoryDatabaseBuilder(ctx, ThoughtCanvasDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        index = JournalIndexRepository(
            db = db,
            entryDao = db.journalEntryDao(),
            tagDao = db.tagDao(),
            attachmentDao = db.attachmentDao(),
        )
        files = InMemoryFileRepository()
        repo = JournalRepository(
            fileRepository = files,
            indexRepository = index,
            paths = paths,
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun load_returnsEmptyShellWhenNothingOnDisk() = runTest {
        val content = repo.load(today)
        assertEquals("", content.markdown)
        assertEquals(emptyList<String>(), content.tags)
        assertNull(content.createdAt)
    }

    @Test
    fun save_writesMarkdownAndMetadataAndIndex() = runTest {
        repo.save(today, markdown = "# hello", tags = listOf("work", "ideas"))

        // Files landed on disk in the expected locations.
        val markdownPath = paths.markdownFile(today)
        val metadataPath = paths.metadataFile(today)
        assertEquals("# hello", files.readText(markdownPath))

        val rawMetadata = files.readText(metadataPath)
        assertNotNull(rawMetadata)
        val json = Json.parseToJsonElement(rawMetadata!!).jsonObject
        assertEquals("2025-01-17", json["date"]!!.jsonPrimitive.content)
        assertEquals(listOf("work", "ideas"), json["tags"]!!.jsonArray.map { it.jsonPrimitive.content })

        // Index mirrors filesystem.
        val entry = index.observeEntry(today).first()
        assertNotNull(entry)
        val tags = index.observeTags(today).first().map { it.name }
        assertEquals(listOf("ideas", "work"), tags) // observeTags orders alphabetically
    }

    @Test
    fun save_preservesCreatedAtAcrossUpdates() = runTest {
        repo.save(today, "first", tags = emptyList())
        val first = index.observeEntry(today).first()!!
        Thread.sleep(5) // ensure clock ticks
        repo.save(today, "second", tags = emptyList())
        val second = index.observeEntry(today).first()!!

        assertEquals(first.createdAt, second.createdAt)
        assertTrue(second.updatedAt >= first.updatedAt)
    }

    @Test
    fun save_normalisesAndDedupesTags() = runTest {
        repo.save(today, markdown = "x", tags = listOf(" work ", "work", "", "  ideas"))
        val tags = index.observeTags(today).first().map { it.name }
        assertEquals(listOf("ideas", "work"), tags)
    }

    @Test
    fun addImage_writesFileAndReturnsEntryRelativePath() = runTest {
        repo.save(today, "seed", tags = emptyList())
        val bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47) // fake PNG header
        val markdownLink = repo.addImage(today, bytes, extension = "png")

        // Return value is the path a markdown link should use (relative to journal.md).
        assertTrue(markdownLink.startsWith("images/"))
        assertTrue(markdownLink.endsWith(".png"))

        // File is on disk at the full root-relative location.
        val fileName = markdownLink.removePrefix("images/")
        val rootRelative = "${paths.imagesDirectory(today)}/$fileName"
        assertTrue(files.snapshotFilePaths().contains(rootRelative))

        // Attachment row present in the index (with the root-relative path).
        val attachments = index.observeAttachments(today).first()
        assertEquals(1, attachments.size)
        assertEquals("image", attachments.single().kind)
        assertEquals(rootRelative, attachments.single().relativePath)
    }

    @Test
    fun addImage_defaultsUnknownExtensionToJpg() = runTest {
        repo.save(today, "seed", tags = emptyList())
        val link = repo.addImage(today, byteArrayOf(1, 2, 3), extension = "")
        assertTrue(link.endsWith(".jpg"))
    }

    @Test
    fun observeImageAttachments_emitsOnlyImages() = runTest {
        repo.save(today, "seed", tags = emptyList())
        repo.addImage(today, byteArrayOf(1), "jpg")
        repo.addImage(today, byteArrayOf(2), "png")
        // Directly add a non-image attachment via the index to prove filtering.
        index.addAttachment(today, kind = "audio", relativePath = "audio/a.mp3", createdAt = 1L)

        val onlyImages = repo.observeImageAttachments(today).first()
        assertEquals(2, onlyImages.size)
        assertTrue(onlyImages.all { it.contains("/images/") })
    }
}
