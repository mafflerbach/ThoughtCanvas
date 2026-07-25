package io.github.mafflerbach.thoughtcanvas.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Many-to-many bridge between [JournalEntryEntity] and [TagEntity]. Composite
 * PK; both foreign keys cascade on delete so removing an entry or a tag
 * cleans up the association automatically.
 */
@Entity(
    tableName = "entry_tag_cross_ref",
    primaryKeys = ["entry_date", "tag_id"],
    indices = [Index("tag_id")],
    foreignKeys = [
        ForeignKey(
            entity = JournalEntryEntity::class,
            parentColumns = ["date"],
            childColumns = ["entry_date"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class EntryTagCrossRef(
    @ColumnInfo(name = "entry_date")
    val entryDate: String,
    @ColumnInfo(name = "tag_id")
    val tagId: Long,
)
