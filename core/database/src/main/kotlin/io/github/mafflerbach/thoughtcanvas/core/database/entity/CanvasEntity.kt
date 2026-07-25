package io.github.mafflerbach.thoughtcanvas.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Index row for a canvas manifest on disk.
 *
 * The [path] is storage-root-relative (e.g.
 * `Journal/2025/01/17/daily.canvas.json`) and is the join key for locating
 * the manifest file. Titles are derived by the app (often from the file
 * name) and cached here for fast listings.
 */
@Entity(
    tableName = "canvases",
    indices = [Index(value = ["path"], unique = true)],
)
data class CanvasEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "path")
    val path: String,
    @ColumnInfo(name = "title")
    val title: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
