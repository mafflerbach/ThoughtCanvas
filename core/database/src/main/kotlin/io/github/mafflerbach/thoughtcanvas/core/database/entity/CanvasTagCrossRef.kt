package io.github.mafflerbach.thoughtcanvas.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "canvas_tag_cross_ref",
    primaryKeys = ["canvas_id", "tag_id"],
    indices = [Index("tag_id")],
    foreignKeys = [
        ForeignKey(
            entity = CanvasEntity::class,
            parentColumns = ["id"],
            childColumns = ["canvas_id"],
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
data class CanvasTagCrossRef(
    @ColumnInfo(name = "canvas_id")
    val canvasId: String,
    @ColumnInfo(name = "tag_id")
    val tagId: Long,
)
