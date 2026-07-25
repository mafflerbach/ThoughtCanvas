package io.github.mafflerbach.thoughtcanvas.feature.canvas

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.mafflerbach.thoughtcanvas.core.database.CanvasIndexRepository
import io.github.mafflerbach.thoughtcanvas.core.database.ThoughtCanvasDatabase
import io.github.mafflerbach.thoughtcanvas.core.storage.JournalPathResolver
import io.github.mafflerbach.thoughtcanvas.feature.canvas.model.BlockKind
import io.github.mafflerbach.thoughtcanvas.feature.canvas.testing.InMemoryFileRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DailyCanvasBootstrapTest {
    private lateinit var db: ThoughtCanvasDatabase
    private lateinit var index: CanvasIndexRepository
    private lateinit var files: InMemoryFileRepository
    private lateinit var canvasRepo: CanvasRepository
    private lateinit var bootstrap: DailyCanvasBootstrap

    private val today = LocalDate.of(2025, 1, 17)
    private val paths = JournalPathResolver()

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room
            .inMemoryDatabaseBuilder(ctx, ThoughtCanvasDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        index = CanvasIndexRepository(
            db = db,
            canvasDao = db.canvasDao(),
            blockDao = db.blockDao(),
            edgeDao = db.edgeDao(),
            tagDao = db.tagDao(),
            canvasTagDao = db.canvasTagDao(),
        )
        files = InMemoryFileRepository()
        canvasRepo = CanvasRepository(files)
        bootstrap = DailyCanvasBootstrap(
            fileRepository = files,
            paths = paths,
            canvasRepository = canvasRepo,
            indexRepository = index,
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun firstCall_createsCanvasMarkdownAndDbRow() = runTest {
        val handle = bootstrap.openOrCreate(today)

        assertTrue(handle.wasCreated)
        assertEquals("Journal/2025/01/17/daily.canvas.json", handle.canvasPath)
        assertEquals("Journal/2025/01/17/journal.md", handle.markdownPath)

        // Files on disk.
        assertTrue(files.files().containsKey(handle.canvasPath))
        assertTrue(files.files().containsKey(handle.markdownPath))

        // Manifest has a single markdown-embed block pointing at journal.md.
        val manifest = handle.manifest
        val block = manifest.blocks.single()
        assertEquals(BlockKind.MARKDOWN_EMBED, block.kind)
        assertEquals("journal.md", block.ref)

        // DB row mirrors filesystem.
        val row = index.observeCanvas(manifest.id).first()
        assertNotNull(row)
        assertEquals(handle.canvasPath, row!!.path)

        val blocks = index.observeBlocks(manifest.id).first()
        assertEquals(1, blocks.size)
        assertEquals(BlockKind.MARKDOWN_EMBED, blocks.single().kind)
    }

    @Test
    fun secondCall_returnsExistingCanvasUnchanged() = runTest {
        val first = bootstrap.openOrCreate(today)
        val second = bootstrap.openOrCreate(today)

        assertTrue(first.wasCreated)
        assertFalse(second.wasCreated)
        assertEquals(first.canvasId, second.canvasId)
        assertEquals(first.manifest, second.manifest)
    }

    @Test
    fun bootstrap_writesRefAsRelativePath() = runTest {
        val handle = bootstrap.openOrCreate(today)
        // Simulate loading fresh from disk to confirm the persisted ref.
        val reloaded = canvasRepo.load(handle.canvasPath)!!
        assertEquals("journal.md", reloaded.blocks.single().ref)
        assertEquals(
            handle.markdownPath,
            canvasRepo.resolveRef(handle.canvasPath, reloaded.blocks.single().ref!!),
        )
    }
}
