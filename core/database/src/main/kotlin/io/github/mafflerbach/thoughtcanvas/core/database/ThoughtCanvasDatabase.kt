package io.github.mafflerbach.thoughtcanvas.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.mafflerbach.thoughtcanvas.core.database.dao.AttachmentDao
import io.github.mafflerbach.thoughtcanvas.core.database.dao.JournalEntryDao
import io.github.mafflerbach.thoughtcanvas.core.database.dao.TagDao
import io.github.mafflerbach.thoughtcanvas.core.database.entity.AttachmentEntity
import io.github.mafflerbach.thoughtcanvas.core.database.entity.EntryTagCrossRef
import io.github.mafflerbach.thoughtcanvas.core.database.entity.JournalEntryEntity
import io.github.mafflerbach.thoughtcanvas.core.database.entity.TagEntity

@Database(
    entities = [
        JournalEntryEntity::class,
        TagEntity::class,
        EntryTagCrossRef::class,
        AttachmentEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class ThoughtCanvasDatabase : RoomDatabase() {
    abstract fun journalEntryDao(): JournalEntryDao

    abstract fun tagDao(): TagDao

    abstract fun attachmentDao(): AttachmentDao

    companion object {
        const val DATABASE_NAME = "thoughtcanvas.db"
    }
}
