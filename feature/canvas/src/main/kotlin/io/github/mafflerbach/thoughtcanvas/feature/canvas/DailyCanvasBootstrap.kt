package io.github.mafflerbach.thoughtcanvas.feature.canvas

import io.github.mafflerbach.thoughtcanvas.core.database.CanvasIndexRepository
import io.github.mafflerbach.thoughtcanvas.core.database.entity.BlockEntity
import io.github.mafflerbach.thoughtcanvas.core.database.entity.CanvasEntity
import io.github.mafflerbach.thoughtcanvas.core.storage.FileRepository
import io.github.mafflerbach.thoughtcanvas.core.storage.JournalPathResolver
import io.github.mafflerbach.thoughtcanvas.feature.canvas.model.Block
import io.github.mafflerbach.thoughtcanvas.feature.canvas.model.BlockKind
import io.github.mafflerbach.thoughtcanvas.feature.canvas.model.CanvasManifest
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of opening (or freshly creating) the daily canvas for a date.
 *
 * [canvasPath] and [markdownPath] are storage-root-relative and safe to
 * hand to `FileRepository`.
 */
data class DailyCanvasHandle(
    val canvasId: String,
    val canvasPath: String,
    val markdownPath: String,
    val manifest: CanvasManifest,
    val wasCreated: Boolean,
)

/**
 * On first access of a given day, materialises:
 *  - `Journal/YYYY/MM/DD/journal.md` (empty)
 *  - `Journal/YYYY/MM/DD/daily.canvas.json` containing a single
 *    `markdown-embed` block pointing at `journal.md`
 *
 * On subsequent access the existing files are returned unchanged.
 *
 * The DB index is mirrored (canvas row + block row); missing rows are
 * regenerated from the manifest.
 */
@Singleton
class DailyCanvasBootstrap
    @Inject
    constructor(
        private val fileRepository: FileRepository,
        private val paths: JournalPathResolver,
        private val canvasRepository: CanvasRepository,
        private val indexRepository: CanvasIndexRepository,
    ) {
        private val clock: Clock = Clock.systemDefaultZone()

        suspend fun openOrCreate(date: LocalDate): DailyCanvasHandle {
            val canvasPath = "${paths.entryDirectory(date)}/$DAILY_CANVAS_FILE"
            val markdownRelative = "journal.md" // relative to the canvas file's dir
            val markdownPath = paths.markdownFile(date)

            val existing = canvasRepository.load(canvasPath)
            if (existing != null) {
                ensureMarkdownFile(markdownPath)
                mirrorIndex(existing, canvasPath)
                return DailyCanvasHandle(
                    canvasId = existing.id,
                    canvasPath = canvasPath,
                    markdownPath = markdownPath,
                    manifest = existing,
                    wasCreated = false,
                )
            }

            val now = clock.millis()
            val markdownBlock = Block(
                id = UUID.randomUUID().toString(),
                kind = BlockKind.MARKDOWN_EMBED,
                x = MARKDOWN_BLOCK_INSET,
                y = MARKDOWN_BLOCK_INSET,
                width = MARKDOWN_BLOCK_WIDTH,
                height = MARKDOWN_BLOCK_HEIGHT,
                z = 0,
                ref = markdownRelative,
            )
            val manifest = CanvasManifest(
                id = UUID.randomUUID().toString(),
                createdAt = now,
                updatedAt = now,
                blocks = listOf(markdownBlock),
            )

            ensureMarkdownFile(markdownPath)
            canvasRepository.save(canvasPath, manifest)
            mirrorIndex(manifest, canvasPath)

            return DailyCanvasHandle(
                canvasId = manifest.id,
                canvasPath = canvasPath,
                markdownPath = markdownPath,
                manifest = manifest,
                wasCreated = true,
            )
        }

        private suspend fun ensureMarkdownFile(path: String) {
            if (!fileRepository.exists(path)) {
                fileRepository.writeText(path, "")
            }
        }

        private suspend fun mirrorIndex(manifest: CanvasManifest, canvasPath: String) {
            indexRepository.upsertCanvas(
                CanvasEntity(
                    id = manifest.id,
                    path = canvasPath,
                    title = null,
                    createdAt = manifest.createdAt,
                    updatedAt = manifest.updatedAt,
                ),
            )
            val blocks = manifest.blocks.map { block ->
                BlockEntity(
                    id = block.id,
                    canvasId = manifest.id,
                    kind = block.kind,
                    x = block.x,
                    y = block.y,
                    width = block.width,
                    height = block.height,
                    z = block.z,
                    ref = block.ref,
                    updatedAt = manifest.updatedAt,
                )
            }
            indexRepository.replaceBlocksAndEdges(
                canvasId = manifest.id,
                blocks = blocks,
                edges = emptyList(),
            )
            indexRepository.setCanvasTags(manifest.id, manifest.tags)
        }

        private companion object {
            const val DAILY_CANVAS_FILE = "daily.canvas.json"
            const val MARKDOWN_BLOCK_INSET = 120f
            const val MARKDOWN_BLOCK_WIDTH = 640f
            const val MARKDOWN_BLOCK_HEIGHT = 480f
        }
    }
