package io.github.mafflerbach.thoughtcanvas.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Index row for a single day's journal entry.
 *
 * The filesystem is the source of truth; this table only exists to make
 * listing, sorting, and joining with tags fast. [date] is the primary key
 * because the app is strictly one-entry-per-day.
 *
 * Dates are stored as ISO-8601 strings (`YYYY-MM-DD`) so they sort
 * lexicographically and stay human-readable in SQLite dumps.
 */
@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey
    @ColumnInfo(name = "date")
    val date: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
