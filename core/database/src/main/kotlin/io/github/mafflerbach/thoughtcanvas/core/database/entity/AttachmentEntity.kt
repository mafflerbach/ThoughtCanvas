package io.github.mafflerbach.thoughtcanvas.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Index row for a single attachment (image, audio, generic file) that lives
 * inside a journal entry's folder.
 *
 * [relativePath] is stored relative to the storage root so the row keeps
 * working across devices even if the user re-mounts the folder somewhere
 * else.
 */
@Entity(
    tableName = "attachments",
    indices = [Index("entry_date")],
    foreignKeys = [
        ForeignKey(
            entity = JournalEntryEntity::class,
            parentColumns = ["date"],
            childColumns = ["entry_date"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "entry_date")
    val entryDate: String,
    @ColumnInfo(name = "kind")
    val kind: String,
    @ColumnInfo(name = "relative_path")
    val relativePath: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
