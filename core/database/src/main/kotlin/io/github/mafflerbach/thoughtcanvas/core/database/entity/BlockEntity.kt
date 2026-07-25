package io.github.mafflerbach.thoughtcanvas.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blocks",
    indices = [Index("canvas_id")],
    foreignKeys = [
        ForeignKey(
            entity = CanvasEntity::class,
            parentColumns = ["id"],
            childColumns = ["canvas_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class BlockEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "canvas_id")
    val canvasId: String,
    @ColumnInfo(name = "kind")
    val kind: String,
    @ColumnInfo(name = "x")
    val x: Float,
    @ColumnInfo(name = "y")
    val y: Float,
    @ColumnInfo(name = "width")
    val width: Float,
    @ColumnInfo(name = "height")
    val height: Float,
    @ColumnInfo(name = "z")
    val z: Int,
    @ColumnInfo(name = "ref")
    val ref: String?,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
