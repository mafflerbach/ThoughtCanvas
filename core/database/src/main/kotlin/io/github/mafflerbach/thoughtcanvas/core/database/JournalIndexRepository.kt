package io.github.mafflerbach.thoughtcanvas.core.database

import androidx.room.withTransaction
import io.github.mafflerbach.thoughtcanvas.core.database.dao.AttachmentDao
import io.github.mafflerbach.thoughtcanvas.core.database.dao.JournalEntryDao
import io.github.mafflerbach.thoughtcanvas.core.database.dao.TagDao
import io.github.mafflerbach.thoughtcanvas.core.database.entity.AttachmentEntity
import io.github.mafflerbach.thoughtcanvas.core.database.entity.EntryTagCrossRef
import io.github.mafflerbach.thoughtcanvas.core.database.entity.JournalEntryEntity
import io.github.mafflerbach.thoughtcanvas.core.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Journal-shaped facade over the raw DAOs. Business logic lives here so
 * `feature:*` modules never touch DAOs directly.
 *
 * All dates cross the boundary as [LocalDate]; storage-format strings are an
 * internal detail.
 */
@Singleton
class JournalIndexRepository
    @Inject
    constructor(
        private val db: ThoughtCanvasDatabase,
        private val entryDao: JournalEntryDao,
        private val tagDao: TagDao,
        private val attachmentDao: AttachmentDao,
    ) {
        /** Insert-or-update the entry for [date] and mark [updatedAt]. */
        suspend fun upsertEntry(date: LocalDate, updatedAt: Long, createdAt: Long? = null) {
            val key = date.toIsoKey()
            db.withTransaction {
                val existing = entryDao.findByDate(key)
                val created = createdAt ?: existing?.createdAt ?: updatedAt
                entryDao.upsert(JournalEntryEntity(date = key, updatedAt = updatedAt, createdAt = created))
            }
        }

        fun observeEntry(date: LocalDate): Flow<JournalEntryEntity?> =
            entryDao.observeByDate(date.toIsoKey())

        fun observeAllEntries(): Flow<List<JournalEntryEntity>> = entryDao.observeAll()

        suspend fun deleteEntry(date: LocalDate) {
            entryDao.deleteByDate(date.toIsoKey())
        }

        /**
         * Replace the tag set for [date] with [tagNames], creating any tags that
         * do not exist yet.
         */
        suspend fun setTags(date: LocalDate, tagNames: List<String>) {
            val key = date.toIsoKey()
            db.withTransaction {
                tagDao.clearTagsForEntry(key)
                for (rawName in tagNames) {
                    val name = rawName.trim()
                    if (name.isEmpty()) continue
                    val existing = tagDao.findByName(name)
                    val tagId = if (existing != null) {
                        existing.id
                    } else {
                        val inserted = tagDao.insertIgnore(TagEntity(name = name))
                        if (inserted != -1L) inserted else tagDao.findByName(name)!!.id
                    }
                    tagDao.linkEntryToTag(EntryTagCrossRef(entryDate = key, tagId = tagId))
                }
            }
        }

        fun observeTags(date: LocalDate): Flow<List<TagEntity>> =
            tagDao.observeTagsForEntry(date.toIsoKey())

        fun observeAllTags(): Flow<List<TagEntity>> = tagDao.observeAll()

        suspend fun addAttachment(
            date: LocalDate,
            kind: String,
            relativePath: String,
            createdAt: Long,
        ): Long = attachmentDao.insert(
            AttachmentEntity(
                entryDate = date.toIsoKey(),
                kind = kind,
                relativePath = relativePath,
                createdAt = createdAt,
            ),
        )

        fun observeAttachments(date: LocalDate): Flow<List<AttachmentEntity>> =
            attachmentDao.observeForEntry(date.toIsoKey())

        suspend fun deleteAttachment(id: Long) {
            attachmentDao.deleteById(id)
        }

        private fun LocalDate.toIsoKey(): String = toString() // ISO-8601 YYYY-MM-DD
    }
