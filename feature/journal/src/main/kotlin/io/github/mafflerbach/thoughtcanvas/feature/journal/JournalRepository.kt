package io.github.mafflerbach.thoughtcanvas.feature.journal

import io.github.mafflerbach.thoughtcanvas.core.database.JournalIndexRepository
import io.github.mafflerbach.thoughtcanvas.core.storage.FileRepository
import io.github.mafflerbach.thoughtcanvas.core.storage.JournalPathResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Journal-level facade. Composes filesystem I/O (`FileRepository`) with the
 * fast index (`JournalIndexRepository`) so callers never touch either
 * directly.
 *
 * Contract:
 * - Filesystem is source of truth. If the DB row is missing but the file is
 *   present, the file wins on next load.
 * - Writes are always: (1) write files, (2) mirror to the index. Failure
 *   during (1) leaves nothing inconsistent; failure between (1) and (2)
 *   leaves an orphan on disk which the next load will re-index.
 * - Timestamps in ms since epoch, produced from [clock] so tests can freeze
 *   time.
 */
@Singleton
class JournalRepository
    @Inject
    constructor(
        private val fileRepository: FileRepository,
        private val indexRepository: JournalIndexRepository,
        private val paths: JournalPathResolver,
    ) {
        private val clock: Clock = Clock.systemDefaultZone()
        private val json: Json = DefaultJson

        /**
         * Load the entry for [date]. Returns an empty [JournalContent] shell if
         * no metadata exists on disk yet; the caller can treat that as
         * "brand-new entry".
         */
        suspend fun load(date: LocalDate): JournalContent {
            val metadata = readMetadata(date)
            val markdown = fileRepository.readText(paths.markdownFile(date)).orEmpty()
            return JournalContent(
                date = date,
                markdown = markdown,
                tags = metadata?.tags.orEmpty(),
                createdAt = metadata?.createdAt,
            )
        }

        /**
         * Persist [markdown] and [tags] for [date]. Both files and the DB index
         * are updated. Missing directories are created on demand.
         */
        suspend fun save(date: LocalDate, markdown: String, tags: List<String>) {
            val now = clock.millis()
            val existing = readMetadata(date)
            val createdAt = existing?.createdAt ?: now
            val cleanedTags = tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct()

            val metadata = JournalMetadata(
                date = date.toString(),
                createdAt = createdAt,
                updatedAt = now,
                tags = cleanedTags,
            )

            fileRepository.writeText(paths.markdownFile(date), markdown)
            fileRepository.writeText(paths.metadataFile(date), json.encodeToString(JournalMetadata.serializer(), metadata))

            indexRepository.upsertEntry(date = date, updatedAt = now, createdAt = createdAt)
            indexRepository.setTags(date = date, tagNames = cleanedTags)
        }

        /**
         * Copy [bytes] into `images/<uuid>.<ext>` under the entry folder and
         * record an attachment row.
         *
         * Returns the path *relative to `journal.md`* (e.g. `images/abc.jpg`)
         * so callers can drop it straight into a Markdown `![]()` link. The
         * full root-relative path is what lands in the database.
         */
        suspend fun addImage(date: LocalDate, bytes: ByteArray, extension: String): String {
            val now = clock.millis()
            val safeExt = extension.trim().trimStart('.').ifBlank { "jpg" }
            val fileName = "${UUID.randomUUID()}.$safeExt"
            val entryRelative = "images/$fileName"
            val rootRelative = "${paths.imagesDirectory(date)}/$fileName"

            fileRepository.writeBytes(rootRelative, bytes)
            indexRepository.addAttachment(
                date = date,
                kind = "image",
                relativePath = rootRelative,
                createdAt = now,
            )
            // Bump entry so today shows up in "recently updated" listings.
            indexRepository.upsertEntry(date = date, updatedAt = now)
            return entryRelative
        }

        fun observeContent(date: LocalDate): Flow<JournalContent> = combine(
            indexRepository.observeEntry(date),
            indexRepository.observeTags(date),
        ) { _, tags ->
            val markdown = fileRepository.readText(paths.markdownFile(date)).orEmpty()
            val metadata = readMetadata(date)
            JournalContent(
                date = date,
                markdown = markdown,
                tags = tags.map { it.name },
                createdAt = metadata?.createdAt,
            )
        }

        fun observeImageAttachments(date: LocalDate): Flow<List<String>> =
            indexRepository.observeAttachments(date).map { attachments ->
                attachments.filter { it.kind == "image" }.map { it.relativePath }
            }

        private suspend fun readMetadata(date: LocalDate): JournalMetadata? {
            val raw = fileRepository.readText(paths.metadataFile(date)) ?: return null
            return runCatching { json.decodeFromString(JournalMetadata.serializer(), raw) }.getOrNull()
        }

        private companion object {
            val DefaultJson = Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
        }
    }

/**
 * Snapshot of a journal entry served to the UI. Immutable; callers build
 * modified copies and push them back through [JournalRepository.save].
 */
data class JournalContent(
    val date: LocalDate,
    val markdown: String,
    val tags: List<String>,
    val createdAt: Long?,
)
