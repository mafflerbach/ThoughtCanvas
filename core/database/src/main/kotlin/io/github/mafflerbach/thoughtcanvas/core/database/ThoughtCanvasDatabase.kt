package io.github.mafflerbach.thoughtcanvas.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.mafflerbach.thoughtcanvas.core.database.dao.AttachmentDao
import io.github.mafflerbach.thoughtcanvas.core.database.dao.BlockDao
import io.github.mafflerbach.thoughtcanvas.core.database.dao.CanvasDao
import io.github.mafflerbach.thoughtcanvas.core.database.dao.CanvasTagDao
import io.github.mafflerbach.thoughtcanvas.core.database.dao.EdgeDao
import io.github.mafflerbach.thoughtcanvas.core.database.dao.JournalEntryDao
import io.github.mafflerbach.thoughtcanvas.core.database.dao.TagDao
import io.github.mafflerbach.thoughtcanvas.core.database.entity.AttachmentEntity
import io.github.mafflerbach.thoughtcanvas.core.database.entity.BlockEntity
import io.github.mafflerbach.thoughtcanvas.core.database.entity.BlockTagCrossRef
import io.github.mafflerbach.thoughtcanvas.core.database.entity.CanvasEntity
import io.github.mafflerbach.thoughtcanvas.core.database.entity.CanvasTagCrossRef
import io.github.mafflerbach.thoughtcanvas.core.database.entity.EdgeEntity
import io.github.mafflerbach.thoughtcanvas.core.database.entity.EntryTagCrossRef
import io.github.mafflerbach.thoughtcanvas.core.database.entity.JournalEntryEntity
import io.github.mafflerbach.thoughtcanvas.core.database.entity.TagEntity

/**
 * Database version history:
 *  - v1: Phase 1 flat journal (journal_entries, tags, entry_tag_cross_ref, attachments).
 *  - v2: Phase 2 block canvas (adds canvases, blocks, edges,
 *        canvas_tag_cross_ref, block_tag_cross_ref). Legacy v1 tables are
 *        still declared so Room can accept destructive migration cleanly
 *        during the transition; they will be dropped once the flat journal
 *        UI is removed in Phase 2.6.
 *
 * See ADR-0004 for the block canvas schema decisions.
 */
@Database(
    entities = [
        // Legacy (Phase 1) — retained until Phase 2.6.
        JournalEntryEntity::class,
        EntryTagCrossRef::class,
        AttachmentEntity::class,
        // Shared.
        TagEntity::class,
        // Phase 2 block canvas.
        CanvasEntity::class,
        BlockEntity::class,
        EdgeEntity::class,
        CanvasTagCrossRef::class,
        BlockTagCrossRef::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class ThoughtCanvasDatabase : RoomDatabase() {
    // Legacy (v1).
    abstract fun journalEntryDao(): JournalEntryDao

    abstract fun attachmentDao(): AttachmentDao

    // Shared.
    abstract fun tagDao(): TagDao

    // Phase 2.
    abstract fun canvasDao(): CanvasDao

    abstract fun blockDao(): BlockDao

    abstract fun edgeDao(): EdgeDao

    abstract fun canvasTagDao(): CanvasTagDao

    companion object {
        const val DATABASE_NAME = "thoughtcanvas.db"
    }
}
