package io.github.mafflerbach.thoughtcanvas.core.database

import androidx.room.withTransaction
import io.github.mafflerbach.thoughtcanvas.core.database.dao.BlockDao
import io.github.mafflerbach.thoughtcanvas.core.database.dao.CanvasDao
import io.github.mafflerbach.thoughtcanvas.core.database.dao.CanvasTagDao
import io.github.mafflerbach.thoughtcanvas.core.database.dao.EdgeDao
import io.github.mafflerbach.thoughtcanvas.core.database.dao.TagDao
import io.github.mafflerbach.thoughtcanvas.core.database.entity.BlockEntity
import io.github.mafflerbach.thoughtcanvas.core.database.entity.CanvasEntity
import io.github.mafflerbach.thoughtcanvas.core.database.entity.CanvasTagCrossRef
import io.github.mafflerbach.thoughtcanvas.core.database.entity.EdgeEntity
import io.github.mafflerbach.thoughtcanvas.core.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Canvas-shaped facade over the Room DAOs. The only surface `:feature:*`
 * modules should import for read-through access to the canvas index.
 *
 * Writes always mirror the on-disk manifest — call these after successfully
 * persisting the `.canvas.json` file so a crashed process cannot leave a DB
 * row pointing at a non-existent manifest.
 */
@Singleton
class CanvasIndexRepository
    @Inject
    constructor(
        private val db: ThoughtCanvasDatabase,
        private val canvasDao: CanvasDao,
        private val blockDao: BlockDao,
        private val edgeDao: EdgeDao,
        private val tagDao: TagDao,
        private val canvasTagDao: CanvasTagDao,
    ) {
        suspend fun upsertCanvas(canvas: CanvasEntity) {
            canvasDao.upsert(canvas)
        }

        suspend fun findCanvasByPath(path: String): CanvasEntity? = canvasDao.findByPath(path)

        fun observeCanvas(id: String): Flow<CanvasEntity?> = canvasDao.observeById(id)

        fun observeAllCanvases(): Flow<List<CanvasEntity>> = canvasDao.observeAll()

        suspend fun deleteCanvas(id: String) {
            canvasDao.deleteById(id)
        }

        /**
         * Replace the full block+edge set of [canvasId] with [blocks] and [edges].
         * Used after loading a manifest from disk.
         */
        suspend fun replaceBlocksAndEdges(
            canvasId: String,
            blocks: List<BlockEntity>,
            edges: List<EdgeEntity>,
        ) {
            db.withTransaction {
                blockDao.deleteForCanvas(canvasId)
                edgeDao.deleteForCanvas(canvasId)
                if (blocks.isNotEmpty()) blockDao.upsertAll(blocks)
                if (edges.isNotEmpty()) edgeDao.upsertAll(edges)
            }
        }

        fun observeBlocks(canvasId: String): Flow<List<BlockEntity>> =
            blockDao.observeForCanvas(canvasId)

        fun observeEdges(canvasId: String): Flow<List<EdgeEntity>> =
            edgeDao.observeForCanvas(canvasId)

        /**
         * Replace canvas-level tags. Tag entities are created on demand and
         * reused via unique-name index.
         */
        suspend fun setCanvasTags(canvasId: String, tagNames: List<String>) {
            db.withTransaction {
                canvasTagDao.clearTagsForCanvas(canvasId)
                for (rawName in tagNames) {
                    val name = rawName.trim()
                    if (name.isEmpty()) continue
                    val tagId = tagDao.findByName(name)?.id
                        ?: tagDao
                            .insertIgnore(TagEntity(name = name))
                            .takeIf { it != -1L }
                        ?: tagDao.findByName(name)!!.id
                    canvasTagDao.linkCanvasToTag(CanvasTagCrossRef(canvasId, tagId))
                }
            }
        }

        fun observeCanvasTags(canvasId: String): Flow<List<TagEntity>> =
            canvasTagDao.observeTagsForCanvas(canvasId)
    }
