package io.github.mafflerbach.thoughtcanvas.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "edges",
    indices = [Index("canvas_id"), Index("from_block_id"), Index("to_block_id")],
    foreignKeys = [
        ForeignKey(
            entity = CanvasEntity::class,
            parentColumns = ["id"],
            childColumns = ["canvas_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class EdgeEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "canvas_id")
    val canvasId: String,
    @ColumnInfo(name = "from_block_id")
    val fromBlockId: String,
    @ColumnInfo(name = "to_block_id")
    val toBlockId: String,
    @ColumnInfo(name = "from_side")
    val fromSide: String?,
    @ColumnInfo(name = "to_side")
    val toSide: String?,
    @ColumnInfo(name = "label")
    val label: String?,
)
